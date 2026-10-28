package net.gera.veridia.common.aibuff.handlers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Тег "laserbeam" (уровень 32, см. levelzombie.LaserBeamGoal) - лазер должен включаться КАК
 * РЕАКЦИЯ на полученный урон, а не просто по факту того, что зомби заметил/агрит игрока. Тут
 * только выставляется флаг "только что получил урон" (TRIGGERED_KEY) - саму способность запускает
 * LaserBeamGoal.canUse(), читая этот флаг, и сбрасывает его в start().
 */
public class LaserTriggerHandler {

    public static final String TRIGGERED_KEY = "veridia_laser_triggered";

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Zombie zombie)) return;
        if (!zombie.getTags().contains("laserbeam")) return;

        zombie.getPersistentData().putBoolean(TRIGGERED_KEY, true);
    }
}
