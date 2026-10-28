package net.gera.veridia.common.aibuff.villager.angry;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.npc.Villager;

/**
 * Обычный ванильный MeleeAttackGoal всегда "может использоваться", пока есть цель, - независимо
 * от дистанции, потому что сам занимается сближением. Из-за общих MOVE/LOOK флагов это забило бы
 * VillagerProjectileThrowGoal (см. AngryVillagerHandler), поэтому здесь добавлен явный гейт по
 * VILLAGER_MELEE_ENGAGE_RADIUS: пока житель далеко, ближний бой не "перехватывает" приоритет, и
 * работает бросок heavy_projectile; как только подошёл вплотную - переключается на мотыгу.
 * После реально нанесённого удара запускает AngryVillager.startRetreat, из-за чего сам же
 * временно перестаёт canUse() (см. isRetreating), уступая место VillagerRetreatGoal - так
 * получается цикл "подошёл-ударил-отбежал-кинул", а не "прилип и колотит без остановки".
 */
public class VillagerMeleeAttackGoal extends MeleeAttackGoal {

    private final Villager villager;

    public VillagerMeleeAttackGoal(Villager villager) {
        super(villager, AbilityConfig.VILLAGER_MOVE_SPEED, true);
        this.villager = villager;
    }

    @Override
    public boolean canUse() {
        return !AngryVillager.isRetreating(villager) && super.canUse() && isInMeleeRange();
    }

    @Override
    public boolean canContinueToUse() {
        return !AngryVillager.isRetreating(villager) && super.canContinueToUse() && isInMeleeRange();
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        boolean willAttack = canPerformAttack(target);
        super.checkAndPerformAttack(target);
        if (willAttack) {
            AngryVillager.startRetreat(villager);
        }
    }

    private boolean isInMeleeRange() {
        LivingEntity target = villager.getTarget();
        return target != null
                && villager.distanceToSqr(target) <= AbilityConfig.VILLAGER_MELEE_ENGAGE_RADIUS * AbilityConfig.VILLAGER_MELEE_ENGAGE_RADIUS;
    }
}
