package net.gera.veridia.common.recipe;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.recipe.cookingtable.CookingTableRecipe;
import net.gera.veridia.common.recipe.extractor.ExtractorFluidRecipe;
import net.gera.veridia.common.recipe.extractor.ExtractorRecipe;
import net.gera.veridia.common.recipe.villagerupgrade.VillagerUpgradeRecipe;
import net.gera.veridia.common.recipe.mythicaltable.MythicalTableRecipe;
import net.gera.veridia.common.recipe.runicpedestal.RunicPedestalRecipe;
import net.gera.veridia.common.recipe.runicpedestal.RunicPedestalRingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Veridia.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Veridia.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CookingTableRecipe>> COOKING_TABLE_SERIALIZER =
            SERIALIZERS.register("cooking_table", CookingTableRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CookingTableRecipe>> COOKING_TABLE_TYPE =
            TYPES.register("cooking_table", () -> new RecipeType<CookingTableRecipe>() {
                @Override
                public String toString() {
                    return "cooking_table";
                }
            });
    public static final Supplier<RecipeSerializer<MythicalTableRecipe>> MYTHICAL_TABLE_SERIALIZER =
            SERIALIZERS.register("mythical_table", () -> new MythicalTableRecipe.Serializer());
    public static final Supplier<RecipeType<MythicalTableRecipe>> MYTHICAL_TABLE_TYPE =
            TYPES.register("mythical_table", () -> new RecipeType<MythicalTableRecipe>() {
                @Override
                public String toString() {
                    return "mythical_table";
                }
            });
    public static final Supplier<RecipeSerializer<RunicPedestalRecipe>> RUNIC_PEDESTAL_SERIALIZER =
            SERIALIZERS.register("runic_pedestal", () -> new RunicPedestalRecipe.Serializer());
    public static final Supplier<RecipeType<RunicPedestalRecipe>> RUNIC_PEDESTAL_TYPE =
            TYPES.register("runic_pedestal", () -> new RecipeType<RunicPedestalRecipe>() {
                @Override
                public String toString() {
                    return "runic_pedestal";
                }
            });
    public static final Supplier<RecipeSerializer<RunicPedestalRingRecipe>> RUNIC_PEDESTAL_RING_SERIALIZER =
            SERIALIZERS.register("runic_pedestal_ring", () -> new RunicPedestalRingRecipe.Serializer());
    public static final Supplier<RecipeType<RunicPedestalRingRecipe>> RUNIC_PEDESTAL_RING_TYPE =
            TYPES.register("runic_pedestal_ring", () -> new RecipeType<RunicPedestalRingRecipe>() {
                @Override
                public String toString() {
                    return "runic_pedestal_ring";
                }
            });

    public static final Supplier<RecipeSerializer<ExtractorRecipe>> EXTRACTING_SERIALIZER =
            SERIALIZERS.register("extracting", ExtractorRecipe.Serializer::new);
    public static final Supplier<RecipeType<ExtractorRecipe>> EXTRACTING_TYPE =
            TYPES.register("extracting", () -> new RecipeType<ExtractorRecipe>() {
                @Override
                public String toString() {
                    return "extracting";
                }
            });

    public static final Supplier<RecipeSerializer<ExtractorFluidRecipe>> EXTRACTOR_MELTING_SERIALIZER =
            SERIALIZERS.register("extractor_melting", ExtractorFluidRecipe.Serializer::new);
    public static final Supplier<RecipeType<ExtractorFluidRecipe>> EXTRACTOR_MELTING_TYPE =
            TYPES.register("extractor_melting", () -> new RecipeType<ExtractorFluidRecipe>() {
                @Override
                public String toString() {
                    return "extractor_melting";
                }
            });

    // Один общий тип рецепта на стоимость апгрейдов ЛЮБОЙ профессии (lumberjack, farmer, ...) -
    // см. VillagerUpgradeTree/VillagerUpgradeRecipe.
    public static final Supplier<RecipeSerializer<VillagerUpgradeRecipe>> VILLAGER_UPGRADE_SERIALIZER =
            SERIALIZERS.register("villager_upgrade", () -> new VillagerUpgradeRecipe.Serializer());
    public static final Supplier<RecipeType<VillagerUpgradeRecipe>> VILLAGER_UPGRADE_TYPE =
            TYPES.register("villager_upgrade", () -> new RecipeType<VillagerUpgradeRecipe>() {
                @Override
                public String toString() {
                    return "villager_upgrade";
                }
            });

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}