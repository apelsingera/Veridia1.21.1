package net.gera.veridia.common.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.entity.rustynail.RustyNailEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class RustyNailRenderer extends EntityRenderer<RustyNailEntity> {

    private static final ResourceLocation[] TEXTURES = {
            tex("rusty_nail_texture"),
            tex("rusty_nail_texture_2"),
            tex("rusty_nail_texture_3"),
    };

    private final RustyNailModel model;

    public RustyNailRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RustyNailModel(context.bakeLayer(RustyNailModel.LAYER_LOCATION));
    }

    @Override
    public void render(RustyNailEntity nail, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        if (!nail.isLanded()) {
            poseStack.mulPose(Axis.YP.rotationDegrees((nail.tickCount + partialTicks) * 14.0F % 360.0F));
        } else {
            poseStack.translate(0.0D, -0.35D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(Math.floorMod(nail.getId() * 73, 360)));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0.0D, -1.0D, 0.0D);
        poseStack.scale(0.7F, 0.7F, 0.7F);

        VertexConsumer vc = buffer.getBuffer(this.model.renderType(this.getTextureLocation(nail)));
        this.model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
        super.render(nail, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RustyNailEntity nail) {
        return TEXTURES[Math.floorMod(nail.getId(), TEXTURES.length)];
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/entity/rusty_nail/" + name + ".png");
    }
}