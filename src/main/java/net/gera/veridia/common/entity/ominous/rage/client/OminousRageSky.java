package net.gera.veridia.common.entity.ominous.rage.client;

import net.gera.veridia.Veridia;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Небо во время ярости зловещих воинов: поверх затемнения пепельного биома (AshGloom) небо светится фиолетовым,
 * туман лиловый, а облака ярко-пурпурные, будто сияют сами. Включается и гаснет плавно ({@link #amount}).
 * Цвет неба и облаков подкручивает ClientLevelAshGloomMixin, туман - {@link #onComputeFogColor}.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public final class OminousRageSky {
    // За сколько тиков небо разгорается после начала ярости и гаснет после её конца (или выхода из биома)
    private static final int FADE_IN_TICKS = 60;
    private static final int FADE_OUT_TICKS = 100;

    // Цвета при полной силе (RGB 0..1)
    private static final Vec3 SKY_COLOR = new Vec3(0.42D, 0.08D, 0.62D);
    private static final Vec3 CLOUD_COLOR = new Vec3(1.0D, 0.42D, 1.0D);
    private static final Vec3 FOG_COLOR = new Vec3(0.30D, 0.07D, 0.42D);

    private static float previousAmount;
    private static float currentAmount;

    private OminousRageSky() {
    }

    /** Сила фиолетового неба от 0 до 1 с плавным переходом между тиками. */
    public static float amount(float partialTick) {
        return (float) Mth.smoothstep(Mth.lerp(partialTick, previousAmount, currentAmount));
    }

    public static Vec3 tintSky(Vec3 skyColor, float amount) {
        return skyColor.lerp(SKY_COLOR, amount);
    }

    public static Vec3 tintClouds(Vec3 cloudColor, float amount) {
        return cloudColor.lerp(CLOUD_COLOR, amount);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        float step = OminousRageClient.isPlayerInRage() ? 1.0F / FADE_IN_TICKS : -1.0F / FADE_OUT_TICKS;
        previousAmount = currentAmount;
        currentAmount = Mth.clamp(currentAmount + step, 0.0F, 1.0F);
    }

    // После затемнения пепельного биома (у него обычный приоритет), чтобы фиолетовый ложился поверх
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        float amount = amount((float) event.getPartialTick());
        if (amount <= 0.0F) {
            return;
        }
        event.setRed(Mth.lerp(amount, event.getRed(), (float) FOG_COLOR.x));
        event.setGreen(Mth.lerp(amount, event.getGreen(), (float) FOG_COLOR.y));
        event.setBlue(Mth.lerp(amount, event.getBlue(), (float) FOG_COLOR.z));
    }
}
