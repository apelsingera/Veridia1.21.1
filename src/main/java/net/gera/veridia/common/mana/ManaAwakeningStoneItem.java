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

public class ManaAwakeningStoneItem extends Item {

    public ManaAwakeningStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        if (ManaHandler.isUnlocked(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("message.veridia.mana_already_unlocked"), true);
            return InteractionResultHolder.fail(stack);
        }

        serverPlayer.setData(ModAttachments.MANA_UNLOCKED, true);

        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 10.0F, 0.6F);
        serverPlayer.displayClientMessage(Component.translatable("message.veridia.mana_unlocked"), false);

        stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }
}
