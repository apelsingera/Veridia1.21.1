package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.item.custom.heavy_projectile.HeavyProjectileEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;

import java.util.EnumSet;

/**
 * Уровень 11 (тег "heavycore") - пока цель в радиусе HEAVY_CORE_THROW_RADIUS и видна, кидает
 * HeavyProjectileEntity (тот же снаряд, что и у злого жителя - см. villager/VillagerProjectileThrowGoal)
 * с перезарядкой HEAVY_CORE_COOLDOWN_TICKS. Сразу после броска ставит флаг отступления
 * (RETREAT_UNTIL_KEY), который подхватывает HeavyCoreRetreatGoal - "кинул и убежал", чтобы снова
 * зарядить бросок, а не топтаться вплотную до следующего броска.
 */
public class HeavyCoreThrowGoal extends Goal {

    static final String RETREAT_UNTIL_KEY = "veridia_heavycore_retreat_until";
    private static final String COOLDOWN_KEY = "veridia_heavycore_cooldown";

    private final Zombie zombie;

    public HeavyCoreThrowGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (isRetreating()) return false;
        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (zombie.distanceToSqr(target) > AbilityConfig.HEAVY_CORE_THROW_RADIUS * AbilityConfig.HEAVY_CORE_THROW_RADIUS) return false;
        return isCooldownReady();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        zombie.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = zombie.getTarget();
        if (target == null) return;

        zombie.getLookControl().setLookAt(target, 30.0F, 30.0F);
        zombie.getNavigation().stop();

        if (!zombie.getSensing().hasLineOfSight(target)) return;

        throwProjectile(target);
        setCooldown();
        startRetreat();
    }

    private void throwProjectile(LivingEntity target) {
        HeavyProjectileEntity projectile = new HeavyProjectileEntity(zombie, zombie.level());
        projectile.setDamage(AbilityConfig.HEAVY_CORE_DAMAGE);

        double dx = target.getX() - zombie.getX();
        double dy = target.getEyeY() - 1.1F - projectile.getY();
        double dz = target.getZ() - zombie.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz) * 0.2;

        projectile.shoot(dx, dy + horizontalDist, dz, 1.5F, 8.0F);
        zombie.level().addFreshEntity(projectile);
        zombie.swing(InteractionHand.MAIN_HAND);
    }

    private boolean isCooldownReady() {
        long last = zombie.getPersistentData().getLong(COOLDOWN_KEY);
        return zombie.level().getGameTime() - last >= AbilityConfig.HEAVY_CORE_COOLDOWN_TICKS;
    }

    private void setCooldown() {
        zombie.getPersistentData().putLong(COOLDOWN_KEY, zombie.level().getGameTime());
    }

    private void startRetreat() {
        zombie.getPersistentData().putLong(RETREAT_UNTIL_KEY,
                zombie.level().getGameTime() + AbilityConfig.HEAVY_CORE_RETREAT_TICKS);
    }

    private boolean isRetreating() {
        return zombie.level().getGameTime() < zombie.getPersistentData().getLong(RETREAT_UNTIL_KEY);
    }
}
