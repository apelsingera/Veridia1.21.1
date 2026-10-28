package net.gera.veridia.common.worldgen.structure;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.village.calc.VillageStyle;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class ModVillageHouses {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** relativeShare - во сколько раз дом чаще или реже остальных наших домов (сами числа не важны, важно
     * их соотношение); сколько это в весе пула, считается в weightOf. */
    private record VillageHouse(String name, int relativeShare, int maxPerVillage) {}

    /** Пул домов ванильной деревни и стиль (VillageStyle), чьи шаблоны в него добавляются: файлы лежат в
     * data/veridia/structure/village/&lt;папка стиля&gt;/. */
    private record HousePool(String poolId, VillageStyle style) {}

    private static VillageHouse house(String name, int relativeShare, int maxPerVillage) {
        return new VillageHouse(name, relativeShare, maxPerVillage);
    }

    /**
     * Какую долю пула домов занимают ВСЕ наши дома вместе (остальное - ванильные дома и пустой элемент).
     * Одна для всех стилей: вес считается от суммы весов ванильного пула, поэтому в тайге, где пул
     * ванильных домов меньше, наши дома встречаются так же редко, как в равнинах. 0.17 - как было в
     * равнинах при весах 6/4/4/4 (18 из 105).
     */
    private static final double TOTAL_POOL_SHARE = 0.17;

    private static final List<VillageHouse> HOUSES = List.of(
            house("warrior_house", 6, 1),
            house("nailer_house", 4, 1),
            house("lumberjack_house", 4, 1),
            house("storage", 4, 1)
    );

    private static final List<HousePool> TARGET_POOLS = List.of(
            new HousePool("minecraft:village/plains/houses", VillageStyle.PLAINS),
            new HousePool("minecraft:village/taiga/houses", VillageStyle.TAIGA),
            new HousePool("minecraft:village/snowy/houses", VillageStyle.SNOWY)
            // , new HousePool("minecraft:village/savanna/houses", ...)
            // , new HousePool("minecraft:village/desert/houses", ...)
    );

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        Registry<StructureTemplatePool> pools = server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);

        StructurePieceLimits.clearLimits();

        for (HousePool target : TARGET_POOLS) {
            ResourceLocation poolId = ResourceLocation.parse(target.poolId());
            StructureTemplatePool pool = pools.get(poolId);
            if (pool == null) {
                LOGGER.warn("Пул {} не найден — наши дома в деревни этого стиля не попадут", poolId);
                continue;
            }
            // Сумма считается до добавления своих домов: иначе каждый следующий дом считал бы предыдущие ванильными.
            int vanillaWeight = totalWeight(pool);
            for (VillageHouse house : HOUSES) {
                addHouse(server, pool, poolId, target.style(), house, weightOf(house, vanillaWeight));
            }
            int ourWeight = totalWeight(pool) - vanillaWeight;
            LOGGER.info("Пул {}: наши дома занимают {} из {} ({}%)", poolId, ourWeight, totalWeight(pool),
                    Math.round(1000.0 * ourWeight / totalWeight(pool)) / 10.0);
        }
    }

    private static int totalWeight(StructureTemplatePool pool) {
        int total = 0;
        for (Pair<StructurePoolElement, Integer> entry : pool.rawTemplates) {
            total += entry.getSecond();
        }
        return total;
    }

    /** Вес дома, при котором все наши дома вместе занимают TOTAL_POOL_SHARE пула, а между собой делятся по relativeShare. */
    private static int weightOf(VillageHouse house, int vanillaWeight) {
        int shareSum = 0;
        for (VillageHouse other : HOUSES) {
            shareSum += other.relativeShare();
        }
        double ourTotalWeight = vanillaWeight * TOTAL_POOL_SHARE / (1 - TOTAL_POOL_SHARE);
        return Math.max(1, (int) Math.round(ourTotalWeight * house.relativeShare() / shareSum));
    }

    private static void addHouse(MinecraftServer server, StructureTemplatePool pool, ResourceLocation poolId,
                                 VillageStyle style, VillageHouse house, int weight) {
        ResourceLocation structureId = style.house(house.name());
        if (!isUsable(server, structureId)) {
            // Файла нет или он пустой (размер 0): такой кусок в пуле ломает сборку деревни.
            LOGGER.warn("Дом {} отсутствует или пустой — в пул {} не добавлен", structureId, poolId);
            return;
        }

        StructurePoolElement piece = StructurePoolElement.legacy(structureId.toString())
                .apply(StructureTemplatePool.Projection.RIGID);

        for (int i = 0; i < weight; i++) {
            pool.templates.add(piece);
        }
        List<Pair<StructurePoolElement, Integer>> raw = new ArrayList<>(pool.rawTemplates);
        raw.add(new Pair<>(piece, weight));
        pool.rawTemplates = raw;

        if (house.maxPerVillage() > 0) {
            StructurePieceLimits.limit(piece, house.maxPerVillage());
        }

        LOGGER.info("Домик {} добавлен в пул {} с весом {}, лимит на деревню: {}",
                structureId, poolId, weight,
                house.maxPerVillage() > 0 ? house.maxPerVillage() : "без ограничения");
    }

    private static boolean isUsable(MinecraftServer server, ResourceLocation structureId) {
        return server.getStructureManager().get(structureId)
                .map(ModVillageHouses::hasBlocks)
                .orElse(false);
    }

    private static boolean hasBlocks(StructureTemplate template) {
        return template.getSize().getX() > 0 && template.getSize().getY() > 0 && template.getSize().getZ() > 0;
    }
}
