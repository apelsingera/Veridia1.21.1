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
import net.gera.veridia.common.recipe.runicpedestal.RunicPedestalRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class RunicPedestalRecipeCategory implements IRecipeCategory<RunicPedestalRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "runic_pedestal");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,
            "textures/gui/runic_pedestal/runic_pedestal_gui.png");

    public static final RecipeType<RunicPedestalRecipe> RUNIC_PEDESTAL_RECIPE_RECIPE_TYPE =
            new RecipeType<>(UID, RunicPedestalRecipe.class);

    private static final int MATERIAL_1_X = 40, MATERIAL_1_Y = 35;
    private static final int MATERIAL_2_X = 58, MATERIAL_2_Y = 14;
    private static final int MATERIAL_3_X = 80, MATERIAL_3_Y = 6;
    private static final int MATERIAL_4_X = 102, MATERIAL_4_Y = 14;
    private static final int OUTPUT_X = 80, OUTPUT_Y = 58;

    // Те же координаты, что и в RunicPedestalScreen - оба используют одну систему координат
    // (176x80 верхняя часть runic_pedestal_gui.png).
    private static final int XP_TEXT_X = 100, XP_TEXT_Y = 58;
    private static final int XP_TEXT_COLOR = 0x80FF20; // цвет ванильного уровня опыта

    private final IDrawable background;
    private final IDrawable icon;

    public RunicPedestalRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 80);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.RUNIC_PEDESTAL.get()));
    }

    @Override
    public RecipeType<RunicPedestalRecipe> getRecipeType() {
        return RUNIC_PEDESTAL_RECIPE_RECIPE_TYPE;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RunicPedestalRecipe recipe, IFocusGroup focuses) {
        List<SlotIngredient> materials = recipe.materials();
        addSlot(builder, materials, 0, MATERIAL_1_X, MATERIAL_1_Y);
        addSlot(builder, materials, 1, MATERIAL_2_X, MATERIAL_2_Y);
        addSlot(builder, materials, 2, MATERIAL_3_X, MATERIAL_3_Y);
        addSlot(builder, materials, 3, MATERIAL_4_X, MATERIAL_4_Y);

        ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(result);
    }

    private void addSlot(IRecipeLayoutBuilder builder, List<SlotIngredient> materials, int index, int x, int y) {
        if (index >= materials.size()) return;
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
    public void draw(RunicPedestalRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
        IRecipeCategory.super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY);

        Component xpText = Component.translatable("gui.veridia.runic_pedestal_xp_amount", recipe.xpLevels());
        guiGraphics.drawString(Minecraft.getInstance().font, xpText, XP_TEXT_X, XP_TEXT_Y, XP_TEXT_COLOR, true);
    }
}