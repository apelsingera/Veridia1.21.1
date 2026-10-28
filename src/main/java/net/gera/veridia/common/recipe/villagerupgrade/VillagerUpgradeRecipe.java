package net.gera.veridia.common.recipe.villagerupgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.gera.veridia.common.recipe.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Стоимость разблокировки узла ЛЮБОГО дерева улучшений жителя (LumberjackUpgrades,
 * FarmerUpgrades, и т.п. - см. общий движок VillagerUpgradeTree) - один общий тип рецепта
 * "veridia:villager_upgrade" на все профессии, JSON лежат в
 * data/veridia/recipe/villager_upgrade/<профессия>/<id узла>.json. Пока всегда 1 ингредиент
 * (count), но это Ingredient - принимает и теги, датапаки могут менять/добавлять узлы без
 * пересборки мода.
 */
public record VillagerUpgradeRecipe(Ingredient ingredient, int count) implements Recipe<VillagerUpgradeRecipeInput> {

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
        return list;
    }

    @Override
    public boolean matches(VillagerUpgradeRecipeInput input, Level level) {
        ItemStack stack = input.stack();
        return !stack.isEmpty() && ingredient.test(stack) && stack.getCount() >= count;
    }

    @Override
    public ItemStack assemble(VillagerUpgradeRecipeInput input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.VILLAGER_UPGRADE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.VILLAGER_UPGRADE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<VillagerUpgradeRecipe> {
        public static final MapCodec<VillagerUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(VillagerUpgradeRecipe::ingredient),
                Codec.INT.optionalFieldOf("count", 1).forGetter(VillagerUpgradeRecipe::count)
        ).apply(inst, VillagerUpgradeRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, VillagerUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, VillagerUpgradeRecipe::ingredient,
                ByteBufCodecs.VAR_INT, VillagerUpgradeRecipe::count,
                VillagerUpgradeRecipe::new
        );

        @Override
        public @NotNull MapCodec<VillagerUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, VillagerUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
