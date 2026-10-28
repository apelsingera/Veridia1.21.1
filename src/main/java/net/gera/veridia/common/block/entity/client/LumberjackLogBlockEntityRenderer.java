package net.gera.veridia.common.block.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.gera.veridia.common.block.LumberjackLogBlock;
import net.gera.veridia.common.block.entity.types.LumberjackLogBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class LumberjackLogBlockEntityRenderer implements BlockEntityRenderer<LumberjackLogBlockEntity> {

    private static final double AXE_HEIGHT = 1.2;
    private static final double AXE_OUTWARD_OFFSET = 0.4;
    private static final float AXE_SCALE = 0.8F;
    private static final float AXE_ROTATION_X = 150.0F;
    private static final float AXE_ROTATION_Y = -90.0F;
    private static final float AXE_ROTATION_Z = 0.0F;

    public LumberjackLogBlockEntityRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }
    @Override
    public void render(LumberjackLogBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        Direction facing = blockEntity.getBlockState().getValue(LumberjackLogBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5, AXE_HEIGHT, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.translate(0, 0, -AXE_OUTWARD_OFFSET);
        poseStack.mulPose(Axis.XP.rotationDegrees(AXE_ROTATION_X));
        poseStack.mulPose(Axis.YP.rotationDegrees(AXE_ROTATION_Y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(AXE_ROTATION_Z));
        poseStack.scale(AXE_SCALE, AXE_SCALE, AXE_SCALE);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(Items.IRON_AXE), ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, bufferSource,
                blockEntity.getLevel(), (int) blockEntity.getBlockPos().asLong());

        poseStack.popPose();
    }
}