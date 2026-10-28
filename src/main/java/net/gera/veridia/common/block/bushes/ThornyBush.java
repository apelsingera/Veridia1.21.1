package net.gera.veridia.common.block.bushes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.jetbrains.annotations.Nullable;

/**
 * Колючий куст, который мобы обходят, как ванильный ягодный куст. Ванильный поиск пути узнаёт его по самому блоку
 * Blocks.SWEET_BERRY_BUSH, а наши кусты - другие блоки, поэтому тип пути задан здесь, как у ванильного:
 * на кусте DAMAGE_OTHER, рядом с ним DANGER_OTHER.
 */
public interface ThornyBush extends IBlockExtension {
    @Override
    default @Nullable PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return PathType.DAMAGE_OTHER;
    }

    @Override
    default @Nullable PathType getAdjacentBlockPathType(BlockState state, BlockGetter level, BlockPos pos,
                                                        @Nullable Mob mob, PathType originalType) {
        return PathType.DANGER_OTHER;
    }
}
