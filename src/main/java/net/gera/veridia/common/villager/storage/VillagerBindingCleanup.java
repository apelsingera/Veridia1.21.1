package net.gera.veridia.common.villager.storage;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.types.BuildingMaterialsBlockEntity;
import net.gera.veridia.common.block.entity.types.FoodStorageBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * Умер или исчез житель (убит, удалён командой) - его место в хранилище освобождается сразу, а следующие места
 * сдвигаются вверх (WorkerSlots.release). Раньше место оставалось занятым навсегда: UUID умершего жителя лежал
 * в хранилище, и места быстро кончались.
 * Выгрузка жителя вместе с чанком (UNLOADED_*) жителя не убирает, это не конец его работы.
 * Если хранилище в этот момент выгружено, дотянуться до него нельзя: смерть записывается в VillagerGraveyard,
 * и хранилище снимет это место при первой проверке (VillagerStorageBinding.pruneGhosts).
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class VillagerBindingCleanup {

    private VillagerBindingCleanup() {
    }

    @SubscribeEvent
    public static void onVillagerLeaves(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (villager.getRemovalReason() == null || !villager.getRemovalReason().shouldDestroy()) {
            return;
        }
        boolean wasWorker = VillagerStorageBinding.hasAnyBinding(villager)
                || StorageKind.anyAccepts(villager.getVillagerData().getProfession());
        VillagerStorageBinding.release(villager, level, StorageKind.MATERIALS,
                ModBlocks.BUILDING_MATERIALS.get(), BuildingMaterialsBlockEntity.class);
        VillagerStorageBinding.release(villager, level, StorageKind.FOOD,
                ModBlocks.FOOD_STORAGE.get(), FoodStorageBlockEntity.class);
        if (wasWorker) {
            VillagerGraveyard.get(level).bury(villager.getUUID(), level.getGameTime());
        }
    }
}
