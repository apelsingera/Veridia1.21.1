package net.gera.veridia.common.mana.spell;

import net.gera.veridia.common.enchantment.ModEnchantments;
import net.gera.veridia.common.item.custom.GemCooldowns;
import net.gera.veridia.common.mana.ManaHandler;
import net.gera.veridia.common.mana.ManaProgression;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SpellCasting {
    private static final float FAILURE_DAMAGE = 8.0F;

    public static void tryCast(ServerPlayer player, Item item, GemSpell spell, ItemStack stack, int manaCost) {
        tryCast(player, item, spell, stack, manaCost, GemCooldowns.COOLDOWN_TICKS);
    }
    public static void tryCast(ServerPlayer player, Item item, GemSpell spell, ItemStack stack, int manaCost, int cooldownTicks) {
        if (GemCooldowns.isOnCooldown(player, item)) {
            return;
        }
        int effectiveManaCost = ModEnchantments.reduceManaCost(player, stack, manaCost);
        if (!ManaHandler.consumeMana(player, effectiveManaCost)) {
            return;
        }
        GemCooldowns.applyCooldown(player, item, cooldownTicks);

        double failureRoll = player.getRandom().nextDouble() * 100.0;
        if (failureRoll < ManaProgression.getFailureChance(player)) {
            player.hurt(player.level().damageSources().magic(), FAILURE_DAMAGE);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6F, 0.6F);
            return;
        }

        double superRoll = player.getRandom().nextDouble() * 100.0;
        if (superRoll < ManaProgression.getSuperChance(player)) {
            spell.castSuper(player, stack);
        } else {
            spell.castNormal(player, stack);
        }
    }
}