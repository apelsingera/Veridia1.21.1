package net.gera.veridia.common.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.villager.nailer.NailerUpgrades;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class NailerTreeView implements ProfessionUpgradeTreeView {

    private static final ResourceLocation SPEED =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/nailer/speed.png");
    private static final ResourceLocation HAMMER =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/nailer/hammer.png");
    private static final ResourceLocation BRAIN =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/nailer/brain.png");
    private static final ResourceLocation CLOCK =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/nailer/clock.png");
    private static final ResourceLocation EMERALD =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/nailer/emerald.png");
    private static final ResourceLocation TECHNOLOGY =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/nailer/technology1.png");
    private static final ResourceLocation BLANK_UPGRADE =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/blank_upgrade.png");

    @Override
    public int size() {
        return NailerUpgrades.NODES.size();
    }

    @Override
    public int x(int index) {
        return NailerUpgrades.NODES.get(index).x();
    }

    @Override
    public int y(int index) {
        return NailerUpgrades.NODES.get(index).y();
    }

    @Override
    public int prerequisiteIndex(int index) {
        return NailerUpgrades.prerequisiteIndex(index);
    }

    @Override
    public ResourceLocation icon(int index) {
        return switch (NailerUpgrades.NODES.get(index).type()) {
            case SPEED -> SPEED;
            case HAMMER -> HAMMER;
            case BRAIN -> BRAIN;
            case CLOCK -> CLOCK;
            case TRADE_DISCOUNT -> EMERALD;
            case TECHNOLOGY -> TECHNOLOGY;
            case COPPER_BLOCK -> BLANK_UPGRADE;
            case GOLD_BLOCK -> BLANK_UPGRADE;
        };
    }

    @Override
    @Nullable
    public ItemStack overlayItem(int index) {
        return switch (NailerUpgrades.NODES.get(index).type()) {
            case COPPER_BLOCK -> new ItemStack(Items.COPPER_BLOCK);
            case GOLD_BLOCK -> new ItemStack(Items.GOLD_BLOCK);
            default -> null;
        };
    }

    @Override
    public Component describe(int index) {
        return switch (NailerUpgrades.NODES.get(index).type()) {
            case SPEED -> Component.translatable("tooltip.veridia.nailer_upgrade.speed",
                    pct(NailerUpgrades.SPEED_WALK_BONUS_PER_NODE));
            case HAMMER -> Component.translatable("tooltip.veridia.nailer_upgrade.hammer",
                    pct(NailerUpgrades.HAMMER_BUILD_SPEED_BONUS_PER_NODE));
            case BRAIN -> Component.translatable("tooltip.veridia.nailer_upgrade.brain");
            case CLOCK -> Component.translatable("tooltip.veridia.nailer_upgrade.clock",
                    NailerUpgrades.CLOCK_START_SHIFT_PER_NODE, NailerUpgrades.CLOCK_END_SHIFT_PER_NODE);
            case TRADE_DISCOUNT -> Component.translatable("tooltip.veridia.nailer_upgrade.trade_discount",
                    pct(NailerUpgrades.TRADE_DISCOUNT_PER_NODE));
            // Больше не покупается за ресурсы (рецепт удалён) - разблокируется сам, когда nailer
            // достроит всю стену (см. NailerUpgrades.markWallComplete).
            case COPPER_BLOCK -> Component.translatable("tooltip.veridia.nailer_upgrade.copper_block");
            // tech_1/tech_2 - c реальным эффектом (см. NailerUpgrades.isTechUnlocked/isTech2Unlocked),
            // tech_3 пока заглушка.
            case TECHNOLOGY -> switch (NailerUpgrades.NODES.get(index).id()) {
                case "tech_1" -> Component.translatable("tooltip.veridia.nailer_upgrade.technology_wall_upgrade");
                case "tech_2" -> Component.translatable("tooltip.veridia.nailer_upgrade.technology_towers");
                default -> Component.translatable("tooltip.veridia.nailer_upgrade.technology");
            };
            case GOLD_BLOCK -> Component.translatable("tooltip.veridia.nailer_upgrade.gold_block");
        };
    }

    private static int pct(float value) {
        return Math.round(value * 100);
    }

    @Override
    public boolean isAvailable(Villager villager, int index) {
        return NailerUpgrades.isAvailable(villager, index);
    }

    @Override
    public boolean isUnlocked(Villager villager, int index) {
        return NailerUpgrades.isUnlocked(villager, index);
    }

    @Override
    public ItemStack costDisplayStack(Player player, int index) {
        return NailerUpgrades.costDisplayStack(player, index);
    }

    /** Level2 (tech_1..gold_block) полностью заменяет показ первого экрана, как только nailer
     * реально достроил стену у ЭТОГО жителя - см. javadoc NailerUpgrades.NODES/markWallComplete.
     * Узел скрыт, когда "это узел level2" не совпадает с "level2 разблокирован": level1-узлы
     * скрываются после разблокировки, level2-узлы скрыты до неё. */
    @Override
    public boolean isHidden(Villager villager, int index) {
        return NailerUpgrades.isLevel2Node(index) != NailerUpgrades.isLevel2Unlocked(villager);
    }
}
