package net.gera.veridia.common.item.custom;

import net.gera.veridia.common.villager.storage.VillagerEmployment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;

public class FiringContractItem extends Item {

    private static final String PENDING_VILLAGER_KEY = "pendingVillager";
    private static final String PENDING_UNTIL_KEY = "pendingUntil";
    private static final int CONFIRM_WINDOW_TICKS = 20 * 6;

    public FiringContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(target instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.NONE) {
            player.displayClientMessage(Component.translatable("item.veridia.firing_contract.no_profession"), true);
            return InteractionResult.FAIL;
        }

        ItemStack liveStack = player.getItemInHand(hand);
        CompoundTag tag = liveStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        boolean confirmed = tag.hasUUID(PENDING_VILLAGER_KEY)
                && tag.getUUID(PENDING_VILLAGER_KEY).equals(villager.getUUID())
                && level.getGameTime() < tag.getLong(PENDING_UNTIL_KEY);

        if (confirmed) {
            CustomData.update(DataComponents.CUSTOM_DATA, liveStack, t -> {
                t.remove(PENDING_VILLAGER_KEY);
                t.remove(PENDING_UNTIL_KEY);
            });
            VillagerEmployment.fire(villager, level);
            player.displayClientMessage(
                    Component.translatable("item.veridia.firing_contract.fired", professionName(profession)), true);
            return InteractionResult.SUCCESS;
        }

        long until = level.getGameTime() + CONFIRM_WINDOW_TICKS;
        CustomData.update(DataComponents.CUSTOM_DATA, liveStack, t -> {
            t.putUUID(PENDING_VILLAGER_KEY, villager.getUUID());
            t.putLong(PENDING_UNTIL_KEY, until);
        });
        player.displayClientMessage(
                Component.translatable("item.veridia.firing_contract.confirm", professionName(profession)), true);
        return InteractionResult.SUCCESS;
    }

    private static Component professionName(VillagerProfession profession) {
        ResourceLocation key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        if (key == null) return Component.literal("?");
        String suffix = key.getNamespace().equals("minecraft") ? key.getPath() : key.getNamespace() + "." + key.getPath();
        return Component.translatable("entity.minecraft.villager." + suffix);
    }
}
