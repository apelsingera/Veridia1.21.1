package net.gera.veridia.common.worldgen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.bushes.BlueBerryBush;
import net.gera.veridia.common.block.bushes.CranBerryBush;
import net.gera.veridia.common.block.bushes.GooseBerryBush;
import net.gera.veridia.common.worldgen.feature.MagmaPatchConfiguration;
import net.gera.veridia.common.worldgen.feature.ModFeatures;
import net.gera.veridia.common.worldgen.feature.SpikeConfiguration;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {

    public static final int SCULK_SHROOM_PATCH_TRIES = 5;
    public static final int NETHER_BARIUM_VEIN_SIZE = 5;
    public static final int JUNGLE_BUSH_LOGS = 2;
    public static final int JUNGLE_BUSH_FOLIAGE_RADIUS = 2;
    public static final int JUNGLE_BUSH_FOLIAGE_OFFSET = 1;
    public static final int JUNGLE_BUSH_FOLIAGE_HEIGHT = 4;

    public static final ResourceKey<ConfiguredFeature<?, ?>> SCULK_TREE_KEY = registerKey("sculk_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SCULK_SHROOM_KEY = registerKey("sculk_shroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_BARIUM_KEY = registerKey("nether_barium_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BLUE_BERRY_KEY = registerKey("blue_berry_key");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GOOSE_BERRY_KEY = registerKey("goose_berry_key");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CRANBERRY_KEY = registerKey("cranberry_key");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CUMA_CUMA_BUSH_KEY = registerKey("cuma_cuma_bush");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MAGMA_PATCH_KEY = registerKey("magma_patch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LAVA_LAKE_KEY = registerKey("lava_lake");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TUFF_CLUSTER_KEY = registerKey("tuff_cluster");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SMOOTH_BASALT_CLUSTER_KEY = registerKey("smooth_basalt_cluster");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIKE_KEY = registerKey("spike");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RIVER_GRAVEL_DISK_KEY = registerKey("river_gravel_disk");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RIVER_TUFF_DISK_KEY = registerKey("river_tuff_disk");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ASHEN_GRASS_KEY = registerKey("ashen_grass");

    // Опалённые руды: копии ванильных жил (размер и шанс отбросить блок у воздуха те же) с первой целью "опалённый
    // камень -> опалённая руда"; остальные цели ванильные, так что в обычном камне ниже y=50 руда ванильная.
    // Взяты только руды, которые в ваниле бывают выше y=50 (границы опалённого камня): уголь, железо, медь, лазурит.
    public static final ResourceKey<ConfiguredFeature<?, ?>> SINGED_COAL_KEY = registerKey("singed_coal");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SINGED_COAL_BURIED_KEY = registerKey("singed_coal_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SINGED_IRON_KEY = registerKey("singed_iron");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SINGED_IRON_SMALL_KEY = registerKey("singed_iron_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SINGED_COPPER_KEY = registerKey("singed_copper");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SINGED_LAPIS_BURIED_KEY = registerKey("singed_lapis_buried");

    // Пепельная трава: попыток на пятно, разброс пятна по горизонтали и вертикали (у ванильной травы 32, 7, 3)
    private static final int ASHEN_GRASS_TRIES = 32;
    private static final int ASHEN_GRASS_XZ_SPREAD = 7;
    private static final int ASHEN_GRASS_Y_SPREAD = 3;

    // Диски на дне пепельной реки (у ванильных дисков песка и гравия радиус 2-6 и 2-5, полувысота 2)
    private static final IntProvider RIVER_GRAVEL_DISK_RADIUS = UniformInt.of(2, 6);
    private static final IntProvider RIVER_TUFF_DISK_RADIUS = UniformInt.of(2, 5);
    private static final int RIVER_DISK_HALF_HEIGHT = 2;

    // Каменные шипы: высота главного шипа, число шипов поменьше вокруг него и состав камня (вес блока)
    private static final int SPIKE_MIN_HEIGHT = 10;
    private static final int SPIKE_MAX_HEIGHT = 28;
    private static final int SPIKE_MIN_SATELLITES = 3;
    private static final int SPIKE_MAX_SATELLITES = 5;
    private static final int SPIKE_BLACKSTONE_WEIGHT = 5;
    private static final int SPIKE_BASALT_WEIGHT = 3;
    private static final int SPIKE_SMOOTH_BASALT_WEIGHT = 2;

    // Скопления туффа и гладкого базальта на поверхности: размер жилы (у ванильного туфа под землёй 64).
    // Жила наполовину торчит из земли, поэтому на поверхности видна неровным пятном.
    private static final int TUFF_CLUSTER_SIZE = 40;
    private static final int SMOOTH_BASALT_CLUSTER_SIZE = 32;
    // Что скопления заменяют: камень, чёрный камень поверхности биома и обычный грунт на случай, если правила поверхности не сработали
    private static final List<RuleTest> CLUSTER_TARGETS = List.of(
            new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD),
            new BlockMatchTest(Blocks.BLACKSTONE),
            new TagMatchTest(BlockTags.SAND),
            new TagMatchTest(BlockTags.DIRT),
            new TagMatchTest(BlockTags.TERRACOTTA));

    // Кучи blackstone_magma на поверхности: радиус и максимальная доля радиуса, занятая лавой в центре кучи
    private static final int MAGMA_PATCH_MIN_RADIUS = 4;
    private static final int MAGMA_PATCH_MAX_RADIUS = 10;
    private static final float MAGMA_PATCH_LAVA_FRACTION = 0.45F;
    public static final ResourceKey<ConfiguredFeature<?, ?>> JUNGLE_BUSH_KEY = registerKey("jungle_bush");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {


        register(context, SCULK_TREE_KEY, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.SCULK_LOG.get()),
                new ForkingTrunkPlacer(4, 4, 3),

                BlockStateProvider.simple(ModBlocks.SCULK_LEAVES.get()),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(3), 3),

                new TwoLayersFeatureSize(1, 0, 2)).build());
        register(context, NETHER_BARIUM_KEY, Feature.ORE, new OreConfiguration(
                new TagMatchTest(BlockTags.BASE_STONE_NETHER),
                ModBlocks.NETHER_BARIUM_ORE.get().defaultBlockState(),
                NETHER_BARIUM_VEIN_SIZE));
        register(context, SCULK_SHROOM_KEY, Feature.RANDOM_PATCH, FeatureUtils.simpleRandomPatchConfiguration(
                SCULK_SHROOM_PATCH_TRIES,
                PlacementUtils.onlyWhenEmpty(
                        Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.SCULK_MUSHROOM.get())))));
        register(context, BLUE_BERRY_KEY, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(
                Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.BLUE_BERRY_BUSH.get()
                        .defaultBlockState().setValue(BlueBerryBush.AGE, 1))), List.of(Blocks.GRASS_BLOCK)));
        register(context, GOOSE_BERRY_KEY, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(
                Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.GOOSE_BERRY_BUSH.get()
                        .defaultBlockState().setValue(GooseBerryBush.AGE, 1))), List.of(Blocks.GRASS_BLOCK)));
        register(context, CRANBERRY_KEY, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(
                Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.CRANBERRY_BUSH.get()
                        .defaultBlockState().setValue(CranBerryBush.AGE, 1))), List.of(Blocks.GRASS_BLOCK)));
        register(context, CUMA_CUMA_BUSH_KEY, ModFeatures.CUMA_CUMA_BUSH.get(), NoneFeatureConfiguration.INSTANCE);
        register(context, MAGMA_PATCH_KEY, ModFeatures.MAGMA_PATCH.get(), new MagmaPatchConfiguration(
                MAGMA_PATCH_MIN_RADIUS, MAGMA_PATCH_MAX_RADIUS, MAGMA_PATCH_LAVA_FRACTION));
        register(context, LAVA_LAKE_KEY, ModFeatures.LAVA_LAKE.get(), NoneFeatureConfiguration.INSTANCE);
        register(context, TUFF_CLUSTER_KEY, Feature.ORE, surfaceCluster(Blocks.TUFF, TUFF_CLUSTER_SIZE));
        register(context, SMOOTH_BASALT_CLUSTER_KEY, Feature.ORE,
                surfaceCluster(Blocks.SMOOTH_BASALT, SMOOTH_BASALT_CLUSTER_SIZE));
        register(context, SPIKE_KEY, ModFeatures.SPIKE.get(), new SpikeConfiguration(spikeStones(),
                SPIKE_MIN_HEIGHT, SPIKE_MAX_HEIGHT, SPIKE_MIN_SATELLITES, SPIKE_MAX_SATELLITES));
        register(context, RIVER_GRAVEL_DISK_KEY, Feature.DISK, riverBedDisk(Blocks.GRAVEL, RIVER_GRAVEL_DISK_RADIUS));
        register(context, RIVER_TUFF_DISK_KEY, Feature.DISK, riverBedDisk(Blocks.TUFF, RIVER_TUFF_DISK_RADIUS));
        register(context, ASHEN_GRASS_KEY, Feature.RANDOM_PATCH, ashenGrassPatch());
        registerSingedOres(context);
        register(context, JUNGLE_BUSH_KEY, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.JUNGLE_LOG),
                new StraightTrunkPlacer(JUNGLE_BUSH_LOGS, 0, 0),

                BlockStateProvider.simple(Blocks.JUNGLE_LEAVES),
                new FancyFoliagePlacer(ConstantInt.of(JUNGLE_BUSH_FOLIAGE_RADIUS),
                        ConstantInt.of(JUNGLE_BUSH_FOLIAGE_OFFSET), JUNGLE_BUSH_FOLIAGE_HEIGHT),

                new TwoLayersFeatureSize(0, 0, 0)).build());
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, name));
    }

    // Диск заменяет выжженную землю (дно реки), а на всякий случай и ванильную землю с дёрном
    private static DiskConfiguration riverBedDisk(Block block, IntProvider radius) {
        return new DiskConfiguration(RuleBasedBlockStateProvider.simple(block),
                BlockPredicate.matchesBlocks(ModBlocks.INCINERATED_DIRT.get(), Blocks.DIRT, Blocks.GRASS_BLOCK),
                radius, RIVER_DISK_HALF_HEIGHT);
    }

    private static void registerSingedOres(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Размер жилы и шанс отбросить блок у воздуха - как у ванильных ore_coal, ore_coal_buried, ore_iron,
        // ore_iron_small, ore_copper_small, ore_lapis_buried (прочитать их при генерации данных нельзя, поэтому числа здесь)
        singedOre(context, SINGED_COAL_KEY, ModBlocks.SINGED_COAL_ORE.get(), Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, 17, 0.0F);
        singedOre(context, SINGED_COAL_BURIED_KEY, ModBlocks.SINGED_COAL_ORE.get(), Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, 17, 0.5F);
        singedOre(context, SINGED_IRON_KEY, ModBlocks.SINGED_IRON_ORE.get(), Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, 9, 0.0F);
        singedOre(context, SINGED_IRON_SMALL_KEY, ModBlocks.SINGED_IRON_ORE.get(), Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, 4, 0.0F);
        singedOre(context, SINGED_COPPER_KEY, ModBlocks.SINGED_COPPER_ORE.get(), Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, 10, 0.0F);
        singedOre(context, SINGED_LAPIS_BURIED_KEY, ModBlocks.SINGED_LAPIS_ORE.get(), Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, 7, 1.0F);
    }

    // Жила с целями: опалённый камень -> опалённая руда (первая, поэтому главная), затем как в ваниле:
    // обычный камень -> обычная руда, сланец -> сланцевая руда
    private static void singedOre(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                  ResourceKey<ConfiguredFeature<?, ?>> key, Block singedOre, Block stoneOre,
                                  Block deepslateOre, int size, float discardChanceOnAirExposure) {
        List<OreConfiguration.TargetBlockState> targets = List.of(
                OreConfiguration.target(new BlockMatchTest(ModBlocks.SINGE_STONE.get()), singedOre.defaultBlockState()),
                OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), stoneOre.defaultBlockState()),
                OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), deepslateOre.defaultBlockState()));
        register(context, key, Feature.ORE, new OreConfiguration(targets, size, discardChanceOnAirExposure));
    }

    // Трава только на воздухе над выжженной землёй
    private static RandomPatchConfiguration ashenGrassPatch() {
        return new RandomPatchConfiguration(ASHEN_GRASS_TRIES, ASHEN_GRASS_XZ_SPREAD, ASHEN_GRASS_Y_SPREAD,
                PlacementUtils.filtered(Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.ASHEN_GRASS.get())),
                        BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                                BlockPredicate.matchesBlocks(Direction.DOWN.getNormal(), ModBlocks.INCINERATED_DIRT.get()))));
    }

    private static BlockStateProvider spikeStones() {
        return new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                .add(Blocks.BLACKSTONE.defaultBlockState(), SPIKE_BLACKSTONE_WEIGHT)
                .add(Blocks.BASALT.defaultBlockState(), SPIKE_BASALT_WEIGHT)
                .add(Blocks.SMOOTH_BASALT.defaultBlockState(), SPIKE_SMOOTH_BASALT_WEIGHT)
                .build());
    }

    // Шанс отбросить блок из-за контакта с воздухом = 0: на поверхности воздух рядом всегда, иначе жила бы пропадала
    private static OreConfiguration surfaceCluster(Block block, int size) {
        List<OreConfiguration.TargetBlockState> targets = CLUSTER_TARGETS.stream()
                .map(test -> OreConfiguration.target(test, block.defaultBlockState()))
                .toList();
        return new OreConfiguration(targets, size, 0.0F);
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}