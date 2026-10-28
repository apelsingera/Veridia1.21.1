package net.gera.veridia.common.item.custom.gems.lvl2;

import net.gera.veridia.Veridia;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class EyeOfSauronHandler {

    public static final String FIRING_UNTIL_KEY = "veridia_eye_of_sauron_lvl2_firing_until";
    public static final String FIRING_SUPER_KEY = "veridia_eye_of_sauron_lvl2_firing_super";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        long firingUntil = player.getPersistentData().getLong(FIRING_UNTIL_KEY);
        if (firingUntil == 0L) return;

        if (player.level().getGameTime() >= firingUntil) {
            player.getPersistentData().remove(FIRING_UNTIL_KEY);
            player.getPersistentData().remove(FIRING_SUPER_KEY);
            return;
        }

        EyeOfSauronItem.fireLaser(player);
    }
}