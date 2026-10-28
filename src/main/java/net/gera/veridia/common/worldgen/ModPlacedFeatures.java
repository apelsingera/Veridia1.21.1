package net.gera.veridia.common.worldgen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class ModPlacedFeatures {

    private static final int SCULK_SHROOM_RARITY = 6;
    private static final int SCULK_SHROOM_TRIES_PER_CHUNK = 2;
    private static final int SCULK_SHROOM_MIN_Y = -58;
    private static final int SCULK_SHROOM_MAX_Y = 16;
    private static final int NETHER_BARIUM_COUNT = 2;
    private static final int NETHER_BARIUM_MIN_Y = 100;
    private static final int NETHER_BARIUM_MAX_Y = 122;
    private static final int CUMA_CUMA_BUSH_RARITY = 12;
    private static final int JUNGLE_BUSH_COUNT = 3;
    private static final float JUNGLE_BUSH_EXTRA_CHANCE = 0.2F;
    private static final int JUNGLE_BUSH_EXTRA_COUNT = 1;
    private static final int SPARSE_JUNGLE_TALL_GRASS_COUNT = 5;
    private static final int SPARSE_JUNGLE_LARGE_FERN_COUNT = 2;
    // Куча магмы в среднем раз на столько чанков (в биоме Пепельных пустошей)
    private static final int MAGMA_PATCH_CHUNKS_PER_PATCH = 3;
    // Скопления туффа и гладкого базальта: в среднем раз на столько чанков (в биоме Пепельных пустошей)
    private static final int TUFF_CLUSTER_CHUNKS_PER_CLUSTER = 3;
    private static final int SMOOTH_BASALT_CLUSTER_CHUNKS_PER_CLUSTER = 4;
    // Смещение точки размещения относительно воздуха над поверхностью: -1 - сам твёрдый блок поверхности
    private static final IntProvider SURFACE_BLOCK_OFFSET = ConstantInt.of(-1);
    // Центр жилы скопления: от блока поверхности до двух блоков под ним (ниже - меньше жилы торчит наружу)
    private static final IntProvider CLUSTER_OFFSET = UniformInt.of(-3, -1);
    // Группа каменных шипов: в среднем раз на столько чанков (в биоме Пепельных пустошей)
    private static final int SPIKE_CHUNKS_PER_GROUP = 6;
    // Диски на дне пепельной реки: сколько попыток на чанк (у ванильных: песок 3, гравий 1)
    private static final int RIVER_GRAVEL_DISK_COUNT = 3;
    private static final int RIVER_TUFF_DISK_COUNT = 1;
    // Пятен пепельной травы на чанк (в биомах пустошей и реки)
    private static final int ASHEN_GRASS_PATCHES_PER_CHUNK = 3;

    public static final ResourceKey<PlacedFeature> SCULK_TREE_PLACED_KEY = registerKey("sculk_tree_placed");
    public static final ResourceKey<PlacedFeature> SCULK_SHROOM_PLACED_KEY = registerKey("sculk_shroom_placed");
    public static final ResourceKey<PlacedFeature> BARIUM_NETHER_ORE_PLACED_KEY = registerKey("barium_nether_ore_placed");

    public static final ResourceKey<PlacedFeature> BLUE_BERRY_PLACED_KEY = registerKey("blue_berry_placed_key");
    public static final ResourceKey<PlacedFeature> GOOSE_BERRY_PLACED_KEY = registerKey("goose_berry_placed_key");
    public static final ResourceKey<PlacedFeature> CRANBERRY_PLACED_KEY = registerKey("cranberry_placed_key");
    public static final ResourceKey<PlacedFeature> CUMA_CUMA_BUSH_PLACED_KEY = registerKey("cuma_cuma_bush_placed");
    public static final ResourceKey<PlacedFeature> MAGMA_PATCH_PLACED_KEY = registerKey("magma_patch_placed");
    public static final ResourceKey<PlacedFeature> LAVA_LAKE_PLACED_KEY = registerKey("lava_lake_placed");
    public static final ResourceKey<PlacedFeature> TUFF_CLUSTER_PLACED_KEY = registerKey("tuff_cluster_placed");
    public static final ResourceKey<PlacedFeature> SMOOTH_BASALT_CLUSTER_PLACED_KEY = registerKey("smooth_basalt_cluster_placed");
    public static final ResourceKey<PlacedFeature> SPIKE_PLACED_KEY = registerKey("spike_placed");
    public static final ResourceKey<PlacedFeature> RIVER_GRAVEL_DISK_PLACED_KEY = registerKey("river_gravel_disk_placed");
    public static final ResourceKey<PlacedFeature> RIVER_TUFF_DISK_PLACED_KEY = registerKey("river_tuff_disk_placed");
    public static final ResourceKey<PlacedFeature> ASHEN_GRASS_PLACED_KEY = registerKey("ashen_grass_placed");

    // Опалённые руды: размещение (количество, высота, фильтр биома) берётся у ванильной руды, которую они заменяют
    public static final ResourceKey<PlacedFeature> SINGED_COAL_UPPER_PLACED_KEY = registerKey("singed_coal_upper");
    public static final ResourceKey<PlacedFeature> SINGED_COAL_LOWER_PLACED_KEY = registerKey("singed_coal_lower");
    public static final ResourceKey<PlacedFeature> SINGED_IRON_UPPER_PLACED_KEY = registerKey("singed_iron_upper");
    public static final ResourceKey<PlacedFeature> SINGED_IRON_MIDDLE_PLACED_KEY = registerKey("singed_iron_middle");
    public static final ResourceKey<PlacedFeature> SINGED_IRON_SMALL_PLACED_KEY = registerKey("singed_iron_small");
    public static final ResourceKey<PlacedFeature> SINGED_COPPER_PLACED_KEY = registerKey("singed_copper");
    public static final ResourceKey<PlacedFeature> SINGED_LAPIS_BURIED_PLACED_KEY = registerKey("singed_lapis_buried");
    public static final ResourceKey<PlacedFeature> JUNGLE_BUSH_PLACED_KEY = registerKey("jungle_bush_placed");
    public static final ResourceKey<PlacedFeature> SPARSE_JUNGLE_TALL_GRASS_PLACED_KEY = registerKey("sparse_jungle_tall_grass_placed");
    public static final ResourceKey<PlacedFeature> SPARSE_JUNGLE_LARGE_FERN_PLACED_KEY = registerKey("sparse_jungle_large_fern_placed");

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
        register(context, SCULK_SHROOM_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.SCULK_SHROOM_KEY),
                List.of(
                        RarityFilter.onAverageOnceEvery(SCULK_SHROOM_RARITY),
                        CountPlacement.of(SCULK_SHROOM_TRIES_PER_CHUNK),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(
                                VerticalAnchor.absolute(SCULK_SHROOM_MIN_Y),
                                VerticalAnchor.absolute(SCULK_SHROOM_MAX_Y)),
                        EnvironmentScanPlacement.scanningFor(Direction.DOWN,
                                BlockPredicate.solid(),
                                BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
                        RandomOffsetPlacement.vertical(ConstantInt.of(1)),
                        BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(
                                ModBlocks.SCULK_MUSHROOM.get().defaultBlockState(), BlockPos.ZERO)),
                        BiomeFilter.biome()
                ));
        register(context, BARIUM_NETHER_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.NETHER_BARIUM_KEY),
                List.of(
                        CountPlacement.of(NETHER_BARIUM_COUNT),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(
                                VerticalAnchor.absolute(NETHER_BARIUM_MIN_Y),
                                VerticalAnchor.absolute(NETHER_BARIUM_MAX_Y)),
                        BiomeFilter.biome()
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
        register(context, CUMA_CUMA_BUSH_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.CUMA_CUMA_BUSH_KEY),
                List.of(
                        RarityFilter.onAverageOnceEvery(CUMA_CUMA_BUSH_RARITY),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                        BiomeFilter.biome()
                ));
        register(context, MAGMA_PATCH_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.MAGMA_PATCH_KEY),
                surfacePlacement(MAGMA_PATCH_CHUNKS_PER_PATCH, SURFACE_BLOCK_OFFSET));
        // Без шанса и без смещения: фича идёт в каждом чанке и сама решает, есть ли здесь часть озера (см. LavaLakeField)
        register(context, LAVA_LAKE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.LAVA_LAKE_KEY),
                List.of());
        register(context, TUFF_CLUSTER_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.TUFF_CLUSTER_KEY),
                surfacePlacement(TUFF_CLUSTER_CHUNKS_PER_CLUSTER, CLUSTER_OFFSET));
        register(context, SMOOTH_BASALT_CLUSTER_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.SMOOTH_BASALT_CLUSTER_KEY),
                surfacePlacement(SMOOTH_BASALT_CLUSTER_CHUNKS_PER_CLUSTER, CLUSTER_OFFSET));
        register(context, SPIKE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.SPIKE_KEY),
                surfacePlacement(SPIKE_CHUNKS_PER_GROUP, SURFACE_BLOCK_OFFSET));
        register(context, RIVER_GRAVEL_DISK_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.RIVER_GRAVEL_DISK_KEY),
                riverBedDiskPlacement(RIVER_GRAVEL_DISK_COUNT));
        register(context, RIVER_TUFF_DISK_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.RIVER_TUFF_DISK_KEY),
                riverBedDiskPlacement(RIVER_TUFF_DISK_COUNT));
        register(context, ASHEN_GRASS_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.ASHEN_GRASS_KEY),
                List.of(
                        CountPlacement.of(ASHEN_GRASS_PATCHES_PER_CHUNK),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                        BiomeFilter.biome()
                ));
        registerSingedOres(context);
        register(context, JUNGLE_BUSH_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.JUNGLE_BUSH_KEY),
                List.of(
                        PlacementUtils.countExtra(JUNGLE_BUSH_COUNT, JUNGLE_BUSH_EXTRA_CHANCE, JUNGLE_BUSH_EXTRA_COUNT),
                        InSquarePlacement.spread(),
                        SurfaceWaterDepthFilter.forMaxDepth(0),
                        PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
                        PlacementUtils.filteredByBlockSurvival(Blocks.JUNGLE_SAPLING),
                        BiomeFilter.biome()
                ));
        register(context, SPARSE_JUNGLE_TALL_GRASS_PLACED_KEY, configuredFeatures.getOrThrow(VegetationFeatures.PATCH_TALL_GRASS),
                List.of(
                        CountPlacement.of(SPARSE_JUNGLE_TALL_GRASS_COUNT),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP,
                        BiomeFilter.biome()
                ));
        register(context, SPARSE_JUNGLE_LARGE_FERN_PLACED_KEY, configuredFeatures.getOrThrow(VegetationFeatures.PATCH_LARGE_FERN),
                List.of(
                        CountPlacement.of(SPARSE_JUNGLE_LARGE_FERN_COUNT),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP,
                        BiomeFilter.biome()
                ));

    }

    // Количество жил на чанк и высоты - как у ванильных ore_coal_upper/lower, ore_iron_upper/middle/small,
    // ore_copper и ore_lapis_buried (из OrePlacements; прочитать их при генерации данных нельзя)
    private static void registerSingedOres(BootstrapContext<PlacedFeature> context) {
        singedOre(context, SINGED_COAL_UPPER_PLACED_KEY, ModConfiguredFeatures.SINGED_COAL_KEY, 30,
                HeightRangePlacement.uniform(VerticalAnchor.absolute(136), VerticalAnchor.top()));
        singedOre(context, SINGED_COAL_LOWER_PLACED_KEY, ModConfiguredFeatures.SINGED_COAL_BURIED_KEY, 20,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(0), VerticalAnchor.absolute(192)));
        singedOre(context, SINGED_IRON_UPPER_PLACED_KEY, ModConfiguredFeatures.SINGED_IRON_KEY, 90,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(80), VerticalAnchor.absolute(384)));
        singedOre(context, SINGED_IRON_MIDDLE_PLACED_KEY, ModConfiguredFeatures.SINGED_IRON_KEY, 10,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-24), VerticalAnchor.absolute(56)));
        singedOre(context, SINGED_IRON_SMALL_PLACED_KEY, ModConfiguredFeatures.SINGED_IRON_SMALL_KEY, 10,
                HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(72)));
        singedOre(context, SINGED_COPPER_PLACED_KEY, ModConfiguredFeatures.SINGED_COPPER_KEY, 16,
                HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)));
        singedOre(context, SINGED_LAPIS_BURIED_PLACED_KEY, ModConfiguredFeatures.SINGED_LAPIS_BURIED_KEY, 4,
                HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)));
    }

    // Как ванильное commonOrePlacement: count жил на чанк, случайная точка в квадрате чанка и по высоте, фильтр биома
    private static void singedOre(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                                  ResourceKey<ConfiguredFeature<?, ?>> singedFeature, int count,
                                  PlacementModifier heightRange) {
        register(context, key, context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(singedFeature),
                List.of(CountPlacement.of(count), InSquarePlacement.spread(), heightRange, BiomeFilter.biome()));
    }

    // Раз в N чанков, в точке со смещением относительно воздуха над поверхностью без растений (OCEAN_FLOOR_WG)
    private static List<PlacementModifier> surfacePlacement(int chunksPerFeature, IntProvider verticalOffset) {
        return List.of(
                RarityFilter.onAverageOnceEvery(chunksPerFeature),
                InSquarePlacement.spread(),
                HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG),
                RandomOffsetPlacement.vertical(verticalOffset),
                BiomeFilter.biome());
    }

    // Как у ванильных дисков: верхний твёрдый блок, над которым вода (то есть дно)
    private static List<PlacementModifier> riverBedDiskPlacement(int count) {
        return List.of(
                CountPlacement.of(count),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID,
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesFluids(Fluids.WATER)),
                BiomeFilter.biome());
    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}