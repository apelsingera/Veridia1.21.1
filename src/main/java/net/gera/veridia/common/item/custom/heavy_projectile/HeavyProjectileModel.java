package net.gera.veridia.common.item.custom.heavy_projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.gera.veridia.Veridia;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class HeavyProjectileModel extends EntityModel<HeavyProjectileEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,"heavy_projectile"), "main");
    private final ModelPart heavyProjectile;

    public HeavyProjectileModel(ModelPart root) {
        this.heavyProjectile = root.getChild("heavyProjectile");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition heavyProjectile = partdefinition.addOrReplaceChild("heavyProjectile",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-8.0F, -8.0F, 0.0F, 8.0F, 8.0F, 8.0F,
                                new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(HeavyProjectileEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        heavyProjectile.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}