package net.gera.veridia.common.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.item.custom.heavy_projectile.HeavyProjectileEntity;
import net.gera.veridia.common.item.custom.heavy_projectile.HeavyProjectileModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class HeavyProjectileRenderer extends EntityRenderer<HeavyProjectileEntity> {
    private final HeavyProjectileModel model;

    public HeavyProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new HeavyProjectileModel(context.bakeLayer(HeavyProjectileModel.LAYER_LOCATION));
    }
    @Override
    public void render(HeavyProjectileEntity pEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        if(!pEntity.isGrounded()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, pEntity.yRotO, pEntity.getYRot())));
            poseStack.mulPose(Axis.XP.rotationDegrees(pEntity.getRenderingRotation() * 5f));
            poseStack.translate(0, -1.0f, 0);
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(pEntity.groundedOffset.y));
            poseStack.mulPose(Axis.XP.rotationDegrees(pEntity.groundedOffset.x));
            poseStack.translate(0, -1.0f, 0);
        }

        VertexConsumer vertexconsumer = ItemRenderer.getFoilBufferDirect(
                buffer, this.model.renderType(this.getTextureLocation(pEntity)),false, false);
        this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(pEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(HeavyProjectileEntity heavyProjectileEntity) {
        return ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/entity/heavy_projectile/heavy_projectile.png");
    }
}