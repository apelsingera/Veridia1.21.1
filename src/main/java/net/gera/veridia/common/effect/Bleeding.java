package net.gera.veridia.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
public class Bleeding extends MobEffect {

    public static final int DAMAGE_INTERVAL_TICKS = 20 * 3;
    public static final float DAMAGE_PER_TICK = 0.5F;

    public Bleeding(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        livingEntity.hurt(livingEntity.damageSources().magic(), DAMAGE_PER_TICK);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % DAMAGE_INTERVAL_TICKS == 0;
    }
}
