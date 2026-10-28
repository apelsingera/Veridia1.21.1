package net.gera.veridia.common.entity.mutant.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.gera.veridia.common.entity.mutant.MutantEntity;
import net.gera.veridia.common.entity.mutant.Mutant;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class MutantHeldItemLayer extends RenderLayer<MutantEntity, Mutant> {
    private static final double OFFSET_ALONG_ARM = 0.85D;
    private static final float ARM_AXIS_REALIGN_Z_DEGREES = -90.0F;

    private static final double HAND_NUDGE_X = 0.0D;
    private static final double HAND_NUDGE_Y = -0.3D;
    private static final double HAND_NUDGE_Z = 0.0D;

    private static final double FOOD_OFFSET_ALONG_ARM = 0.72D;
    private static final float FOOD_ARM_AXIS_REALIGN_Z_DEGREES = 180.0F;
    private static final double FOOD_HAND_NUDGE_X = 0.0D;
    private static final double FOOD_HAND_NUDGE_Y = 0.0D;
    private static final double FOOD_HAND_NUDGE_Z = 0.0D;

    private static final float HELD_ITEM_EXTRA_PITCH_DEGREES = 0.0F;
    private static final float HELD_ITEM_EXTRA_YAW_DEGREES = 0.0F;
    private static final float HELD_ITEM_EXTRA_ROLL_DEGREES = 90.0F;

    private final ItemInHandRenderer itemInHandRenderer;

    public MutantHeldItemLayer(RenderLayerParent<MutantEntity, Mutant> renderer, ItemInHandRenderer itemInHandRenderer) {
        super(renderer);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, MutantEntity entity,
                        float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                        float netHeadYaw, float headPitch) {
        ItemStack stack = entity.getStolenItem();
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        if (stack.has(DataComponents.FOOD)) {
            placeInFrontLeftHand(poseStack);
        } else {
            placeInSecondLeftHand(poseStack);
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(HELD_ITEM_EXTRA_PITCH_DEGREES));
        poseStack.mulPose(Axis.YP.rotationDegrees(HELD_ITEM_EXTRA_YAW_DEGREES));
        poseStack.mulPose(Axis.ZP.rotationDegrees(HELD_ITEM_EXTRA_ROLL_DEGREES));

        this.itemInHandRenderer.renderItem(entity, stack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                true, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    private void placeInSecondLeftHand(PoseStack poseStack) {
        Mutant model = this.getParentModel();
        walkToArmCube(poseStack, model.getLeftArm2(), model.getLeftArm2Cube());
        poseStack.translate(OFFSET_ALONG_ARM, 0.0D, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(ARM_AXIS_REALIGN_Z_DEGREES));
        applyHandSpace(poseStack, HAND_NUDGE_X, HAND_NUDGE_Y, HAND_NUDGE_Z);
    }

    private void placeInFrontLeftHand(PoseStack poseStack) {
        Mutant model = this.getParentModel();
        walkToArmCube(poseStack, model.getLeftArm(), model.getLeftArmCube());
        poseStack.translate(0.0D, -FOOD_OFFSET_ALONG_ARM, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(FOOD_ARM_AXIS_REALIGN_Z_DEGREES));
        applyHandSpace(poseStack, FOOD_HAND_NUDGE_X, FOOD_HAND_NUDGE_Y, FOOD_HAND_NUDGE_Z);
    }

    private void walkToArmCube(PoseStack poseStack, ModelPart arm, ModelPart armCube) {
        Mutant model = this.getParentModel();
        model.getAll().translateAndRotate(poseStack);
        model.getBody().translateAndRotate(poseStack);
        model.getArms().translateAndRotate(poseStack);
        arm.translateAndRotate(poseStack);
        armCube.translateAndRotate(poseStack);
    }

    private static void applyHandSpace(PoseStack poseStack, double nudgeX, double nudgeY, double nudgeZ) {
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(nudgeX, nudgeY, nudgeZ);
    }
}