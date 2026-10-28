package net.gera.veridia.common.worldgen.feature;

import net.gera.veridia.common.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Выжженная земля вокруг лавового озера: у озера нужна не земля, а чёрный камень. В круге FREE_ZONE от края озера
 * верхний блок земли заменяется чёрным камнем, дальше (FADE_ZONE) замена становится всё реже до нуля.
 */
final class ScorchedGround {
    // На сколько блоков от края лавы земля вся заменяется чёрным камнем и на сколько дальше замена затухает до нуля
    private static final double FREE_ZONE = 6.0D;
    private static final double FADE_ZONE = 5.0D;

    /** На сколько блоков за край озера доходит замена. */
    static final int REACH = (int) Math.ceil(FREE_ZONE + FADE_ZONE);

    /** Расстояние от колонки (dx, dz) до края озера: меньше 0 внутри озера. */
    @FunctionalInterface
    interface DistanceToLake {
        double outsideEdge(int dx, int dz);
    }

    private ScorchedGround() {
    }

    /**
     * @param span     на сколько блоков от origin по каждой оси проверять колонки
     * @param writable куда можно писать блоки; остальное пропускается
     */
    static void clearAround(WorldGenLevel level, BlockPos origin, int span, DistanceToLake distanceToLake,
                            BoundingBox writable, RandomSource random) {
        for (int dx = -span; dx <= span; dx++) {
            for (int dz = -span; dz <= span; dz++) {
                if (random.nextDouble() >= clearChance(distanceToLake.outsideEdge(dx, dz))) {
                    continue;
                }
                BlockPos ground = groundAt(level, origin.getX() + dx, origin.getZ() + dz);
                if (writable.isInside(ground) && level.getBlockState(ground).is(ModBlocks.INCINERATED_DIRT.get())) {
                    level.setBlock(ground, Blocks.BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    // 1 у озера и до FREE_ZONE, затем линейно до 0 на расстоянии FREE_ZONE + FADE_ZONE
    private static double clearChance(double distanceFromLake) {
        if (distanceFromLake <= FREE_ZONE) {
            return 1.0D;
        }
        return Math.max(0.0D, 1.0D - (distanceFromLake - FREE_ZONE) / FADE_ZONE);
    }

    // Земля (твёрдый блок под первым воздухом по карте высот) в столбце
    static BlockPos groundAt(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1, z);
    }
}
