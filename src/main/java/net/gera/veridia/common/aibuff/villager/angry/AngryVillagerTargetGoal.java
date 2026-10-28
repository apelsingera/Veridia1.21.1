package net.gera.veridia.common.aibuff.villager.angry;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;

import java.util.EnumSet;

/**
 * Держит цель жителя на обидчике (игрок или враждебный моб), пока AngryVillager.isAngry() не
 * истечёт - как только злость прошла (3 минуты после удара), canContinueToUse() вернёт false и
 * Goal.stop() сам сбросит target, отдельный хендлер прощения не нужен.
 */
public class AngryVillagerTargetGoal extends Goal {

    private final Villager villager;
    private LivingEntity target;

    public AngryVillagerTargetGoal(Villager villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!AngryVillager.isAngry(villager)) return false;
        LivingEntity attacker = AngryVillager.resolveTarget(villager);
        if (attacker == null) return false;
        this.target = attacker;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return AngryVillager.isAngry(villager) && target != null && target.isAlive();
    }

    @Override
    public void start() {
        villager.setTarget(target);
    }

    @Override
    public void stop() {
        villager.setTarget(null);
        target = null;
    }
}
