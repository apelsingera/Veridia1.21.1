package net.gera.veridia.common.aibuff;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class LongReachAttackGoal extends MeleeAttackGoal {

    private final double reach;

    public LongReachAttackGoal(PathfinderMob mob,
                               double speedModifier,
                               boolean followingTargetEvenIfNotSeen,
                               double reach) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
        this.reach = reach;
    }

    @Override
    protected boolean canPerformAttack(LivingEntity entity) {
        double reachSqr = this.reach * this.reach;
        return this.isTimeToAttack()
                && this.mob.distanceToSqr(entity) <= reachSqr
                && this.mob.getSensing().hasLineOfSight(entity);
    }
}
