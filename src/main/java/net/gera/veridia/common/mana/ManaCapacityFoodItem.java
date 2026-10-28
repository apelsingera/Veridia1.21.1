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

public class ManaCapacityFoodItem extends Item {
    private final int bonusAmount;
    private final int durationTicks;

    public ManaCapacityFoodItem(int bonusAmount, int durationTicks, Properties properties) {
        super(properties);
        this.bonusAmount = bonusAmount;
        this.durationTicks = durationTicks;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            ManaProgression.applyTemporaryMaxManaBoost(player, bonusAmount, durationTicks);
        }

        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.veridia.mana_capacity_boost", bonusAmount, durationTicks / 20)
                .withStyle(ChatFormatting.GRAY));
    }
}
