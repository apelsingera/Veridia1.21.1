package net.gera.veridia.common.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class CreativeBoneMealItem extends BoneMealItem {

    private static final int CHARGES = 64;

    public CreativeBoneMealItem(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos relative = pos.relative(context.getClickedFace());
        ItemStack dummy = new ItemStack(Items.BONE_MEAL, CHARGES);

        boolean any = false;
        for (int i = 0; i < CHARGES && !dummy.isEmpty(); i++) {
            if (growCrop(dummy, level, pos)) {
                any = true;
                continue;
            }
            BlockState state = level.getBlockState(pos);
            boolean sturdy = state.isFaceSturdy(level, pos, context.getClickedFace());
            if (sturdy && growWaterPlant(dummy, level, relative, context.getClickedFace())) {
                any = true;
            } else {
                break;
            }
        }

        if (!any) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (context.getPlayer() != null) {
                context.getPlayer().gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            }
            level.levelEvent(1505, pos, 15);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
