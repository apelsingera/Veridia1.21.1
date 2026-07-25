package net.gera.veridiafoods.block.tree;

import net.gera.veridiafoods.block.ModBlocks;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

public class LogBlock extends RotatedPillarBlock {
    public LogBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {

        if(context.getItemInHand().getItem() instanceof AxeItem) {
            if(state.is(ModBlocks.SCULK_LOG)) {
                return ModBlocks.STRIPPED_SCULK_LOG.get().defaultBlockState().getBlock().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
            }
            if(state.is(ModBlocks.SCULK_WOOD)) {
                return ModBlocks.STRIPPED_SCULK_WOOD.get().defaultBlockState().getBlock().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
            }
        }


        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}
