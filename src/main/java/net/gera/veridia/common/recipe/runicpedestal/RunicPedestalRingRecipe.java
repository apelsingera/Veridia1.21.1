package net.gera.veridia.common.recipe.runicpedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.gera.veridia.common.recipe.knowledge.RecipeKnowledge;
import net.gera.veridia.common.recipe.ModRecipes;
import net.gera.veridia.common.recipe.mythicaltable.SlotIngredient;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Второй, независимый тип рецепта Рунического пьедестала - в отличие от RunicPedestalRecipe (руны
 * по шагам ManaProgression), не завязан на ману вообще. Требует, чтобы игрок открыл знание тира
 * через AncientDocumentItem (см. RecipeKnowledge.KnowledgeTier.isUnlocked) - смотри
 * RunicPedestalBlockEntity.requestCraft/currentRingRecipe. Крафтится мгновенно по нажатию той же
 * кнопки, без анимации прогресса/траты опыта - это отдельная, более простая ветка.
 *
 * requiredKnowledge - это СТРОКОВЫЙ id тира (RecipeKnowledge.KnowledgeTier.id, например
 * "knockback_ring_2" или "slow_killer_2"), а не голый номер - несколько деревьев знаний могут иметь
 * тиры с одинаковым номером (оба дерева нумеруются 2..5), голого int было бы недостаточно, чтобы
 * различить их.
 */
public record RunicPedestalRingRecipe(String requiredKnowledge, List<SlotIngredient> materials,
                                       Item result) implements Recipe<RunicPedestalRecipeInput> {

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

    public boolean isUnlockedFor(Player player) {
        return RecipeKnowledge.byId(requiredKnowledge)
                .map(tier -> tier.isUnlocked(player))
                .orElse(false);
    }

    /** Для превью рецепта в AncientDocumentScreen (клиентский RecipeManager синкается вместе с остальными). */
    public static Optional<RunicPedestalRingRecipe> findForKnowledge(Level level, String knowledgeId) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.RUNIC_PEDESTAL_RING_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.requiredKnowledge().equals(knowledgeId))
                .findFirst();
    }

    @Override
    public ItemStack assemble(RunicPedestalRecipeInput input, HolderLookup.Provider provider) {
        return new ItemStack(result);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return new ItemStack(result);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RUNIC_PEDESTAL_RING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.RUNIC_PEDESTAL_RING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<RunicPedestalRingRecipe> {
        public static final MapCodec<RunicPedestalRingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.STRING.fieldOf("required_knowledge").forGetter(RunicPedestalRingRecipe::requiredKnowledge),
                SlotIngredient.CODEC.listOf(1, 4).fieldOf("materials").forGetter(RunicPedestalRingRecipe::materials),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(RunicPedestalRingRecipe::result)
        ).apply(inst, RunicPedestalRingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, RunicPedestalRingRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RunicPedestalRingRecipe::requiredKnowledge,
                SlotIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(4)), RunicPedestalRingRecipe::materials,
                ByteBufCodecs.registry(Registries.ITEM), RunicPedestalRingRecipe::result,
                RunicPedestalRingRecipe::new
        );

        @Override
        public @NotNull MapCodec<RunicPedestalRingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, RunicPedestalRingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
