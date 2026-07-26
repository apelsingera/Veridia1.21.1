package net.gera.veridiafoods.item.custom.tools;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class CrudeKnifeItem extends Item {

    public CrudeKnifeItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(32));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        boolean isGrass = state.is(Blocks.SHORT_GRASS);
        boolean isDandelion = state.is(Blocks.DANDELION);

        if (isGrass || isDandelion) {
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                RandomSource random = serverLevel.getRandom();
                ItemStack dropStack;
                float breakChance;

                if (isGrass) {
                    dropStack = new ItemStack(ModItems.PLANT_CLUMP.get());
                    breakChance = 0.45F;
                } else {
                    if (random.nextFloat() < 0.32F) {
                        dropStack = new ItemStack(ModItems.DANDELIONS_ROOT.get());
                    } else {
                        dropStack = new ItemStack(Items.DANDELION);
                    }
                    breakChance = 0.40F;
                }

                ItemEntity itemEntity = new ItemEntity(
                        serverLevel,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        dropStack
                );
                serverLevel.addFreshEntity(itemEntity);

                if (random.nextFloat() < breakChance) {
                    serverLevel.destroyBlock(pos, false);
                }

                if (player instanceof ServerPlayer serverPlayer) {
                    EquipmentSlot slot = context.getHand() == InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND
                            : EquipmentSlot.OFFHAND;

                    stack.hurtAndBreak(1, serverLevel, serverPlayer, (item) ->
                            serverPlayer.onEquippedItemBroken(item, slot)
                    );
                }
            }

            if (player != null) {
                player.getCooldowns().addCooldown(this, 30);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}