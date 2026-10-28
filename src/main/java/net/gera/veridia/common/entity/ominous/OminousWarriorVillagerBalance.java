package net.gera.veridia.common.entity.ominous;

import net.gera.veridia.Veridia;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Зловещий воин против защитников деревни - жителей (воинов и вооружённых) и железных големов: даже толпой они не
 * должны его одолеть.
 * <ul>
 *   <li>Защитники бьют его слабо (стрелами тоже - считается стрелок), а он их - втрое сильнее.</li>
 *   <li>Раны от защитников затягиваются ({@link OminousWarriorEntity#woundedByVillageDefender}): лучники держат
 *       дистанцию, и без этого за долгий бой всё же добивали бы его. Урон от игроков не затягивается.</li>
 *   <li>Кровотечение и поджог из улучшений жителя-воина на него не действуют (WarriorVillagerHandler).</li>
 * </ul>
 * Против игроков и других мобов ничего не меняется.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class OminousWarriorVillagerBalance {
    // Урон от защитника по зловещему воину и от зловещего воина по защитнику (множители до брони)
    private static final float DAMAGE_FROM_DEFENDERS = 0.1F;
    private static final float DAMAGE_TO_DEFENDERS = 3.0F;

    private OminousWarriorVillagerBalance() {
    }

    private static boolean isVillageDefender(Entity entity) {
        return entity instanceof Villager || entity instanceof IronGolem;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof OminousWarriorEntity && isVillageDefender(attacker)) {
            event.setAmount(event.getAmount() * DAMAGE_FROM_DEFENDERS);
        } else if (isVillageDefender(event.getEntity()) && attacker instanceof OminousWarriorEntity) {
            event.setAmount(event.getAmount() * DAMAGE_TO_DEFENDERS);
        }
    }

    // Сколько на самом деле сняли (после брони и щита) - столько и затянется
    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof OminousWarriorEntity warrior && isVillageDefender(event.getSource().getEntity())) {
            warrior.woundedByVillageDefender(event.getNewDamage());
        }
    }
}
