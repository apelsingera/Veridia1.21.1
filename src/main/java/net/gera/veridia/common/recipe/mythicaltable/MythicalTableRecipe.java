package net.gera.veridia.common.recipe.mythicaltable;

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

import java.util.Optional;

public record MythicalTableRecipe(Optional<SlotIngredient> lapis,
                                  Optional<SlotIngredient> gemstone,
                                  Optional<SlotIngredient> primary,
                                  Optional<SlotIngredient> secondary,
                                  ItemStack output) implements Recipe<MythicalTableRecipeInput> {

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        lapis.ifPresent(s -> list.add(s.ingredient()));
        gemstone.ifPresent(s -> list.add(s.ingredient()));
        primary.ifPresent(s -> list.add(s.ingredient()));
        secondary.ifPresent(s -> list.add(s.ingredient()));
        return list;
    }

    @Override
    public boolean matches(MythicalTableRecipeInput input, Level level) {
        if (level.isClientSide()) return false;

        return slotMatches(lapis, input.lapis())
                && slotMatches(gemstone, input.gemstone())
                && slotMatches(primary, input.primary())
                && slotMatches(secondary, input.secondary());
    }

    private boolean slotMatches(Optional<SlotIngredient> recipeSlot, ItemStack actualStack) {
        if (recipeSlot.isEmpty()) {
            return actualStack.isEmpty();
        }
        return recipeSlot.get().matches(actualStack);
    }

    @Override
    public ItemStack assemble(MythicalTableRecipeInput input, HolderLookup.Provider provider) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MYTHICAL_TABLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MYTHICAL_TABLE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<MythicalTableRecipe> {
        public static final MapCodec<MythicalTableRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                SlotIngredient.CODEC.optionalFieldOf("lapis").forGetter(MythicalTableRecipe::lapis),
                SlotIngredient.CODEC.optionalFieldOf("gemstone").forGetter(MythicalTableRecipe::gemstone),
                SlotIngredient.CODEC.optionalFieldOf("primary").forGetter(MythicalTableRecipe::primary),
                SlotIngredient.CODEC.optionalFieldOf("secondary").forGetter(MythicalTableRecipe::secondary),
                ItemStack.CODEC.fieldOf("result").forGetter(MythicalTableRecipe::output)
        ).apply(inst, MythicalTableRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MythicalTableRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(SlotIngredient.STREAM_CODEC), MythicalTableRecipe::lapis,
                ByteBufCodecs.optional(SlotIngredient.STREAM_CODEC), MythicalTableRecipe::gemstone,
                ByteBufCodecs.optional(SlotIngredient.STREAM_CODEC), MythicalTableRecipe::primary,
                ByteBufCodecs.optional(SlotIngredient.STREAM_CODEC), MythicalTableRecipe::secondary,
                ItemStack.STREAM_CODEC, MythicalTableRecipe::output,
                MythicalTableRecipe::new
        );

        @Override
        public @NotNull MapCodec<MythicalTableRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, MythicalTableRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}