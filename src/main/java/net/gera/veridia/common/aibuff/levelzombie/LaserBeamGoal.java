package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.aibuff.handlers.LaserTriggerHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Уровень 32 (тег "laserbeam") - НЕ включается просто по факту агро на игрока: триггер -
 * полученный урон (см. LaserTriggerHandler, ставит TRIGGERED_KEY при попадании по зомби). После
 * этого - самонаводящийся луч активен LASER_ACTIVE_TICKS, поворот ограничен
 * LASER_TURN_RATE_DEGREES/тик ("медленнее, чем игрок бегает" - от луча можно убежать в сторону),
 * длина LASER_RANGE. Бьёт LASER_DAMAGE_PER_TICK любую НЕ-"злую" LivingEntity (см. Enemy - других
 * хостайл-мобов не задевает) в узком коридоре вдоль направления взгляда - без вокселного рейкаста
 * через блоки, просто пересечение отрезка с хитбоксом цели. Перезарядка LASER_COOLDOWN_TICKS.
 */
public class LaserBeamGoal extends Goal {

    private static final String COOLDOWN_KEY = "veridia_laser_cooldown";
    private static final double BEAM_WIDTH = 0.6D;

    private final Zombie zombie;
    private int activeTicksLeft;
    private LivingEntity castTarget;

    public LaserBeamGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (!zombie.getPersistentData().getBoolean(LaserTriggerHandler.TRIGGERED_KEY)) return false;
        return isCooldownReady();
    }

    @Override
    public boolean canContinueToUse() {
        return activeTicksLeft > 0 && castTarget != null && castTarget.isAlive();
    }

    @Override
    public void start() {
        zombie.getPersistentData().putBoolean(LaserTriggerHandler.TRIGGERED_KEY, false);
        castTarget = zombie.getTarget();
        activeTicksLeft = AbilityConfig.LASER_ACTIVE_TICKS;
        setCooldown();
        if (zombie.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, zombie.getX(), zombie.getY(), zombie.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0F, 1.8F);
        }
    }

    @Override
    public void stop() {
        activeTicksLeft = 0;
        castTarget = null;
    }

    @Override
    public void tick() {
        if (castTarget == null) return;
        activeTicksLeft--;

        turnTowards(castTarget);

        Vec3 origin = zombie.getEyePosition();
        Vec3 dir = Vec3.directionFromRotation(zombie.getXRot(), zombie.getYRot());
        Vec3 end = origin.add(dir.scale(AbilityConfig.LASER_RANGE));

        if (zombie.level() instanceof ServerLevel serverLevel) {
            for (double t = 0; t <= 1.0; t += 0.1) {
                Vec3 p = origin.lerp(end, t);
                serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        AABB area = new AABB(origin, end).inflate(BEAM_WIDTH);
        List<LivingEntity> hit = zombie.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && !(e instanceof Enemy) && e != zombie);
        for (LivingEntity victim : hit) {
            // Раньше тут сравнивалось расстояние до victim.position() (это НОГИ цели) с лучом,
            // который летит на высоте глаз зомби - разница метр-полтора всегда была больше
            // BEAM_WIDTH, и попадание почти никогда не засчитывалось. Правильная проверка - честное
            // пересечение отрезка луча с реальным хитбоксом цели (несколько раздутым для запаса).
            if (victim.getBoundingBox().inflate(0.25).clip(origin, end).isEmpty()) continue;
            victim.hurt(zombie.damageSources().magic(), AbilityConfig.LASER_DAMAGE_PER_TICK);
        }
    }

    private void turnTowards(LivingEntity target) {
        double dx = target.getX() - zombie.getX();
        double dz = target.getZ() - zombie.getZ();
        double dy = target.getEyeY() - zombie.getEyeY();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float wantedYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float wantedPitch = (float) -(Mth.atan2(dy, horizontalDist) * (180.0 / Math.PI));

        zombie.setYRot(rotateTowards(zombie.getYRot(), wantedYaw));
        zombie.setXRot(rotateTowards(zombie.getXRot(), wantedPitch));
        zombie.yHeadRot = zombie.getYRot();
    }

    private float rotateTowards(float current, float wanted) {
        float delta = Mth.wrapDegrees(wanted - current);
        float clamped = Math.max(-(float) AbilityConfig.LASER_TURN_RATE_DEGREES,
                Math.min((float) AbilityConfig.LASER_TURN_RATE_DEGREES, delta));
        return current + clamped;
    }

    private boolean isCooldownReady() {
        long last = zombie.getPersistentData().getLong(COOLDOWN_KEY);
        return zombie.level().getGameTime() - last >= AbilityConfig.LASER_COOLDOWN_TICKS;
    }

    private void setCooldown() {
        zombie.getPersistentData().putLong(COOLDOWN_KEY, zombie.level().getGameTime());
    }
}
