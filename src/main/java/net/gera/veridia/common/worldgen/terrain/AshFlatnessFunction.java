package net.gera.veridia.common.worldgen.terrain;

import com.mojang.serialization.MapCodec;
import net.gera.veridia.common.worldgen.biome.AshLands;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Функция плотности "насколько здесь равнина": 0 - обычный рельеф мира, 1 - ровные равнины Пепельных земель
 * ({@link AshLands#flatness}). На неё опираются подмены overworld/offset, factor и jaggedness: между обычным и
 * равнинным рельефом они переходят по этому числу. Зависит только от x и z.
 */
public final class AshFlatnessFunction implements DensityFunction.SimpleFunction {
    public static final AshFlatnessFunction INSTANCE = new AshFlatnessFunction();
    public static final MapCodec<AshFlatnessFunction> CODEC = MapCodec.unit(INSTANCE);
    private static final KeyDispatchDataCodec<AshFlatnessFunction> DATA_CODEC = KeyDispatchDataCodec.of(CODEC);

    private AshFlatnessFunction() {
    }

    @Override
    public double compute(FunctionContext context) {
        return AshLands.flatness(context.blockX(), context.blockZ());
    }

    @Override
    public double minValue() {
        return 0.0D;
    }

    @Override
    public double maxValue() {
        return 1.0D;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return DATA_CODEC;
    }
}
