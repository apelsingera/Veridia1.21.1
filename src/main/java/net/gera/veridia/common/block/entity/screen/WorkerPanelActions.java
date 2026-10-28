package net.gera.veridia.common.block.entity.screen;

import net.gera.veridia.common.aibuff.villager.farmer.FarmerPlantGoal;
import net.gera.veridia.common.aibuff.villager.lumberjack.LumberjackChopGoal;
import net.gera.veridia.common.aibuff.villager.nailer.NailerBuildGoal;
import net.gera.veridia.common.villager.storage.VillagerEmployment;
import net.gera.veridia.common.villager.storage.WorkerSlots;
import net.gera.veridia.common.villager.storage.WorkerStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Общая обработка нажатий на панель жителей (солнышко/on/off/вопрос) - вызывается из
 * BuildingMaterialsMenu.clickMenuButton и FoodStorageMenu.clickMenuButton, чтобы не дублировать
 * логику в двух местах. id кодируется как slotMask * 4 + action, см. ACTION_*. slotMask - битовая
 * маска слотов (один ряд панели = один житель, в маске один бит, см. WorkerPanelWidget.workerRows),
 * действие применяется ко всем жителям, чьи слоты входят в маску.
 */
public final class WorkerPanelActions {

    private WorkerPanelActions() {
    }

    public static final int ACTION_GLOW = 0;
    public static final int ACTION_ON = 1;
    public static final int ACTION_OFF = 2;
    public static final int ACTION_STATUS = 3;
    public static final int ACTIONS_PER_SLOT = 4;

    public static boolean handleClick(WorkerStorage storage, Player player, Level level, int id) {
        int slotMask = id / ACTIONS_PER_SLOT;
        int action = id % ACTIONS_PER_SLOT;
        int maxMask = (1 << WorkerSlots.MAX_WORKERS) - 1;
        if (slotMask <= 0 || slotMask > maxMask || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        boolean handledAny = false;
        for (int slot = 0; slot < WorkerSlots.MAX_WORKERS; slot++) {
            if ((slotMask & (1 << slot)) == 0) continue;
            UUID workerId = storage.getWorkers().get(slot);
            if (workerId == null) continue;
            handledAny = true;

            switch (action) {
                case ACTION_GLOW -> VillagerEmployment.glow(serverLevel, workerId, VillagerEmployment.GLOW_DURATION_TICKS);
                case ACTION_ON -> withVillager(serverLevel, workerId, v -> VillagerEmployment.setWorkEnabled(v, true));
                case ACTION_OFF -> withVillager(serverLevel, workerId, v -> VillagerEmployment.setWorkEnabled(v, false));
                case ACTION_STATUS -> withVillager(serverLevel, workerId, v -> sendStatus(player, v));
                default -> {
                }
            }
        }
        return handledAny;
    }

    private static void withVillager(ServerLevel level, UUID id, java.util.function.Consumer<Villager> action) {
        if (level.getEntity(id) instanceof Villager villager) {
            action.accept(villager);
        }
    }

    private static void sendStatus(Player player, Villager villager) {
        Component profession = professionName(villager.getVillagerData().getProfession());
        Component name = villager.hasCustomName()
                ? villager.getCustomName()
                : Component.translatable("gui.veridia.worker_panel.status.unnamed");
        player.sendSystemMessage(Component.translatable(
                "gui.veridia.worker_panel.status_line", profession, name, describeActivity(villager)));
    }

    private static Component describeActivity(Villager villager) {
        for (Goal goal : goalsOf(villager)) {
            if (goal instanceof LumberjackChopGoal g) return g.statusText();
            if (goal instanceof FarmerPlantGoal g) return g.statusText();
            if (goal instanceof NailerBuildGoal g) return g.statusText();
        }
        return Component.translatable("gui.veridia.worker_panel.status.idle");
    }

    private static Iterable<Goal> goalsOf(Villager villager) {
        return villager.goalSelector.getAvailableGoals().stream().map(wrapped -> wrapped.getGoal())::iterator;
    }

    private static Component professionName(VillagerProfession profession) {
        ResourceLocation key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        if (key == null) return Component.literal("?");
        String suffix = key.getNamespace().equals("minecraft") ? key.getPath() : key.getNamespace() + "." + key.getPath();
        return Component.translatable("entity.minecraft.villager." + suffix);
    }
}
