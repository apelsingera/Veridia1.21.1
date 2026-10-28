package net.gera.veridia.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

/**
 * Магма в чёрном камне. Урон и свет те же, что у ванильной (MagmaBlock), но ванильный поиск пути узнаёт магму
 * по самому блоку Blocks.MAGMA_BLOCK, поэтому мобы не обходили этот блок. Здесь путь описан так же, как у ванильной
 * магмы: на ней опасность огня (DAMAGE_FIRE), рядом с ней - DANGER_FIRE.
 */
public class BlackstoneMagmaBlock extends MagmaBlock {
    public BlackstoneMagmaBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return PathType.DAMAGE_FIRE;
    }

    @Override
    public @Nullable PathType getAdjacentBlockPathType(BlockState state, BlockGetter level, BlockPos pos,
                                                       @Nullable Mob mob, PathType originalType) {
        return PathType.DANGER_FIRE;
    }
}
