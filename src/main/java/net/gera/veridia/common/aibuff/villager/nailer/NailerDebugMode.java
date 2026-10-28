package net.gera.veridia.common.aibuff.villager.nailer;

import net.minecraft.world.entity.npc.Villager;

public final class NailerDebugMode {
    private NailerDebugMode() {
    }

    private static final String KEY = "veridia_nailer_infinite_mode";

    public static boolean isEnabled(Villager villager) {
        return villager.getPersistentData().getBoolean(KEY);
    }

    public static void setEnabled(Villager villager, boolean enabled) {
        villager.getPersistentData().putBoolean(KEY, enabled);
    }
}
