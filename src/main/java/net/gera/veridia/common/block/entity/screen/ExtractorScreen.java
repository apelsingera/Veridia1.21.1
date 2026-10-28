package net.gera.veridia.common.block.entity.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.gera.veridia.Veridia;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class ExtractorScreen extends AbstractContainerScreen<ExtractorMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/extractor/extractor_gui.png");
    private static final ResourceLocation PROGRESS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/extractor_progress.png");

    private static final int PROGRESS_W = 2;
    private static final int PROGRESS_H = 20;
    private static final int PROGRESS_X = 101;
    private static final int PROGRESS_Y = 33;

    private static final int TANK_X = 65;
    private static final int TANK_Y = 41;
    private static final int TANK_W = 16;
    private static final int TANK_H = 33;

    public ExtractorScreen(ExtractorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderFluidTank(guiGraphics, x, y);
        renderProgress(guiGraphics, x, y);
    }

    private void renderProgress(GuiGraphics guiGraphics, int x, int y) {
        int filled = menu.getScaledProgress(PROGRESS_H);
        if (filled <= 0) {
            return;
        }
        guiGraphics.blit(PROGRESS_TEXTURE,
                x + PROGRESS_X, y + PROGRESS_Y,
                0.0F, 0.0F,
                PROGRESS_W, filled,
                PROGRESS_W, PROGRESS_H);
    }

    private void renderFluidTank(GuiGraphics guiGraphics, int x, int y) {
        FluidTank tank = menu.blockEntity.getInputTank();
        FluidStack stack = tank.getFluid();
        if (stack.isEmpty() || tank.getCapacity() <= 0) {
            return;
        }

        int h = (int) ((long) TANK_H * stack.getAmount() / tank.getCapacity());
        if (h <= 0) {
            return;
        }

        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(stack.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ext.getStillTexture(stack));
        int tint = ext.getTintColor(stack);
        float alpha = FastColor.ARGB32.alpha(tint) == 0 ? 1.0F : FastColor.ARGB32.alpha(tint) / 255.0F;

        int left = x + TANK_X;
        int bottom = y + TANK_Y + TANK_H;
        int top = bottom - h;

        RenderSystem.setShaderColor(
                FastColor.ARGB32.red(tint) / 255.0F,
                FastColor.ARGB32.green(tint) / 255.0F,
                FastColor.ARGB32.blue(tint) / 255.0F,
                alpha);
        guiGraphics.enableScissor(left, top, left + TANK_W, bottom);
        for (int oy = 0; oy < h + 16; oy += 16) {
            guiGraphics.blit(left, bottom - 16 - oy, 0, TANK_W, 16, sprite);
        }
        guiGraphics.disableScissor();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            FluidTank tank = menu.blockEntity.getInputTank();
            Component name = tank.getFluid().isEmpty()
                    ? Component.translatable("tooltip.veridia.extractor.empty")
                    : tank.getFluid().getHoverName();
            guiGraphics.renderComponentTooltip(font, List.of(
                    name,
                    Component.literal(tank.getFluidAmount() + " / " + tank.getCapacity() + " mB")
                            .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}