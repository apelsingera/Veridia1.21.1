package net.gera.veridia.common.block.entity.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.gera.veridia.Veridia;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Окно хранилища с панелью жителей (материалы и еда отличаются только меню): фон уровня, полоса прокачки и
 * WorkerPanelWidget с прокруткой. Фон - текстура 512x512 в двойном разрешении: окно занимает её левый верхний
 * угол размером imageWidth*2 x imageHeight*2.
 */
public abstract class WorkerStorageScreen<M extends AbstractContainerMenu & WorkerPanelMenuAccess>
        extends AbstractContainerScreen<M> {

    private static final ResourceLocation BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager_xp_bar.png");
    private static final int BAR_X = 12;
    private static final int BAR_Y = 9;
    private static final int BAR_WIDTH = 52;
    private static final int BAR_HEIGHT = 7;

    /** Сторона текстуры фона в пикселях (текстуры уровней сделаны 512x512, окно 176x166 занимает её часть вдвое крупнее). */
    private static final int BACKGROUND_TEXTURE_SIZE = 512;

    private final WorkerPanelWidget workerPanel = new WorkerPanelWidget();

    protected WorkerStorageScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation guiTexture = menu.layout().texture();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, guiTexture);

        guiGraphics.blit(guiTexture, leftPos, topPos, imageWidth, imageHeight, 0.0F, 0.0F,
                imageWidth * WorkerPanelWidget.TEXTURE_PER_GUI_PIXEL, imageHeight * WorkerPanelWidget.TEXTURE_PER_GUI_PIXEL,
                BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE);

        renderProgressBar(guiGraphics);
        workerPanel.render(guiGraphics, this.font, menu, leftPos, topPos);
    }

    private void renderProgressBar(GuiGraphics guiGraphics) {
        int filled = menu.getScaledProgress(BAR_WIDTH);
        if (filled <= 0) {
            return;
        }
        guiGraphics.blit(BAR_TEXTURE, leftPos + BAR_X, topPos + BAR_Y, 0, 0, filled, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int hovered = workerPanel.hitTest(menu, mouseX - leftPos, mouseY - topPos);
        if (hovered >= 0) {
            guiGraphics.renderTooltip(this.font, WorkerPanelWidget.tooltipFor(hovered), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            double relX = mouseX - leftPos;
            double relY = mouseY - topPos;
            if (workerPanel.mousePressed(menu, relX, relY)) {
                return true;
            }
            int action = workerPanel.hitTest(menu, relX, relY);
            if (action == WorkerPanelWidget.ACTION_INFO) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    WorkerPanelWidget.sendInfoMessage(this.minecraft.player);
                }
                return true;
            }
            if (action >= 0) {
                assert this.minecraft != null;
                assert this.minecraft.gameMode != null;
                this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && workerPanel.mouseDragged(menu, mouseY - topPos)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            workerPanel.mouseReleased();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (workerPanel.mouseScrolled(menu, mouseX - leftPos, mouseY - topPos, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
