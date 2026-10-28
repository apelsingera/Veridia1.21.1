package net.gera.veridia.common.block;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

/** Выжженная земля: мотыга превращает её в выжженную пашню (если сверху воздух), как мотыга землю в пашню. */
public class IncineratedDirtBlock extends Block {
    public IncineratedDirtBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        if (itemAbility == ItemAbilities.HOE_TILL
                && context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
            return ModBlocks.INCINERATED_FARMLAND.get().defaultBlockState();
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}
