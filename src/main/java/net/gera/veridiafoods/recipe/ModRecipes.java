package net.gera.veridiafoods.recipe;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.recipe.cookingtable.CookingTableRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, VeridiaFoods.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, VeridiaFoods.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CookingTableRecipe>> COOKING_TABLE_SERIALIZER =
            SERIALIZERS.register("cooking_table", CookingTableRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CookingTableRecipe>> COOKING_TABLE_TYPE =
            TYPES.register("cooking_table", () -> new RecipeType<CookingTableRecipe>() {
                @Override
                public String toString() {
                    return "cooking_table";
                }
            });

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}