package net.gera.veridia.common.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.villager.lumberjack.LumberjackUpgrades;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

final class LumberjackTreeView implements ProfessionUpgradeTreeView {

    private static final ResourceLocation AXE =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/lumberjack/axe.png");
    private static final ResourceLocation CLOCK =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/lumberjack/clock.png");
    private static final ResourceLocation SPEED =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/lumberjack/speed.png");
    private static final ResourceLocation BLANK_UPGRADE =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/blank_upgrade.png");

    @Override
    public int size() {
        return LumberjackUpgrades.NODES.size();
    }

    @Override
    public int x(int index) {
        return LumberjackUpgrades.NODES.get(index).x();
    }

    @Override
    public int y(int index) {
        return LumberjackUpgrades.NODES.get(index).y();
    }

    @Override
    public int prerequisiteIndex(int index) {
        return LumberjackUpgrades.prerequisiteIndex(index);
    }

    @Override
    public ResourceLocation icon(int index) {
        return switch (LumberjackUpgrades.NODES.get(index).type()) {
            case AXE -> AXE;
            case CLOCK -> CLOCK;
            case SPEED -> SPEED;
            case LOG -> BLANK_UPGRADE;
        };
    }

    @Override
    @Nullable
    public ItemStack overlayItem(int index) {
        return LumberjackUpgrades.NODES.get(index).type() == LumberjackUpgrades.NodeType.LOG
                ? new ItemStack(Items.OAK_LOG) : null;
    }

    @Override
    public Component describe(int index) {
        return switch (LumberjackUpgrades.NODES.get(index).type()) {
            case AXE -> Component.translatable("tooltip.veridia.lumberjack_upgrade.axe",
                    pct(LumberjackUpgrades.AXE_DIG_SPEED_BONUS_PER_NODE));
            case LOG -> Component.translatable("tooltip.veridia.lumberjack_upgrade.log",
                    pct(LumberjackUpgrades.LOG_BONUS_CHANCE_PER_NODE), pct(LumberjackUpgrades.LOG_BONUS_AMOUNT_PER_NODE));
            case CLOCK -> Component.translatable("tooltip.veridia.lumberjack_upgrade.clock",
                    LumberjackUpgrades.CLOCK_START_SHIFT_PER_NODE, LumberjackUpgrades.CLOCK_END_SHIFT_PER_NODE);
            case SPEED -> Component.translatable("tooltip.veridia.lumberjack_upgrade.speed",
                    pct(LumberjackUpgrades.SPEED_WALK_BONUS));
        };
    }

    private static int pct(float value) {
        return Math.round(value * 100);
    }

    @Override
    public boolean isAvailable(Villager villager, int index) {
        return LumberjackUpgrades.isAvailable(villager, index);
    }

    @Override
    public boolean isUnlocked(Villager villager, int index) {
        return LumberjackUpgrades.isUnlocked(villager, index);
    }

    @Override
    public ItemStack costDisplayStack(Player player, int index) {
        return LumberjackUpgrades.costDisplayStack(player, index);
    }
}
