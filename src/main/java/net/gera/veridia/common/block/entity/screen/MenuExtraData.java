package net.gera.veridia.common.block.entity.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

final class MenuExtraData {
    private MenuExtraData() {}

    static <T extends BlockEntity> T resolveBlockEntity(Inventory inv, @Nullable FriendlyByteBuf extraData, Class<T> type) {
        Level level = inv.player.level();

        if (extraData != null) {
            BlockEntity be = level.getBlockEntity(extraData.readBlockPos());
            //noinspection DataFlowIssue
            return type.isInstance(be) ? type.cast(be) : null;
        }

        HitResult hit = inv.player.pick(8.0D, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit) {
            BlockEntity be = level.getBlockEntity(blockHit.getBlockPos());
            if (type.isInstance(be)) {
                return type.cast(be);
            }
        }

        BlockPos origin = inv.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, -3, -5), origin.offset(5, 3, 5))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (type.isInstance(be)) {
                return type.cast(be);
            }
        }
        //noinspection DataFlowIssue
        return null;
    }
}