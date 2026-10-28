package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Уровень 11 (тег "heavycore") - активен HEAVY_CORE_RETREAT_TICKS сразу после броска
 * HeavyProjectileEntity (см. HeavyCoreThrowGoal.startRetreat) - зомби отбегает от цели, чтобы
 * снова оказаться на дистанции для следующего броска, а не топтаться вплотную. Тот же паттерн, что
 * и у villager/VillagerRetreatGoal.
 */
public class HeavyCoreRetreatGoal extends Goal {

    private final Zombie zombie;

    public HeavyCoreRetreatGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (zombie.level().getGameTime() >= zombie.getPersistentData().getLong(HeavyCoreThrowGoal.RETREAT_UNTIL_KEY)) return false;
        return zombie.getTarget() != null;
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

        Vec3 away = zombie.position().subtract(target.position());
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(zombie.getRandom().nextDouble() - 0.5, 0.0, zombie.getRandom().nextDouble() - 0.5);
        }
        Vec3 destination = zombie.position().add(away.normalize().scale(AbilityConfig.HEAVY_CORE_RETREAT_DISTANCE));

        zombie.getNavigation().moveTo(destination.x, destination.y, destination.z, AbilityConfig.HEAVY_CORE_MOVE_SPEED);
    }
}
