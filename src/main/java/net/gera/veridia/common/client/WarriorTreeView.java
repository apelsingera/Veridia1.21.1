package net.gera.veridia.common.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorUpgrades;
import net.gera.veridia.common.effect.Bleeding;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class WarriorTreeView implements ProfessionUpgradeTreeView {

    private static final ResourceLocation SPEED =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/warrior/speed.png");
    private static final ResourceLocation HEALTH_BOOST =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/warrior/health_boost.png");
    private static final ResourceLocation ARMOR =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/warrior/armor.png");
    private static final ResourceLocation BLEEDING =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/warrior/bleeding.png");
    private static final ResourceLocation FIRE_ASPECT =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/warrior/fire_aspect.png");
    private static final ResourceLocation BRAIN =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/warrior/brain.png");

    @Override
    public int size() {
        return WarriorUpgrades.NODES.size();
    }

    @Override
    public int x(int index) {
        return WarriorUpgrades.NODES.get(index).x();
    }

    @Override
    public int y(int index) {
        return WarriorUpgrades.NODES.get(index).y();
    }

    @Override
    public int prerequisiteIndex(int index) {
        return WarriorUpgrades.prerequisiteIndex(index);
    }

    @Override
    public ResourceLocation icon(int index) {
        return switch (WarriorUpgrades.NODES.get(index).type()) {
            case SPEED -> SPEED;
            case HEART -> HEALTH_BOOST;
            case ARMOR -> ARMOR;
            case BLEED -> BLEEDING;
            case FIRE_ASPECT -> FIRE_ASPECT;
            case BRAIN -> BRAIN;
        };
    }

    @Override
    @Nullable
    public ItemStack overlayItem(int index) {
        // У warrior все узлы - готовые дедицированные иконки (в отличие от farmer с bone_meal/
        // rich_soil), поверх icon() ничего рисовать не нужно.
        return null;
    }

    @Override
    public Component describe(int index) {
        return switch (WarriorUpgrades.NODES.get(index).type()) {
            case SPEED -> Component.translatable("tooltip.veridia.warrior_upgrade.speed",
                    pct(WarriorUpgrades.SPEED_WALK_BONUS_PER_NODE));
            case HEART -> Component.translatable("tooltip.veridia.warrior_upgrade.heart",
                    Math.round(WarriorUpgrades.HEART_MAX_HEALTH_BONUS_PER_NODE));
            case ARMOR -> Component.translatable("tooltip.veridia.warrior_upgrade.armor");
            case BLEED -> Component.translatable("tooltip.veridia.warrior_upgrade.bleed",
                    WarriorUpgrades.BLEED_TICK_APPLICATIONS_PER_NODE, Bleeding.DAMAGE_PER_TICK,
                    Bleeding.DAMAGE_INTERVAL_TICKS / 20);
            case FIRE_ASPECT -> Component.translatable("tooltip.veridia.warrior_upgrade.fire_aspect",
                    pct(WarriorUpgrades.FIRE_ASPECT_CHANCE), WarriorUpgrades.FIRE_ASPECT_IGNITE_SECONDS);
            case BRAIN -> Component.translatable("tooltip.veridia.warrior_upgrade.brain",
                    Math.round(WarriorUpgrades.BRAIN_HEAL_AMOUNT), WarriorUpgrades.BRAIN_HEAL_NO_DAMAGE_TICKS / 20);
        };
    }

    private static int pct(float value) {
        return Math.round(value * 100);
    }

    @Override
    public boolean isAvailable(Villager villager, int index) {
        return WarriorUpgrades.isAvailable(villager, index);
    }

    @Override
    public boolean isUnlocked(Villager villager, int index) {
        return WarriorUpgrades.isUnlocked(villager, index);
    }

    @Override
    public ItemStack costDisplayStack(Player player, int index) {
        return WarriorUpgrades.costDisplayStack(player, index);
    }
}