package net.gera.veridia.common.recipe.mythicaltable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record MythicalTableRecipeInput(ItemStack lapis, ItemStack gemstone,
                                       ItemStack primary, ItemStack secondary) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> lapis;
            case 1 -> gemstone;
            case 2 -> primary;
            case 3 -> secondary;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 4;
    }
}