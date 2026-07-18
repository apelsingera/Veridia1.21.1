package net.gera.veridia.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.block.ModBlocks;
import net.gera.veridia.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        List<ItemLike> SMELT_ORE_BLOCKS = List.of(ModBlocks.GRANITE_COPPER_ORE.get());


        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PLANT_CLUMP_BLOCK.get())
                .pattern("AA")
                .pattern("AA")
                .define('A', ModItems.PLANT_CLUMP.get())
                .unlockedBy("has_clump", has(ModItems.PLANT_CLUMP)).save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PLANT_STRING.get())
                .pattern("AA")
                .pattern("A ")
                .define('A', ModItems.PLANT_CLUMP.get())
                .unlockedBy("has_clump", has(ModItems.PLANT_CLUMP)).save(recipeOutput);
        //SHAPELESS
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PLANT_CLUMP.get(), 9)
                .requires(ModBlocks.PLANT_CLUMP_BLOCK)
                .unlockedBy("has_clump_block", has(ModBlocks.PLANT_CLUMP_BLOCK)).save(recipeOutput, "veridia:clump_block_to_clump_1");
        //ore
        oreSmelting(recipeOutput, SMELT_ORE_BLOCKS, RecipeCategory.MISC, ModBlocks.GRANITE_COPPER_ORE.get(), 0.25f, 200, "granite");
        oreBlasting(recipeOutput, SMELT_ORE_BLOCKS, RecipeCategory.MISC, ModBlocks.GRANITE_COPPER_ORE.get(), 0.25f, 100, "granite");

        stairBuilder(ModBlocks.TIN_STAIRS.get(), Ingredient.of(ModBlocks.TIN_BLOCK)).group("tin_block ")
                .unlockedBy("has_tin_ingot", has(ModItems.TIN_INGOT)).save(recipeOutput);

        slab(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.TIN_SLAB.get(), ModBlocks.TIN_BLOCK.get());

        buttonBuilder(ModBlocks.TIN_BUTTON.get(), Ingredient.of(ModBlocks.TIN_BLOCK.get())).group("tin_block")
                .unlockedBy("has_tin_ingot", has(ModItems.TIN_INGOT.get())).save(recipeOutput);

        pressurePlate(recipeOutput, ModBlocks.TIN_PRESSURE_PLATE.get(), ModBlocks.TIN_BLOCK.get());

        wall(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.TIN_WALL.get(), ModItems.TIN_INGOT.get());

        doorBuilder(ModBlocks.TIN_DOOR.get(), Ingredient.of(ModItems.TIN_INGOT.get())).group("tin_block")
                .unlockedBy("has_tin_ingot", has(ModItems.TIN_INGOT.get())).save(recipeOutput);

        trapdoorBuilder(ModBlocks.TIN_TRAPDOOR.get(), Ingredient.of(ModItems.TIN_INGOT.get())).group("tin_block")
                .unlockedBy("has_tin_ingot", has(ModItems.TIN_INGOT.get())).save(recipeOutput);
    }

    //helper methods

        protected static void oreSmelting (RecipeOutput recipeOutput, List < ItemLike > pIngredients, RecipeCategory
        pCategory, ItemLike pResult,
        float pExperience, int pCookingTIme, String pGroup){
            oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult,
                    pExperience, pCookingTIme, pGroup, "_from_smelting");
        }

        protected static void oreBlasting (RecipeOutput recipeOutput, List < ItemLike > pIngredients, RecipeCategory
        pCategory, ItemLike pResult,
        float pExperience, int pCookingTime, String pGroup){
            oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult,
                    pExperience, pCookingTime, pGroup, "_from_blasting");
        }

        protected static <T extends AbstractCookingRecipe > void oreCooking (RecipeOutput
        recipeOutput, RecipeSerializer < T > pCookingSerializer, AbstractCookingRecipe.Factory < T > factory,
                List < ItemLike > pIngredients, RecipeCategory pCategory, ItemLike pResult,float pExperience,
        int pCookingTime, String pGroup, String pRecipeName){
            for (ItemLike itemlike : pIngredients) {
                SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                        .save(recipeOutput, Veridia.MOD_ID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
            }
        }
    }