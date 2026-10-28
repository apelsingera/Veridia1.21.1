package net.gera.veridia.common.aibuff.villager.warrior;

import net.gera.veridia.common.aibuff.LongReachAttackGoal;
import net.minecraft.world.entity.npc.Villager;

/**
 * Ближний бой воина. Наследуемся от LongReachAttackGoal, потому что дистанция удара зависит от
 * выпавшего оружия: пике даём WarriorVillagerConfig.PIKE_REACH, всем остальным - MELEE_REACH.
 * <p>
 * Заодно это гасит дубль: LongReachHandler на EntityJoinLevelEvent вешает свой LongReachAttackGoal
 * всем, у кого есть тег "longreach" (а он у пикинёра есть), но перед этим проверяет, нет ли уже
 * такой Goal - наш подкласс эту проверку проходит.
 * <p>
 * Лучник (WarriorVillager.usesBow) сюда не заходит вообще: у него своя WarriorBowAttackGoal, а
 * общие MOVE/LOOK флаги иначе перетягивали бы управление на ближний бой.
 */
public class WarriorMeleeAttackGoal extends LongReachAttackGoal {

    private final Villager villager;

    public WarriorMeleeAttackGoal(Villager villager) {
        super(villager, WarriorVillagerConfig.MOVE_SPEED, true, WarriorVillager.meleeReach(villager));
        this.villager = villager;
    }

    @Override
    public boolean canUse() {
        return !WarriorVillager.usesBow(villager) && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !WarriorVillager.usesBow(villager) && super.canContinueToUse();
    }
}