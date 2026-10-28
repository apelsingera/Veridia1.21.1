package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.villager.golemhealer.HealerVillagerGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Любой житель с профессией оружейника (VillagerProfession.WEAPONSMITH) получает HealerVillagerGoal:
 * пока рядом есть раненый железный голем, житель достаёт железный слиток, бежит к голему и лечит его
 * (см. HealerVillagerGoal). Схема "ленивого" навешивания Goal прямо на тике - та же, что и в
 * AngryVillagerHandler; @EventBusSubscriber без указания шины сам регистрирует это на игровой шине.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public class HealerVillagerHandler {

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;
        if (villager.getVillagerData().getProfession() != VillagerProfession.WEAPONSMITH) return;

        boolean hasHealGoal = villager.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof HealerVillagerGoal);
        if (!hasHealGoal) {
            villager.goalSelector.addGoal(2, new HealerVillagerGoal(villager));
        }
    }
}
