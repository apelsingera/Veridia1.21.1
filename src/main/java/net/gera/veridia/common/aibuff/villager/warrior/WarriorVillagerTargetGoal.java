package net.gera.veridia.common.aibuff.villager.warrior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class WarriorVillagerTargetGoal extends Goal {

    private final Villager villager;
    private LivingEntity target;
    private int rescanCooldown;

    public WarriorVillagerTargetGoal(Villager villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (rescanCooldown > 0) {
            rescanCooldown--;
            return false;
        }
        rescanCooldown = WarriorVillagerConfig.TARGET_RESCAN_TICKS;

        this.target = findNearestHostile();
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity current = villager.getTarget();
        if (current == null || !current.isAlive()) return false;

        double giveUp = WarriorVillagerConfig.TARGET_GIVE_UP_RADIUS;
        return villager.distanceToSqr(current) <= giveUp * giveUp;
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

    private LivingEntity findNearestHostile() {
        double radius = WarriorVillagerConfig.TARGET_RADIUS;
        AABB area = villager.getBoundingBox().inflate(radius);
        List<Mob> candidates = villager.level().getEntitiesOfClass(Mob.class, area, WarriorVillagerTargetGoal::isValidTarget);

        LivingEntity nearest = null;
        double nearestDistSqr = radius * radius;
        for (Mob candidate : candidates) {
            double distSqr = villager.distanceToSqr(candidate);
            if (distSqr >= nearestDistSqr) continue;
            if (!villager.getSensing().hasLineOfSight(candidate)) continue;

            nearest = candidate;
            nearestDistSqr = distSqr;
        }
        return nearest;
    }

    private static boolean isValidTarget(Mob mob) {
        if (!(mob instanceof Enemy) || !mob.isAlive()) return false;
        return WarriorVillagerConfig.ATTACK_CREEPERS || !(mob instanceof Creeper);
    }
}