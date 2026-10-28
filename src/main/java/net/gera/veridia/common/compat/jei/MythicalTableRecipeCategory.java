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
import net.gera.veridia.common.recipe.mythicaltable.MythicalTableRecipe;
import net.gera.veridia.common.recipe.mythicaltable.SlotIngredient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MythicalTableRecipeCategory implements IRecipeCategory<MythicalTableRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "mythical_table");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,
            "textures/gui/mythical_table/mythical_table_gui.png");

    public static final RecipeType<MythicalTableRecipe> MYTHICAL_TABLE_RECIPE_RECIPE_TYPE =
            new RecipeType<>(UID, MythicalTableRecipe.class);

    private static final int LAPIS_X = 28, LAPIS_Y = 26;       // (^<)
    private static final int GEMSTONE_X = 28, GEMSTONE_Y = 44;   // (v<)
    private static final int PRIMARY_X = 57, PRIMARY_Y = 26;    // (^>)
    private static final int SECONDARY_X = 57, SECONDARY_Y = 44;  // (v>)

    private static final int OUTPUT_X = 127, OUTPUT_Y = 35;

    private final IDrawable background;
    private final IDrawable icon;

    public MythicalTableRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 80);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.MYTHICAL_TABLE.get()));
    }

    @Override
    public RecipeType<MythicalTableRecipe> getRecipeType() {
        return MYTHICAL_TABLE_RECIPE_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.veridia.mythical_table");
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
    public void setRecipe(IRecipeLayoutBuilder builder, MythicalTableRecipe recipe, IFocusGroup focuses) {
        addSlot(builder, recipe.lapis(), LAPIS_X, LAPIS_Y);
        addSlot(builder, recipe.gemstone(), GEMSTONE_X, GEMSTONE_Y);
        addSlot(builder, recipe.primary(), PRIMARY_X, PRIMARY_Y);
        addSlot(builder, recipe.secondary(), SECONDARY_X, SECONDARY_Y);

        // Выходной слот
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.output());
    }

    private void addSlot(IRecipeLayoutBuilder builder, java.util.Optional<SlotIngredient> slot, int x, int y) {
        slot.ifPresent(s -> {
            List<ItemStack> stacksWithCount = java.util.Arrays.stream(s.ingredient().getItems())
                    .map(stack -> {
                        ItemStack copy = stack.copy();
                        copy.setCount(s.count());
                        return copy;
                    })
                    .toList();

            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addItemStacks(stacksWithCount);
        });
    }

    @Override
    public void draw(MythicalTableRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);
    }
}