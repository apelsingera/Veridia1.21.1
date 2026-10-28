package net.gera.veridia.common.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.recipe.cookingtable.CookingTableRecipe;
import net.gera.veridia.common.recipe.cookingtable.ToolRequirement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CookingTableRecipeCategory implements IRecipeCategory<CookingTableRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "cooking_table");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,
            "textures/gui/cooking_table/cooking_table_gui.png");

    public static final RecipeType<CookingTableRecipe> COOKING_TABLE_RECIPE_RECIPE_TYPE =
            new RecipeType<>(UID, CookingTableRecipe.class);

    private static final int[][] GRID_SLOTS = {
            {18, 16}, {36, 16}, {54, 16},
            {18, 34}, {36, 34}, {54, 34},
            {18, 52}, {36, 52}, {54, 52}
    };
    private static final int OUTPUT_X = 119, OUTPUT_Y = 34;
    private static final int KNIFE_X = 130, KNIFE_Y = 60;
    private static final int HAMMER_X = 109, HAMMER_Y = 60;

    private final IDrawable background;
    private final IDrawable icon;

    public CookingTableRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 80);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.COOKING_TABLE));
    }

    @Override
    public RecipeType<CookingTableRecipe> getRecipeType() {
        return COOKING_TABLE_RECIPE_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.veridia.cooking_table");
    }

    @Override
    public int getWidth() {
        return background.getWidth();
    }

    @Override
    public int getHeight() {
        return background.getHeight();
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CookingTableRecipe recipe, IFocusGroup focuses) {
        List<Ingredient> ingredients = recipe.ingredients();

        for (int i = 0; i < GRID_SLOTS.length; i++) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, GRID_SLOTS[i][0], GRID_SLOTS[i][1]);
            if (i < ingredients.size()) {
                slot.addIngredients(ingredients.get(i));
            }
        }

        if (recipe.toolRequirement() == ToolRequirement.KNIFE) {
            builder.addSlot(RecipeIngredientRole.INPUT, KNIFE_X, KNIFE_Y)
                    .addItemStack(new ItemStack(ModItems.CRUDE_KNIFE.get()));
        } else if (recipe.toolRequirement() == ToolRequirement.HAMMER) {
            builder.addSlot(RecipeIngredientRole.INPUT, HAMMER_X, HAMMER_Y)
                    .addItemStack(new ItemStack(ModItems.CRUDE_HAMMER.get()));
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(CookingTableRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
    }
}