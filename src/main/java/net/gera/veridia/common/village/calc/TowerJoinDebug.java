package net.gera.veridia.common.village.calc;

import com.mojang.logging.LogUtils;
import net.gera.veridia.common.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ВРЕМЕННАЯ диагностика стыков башня-стена (TOWER-DEBUG). На /calculate result пишет в
 * run/veridia-debug/towers-*.txt по три карты вокруг каждой башни:
 * <ul>
 *     <li>PLAN - что решил WallPlan (до мира);</li>
 *     <li>BUILD - что решил VillageCalc.build: какие куски стены встали целиком, какие развалились на
 *     столбики, какие столбики отброшены, что убрал/домостил второй dropStrays;</li>
 *     <li>WORLD - что реально стоит в мире после постройки.</li>
 * </ul>
 * И короткий вердикт по каждой башне в чат ("касаются башни N"). Логику стены ничего из этого не
 * меняет. Всё, что связано с диагностикой, помечено TOWER-DEBUG - удалить одним поиском.
 */
public final class TowerJoinDebug {

    private TowerJoinDebug() {
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    // Выключено после проверки игроком (башни стыкуются, лестница ок) - код оставлен, включить
    // обратно этим флагом, если с башнями снова что-то всплывёт.
    public static final boolean ENABLED = false;
    /** Радиус окна вокруг анкера башни, по которому рисуются карты (клеток = блоков). */
    public static final int WINDOW = 12;
    /** Где в колонке искать блоки стены относительно рельефа скана. */
    private static final int WORLD_PROBE_DOWN = 2;
    private static final int WORLD_PROBE_UP = 10;
    /** Больше этого воздух под стеной не считаем - на карте всё равно одна цифра. */
    private static final int MAX_AIR_BELOW = 9;
    /** Кусок стены ближе этого (в блоках) к телу башни, но не касающийся его - "ОБРЫВ". */
    private static final int DANGLING_RANGE = 4;

    private static final String LEGEND = """
            Карты: строки - мировой Z (слева), столбцы - мировой X (две строки цифр сверху: десятки и единицы,
            'v' над анкером башни, '>' слева у строки анкера). Одна клетка = один блок по XZ.
            Общий фон на всех картах:
              ~ вне territory   . territory   : КЛЕТКА КОНТУРА (isBoundary), НА КОТОРОЙ НИЧЕГО НЕТ
              # isStructure     w вода        (пробел) за пределами скана
            PLAN (WallPlan.build, до мира):
              T тело башни (TowerFootprint)   H клетка лестницы   S клетка куска стены (Segment)   E кусок-вход
              Filler по этапу, который его добавил: f tileRuns (хвост ряда)  j стык cutWallForTowers
                b мост dropStrays  g sealNearWaterGaps  L sealLeaks   ? неизвестно
              r клетка, выкинутая dropStrays как обрывок
            BUILD (решения VillageCalc.build = /calculate result):
              T колонка башни с блоками   H клетка лестницы   t колонка bbox башни без блоков (структурная пустота)
              W кусок стены целиком (fits)  c столбик поставлен   p столбик в списке, но placeFiller отказал
              B столбик-мост второго dropStrays   r убран вторым dropStrays
              x столбик ОТБРОШЕН addColumn: место занято/нет опоры (fillerFits) или башня
              d клетка куска, который не влез целиком (fits=false), и столбика на ней не записано вообще
              (S в PLAN, а в BUILD не W - кусок развалился на столбики c/x/d)
            WORLD (реальный мир после постройки):
              T колонка башни   H клетка лестницы (проходима, касанием не считается)   X блок стены/столбика стоит на земле
              1..9 блок стены есть, но под ним N блоков воздуха/травы (висит)
            """;

    // --- запись по ходу VillageCalc.build (хуки там помечены TOWER-DEBUG) ---

    /** Что решила одна /calculate result. Пересоздаётся в beginBuild. */
    private static final class BuildLog {
        /** Башня -> её bbox в мире, null - не влезла. Нет ключа - башни в /calculate выключены. */
        final Map<WallPlan.Tower, BoundingBox> towerBoxes = new LinkedHashMap<>();
        final Set<Cell> degradedSegmentCells = new HashSet<>();
        final Set<Cell> rejectedColumns = new HashSet<>();
        final Set<Cell> cellsBeforeStrays = new HashSet<>();
        final Set<Cell> removedByStrays = new HashSet<>();
        final Set<Cell> bridgedByStrays = new HashSet<>();
        final Set<Cell> intactSegmentCells = new HashSet<>();
        final Set<Cell> columnCells = new HashSet<>();
        final Set<Cell> failedColumns = new HashSet<>();
    }

    private static BuildLog build = new BuildLog();
    private static final List<String> summary = new ArrayList<>();

    /** Строки для чата после последнего /calculate result. */
    public static List<String> lastSummary() {
        return summary;
    }

    static void beginBuild() {
        build = new BuildLog();
    }

    static void tower(WallPlan.Tower spot, @Nullable BoundingBox box) {
        build.towerBoxes.put(spot, box);
    }

    static void segmentDegraded(WallPlan.Segment segment) {
        build.degradedSegmentCells.addAll(segment.cells());
    }

    static void columnRejected(int cx, int cz) {
        build.rejectedColumns.add(new Cell(cx, cz));
    }

    static void beforeStrays(List<WallPlan.Segment> ready, List<WallPlan.Filler> columns) {
        build.cellsBeforeStrays.addAll(cellsOf(ready, columns));
    }

    static void afterStrays(List<WallPlan.Segment> ready, List<WallPlan.Filler> columns) {
        Set<Cell> after = cellsOf(ready, columns);
        build.removedByStrays.addAll(build.cellsBeforeStrays);
        build.removedByStrays.removeAll(after);
        build.bridgedByStrays.addAll(after);
        build.bridgedByStrays.removeAll(build.cellsBeforeStrays);
        for (WallPlan.Segment segment : ready) {
            build.intactSegmentCells.addAll(segment.cells());
        }
        for (WallPlan.Filler filler : columns) {
            build.columnCells.add(filler.cell());
        }
    }

    static void placeFailed(WallPlan.Filler filler) {
        build.failedColumns.add(filler.cell());
    }

    private static Set<Cell> cellsOf(List<WallPlan.Segment> segments, List<WallPlan.Filler> fillers) {
        Set<Cell> cells = new HashSet<>();
        for (WallPlan.Segment segment : segments) {
            cells.addAll(segment.cells());
        }
        for (WallPlan.Filler filler : fillers) {
            cells.add(filler.cell());
        }
        return cells;
    }

    // --- дамп ---

    static void dump(ServerLevel level, VillageScan scan, WallPlan plan) {
        summary.clear();
        if (!ENABLED) {
            return;
        }
        MapPainter painter = new MapPainter(level, scan, plan, PlanCells.of(scan, plan), wallBlocks());
        StringBuilder report = new StringBuilder(header(scan, plan)).append(LEGEND);
        int number = 0;
        for (WallPlan.Tower tower : plan.towers()) {
            number++;
            report.append(describeTower(painter, tower, number));
        }
        writeReport(report.toString());
    }

    private static String header(VillageScan scan, WallPlan plan) {
        return """
                TOWER-DEBUG дамп стыков башня-стена, %s
                скан: minX=%d minZ=%d size=%dx%d
                башен в плане: %d

                """.formatted(LocalDateTime.now(), scan.minX(), scan.minZ(), scan.sizeX(), scan.sizeZ(),
                plan.towers().size());
    }

    private static String describeTower(MapPainter painter, WallPlan.Tower tower, int number) {
        VillageScan scan = painter.scan();
        Window window = Window.around(tower);
        char[][] planMap = window.draw(painter::planChar);
        char[][] buildMap = window.draw(painter::buildChar);
        char[][] worldMap = window.draw(painter::worldChar);

        String title = "башня #" + number + " " + worldPos(scan, new Cell(tower.cellX(), tower.cellZ()))
                + " out=" + tower.outward() + " " + (TowerFootprint.secondaryOutward(scan, tower) != null ? "угол" : "прямая");
        String verdict = verdict(scan, tower, window, worldMap);
        summary.add(title + ": " + verdict);

        StringBuilder out = new StringBuilder();
        out.append("\n==================================================================\n");
        out.append(title).append('\n');
        out.append("  bbox: ").append(describeBox(tower)).append('\n');
        out.append("  вырез: ").append(describeCut(scan, painter.plan(), tower)).append('\n');
        out.append("  итог по миру: ").append(verdict).append("\n\n");
        appendMaps(out, scan, window, planMap, buildMap, worldMap);
        return out.toString();
    }

    private static String describeBox(WallPlan.Tower tower) {
        if (!build.towerBoxes.containsKey(tower)) {
            return "башни в /calculate result выключены";
        }
        BoundingBox box = build.towerBoxes.get(tower);
        if (box == null) {
            return "НЕ ПОСТАВЛЕНА (towerFits=false)";
        }
        return "%d..%d x %d..%d z %d..%d".formatted(box.minX(), box.maxX(), box.minY(), box.maxY(), box.minZ(), box.maxZ());
    }

    private static String describeCut(VillageScan scan, WallPlan plan, WallPlan.Tower tower) {
        WallPlan.TowerCut cut = plan.towerCuts().stream()
                .filter(candidate -> candidate.tower().equals(tower))
                .findFirst()
                .orElse(null);
        if (cut == null) {
            return "(нет записи)";
        }
        String joints = cut.jointFillers().stream()
                .map(joint -> worldPos(scan, joint.cell()))
                .collect(Collectors.joining(" "));
        return "вырезано кусков %d, столбиков под телом %d, стыков %d: %s".formatted(
                cut.removedSegments().size(), cut.removedFillers().size(), cut.jointFillers().size(), joints);
    }

    private static void writeReport(String report) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = FMLPaths.GAMEDIR.get().resolve("veridia-debug").resolve("towers-" + timestamp + ".txt");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, report, StandardCharsets.UTF_8);
            summary.addFirst("Дамп стыков башен: " + file.toAbsolutePath());
        } catch (IOException e) {
            summary.addFirst("Дамп стыков башен НЕ записан: " + e.getMessage());
            LOGGER.error("TOWER-DEBUG: failed to write dump", e);
        }
        for (String line : summary) {
            LOGGER.info("TOWER-DEBUG: {}", line);
        }
    }

    private static String worldPos(VillageScan scan, Cell cell) {
        return "(" + scan.worldX(cell.x()) + "," + scan.worldZ(cell.z()) + ")";
    }

    // --- карты ---

    /** Квадрат клеток вокруг анкера башни, по которому рисуются карты. */
    private record Window(int minX, int minZ, int size) {

        static Window around(WallPlan.Tower tower) {
            return new Window(tower.cellX() - WINDOW, tower.cellZ() - WINDOW, 2 * WINDOW + 1);
        }

        Cell cellAt(int row, int col) {
            return new Cell(minX + col, minZ + row);
        }

        char[][] draw(Function<Cell, Character> painter) {
            char[][] map = new char[size][size];
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    map[row][col] = painter.apply(cellAt(row, col));
                }
            }
            return map;
        }

        /** Клетки окна, чей символ на карте подходит под условие - в порядке строк. */
        Set<Cell> cellsWhere(char[][] map, java.util.function.Predicate<Character> test) {
            Set<Cell> cells = new LinkedHashSet<>();
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    if (test.test(map[row][col])) {
                        cells.add(cellAt(row, col));
                    }
                }
            }
            return cells;
        }
    }

    /** Содержимое плана, разложенное по клеткам - чтобы не перебирать списки для каждой клетки карты. */
    private record PlanCells(Map<Cell, WallPlan.Segment> segments, Set<Cell> fillers,
                             Set<Cell> towerBodies, Set<Cell> ladders) {

        static PlanCells of(VillageScan scan, WallPlan plan) {
            Map<Cell, WallPlan.Segment> segments = new HashMap<>();
            for (WallPlan.Segment segment : plan.segments()) {
                for (Cell cell : segment.cells()) {
                    segments.put(cell, segment);
                }
            }
            Set<Cell> fillers = new HashSet<>();
            for (WallPlan.Filler filler : plan.fillers()) {
                fillers.add(filler.cell());
            }
            Set<Cell> towerBodies = new HashSet<>();
            Set<Cell> ladders = new HashSet<>();
            for (WallPlan.Tower tower : plan.towers()) {
                towerBodies.addAll(TowerFootprint.bodyCells(scan, tower));
                ladders.add(TowerFootprint.ladderCell(scan, tower));
            }
            return new PlanCells(segments, fillers, towerBodies, ladders);
        }
    }

    /** Что в колонке мира от поставленной башни. */
    private enum TowerPart { NONE, BODY, LADDER, EMPTY }

    /** Символ каждой клетки на каждой из трёх карт (легенда - см. LEGEND). */
    private record MapPainter(ServerLevel level, VillageScan scan, WallPlan plan, PlanCells planCells,
                              Set<Block> wallBlocks) {

        char planChar(Cell cell) {
            if (planCells.towerBodies().contains(cell)) {
                return 'T';
            }
            if (planCells.ladders().contains(cell)) {
                return 'H';
            }
            WallPlan.Segment segment = planCells.segments().get(cell);
            if (segment != null) {
                return segment.entrance() ? 'E' : 'S';
            }
            if (planCells.fillers().contains(cell)) {
                return plan.debugFillerStage().getOrDefault(cell, '?');
            }
            if (plan.debugStrayRemoved().contains(cell)) {
                return 'r';
            }
            return background(cell);
        }

        char buildChar(Cell cell) {
            TowerPart part = towerPartAt(cell);
            if (part == TowerPart.BODY) {
                return 'T';
            }
            if (part == TowerPart.LADDER) {
                return 'H';
            }
            if (build.intactSegmentCells.contains(cell)) {
                return 'W';
            }
            if (build.bridgedByStrays.contains(cell)) {
                return 'B';
            }
            if (build.columnCells.contains(cell)) {
                return build.failedColumns.contains(cell) ? 'p' : 'c';
            }
            if (build.removedByStrays.contains(cell)) {
                return 'r';
            }
            if (build.rejectedColumns.contains(cell)) {
                return 'x';
            }
            if (part == TowerPart.EMPTY) {
                return 't';
            }
            if (build.degradedSegmentCells.contains(cell)) {
                return 'd';
            }
            return background(cell);
        }

        char worldChar(Cell cell) {
            TowerPart part = towerPartAt(cell);
            if (part == TowerPart.BODY) {
                return 'T';
            }
            if (part == TowerPart.LADDER) {
                return 'H';
            }
            // На isStructure стена не планируется - там дома деревни (доски/брёвна те же, что у стены),
            // в первом дампе они давали ложные "ОБРЫВ".
            if (!scan.inBounds(cell.x(), cell.z()) || scan.isStructure(cell.x(), cell.z())) {
                return background(cell);
            }
            int airBelow = airBelowWall(cell);
            if (airBelow < 0) {
                return background(cell);
            }
            return airBelow == 0 ? 'X' : Character.forDigit(airBelow, 10);
        }

        private char background(Cell cell) {
            int cx = cell.x();
            int cz = cell.z();
            if (!scan.inBounds(cx, cz)) {
                return ' ';
            }
            if (scan.isStructure(cx, cz)) {
                return '#';
            }
            if (scan.isWater(cx, cz)) {
                return 'w';
            }
            if (scan.isBoundary(cx, cz)) {
                return ':';
            }
            return scan.isTerritory(cx, cz) ? '.' : '~';
        }

        private TowerPart towerPartAt(Cell cell) {
            int x = scan.worldX(cell.x());
            int z = scan.worldZ(cell.z());
            for (BoundingBox box : build.towerBoxes.values()) {
                if (box == null || !box.isInside(x, box.minY(), z)) {
                    continue;
                }
                if (planCells.ladders().contains(cell)) {
                    return TowerPart.LADDER;
                }
                // Нижний ряд пропускаем - там BlockIgnoreProcessor.AIR мог оставить исходную траву
                // ровно на месте структурной пустоты: трава не air, но башни там нет.
                return hasBlockAbove(x, box.minY() + 1, box.maxY(), z) ? TowerPart.BODY : TowerPart.EMPTY;
            }
            return TowerPart.NONE;
        }

        private boolean hasBlockAbove(int x, int fromY, int toY, int z) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int y = fromY; y <= toY; y++) {
                if (!level.getBlockState(cursor.set(x, y, z)).isAir()) {
                    return true;
                }
            }
            return false;
        }

        /** -1 - в колонке нет блоков стены; иначе сколько блоков воздуха/травы под самым нижним из них. */
        private int airBelowWall(Cell cell) {
            int x = scan.worldX(cell.x());
            int z = scan.worldZ(cell.z());
            int surface = scan.surfaceYAt(cell.x(), cell.z());
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int y = surface - WORLD_PROBE_DOWN; y <= surface + WORLD_PROBE_UP; y++) {
                BlockState state = level.getBlockState(cursor.set(x, y, z));
                if (!state.isAir() && wallBlocks.contains(state.getBlock())) {
                    return countAirBelow(x, y, z);
                }
            }
            return -1;
        }

        private int countAirBelow(int x, int wallBottomY, int z) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            int air = 0;
            while (air < MAX_AIR_BELOW) {
                BlockState below = level.getBlockState(cursor.set(x, wallBottomY - 1 - air, z));
                if (!below.isAir() && !below.canBeReplaced()) {
                    break;
                }
                air++;
            }
            return air;
        }
    }

    /** Блоки, из которых бывают стена, столбики и башня - по ним WORLD-карта узнаёт "тут стена". */
    private static Set<Block> wallBlocks() {
        Set<Block> blocks = new HashSet<>(List.of(
                ModBlocks.OAK_SCRAP_WOOD.get(), ModBlocks.SPRUCE_SCRAP_WOOD.get(), FillerDecoration.ROPE_FENCE,
                Blocks.OAK_PLANKS, Blocks.OAK_SLAB, Blocks.OAK_STAIRS,
                Blocks.OAK_FENCE, Blocks.OAK_FENCE_GATE, Blocks.OAK_DOOR,
                Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.LADDER,
                Blocks.PACKED_MUD, Blocks.MUD, Blocks.MUD_BRICKS,
                Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.POLISHED_DIORITE, ModBlocks.SNOW_BRICKS.get(), ModBlocks.PACKED_ICE_BRICKS.get(),
                Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_FENCE,
                Blocks.SPRUCE_FENCE_GATE, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG));
        // ROPE_FENCE - из farmersdelight; если мода нет, реестр вернёт воздух.
        blocks.remove(Blocks.AIR);
        return blocks;
    }

    // --- вердикт по миру ---

    /** Связный (8-соседство) кусок реальной стены в окне. */
    private record WallPiece(int size, int distanceToTower, Cell closest) {

        boolean touchesTower() {
            return distanceToTower <= 1;
        }
    }

    private static String verdict(VillageScan scan, WallPlan.Tower tower, Window window, char[][] worldMap) {
        if (!build.towerBoxes.containsKey(tower)) {
            return "башни выключены (/calculate towers true), по миру не проверить";
        }
        if (build.towerBoxes.get(tower) == null) {
            return "башня не поставлена (не влезла)";
        }
        Set<Cell> towerBody = window.cellsWhere(worldMap, c -> c == 'T');
        List<WallPiece> pieces = findWallPieces(window.cellsWhere(worldMap, TowerJoinDebug::isWallChar), towerBody);
        long touching = pieces.stream().filter(WallPiece::touchesTower).count();

        StringBuilder verdict = new StringBuilder();
        verdict.append("кусков стены в окне ").append(pieces.size()).append(", касаются башни ").append(touching);
        if (touching < 2) {
            verdict.append(" (ОЖИДАЛОСЬ 2)");
        }
        for (WallPiece piece : pieces) {
            if (!piece.touchesTower() && piece.distanceToTower() <= DANGLING_RANGE) {
                verdict.append("; ОБРЫВ: ").append(piece.size()).append(" кл. в ").append(piece.distanceToTower())
                        .append(" бл. от башни, ближайшая клетка ").append(worldPos(scan, piece.closest()));
            }
        }
        return verdict.toString();
    }

    private static boolean isWallChar(char c) {
        return c == 'X' || Character.isDigit(c);
    }

    private static List<WallPiece> findWallPieces(Set<Cell> wallCells, Set<Cell> towerBody) {
        Set<Cell> unvisited = new LinkedHashSet<>(wallCells);
        List<WallPiece> pieces = new ArrayList<>();
        while (!unvisited.isEmpty()) {
            List<Cell> piece = floodFill(unvisited.iterator().next(), unvisited);
            pieces.add(describePiece(piece, towerBody));
        }
        return pieces;
    }

    /** Все клетки из unvisited, связанные со start по 8 соседям (найденные из unvisited удаляются). */
    private static List<Cell> floodFill(Cell start, Set<Cell> unvisited) {
        List<Cell> piece = new ArrayList<>();
        Deque<Cell> queue = new ArrayDeque<>();
        unvisited.remove(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Cell cell = queue.poll();
            piece.add(cell);
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    Cell next = new Cell(cell.x() + dx, cell.z() + dz);
                    if (unvisited.remove(next)) {
                        queue.add(next);
                    }
                }
            }
        }
        return piece;
    }

    private static WallPiece describePiece(List<Cell> piece, Set<Cell> towerBody) {
        Cell closest = piece.getFirst();
        int best = Integer.MAX_VALUE;
        for (Cell cell : piece) {
            for (Cell body : towerBody) {
                int distance = Math.max(Math.abs(cell.x() - body.x()), Math.abs(cell.z() - body.z()));
                if (distance < best) {
                    best = distance;
                    closest = cell;
                }
            }
        }
        return new WallPiece(piece.size(), best, closest);
    }

    // --- вывод карт в текст ---

    private static final String MAP_GAP = "     ";
    /** Ширина подписи строки: "%6d" (мировой Z) + маркер '>' + пробел. */
    private static final String ROW_LABEL_PAD = "        ";

    private static void appendMaps(StringBuilder out, VillageScan scan, Window window, char[][]... maps) {
        String[] titles = {"PLAN", "BUILD", "WORLD"};
        out.append(ROW_LABEL_PAD);
        for (String title : titles) {
            out.append(String.format("%-" + window.size() + "s", title)).append(MAP_GAP);
        }
        out.append('\n');
        for (String axisLine : xAxisLines(scan, window)) {
            out.append(ROW_LABEL_PAD).append((axisLine + MAP_GAP).repeat(maps.length)).append('\n');
        }
        for (int row = 0; row < window.size(); row++) {
            int worldZ = scan.worldZ(window.minZ() + row);
            out.append(String.format("%6d%s ", worldZ, row == WINDOW ? ">" : " "));
            for (char[][] map : maps) {
                out.append(map[row]).append(MAP_GAP);
            }
            out.append('\n');
        }
    }

    /** Три строки над каждой картой: десятки мирового X, единицы, 'v' над анкером. */
    private static List<String> xAxisLines(VillageScan scan, Window window) {
        StringBuilder tens = new StringBuilder();
        StringBuilder units = new StringBuilder();
        StringBuilder anchorMark = new StringBuilder();
        for (int col = 0; col < window.size(); col++) {
            int worldX = Math.abs(scan.worldX(window.minX() + col));
            tens.append(Character.forDigit(worldX / 10 % 10, 10));
            units.append(Character.forDigit(worldX % 10, 10));
            anchorMark.append(col == WINDOW ? 'v' : ' ');
        }
        return List.of(tens.toString(), units.toString(), anchorMark.toString());
    }
}
