package net.gera.veridiafoods.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.block.entity.screen.CookingTableScreen;
import net.gera.veridiafoods.recipe.ModRecipes;
import net.gera.veridiafoods.recipe.cookingtable.CookingTableRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class JeiModPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(VeridiaFoods.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CookingTableRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<CookingTableRecipe> cookingTableRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.COOKING_TABLE_TYPE.get()).stream().map(RecipeHolder::value).toList();
        registration.addRecipes(CookingTableRecipeCategory.COOKING_TABLE_RECIPE_RECIPE_TYPE, cookingTableRecipes);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        IModPlugin.super.registerGuiHandlers(registration);
        registration.addRecipeClickArea(CookingTableScreen.class, 77, 34, 31,16,
                CookingTableRecipeCategory.COOKING_TABLE_RECIPE_RECIPE_TYPE);
    }
}
