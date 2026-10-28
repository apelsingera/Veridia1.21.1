package net.gera.veridia.common.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Vector3f;

public class TanninFluidClientExtensions implements IClientFluidTypeExtensions {

    private static final ResourceLocation STILL_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation UNDERWATER_OVERLAY = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");
    private static final int TANNIN_TINT = 0xFF4A3222;
    private static final Vector3f FOG_COLOR = new Vector3f(0.08F, 0.05F, 0.02F);

    @Override
    public ResourceLocation getStillTexture() {
        return STILL_TEXTURE;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return FLOWING_TEXTURE;
    }

    @Override
    public int getTintColor() {
        return TANNIN_TINT;
    }

    @Override
    public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
        return UNDERWATER_OVERLAY;
    }

    @Override
    public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                    float darkenWorldAmount, Vector3f fluidFogColor) {
        return FOG_COLOR;
    }

    @Override
    public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick,
                                 float nearDistance, float farDistance, FogShape shape) {
        RenderSystem.setShaderFogStart(0.0F);
        RenderSystem.setShaderFogEnd(2.0F);
        RenderSystem.setShaderFogShape(FogShape.SPHERE);
    }
}