package net.gera.veridia.common.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Группа каменных шипов: главный шип и несколько шипов поменьше рядом.
 *
 * @param stateProvider из каких блоков сложены шипы (каждый блок выбирается случайно)
 * @param minHeight     высота главного шипа, от
 * @param maxHeight     высота главного шипа, до
 * @param minSatellites сколько шипов поменьше вокруг главного, от
 * @param maxSatellites сколько шипов поменьше вокруг главного, до
 */
public record SpikeConfiguration(BlockStateProvider stateProvider, int minHeight, int maxHeight,
                                 int minSatellites, int maxSatellites) implements FeatureConfiguration {

    public static final Codec<SpikeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(SpikeConfiguration::stateProvider),
            Codec.intRange(2, 48).fieldOf("min_height").forGetter(SpikeConfiguration::minHeight),
            Codec.intRange(2, 48).fieldOf("max_height").forGetter(SpikeConfiguration::maxHeight),
            Codec.intRange(0, 8).fieldOf("min_satellites").forGetter(SpikeConfiguration::minSatellites),
            Codec.intRange(0, 8).fieldOf("max_satellites").forGetter(SpikeConfiguration::maxSatellites)
    ).apply(instance, SpikeConfiguration::new));
}
