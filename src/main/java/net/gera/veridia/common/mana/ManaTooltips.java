package net.gera.veridia.common.mana;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ManaTooltips {
    public static void appendCooldownAndManaCost(List<Component> tooltip, int cooldownTicks, int manaCost) {
        tooltip.add(Component.translatable("tooltip.veridia.cooldown", cooldownTicks / 20)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.veridia.mana_cost", manaCost)
                .withStyle(ChatFormatting.GRAY));
    }
    @Deprecated //не использовать данный метод так как он помечен Deprecated и может вызывать конфликты
    public static void appendDurationAndManaCost(List<Component> tooltip, int durationTicks, int manaCost) {
        tooltip.add(Component.translatable("tooltip.veridia.duration", durationTicks / 20)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.veridia.mana_cost", manaCost)
                .withStyle(ChatFormatting.GRAY));
    }
    public static void appendDurationCooldownAndManaCost(List<Component> tooltip, int durationTicks, int cooldownTicks, int manaCost) {
        tooltip.add(Component.translatable("tooltip.veridia.duration", durationTicks / 20)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.veridia.cooldown", cooldownTicks / 20)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.veridia.mana_cost", manaCost)
                .withStyle(ChatFormatting.GRAY));
    }
}
