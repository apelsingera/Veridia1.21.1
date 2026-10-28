package net.gera.veridia.common.aibuff.handlers;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Теги "slow0"/"slow1" (см. LevelZombieConfig.LEVEL_PROFILES) - при попадании по цели навешивает
 * Медлительность соответствующего уровня (0 = Slowness I, 1 = Slowness II). Не завязан конкретно
 * на зомби - сработает на любом LivingEntity с этим тегом.
 */
public class SlownessOnHitHandler {

    private static final int DURATION_TICKS = 20 * 4;

    @SubscribeEvent
    public static void onAttack(LivingIncomingDamageEvent event) {
        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        LivingEntity target = event.getEntity();

        if (attacker.getTags().contains("slow1")) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION_TICKS, 1));
        } else if (attacker.getTags().contains("slow0")) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION_TICKS, 0));
        }
    }
}
