package net.gera.veridia.common.aibuff.handlers;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

public class NoKnockbackHandler {

    private static final String TAG = "noknockback";
    private static final String PENDING_KEY = "veridia_pending_enchant_knockback";

    @SubscribeEvent
    public static void onAttack(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!target.getTags().contains(TAG)) return;

        Entity attackerEntity = event.getSource().getEntity();
        if (attackerEntity instanceof Player player) {
            int knockbackLevel = getKnockbackLevel(player.getMainHandItem(), player);
            if (knockbackLevel > 0) {
                target.getPersistentData().putLong(PENDING_KEY, target.level().getGameTime());
            }
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getTags().contains(TAG)) return;

        long pendingTick = entity.getPersistentData().getLong(PENDING_KEY);
        long now = entity.level().getGameTime();

        if (now - pendingTick <= 1) {
            return; // разрешаем - это был зачарованный удар
        }

        event.setStrength(0.0F);
    }

    private static int getKnockbackLevel(ItemStack stack, LivingEntity contextEntity) {
        Holder<Enchantment> knockbackHolder = contextEntity.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.KNOCKBACK);
        return stack.getEnchantmentLevel(knockbackHolder);
    }
}