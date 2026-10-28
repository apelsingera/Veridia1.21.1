package net.gera.veridia.common.aibuff.levelzombie;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;


public class MageZombieCastGoal extends Goal {

    private static final String COOLDOWN_KEY = "veridia_zombie_mage_cooldown";

    private final Zombie mob;
    private int windupTicksLeft;
    private int pendingSpell;
    private LivingEntity castTarget;

    public MageZombieCastGoal(Zombie mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (outOfCastRange(target)) return false;
        return isCooldownReady();
    }

    @Override
    public boolean canContinueToUse() {
        return windupTicksLeft > 0 && mob.isAlive() && castTarget != null && castTarget.isAlive();
    }

    @Override
    public void start() {
        castTarget = mob.getTarget();
        pendingSpell = mob.getRandom().nextInt(3);
        windupTicksLeft = lerp(LevelZombieConfig.MAGE_WINDUP_MAX_TICKS, LevelZombieConfig.MAGE_WINDUP_MIN_TICKS, levelProgress());
    }

    @Override
    public void stop() {
        windupTicksLeft = 0;
        castTarget = null;
    }

    @Override
    public void tick() {
        if (castTarget == null) return;

        mob.getLookControl().setLookAt(castTarget, 30.0F, 30.0F);
        spawnWindupParticles();
        windupTicksLeft--;

        if (windupTicksLeft <= 0) {
            resolveCast();
            setCooldown();
        }
    }

    private void resolveCast() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        if (castTarget == null || !castTarget.isAlive()) return;

        if (outOfCastRange(castTarget) || !mob.getSensing().hasLineOfSight(castTarget)) {
            spawnMissParticles(serverLevel);
            return;
        }

        double t = levelProgress();
        switch (pendingSpell) {
            case 0 -> castPoisonBolt(serverLevel, castTarget, t);
            case 1 -> castKnockbackBlast(serverLevel, castTarget, t);
            default -> castLifestealBolt(serverLevel, castTarget, t);
        }
    }

    private boolean outOfCastRange(LivingEntity target) {
        return mob.distanceToSqr(target) > LevelZombieConfig.MAGE_CAST_RANGE * LevelZombieConfig.MAGE_CAST_RANGE;
    }

    private void spawnWindupParticles() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(ParticleTypes.WITCH, mob.getX(), mob.getEyeY(), mob.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
    }

    private void spawnMissParticles(ServerLevel level) {
        level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getEyeY(), mob.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.8F, 1.4F);
    }

    private double levelProgress() {
        int level = mob.getPersistentData().getInt(LevelZombieHandler.LEVEL_KEY);
        int tierMin = LevelZombieConfig.TIER3_MAX_LEVEL + 1;
        int span = LevelZombieConfig.MAX_LEVEL - tierMin;
        if (span <= 0) return 1.0;
        return Math.min(1.0, Math.max(0.0, (level - tierMin) / (double) span));
    }

    private void castPoisonBolt(ServerLevel level, LivingEntity target, double t) {
        int duration = lerp(LevelZombieConfig.MAGE_POISON_MIN_DURATION_TICKS, LevelZombieConfig.MAGE_POISON_MAX_DURATION_TICKS, t);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 1));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LevelZombieConfig.MAGE_SLOW_DURATION_TICKS, 1));

        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY(0.5), target.getZ(), 20, 0.3, 0.4, 0.3, 0.01);
    }

    private void castKnockbackBlast(ServerLevel level, LivingEntity target, double t) {
        double strength = lerp(LevelZombieConfig.MAGE_KNOCKBACK_MIN_STRENGTH, LevelZombieConfig.MAGE_KNOCKBACK_MAX_STRENGTH, t);

        Vec3 dir = target.position().subtract(mob.position());
        if (dir.lengthSqr() < 1.0E-4) {
            dir = new Vec3(0.1, 0.0, 0.1);
        }
        Vec3 push = dir.normalize().scale(strength);
        Vec3 current = target.getDeltaMovement();
        target.setDeltaMovement(current.x + push.x, Math.max(current.y, 0.35), current.z + push.z);
        target.hurtMarked = true;

        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.MACE_SMASH_AIR, SoundSource.HOSTILE, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.GUST, target.getX(), target.getY(0.2), target.getZ(), 10, 0.3, 0.1, 0.3, 0.0);
    }

    private void castLifestealBolt(ServerLevel level, LivingEntity target, double t) {
        float damage = (float) lerp(LevelZombieConfig.MAGE_LIFESTEAL_MIN_DAMAGE, LevelZombieConfig.MAGE_LIFESTEAL_MAX_DAMAGE, t);

        target.hurt(mob.damageSources().magic(), damage);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, LevelZombieConfig.MAGE_WITHER_DURATION_TICKS, 0));
        mob.heal(damage);

        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 1.0F, 0.7F);
        level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY(0.5), target.getZ(), 15, 0.3, 0.4, 0.3, 0.01);

    }

    private static int lerp(int min, int max, double t) {
        return min + (int) ((max - min) * t);
    }

    private static double lerp(double min, double max, double t) {
        return min + (max - min) * t;
    }

    private boolean isCooldownReady() {
        long last = mob.getPersistentData().getLong(COOLDOWN_KEY);
        int cooldown = lerp(LevelZombieConfig.MAGE_MAX_COOLDOWN_TICKS, LevelZombieConfig.MAGE_MIN_COOLDOWN_TICKS, levelProgress());
        return mob.level().getGameTime() - last >= cooldown;
    }

    private void setCooldown() {
        mob.getPersistentData().putLong(COOLDOWN_KEY, mob.level().getGameTime());
    }
}
