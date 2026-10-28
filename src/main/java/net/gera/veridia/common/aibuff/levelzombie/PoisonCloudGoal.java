package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Уровень 31 (тег "poisoncloud") - замах, затем на месте растёт ядовитое облако от 0 до
 * POISON_CLOUD_MAX_RADIUS за POISON_CLOUD_LIFETIME_TICKS, травит всех НЕ-"злых" LivingEntity (см.
 * Enemy - других хостайл-мобов не задевает) внутри текущего радиуса. Частицы - зелёный дым (WITCH)
 * + костровые (CAMPFIRE_COSY_SMOKE), как просили - "плотнее и реалистичнее". По завершении - зомби
 * прячется за союзником (см. HideBehindAlliesGoal). Перезарядка POISON_CLOUD_COOLDOWN_TICKS.
 * Пассивная Медлительность на себя во время каста задаётся отдельно тегом "selfslow0" (TagEffects).
 */
public class PoisonCloudGoal extends Goal {

    private static final String COOLDOWN_KEY = "veridia_poisoncloud_cooldown";

    private final Zombie zombie;
    private int windupTicksLeft;
    private int cloudTicksLeft;
    private double radius;
    private LivingEntity castTarget;

    public PoisonCloudGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) return false;
        return isCooldownReady();
    }

    @Override
    public boolean canContinueToUse() {
        return (windupTicksLeft > 0 || cloudTicksLeft > 0) && castTarget != null;
    }

    @Override
    public void start() {
        castTarget = zombie.getTarget();
        windupTicksLeft = AbilityConfig.POISON_CLOUD_WINDUP_TICKS;
        cloudTicksLeft = 0;
        radius = 0.0;
    }

    @Override
    public void stop() {
        windupTicksLeft = 0;
        cloudTicksLeft = 0;
        castTarget = null;
    }

    @Override
    public void tick() {
        if (castTarget != null) {
            zombie.getLookControl().setLookAt(castTarget, 30.0F, 30.0F);
        }

        if (windupTicksLeft > 0) {
            windupTicksLeft--;
            if (windupTicksLeft <= 0) {
                setCooldown();
                cloudTicksLeft = AbilityConfig.POISON_CLOUD_LIFETIME_TICKS;
                if (zombie.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, zombie.getX(), zombie.getY(), zombie.getZ(),
                            SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.2F, 0.7F);
                }
                // Гарантированный мгновенный тик по цели каста, если она ещё в радиусе на момент
                // старта - иначе при медленном росте радиуса игрок мог просто убежать и ни разу не
                // попасть под условие ниже, отравления не будет вообще ни разу за весь каст.
                if (castTarget != null && castTarget.isAlive()
                        && zombie.distanceToSqr(castTarget) <= AbilityConfig.POISON_CLOUD_MAX_RADIUS * AbilityConfig.POISON_CLOUD_MAX_RADIUS) {
                    castTarget.addEffect(new MobEffectInstance(MobEffects.POISON, AbilityConfig.POISON_CLOUD_EFFECT_DURATION_TICKS, 0));
                }
            }
            return;
        }

        if (cloudTicksLeft > 0) {
            cloudTicksLeft--;
            radius = Math.min(AbilityConfig.POISON_CLOUD_MAX_RADIUS, radius + AbilityConfig.POISON_CLOUD_EXPAND_PER_TICK);
            tickCloud();
            if (cloudTicksLeft <= 0) {
                HideBehindAlliesGoal.startHiding(zombie);
            }
        }
    }

    private void tickCloud() {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;

        int points = 12 + (int) (radius * 2);
        for (int i = 0; i < points; i++) {
            double angle = zombie.getRandom().nextDouble() * Math.PI * 2;
            double dist = zombie.getRandom().nextDouble() * radius;
            double x = zombie.getX() + Math.cos(angle) * dist;
            double z = zombie.getZ() + Math.sin(angle) * dist;
            double y = zombie.getY() + zombie.getRandom().nextDouble() * 0.5;
            serverLevel.sendParticles(ParticleTypes.WITCH, x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
            if (zombie.tickCount % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 1, 0.02, 0.05, 0.02, 0.0);
            }
        }

        if (zombie.tickCount % 5 != 0) return;

        AABB area = new AABB(
                zombie.getX() - radius, zombie.getY() - 1.0, zombie.getZ() - radius,
                zombie.getX() + radius, zombie.getY() + 2.0, zombie.getZ() + radius);
        List<LivingEntity> victims = zombie.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && !(e instanceof Enemy) && e != zombie);
        for (LivingEntity victim : victims) {
            if (zombie.distanceToSqr(victim) > radius * radius) continue;
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, AbilityConfig.POISON_CLOUD_EFFECT_DURATION_TICKS, 0));
        }
    }
    private boolean isCooldownReady() {
        long last = zombie.getPersistentData().getLong(COOLDOWN_KEY);
        return zombie.level().getGameTime() - last >= AbilityConfig.POISON_CLOUD_COOLDOWN_TICKS;
    }

    private void setCooldown() {
        zombie.getPersistentData().putLong(COOLDOWN_KEY, zombie.level().getGameTime());
    }
}
