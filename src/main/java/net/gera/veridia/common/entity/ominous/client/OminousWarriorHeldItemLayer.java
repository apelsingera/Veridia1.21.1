package net.gera.veridia.common.entity.ominous.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.gera.veridia.common.entity.ominous.OminousWarrior;
import net.gera.veridia.common.entity.ominous.OminousWarriorEntity;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Предметы в руках зловещего воина: любой предмет в основной и второй руке (мечи, инструменты, щиты, блоки, еда,
 * всё остальное) рисуется так же, как у игрока и гуманоидных мобов, то есть по настройкам отображения самой
 * модели предмета (third person). Модель ставит систему координат в руку ({@link OminousWarrior#translateToHand}):
 * предмет держится в кисти, у нижнего края руки, и двигается вместе с её анимацией.
 * Ниже подстройка хвата: сдвиги в системе руки (вперёд - минус Z, вниз - плюс Y), все в блоках.
 */
public class OminousWarriorHeldItemLayer extends ItemInHandLayer<OminousWarriorEntity, OminousWarrior> {
    // Куда сдвинуть предмет относительно ванильного хвата: наружу от тела, вниз вдоль руки, вперёд
    private static final float GRIP_OUTWARD = 0.0F;
    private static final float GRIP_DOWN = 0.0F;
    private static final float GRIP_FORWARD = 0.0F;
    // Размер предмета в руке: воин крупнее игрока, при необходимости можно чуть увеличить
    private static final float ITEM_SCALE = 1.0F;

    private final ItemInHandRenderer itemInHandRenderer;

    public OminousWarriorHeldItemLayer(RenderLayerParent<OminousWarriorEntity, OminousWarrior> renderer,
                                       ItemInHandRenderer itemInHandRenderer) {
        super(renderer, itemInHandRenderer);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    protected void renderArmWithItem(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                                     HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (stack.isEmpty()) {
            return;
        }
        boolean leftArm = arm == HumanoidArm.LEFT;
        poseStack.pushPose();
        this.getParentModel().translateToHand(arm, poseStack);
        // Подстройка хвата: наружу для левой руки это плюс X, для правой минус
        poseStack.translate(leftArm ? GRIP_OUTWARD : -GRIP_OUTWARD, GRIP_DOWN, -GRIP_FORWARD);
        // Дальше то же, что у ванильного слоя: предмет лежит вдоль направления вперёд, у нижнего края руки
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate((leftArm ? -1 : 1) / 16.0F, 0.125F, -0.625F);
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        this.itemInHandRenderer.renderItem(entity, stack, displayContext, leftArm, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
