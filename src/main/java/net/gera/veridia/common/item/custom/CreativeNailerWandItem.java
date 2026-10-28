package net.gera.veridia.common.item.custom;

import net.gera.veridia.common.aibuff.villager.nailer.NailerDebugMode;
import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CreativeNailerWandItem extends Item {

    public CreativeNailerWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(target instanceof Villager villager) || villager.level().isClientSide()) {
            return InteractionResult.PASS;
        }
        if (villager.getVillagerData().getProfession() != ModVillagers.NAILER.value()) {
            player.displayClientMessage(Component.translatable("item.veridia.creative_nailer_wand.not_nailer"), true);
            return InteractionResult.FAIL;
        }

        boolean now = !NailerDebugMode.isEnabled(villager);
        NailerDebugMode.setEnabled(villager, now);
        player.displayClientMessage(Component.translatable(now
                ? "item.veridia.creative_nailer_wand.enabled"
                : "item.veridia.creative_nailer_wand.disabled"), true);
        return InteractionResult.SUCCESS;
    }
}
