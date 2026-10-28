package net.gera.veridia.common.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;

public class Plague extends MobEffect {
    public Plague(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (livingEntity.isInvertedHealAndHarm()) {
            return true;
        }

        MobEffectInstance instance = livingEntity.getEffect(ModEffects.PLAGUE);
        if (instance == null) return true;

        int duration = instance.getDuration();

        if (duration <= 1200 && duration > 300) {
            if (duration % 40 == 0) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false, false));
            }
        }

        if (duration == 300) {
            if (livingEntity instanceof Player player) {
                player.kill();
            } else {
                livingEntity.hurt(livingEntity.damageSources().magic(), Float.MAX_VALUE);
            }
        }

        if (duration == 299) {
            if (livingEntity.level() instanceof ServerLevel level) {
                Zombie zombie = EntityType.ZOMBIE.create(level);
                if (zombie != null) {
                    zombie.moveTo(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), livingEntity.getYRot(), livingEntity.getXRot());
                    zombie.setPersistenceRequired();
                    zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(livingEntity.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    level.addFreshEntity(zombie);
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}