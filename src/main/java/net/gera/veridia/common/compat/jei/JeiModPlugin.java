package net.gera.veridia.common.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.screen.CookingTableScreen;
import net.gera.veridia.common.block.entity.screen.ExtractorScreen;
import net.gera.veridia.common.block.entity.screen.MythicalTableScreen;
import net.gera.veridia.common.block.entity.screen.RunicPedestalScreen;
import net.gera.veridia.common.recipe.ModRecipes;
import net.gera.veridia.common.recipe.cookingtable.CookingTableRecipe;
import net.gera.veridia.common.recipe.extractor.ExtractorFluidRecipe;
import net.gera.veridia.common.recipe.extractor.ExtractorRecipe;
import net.gera.veridia.common.recipe.mythicaltable.MythicalTableRecipe;
import net.gera.veridia.common.recipe.runicpedestal.RunicPedestalRecipe;
import net.gera.veridia.common.recipe.runicpedestal.RunicPedestalRingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@JeiPlugin
public class JeiModPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CookingTableRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new MythicalTableRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RunicPedestalRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RunicPedestalRingRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ExtractorRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ExtractorMeltingRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        List<CookingTableRecipe> cookingTableRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.COOKING_TABLE_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(CookingTableRecipeCategory.COOKING_TABLE_RECIPE_RECIPE_TYPE, cookingTableRecipes);
        List<MythicalTableRecipe> mythicalTableRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.MYTHICAL_TABLE_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(MythicalTableRecipeCategory.MYTHICAL_TABLE_RECIPE_RECIPE_TYPE, mythicalTableRecipes);
        List<RunicPedestalRecipe> runicPedestalRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.RUNIC_PEDESTAL_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(RunicPedestalRecipeCategory.RUNIC_PEDESTAL_RECIPE_RECIPE_TYPE, runicPedestalRecipes);
        List<RunicPedestalRingRecipe> runicPedestalRingRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.RUNIC_PEDESTAL_RING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(RunicPedestalRingRecipeCategory.RUNIC_PEDESTAL_RING_RECIPE_TYPE, runicPedestalRingRecipes);

        List<ExtractorRecipe> extractingRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.EXTRACTING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(ExtractorRecipeCategory.EXTRACTING_RECIPE_TYPE, extractingRecipes);

        List<ExtractorFluidRecipe> extractorMeltingRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.EXTRACTOR_MELTING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(ExtractorMeltingRecipeCategory.EXTRACTOR_MELTING_RECIPE_TYPE, extractorMeltingRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ItemStack extractor = new ItemStack(ModBlocks.EXTRACTOR);
        registration.addRecipeCatalyst(extractor, ExtractorRecipeCategory.EXTRACTING_RECIPE_TYPE);
        registration.addRecipeCatalyst(extractor, ExtractorMeltingRecipeCategory.EXTRACTOR_MELTING_RECIPE_TYPE);
    }

    @Nullable
    private static IJeiRuntime jeiRuntime;

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        JeiRefreshHook.onRunicRingKnowledgeChanged = JeiModPlugin::refreshRunicPedestalRingRecipeVisibility;
        refreshRunicPedestalRingRecipeVisibility();
    }

    @Override
    public void onRuntimeUnavailable() {
        jeiRuntime = null;
        JeiRefreshHook.onRunicRingKnowledgeChanged = () -> {};
    }

    private static void refreshRunicPedestalRingRecipeVisibility() {
        if (jeiRuntime == null) return;
        Player player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().level == null) return;

        List<RunicPedestalRingRecipe> all = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.RUNIC_PEDESTAL_RING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();

        List<RunicPedestalRingRecipe> locked = all.stream().filter(recipe -> !recipe.isUnlockedFor(player)).toList();
        List<RunicPedestalRingRecipe> unlocked = all.stream().filter(recipe -> recipe.isUnlockedFor(player)).toList();

        RecipeType<RunicPedestalRingRecipe> type = RunicPedestalRingRecipeCategory.RUNIC_PEDESTAL_RING_RECIPE_TYPE;
        jeiRuntime.getRecipeManager().hideRecipes(type, locked);
        jeiRuntime.getRecipeManager().unhideRecipes(type, unlocked);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        IModPlugin.super.registerGuiHandlers(registration);

        registration.addRecipeClickArea(CookingTableScreen.class, 77, 34, 31, 16,
                CookingTableRecipeCategory.COOKING_TABLE_RECIPE_RECIPE_TYPE);

        registration.addRecipeClickArea(MythicalTableScreen.class, 78, 39, 46, 8,
                MythicalTableRecipeCategory.MYTHICAL_TABLE_RECIPE_RECIPE_TYPE);

        registration.addRecipeClickArea(RunicPedestalScreen.class, 103, 37, 7, 20,
                RunicPedestalRecipeCategory.RUNIC_PEDESTAL_RECIPE_RECIPE_TYPE);

        registration.addRecipeClickArea(ExtractorScreen.class, 63, 12, 62, 64,
                ExtractorRecipeCategory.EXTRACTING_RECIPE_TYPE,
                ExtractorMeltingRecipeCategory.EXTRACTOR_MELTING_RECIPE_TYPE);
    }

}