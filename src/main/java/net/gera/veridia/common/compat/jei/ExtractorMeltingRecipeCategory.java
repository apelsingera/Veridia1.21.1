package net.gera.veridia.common.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.recipe.extractor.ExtractorFluidRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class ExtractorMeltingRecipeCategory implements IRecipeCategory<ExtractorFluidRecipe> {
    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "extractor_melting");

    public static final RecipeType<ExtractorFluidRecipe> EXTRACTOR_MELTING_RECIPE_TYPE =
            new RecipeType<>(UID, ExtractorFluidRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public ExtractorMeltingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(100, 26);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.EXTRACTOR));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<ExtractorFluidRecipe> getRecipeType() {
        return EXTRACTOR_MELTING_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.veridia.extractor.melting");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ExtractorFluidRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 5)
                .addIngredients(recipe.input());

        FluidStack out = recipe.result();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 79, 1)
                .setFluidRenderer(out.getAmount(), true, 16, 24)
                .addFluidStack(out.getFluid(), out.getAmount());
    }

    @Override
    public void draw(ExtractorFluidRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
        arrow.draw(guiGraphics, 34, 4);
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
    }
}