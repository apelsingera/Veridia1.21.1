package net.gera.veridia.common.recipe.extractor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Вход рецепта разжижения {@link ExtractorFluidRecipe}: один предмет из среднего слота экстрактора. */
public record ExtractorFluidRecipeInput(ItemStack stack) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? stack : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
