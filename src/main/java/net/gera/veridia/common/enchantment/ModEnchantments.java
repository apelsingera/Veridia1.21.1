package net.gera.veridia.common.enchantment;

import net.gera.veridia.Veridia;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantments {

    private ModEnchantments() {
    }

    public static final ResourceKey<Enchantment> WISE = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "wise"));

    private static final double BASE_MANA_REDUCTION = 0.20D;
    private static final double EXTRA_MANA_REDUCTION_PER_LEVEL = 0.10D;

    public static int reduceManaCost(Player player, ItemStack stack, int baseCost) {
        if (baseCost <= 0 || stack.isEmpty()) {
            return baseCost;
        }

        Holder<Enchantment> wise = player.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(WISE);

        int level = stack.getEnchantmentLevel(wise);
        if (level <= 0) {
            return baseCost;
        }

        double reduction = Math.min(1.0D, BASE_MANA_REDUCTION + EXTRA_MANA_REDUCTION_PER_LEVEL * (level - 1));
        return Math.max(0, (int) Math.round(baseCost * (1.0D - reduction)));
    }
}