package net.gera.veridia.common.village.calc;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Ямы, провалы и ущелья на рельефе скана - места, куда стена не должна спускаться: деревня на краю
 * обрыва или карстовой воронки иначе строила стену вниз по отвесным склонам, а башню над пропастью.
 * Работает только по карте высот земли (VillageScan.surfaceYAt), без чтения мира.
 * <p>
 * Клетка - яма, если выполняется одно из двух (оба измеряются в блоках, порог PIT_MIN_DEPTH):
 * <ol>
 *     <li><b>Замкнутая котловина.</b> Глубина до уровня, на котором воронка переливается наружу (как у озера:
 *     сколько воды в неё можно налить, пока она не польётся через край). Считается заливкой с приоритетом
 *     от краёв скана, поэтому находит воронки любой ширины.</li>
 *     <li><b>Ущелье.</b> Клетка ниже обоих краёв вдоль оси X или Z, каждый край - самая высокая земля в
 *     пределах PIT_ENCLOSE_RADIUS. Находит узкие овраги, у которых есть выход вниз по склону и потому
 *     "котловиной" они не считаются.</li>
 * </ol>
 * Вода не считается (озеро - отдельная история, см. WaterCell): мокрые клетки ямой не бывают.
 */
public final class TerrainHazards {

    private TerrainHazards() {
    }

    /** Клетки-ямы (без отступа от края): loaded - у каких клеток высота вообще измерена, wet - где вода. */
    public static boolean[] find(int[] surface, boolean[] loaded, boolean[] wet, int sizeX, int sizeZ) {
        int[] depression = depressionDepth(surface, loaded, sizeX, sizeZ);
        boolean[] pit = new boolean[surface.length];
        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                int i = cz * sizeX + cx;
                if (!loaded[i] || wet[i]) {
                    continue;
                }
                int depth = Math.max(depression[i], gorgeDepth(surface, loaded, sizeX, sizeZ, cx, cz));
                pit[i] = depth >= VillageCalcSettings.PIT_MIN_DEPTH;
            }
        }
        dropSmall(pit, sizeX, sizeZ, VillageCalcSettings.PIT_MIN_AREA);
        return pit;
    }

    /**
     * Убирает мелкие ямы: связная (по 8 соседям) группа клеток-ям площадью меньше minArea - обычная
     * воронка или вход в пещеру в 2-4 клетки. Они стене не мешают: кусок стены перекрывает их сверху, а
     * вырезать территорию вокруг каждой такой дырки значило бы изрезать всю деревню.
     */
    private static void dropSmall(boolean[] pit, int sizeX, int sizeZ, int minArea) {
        boolean[] seen = new boolean[pit.length];
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        java.util.List<Integer> group = new java.util.ArrayList<>();
        for (int start = 0; start < pit.length; start++) {
            if (!pit[start] || seen[start]) {
                continue;
            }
            group.clear();
            seen[start] = true;
            queue.add(start);
            while (!queue.isEmpty()) {
                int cell = queue.poll();
                group.add(cell);
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int x = cell % sizeX + dx;
                        int z = cell / sizeX + dz;
                        if (x < 0 || z < 0 || x >= sizeX || z >= sizeZ) {
                            continue;
                        }
                        int next = z * sizeX + x;
                        if (pit[next] && !seen[next]) {
                            seen[next] = true;
                            queue.add(next);
                        }
                    }
                }
            }
            if (group.size() < minArea) {
                for (int cell : group) {
                    pit[cell] = false;
                }
            }
        }
    }

    // --- замкнутая котловина: заливка с приоритетом ---

    /**
     * Для каждой клетки - на сколько её нужно "залить", чтобы вода пошла за край: уровень перелива минус
     * высота земли. Начинаем с клеток, откуда вода уходит за карту (край скана или соседство с
     * непрогруженной клеткой), и идём внутрь от самых низких: уровень перелива клетки - большее из её
     * собственной высоты и уровня того, откуда пришли.
     */
    private static int[] depressionDepth(int[] surface, boolean[] loaded, int sizeX, int sizeZ) {
        int[] level = new int[surface.length];
        Arrays.fill(level, Integer.MAX_VALUE);
        // Ключ - уровень в старших битах и клетка в младших: неизменяемый, так что порядок очереди не ломается.
        PriorityQueue<Long> queue = new PriorityQueue<>();
        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                int i = cz * sizeX + cx;
                if (loaded[i] && isOutlet(loaded, sizeX, sizeZ, cx, cz)) {
                    level[i] = surface[i];
                    queue.add(key(level[i], i));
                }
            }
        }
        boolean[] done = new boolean[surface.length];
        while (!queue.isEmpty()) {
            int i = indexOf(queue.poll());
            if (done[i]) {
                continue;
            }
            done[i] = true;
            spill(surface, loaded, level, queue, sizeX, sizeZ, i, i % sizeX - 1, i / sizeX);
            spill(surface, loaded, level, queue, sizeX, sizeZ, i, i % sizeX + 1, i / sizeX);
            spill(surface, loaded, level, queue, sizeX, sizeZ, i, i % sizeX, i / sizeX - 1);
            spill(surface, loaded, level, queue, sizeX, sizeZ, i, i % sizeX, i / sizeX + 1);
        }
        int[] depth = new int[surface.length];
        for (int i = 0; i < depth.length; i++) {
            depth[i] = level[i] == Integer.MAX_VALUE ? 0 : level[i] - surface[i];
        }
        return depth;
    }

    private static void spill(int[] surface, boolean[] loaded, int[] level, PriorityQueue<Long> queue,
                              int sizeX, int sizeZ, int from, int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= sizeX || cz >= sizeZ) {
            return;
        }
        int next = cz * sizeX + cx;
        int spillLevel = Math.max(surface[next], level[from]);
        if (loaded[next] && spillLevel < level[next]) {
            level[next] = spillLevel;
            queue.add(key(spillLevel, next));
        }
    }

    private static boolean isOutlet(boolean[] loaded, int sizeX, int sizeZ, int cx, int cz) {
        return cx == 0 || cz == 0 || cx == sizeX - 1 || cz == sizeZ - 1
                || !loaded[cz * sizeX + cx - 1] || !loaded[cz * sizeX + cx + 1]
                || !loaded[(cz - 1) * sizeX + cx] || !loaded[(cz + 1) * sizeX + cx];
    }

    private static long key(int level, int index) {
        // Смещение: высоты бывают отрицательными, а порядок очереди - по возрастанию ключа.
        return ((long) (level + (1 << 20)) << 32) | index;
    }

    private static int indexOf(long key) {
        return (int) (key & 0xffffffffL);
    }

    // --- ущелье: ниже обоих краёв вдоль оси ---

    private static int gorgeDepth(int[] surface, boolean[] loaded, int sizeX, int sizeZ, int cx, int cz) {
        int here = surface[cz * sizeX + cx];
        int alongX = Math.min(ridge(surface, loaded, sizeX, sizeZ, cx, cz, -1, 0),
                ridge(surface, loaded, sizeX, sizeZ, cx, cz, 1, 0)) - here;
        int alongZ = Math.min(ridge(surface, loaded, sizeX, sizeZ, cx, cz, 0, -1),
                ridge(surface, loaded, sizeX, sizeZ, cx, cz, 0, 1)) - here;
        return Math.max(alongX, alongZ);
    }

    /** Самая высокая земля в пределах PIT_ENCLOSE_RADIUS в одну сторону; если с этой стороны ничего
     * не измерено - очень низкое значение, и ущельем клетка по этой оси не считается. */
    private static int ridge(int[] surface, boolean[] loaded, int sizeX, int sizeZ,
                             int cx, int cz, int stepX, int stepZ) {
        int highest = Integer.MIN_VALUE / 2;
        for (int step = 1; step <= VillageCalcSettings.PIT_ENCLOSE_RADIUS; step++) {
            int x = cx + stepX * step;
            int z = cz + stepZ * step;
            if (x < 0 || z < 0 || x >= sizeX || z >= sizeZ || !loaded[z * sizeX + x]) {
                break;
            }
            highest = Math.max(highest, surface[z * sizeX + x]);
        }
        return highest;
    }
}
