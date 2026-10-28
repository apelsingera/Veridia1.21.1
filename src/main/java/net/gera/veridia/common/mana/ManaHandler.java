package net.gera.veridia.common.mana;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ManaHandler {

    public static boolean isUnlocked(Player player) {
        return player.getData(ModAttachments.MANA_UNLOCKED);
    }
    public static boolean isGodMode(Player player) {
        return player.getData(ModAttachments.MANA_GOD_MODE);
    }

    public static int getMana(Player player) {
        if (!isUnlocked(player)) return 0;
        return player.getData(ModAttachments.MANA);
    }

    //public static boolean hasMana(Player player, int amount) {
    //    return getMana(player) >= amount;
    //}

    public static boolean consumeMana(ServerPlayer player, int amount) {
        if (!isUnlocked(player)) return false;
        if (isGodMode(player)) return true;
        int current = getMana(player);
        if (current < amount) {
            return false;
        }
        player.setData(ModAttachments.MANA, current - amount);
        return true;
    }

    public static void addMana(ServerPlayer player, int amount) {
        if (amount <= 0 || !isUnlocked(player)) return;
        int current = getMana(player);
        int max = ManaProgression.getMaxMana(player);
        int updated = Math.min(max, current + amount);
        if (updated != current) {
            player.setData(ModAttachments.MANA, updated);
        }
    }
}
