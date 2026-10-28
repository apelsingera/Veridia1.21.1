package net.gera.veridia.common.recipe.extractor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.gera.veridia.common.recipe.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.Optional;

/**
 * Основной рецепт экстрактора: жидкость из входного танка (+ опционально доп. ингредиент из верхнего
 * слота) -> предмет в выходной слот. Жидкость набирается либо переработкой предмета в среднем слоте
 * (см. {@link ExtractorFluidRecipe}), либо заливается снаружи (труба/ведро) - рецепту всё равно, откуда.
 *
 * <pre>{@code
 * {
 *   "type": "veridia:extracting",
 *   "ingredient": { "item": "minecraft:sugar" },              // опционально
 *   "upgrade": { "item": "veridia:worker_upgrade" },     // опционально: нужен апгрейд в слоте
 *   "fluid": { "ingredient": { "fluid": "veridia:tannin" }, "amount": 250 },
 *   "result": { "id": "veridia:tanned_leather", "count": 1 },
 *   "time": 200                                               // опционально, тики
 * }
 * }</pre>
 */
public record ExtractorRecipe(Optional<Ingredient> ingredient, Optional<Ingredient> upgrade,
                              SizedFluidIngredient fluid, ItemStack result, int time)
        implements Recipe<ExtractorRecipeInput> {

    public static final int DEFAULT_TIME = 200;

    @Override
    public boolean matches(ExtractorRecipeInput input, Level level) {
        if (!fluid.test(input.fluid())) {
            return false;
        }
        // Апгрейд не расходуется - просто должен лежать в слоте (128,35).
        if (upgrade.isPresent() && !upgrade.get().test(input.upgrade())) {
            return false;
        }
        if (ingredient.isPresent()) {
            return ingredient.get().test(input.ingredient());
        }
        // Рецепт без доп. ингредиента срабатывает только если верхний слот пуст - иначе рецепты
        // "с ингредиентом" и "без" перекрывались бы на одном и том же наборе.
        return input.ingredient().isEmpty();
    }

    public boolean consumesIngredient() {
        return ingredient.isPresent();
    }

    public boolean requiresUpgrade() {
        return upgrade.isPresent();
    }

    @Override
    public ItemStack assemble(ExtractorRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.EXTRACTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.EXTRACTING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ExtractorRecipe> {
        public static final MapCodec<ExtractorRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.optionalFieldOf("ingredient").forGetter(ExtractorRecipe::ingredient),
                Ingredient.CODEC_NONEMPTY.optionalFieldOf("upgrade").forGetter(ExtractorRecipe::upgrade),
                SizedFluidIngredient.NESTED_CODEC.fieldOf("fluid").forGetter(ExtractorRecipe::fluid),
                ItemStack.CODEC.fieldOf("result").forGetter(ExtractorRecipe::result),
                Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(ExtractorRecipe::time)
        ).apply(inst, ExtractorRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ExtractorRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), ExtractorRecipe::ingredient,
                ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), ExtractorRecipe::upgrade,
                SizedFluidIngredient.STREAM_CODEC, ExtractorRecipe::fluid,
                ItemStack.STREAM_CODEC, ExtractorRecipe::result,
                ByteBufCodecs.VAR_INT, ExtractorRecipe::time,
                ExtractorRecipe::new);

        @Override
        public MapCodec<ExtractorRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExtractorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
