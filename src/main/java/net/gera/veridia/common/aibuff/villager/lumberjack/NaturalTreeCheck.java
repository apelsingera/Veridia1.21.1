package net.gera.veridia.common.aibuff.villager.lumberjack;

import net.gera.veridia.common.village.calc.VillageBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Настоящее ванильное дерево или постройка из брёвен (таёжный дом, стена, башня, сарай). Блок
 * #minecraft:logs их не различает, а "рядом есть листва" не годится: дом в лесу стоит под кронами.
 * Различают три признака, ствол Dynamic Trees (ветка) их не касается:
 * <ul>
 *     <li>в дереве нет обработанных брёвен: ошкуренных (stripped) и "дерева" со всех сторон (_wood);</li>
 *     <li>ствол не касается рукотворных блоков (доски, камень, стекло, двери, факелы...);</li>
 *     <li>у дерева есть настоящая листва: сгенерированная миром или выросшая из саженца, у неё
 *     persistent=false; листва, поставленная строителем (в том числе в шаблоне дома), persistent=true.</li>
 * </ul>
 */
final class NaturalTreeCheck {

    private NaturalTreeCheck() {
    }

    /** Бревно start принадлежит настоящему дереву, которое можно рубить. */
    static boolean isNaturalTree(ServerLevel level, BlockPos start) {
        List<BlockPos> logs = new ArrayList<>();
        AtomicBoolean handMade = new AtomicBoolean();
        // Обход прекращается на первом же рукотворном месте: у дома оно рядом с первым бревном.
        walkTouchingLogs(level, start, log -> {
            logs.add(log);
            handMade.set(isHandMade(level, log));
            return !handMade.get();
        });
        return !handMade.get() && hasNaturalLeavesAround(level, logs);
    }

    /** Брёвна, связанные с start гранями: ствол дерева или каркас постройки; обход ограничен по размеру и расстоянию. */
    static List<BlockPos> touchingLogs(ServerLevel level, BlockPos start) {
        List<BlockPos> logs = new ArrayList<>();
        walkTouchingLogs(level, start, log -> logs.add(log));
        return logs;
    }

    /** Обходит брёвна вширь от start, пока visitor возвращает true. */
    private static void walkTouchingLogs(ServerLevel level, BlockPos start, Predicate<BlockPos> visitor) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        int walked = 0;
        while (!queue.isEmpty() && walked < LumberjackVillagerConfig.MAX_TREE_SCAN_BLOCKS) {
            BlockPos pos = queue.poll();
            walked++;
            if (!visitor.test(pos)) {
                return;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.add(next)
                        || next.distManhattan(start) > LumberjackVillagerConfig.TREE_SCAN_MAX_DISTANCE
                        || !level.isLoaded(next)
                        || !LumberjackVillagerConfig.isChoppableLog(level.getBlockState(next))) {
                    continue;
                }
                queue.add(next);
            }
        }
    }

    private static boolean isHandMade(ServerLevel level, BlockPos log) {
        if (isWorkedLog(level.getBlockState(log))) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (isHandMadeBlock(level.getBlockState(log.relative(direction)))) {
                return true;
            }
        }
        return false;
    }

    /** Ошкуренное бревно и "дерево со всех сторон" в природе не растут. */
    private static boolean isWorkedLog(BlockState state) {
        String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return name.startsWith("stripped_") || name.endsWith("_wood") || name.endsWith("_hyphae");
    }

    /** Не земля, не растение и не часть дерева: такое ствол сам по себе не касается. Исключения - то, что на
     * деревьях бывает само: ульи, какао и лёд у ствола в снежных биомах. */
    private static boolean isHandMadeBlock(BlockState state) {
        return VillageBlocks.isStructure(state)
                && !state.is(BlockTags.BEEHIVES)
                && !state.is(BlockTags.ICE)
                && !state.is(Blocks.COCOA);
    }

    private static boolean hasNaturalLeavesAround(ServerLevel level, List<BlockPos> logs) {
        int radius = LumberjackVillagerConfig.LEAF_SEARCH_RADIUS;
        for (BlockPos log : logs) {
            for (BlockPos pos : BlockPos.betweenClosed(log.offset(-radius, -radius, -radius), log.offset(radius, radius, radius))) {
                if (level.isLoaded(pos) && isNaturalLeaves(level.getBlockState(pos))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Листва мира или саженца: у поставленной вручную persistent=true, и она не опадает. */
    private static boolean isNaturalLeaves(BlockState state) {
        return state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT);
    }
}
