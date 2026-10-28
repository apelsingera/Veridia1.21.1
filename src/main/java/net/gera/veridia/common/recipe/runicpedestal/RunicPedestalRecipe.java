package net.gera.veridia.common.recipe.runicpedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.gera.veridia.common.mana.ModManaItems;
import net.gera.veridia.common.recipe.ModRecipes;
import net.gera.veridia.common.recipe.mythicaltable.SlotIngredient;
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

import java.util.List;

public record RunicPedestalRecipe(int step, List<SlotIngredient> materials,
                                   int xpLevels) implements Recipe<RunicPedestalRecipeInput> {

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        materials.forEach(material -> list.add(material.ingredient()));
        return list;
    }

    @Override
    public boolean matches(RunicPedestalRecipeInput input, Level level) {
        if (level.isClientSide()) return false;

        for (int i = 0; i < materials.size(); i++) {
            if (!materials.get(i).matches(input.getItem(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(RunicPedestalRecipeInput input, HolderLookup.Provider provider) {
        return chargedRune();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return chargedRune();
    }

    private ItemStack chargedRune() {
        return new ItemStack(ModManaItems.itemForStep(step).get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RUNIC_PEDESTAL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.RUNIC_PEDESTAL_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<RunicPedestalRecipe> {
        public static final MapCodec<RunicPedestalRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.INT.fieldOf("step").forGetter(RunicPedestalRecipe::step),
                SlotIngredient.CODEC.listOf(4, 4).fieldOf("materials").forGetter(RunicPedestalRecipe::materials),
                Codec.INT.fieldOf("xp_levels").forGetter(RunicPedestalRecipe::xpLevels)
        ).apply(inst, RunicPedestalRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, RunicPedestalRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RunicPedestalRecipe::step,
                SlotIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(4)), RunicPedestalRecipe::materials,
                ByteBufCodecs.VAR_INT, RunicPedestalRecipe::xpLevels,
                RunicPedestalRecipe::new
        );

        @Override
        public @NotNull MapCodec<RunicPedestalRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, RunicPedestalRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
