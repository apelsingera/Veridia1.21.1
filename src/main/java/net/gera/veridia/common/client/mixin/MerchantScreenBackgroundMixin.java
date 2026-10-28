package net.gera.veridia.common.client.mixin;

import net.gera.veridia.common.client.VillagerMerchantGuiOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@code guiGraphics.blit(VILLAGER_LOCATION, i, j, 0, 0.0F, 0.0F, imageWidth, imageHeight, 512, 256)}
 */
@Mixin(MerchantScreen.class)
public class MerchantScreenBackgroundMixin {

    @Redirect(
            method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIFFIIII)V"),
            remap = false
    )
    private void veridia$swapBackground(GuiGraphics graphics, ResourceLocation location, int x, int y,
                                         int blitOffset, float u, float v, int width, int height,
                                         int textureWidth, int textureHeight) {
        graphics.blit(VillagerMerchantGuiOverlay.backgroundOverride(location),
                x, y, blitOffset, u, v, width, height, textureWidth, textureHeight);
    }
}
