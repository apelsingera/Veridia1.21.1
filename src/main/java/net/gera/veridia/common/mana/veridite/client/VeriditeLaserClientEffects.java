package net.gera.veridia.common.mana.veridite.client;

import net.gera.veridia.Veridia;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public class VeriditeLaserClientEffects {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final float MAX_SHAKE_DEGREES = 3.0F;
    private static int remainingShakeTicks = 0;
    private static int totalShakeTicks = 1;

    public static void startShake(int durationTicks) {
        remainingShakeTicks = durationTicks;
        totalShakeTicks = Math.max(durationTicks, 1);
    }
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remainingShakeTicks > 0) {
            remainingShakeTicks--;
        }
    }
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (remainingShakeTicks <= 0) return;

        float intensity = remainingShakeTicks / (float) totalShakeTicks;
        float shake = intensity * MAX_SHAKE_DEGREES;

        event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5F) * shake);
        event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5F) * shake * 0.6F);
        event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5F) * shake * 0.6F);
    }
}
