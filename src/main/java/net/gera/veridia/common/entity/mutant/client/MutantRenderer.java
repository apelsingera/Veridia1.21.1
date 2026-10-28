package net.gera.veridia.common.entity.mutant.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.entity.mutant.MutantEntity;
import net.gera.veridia.common.entity.mutant.Mutant;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MutantRenderer extends MobRenderer<MutantEntity, Mutant> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/entity/mutant/mutant.png");
    private static final ResourceLocation TEXTURE_HEAL =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/entity/mutant/mutant_healed.png");

    private static final float MODEL_FORWARD_CORRECTION_DEGREES = -90.0F;

    public MutantRenderer(EntityRendererProvider.Context context) {
        super(context, new Mutant(context.bakeLayer(Mutant.LAYER_LOCATION)), 0.5F);
        this.addLayer(new MutantHeldItemLayer(this, context.getItemInHandRenderer()));
        this.addLayer(new MutantRobeLayer(this, context.getModelSet()));
    }

    @Override
    protected void setupRotations(MutantEntity entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(MODEL_FORWARD_CORRECTION_DEGREES));
    }
    @Override
    protected float getFlipDegrees(MutantEntity entity) {
        return 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(MutantEntity entity) {
        return entity.isHealedForm() ? TEXTURE_HEAL : TEXTURE;
    }
}