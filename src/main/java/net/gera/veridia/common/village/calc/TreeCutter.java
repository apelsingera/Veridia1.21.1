package net.gera.veridia.common.village.calc;

import net.gera.veridia.common.compat.dynamictrees.DynamicTreesCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Вырубка ОДНОГО дерева, до которого дотянулась стена или дом.
 * <p>
 * Раньше рубилась вся связная масса: заливка шла по любым блокам дерева, включая листву, а листва
 * соседних деревьев касается друг друга - в густой тайге заливка уходила по всему лесу (до предела
 * MAX_TREE_BLOCKS) и вырубала рощу вокруг стены, оставляя ещё и недорубленные деревья без корня, которые
 * потом гибли сами и уже не попадали в снимок отката.
 * <p>
 * Брёвна построек деревни (крыша и каркас таёжных домов) деревом не считаются и не рубятся (см. VillageBuildings).
 * Теперь дерево - это "скелет" (брёвна/ветки/корни, связанные друг с другом) плюс крона. Листва
 * достаётся тому дереву, до чьего ствола ближе (не дальше TREE_LEAF_REACH): общая листва двух деревьев
 * остаётся ближнему, и соседи не обгрызаются. Каждый вырубленный блок записывается через Recorder до
 * замены.
 */
public final class TreeCutter {

    private TreeCutter() {
    }

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    // Клиентам - обновление, соседям - ничего: без побочных цепочек (листва пересчитывает расстояние,
    // Dynamic Trees роняет ветки) и без дропа.
    private static final int REPLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Лист или ветка на пути поиска, и чей он: ours - достанется вырубаемому дереву. */
    private record Claim(BlockPos pos, boolean ours, int hops) {}

    /** Скелет дерева (стволы, ветки, корни без кроны): base - самый нижний блок (у Dynamic Trees это корневой блок). */
    public record Skeleton(BlockPos base, Set<BlockPos> wood) {}

    /** Скелет дерева, к которому относится ствол/ветка в pos; null, если там не древесина (крона и постройки не в счёт). */
    @Nullable
    public static Skeleton skeletonAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!VillageBlocks.isTreePartAt(level, pos, state) || VillageBlocks.isCanopy(state)) {
            return null;
        }
        Set<BlockPos> wood = woodNetwork(level, pos, DynamicTreesCompat.isDynamicTreesBlock(state));
        BlockPos base = wood.stream()
                .min(java.util.Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ))
                .orElse(pos);
        return new Skeleton(base, wood);
    }

    /** Вырубает дерево, к которому относится блок start. Возвращает число убранных блоков. */
    public static int cut(ServerLevel level, BlockPos start, Recorder recorder) {
        BlockState startState = level.getBlockState(start);
        if (!VillageBlocks.isTreePartAt(level, start, startState)) {
            return 0;
        }
        // Ветка/лист Dynamic Trees и ванильное #minecraft:logs-бревно построек игрока (поленница,
        // сарай) технически могут касаться друг друга, но деревом является только одна сторона -
        // dynamic фиксируется один раз по стартовому блоку и не даёт перейти границу двух "сетей"
        // (см. отчёт игрока "пропали все брёвна у фермы").
        boolean dynamic = DynamicTreesCompat.isDynamicTreesBlock(startState);

        BlockPos trunk = VillageBlocks.isCanopy(startState) ? nearestWood(level, start, dynamic) : start;
        Set<BlockPos> tree = trunk == null
                ? orphanCanopy(level, start, dynamic)
                : withCanopy(level, woodNetwork(level, trunk, dynamic), dynamic);

        if (dynamic) {
            recordNeighbours(level, tree, recorder);
        }
        // Всё дерево записывается ДО первой замены. Раньше запись шла внутри цикла замены, а первая же замененная
        // ветка обрушивала через DT всю остальную сеть (см. DynamicTreesCompat.withoutBranchCascade): остальные блоки
        // дерева к моменту их записи были уже воздухом, в снимок попадал воздух, и /calculate reset возвращал
        // корень с парой веток - "обрубок". В старом снимке мира "aНовый мир" записан 1 DT-корень, 1 ветка и 0 листьев
        // (размер срубленного дерева там неизвестен, так что это согласуется с гипотезой, но не доказывает её).
        for (BlockPos pos : tree) {
            recorder.record(pos, level.getBlockState(pos));
        }
        DynamicTreesCompat.withoutBranchCascade(() -> removeAll(level, tree));
        NailerQueueAudit.log("срублено дерево у {}: {} блоков (Dynamic Trees: {})", start.toShortString(), tree.size(), dynamic);
        return tree.size();
    }

    private static void removeAll(ServerLevel level, Set<BlockPos> tree) {
        for (BlockPos pos : tree) {
            level.setBlock(pos, replacementFor(level.getBlockState(pos)), REPLACE_FLAGS);
        }
    }

    /**
     * Блоки Dynamic Trees вокруг срубленного дерева записываются в снимок, но не трогаются. Листва у DT держится
     * на цепочке "расстояний" от веток, в том числе от веток соседних деревьев: убрали дерево - часть листвы
     * соседей остаётся без опоры и засыхает сама (random tick), пока дерево срублено (между result и reset
     * проходят минуты). Без записи reset возвращал только срубленное, а соседи оставались голыми стволами.
     * Теперь reset вернёт и засохшее: каждый записанный блок встаёт на место таким, каким был.
     */
    private static void recordNeighbours(ServerLevel level, Set<BlockPos> tree, Recorder recorder) {
        BoundingBox area = BoundingBox.encapsulatingPositions(tree).orElseThrow()
                .inflatedBy(VillageCalcSettings.TREE_LEAF_REACH + VillageCalcSettings.TREE_BYSTANDER_MARGIN);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = area.minY(); y <= area.maxY(); y++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                for (int x = area.minX(); x <= area.maxX(); x++) {
                    cursor.set(x, y, z);
                    BlockState state = stateAt(level, cursor);
                    if (state != null && DynamicTreesCompat.isDynamicTreesBlock(state) && !tree.contains(cursor)) {
                        recorder.record(cursor.immutable(), state);
                    }
                }
            }
        }
    }

    /** Корневой блок Dynamic Trees - это и есть земля под деревом: заменяем его обычной землёй, а не
     * воздухом, иначе на месте дерева остаётся яма. Всё остальное дерево - воздух. */
    private static BlockState replacementFor(BlockState state) {
        BlockState soil = DynamicTreesCompat.isDynamicTreesBlock(state) ? DynamicTreesCompat.primitiveSoil(state) : null;
        return soil != null ? soil : AIR;
    }

    // --- скелет дерева ---

    /** Все брёвна/ветки/корни, связанные с start (по 26 соседям: ветки и ёлочные лапы идут по диагонали). */
    private static Set<BlockPos> woodNetwork(ServerLevel level, BlockPos start, boolean dynamic) {
        Set<BlockPos> wood = new LinkedHashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        wood.add(start);
        queue.add(start);
        while (!queue.isEmpty() && wood.size() < VillageCalcSettings.MAX_TREE_BLOCKS) {
            for (BlockPos next : neighbours(queue.poll())) {
                if (!wood.contains(next) && isWood(level, next, dynamic)) {
                    wood.add(next);
                    queue.add(next);
                }
            }
        }
        return wood;
    }

    /** Ближайший к листу ствол: стена коснулась кроны, и надо найти дерево, которому она принадлежит. */
    @Nullable
    private static BlockPos nearestWood(ServerLevel level, BlockPos start, boolean dynamic) {
        Set<BlockPos> seen = new HashSet<>();
        seen.add(start);
        List<BlockPos> layer = List.of(start);
        for (int hop = 0; hop < VillageCalcSettings.TREE_LEAF_REACH && !layer.isEmpty(); hop++) {
            List<BlockPos> nextLayer = new ArrayList<>();
            for (BlockPos pos : layer) {
                for (BlockPos next : neighbours(pos)) {
                    if (!seen.add(next)) {
                        continue;
                    }
                    if (isWood(level, next, dynamic)) {
                        return next;
                    }
                    if (isCanopy(level, next, dynamic)) {
                        nextLayer.add(next);
                    }
                }
            }
            layer = nextLayer;
        }
        return null;
    }

    // --- крона ---

    private static Set<BlockPos> withCanopy(ServerLevel level, Set<BlockPos> wood, boolean dynamic) {
        Set<BlockPos> tree = new LinkedHashSet<>(wood);
        tree.addAll(canopyNearestTo(level, wood, dynamic));
        return tree;
    }

    /**
     * Листва, до которой от нашего скелета ближе, чем от скелета любого другого дерева рядом. Волна идёт
     * от всех стволов сразу, слой за слоем (поэтому первым лист достаётся ближайшему); при равном
     * расстоянии лист остаётся чужому дереву - ему он нужнее, чем стене.
     */
    private static Set<BlockPos> canopyNearestTo(ServerLevel level, Set<BlockPos> wood, boolean dynamic) {
        BoundingBox area = BoundingBox.encapsulatingPositions(wood).orElseThrow()
                .inflatedBy(VillageCalcSettings.TREE_LEAF_REACH);

        Deque<Claim> wave = new ArrayDeque<>();
        addOtherTrees(level, area, wood, dynamic, wave);
        for (BlockPos pos : wood) {
            wave.add(new Claim(pos, true, 0));
        }

        Map<BlockPos, Boolean> leafOwner = new HashMap<>();
        while (!wave.isEmpty()) {
            Claim claim = wave.poll();
            if (claim.hops() >= VillageCalcSettings.TREE_LEAF_REACH) {
                continue;
            }
            for (BlockPos next : neighbours(claim.pos())) {
                if (leafOwner.containsKey(next) || !area.isInside(next) || !isCanopy(level, next, dynamic)) {
                    continue;
                }
                leafOwner.put(next, claim.ours());
                wave.add(new Claim(next, claim.ours(), claim.hops() + 1));
            }
        }
        Set<BlockPos> ours = new LinkedHashSet<>();
        for (Map.Entry<BlockPos, Boolean> leaf : leafOwner.entrySet()) {
            if (leaf.getValue()) {
                ours.add(leaf.getKey());
            }
        }
        return ours;
    }

    /** Стволы всех остальных деревьев в области - источники волны, которые встают в очередь ПЕРЕД нашим. */
    private static void addOtherTrees(ServerLevel level, BoundingBox area, Set<BlockPos> ours, boolean dynamic,
                                      Deque<Claim> wave) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = area.minY(); y <= area.maxY(); y++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                for (int x = area.minX(); x <= area.maxX(); x++) {
                    cursor.set(x, y, z);
                    if (isWood(level, cursor, dynamic) && !ours.contains(cursor)) {
                        wave.add(new Claim(cursor.immutable(), false, 0));
                    }
                }
            }
        }
    }

    /** Листва без ствола (остаток дерева, срубленного раньше): убираем то, что по листве в пределах
     * TREE_LEAF_REACH от стартового листа. */
    private static Set<BlockPos> orphanCanopy(ServerLevel level, BlockPos start, boolean dynamic) {
        Set<BlockPos> canopy = new LinkedHashSet<>();
        canopy.add(start);
        List<BlockPos> layer = List.of(start);
        for (int hop = 0; hop < VillageCalcSettings.TREE_LEAF_REACH && !layer.isEmpty(); hop++) {
            List<BlockPos> nextLayer = new ArrayList<>();
            for (BlockPos pos : layer) {
                for (BlockPos next : neighbours(pos)) {
                    if (!canopy.contains(next) && isCanopy(level, next, dynamic)) {
                        canopy.add(next);
                        nextLayer.add(next);
                    }
                }
            }
            layer = nextLayer;
        }
        return canopy;
    }

    // --- классификация ---

    private static boolean isWood(ServerLevel level, BlockPos pos, boolean dynamic) {
        BlockState state = stateAt(level, pos);
        return state != null && VillageBlocks.isTreePart(state) && !VillageBlocks.isCanopy(state)
                && DynamicTreesCompat.isDynamicTreesBlock(state) == dynamic
                && !VillageBuildings.isLogOfBuilding(level, pos, state);
    }

    private static boolean isCanopy(ServerLevel level, BlockPos pos, boolean dynamic) {
        BlockState state = stateAt(level, pos);
        return state != null && VillageBlocks.isCanopy(state)
                && DynamicTreesCompat.isDynamicTreesBlock(state) == dynamic;
    }

    @Nullable
    private static BlockState stateAt(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) ? level.getBlockState(pos) : null;
    }

    /** 26 соседей блока (включая диагонали). */
    private static List<BlockPos> neighbours(BlockPos pos) {
        List<BlockPos> neighbours = new ArrayList<>(26);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        neighbours.add(pos.offset(dx, dy, dz));
                    }
                }
            }
        }
        return neighbours;
    }
}
