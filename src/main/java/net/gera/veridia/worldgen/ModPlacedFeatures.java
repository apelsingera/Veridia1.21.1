package net.gera.veridia.worldgen;

import net.gera.veridia.Veridia;
import net.gera.veridia.block.ModBlocks;
import net.gera.veridia.worldgen.oreHelper.OrePlacement;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> STONE_COPPER_ORE_PLACED_KEY = registerKey("stone_copper_ore_placed");
    public static final ResourceKey<PlacedFeature> DEEPSLATE_COPPER_ORE_PLACED_KEY = registerKey("deepslate_copper_ore_placed");
    public static final ResourceKey<PlacedFeature> GRANITE_COPPER_ORE_PLACED_KEY = registerKey("granite_copper_ore_placed");
    public static final ResourceKey<PlacedFeature> ANDESITE_COPPER_ORE_PLACED_KEY = registerKey("andesite_copper_ore_placed");
    public static final ResourceKey<PlacedFeature> DIORITE_COPPER_ORE_PLACED_KEY = registerKey("diorite_copper_ore_placed");
    public static final ResourceKey<PlacedFeature> TUFF_COPPER_ORE_PLACED_KEY = registerKey("tuff_copper_ore_placed");

    public static final ResourceKey<PlacedFeature> STONE_TIN_ORE_PLACED_KEY = registerKey("stone_tin_ore_placed");
    public static final ResourceKey<PlacedFeature> DEEPSLATE_TIN_ORE_PLACED_KEY = registerKey("deepslate_tin_ore_placed");
    public static final ResourceKey<PlacedFeature> GRANITE_TIN_ORE_PLACED_KEY = registerKey("granite_tin_ore_placed");
    public static final ResourceKey<PlacedFeature> ANDESITE_TIN_ORE_PLACED_KEY = registerKey("andesite_tin_ore_placed");
    public static final ResourceKey<PlacedFeature> DIORITE_TIN_ORE_PLACED_KEY = registerKey("diorite_tin_ore_placed");
    public static final ResourceKey<PlacedFeature> TUFF_TIN_ORE_PLACED_KEY = registerKey("tuff_tin_ore_placed");

    public static final ResourceKey<PlacedFeature> DATOLITE_NETHER_ORE_PLACED_KEY = registerKey("datolite_nether_ore_placed");
    public static final ResourceKey<PlacedFeature> AZOTIC_TOPAZ_END_ORE_PLACED_KEY = registerKey("azotic_topaz_end_ore_placed");

    public static final ResourceKey<PlacedFeature> SCULK_TREE_PLACED_KEY = registerKey("sculk_tree_placed");

    public static final ResourceKey<PlacedFeature> BLUE_BERRY_PLACED_KEY = registerKey("blue_berry_placed_key");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);



        List<PlacementModifier> overworldCopperPlacement = OrePlacement.rareCountOrePlacement(1, 8,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-21), VerticalAnchor.absolute(126)));
        register(context, STONE_COPPER_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.STONE_COPPER_KEY), overworldCopperPlacement);
        register(context, DEEPSLATE_COPPER_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.DEEPSLATE_COPPER_KEY), overworldCopperPlacement);
        register(context, GRANITE_COPPER_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.GRANITE_COPPER_KEY), overworldCopperPlacement);
        register(context, ANDESITE_COPPER_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.ANDESITE_COPPER_KEY), overworldCopperPlacement);
        register(context, DIORITE_COPPER_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.DIORITE_COPPER_KEY), overworldCopperPlacement);
        register(context, TUFF_COPPER_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.TUFF_COPPER_KEY), overworldCopperPlacement);

        List<PlacementModifier> overworldTinPlacement = OrePlacement.rareCountOrePlacement(1, 11,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(80)));
        register(context, STONE_TIN_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.STONE_TIN_KEY), overworldTinPlacement);
        register(context, DEEPSLATE_TIN_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.DEEPSLATE_TIN_KEY), overworldTinPlacement);
        register(context, GRANITE_TIN_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.GRANITE_TIN_KEY), overworldTinPlacement);
        register(context, ANDESITE_TIN_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.ANDESITE_TIN_KEY), overworldTinPlacement);
        register(context, DIORITE_TIN_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.DIORITE_TIN_KEY), overworldTinPlacement);
        register(context, TUFF_TIN_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.TUFF_TIN_KEY), overworldTinPlacement);

        register(context, DATOLITE_NETHER_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.NETHER_DATOLITE_KEY),
                OrePlacement.rareCountOrePlacement(3, 3, HeightRangePlacement.triangle(VerticalAnchor.absolute(60), VerticalAnchor.absolute(126))));
        register(context, AZOTIC_TOPAZ_END_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.END_AZOTIC_TOPAZ_KEY),
                OrePlacement.rareCountOrePlacement(3, 3, HeightRangePlacement.triangle(VerticalAnchor.absolute(0), VerticalAnchor.absolute(80))));

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
                        RarityFilter.onAverageOnceEvery(32),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                        BiomeFilter.biome()));

    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}