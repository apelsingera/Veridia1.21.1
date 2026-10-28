package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public class PoisonOnHitHandler {

    private static final String TAG = "poison";

    @SubscribeEvent
    public static void onAttack(LivingIncomingDamageEvent event) {
        Entity attackerEntity = event.getSource().getEntity();

        if (!(attackerEntity instanceof LivingEntity attacker))
            return;

        if (!attacker.getPersistentData().getBoolean(TAG))
            return;

        LivingEntity target = event.getEntity();

        target.addEffect(new MobEffectInstance(
                MobEffects.POISON,
                AbilityConfig.POISON_DURATION_TICKS,
                AbilityConfig.POISON_AMPLIFIER
        ));
    }
}