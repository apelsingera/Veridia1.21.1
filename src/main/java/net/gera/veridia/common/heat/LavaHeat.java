package net.gera.veridia.common.heat;

import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.OptionalDouble;

/**
 * Жар от больших лавовых озёр в пепельных биомах. Озеро определяется по блокам: вокруг точки выбираются столбики
 * через STEP блоков, и если среди них достаточно лавы (MIN_LAVA_SAMPLES), рядом большое озеро, а не одинокий
 * ручеёк. Так же работает для любой лавы в пепельных биомах (кучи магмы с озерцом поменьше не дотягивают до порога).
 */
final class LavaHeat {
    // Как далеко от игрока (блоков по горизонтали) озеро ещё жарит
    private static final int RADIUS = 18;
    // Шаг сетки столбиков: больше - дешевле проверка, но грубее
    private static final int STEP = 2;
    // Сколько блоков ниже и выше игрока просматривается
    private static final int DEPTH_BELOW = 8;
    private static final int HEIGHT_ABOVE = 4;
    // Сколько найденных лавовых блоков (при шаге STEP) считается большим озером
    private static final int MIN_LAVA_SAMPLES = 25;

    private LavaHeat() {
    }

    /** Расстояние от точки до ближайшей лавы большого озера или пусто, если озера рядом нет. */
    static OptionalDouble nearestLakeDistance(Level level, BlockPos center) {
        if (!ModBiomes.isAshBiome(level.getBiome(center))) {
            return OptionalDouble.empty();
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int lavaSamples = 0;
        double nearestSquared = Double.MAX_VALUE;
        for (int dx = -RADIUS; dx <= RADIUS; dx += STEP) {
            for (int dz = -RADIUS; dz <= RADIUS; dz += STEP) {
                pos.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                if (!level.hasChunkAt(pos)) {
                    continue;
                }
                for (int dy = -DEPTH_BELOW; dy <= HEIGHT_ABOVE; dy++) {
                    pos.setY(center.getY() + dy);
                    if (level.getBlockState(pos).is(Blocks.LAVA)) {
                        lavaSamples++;
                        nearestSquared = Math.min(nearestSquared, dx * dx + dy * dy + dz * dz);
                    }
                }
            }
        }
        return lavaSamples >= MIN_LAVA_SAMPLES ? OptionalDouble.of(Math.sqrt(nearestSquared)) : OptionalDouble.empty();
    }
}
