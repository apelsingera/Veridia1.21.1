package net.gera.veridia.common.recipe.villagerupgrade;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Один предмет-кандидат из инвентаря игрока - см. VillagerUpgradeRecipe.matches(). */
public record VillagerUpgradeRecipeInput(ItemStack stack) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return stack;
    }

    @Override
    public int size() {
        return 1;
    }
}
