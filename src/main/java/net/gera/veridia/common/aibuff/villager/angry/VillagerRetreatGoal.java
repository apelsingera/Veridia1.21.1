package net.gera.veridia.common.aibuff.villager.angry;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Активен VILLAGER_RETREAT_DURATION_TICKS сразу после удара мотыгой (см.
 * VillagerMeleeAttackGoal.checkAndPerformAttack -> AngryVillager.startRetreat) - житель отбегает
 * от обидчика (игрок или враждебный моб) примерно на VILLAGER_RETREAT_DISTANCE, чтобы снова
 * оказаться на дистанции броска heavy_projectile, а не топтаться вплотную. Приоритет выше и
 * мотыги, и броска, чтобы точно перехватить управление на время отступления.
 */
public class VillagerRetreatGoal extends Goal {

    private final Villager villager;

    public VillagerRetreatGoal(Villager villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AngryVillager.isRetreating(villager) && villager.getTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = villager.getTarget();
        if (target == null) return;

        villager.getLookControl().setLookAt(target, 30.0F, 30.0F);

        Vec3 away = villager.position().subtract(target.position());
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(villager.getRandom().nextDouble() - 0.5, 0.0, villager.getRandom().nextDouble() - 0.5);
        }
        Vec3 destination = villager.position().add(away.normalize().scale(AbilityConfig.VILLAGER_RETREAT_DISTANCE));

        villager.getNavigation().moveTo(destination.x, destination.y, destination.z, AbilityConfig.VILLAGER_MOVE_SPEED);
    }
}
