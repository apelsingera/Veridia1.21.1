package net.gera.veridia.common.village.calc;

import com.mojang.logging.LogUtils;
import net.gera.veridia.Veridia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class VillageAutoPregen {

    private VillageAutoPregen() {
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public record VillagePlan(BlockPos anchor, VillageScan scan, WallPlan plan, VillageStyle style) {}

    /** Деревня, которая ждёт расчёта плана: area - её структура из генерации мира (все дома и улицы) с
     * запасом PREGEN_LOAD_MARGIN, waitedChecks - сколько проверок она уже прождала, пока игрок рядом. */
    private record PendingVillage(BlockPos anchor, BoundingBox area, int waitedChecks) {
        PendingVillage waitedOnceMore() {
            return new PendingVillage(anchor, area, waitedChecks + 1);
        }
    }

    private static final Map<ServerLevel, List<BlockPos>> KNOWN_ANCHORS = new HashMap<>();
    private static final Map<ServerLevel, Deque<PendingVillage>> PENDING = new HashMap<>();
    private static final Map<ServerLevel, List<VillagePlan>> PLANS = new HashMap<>();

    private static int tickCounter;

    public static int knownVillages(ServerLevel level) {
        return KNOWN_ANCHORS.getOrDefault(level, List.of()).size();
    }
    public static int queuedVillages(ServerLevel level) {
        Deque<PendingVillage> queue = PENDING.get(level);
        return queue == null ? 0 : queue.size();
    }

    public static int cachedPlans(ServerLevel level) {
        return PLANS.getOrDefault(level, List.of()).size();
    }

    public static List<VillagePlan> plans(ServerLevel level) {
        return PLANS.getOrDefault(level, List.of());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkPos chunkPos = event.getChunk().getPos();
        BlockPos probe = new BlockPos(
                chunkPos.getMinBlockX() + 8, level.getSeaLevel(), chunkPos.getMinBlockZ() + 8);

        Optional<BlockPos> found = level.getPoiManager()
                .getInSquare(holder -> holder.is(PoiTypeTags.VILLAGE), probe,
                        VillageCalcSettings.PREGEN_DETECT_RADIUS, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(pos -> VillageOwnershipFilter.isInVillageStructure(level, pos))
                .findFirst();
        found.ifPresent(anchor -> registerCandidate(level, anchor));
    }

    private static void registerCandidate(ServerLevel level, BlockPos anchor) {
        List<BlockPos> anchors = KNOWN_ANCHORS.computeIfAbsent(level, key -> new ArrayList<>());
        int mergeRadius = VillageCalc.scanRadius();
        for (BlockPos known : anchors) {
            int dx = Math.abs(known.getX() - anchor.getX());
            int dz = Math.abs(known.getZ() - anchor.getZ());
            if (Math.max(dx, dz) <= mergeRadius) {
                return;
            }
        }
        anchors.add(anchor);
        PENDING.computeIfAbsent(level, key -> new ArrayDeque<>()).add(new PendingVillage(anchor, villageArea(level, anchor), 0));
        LOGGER.info("Veridia: новая деревня в {} у {}, план стены посчитается, когда она прогрузится целиком ({}-я по счёту)",
                level.dimension().location(), anchor, anchors.size());
    }

    /** Структура деревни из генерации мира (все её дома и улицы) с запасом PREGEN_LOAD_MARGIN вокруг. */
    private static BoundingBox villageArea(ServerLevel level, BlockPos anchor) {
        StructureStart village = level.structureManager().getStructureWithPieceAt(anchor, StructureTags.VILLAGE);
        BoundingBox structure = village.isValid() ? village.getBoundingBox() : new BoundingBox(anchor);
        return structure.inflatedBy(VillageCalcSettings.PREGEN_LOAD_MARGIN);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < VillageCalcSettings.PREGEN_TICK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        // Не больше одного плана за раз - скан тяжёлый.
        for (Map.Entry<ServerLevel, Deque<PendingVillage>> entry : PENDING.entrySet()) {
            PendingVillage village = pollReadyVillage(entry.getKey(), entry.getValue());
            if (village != null) {
                computePlan(entry.getKey(), village.anchor());
                break;
            }
        }
    }

    /**
     * Первая деревня из очереди, которую уже можно сканировать; остальные уходят в конец очереди.
     * <p>
     * Раньше план считался сразу, как только прогружался ПЕРВЫЙ чанк деревни: игрок подлетает, деревня
     * только появилась на краю прорисовки, а всё непрогруженное VillageScan просто отрезает от
     * территории. План при этом один на всю игровую сессию - и nailer строил стену по краю тогдашней
     * прогрузки, прямыми линиями поперёк деревни, половина домов снаружи (скриншот игрока). /calculate
     * этим не страдал: его запускают, стоя в деревне, когда вокруг всё прогружено.
     * <p>
     * Ждём не весь радиус скана (до 128 блоков - при маленькой прорисовке он не прогружался никогда, и
     * nailer'у было нечего строить, см. старый отчёт "ничего не делает"), а только саму деревню с
     * запасом. Если игрок рядом, но деревня целиком так и не прогрузилась за PREGEN_MAX_WAIT_CHECKS
     * проверок (совсем маленькая прорисовка), - считаем по тому, что есть, как раньше.
     */
    @Nullable
    private static PendingVillage pollReadyVillage(ServerLevel level, Deque<PendingVillage> queue) {
        for (int left = queue.size(); left > 0; left--) {
            PendingVillage village = queue.poll();
            if (isLoaded(level, village.area())) {
                return village;
            }
            if (!isLoaded(level, new BoundingBox(village.anchor()))) {
                // Игрок ушёл от деревни - ждём, пока вернётся; это время не считается.
                queue.addLast(village);
                continue;
            }
            if (village.waitedChecks() >= VillageCalcSettings.PREGEN_MAX_WAIT_CHECKS) {
                LOGGER.warn("Veridia: деревня у {} так и не прогрузилась целиком - считаю план по тому, что есть "
                        + "(стена может пройти по краю прогрузки; помогает дальность прорисовки побольше)", village.anchor());
                return village;
            }
            queue.addLast(village.waitedOnceMore());
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static boolean isLoaded(ServerLevel level, BoundingBox area) {
        for (int chunkX = SectionPos.blockToSectionCoord(area.minX()); chunkX <= SectionPos.blockToSectionCoord(area.maxX()); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(area.minZ()); chunkZ <= SectionPos.blockToSectionCoord(area.maxZ()); chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void computePlan(ServerLevel level, BlockPos anchor) {
        int radius = VillageCalc.scanRadius();
        LOGGER.info("Veridia: начинаю расчёт плана стены для деревни у {} в {} (радиус {})",
                anchor, level.dimension().location(), radius);
        VillageScan scan = VillageScan.scan(level, anchor, VillageCalc.scanShape(), radius,
                VillageCalcSettings.MarkerSource.VILLAGE);
        LOGGER.info("Veridia: скан территории готов для {}, считаю контур стены...", anchor);
        WallPlan plan = WallPlan.build(scan);
        VillageStyle style = VillageStyle.of(level, anchor);
        PLANS.computeIfAbsent(level, key -> new ArrayList<>()).add(new VillagePlan(anchor, scan, plan, style));
        LOGGER.info("Veridia: план стены готов заранее для деревни у {} в {} (кусков стены: {}, стиль: {})",
                anchor, level.dimension().location(), plan.segments().size(), style);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        KNOWN_ANCHORS.clear();
        PENDING.clear();
        PLANS.clear();
        tickCounter = 0;
    }
}