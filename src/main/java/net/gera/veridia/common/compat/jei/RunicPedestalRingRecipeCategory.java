package net.gera.veridia.common.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.recipe.mythicaltable.SlotIngredient;
import net.gera.veridia.common.recipe.runicpedestal.RunicPedestalRingRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class RunicPedestalRingRecipeCategory implements IRecipeCategory<RunicPedestalRingRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "runic_pedestal_ring");
    public static final RecipeType<RunicPedestalRingRecipe> RUNIC_PEDESTAL_RING_RECIPE_TYPE =
            new RecipeType<>(UID, RunicPedestalRingRecipe.class);

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_SPACING = 22;
    private static final int SLOT_Y = 4;
    private static final int MATERIAL_SLOTS = 4;
    private static final int OUTPUT_X = 4 + MATERIAL_SLOTS * SLOT_SPACING + 8;
    private static final int WIDTH = OUTPUT_X + SLOT_SIZE + 4;
    private static final int HEIGHT = 26;

    private final IDrawable background;
    private final IDrawable icon;

    public RunicPedestalRingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.RUNIC_PEDESTAL.get()));
    }

    @Override
    public RecipeType<RunicPedestalRingRecipe> getRecipeType() {
        return RUNIC_PEDESTAL_RING_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.veridia.runic_pedestal");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RunicPedestalRingRecipe recipe, IFocusGroup focuses) {
        List<SlotIngredient> materials = recipe.materials();
        for (int i = 0; i < materials.size() && i < MATERIAL_SLOTS; i++) {
            addSlot(builder, materials, i, 4 + i * SLOT_SPACING, SLOT_Y);
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addItemStack(new ItemStack(recipe.result()));
    }

    private void addSlot(IRecipeLayoutBuilder builder, List<SlotIngredient> materials, int index, int x, int y) {
        SlotIngredient material = materials.get(index);

        List<ItemStack> stacksWithCount = Arrays.stream(material.ingredient().getItems())
                .map(stack -> {
                    ItemStack copy = stack.copy();
                    copy.setCount(material.count());
                    return copy;
                })
                .toList();

        builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                .addItemStacks(stacksWithCount);
    }

    @Override
    public void draw(RunicPedestalRingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                      GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
    }
}