package net.gera.veridia.common.aibuff;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;

import java.util.EnumSet;

public class BurstBowAttackGoal<T extends Mob & RangedAttackMob> extends Goal {

    private final T mob;
    private final double moveSpeed;
    private final float attackRadius;
    private final float attackRadiusSqr;

    private int reloadTimer;
    private int arrowsLeftInBurst;
    private int ticksUntilNextArrow;

    public BurstBowAttackGoal(T mob, double moveSpeed, float attackRadius) {
        this.mob = mob;
        this.moveSpeed = moveSpeed;
        this.attackRadius = attackRadius;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override
    public boolean canUse() {
        return this.mob.getTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() || !this.mob.getNavigation().isDone() || this.arrowsLeftInBurst > 0;
    }

    @Override
    public void start() {
        System.out.println("BURST GOAL START: " + this.mob);
        this.mob.setAggressive(true);
        this.reloadTimer = 0;
        this.arrowsLeftInBurst = 0;
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.reloadTimer = 0;
        this.arrowsLeftInBurst = 0;
        this.mob.stopUsingItem();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        boolean canSee = this.mob.getSensing().hasLineOfSight(target);
        double distSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean inRange = distSqr <= (double) this.attackRadiusSqr;

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Идёт очередь — стоим на месте, пока не выпустим все стрелы
        if (this.arrowsLeftInBurst > 0) {
            this.mob.getNavigation().stop();

            if (this.ticksUntilNextArrow > 0) {
                this.ticksUntilNextArrow--;
                return;
            }
            if (!canSee) return;

            float distanceFactor = Mth.clamp(Mth.sqrt((float) distSqr) / this.attackRadius, 0.1F, 1.0F);
            this.mob.performRangedAttack(target, distanceFactor);

            this.arrowsLeftInBurst--;
            this.ticksUntilNextArrow = AbilityConfig.BURST_ARROW_DELAY_TICKS;
            return;
        }

        // Подход на дистанцию / перезарядка перед новой очередью
        if (inRange && canSee) {
            this.mob.getNavigation().stop();
            this.reloadTimer++;
            if (this.reloadTimer >= AbilityConfig.BURST_RELOAD_TICKS) {
                this.reloadTimer = 0;
                this.arrowsLeftInBurst = AbilityConfig.BURST_ARROW_COUNT;
                this.ticksUntilNextArrow = 0;
            }
        } else {
            this.mob.getNavigation().moveTo(target, this.moveSpeed);
            this.reloadTimer = 0;
        }

    }
}