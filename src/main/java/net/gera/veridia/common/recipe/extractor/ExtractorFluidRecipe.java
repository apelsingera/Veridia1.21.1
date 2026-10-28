package net.gera.veridia.common.recipe.extractor;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.gera.veridia.common.recipe.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Разжижение: предмет в среднем слоте экстрактора превращается в порцию жидкости, которая
 * заливается во входной танк. Результат не является предметом, поэтому {@link #assemble} пуст.
 *
 * <pre>{@code
 * {
 *   "type": "veridia:extractor_melting",
 *   "input": { "item": "veridia:tree_bark" },
 *   "result": { "id": "veridia:tannin", "amount": 250 }
 * }
 * }</pre>
 */
public record ExtractorFluidRecipe(Ingredient input, FluidStack result) implements Recipe<ExtractorFluidRecipeInput> {

    @Override
    public boolean matches(ExtractorFluidRecipeInput input, Level level) {
        return this.input.test(input.stack());
    }

    @Override
    public ItemStack assemble(ExtractorFluidRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.EXTRACTOR_MELTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.EXTRACTOR_MELTING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ExtractorFluidRecipe> {
        public static final MapCodec<ExtractorFluidRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(ExtractorFluidRecipe::input),
                FluidStack.CODEC.fieldOf("result").forGetter(ExtractorFluidRecipe::result)
        ).apply(inst, ExtractorFluidRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ExtractorFluidRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, ExtractorFluidRecipe::input,
                FluidStack.STREAM_CODEC, ExtractorFluidRecipe::result,
                ExtractorFluidRecipe::new);

        @Override
        public MapCodec<ExtractorFluidRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExtractorFluidRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
