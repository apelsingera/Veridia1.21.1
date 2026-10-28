package net.gera.veridia.common.worldgen.terrain;

import com.mojang.serialization.MapCodec;
import net.gera.veridia.Veridia;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Свои типы функций плотности. Подмены рельефа лежат в data/minecraft/worldgen/density_function/overworld. */
public final class ModDensityFunctions {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Veridia.MOD_ID);

    public static final Supplier<MapCodec<AshFlatnessFunction>> ASH_FLATNESS =
            FUNCTION_TYPES.register("ash_flatness", () -> AshFlatnessFunction.CODEC);

    private ModDensityFunctions() {
    }

    public static void register(IEventBus eventBus) {
        FUNCTION_TYPES.register(eventBus);
    }
}
