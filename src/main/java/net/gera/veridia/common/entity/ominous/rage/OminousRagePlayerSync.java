package net.gera.veridia.common.entity.ominous.rage;

import net.gera.veridia.Veridia;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Сообщает игроку, идёт ли ярость в его измерении: при входе в мир, смене измерения и возрождении. */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class OminousRagePlayerSync {
    private OminousRagePlayerSync() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OminousRage.sendState(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OminousRage.sendState(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OminousRage.sendState(player);
        }
    }
}
