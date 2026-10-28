package net.gera.veridia.common.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.recipe.extractor.ExtractorRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ExtractorRecipeCategory implements IRecipeCategory<ExtractorRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "extracting");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,
            "textures/gui/extractor/extractor_gui.png");

    public static final RecipeType<ExtractorRecipe> EXTRACTING_RECIPE_TYPE =
            new RecipeType<>(UID, ExtractorRecipe.class);

    private static final int INGREDIENT_X = 94, INGREDIENT_Y = 13;
    private static final int UPGRADE_X = 128, UPGRADE_Y = 35;
    private static final int OUTPUT_X = 94, OUTPUT_Y = 58;
    private static final int TANK_X = 65, TANK_Y = 41, TANK_W = 16, TANK_H = 33;

    private final IDrawable background;
    private final IDrawable icon;

    public ExtractorRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 78);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.EXTRACTOR));
    }

    @Override
    public RecipeType<ExtractorRecipe> getRecipeType() {
        return EXTRACTING_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.veridia.extractor");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ExtractorRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, TANK_X, TANK_Y)
                .setFluidRenderer(recipe.fluid().amount(), true, TANK_W, TANK_H)
                .addIngredients(NeoForgeTypes.FLUID_STACK, Arrays.asList(recipe.fluid().getFluids()));

        recipe.ingredient().ifPresent(ing ->
                builder.addSlot(RecipeIngredientRole.INPUT, INGREDIENT_X, INGREDIENT_Y).addIngredients(ing));

        recipe.upgrade().ifPresent(up ->
                builder.addSlot(RecipeIngredientRole.CATALYST, UPGRADE_X, UPGRADE_Y).addIngredients(up));

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(ExtractorRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);

        Component time = Component.translatable("gui.veridia.extractor.seconds", String.format("%.1f", recipe.time() / 20.0))
                .withStyle(ChatFormatting.DARK_GRAY);
        guiGraphics.drawString(Minecraft.getInstance().font, time, 118, 15, 0xFF404040, false);
    }
}