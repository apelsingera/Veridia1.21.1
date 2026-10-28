package net.gera.veridia.common.worldgen.biome;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.entity.ModEntities;
import net.gera.veridia.common.worldgen.ModPlacedFeatures;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

/** Биомы Veridia. JSON создаёт датаген (см. ModDatapackProvider): data/veridia/worldgen/biome. */
public class ModBiomes {
    // Две суши Пепельных земель: равнины без деревьев и лес. Различаются только деревьями (их сажает Dynamic Trees
    // по trees/veridia/world_gen), остальное у них одинаковое. Где что - решает AshLands.
    public static final ResourceKey<Biome> ASHEN_PLAINS = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "ashen_plains"));

    public static final ResourceKey<Biome> ASHEN_FOREST = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "ashen_forest"));

    public static final ResourceKey<Biome> ASHEN_RIVER = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "ashen_river"));

    /** Все пепельные биомы: на них действуют поверхность, затемнение, частицы пепла и своя рыбалка. */
    public static final List<ResourceKey<Biome>> ASH_BIOMES = List.of(ASHEN_PLAINS, ASHEN_FOREST, ASHEN_RIVER);

    // Цвета (RGB)
    private static final int SKY_COLOR = 0x969DC9;
    // public: от этого цвета отталкиваются и облака при затемнении (см. AshGloom)
    public static final int FOG_COLOR = 0x6B6B6B;
    private static final int WATER_COLOR = 0x6B6B6B;
    private static final int WATER_FOG_COLOR = 0x4A4A4A;

    // Шанс появления частицы пепла (у базальтовых дельт 0.118)
    private static final float ASH_PARTICLE_PROBABILITY = 0.008F;

    // Зловещие воины: вес среди монстров (у зомби и скелета 95-100) и размер группы
    private static final int OMINOUS_WARRIOR_SPAWN_WEIGHT = 30;
    private static final int OMINOUS_WARRIOR_GROUP_MIN = 3;
    private static final int OMINOUS_WARRIOR_GROUP_MAX = 5;
    // Высокие (Brutal) - реже и поодиночке или парой
    private static final int BRUTAL_OMINOUS_WARRIOR_SPAWN_WEIGHT = 10;
    private static final int BRUTAL_OMINOUS_WARRIOR_GROUP_MIN = 1;
    private static final int BRUTAL_OMINOUS_WARRIOR_GROUP_MAX = 2;

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        context.register(ASHEN_PLAINS, ashBiome(placedFeatures, carvers, AshKind.PLAINS));
        context.register(ASHEN_FOREST, ashBiome(placedFeatures, carvers, AshKind.FOREST));
        context.register(ASHEN_RIVER, ashBiome(placedFeatures, carvers, AshKind.RIVER));
    }

    /** Пепельный ли биом в этой точке (см. {@link #ASH_BIOMES}). */
    public static boolean isAshBiome(Holder<Biome> biome) {
        return ASH_BIOMES.stream().anyMatch(biome::is);
    }

    // Чем отличаются пепельные биомы: у реки на дне вместо водорослей и песка диски. Остальное (цвета, мобы, фичи)
    // у всех одинаковое; деревья в лесу сажает Dynamic Trees, озёра - LavaLakeField.
    private enum AshKind {
        PLAINS, FOREST, RIVER
    }

    private static Biome ashBiome(HolderGetter<PlacedFeature> placedFeatures,
                                  HolderGetter<ConfiguredWorldCarver<?>> carvers, AshKind kind) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(2.0F)
                .downfall(0.0F)
                .specialEffects(effects())
                .mobSpawnSettings(spawns())
                .generationSettings(generation(placedFeatures, carvers, kind))
                .build();
    }

    private static BiomeSpecialEffects effects() {
        return new BiomeSpecialEffects.Builder()
                .skyColor(SKY_COLOR)
                .fogColor(FOG_COLOR)
                .waterColor(WATER_COLOR)
                .waterFogColor(WATER_FOG_COLOR)
                .ambientParticle(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, ASH_PARTICLE_PROBABILITY))
                .ambientLoopSound(SoundEvents.AMBIENT_BASALT_DELTAS_LOOP)
                .ambientMoodSound(new AmbientMoodSettings(SoundEvents.AMBIENT_BASALT_DELTAS_MOOD, 6000, 8, 2.0D))
                .ambientAdditionsSound(new AmbientAdditionsSettings(SoundEvents.AMBIENT_BASALT_DELTAS_ADDITIONS, 0.0111D))
                .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_BASALT_DELTAS))
                .build();
    }

    private static MobSpawnSettings spawns() {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.commonSpawns(builder);
        builder.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.OMINOUS_WARRIOR.get(),
                OMINOUS_WARRIOR_SPAWN_WEIGHT, OMINOUS_WARRIOR_GROUP_MIN, OMINOUS_WARRIOR_GROUP_MAX));
        builder.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.BRUTAL_OMINOUS_WARRIOR.get(),
                BRUTAL_OMINOUS_WARRIOR_SPAWN_WEIGHT, BRUTAL_OMINOUS_WARRIOR_GROUP_MIN, BRUTAL_OMINOUS_WARRIOR_GROUP_MAX));
        return builder.build();
    }

    // Структуры берутся не из биома, а из тегов has_structure/*: нашего биома в них нет,
    // поэтому деревни, храмы и прочее здесь не генерируются.
    private static BiomeGenerationSettings generation(HolderGetter<PlacedFeature> placedFeatures,
                                                      HolderGetter<ConfiguredWorldCarver<?>> carvers, AshKind kind) {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(builder);
        BiomeDefaultFeatures.addDefaultCrystalFormations(builder);
        BiomeDefaultFeatures.addDefaultMonsterRoom(builder);
        addUndergroundVarietyWithoutDirt(builder);
        BiomeDefaultFeatures.addDefaultSprings(builder);
        addOresWithSingedOres(builder);
        if (kind == AshKind.RIVER) {
            addRiverBedDisks(builder);
        }
        builder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, ModPlacedFeatures.MAGMA_PATCH_PLACED_KEY);
        // Озёра стоят только на равнинах, но решает это поле озёр (LavaLakeField), а не биом: озеро больше чанка
        builder.addFeature(GenerationStep.Decoration.SURFACE_STRUCTURES, ModPlacedFeatures.LAVA_LAKE_PLACED_KEY);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.TUFF_CLUSTER_PLACED_KEY);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.SMOOTH_BASALT_CLUSTER_PLACED_KEY);
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.ASHEN_GRASS_PLACED_KEY);
        builder.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, ModPlacedFeatures.SPIKE_PLACED_KEY);
        return builder.build();
    }

    // BiomeDefaultFeatures.addDefaultOres: уголь, железо, медь и лазурит заменены опалёнными (они же ставят ванильную
    // руду в обычный камень ниже y=50), остальное и порядок как в ваниле. Золото, редстоун и алмазы выше y=50 не бывают.
    private static void addOresWithSingedOres(BiomeGenerationSettings.Builder builder) {
        addUndergroundOres(builder,
                ModPlacedFeatures.SINGED_COAL_UPPER_PLACED_KEY,
                ModPlacedFeatures.SINGED_COAL_LOWER_PLACED_KEY,
                ModPlacedFeatures.SINGED_IRON_UPPER_PLACED_KEY,
                ModPlacedFeatures.SINGED_IRON_MIDDLE_PLACED_KEY,
                ModPlacedFeatures.SINGED_IRON_SMALL_PLACED_KEY,
                OrePlacements.ORE_GOLD,
                OrePlacements.ORE_GOLD_LOWER,
                OrePlacements.ORE_REDSTONE,
                OrePlacements.ORE_REDSTONE_LOWER,
                OrePlacements.ORE_DIAMOND,
                OrePlacements.ORE_DIAMOND_MEDIUM,
                OrePlacements.ORE_DIAMOND_LARGE,
                OrePlacements.ORE_DIAMOND_BURIED,
                OrePlacements.ORE_LAPIS,
                ModPlacedFeatures.SINGED_LAPIS_BURIED_PLACED_KEY,
                ModPlacedFeatures.SINGED_COPPER_PLACED_KEY,
                CavePlacements.UNDERWATER_MAGMA);
    }

    @SafeVarargs
    private static void addUndergroundOres(BiomeGenerationSettings.Builder builder, ResourceKey<PlacedFeature>... features) {
        for (ResourceKey<PlacedFeature> feature : features) {
            builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature);
        }
    }

    // BiomeDefaultFeatures.addDefaultUndergroundVariety без ore_dirt: ванильная земля (пятна под землёй, видные на склонах)
    // в пепельных биомах не нужна - вся земля тут выжженная. Остальное и порядок как в ваниле.
    private static void addUndergroundVarietyWithoutDirt(BiomeGenerationSettings.Builder builder) {
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_GRAVEL);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_GRANITE_UPPER);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_GRANITE_LOWER);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIORITE_UPPER);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIORITE_LOWER);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_ANDESITE_UPPER);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_ANDESITE_LOWER);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_TUFF);
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CavePlacements.GLOW_LICHEN);
    }

    // Ванильная река красит дно дисками песка, глины и гравия (в том же порядке) и сажает водоросли.
    // Здесь песок заменён гравием, гравий - туфом, глина осталась ванильной, водорослей нет.
    private static void addRiverBedDisks(BiomeGenerationSettings.Builder builder) {
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.RIVER_GRAVEL_DISK_PLACED_KEY);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_CLAY);
        builder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.RIVER_TUFF_DISK_PLACED_KEY);
    }
}
