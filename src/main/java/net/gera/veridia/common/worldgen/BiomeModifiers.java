package net.gera.veridia.common.worldgen;

import net.gera.veridia.Veridia;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class BiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_TREE_SCULK = registerKey("add_tree_sculk");
    public static final ResourceKey<BiomeModifier> ADD_SCULK_SHROOM = registerKey("add_sculk_shroom");

    public static final ResourceKey<BiomeModifier> ADD_BLUE_BERRY = registerKey("add_blue_berry");
    public static final ResourceKey<BiomeModifier> ADD_GOOSE_BERRY = registerKey("add_goose_berry");
    public static final ResourceKey<BiomeModifier> ADD_CRANBERRY = registerKey("add_cranberry");
    public static final ResourceKey<BiomeModifier> ADD_CUMA_CUMA_BUSH = registerKey("add_cuma_cuma_bush");
    public static final ResourceKey<BiomeModifier> ADD_SPARSE_JUNGLE_BUSH = registerKey("add_sparse_jungle_bush");
    public static final ResourceKey<BiomeModifier> ADD_SPARSE_JUNGLE_GRASS = registerKey("add_sparse_jungle_grass");

    public static final ResourceKey<BiomeModifier> ADD_BARIUM_NETHER_ORE = registerKey("add_barium_nether_ore");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);


        context.register(ADD_TREE_SCULK, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.FOREST), biomes.getOrThrow(Biomes.PLAINS)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.SCULK_TREE_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_SCULK_SHROOM, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.DEEP_DARK)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.SCULK_SHROOM_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_BLUE_BERRY, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.FOREST)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.BLUE_BERRY_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_GOOSE_BERRY, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.PLAINS)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.GOOSE_BERRY_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_CRANBERRY, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.PLAINS)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.CRANBERRY_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));
        context.register(ADD_CUMA_CUMA_BUSH, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.JUNGLE), biomes.getOrThrow(Biomes.SPARSE_JUNGLE)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.CUMA_CUMA_BUSH_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_SPARSE_JUNGLE_BUSH, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.SPARSE_JUNGLE)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.JUNGLE_BUSH_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_SPARSE_JUNGLE_GRASS, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.SPARSE_JUNGLE)),
                HolderSet.direct(
                        placedFeatures.getOrThrow(ModPlacedFeatures.SPARSE_JUNGLE_TALL_GRASS_PLACED_KEY),
                        placedFeatures.getOrThrow(ModPlacedFeatures.SPARSE_JUNGLE_LARGE_FERN_PLACED_KEY)),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ADD_BARIUM_NETHER_ORE, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.getOrThrow(Biomes.BASALT_DELTAS)),
                HolderSet.direct(placedFeatures.getOrThrow(ModPlacedFeatures.BARIUM_NETHER_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, name));
    }
}