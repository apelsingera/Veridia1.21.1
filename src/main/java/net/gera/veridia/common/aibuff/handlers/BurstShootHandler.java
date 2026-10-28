package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.aibuff.BurstBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class BurstShootHandler {

    private static final String TAG = "burst";
    //private static final String APPLIED_TAG = "burst_applied";

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractSkeleton skeleton)) return;
        if (skeleton.level().isClientSide()) return;
        if (!skeleton.getTags().contains(TAG)) return;

        boolean hasGoal = skeleton.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof BurstBowAttackGoal);

        if (hasGoal) return;

        skeleton.goalSelector.getAvailableGoals()
                .removeIf(wrapped -> wrapped.getGoal() instanceof RangedBowAttackGoal);

        skeleton.goalSelector.addGoal(4,
                new BurstBowAttackGoal<>(skeleton,
                        AbilityConfig.BURST_MOVE_SPEED,
                        AbilityConfig.BURST_ATTACK_RADIUS));
    }
}