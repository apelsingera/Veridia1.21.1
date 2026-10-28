package net.gera.veridia.common.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Куда фича может писать блоки. Фичу ставят в одном чанке, а менять она может только его самого и по одному чанку
 * вокруг (остальные ещё не готовы); запись дальше отклоняется. Крупные фичи проверяют эту область заранее, чтобы
 * не получилось озеро с обрезанным краем.
 */
final class WriteRange {
    private static final int CHUNKS_AROUND = 1;

    private WriteRange() {
    }

    /** Область записи для фичи с точкой размещения origin. */
    static BoundingBox around(BlockPos origin) {
        ChunkPos chunk = new ChunkPos(origin);
        int reach = CHUNKS_AROUND * 16;
        return new BoundingBox(
                chunk.getMinBlockX() - reach, origin.getY() - 512, chunk.getMinBlockZ() - reach,
                chunk.getMaxBlockX() + reach, origin.getY() + 512, chunk.getMaxBlockZ() + reach);
    }
}
