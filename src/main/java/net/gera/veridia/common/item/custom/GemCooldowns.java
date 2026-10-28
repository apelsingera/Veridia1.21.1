package net.gera.veridia.common.item.custom;

import net.gera.veridia.common.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;
@SuppressWarnings("all")
public class GemCooldowns {
    public static final int COOLDOWN_TICKS = 20 * 105;
    public enum GemGroup {
        EYE_OF_SAURON(() -> List.<Item>of(
                ModItems.EYE_OF_SAURON.get(), ModItems.EYE_OF_SAURON_2.get(), ModItems.EYE_OF_SAURON_3.get(),
                ModItems.EYE_OF_SAURON_4.get(), ModItems.EYE_OF_SAURON_5.get())),
        SLOW_KILLER(() -> List.<Item>of(
                ModItems.SLOW_KILLER.get(), ModItems.SLOW_KILLER_2.get(), ModItems.SLOW_KILLER_3.get(),
                ModItems.SLOW_KILLER_4.get(), ModItems.SLOW_KILLER_5.get())),
        INFERNAL_STAFF(() -> List.<Item>of(
                ModItems.INFERNAL_STAFF.get(), ModItems.INFERNAL_STAFF_2.get(), ModItems.INFERNAL_STAFF_3.get(),
                ModItems.INFERNAL_STAFF_4.get(), ModItems.INFERNAL_STAFF_5.get()));

        private final Supplier<List<Item>> items;

        GemGroup(Supplier<List<Item>> items) {
            this.items = items;
        }

        public List<Item> items() {
            return items.get();
        }
    }

    public static GemGroup groupFor(Item item) {
        for (GemGroup group : GemGroup.values()) {
            if (group.items().contains(item)) {
                return group;
            }
        }
        return null;
    }

    public static boolean isOnCooldown(ServerPlayer player, Item currentItem) {
        return player.getCooldowns().isOnCooldown(currentItem);
    }

    public static void applyCooldown(ServerPlayer player, Item item) {
        applyCooldown(player, item, COOLDOWN_TICKS);
    }

    public static void applyCooldown(ServerPlayer player, Item item, int cooldownTicks) {
        GemGroup group = groupFor(item);
        if (group == null) {
            player.getCooldowns().addCooldown(item, cooldownTicks);
            return;
        }
        for (Item member : group.items()) {
            player.getCooldowns().addCooldown(member, cooldownTicks);
        }
    }
}
