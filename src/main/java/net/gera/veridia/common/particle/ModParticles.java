package net.gera.veridia.common.particle;

import net.gera.veridia.Veridia;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Veridia.MOD_ID);

    public static final Supplier<SimpleParticleType> SPARK =
            PARTICLE_TYPES.register("spark", () -> new SimpleParticleType(false));

    // Красный пузырь поднимается в воде пепельной реки и лопается (RED_BUBBLE_POP) у поверхности
    public static final Supplier<SimpleParticleType> RED_BUBBLE =
            PARTICLE_TYPES.register("red_bubble", () -> new SimpleParticleType(false));
    public static final Supplier<SimpleParticleType> RED_BUBBLE_POP =
            PARTICLE_TYPES.register("red_bubble_pop", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
