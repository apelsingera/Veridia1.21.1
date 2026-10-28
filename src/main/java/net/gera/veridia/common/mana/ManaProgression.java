package net.gera.veridia.common.mana;

import net.gera.veridia.Veridia;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Set;

public class ManaProgression {

    public static final int BASE_MAX_MANA = 30;
    private static final double BASE_REGEN_PER_SECOND = (1.0 / 3.0) / 2.0;
    private static final double BASE_FAILURE_CHANCE = 25.0;
    private static final double BASE_SUPER_CHANCE = 2.5;
    private static final double SUPER_POWER_MULTIPLIER = 1.5;

    private static final ResourceLocation EXTRA_HEART_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "mana_rune_extra_heart");
    private static final ResourceLocation ATTACK_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "mana_rune_attack_speed");

    public static final List<UpgradeStep> STEPS = List.of(
            // LVL 1 - NEWBIE_RUNE
            new UpgradeStep("newbie", 1, "newbie_rune_1", 10, 0, 0, 0),
            new UpgradeStep("newbie", 2, "newbie_rune_2", 0, 5, 0, 0),
            new UpgradeStep("newbie", 3, "newbie_rune_3", 10, 0, 0, 0),
            new UpgradeStep("newbie", 4, "newbie_rune_4", 0, 0, -7.5, 0),
            new UpgradeStep("newbie", 5, "newbie_rune_5", 0, 10, 0, 0),
            new UpgradeStep("newbie", 6, "newbie_rune_6", 5, 0, 0, 0),
            new UpgradeStep("newbie", 7, "newbie_rune_7", 0, 0, -5.0, 0),
            new UpgradeStep("newbie", 8, "newbie_rune_8", 0, 10, 0, 0),
            new UpgradeStep("newbie", 9, "newbie_rune_9", 5, 0, 0, 0),
            new UpgradeStep("newbie", 10, "newbie_rune_10", 0, 0, 0, 0.75),
            // LVL 2 - APPRENTICE_RUNE
            new UpgradeStep("apprentice", 1, "apprentice_rune_1", 5, 0, 0, 0),
            new UpgradeStep("apprentice", 2, "apprentice_rune_2", 0, 0, -5.0, 0),
            new UpgradeStep("apprentice", 3, "apprentice_rune_3", 0, 0, 0, 0, Set.of(Perk.EXTRA_HEART)),
            new UpgradeStep("apprentice", 4, "apprentice_rune_4", 0, 10, 0, 0),
            new UpgradeStep("apprentice", 5, "apprentice_rune_5", 10, 0, 0, 0),
            new UpgradeStep("apprentice", 6, "apprentice_rune_6", 0, 5, 0, 0),
            new UpgradeStep("apprentice", 7, "apprentice_rune_7", 5, 0, 0, 0),
            new UpgradeStep("apprentice", 8, "apprentice_rune_8", 0, 0, -2.5, 0),
            new UpgradeStep("apprentice", 9, "apprentice_rune_9", 0, 0, 0, 0.75),
            new UpgradeStep("apprentice", 10, "apprentice_rune_10", 10, 5, 0, 0),
            // LVL 3 - EXPERIENCED_RUNE
            new UpgradeStep("experienced", 1, "experienced_rune_1", 0, 0, 0, 0, Set.of(Perk.ATTACK_SPEED)),
            new UpgradeStep("experienced", 2, "experienced_rune_2", 0, 15, 0, 0),
            new UpgradeStep("experienced", 3, "experienced_rune_3", 15, 0, 0, 0),
            new UpgradeStep("experienced", 4, "experienced_rune_4", 0, 0, -0.5, 0),
            new UpgradeStep("experienced", 5, "experienced_rune_5", 0, 10, 0, 0),
            new UpgradeStep("experienced", 6, "experienced_rune_6", 5, 0, 0, 0),
            new UpgradeStep("experienced", 7, "experienced_rune_7", 0, 5, 0, 0),
            new UpgradeStep("experienced", 8, "experienced_rune_8", 5, 0, 0, 0),
            // LVL 4 - WIZARD_RUNE
            new UpgradeStep("wizard", 1, "wizard_rune_1", 0, 0, 0, 0.5),
            new UpgradeStep("wizard", 2, "wizard_rune_2", 0, 0, 0, 0, Set.of(Perk.SUPER_POWER)),
            new UpgradeStep("wizard", 3, "wizard_rune_3", 25, 25, 0, 0),
            new UpgradeStep("wizard", 4, "wizard_rune_4", 0, 0, -2.0, 0),
            new UpgradeStep("wizard", 5, "wizard_rune_5", 10, 25, 0, 0, Set.of(Perk.VERIDITE_LASER))
    );

    public static int progress(Player player) {
        return player.getData(ModAttachments.MANA_UPGRADE_PROGRESS);
    }

    public static int getMaxMana(Player player) {
        return BASE_MAX_MANA + sum(progress(player), UpgradeStep::capacityDelta).intValue() + getActiveMaxManaBonus(player);
    }

    private static int getActiveMaxManaBonus(Player player) {
        long expiresAt = player.getData(ModAttachments.MANA_MAX_BONUS_EXPIRES_AT);
        if (expiresAt <= 0L || player.level().getGameTime() >= expiresAt) {
            return 0;
        }
        return player.getData(ModAttachments.MANA_MAX_BONUS);
    }

    public static void applyTemporaryMaxManaBoost(ServerPlayer player, int bonusAmount, int durationTicks) {
        player.setData(ModAttachments.MANA_MAX_BONUS, bonusAmount);
        player.setData(ModAttachments.MANA_MAX_BONUS_EXPIRES_AT, player.level().getGameTime() + durationTicks);
    }

    public static double getRegenPerTick(Player player) {
        double regenPercent = sum(progress(player), UpgradeStep::regenPercentDelta)
                + ManaArmorEffects.getRegenPercentModifier(player);
        double perSecond = BASE_REGEN_PER_SECOND * (1.0 + regenPercent / 100.0);
        return perSecond / 20.0;
    }

    public static double getFailureChance(Player player) {
        return Math.max(0.0, BASE_FAILURE_CHANCE + sum(progress(player), UpgradeStep::failureChanceDelta));
    }

    public static double getSuperChance(Player player) {
        return BASE_SUPER_CHANCE + sum(progress(player), UpgradeStep::superChanceDelta);
    }

    public static double getSuperPowerMultiplier(Player player) {
        return hasPerk(player, Perk.SUPER_POWER) ? SUPER_POWER_MULTIPLIER : 1.0;
    }

    public static boolean hasPerk(Player player, Perk perk) {
        int owned = Math.min(progress(player), STEPS.size());
        for (int i = 0; i < owned; i++) {
            if (STEPS.get(i).perks().contains(perk)) {
                return true;
            }
        }
        return false;
    }

    private static Double sum(int progress, java.util.function.ToDoubleFunction<UpgradeStep> selector) {
        int owned = Math.min(progress, STEPS.size());
        double total = 0;
        for (int i = 0; i < owned; i++) {
            total += selector.applyAsDouble(STEPS.get(i));
        }
        return total;
    }

    public static void reconcileAttributes(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            if (hasPerk(player, Perk.EXTRA_HEART)) {
                maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                        EXTRA_HEART_MODIFIER_ID, 2.0, AttributeModifier.Operation.ADD_VALUE));
            } else {
                maxHealth.removeModifier(EXTRA_HEART_MODIFIER_ID);
            }
        }

        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            if (hasPerk(player, Perk.ATTACK_SPEED)) {
                attackSpeed.addOrUpdateTransientModifier(new AttributeModifier(
                        ATTACK_SPEED_MODIFIER_ID, 0.075, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_ID);
            }
        }
    }
}