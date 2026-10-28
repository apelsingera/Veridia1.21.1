package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Дорисовка тропинок у нового дома (по просьбе игрока: "так как идеально спавнить не выйдет - алгоритм,
 * который дорисовывает дорожки"). Рядом с домом тропинки деревни могут оказаться разорваны (дом с
 * откосом встал между кусками) или дом стоит в стороне от них. Тут куски тропинок рядом с домом и сам
 * дом (его дорожка вокруг) соединяются кратчайшими новыми тропинками - минимальным остовным деревом,
 * как мосты стены в WallPlan.dropStrays: ровно столько новых тропинок, сколько нужно, чтобы всё стало
 * связным, и не длиннее HOUSE_PATH_MAX_LENGTH (дальние куски не тянем).
 * <p>
 * Работает по земле ПОСЛЕ земляных работ (HouseEarthworks.FinishedGround) - так nailer может посчитать
 * тропинки заранее, пока площадка ещё не готова.
 */
public final class HousePaths {

    private HousePaths() {
    }

    private static final BlockState PATH = Blocks.DIRT_PATH.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** Кратчайшая новая тропинка между двумя кусками: сколько блоков тропинки добавить и где. */
    private record Route(int from, int to, List<Cell> newCells) {}

    public static List<HouseEarthworks.Change> plan(ServerLevel level, VillageScan scan, HouseEarthworks.FinishedGround ground) {
        Set<Cell> existing = existingPaths(level, scan, ground);
        List<Set<Cell>> pieces = connectedPieces(existing);
        pieces.add(ground.walkway());

        List<Route> routes = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            routes.addAll(shortestRoutesFrom(i, pieces, ground, existing));
        }
        routes.sort(Comparator.comparingInt(route -> route.newCells().size()));

        int[] parent = new int[pieces.size()];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }
        List<HouseEarthworks.Change> plants = new ArrayList<>();
        List<HouseEarthworks.Change> paths = new ArrayList<>();
        Set<Cell> drawn = new HashSet<>();
        for (Route route : routes) {
            int rootFrom = root(parent, route.from());
            int rootTo = root(parent, route.to());
            if (rootFrom == rootTo) {
                continue;
            }
            parent[rootFrom] = rootTo;
            for (Cell cell : route.newCells()) {
                if (drawn.add(cell)) {
                    addPathBlock(level, scan, cell, ground.walkableTops().get(cell), plants, paths);
                }
            }
        }
        List<HouseEarthworks.Change> ordered = new ArrayList<>(plants);
        ordered.addAll(paths);
        return ordered;
    }

    /** Колонки вокруг дома, где тропинка уже есть: была в деревне (скан) или уже лежит на итоговой земле. */
    private static Set<Cell> existingPaths(ServerLevel level, VillageScan scan, HouseEarthworks.FinishedGround ground) {
        Set<Cell> paths = new LinkedHashSet<>();
        for (Map.Entry<Cell, Integer> column : ground.walkableTops().entrySet()) {
            Cell cell = column.getKey();
            BlockPos top = new BlockPos(scan.worldX(cell.x()), column.getValue(), scan.worldZ(cell.z()));
            if (scan.isPathBlock(cell.x(), cell.z()) || level.getBlockState(top).is(Blocks.DIRT_PATH)) {
                paths.add(cell);
            }
        }
        return paths;
    }

    /** Куски тропинок, связные по сторонам (4 соседа). */
    private static List<Set<Cell>> connectedPieces(Set<Cell> cells) {
        Set<Cell> unvisited = new LinkedHashSet<>(cells);
        List<Set<Cell>> pieces = new ArrayList<>();
        while (!unvisited.isEmpty()) {
            Cell start = unvisited.iterator().next();
            Set<Cell> piece = new HashSet<>();
            Deque<Cell> queue = new ArrayDeque<>();
            unvisited.remove(start);
            queue.add(start);
            while (!queue.isEmpty()) {
                Cell cell = queue.poll();
                piece.add(cell);
                for (Cell next : sideNeighbours(cell)) {
                    if (unvisited.remove(next)) {
                        queue.add(next);
                    }
                }
            }
            pieces.add(piece);
        }
        return pieces;
    }

    /**
     * Кратчайшие маршруты от куска fromIndex до каждого другого куска (поиск в ширину по колонкам, где
     * можно ходить, шаг - только на соседа не круче блока). Длина маршрута - сколько блоков тропинки
     * придётся ДОБАВИТЬ: по уже лежащей тропинке идти бесплатно.
     */
    private static List<Route> shortestRoutesFrom(int fromIndex, List<Set<Cell>> pieces,
                                                  HouseEarthworks.FinishedGround ground, Set<Cell> existing) {
        Map<Cell, Integer> pieceOf = new HashMap<>();
        for (int i = 0; i < pieces.size(); i++) {
            for (Cell cell : pieces.get(i)) {
                pieceOf.putIfAbsent(cell, i);
            }
        }
        Map<Cell, Integer> cost = new HashMap<>();
        Map<Cell, Cell> cameFrom = new HashMap<>();
        Deque<Cell> queue = new ArrayDeque<>();
        for (Cell start : pieces.get(fromIndex)) {
            cost.put(start, 0);
            queue.add(start);
        }
        Map<Integer, Cell> reachedAt = new HashMap<>();
        while (!queue.isEmpty()) {
            Cell cell = queue.pollFirst();
            int here = cost.get(cell);
            if (here > VillageCalcSettings.HOUSE_PATH_MAX_LENGTH) {
                continue;
            }
            Integer piece = pieceOf.get(cell);
            if (piece != null && piece != fromIndex && !reachedAt.containsKey(piece)) {
                reachedAt.put(piece, cell);
            }
            for (Cell next : sideNeighbours(cell)) {
                Integer nextTop = ground.walkableTops().get(next);
                if (nextTop == null || Math.abs(nextTop - ground.walkableTops().get(cell)) > 1) {
                    continue;
                }
                boolean free = existing.contains(next);
                int nextCost = here + (free ? 0 : 1);
                if (nextCost < cost.getOrDefault(next, Integer.MAX_VALUE)) {
                    cost.put(next, nextCost);
                    cameFrom.put(next, cell);
                    // 0-1 поиск: бесплатный шаг - в начало очереди, платный - в конец.
                    if (free) {
                        queue.addFirst(next);
                    } else {
                        queue.addLast(next);
                    }
                }
            }
        }

        List<Route> routes = new ArrayList<>();
        for (Map.Entry<Integer, Cell> reached : reachedAt.entrySet()) {
            // Весь маршрут от конца до начала, кроме того, что уже тропинка. Концы у дома (клетки дорожки
            // вокруг него) тоже получают тропинку - она подходит вплотную к дому.
            List<Cell> newCells = new ArrayList<>();
            Cell cell = reached.getValue();
            while (true) {
                if (!existing.contains(cell)) {
                    newCells.add(cell);
                }
                Cell previous = cameFrom.get(cell);
                if (previous == null) {
                    break;
                }
                cell = previous;
            }
            routes.add(new Route(fromIndex, reached.getKey(), newCells));
        }
        return routes;
    }

    private static void addPathBlock(ServerLevel level, VillageScan scan, Cell cell, int top,
                                     List<HouseEarthworks.Change> plants, List<HouseEarthworks.Change> paths) {
        int x = scan.worldX(cell.x());
        int z = scan.worldZ(cell.z());
        BlockPos above = new BlockPos(x, top + 1, z);
        // Трава/цветы сверху - убрать: тропинка под растением в игре выглядит сломанной.
        if (!level.getBlockState(above).isAir() && level.getBlockState(above).canBeReplaced()) {
            plants.add(new HouseEarthworks.Change(above, AIR));
        }
        paths.add(new HouseEarthworks.Change(new BlockPos(x, top, z), PATH));
    }

    private static List<Cell> sideNeighbours(Cell cell) {
        return List.of(new Cell(cell.x() + 1, cell.z()), new Cell(cell.x() - 1, cell.z()),
                new Cell(cell.x(), cell.z() + 1), new Cell(cell.x(), cell.z() - 1));
    }

    private static int root(int[] parent, int index) {
        while (parent[index] != index) {
            parent[index] = parent[parent[index]];
            index = parent[index];
        }
        return index;
    }
}
