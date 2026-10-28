package net.gera.veridia.common.aibuff.handlers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.villager.farmer.FarmerPlantGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Set;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class FarmerVillagerHandler {

    /** Ванильное условие Activity.WORK у жителя (профессия с рабочим местом) - см.
     * VillagerGoalPackages/Brain: активность включается, когда есть JOB_SITE. Возвращаем это
     * условие, когда фермер не в пути к месту посадки - см. blockWorkWhileTraveling. */
    private static final Set<Pair<MemoryModuleType<?>, MemoryStatus>> WORK_NORMAL =
            ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT));
    /** Условие, которое у жителя не выполнится никогда - см. WarriorVillagerHandler.PANIC_NEVER,
     * тот же приём. */
    private static final Set<Pair<MemoryModuleType<?>, MemoryStatus>> WORK_NEVER =
            ImmutableSet.of(Pair.of(MemoryModuleType.ADMIRING_ITEM, MemoryStatus.VALUE_PRESENT));

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;
        if (villager.getVillagerData().getProfession() != VillagerProfession.FARMER) return;

        boolean hasPlantGoal = villager.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof FarmerPlantGoal);
        if (!hasPlantGoal) {
            villager.goalSelector.addGoal(2, new FarmerPlantGoal(villager));
        }

        blockWorkWhileTraveling(villager);
    }

    /**
     * Пока FarmerPlantGoal реально ведёт фермера сажать саженец (склад -> поиск места -> дорога ->
     * посадка), блокируем ванильную Activity.WORK (HarvestFarmland/WorkAtComposter, см.
     * ComposterBlockMixin/HarvestFarmlandMixin) - иначе она регулярно перехватывает WALK_TARGET
     * ближайшей грядкой/компостером прямо посреди похода за забор, и фермер "отвлекается",
     * так и не дойдя до места посадки (её постоянно тянет обратно к грядкам - отсюда и "не уходит
     * далеко от забора"). Приём тот же, что WarriorVillagerHandler.blockPanicActivity - подменяем
     * ТОЛЬКО условие активности на заведомо невыполнимое, список поведений (ImmutableList.of())
     * не трогаем. Делается каждый тик, потому что мозг пересоздаётся при смене профессии/перезаходе.
     */
    private static void blockWorkWhileTraveling(Villager villager) {
        Set<Pair<MemoryModuleType<?>, MemoryStatus>> requirements =
                FarmerPlantGoal.isTravelingToPlant(villager) ? WORK_NEVER : WORK_NORMAL;
        villager.getBrain().addActivityWithConditions(Activity.WORK, ImmutableList.of(), requirements);
    }
}
