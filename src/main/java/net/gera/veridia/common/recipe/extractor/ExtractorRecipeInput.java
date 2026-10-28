package net.gera.veridia.common.recipe.extractor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Вход рецепта {@link ExtractorRecipe}: доп. ингредиент из верхнего слота, апгрейд из слота (128,35)
 * и текущее содержимое входного танка экстрактора. Жидкость не "предмет", поэтому в {@link #getItem}
 * не участвует.
 */
public record ExtractorRecipeInput(ItemStack ingredient, ItemStack upgrade, FluidStack fluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> ingredient;
            case 1 -> upgrade;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return ingredient.isEmpty() && upgrade.isEmpty() && fluid.isEmpty();
    }
}
