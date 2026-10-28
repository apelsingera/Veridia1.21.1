package net.gera.veridia.common.util;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
public class ModTiers {
    public static final Tier ELECTRIC_TOOL = new SimpleTier(ModTags.Blocks.INCORRECT_FOR_ELECTRIC_TOOL,
            514, 5.5f, 0.0f, 21, () -> Ingredient.of(Items.COPPER_INGOT));
    public static final Tier VERIDITE_TOOL = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            3000, 12.0f, 5.0f, 20, () -> Ingredient.of(ModItems.VERIDITE.get()));
}