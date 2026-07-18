package net.gera.veridia.worldgen;

import net.gera.veridia.Veridia;
import net.gera.veridia.block.ModBlocks;
import net.gera.veridia.block.bushes.BlueBerryBush;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> STONE_COPPER_KEY = registerKey("stone_copper_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEPSLATE_COPPER_KEY = registerKey("deepslate_copper_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GRANITE_COPPER_KEY = registerKey("granite_copper_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ANDESITE_COPPER_KEY = registerKey("andesite_copper_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DIORITE_COPPER_KEY = registerKey("diorite_copper_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TUFF_COPPER_KEY = registerKey("tuff_copper_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> STONE_TIN_KEY = registerKey("stone_tin_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEPSLATE_TIN_KEY = registerKey("deepslate_tin_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GRANITE_TIN_KEY = registerKey("granite_tin_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ANDESITE_TIN_KEY = registerKey("andesite_tin_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DIORITE_TIN_KEY = registerKey("diorite_tin_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TUFF_TIN_KEY = registerKey("tuff_tin_ore");

    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_DATOLITE_KEY = registerKey("datolite_nether_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> END_AZOTIC_TOPAZ_KEY = registerKey("azotic_topaz_end_ore");
    //TREES
    public static final ResourceKey<ConfiguredFeature<?, ?>> SCULK_TREE_KEY = registerKey("sculk_tree");
    //BUSH
    public static final ResourceKey<ConfiguredFeature<?, ?>> BLUE_BERRY_KEY = registerKey("blue_berry_key");




    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {

        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        RuleTest graniteReplaceables = new BlockMatchTest(Blocks.GRANITE);
        RuleTest andesiteReplaceables = new BlockMatchTest(Blocks.ANDESITE);
        RuleTest dioriteReplaceables = new BlockMatchTest(Blocks.DIORITE);
        RuleTest tuffReplaceables = new BlockMatchTest(Blocks.TUFF);

        RuleTest netherrackReplaceables = new BlockMatchTest(Blocks.NETHERRACK);
        RuleTest endReplaceables = new BlockMatchTest(Blocks.END_STONE);


        register(context, STONE_COPPER_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(stoneReplaceables, ModBlocks.COPPER_ORE.get().defaultBlockState())), 38));
        register(context, DEEPSLATE_COPPER_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(deepslateReplaceables, ModBlocks.DEEPSLATE_COPPER_ORE.get().defaultBlockState())), 38));
        register(context, GRANITE_COPPER_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(graniteReplaceables, ModBlocks.GRANITE_COPPER_ORE.get().defaultBlockState())), 42));
        register(context, ANDESITE_COPPER_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(andesiteReplaceables, ModBlocks.ANDESITE_COPPER_ORE.get().defaultBlockState())), 33));
        register(context, DIORITE_COPPER_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(dioriteReplaceables, ModBlocks.DIORITE_COPPER_ORE.get().defaultBlockState())), 32));
        register(context, TUFF_COPPER_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(tuffReplaceables, ModBlocks.TUFF_COPPER_ORE.get().defaultBlockState())), 33));

        register(context, STONE_TIN_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(stoneReplaceables, ModBlocks.TIN_ORE.get().defaultBlockState())), 19));
        register(context, DEEPSLATE_TIN_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(deepslateReplaceables, ModBlocks.DEEPSLATE_TIN_ORE.get().defaultBlockState())), 27));
        register(context, GRANITE_TIN_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(graniteReplaceables, ModBlocks.GRANITE_TIN_ORE.get().defaultBlockState())), 20));
        register(context, ANDESITE_TIN_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(andesiteReplaceables, ModBlocks.ANDESITE_TIN_ORE.get().defaultBlockState())), 20));
        register(context, DIORITE_TIN_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(dioriteReplaceables, ModBlocks.DIORITE_TIN_ORE.get().defaultBlockState())), 21));
        register(context, TUFF_TIN_KEY, Feature.ORE, new OreConfiguration(
                List.of(OreConfiguration.target(tuffReplaceables, ModBlocks.TUFF_TIN_ORE.get().defaultBlockState())), 22));

        register(context, NETHER_DATOLITE_KEY, Feature.ORE, new OreConfiguration(netherrackReplaceables,
                ModBlocks.DATOLITE_NETHER_ORE.get().defaultBlockState(), 5));
        register(context, END_AZOTIC_TOPAZ_KEY, Feature.ORE, new OreConfiguration(endReplaceables,
                ModBlocks.AZOTIC_TOPAZ_END_ORE.get().defaultBlockState(), 2));

        register(context, SCULK_TREE_KEY, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.SCULK_LOG.get()),
                new ForkingTrunkPlacer(4, 4, 3),

                BlockStateProvider.simple(ModBlocks.SCULK_LEAVES.get()),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(3), 3),

                new TwoLayersFeatureSize(1, 0, 2)).build());
        register(context, BLUE_BERRY_KEY, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(
                Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.BLUE_BERRY_BUSH.get()
                        .defaultBlockState().setValue(BlueBerryBush.AGE, 3))), List.of(Blocks.GRASS_BLOCK)));
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}