package net.gera.veridia.common.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.villager.farmer.FarmerUpgrades;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

final class FarmerTreeView implements ProfessionUpgradeTreeView {

    private static final ResourceLocation TREE_ICON =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/farmer/tree.png");
    private static final ResourceLocation CLOCK =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/farmer/clock.png");
    private static final ResourceLocation SPEED =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/farmer/speed.png");
    private static final ResourceLocation BLANK_UPGRADE =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/blank_upgrade.png");

    // farmersdelight - мягкая зависимость, ищем по id, а не по прямой ссылке на класс мода.
    // Ищем не при загрузке класса (регистр предметов другого мода может ещё не заполниться), а
    // прямо в момент рендера - реестры на тот момент уже точно готовы.
    private static Item richSoilItem() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("farmersdelight", "rich_soil"));
    }

    @Override
    public int size() {
        return FarmerUpgrades.NODES.size();
    }

    @Override
    public int x(int index) {
        return FarmerUpgrades.NODES.get(index).x();
    }

    @Override
    public int y(int index) {
        return FarmerUpgrades.NODES.get(index).y();
    }

    @Override
    public int prerequisiteIndex(int index) {
        return FarmerUpgrades.prerequisiteIndex(index);
    }

    @Override
    public ResourceLocation icon(int index) {
        return switch (FarmerUpgrades.NODES.get(index).type()) {
            case TREE -> TREE_ICON;
            case CLOCK -> CLOCK;
            case SPEED -> SPEED;
            case BONE_MEAL, RICH_SOIL -> BLANK_UPGRADE;
        };
    }

    @Override
    @Nullable
    public ItemStack overlayItem(int index) {
        return switch (FarmerUpgrades.NODES.get(index).type()) {
            case BONE_MEAL -> new ItemStack(Items.BONE_MEAL);
            case RICH_SOIL -> new ItemStack(richSoilItem());
            default -> null;
        };
    }

    @Override
    public Component describe(int index) {
        return switch (FarmerUpgrades.NODES.get(index).type()) {
            case SPEED -> Component.translatable("tooltip.veridia.farmer_upgrade.speed",
                    Math.round(FarmerUpgrades.SPEED_WALK_BONUS * 100));
            case CLOCK -> Component.translatable("tooltip.veridia.farmer_upgrade.clock",
                    FarmerUpgrades.CLOCK_START_SHIFT_PER_NODE);
            case BONE_MEAL -> Component.translatable("tooltip.veridia.farmer_upgrade.bone_meal",
                    Math.round(FarmerUpgrades.BONE_MEAL_CHANCE_BONUS_PER_NODE * 100));
            case TREE -> Component.translatable("tooltip.veridia.farmer_upgrade.tree",
                    FarmerUpgrades.PLANT_HOLD_TICKS_REDUCTION_PER_NODE / 20);
            case RICH_SOIL -> Component.translatable("tooltip.veridia.farmer_upgrade.rich_soil",
                    Math.round(FarmerUpgrades.RICH_SOIL_UPGRADE_CHANCE_PER_NODE * 100));
        };
    }

    @Override
    public boolean isAvailable(Villager villager, int index) {
        return FarmerUpgrades.isAvailable(villager, index);
    }

    @Override
    public boolean isUnlocked(Villager villager, int index) {
        return FarmerUpgrades.isUnlocked(villager, index);
    }

    @Override
    public ItemStack costDisplayStack(Player player, int index) {
        return FarmerUpgrades.costDisplayStack(player, index);
    }
}
