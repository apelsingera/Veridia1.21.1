package net.gera.veridiafoods.worldgen;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class ModPlacedFeatures {

    public static final ResourceKey<PlacedFeature> SCULK_TREE_PLACED_KEY = registerKey("sculk_tree_placed");

    public static final ResourceKey<PlacedFeature> BLUE_BERRY_PLACED_KEY = registerKey("blue_berry_placed_key");
    public static final ResourceKey<PlacedFeature> GOOSE_BERRY_PLACED_KEY = registerKey("goose_berry_placed_key");
    public static final ResourceKey<PlacedFeature> CRANBERRY_PLACED_KEY = registerKey("cranberry_placed_key");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);


        register(context, SCULK_TREE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.SCULK_TREE_KEY),
                List.of(
                        RarityFilter.onAverageOnceEvery(250),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                        PlacementUtils.filteredByBlockSurvival(ModBlocks.SCULK_SPROUT.get()),
                        SurfaceWaterDepthFilter.forMaxDepth(0)
                ));
        register(context, BLUE_BERRY_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.BLUE_BERRY_KEY),
                List.of(
                        RarityFilter.onAverageOnceEvery(40),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                        BiomeFilter.biome()
                ));
        register(context, GOOSE_BERRY_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.GOOSE_BERRY_KEY),
                List.of(
                        RarityFilter.onAverageOnceEvery(50),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                        BiomeFilter.biome()
                ));
        register(context, CRANBERRY_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.CRANBERRY_KEY),
                List.of(
                        RarityFilter.onAverageOnceEvery(40),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                        BiomeFilter.biome()
                ));

    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(VeridiaFoods.MOD_ID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}