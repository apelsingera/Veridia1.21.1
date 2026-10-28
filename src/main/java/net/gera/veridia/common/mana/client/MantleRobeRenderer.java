package net.gera.veridia.common.mana.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public class MantleRobeRenderer {

    private static final ResourceLocation NEWBIE_LEGS_TEXTURE = legsTexture("newbie");
    private static final ResourceLocation EXPERIENCED_LEGS_TEXTURE = legsTexture("experienced");

    private static ResourceLocation legsTexture(String tier) {
        return ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/models/armor/" + tier + "_layer_2.png");
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(robeExtensions(NEWBIE_LEGS_TEXTURE), ModItems.NEWBIE_MANTLE_CHESTPLATE.get());
        event.registerItem(robeExtensions(EXPERIENCED_LEGS_TEXTURE), ModItems.EXPERIENCED_MANTLE_CHESTPLATE.get());
    }

    private static IClientItemExtensions robeExtensions(ResourceLocation legsTexture) {
        return new IClientItemExtensions() {
            @Override
            public Model getGenericArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                               EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (equipmentSlot != EquipmentSlot.CHEST) {
                    return IClientItemExtensions.super.getGenericArmorModel(livingEntity, itemStack, equipmentSlot, original);
                }
                return new RobeModel(original, legsTexture);
            }
        };
    }

    private static final class RobeModel extends Model {
        private final HumanoidModel<?> original;
        private final ResourceLocation legsTexture;

        RobeModel(HumanoidModel<?> original, ResourceLocation legsTexture) {
            super(RenderType::armorCutoutNoCull);
            this.original = original;
            this.legsTexture = legsTexture;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
            original.head.render(poseStack, buffer, packedLight, packedOverlay, color);
            original.hat.render(poseStack, buffer, packedLight, packedOverlay, color);
            original.body.render(poseStack, buffer, packedLight, packedOverlay, color);
            original.rightArm.render(poseStack, buffer, packedLight, packedOverlay, color);
            original.leftArm.render(poseStack, buffer, packedLight, packedOverlay, color);

            original.rightLeg.visible = true;
            original.leftLeg.visible = true;
            VertexConsumer legsBuffer = Minecraft.getInstance().renderBuffers().bufferSource()
                    .getBuffer(RenderType.armorCutoutNoCull(legsTexture));
            original.rightLeg.render(poseStack, legsBuffer, packedLight, packedOverlay, color);
            original.leftLeg.render(poseStack, legsBuffer, packedLight, packedOverlay, color);
        }
    }
}