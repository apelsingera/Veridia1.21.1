package net.gera.veridia.common.mana;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TierToolItem extends Item {
    private final int stepIndex;

    public TierToolItem(int stepIndex, Properties properties) {
        super(properties);
        this.stepIndex = stepIndex;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        int progress = ManaProgression.progress(serverPlayer);
        if (progress != stepIndex) {
            serverPlayer.displayClientMessage(Component.translatable("message.veridia.rune_out_of_order"), true);
            return InteractionResultHolder.fail(stack);
        }

        serverPlayer.setData(ModAttachments.MANA_UPGRADE_PROGRESS, progress + 1);
        ManaProgression.reconcileAttributes(serverPlayer);

        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

        stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }
}
