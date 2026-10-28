package net.gera.veridia.common.aibuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Мир для WitherPathfinder. Клетки в непрогруженных чанках считаются стеной, которую нельзя ломать: Level.getBlockState
 * на непрогруженном чанке загружает его прямо в потоке сервера, а окно поиска выходит за моба и цель на margin блоков
 * и у границы прогруженной области могло это сделать.
 */
final class LevelWitherTerrain implements WitherPathfinder.Terrain {

    private final Level level;

    LevelWitherTerrain(Level level) {
        this.level = level;
    }

    @Override
    public boolean isPassable(BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getCollisionShape(level, pos).isEmpty();
    }

    @Override
    public boolean isSolidFloor(BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    public boolean isMineable(BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && AbilityConfig.isPickaxeWhitelisted(state);
    }
}
