package net.gera.veridia.common.village.calc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class TerritoryShaper {

    private TerritoryShaper() {
    }
    public static void closeGaps(boolean[] cells, int sizeX, int sizeZ, int maxGap) {
        if (maxGap <= 0) {
            return;
        }
        boolean[] filled = cells.clone();
        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                if (cells[cz * sizeX + cx]) {
                    continue;
                }
                boolean horizontal = reaches(cells, sizeX, sizeZ, cx, cz, -1, 0, maxGap)
                        && reaches(cells, sizeX, sizeZ, cx, cz, 1, 0, maxGap);
                boolean vertical = reaches(cells, sizeX, sizeZ, cx, cz, 0, -1, maxGap)
                        && reaches(cells, sizeX, sizeZ, cx, cz, 0, 1, maxGap);
                if (horizontal || vertical) {
                    filled[cz * sizeX + cx] = true;
                }
            }
        }
        System.arraycopy(filled, 0, cells, 0, cells.length);
    }

    private static boolean reaches(boolean[] cells, int sizeX, int sizeZ,
                                   int cx, int cz, int stepX, int stepZ, int maxGap) {
        for (int step = 1; step <= maxGap; step++) {
            int x = cx + stepX * step;
            int z = cz + stepZ * step;
            if (x < 0 || z < 0 || x >= sizeX || z >= sizeZ) {
                return false;
            }
            if (cells[z * sizeX + x]) {
                return true;
            }
        }
        return false;
    }
    public static void bridgeWater(boolean[] cells, boolean[] water, int sizeX, int sizeZ, int maxGap) {
        if (maxGap <= 0) {
            return;
        }
        boolean[] filled = cells.clone();
        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                int index = cz * sizeX + cx;
                if (cells[index] || !water[index]) {
                    continue;
                }
                boolean horizontal = reaches(cells, sizeX, sizeZ, cx, cz, -1, 0, maxGap)
                        && reaches(cells, sizeX, sizeZ, cx, cz, 1, 0, maxGap);
                boolean vertical = reaches(cells, sizeX, sizeZ, cx, cz, 0, -1, maxGap)
                        && reaches(cells, sizeX, sizeZ, cx, cz, 0, 1, maxGap);
                if (horizontal || vertical) {
                    filled[index] = true;
                }
            }
        }
        System.arraycopy(filled, 0, cells, 0, cells.length);
    }

    public static void fillEnclosed(boolean[] cells, int sizeX, int sizeZ) {
        fillEnclosedBy(cells, cells, sizeX, sizeZ);
    }

    public static boolean fillEnclosedBy(boolean[] target, boolean[] blocking, int sizeX, int sizeZ) {
        boolean[] outside = new boolean[target.length];
        Deque<Integer> queue = new ArrayDeque<>();

        for (int cx = 0; cx < sizeX; cx++) {
            seed(blocking, outside, queue, sizeX, sizeZ, cx, 0);
            seed(blocking, outside, queue, sizeX, sizeZ, cx, sizeZ - 1);
        }
        for (int cz = 0; cz < sizeZ; cz++) {
            seed(blocking, outside, queue, sizeX, sizeZ, 0, cz);
            seed(blocking, outside, queue, sizeX, sizeZ, sizeX - 1, cz);
        }

        while (!queue.isEmpty()) {
            int index = queue.poll();
            int cx = index % sizeX;
            int cz = index / sizeX;
            seed(blocking, outside, queue, sizeX, sizeZ, cx - 1, cz);
            seed(blocking, outside, queue, sizeX, sizeZ, cx + 1, cz);
            seed(blocking, outside, queue, sizeX, sizeZ, cx, cz - 1);
            seed(blocking, outside, queue, sizeX, sizeZ, cx, cz + 1);
        }

        boolean changed = false;
        for (int i = 0; i < target.length; i++) {
            if (!target[i] && !blocking[i] && !outside[i]) {
                target[i] = true;
                changed = true;
            }
        }
        return changed;
    }

    private static void seed(boolean[] blocking, boolean[] outside, Deque<Integer> queue,
                             int sizeX, int sizeZ, int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= sizeX || cz >= sizeZ) {
            return;
        }
        int index = cz * sizeX + cx;
        if (blocking[index] || outside[index]) {
            return;
        }
        outside[index] = true;
        queue.add(index);
    }

    public static void smooth(boolean[] cells, int sizeX, int sizeZ, int passes) {
        for (int pass = 0; pass < passes; pass++) {
            boolean[] next = cells.clone();
            for (int cz = 0; cz < sizeZ; cz++) {
                for (int cx = 0; cx < sizeX; cx++) {
                    int index = cz * sizeX + cx;
                    int neighbours = neighbour(cells, sizeX, sizeZ, cx - 1, cz)
                            + neighbour(cells, sizeX, sizeZ, cx + 1, cz)
                            + neighbour(cells, sizeX, sizeZ, cx, cz - 1)
                            + neighbour(cells, sizeX, sizeZ, cx, cz + 1);
                    if (cells[index]) {
                        if (neighbours <= 1) {
                            next[index] = false;
                        }
                    } else if (neighbours >= 3) {
                        next[index] = true;
                    }
                }
            }
            System.arraycopy(next, 0, cells, 0, cells.length);
        }
    }

    private static int neighbour(boolean[] cells, int sizeX, int sizeZ, int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= sizeX || cz >= sizeZ) {
            return 0;
        }
        return cells[cz * sizeX + cx] ? 1 : 0;
    }

    public static void keepSeeded(boolean[] cells, int sizeX, int sizeZ, boolean[] seeds) {
        boolean[] reached = new boolean[cells.length];
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < cells.length; i++) {
            if (seeds[i] && cells[i]) {
                reached[i] = true;
                queue.add(i);
            }
        }
        while (!queue.isEmpty()) {
            int index = queue.poll();
            int cx = index % sizeX;
            int cz = index / sizeX;
            visit(cells, reached, queue, sizeX, sizeZ, cx - 1, cz);
            visit(cells, reached, queue, sizeX, sizeZ, cx + 1, cz);
            visit(cells, reached, queue, sizeX, sizeZ, cx, cz - 1);
            visit(cells, reached, queue, sizeX, sizeZ, cx, cz + 1);
        }
        System.arraycopy(reached, 0, cells, 0, cells.length);
    }

    public static boolean anyTrue(boolean[] cells) {
        for (boolean cell : cells) {
            if (cell) {
                return true;
            }
        }
        return false;
    }

    public static void keepMainComponent(boolean[] cells, int sizeX, int sizeZ, int startX, int startZ) {
        if (startX >= 0 && startZ >= 0 && startX < sizeX && startZ < sizeZ
                && cells[startZ * sizeX + startX]) {
            boolean[] keep = component(cells, sizeX, sizeZ, startZ * sizeX + startX);
            System.arraycopy(keep, 0, cells, 0, cells.length);
            return;
        }
        boolean[] seen = new boolean[cells.length];
        boolean[] best = null;
        int bestSize = 0;
        for (int i = 0; i < cells.length; i++) {
            if (!cells[i] || seen[i]) {
                continue;
            }
            boolean[] candidate = component(cells, sizeX, sizeZ, i);
            int size = 0;
            for (int j = 0; j < candidate.length; j++) {
                if (candidate[j]) {
                    seen[j] = true;
                    size++;
                }
            }
            if (size > bestSize) {
                bestSize = size;
                best = candidate;
            }
        }
        if (best != null) {
            System.arraycopy(best, 0, cells, 0, cells.length);
        }
    }

    private static boolean[] component(boolean[] cells, int sizeX, int sizeZ, int start) {
        boolean[] visited = new boolean[cells.length];
        Deque<Integer> queue = new ArrayDeque<>();
        visited[start] = true;
        queue.add(start);
        while (!queue.isEmpty()) {
            int index = queue.poll();
            int cx = index % sizeX;
            int cz = index / sizeX;
            visit(cells, visited, queue, sizeX, sizeZ, cx - 1, cz);
            visit(cells, visited, queue, sizeX, sizeZ, cx + 1, cz);
            visit(cells, visited, queue, sizeX, sizeZ, cx, cz - 1);
            visit(cells, visited, queue, sizeX, sizeZ, cx, cz + 1);
        }
        return visited;
    }

    private static void visit(boolean[] cells, boolean[] visited, Deque<Integer> queue,
                              int sizeX, int sizeZ, int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= sizeX || cz >= sizeZ) {
            return;
        }
        int index = cz * sizeX + cx;
        if (visited[index] || !cells[index]) {
            return;
        }
        visited[index] = true;
        queue.add(index);
    }
    public static List<Integer> findBreaches(boolean[] passable, boolean[] territory,
                                             int sizeX, int sizeZ) {
        List<Integer> breaches = new ArrayList<>();
        boolean[] visited = new boolean[passable.length];
        Deque<Integer> queue = new ArrayDeque<>();

        for (int cx = 0; cx < sizeX; cx++) {
            seedBreach(passable, territory, visited, queue, sizeX, sizeZ, cx, 0);
            seedBreach(passable, territory, visited, queue, sizeX, sizeZ, cx, sizeZ - 1);
        }
        for (int cz = 0; cz < sizeZ; cz++) {
            seedBreach(passable, territory, visited, queue, sizeX, sizeZ, 0, cz);
            seedBreach(passable, territory, visited, queue, sizeX, sizeZ, sizeX - 1, cz);
        }

        while (!queue.isEmpty()) {
            int index = queue.poll();
            int cx = index % sizeX;
            int cz = index / sizeX;
            expandBreach(passable, territory, visited, queue, breaches, sizeX, sizeZ, cx - 1, cz);
            expandBreach(passable, territory, visited, queue, breaches, sizeX, sizeZ, cx + 1, cz);
            expandBreach(passable, territory, visited, queue, breaches, sizeX, sizeZ, cx, cz - 1);
            expandBreach(passable, territory, visited, queue, breaches, sizeX, sizeZ, cx, cz + 1);
        }
        return breaches;
    }
    private static void seedBreach(boolean[] passable, boolean[] territory, boolean[] visited,
                                   Deque<Integer> queue, int sizeX, int sizeZ, int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= sizeX || cz >= sizeZ) {
            return;
        }
        int index = cz * sizeX + cx;
        if (!passable[index] || territory[index] || visited[index]) {
            return;
        }
        visited[index] = true;
        queue.add(index);
    }

    private static void expandBreach(boolean[] passable, boolean[] territory, boolean[] visited,
                                     Deque<Integer> queue, List<Integer> breaches,
                                     int sizeX, int sizeZ, int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= sizeX || cz >= sizeZ) {
            return;
        }
        int index = cz * sizeX + cx;
        if (visited[index] || !passable[index]) {
            return;
        }
        visited[index] = true;
        if (territory[index]) {
            breaches.add(index);
            return;
        }
        queue.add(index);
    }

    public static boolean[] erode(boolean[] cells, int sizeX, int sizeZ, int radius) {
        boolean[] byX = new boolean[cells.length];
        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                int from = cx - radius;
                int to = cx + radius;
                if (from < 0 || to >= sizeX) {
                    continue;
                }
                boolean allTrue = true;
                for (int x = from; x <= to && allTrue; x++) {
                    allTrue = cells[cz * sizeX + x];
                }
                byX[cz * sizeX + cx] = allTrue;
            }
        }
        boolean[] result = new boolean[cells.length];
        for (int cx = 0; cx < sizeX; cx++) {
            for (int cz = 0; cz < sizeZ; cz++) {
                int from = cz - radius;
                int to = cz + radius;
                if (from < 0 || to >= sizeZ) {
                    continue;
                }
                boolean allTrue = true;
                for (int z = from; z <= to && allTrue; z++) {
                    allTrue = byX[z * sizeX + cx];
                }
                result[cz * sizeX + cx] = allTrue;
            }
        }
        return result;
    }

    /**
     * Засыпает заливы: всё, что морфологическое закрытие (расширить на radius, потом сузить на
     * radius) добавляет к territory, становится territory - то есть любая вогнутость контура уже
     * 2*radius+1 клеток. Клетки из neverAdd (глубокая вода) не добавляются никогда: стена не должна
     * пойти поперёк озера - там контур так и останется по берегу.
     */
    public static void fillBays(boolean[] cells, boolean[] neverAdd, int sizeX, int sizeZ, int radius) {
        if (radius <= 0) {
            return;
        }
        boolean[] closed = erode(dilate(cells, sizeX, sizeZ, radius), sizeX, sizeZ, radius);
        for (int i = 0; i < cells.length; i++) {
            if (closed[i] && !neverAdd[i]) {
                cells[i] = true;
            }
        }
    }

    public static void open(boolean[] cells, int sizeX, int sizeZ, int radius) {
        if (radius <= 0) {
            return;
        }
        boolean[] eroded = erode(cells, sizeX, sizeZ, radius);
        boolean[] opened = dilate(eroded, sizeX, sizeZ, radius);
        System.arraycopy(opened, 0, cells, 0, cells.length);
    }

    public static boolean[] dilate(boolean[] cells, int sizeX, int sizeZ, int radius) {
        boolean[] byX = new boolean[cells.length];
        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                if (!cells[cz * sizeX + cx]) {
                    continue;
                }
                int from = Math.max(0, cx - radius);
                int to = Math.min(sizeX - 1, cx + radius);
                for (int x = from; x <= to; x++) {
                    byX[cz * sizeX + x] = true;
                }
            }
        }
        boolean[] result = new boolean[cells.length];
        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                if (!byX[cz * sizeX + cx]) {
                    continue;
                }
                int from = Math.max(0, cz - radius);
                int to = Math.min(sizeZ - 1, cz + radius);
                for (int z = from; z <= to; z++) {
                    result[z * sizeX + cx] = true;
                }
            }
        }
        return result;
    }
}