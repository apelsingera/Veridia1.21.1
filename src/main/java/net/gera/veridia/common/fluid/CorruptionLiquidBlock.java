package net.gera.veridia.common.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class CorruptionLiquidBlock extends LiquidBlock {

    private static final Direction[] AFFECTED_DIRECTIONS = {
            Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private static final int WITHER_DURATION_TICKS = 20 * 5;

    public CorruptionLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction dir : AFFECTED_DIRECTIONS) {
            cursor.setWithOffset(pos, dir);
            BlockState neighbor = serverLevel.getBlockState(cursor);
            if (neighbor.is(Blocks.GRASS_BLOCK) || neighbor.is(Blocks.MYCELIUM) || neighbor.is(Blocks.PODZOL)) {
                serverLevel.setBlockAndUpdate(cursor.immutable(), Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        living.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION_TICKS, 0));
    }
}