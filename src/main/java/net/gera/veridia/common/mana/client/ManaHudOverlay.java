package net.gera.veridia.common.mana.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.mana.ManaHandler;
import net.gera.veridia.common.mana.ManaProgression;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public class ManaHudOverlay {

    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "mana_bar");

    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int GAP_FROM_HOTBAR = 6;
    private static final int BAR_WIDTH = 6;
    private static final int BAR_HEIGHT = 32;
    private static final int BOTTOM_MARGIN = 3;
    private static final int LABEL_GAP = 3;
    private static final float TEXT_SCALE = 0.6F;

    private static final int COLOR_BORDER = 0xFF000000;
    private static final int COLOR_BACKGROUND = 0xFF1A1A2E;
    private static final int COLOR_FILL = 0xFF4287F5;

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, ManaHudOverlay::render);
    }

    private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) return;

        Player player = minecraft.player;
        if (player == null) return;
        if (!ManaHandler.isUnlocked(player)) return;

        int mana = ManaHandler.getMana(player);
        int maxMana = ManaProgression.getMaxMana(player);
        int level = ManaProgression.progress(player);
        int maxLevel = ManaProgression.STEPS.size();

        int barX = guiGraphics.guiWidth() / 2 + HOTBAR_HALF_WIDTH + GAP_FROM_HOTBAR;
        int barBottom = guiGraphics.guiHeight() - BOTTOM_MARGIN;
        int barTop = barBottom - BAR_HEIGHT;

        guiGraphics.fill(barX - 1, barTop - 1, barX + BAR_WIDTH + 1, barBottom + 1, COLOR_BORDER);
        guiGraphics.fill(barX, barTop, barX + BAR_WIDTH, barBottom, COLOR_BACKGROUND);

        int filledHeight = Math.round(BAR_HEIGHT * (mana / (float) maxMana));
        if (filledHeight > 0) {
            guiGraphics.fill(barX, barBottom - filledHeight, barX + BAR_WIDTH, barBottom, COLOR_FILL);
        }

        int lineHeight = Math.round(minecraft.font.lineHeight * TEXT_SCALE);
        int labelY = barTop - LABEL_GAP - lineHeight;

        Component label = Component.translatable("hud.veridia.mana", mana, maxMana)
                .copy().append("  ")
                .append(Component.translatable("hud.veridia.mana_level", level, maxLevel));
        drawCenteredLabel(guiGraphics, minecraft, label, barX, labelY);
    }

    private static void drawCenteredLabel(GuiGraphics guiGraphics, Minecraft minecraft, Component label, int barX, int labelY) {
        int textWidth = minecraft.font.width(label);
        int scaledWidth = Math.round(textWidth * TEXT_SCALE);
        int labelX = barX + BAR_WIDTH / 2 - scaledWidth / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(labelX, labelY, 0);
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        guiGraphics.drawString(minecraft.font, label, 0, 0, 0xFFFFFF);
        guiGraphics.pose().popPose();
    }
}
