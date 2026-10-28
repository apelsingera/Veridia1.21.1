package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Уровень 30 (тег "launchcast") - замах LAUNCH_CAST_WINDUP_TICKS, затем отдача (knockback) и
 * резкое подкидывание цели вверх, после чего зомби на HIDE_DURATION_TICKS прячется за союзником -
 * см. HideBehindAlliesGoal. Перезарядка LAUNCH_CAST_COOLDOWN_TICKS. Структура (замах-каст-кулдаун)
 * повторяет MageZombieCastGoal, только с одним фиксированным заклинанием вместо случайного из трёх.
 */
public class LaunchCastGoal extends Goal {

    private static final String COOLDOWN_KEY = "veridia_launchcast_cooldown";

    private final Zombie zombie;
    private int windupTicksLeft;
    private LivingEntity castTarget;

    public LaunchCastGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (outOfRange(target)) return false;
        return isCooldownReady();
    }

    @Override
    public boolean canContinueToUse() {
        return windupTicksLeft > 0 && castTarget != null && castTarget.isAlive();
    }

    @Override
    public void start() {
        castTarget = zombie.getTarget();
        windupTicksLeft = AbilityConfig.LAUNCH_CAST_WINDUP_TICKS;
    }

    @Override
    public void stop() {
        windupTicksLeft = 0;
        castTarget = null;
    }

    @Override
    public void tick() {
        if (castTarget == null) return;

        zombie.getLookControl().setLookAt(castTarget, 30.0F, 30.0F);
        if (zombie.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, zombie.getX(), zombie.getEyeY(), zombie.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
        }
        windupTicksLeft--;

        if (windupTicksLeft <= 0) {
            resolveCast();
            setCooldown();
            HideBehindAlliesGoal.startHiding(zombie);
        }
    }

    private void resolveCast() {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;
        if (castTarget == null || !castTarget.isAlive() || outOfRange(castTarget)) return;

        Vec3 dir = castTarget.position().subtract(zombie.position());
        if (dir.lengthSqr() < 1.0E-4) {
            dir = new Vec3(0.1, 0.0, 0.1);
        }
        Vec3 push = dir.normalize().scale(AbilityConfig.LAUNCH_CAST_KNOCKBACK);
        Vec3 current = castTarget.getDeltaMovement();
        castTarget.setDeltaMovement(current.x + push.x, AbilityConfig.LAUNCH_CAST_UP_VELOCITY, current.z + push.z);
        castTarget.hurtMarked = true;

        serverLevel.playSound(null, zombie.getX(), zombie.getY(), zombie.getZ(),
                SoundEvents.MACE_SMASH_AIR, SoundSource.HOSTILE, 1.0F, 0.8F);
        serverLevel.sendParticles(ParticleTypes.GUST, castTarget.getX(), castTarget.getY(0.2), castTarget.getZ(),
                15, 0.3, 0.1, 0.3, 0.0);
    }

    private boolean outOfRange(LivingEntity target) {
        return zombie.distanceToSqr(target) > AbilityConfig.LAUNCH_CAST_RANGE * AbilityConfig.LAUNCH_CAST_RANGE;
    }

    private boolean isCooldownReady() {
        long last = zombie.getPersistentData().getLong(COOLDOWN_KEY);
        return zombie.level().getGameTime() - last >= AbilityConfig.LAUNCH_CAST_COOLDOWN_TICKS;
    }

    private void setCooldown() {
        zombie.getPersistentData().putLong(COOLDOWN_KEY, zombie.level().getGameTime());
    }
}
