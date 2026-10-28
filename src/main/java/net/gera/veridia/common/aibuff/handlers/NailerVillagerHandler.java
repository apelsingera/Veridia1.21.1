package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.villager.nailer.NailerBuildGoal;
import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class NailerVillagerHandler {

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;
        if (villager.getVillagerData().getProfession() != ModVillagers.NAILER.value()) return;

        boolean hasBuildGoal = villager.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof NailerBuildGoal);
        if (!hasBuildGoal) {
            villager.goalSelector.addGoal(2, new NailerBuildGoal(villager));
        }
    }
}
