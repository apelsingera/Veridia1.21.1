package net.gera.veridia.tier;

import net.gera.veridia.item.ModItems;
import net.gera.veridia.util.ModTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public class ModTiers {
    public static final Tier COPPER = new SimpleTier(ModTags.Blocks.INCORRECT_FOR_COPPER_TOOL,
            191, 5f, 0.0f, 15, () -> Ingredient.of(ModItems.COPPER_INGOT));
    public static final Tier BRONZE = new SimpleTier(ModTags.Blocks.INCORRECT_FOR_BRONZE_TOOL,
            373, 5.5f, 0.0f, 13, () -> Ingredient.of(ModItems.BRONZE_INGOT));
    public static final Tier IRON = new SimpleTier(ModTags.Blocks.INCORRECT_FOR_W_IRON_TOOL,
            529, 6f, 0.0f, 17, () -> Ingredient.of(ModItems.WROUGHT_IRON_INGOT));
}