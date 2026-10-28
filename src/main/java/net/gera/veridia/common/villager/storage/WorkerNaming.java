package net.gera.veridia.common.villager.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;

public final class WorkerNaming {
    private WorkerNaming() {
    }

    private static final String NAME_INDEX_KEY = "veridia_worker_name_index";
    public static final int NAME_COUNT = 50;

    /** Номер имени 1..NAME_COUNT, выданный жителю (ensureName); 0 - имени от мода нет. */
    public static int nameIndex(Villager villager) {
        return villager.getPersistentData().getInt(NAME_INDEX_KEY);
    }

    public static void ensureName(Villager villager) {
        if (villager.hasCustomName()) return;
        int index = 1 + villager.getRandom().nextInt(NAME_COUNT);
        villager.getPersistentData().putInt(NAME_INDEX_KEY, index);
        villager.setCustomName(Component.translatable("entity.veridia.worker_name." + index));
        villager.setCustomNameVisible(true);
    }
}
