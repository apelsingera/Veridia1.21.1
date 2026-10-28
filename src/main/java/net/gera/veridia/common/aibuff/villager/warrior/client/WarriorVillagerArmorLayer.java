package net.gera.veridia.common.aibuff.villager.warrior.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;


public class WarriorVillagerArmorLayer extends RenderLayer<Villager, VillagerModel<Villager>> {

    /** Подъём шлема: -2px, потому что голова жителя на 2px выше humanoid-головы. */
    private static final float HEAD_Y_PIXELS = -2.0F;
    /** Голова жителя чуть шире humanoid-головы, шлем немного растягиваем, чтобы не врезался. */
    private static final float HEAD_SCALE = 1.05F;
    private static final float BODY_Y_PIXELS = 0.0F;
    /** Смещение рукавов нагрудника относительно ванильного humanoid-плеча. */
    private static final float ARM_Y_PIXELS = 1.0F;
    private static final float ARM_Z_PIXELS = -1.0F;
    private static final float LEG_Y_PIXELS = 0.0F;

    private final HumanoidModel<Villager> innerModel;
    private final HumanoidModel<Villager> outerModel;

    public WarriorVillagerArmorLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent, EntityModelSet models) {
        super(parent);
        this.innerModel = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        this.outerModel = new HumanoidModel<>(models.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, Villager villager,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        renderPiece(poseStack, buffers, light, villager, EquipmentSlot.CHEST);
        renderPiece(poseStack, buffers, light, villager, EquipmentSlot.LEGS);
        renderPiece(poseStack, buffers, light, villager, EquipmentSlot.FEET);
        renderPiece(poseStack, buffers, light, villager, EquipmentSlot.HEAD);
    }

    private void renderPiece(PoseStack poseStack, MultiBufferSource buffers, int light,
                             Villager villager, EquipmentSlot slot) {
        ItemStack stack = villager.getItemBySlot(slot);
        if (!(stack.getItem() instanceof ArmorItem armorItem)) return;
        if (armorItem.getEquipmentSlot() != slot) return;

        // Поножи рисуются "внутренним" (тонким) слоем брони, всё остальное - внешним: ровно как в
        // ванильном HumanoidArmorLayer.usesInnerModel.
        boolean innerLayer = slot == EquipmentSlot.LEGS;
        HumanoidModel<Villager> armor = innerLayer ? innerModel : outerModel;

        armor.setAllVisible(false);
        setPartVisibility(armor, slot);
        poseFromVillager(armor);

        ArmorMaterial material = armorItem.getMaterial().value();
        IClientItemExtensions extensions = IClientItemExtensions.of(stack);
        int fallbackColor = extensions.getDefaultDyeColor(stack);
        List<ArmorMaterial.Layer> layers = material.layers();

        for (int i = 0; i < layers.size(); i++) {
            ArmorMaterial.Layer layer = layers.get(i);
            // Через extensions, а не напрямую - иначе крашеная кожаная броня рисовалась бы серой.
            int tint = extensions.getArmorLayerTintColor(stack, villager, layer, i, fallbackColor);
            if (tint == 0) continue;

            VertexConsumer consumer = buffers.getBuffer(RenderType.armorCutoutNoCull(layer.texture(innerLayer)));
            armor.renderToBuffer(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, tint);
        }
    }

    /** Копия ванильного HumanoidArmorLayer.setPartVisibility - какие части рисует каждый слот. */
    private static void setPartVisibility(HumanoidModel<Villager> armor, EquipmentSlot slot) {
        switch (slot) {
            case HEAD -> {
                armor.head.visible = true;
                armor.hat.visible = true;
            }
            case CHEST -> {
                armor.body.visible = true;
                armor.rightArm.visible = true;
                armor.leftArm.visible = true;
            }
            case LEGS -> {
                armor.body.visible = true;
                armor.rightLeg.visible = true;
                armor.leftLeg.visible = true;
            }
            case FEET -> {
                armor.rightLeg.visible = true;
                armor.leftLeg.visible = true;
            }
            default -> {
            }
        }
    }

    /** Ставим humanoid-модель брони по костям модели жителя (голова, сложенные руки, ноги). */
    private void poseFromVillager(HumanoidModel<Villager> armor) {
        VillagerModel<Villager> parent = getParentModel();
        ModelPart villagerHead = parent.getHead();
        ModelPart villagerArms = parent.root().getChild("arms");
        ModelPart villagerRightLeg = parent.root().getChild("right_leg");
        ModelPart villagerLeftLeg = parent.root().getChild("left_leg");

        armor.young = false;
        armor.riding = false;
        armor.crouching = false;
        armor.swimAmount = 0.0F;
        armor.attackTime = 0.0F;

        armor.head.setPos(0.0F, HEAD_Y_PIXELS, 0.0F);
        copyRotation(villagerHead, armor.head);
        armor.head.xScale = HEAD_SCALE;
        armor.head.yScale = HEAD_SCALE;
        armor.head.zScale = HEAD_SCALE;
        armor.hat.copyFrom(armor.head);

        armor.body.setPos(0.0F, BODY_Y_PIXELS, 0.0F);
        resetRotation(armor.body);

        armor.rightArm.setPos(-5.0F, 2.0F + ARM_Y_PIXELS, ARM_Z_PIXELS);
        armor.leftArm.setPos(5.0F, 2.0F + ARM_Y_PIXELS, ARM_Z_PIXELS);
        resetRotation(armor.rightArm);
        resetRotation(armor.leftArm);
        armor.rightArm.xRot = villagerArms.xRot;
        armor.leftArm.xRot = villagerArms.xRot;

        armor.rightLeg.setPos(-1.9F, 12.0F + LEG_Y_PIXELS, 0.0F);
        armor.leftLeg.setPos(1.9F, 12.0F + LEG_Y_PIXELS, 0.0F);
        resetRotation(armor.rightLeg);
        resetRotation(armor.leftLeg);
        armor.rightLeg.xRot = villagerRightLeg.xRot;
        armor.leftLeg.xRot = villagerLeftLeg.xRot;
    }

    private static void copyRotation(ModelPart from, ModelPart to) {
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
    }

    private static void resetRotation(ModelPart part) {
        part.xRot = 0.0F;
        part.yRot = 0.0F;
        part.zRot = 0.0F;
    }
}
