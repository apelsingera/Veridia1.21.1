package net.gera.veridia.common.aibuff.villager.warrior.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorVillager;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorVillagerConfig;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorVillagerConfig.HeldItemTransform;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class WarriorVillagerHeldItemLayer extends RenderLayer<Villager, VillagerModel<Villager>> {

    private static final float VANILLA_Y_OFFSET = 0.4F;
    private static final float VANILLA_Z_OFFSET = -0.4F;

    private final ItemInHandRenderer itemInHandRenderer;

    public WarriorVillagerHeldItemLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent,
                                        ItemInHandRenderer itemInHandRenderer) {
        super(parent);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, Villager villager,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack stack = villager.getItemBySlot(EquipmentSlot.MAINHAND);
        if (stack.isEmpty()) return;

        if (WarriorVillager.isWarrior(villager) || isAxe(stack)) {
            renderInHand(poseStack, buffers, light, villager, stack);
        } else {
            renderCrossedArms(poseStack, buffers, light, villager, stack);
        }
    }

    private static boolean isAxe(ItemStack stack) {
        return stack.is(Items.IRON_AXE);
    }

    private void renderCrossedArms(PoseStack poseStack, MultiBufferSource buffers, int light,
                                   Villager villager, ItemStack stack) {
        poseStack.pushPose();
        poseStack.translate(0.0F, VANILLA_Y_OFFSET, VANILLA_Z_OFFSET);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        itemInHandRenderer.renderItem(villager, stack, ItemDisplayContext.GROUND, false, poseStack, buffers, light);
        poseStack.popPose();
    }

    private void renderInHand(PoseStack poseStack, MultiBufferSource buffers, int light,
                              Villager villager, ItemStack stack) {
        ModelPart arms = getParentModel().root().getChild("arms");

        HeldItemTransform transform = WarriorVillagerConfig.heldItemTransform(stack.getItem());

        poseStack.pushPose();
        arms.translateAndRotate(poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(transform.armsPitch()));

        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // Оси кадра после разворота "кость -> ладонь" НЕ совпадают с осями модели, и сдвиг это
        // касается так же, как повороты ниже: frame X -> вбок, frame Y -> вперёд-назад,
        // frame Z -> вверх-вниз. Поэтому поля названы right/forward/up, а не x/y/z: порядок
        // аргументов translate тот же, но теперь имя говорит правду о том, куда поедет предмет.
        poseStack.translate(transform.handRight() / 16.0F,
                transform.handForward() / 16.0F,
                transform.handUp() / 16.0F);

        poseStack.mulPose(Axis.XP.rotationDegrees(transform.pitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(transform.yaw()));
        poseStack.mulPose(Axis.YP.rotationDegrees(transform.roll()));

        if (transform.scale() != 1.0F) {
            poseStack.scale(transform.scale(), transform.scale(), transform.scale());
        }
        itemInHandRenderer.renderItem(villager, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                false, poseStack, buffers, light);
        poseStack.popPose();
    }
}
