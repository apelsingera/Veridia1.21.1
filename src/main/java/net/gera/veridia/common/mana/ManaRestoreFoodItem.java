package net.gera.veridia.common.mana;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ManaRestoreFoodItem extends Item {
    private final int manaAmount;

    public ManaRestoreFoodItem(int manaAmount, Properties properties) {
        super(properties);
        this.manaAmount = manaAmount;
    }
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            ManaHandler.addMana(player, manaAmount);
        }

        return result;
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.veridia.mana_restore", manaAmount)
                .withStyle(ChatFormatting.GRAY));
    }
}
