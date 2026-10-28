package net.gera.veridia.common.block.entity.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.gera.veridia.Veridia;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RunicPedestalScreen extends AbstractContainerScreen<RunicPedestalMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/runic_pedestal/runic_pedestal_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/bar_progress.png");
    private static final int CRAFT_BUTTON_X = 114;
    private static final int CRAFT_BUTTON_Y = 37;
    private static final int CRAFT_BUTTON_SIZE = 16;
    private static final int ARROW_X = 114;
    private static final int ARROW_Y = 37;

    public RunicPedestalScreen(RunicPedestalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderProgressArrow(guiGraphics, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCrafting()) {
            guiGraphics.blit(ARROW_TEXTURE, x + ARROW_X, y + ARROW_Y, 0, 0, menu.getScaledArrowProgress(), 6, 42, 6);
        }
    }

    @Override
    public void render( GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);

        if (isHoveringCraftButton(pMouseX, pMouseY)) {
            pGuiGraphics.renderTooltip(this.font, craftButtonTooltip(), pMouseX, pMouseY);
        }
    }

    private Component craftButtonTooltip() {
        int xpLevels = menu.getCurrentRecipeXpLevels();
        if (xpLevels <= 0) {
            return Component.translatable("gui.veridia.runic_pedestal_craft");
        }
        return Component.translatable("gui.veridia.runic_pedestal_craft_cost", xpLevels);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHoveringCraftButton(mouseX, mouseY)) {
            assert this.minecraft != null;
            assert this.minecraft.gameMode != null;
            this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RunicPedestalMenu.CRAFT_BUTTON_ID);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHoveringCraftButton(double mouseX, double mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        return mouseX >= x + CRAFT_BUTTON_X && mouseX < x + CRAFT_BUTTON_X + CRAFT_BUTTON_SIZE
                && mouseY >= y + CRAFT_BUTTON_Y && mouseY < y + CRAFT_BUTTON_Y + CRAFT_BUTTON_SIZE;
    }
}