package net.gera.veridia.common.entity.mutant.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.entity.mutant.Mutant;
import net.gera.veridia.common.entity.mutant.MutantEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public class MutantRobeLayer extends RenderLayer<MutantEntity, Mutant> {

    private static final float SCALE = 1.0F;
    private static final float Y_OFFSET = -0.9F + 14.0F / 16.0F;
    private static final float Z_OFFSET = -1.5F / 16.0F;
    private static final float YAW_FIX = 90.0F;
    private static final float SELF_YAW = 180.0F;

    private static final ResourceLocation NEWBIE_1 = tex("newbie_layer_1");
    private static final ResourceLocation NEWBIE_2 = tex("newbie_layer_2");
    private static final ResourceLocation EXPERIENCED_1 = tex("experienced_layer_1");
    private static final ResourceLocation EXPERIENCED_2 = tex("experienced_layer_2");

    private final HumanoidModel<MutantEntity> robe;

    public MutantRobeLayer(RenderLayerParent<MutantEntity, Mutant> parent, EntityModelSet models) {
        super(parent);
        this.robe = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, MutantEntity mutant,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        Textures textures = texturesFor(mutant.getItemBySlot(EquipmentSlot.CHEST).getItem());
        if (textures == null) {
            return;
        }

        robe.setupAnim(mutant, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        robe.head.visible = false;
        robe.hat.visible = false;

        Mutant model = getParentModel();
        copyLeg(model.getLeftLeg(), robe.leftLeg);
        copyLeg(model.getRightLeg(), robe.rightLeg);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(YAW_FIX));
        poseStack.translate(0.0F, Y_OFFSET, Z_OFFSET);
        poseStack.mulPose(Axis.YP.rotationDegrees(SELF_YAW));
        poseStack.scale(SCALE, SCALE, SCALE);
        VertexConsumer body = buffers.getBuffer(RenderType.armorCutoutNoCull(textures.layer1));
        robe.body.render(poseStack, body, light, OverlayTexture.NO_OVERLAY, -1);
        robe.rightArm.render(poseStack, body, light, OverlayTexture.NO_OVERLAY, -1);
        robe.leftArm.render(poseStack, body, light, OverlayTexture.NO_OVERLAY, -1);

        VertexConsumer legs = buffers.getBuffer(RenderType.armorCutoutNoCull(textures.layer2));
        robe.rightLeg.render(poseStack, legs, light, OverlayTexture.NO_OVERLAY, -1);
        robe.leftLeg.render(poseStack, legs, light, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
    }
    private static void copyLeg(ModelPart from, ModelPart to) {
        to.xRot = from.zRot;
        to.yRot = 0.0F;
        to.zRot = 0.0F;
    }

    @Nullable
    private static Textures texturesFor(Item item) {
        if (item == ModItems.NEWBIE_MANTLE_CHESTPLATE.get()) {
            return new Textures(NEWBIE_1, NEWBIE_2);
        }
        if (item == ModItems.EXPERIENCED_MANTLE_CHESTPLATE.get()) {
            return new Textures(EXPERIENCED_1, EXPERIENCED_2);
        }
        return null;
    }

    private record Textures(ResourceLocation layer1, ResourceLocation layer2) {
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/models/armor/" + name + ".png");
    }
}