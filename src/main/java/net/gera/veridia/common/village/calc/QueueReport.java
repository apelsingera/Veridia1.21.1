package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Записывающий QueueTrace: собирает решения одной сборки очереди и превращает их в текст для чата
 * ({@link #summary}) и для файла ({@link #details}). Ничего не меняет в мире.
 */
public final class QueueReport implements QueueTrace {

    /** Сколько примеров каждого вида отказа попадает в подробный файл. */
    private static final int DETAIL_SAMPLES = 60;
    /** Сколько самых частых причин отказа показывать в чате. */
    private static final int TOP_REASONS = 6;

    private record SegmentCase(WallPlan.Segment segment, SegmentVerdict verdict, Obstacle obstacle) {}

    private record ColumnCase(int cellX, int cellZ, Obstacle obstacle) {}

    private record OverlapCase(WallPlan.Filler filler, BlockPos overlap) {}

    private record ShadowCase(NailerWallQueue.Entry loser, NailerWallQueue.Entry winner) {}

    private record ComponentCase(int size, boolean kept, BoundingBox box) {}

    private record TowerCase(WallPlan.Tower spot, Obstacle obstacle) {}

    private final VillageScan scan;
    private final Map<SegmentVerdict, Integer> segmentCounts = new EnumMap<>(SegmentVerdict.class);
    private final List<SegmentCase> brokenSegments = new ArrayList<>();
    private final Map<ColumnVerdict, Integer> columnCounts = new EnumMap<>(ColumnVerdict.class);
    private final List<ColumnCase> rejectedColumns = new ArrayList<>();
    private final List<OverlapCase> overlaps = new ArrayList<>();
    private final List<ShadowCase> shadows = new ArrayList<>();
    private final List<ComponentCase> components = new ArrayList<>();
    private final List<TowerCase> towers = new ArrayList<>();
    private int strayRemoved;
    private int strayBridged;
    private int strayBridgesRejected;
    private boolean strayPassRan;

    public QueueReport(VillageScan scan) {
        this.scan = scan;
    }

    // Для карты плана (см. planMap): какие клетки сетки скана чем заняты.
    private final Set<Cell> wholeSegmentCells = new HashSet<>();
    private final Set<Cell> brokenSegmentCells = new HashSet<>();
    private final Set<Cell> columnCells = new HashSet<>();
    private final Set<Cell> rejectedCells = new HashSet<>();

    // --- QueueTrace ---

    @Override
    public void segment(WallPlan.Segment segment, SegmentVerdict verdict, Obstacle obstacle) {
        segmentCounts.merge(verdict, 1, Integer::sum);
        (verdict == SegmentVerdict.WHOLE ? wholeSegmentCells : brokenSegmentCells).addAll(segment.cells());
        if (verdict != SegmentVerdict.WHOLE) {
            brokenSegments.add(new SegmentCase(segment, verdict, obstacle));
        }
    }

    @Override
    public void column(int cellX, int cellZ, ColumnVerdict verdict, Obstacle obstacle) {
        columnCounts.merge(verdict, 1, Integer::sum);
        if (verdict == ColumnVerdict.REJECTED) {
            rejectedColumns.add(new ColumnCase(cellX, cellZ, obstacle));
            rejectedCells.add(new Cell(cellX, cellZ));
        } else {
            columnCells.add(new Cell(cellX, cellZ));
        }
    }

    @Override
    public void columnOverlapsPiece(WallPlan.Filler filler, BlockPos overlap) {
        overlaps.add(new OverlapCase(filler, overlap));
    }

    @Override
    public void shadowed(NailerWallQueue.Entry loser, NailerWallQueue.Entry winner) {
        shadows.add(new ShadowCase(loser, winner));
    }

    @Override
    public void component(List<NailerWallQueue.Entry> entries, boolean kept) {
        BoundingBox box = BoundingBox.encapsulatingPositions(entries.stream().map(NailerWallQueue.Entry::pos).toList()).orElseThrow();
        components.add(new ComponentCase(entries.size(), kept, box));
    }

    @Override
    public void tower(WallPlan.Tower spot, Obstacle obstacle) {
        towers.add(new TowerCase(spot, obstacle));
    }

    @Override
    public void strays(int removed, int bridged, int bridgesRejected) {
        strayPassRan = true;
        strayRemoved = removed;
        strayBridged = bridged;
        strayBridgesRejected = bridgesRejected;
    }

    // --- чат ---

    public List<String> summary() {
        List<String> lines = new ArrayList<>();
        lines.add("Куски плана: целиком " + count(segmentCounts, SegmentVerdict.WHOLE)
                + ", развалились на столбики из-за неровности " + count(segmentCounts, SegmentVerdict.TOO_UNEVEN)
                + ", из-за помехи " + count(segmentCounts, SegmentVerdict.BLOCKED));
        addTopReasons(lines, "  помехи кусков: ", brokenSegments.stream()
                .filter(c -> c.verdict() == SegmentVerdict.BLOCKED).map(SegmentCase::obstacle).toList());
        lines.add("Столбики: поставятся " + count(columnCounts, ColumnVerdict.ADDED)
                + ", отброшено (помеха) " + count(columnCounts, ColumnVerdict.REJECTED)
                + ", отброшено (задевают кусок или башню) " + overlaps.size());
        addTopReasons(lines, "  помехи столбиков: ", rejectedColumns.stream().map(ColumnCase::obstacle).toList());
        lines.add(strayPassRan
                ? "Второй dropStrays (как в /calculate): убрано обрывков " + strayRemoved + ", мостов добавлено " + strayBridged
                        + (strayBridgesRejected > 0 ? " (из них мир не пустил " + strayBridgesRejected + ")" : "")
                : "Второй dropStrays выключен (SECOND_DROP_STRAYS)");
        lines.add("Записей-дублей на одну клетку с разными блоками (проиграли и выброшены): " + shadows.size());
        lines.add("Башни: кандидатов " + towers.size() + ", встанут " + towers.stream().filter(t -> t.obstacle() == null).count()
                + (towers.stream().anyMatch(t -> t.obstacle() != null) ? " (отказы - в файле)" : ""));
        lines.add(componentsLine());
        return lines;
    }

    private String componentsLine() {
        long kept = components.stream().filter(ComponentCase::kept).count();
        List<ComponentCase> dropped = components.stream().filter(c -> !c.kept()).toList();
        String sizes = components.stream().filter(ComponentCase::kept).map(c -> String.valueOf(c.size()))
                .limit(8).reduce((a, b) -> a + ", " + b).orElse("-");
        return "Связных масс в очереди: " + kept + " (размеры блоков: " + sizes + (kept > 8 ? ", ..." : "")
                + "), выброшено мелких (< MIN_COMPONENT_SIZE) " + dropped.size()
                + (dropped.isEmpty() ? "" : " на " + dropped.stream().mapToInt(ComponentCase::size).sum() + " блоков (координаты - в файле)");
    }

    private void addTopReasons(List<String> lines, String prefix, List<Obstacle> obstacles) {
        if (obstacles.isEmpty()) {
            return;
        }
        Map<String, Integer> byReason = new TreeMap<>();
        for (Obstacle obstacle : obstacles) {
            byReason.merge(obstacle.why() + " (" + blockId(obstacle.state()) + ")", 1, Integer::sum);
        }
        byReason.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(TOP_REASONS)
                .forEach(entry -> lines.add(prefix + entry.getValue() + " x " + entry.getKey()));
    }

    // --- файл ---

    public List<String> details() {
        List<String> lines = new ArrayList<>();
        lines.add("=== Куски стены, не вставшие целиком (" + brokenSegments.size() + ") ===");
        brokenSegments.stream().limit(DETAIL_SAMPLES).forEach(c -> lines.add("  кусок " + at(c.segment().cellX(), c.segment().cellZ())
                + " " + c.segment().along() + (c.segment().entrance() ? " (вход)" : "") + ": " + c.verdict()
                + (c.obstacle() == null ? "" : " - " + describe(c.obstacle()))));
        lines.add("=== Столбики, отброшенные живым миром (" + rejectedColumns.size() + ") ===");
        rejectedColumns.stream().limit(DETAIL_SAMPLES).forEach(c -> lines.add("  столбик " + at(c.cellX(), c.cellZ()) + ": " + describe(c.obstacle())));
        lines.add("=== Столбики, задевающие кусок/башню (" + overlaps.size() + ") ===");
        overlaps.stream().limit(DETAIL_SAMPLES).forEach(c -> lines.add("  столбик " + at(c.filler().cellX(), c.filler().cellZ())
                + " пересекается в " + xyz(c.overlap())));
        lines.add("=== Споры за клетку (" + shadows.size() + ") ===");
        shadows.stream().limit(DETAIL_SAMPLES).forEach(c -> lines.add("  " + xyz(c.loser().pos()) + ": выброшен "
                + blockId(c.loser().state()) + ", остался " + blockId(c.winner().state())));
        lines.add("=== Башни ===");
        for (TowerCase tower : towers) {
            lines.add("  башня " + at(tower.spot().cellX(), tower.spot().cellZ()) + " " + tower.spot().outward() + ": "
                    + (tower.obstacle() == null ? "встанет" : "НЕ встанет - " + describe(tower.obstacle())));
        }
        lines.add("=== Связные массы ===");
        for (ComponentCase component : components) {
            lines.add("  " + (component.kept() ? "в очереди" : "ВЫБРОШЕНА") + ": " + component.size() + " блоков, "
                    + "x " + component.box().minX() + ".." + component.box().maxX()
                    + " y " + component.box().minY() + ".." + component.box().maxY()
                    + " z " + component.box().minZ() + ".." + component.box().maxZ());
        }
        return lines;
    }

    // --- карта плана ---

    private static final String MAP_LEGEND = """
            Карта плана (одна клетка = один блок по XZ, сверху север, слева запад):
              S кусок стены встанет целиком     c столбик встанет      x столбик ОТБРОШЕН (помеха - в списке выше)
              u кусок развалился (неровно/помеха), столбиков на этих клетках нет     # постройка (isStructure)
              z зона ямы (вырезана из территории)   ~ глубокая вода   : территория внутри стены   . вне территории
              В центре '+' - точка, вокруг которой окно.""";

    /** Окно (2*half+1) клеток вокруг мировой точки (centerX, centerZ): что план и скан решили для каждой клетки. */
    public List<String> planMap(int centerX, int centerZ, int half) {
        List<String> lines = new ArrayList<>();
        lines.addAll(MAP_LEGEND.lines().toList());
        lines.add("окно x " + (centerX - half) + ".." + (centerX + half) + ", z " + (centerZ - half) + ".." + (centerZ + half));
        for (int worldZ = centerZ - half; worldZ <= centerZ + half; worldZ++) {
            StringBuilder row = new StringBuilder(String.format("%6d ", worldZ));
            for (int worldX = centerX - half; worldX <= centerX + half; worldX++) {
                row.append(worldX == centerX && worldZ == centerZ ? '+' : symbolAt(worldX - scan.minX(), worldZ - scan.minZ()));
            }
            lines.add(row.toString());
        }
        return lines;
    }

    private char symbolAt(int cellX, int cellZ) {
        Cell cell = new Cell(cellX, cellZ);
        if (!scan.inBounds(cellX, cellZ)) return ' ';
        if (rejectedCells.contains(cell)) return 'x';
        if (wholeSegmentCells.contains(cell)) return 'S';
        if (columnCells.contains(cell)) return 'c';
        if (brokenSegmentCells.contains(cell)) return 'u';
        if (scan.isStructure(cellX, cellZ)) return '#';
        if (scan.isPitZone(cellX, cellZ)) return 'z';
        if (scan.isWet(cellX, cellZ)) return '~';
        return scan.isTerritory(cellX, cellZ) ? ':' : '.';
    }

    // --- форматирование ---

    private static int count(Map<?, Integer> counts, Object key) {
        return counts.getOrDefault(key, 0);
    }

    private String at(int cellX, int cellZ) {
        return scan.worldX(cellX) + "," + scan.worldZ(cellZ);
    }

    private static String xyz(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static String describe(Obstacle obstacle) {
        return obstacle.why() + " [" + blockId(obstacle.state()) + " в " + xyz(obstacle.pos()) + "]";
    }
}
