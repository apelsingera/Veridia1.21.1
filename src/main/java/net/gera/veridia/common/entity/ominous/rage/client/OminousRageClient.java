package net.gera.veridia.common.entity.ominous.rage.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.entity.ominous.rage.OminousRage;
import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Что клиент знает о ярости зловещих воинов: до какого времени мира она идёт в текущем измерении
 * (приходит в {@code OminousRagePayload}). Небо ({@link OminousRageSky}) и музыка ({@link OminousRageMusic})
 * включаются, пока ярость идёт и игрок стоит в пепельном биоме.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public final class OminousRageClient {
    private static long endGameTime;

    private OminousRageClient() {
    }

    public static void receive(long rageEndGameTime) {
        endGameTime = rageEndGameTime;
    }

    /** Игрок в пепельном биоме, и в его измерении идёт ярость. */
    public static boolean isPlayerInRage() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        return level != null && player != null
                && level.getGameTime() < endGameTime
                && ModBiomes.isAshBiome(level.getBiome(player.blockPosition()));
    }

    /** Сколько тиков прошло с начала ярости в измерении игрока (по часам мира); ярость не идёт - -1. */
    public static long elapsedTicks() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.getGameTime() >= endGameTime) {
            return -1L;
        }
        return level.getGameTime() - (endGameTime - OminousRage.DURATION_TICKS);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        endGameTime = 0L;
    }
}
