package net.gera.veridia.common.worldgen.feature;

import net.gera.veridia.Veridia;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Veridia.MOD_ID);

    public static final Supplier<Feature<NoneFeatureConfiguration>> CUMA_CUMA_BUSH =
            FEATURES.register("cuma_cuma_bush", () -> new CumaCumaBushFeature(NoneFeatureConfiguration.CODEC));

    public static final Supplier<Feature<MagmaPatchConfiguration>> MAGMA_PATCH =
            FEATURES.register("magma_patch", () -> new MagmaPatchFeature(MagmaPatchConfiguration.CODEC));

    public static final Supplier<Feature<NoneFeatureConfiguration>> LAVA_LAKE =
            FEATURES.register("lava_lake", () -> new LavaLakeFeature(NoneFeatureConfiguration.CODEC));

    public static final Supplier<Feature<SpikeConfiguration>> SPIKE =
            FEATURES.register("spike", () -> new SpikeFeature(SpikeConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
