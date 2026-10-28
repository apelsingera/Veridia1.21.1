package net.gera.veridia.common.recipe.cookingtable;

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

import java.util.ArrayList;
import java.util.List;

public record CookingTableRecipe(List<Ingredient> ingredients, ToolRequirement toolRequirement,
                                 boolean consumeContainers, ItemStack output)
        implements Recipe<CookingTableRecipeInput> {

    private static final int TOOL_SLOT_KNIFE = 9;
    private static final int TOOL_SLOT_HAMMER = 10;

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(ingredients);
        return list;
    }

    @Override
    public boolean matches(CookingTableRecipeInput input, Level level) {
        if (level.isClientSide()) {
            return false;
        }

        if (!toolMatches(input)) {
            return false;
        }

        List<ItemStack> available = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                available.add(stack);
            }
        }

        if (available.size() != ingredients.size()) {
            return false;
        }

        List<ItemStack> remaining = new ArrayList<>(available);
        for (Ingredient ingredient : ingredients) {
            ItemStack match = remaining.stream().filter(ingredient::test).findFirst().orElse(null);
            if (match == null) {
                return false;
            }
            remaining.remove(match);
        }
        return true;
    }

    private boolean toolMatches(CookingTableRecipeInput input) {
        return switch (toolRequirement) {
            case NONE -> true;
            case HAMMER -> !input.getItem(TOOL_SLOT_HAMMER).isEmpty();
            case KNIFE -> !input.getItem(TOOL_SLOT_KNIFE).isEmpty();
        };
    }

    @Override
    public ItemStack assemble(CookingTableRecipeInput input, HolderLookup.Provider provider) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COOKING_TABLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COOKING_TABLE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<CookingTableRecipe> {
        public static final MapCodec<CookingTableRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.listOf(1, 9).fieldOf("ingredients")
                        .forGetter(CookingTableRecipe::ingredients),
                ToolRequirement.CODEC.optionalFieldOf("tool_requirement", ToolRequirement.NONE)
                        .forGetter(CookingTableRecipe::toolRequirement),
                Codec.BOOL.optionalFieldOf("consume_containers", false)
                        .forGetter(CookingTableRecipe::consumeContainers),
                ItemStack.CODEC.fieldOf("result").forGetter(CookingTableRecipe::output)
        ).apply(inst, CookingTableRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CookingTableRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(9)), CookingTableRecipe::ingredients,
                        StreamCodec.of(
                                (buf, val) -> buf.writeVarInt(val.getId()),
                                buf -> ToolRequirement.byId(buf.readVarInt())
                        ), CookingTableRecipe::toolRequirement,
                        ByteBufCodecs.BOOL, CookingTableRecipe::consumeContainers,
                        ItemStack.STREAM_CODEC, CookingTableRecipe::output,
                        CookingTableRecipe::new);

        @Override
        public MapCodec<CookingTableRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CookingTableRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}