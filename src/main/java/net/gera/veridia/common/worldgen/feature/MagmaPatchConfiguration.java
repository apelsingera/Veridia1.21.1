package net.gera.veridia.common.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Куча blackstone_magma с проплешинами из чёрного камня и лавовым озерцом внутри.
 *
 * @param lavaRadiusFraction максимальная доля радиуса кучи, занятая лавой (0 - без лавы)
 */
public record MagmaPatchConfiguration(int minRadius, int maxRadius, float lavaRadiusFraction)
        implements FeatureConfiguration {

    public static final Codec<MagmaPatchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 16).fieldOf("min_radius").forGetter(MagmaPatchConfiguration::minRadius),
            Codec.intRange(1, 16).fieldOf("max_radius").forGetter(MagmaPatchConfiguration::maxRadius),
            Codec.floatRange(0.0F, 0.8F).fieldOf("lava_radius_fraction").forGetter(MagmaPatchConfiguration::lavaRadiusFraction)
    ).apply(instance, MagmaPatchConfiguration::new));
}
