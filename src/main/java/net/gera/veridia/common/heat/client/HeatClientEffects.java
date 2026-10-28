package net.gera.veridia.common.heat.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.gera.veridia.Veridia;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Что игрок видит от жара: красная виньетка по краям экрана и сужение угла обзора. Сила берётся из
 * {@link HeatClientState} (её присылает сервер, она нарастает и спадает плавно). Замедление, урон и красная надпись
 * делает сервер.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public final class HeatClientEffects {
    // ---------- Настройки ----------
    // Насколько сужается угол обзора на полной силе жара (доля: 0.12 - на 12%)
    private static final float FOV_REDUCTION = 0.12F;
    // Насколько непрозрачна виньетка по самым краям на полной силе жара (от 0 до 1)
    private static final float VIGNETTE_MAX_ALPHA = 0.45F;
    // Форма виньетки: до какого расстояния от центра экрана (1 - до середины стороны) он чистый и где краснота
    // становится полной; цвет виньетки (красный, RGB)
    private static final float VIGNETTE_INNER_RADIUS = 0.55F;
    private static final float VIGNETTE_OUTER_RADIUS = 1.3F;
    private static final int VIGNETTE_RED = 200;
    private static final int VIGNETTE_GREEN = 12;
    private static final int VIGNETTE_BLUE = 0;
    // Размер текстуры виньетки (пикселей): растягивается на весь экран с плавной интерполяцией
    private static final int VIGNETTE_TEXTURE_SIZE = 128;

    private static final ResourceLocation VIGNETTE_TEXTURE_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "heat_vignette_texture");
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "heat_vignette");

    // Текстура создаётся при первой отрисовке (нужен рабочий рендер); true, когда она уже есть
    private static boolean vignetteTextureReady;

    private HeatClientEffects() {
    }

    // Виньетка рисуется под панелью предметов, чтобы не закрывать интерфейс
    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.HOTBAR, LAYER_ID, HeatClientEffects::renderVignette);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        HeatClientState.tick();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        HeatClientState.reset();
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        float intensity = HeatClientState.intensity(1.0F);
        if (intensity > 0.0F) {
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F - FOV_REDUCTION * intensity));
        }
    }

    private static void renderVignette(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        float intensity = HeatClientState.intensity(deltaTracker.getGameTimeDeltaPartialTick(false));
        if (intensity <= 0.01F || Minecraft.getInstance().options.hideGui) {
            return;
        }
        ensureVignetteTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, VIGNETTE_MAX_ALPHA * intensity);
        guiGraphics.blit(VIGNETTE_TEXTURE_ID, 0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), 0.0F, 0.0F,
                VIGNETTE_TEXTURE_SIZE, VIGNETTE_TEXTURE_SIZE, VIGNETTE_TEXTURE_SIZE, VIGNETTE_TEXTURE_SIZE);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    // Красный радиальный градиент: прозрачный в середине, к краям и углам всё плотнее (альфа по smoothstep от расстояния)
    private static void ensureVignetteTexture() {
        if (vignetteTextureReady) {
            return;
        }
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, VIGNETTE_TEXTURE_SIZE, VIGNETTE_TEXTURE_SIZE, false);
        for (int y = 0; y < VIGNETTE_TEXTURE_SIZE; y++) {
            for (int x = 0; x < VIGNETTE_TEXTURE_SIZE; x++) {
                float dx = (x + 0.5F) / VIGNETTE_TEXTURE_SIZE * 2.0F - 1.0F;
                float dy = (y + 0.5F) / VIGNETTE_TEXTURE_SIZE * 2.0F - 1.0F;
                float distance = Mth.sqrt(dx * dx + dy * dy);
                float t = Mth.clamp((distance - VIGNETTE_INNER_RADIUS) / (VIGNETTE_OUTER_RADIUS - VIGNETTE_INNER_RADIUS), 0.0F, 1.0F);
                int alpha = Math.round(t * t * (3.0F - 2.0F * t) * 255.0F);
                // NativeImage хранит цвет как ABGR
                image.setPixelRGBA(x, y, (alpha << 24) | (VIGNETTE_BLUE << 16) | (VIGNETTE_GREEN << 8) | VIGNETTE_RED);
            }
        }
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(true, false);
        Minecraft.getInstance().getTextureManager().register(VIGNETTE_TEXTURE_ID, texture);
        vignetteTextureReady = true;
    }
}
