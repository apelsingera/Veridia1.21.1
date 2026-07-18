package net.gera.veridia.worldgen;

import net.gera.veridia.Veridia;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Collections;

public class BiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_COPPER_ORE = registerKey("add_copper_ore");
    public static final ResourceKey<BiomeModifier> ADD_TIN_ORE = registerKey("add_tin_ore");
    public static final ResourceKey<BiomeModifier> ADD_DATOLITE_NETHER_ORE = registerKey("add_datolite_nether_ore");
    public static final ResourceKey<BiomeModifier> ADD_AZOTIC_TOPAZ_END_ORE = registerKey("add_azotic_topaz_end_ore");

    public static final ResourceKey<BiomeModifier> REMOVE_COPPER_ORE = registerKey("remove_copper_ore");

    public static final ResourceKey<BiomeModifier> ADD_TREE_SCULK = registerKey("add_tree_sculk");

    public static final ResourceKey<BiomeModifier> ADD_BLUE_BERRY = registerKey("add_blue_berry");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        context.register(REMOVE_COPPER_ORE, new net.neoforged.neoforge.common.world.BiomeModifiers.RemoveFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(
                        placedFeatures.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.withDefaultNamespace("ore_copper"))),
                        placedFeatures.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.withDefaultNamespace("ore_copper_large")))
                ),
                Collections.singleton(GenerationStep.Decoration.UNDERGROUND_ORES)));

        context.register(ADD_COPPER_ORE, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(
                        placedFeatures.getOrThrow(ModPlacedFeatures.STONE_COPPER_ORE_PLACED_KEY),
                        placedFeatures.getOrThrow(ModPlacedFeatures.DEEPSLATE_COPPER_ORE_PLACED_KEY),
                        placedFeatures.getOrThrow(ModPlacedFeatures.GRANITE_COPPER_ORE_PLACED_KEY),
                        placedFeatures.getOrThrow(ModPlacedFeatures.ANDESITE_COPPER_ORE_PLACED_KEY),
                        placedFeatures.getOrThrow(ModPlacedFeatures.DIORITE_COPPER_ORE_PLACED_KEY),
                        placedFeatures.getOrThrow(ModPlacedFeatures.TUFF_COPPER_ORE_PLACED_KEY)
                ),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_TIN_ORE, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(
                        placedFeatures.getOrThrow(ModPlacedFeatures.STONE_TIN_ORE_PLACED_KEY),
                        placedFeatures.getOrThrow(ModPlacedFeatures.DEEPSLATE_TIN_ORE_PLACED_KEY),
                        placedFeatures.getOrThrow(ModPlacedFeatures.GRANITE_TIN_ORE_PLACED_KEY),
                        placedFeatures.getOrThrow(ModPlacedFeatures.ANDESITE_TIN_ORE_PLACED_KEY),
                        placedFeatures.getOrThrow(ModPlacedFeatures.DIORITE_TIN_ORE_PLACED_KEY),
                        placedFeatures.getOrThrow(ModPlacedFeatures.TUFF_TIN_ORE_PLACED_KEY)
                ),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_DATOLITE_NETHER_ORE, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.BASALT_DELTAS)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.DATOLITE_NETHER_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_AZOTIC_TOPAZ_END_ORE, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_END),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.AZOTIC_TOPAZ_END_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_TREE_SCULK, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.FOREST), biomes.getOrThrow(Biomes.PLAINS)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.SCULK_TREE_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_BLUE_BERRY, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.FOREST)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.BLUE_BERRY_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, name));
    }
}