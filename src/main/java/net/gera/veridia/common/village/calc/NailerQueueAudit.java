package net.gera.veridia.common.village.calc;

import com.mojang.logging.LogUtils;
import net.gera.veridia.common.aibuff.villager.nailer.NailerVillagerConfig;
import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Диагностика очереди nailer'а (команда /calculate nailer и логи [NAILER-DEBUG]): собирает очередь той же
 * NailerWallQueue.build, что у настоящего жителя, но с журналом решений (QueueReport), и сверяет каждую
 * запись очереди с живым миром - сделано / пусто / занято деревом / занято чужим / не прогружено, в разрезе
 * высоты и связных масс. По такому отчёту видно, что очередь отбросила и почему, а что просто ещё не достроено.
 */
public final class NailerQueueAudit {

    private NailerQueueAudit() {
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Выключается одним флагом; логи пишутся в latest.log с меткой [NAILER-DEBUG]. */
    public static final boolean LOG_ENABLED = true;

    /** Сколько недостроенных записей перечислять в файле / в логе при завершении очереди. */
    private static final int DETAIL_ENTRIES = 80;
    private static final int LOG_ENTRIES = 12;
    /** Половина стороны окна карты плана вокруг игрока (или точки команды). */
    private static final int MAP_HALF = 128;

    public enum Status { DONE, EMPTY, TREE, FOREIGN, UNLOADED }

    /** Итог сверки очереди с миром: сколько записей в каждом состоянии. */
    public record Progress(int total, Map<Status, Integer> byStatus) {
        public int count(Status status) {
            return byStatus.getOrDefault(status, 0);
        }

        public int percentDone() {
            return total == 0 ? 100 : count(Status.DONE) * 100 / total;
        }

        public String line() {
            return count(Status.DONE) + "/" + total + " (" + percentDone() + "%), не сделано: пусто " + count(Status.EMPTY)
                    + ", занято деревом " + count(Status.TREE) + ", занято чужим " + count(Status.FOREIGN)
                    + ", не прогружено " + count(Status.UNLOADED);
        }
    }

    public record Result(List<String> chat, @Nullable Path file) {}

    // --- лог настоящего nailer'а ---

    public static void log(String message, Object... args) {
        if (LOG_ENABLED) {
            LOGGER.info("[NAILER-DEBUG] " + message, args);
        }
    }

    // --- состояние записи в мире ---

    public static Status statusOf(ServerLevel level, NailerWallQueue.Entry entry) {
        BlockPos pos = entry.pos();
        if (!level.isLoaded(pos)) {
            return Status.UNLOADED;
        }
        BlockState actual = level.getBlockState(pos);
        if (NailerWallQueue.statesMatch(actual, entry.state())) {
            return Status.DONE;
        }
        if (actual.isAir() || actual.canBeReplaced()) {
            return Status.EMPTY;
        }
        return VillageBlocks.isTreePart(actual) ? Status.TREE : Status.FOREIGN;
    }

    public static Progress progressOf(ServerLevel level, List<NailerWallQueue.Piece> pieces) {
        Map<Status, Integer> byStatus = new EnumMap<>(Status.class);
        int total = 0;
        for (NailerWallQueue.Piece piece : pieces) {
            for (NailerWallQueue.Entry entry : piece.entries()) {
                byStatus.merge(statusOf(level, entry), 1, Integer::sum);
                total++;
            }
        }
        return new Progress(total, byStatus);
    }

    /** Первые limit недостроенных записей очереди по порядку стройки - для лога при завершении смены. */
    public static List<String> firstUnfinished(ServerLevel level, List<NailerWallQueue.Piece> pieces,
                                               Predicate<BlockPos> unreachable, int limit) {
        List<String> lines = new ArrayList<>();
        for (int p = 0; p < pieces.size() && lines.size() < limit; p++) {
            for (NailerWallQueue.Entry entry : pieces.get(p).entries()) {
                Status status = statusOf(level, entry);
                if (status == Status.DONE) continue;
                lines.add("масса #" + p + " " + describe(entry) + " сейчас " + status + (unreachable.test(entry.pos()) ? " [помечен недостижимым]" : ""));
                if (lines.size() >= limit) break;
            }
        }
        return lines;
    }

    // --- команда /calculate nailer ---

    /**
     * fresh = false: очередь по плану, который сессия посчитала при загрузке деревни (именно по нему строит
     * настоящий nailer). fresh = true: по плану, посчитанному заново по ТЕКУЩЕМУ миру, плюс сравнение двух планов.
     * Расхождение значит, что стена, построенная в прошлый запуск, сдвинула расчёт (скан видит её как постройку).
     */
    public static Result run(ServerLevel level, BlockPos near, boolean fresh) {
        return run(level, near, fresh, false);
    }

    /** centeredOnPoint: свежий план со скан-окном строго вокруг near (а не вокруг якоря плана сессии) - воспроизводит план с заданного якоря. */
    public static Result run(ServerLevel level, BlockPos near, boolean fresh, boolean centeredOnPoint) {
        List<VillageAutoPregen.VillagePlan> plans = VillageAutoPregen.plans(level);
        if (plans.isEmpty()) {
            return new Result(List.of("Автопланов стен в этом мире ещё нет (/calculate pregen) - подойди к деревне и подожди расчёта"), null);
        }
        VillageAutoPregen.VillagePlan cached = plans.stream()
                .min(Comparator.comparingDouble(candidate -> candidate.anchor().distSqr(near))).orElseThrow();
        Villager nailer = nearestNailer(level, cached);
        if (nailer == null) {
            return new Result(List.of("Рядом с деревней у " + cached.anchor().toShortString()
                    + " нет nailer'а - очередь зависит от его технологий (tech_1/2/3), посчитать не с чем"), null);
        }
        VillageAutoPregen.VillagePlan plan = fresh ? freshPlanLike(level, centeredOnPoint ? near : cached.anchor()) : cached;

        QueueReport report = new QueueReport(plan.scan());
        List<NailerWallQueue.Piece> pieces = NailerWallQueue.build(level, plan, nailer, report);
        Progress progress = progressOf(level, pieces);

        List<String> chat = new ArrayList<>();
        chat.add("Очередь nailer'а для деревни у " + plan.anchor().toShortString() + ", стиль " + plan.style()
                + ", порядок стройки " + NailerVillagerConfig.WALL_BUILD_ORDER
                + (fresh ? ", ПЛАН СЧИТАН ЗАНОВО по текущему миру" : ", план сессии (по нему строит житель)"));
        if (fresh) {
            chat.add(planDrift(cached, plan));
        }
        chat.addAll(report.summary());
        chat.add("В мире сейчас: сделано " + progress.line());
        chat.add("По высоте Y (сколько % записей слоя уже стоит): " + layerLine(level, pieces));
        chat.add(waterLine(plan));
        chat.add("План опирается на уже построенную стену (поверхность под куском - наш блок): " + segmentsOnOwnWall(level, plan) + " из "
                + plan.plan().segments().size() + " кусков");

        List<String> file = new ArrayList<>(chat);
        file.addAll(componentLines(level, pieces));
        file.addAll(report.planMap(near.getX(), near.getZ(), MAP_HALF));
        file.addAll(report.details());
        file.addAll(unfinishedLines(level, pieces));
        Path path = write(file);
        chat.add(path == null ? "Файл отчёта записать не удалось" : "Подробно (координаты отказов, недостроенные блоки): " + path);
        return new Result(chat, path);
    }

    /** Водные клетки контура: сколько получат решётку под водой, а сколько глубже предела (там стены не будет совсем). */
    private static String waterLine(VillageAutoPregen.VillagePlan plan) {
        int total = plan.plan().waterCells().size();
        int barred = 0;
        int tooDeep = 0;
        for (WallPlan.WaterCell cell : plan.plan().waterCells()) {
            int depth = plan.scan().waterDepthAt(cell.cellX(), cell.cellZ());
            if (depth > VillageCalcSettings.MAX_WATER_BARRIER_HEIGHT) {
                tooDeep++;
            } else if (depth >= VillageCalcSettings.WATER_MIN_DEPTH) {
                barred++;
            }
        }
        return "Вода на контуре: клеток " + total + ", получат решётку " + barred + ", глубже предела "
                + VillageCalcSettings.MAX_WATER_BARRIER_HEIGHT + " блоков (стены там не будет) " + tooDeep;
    }

    private static VillageAutoPregen.VillagePlan freshPlanLike(ServerLevel level, BlockPos anchor) {
        VillageScan scan = VillageScan.scan(level, anchor, VillageCalc.scanShape(), VillageCalc.scanRadius(),
                VillageCalcSettings.MarkerSource.VILLAGE);
        return new VillageAutoPregen.VillagePlan(anchor, scan, WallPlan.build(scan), VillageStyle.of(level, anchor));
    }

    /** Кусок плана в мировых координатах - чтобы сравнивать планы двух разных сканов. */
    private record SegmentKey(int worldX, int worldZ, Direction along, boolean entrance) {
        static SegmentKey of(VillageScan scan, WallPlan.Segment segment) {
            return new SegmentKey(scan.worldX(segment.cellX()), scan.worldZ(segment.cellZ()), segment.along(), segment.entrance());
        }
    }

    private static String planDrift(VillageAutoPregen.VillagePlan cached, VillageAutoPregen.VillagePlan fresh) {
        Set<SegmentKey> before = segmentKeys(cached);
        Set<SegmentKey> now = segmentKeys(fresh);
        Set<SegmentKey> same = new HashSet<>(before);
        same.retainAll(now);
        return "Сравнение планов: кусков в плане сессии " + before.size() + ", в свежем " + now.size() + ", совпало " + same.size()
                + " (только в плане сессии " + (before.size() - same.size()) + ", только в свежем " + (now.size() - same.size())
                + "); столбиков в плане: " + cached.plan().fillers().size() + " -> " + fresh.plan().fillers().size()
                + ", башен: " + cached.plan().towers().size() + " -> " + fresh.plan().towers().size();
    }

    private static Set<SegmentKey> segmentKeys(VillageAutoPregen.VillagePlan plan) {
        Set<SegmentKey> keys = new HashSet<>();
        for (WallPlan.Segment segment : plan.plan().segments()) {
            keys.add(SegmentKey.of(plan.scan(), segment));
        }
        return keys;
    }

    @Nullable
    private static Villager nearestNailer(ServerLevel level, VillageAutoPregen.VillagePlan plan) {
        AABB area = new AABB(plan.anchor()).inflate(NailerVillagerConfig.MAX_VILLAGE_SEARCH_DISTANCE);
        return level.getEntitiesOfClass(Villager.class, area,
                        villager -> villager.getVillagerData().getProfession() == ModVillagers.NAILER.value())
                .stream()
                .min(Comparator.comparingDouble(villager -> villager.blockPosition().distSqr(plan.anchor())))
                .orElse(null);
    }

    /** "y=67:100 y=68:100 ... y=72:97 y=73:0" - по каждому слою высоты. Порог по Y виден сразу: "слоями" строит LAYERED. */
    private static String layerLine(ServerLevel level, List<NailerWallQueue.Piece> pieces) {
        Map<Integer, int[]> doneAndTotalByY = new TreeMap<>();
        for (NailerWallQueue.Piece piece : pieces) {
            for (NailerWallQueue.Entry entry : piece.entries()) {
                int[] counter = doneAndTotalByY.computeIfAbsent(entry.pos().getY(), y -> new int[2]);
                counter[1]++;
                if (statusOf(level, entry) == Status.DONE) counter[0]++;
            }
        }
        StringBuilder line = new StringBuilder();
        doneAndTotalByY.forEach((y, counter) -> line.append(y).append(':').append(counter[0] * 100 / counter[1]).append(' '));
        return line.toString().trim();
    }

    private static List<String> componentLines(ServerLevel level, List<NailerWallQueue.Piece> pieces) {
        List<String> lines = new ArrayList<>();
        lines.add("=== Связные массы очереди: прогресс ===");
        for (int i = 0; i < pieces.size(); i++) {
            Progress progress = progressOf(level, List.of(pieces.get(i)));
            lines.add("  #" + i + " якорь " + pieces.get(i).anchor().toShortString() + ": " + progress.line());
        }
        return lines;
    }

    private static List<String> unfinishedLines(ServerLevel level, List<NailerWallQueue.Piece> pieces) {
        List<String> lines = new ArrayList<>();
        lines.add("=== Первые недостроенные записи по порядку стройки ===");
        lines.addAll(firstUnfinished(level, pieces, pos -> false, DETAIL_ENTRIES));
        return lines;
    }

    /** Кусков плана, у которых под одной из клеток лежит наш же блок стены: план считался поверх построенной стены
     * (перезапуск: VillageAutoPregen пересканирует живой мир, и прежняя стена для скана - постройка). */
    private static int segmentsOnOwnWall(ServerLevel level, VillageAutoPregen.VillagePlan plan) {
        VillageScan scan = plan.scan();
        int count = 0;
        for (WallPlan.Segment segment : plan.plan().segments()) {
            for (Cell cell : segment.cells()) {
                if (!scan.inBounds(cell.x(), cell.z())) continue;
                BlockPos surface = new BlockPos(scan.worldX(cell.x()), scan.surfaceYAt(cell.x(), cell.z()), scan.worldZ(cell.z()));
                if (level.isLoaded(surface) && NailerWallQueue.isWallMaterial(level.getBlockState(surface))) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    static String describe(NailerWallQueue.Entry entry) {
        BlockPos pos = entry.pos();
        return pos.getX() + "," + pos.getY() + "," + pos.getZ() + " нужен " + BuiltInRegistries.BLOCK.getKey(entry.state().getBlock());
    }

    @Nullable
    private static Path write(List<String> lines) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = FMLPaths.GAMEDIR.get().resolve("veridia-debug").resolve("nailer-queue-" + timestamp + ".txt");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            LOGGER.warn("[NAILER-DEBUG] не записать отчёт {}", file, e);
            return null;
        }
    }
}
