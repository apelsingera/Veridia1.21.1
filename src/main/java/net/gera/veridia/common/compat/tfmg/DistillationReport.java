package net.gera.veridia.common.compat.tfmg;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Что блок стального бака TFMG знает о ректификационной колонне - для /veridiadebug distillation.
 *
 * @param main    главный блок бака (TFMG считает нагрев по блокам под ним)
 * @param tower   признан ли бак колонной (иначе TFMG нагрев не считает)
 * @param heat    нагрев колонны у этого блока
 * @param samples что TFMG увидел под баком при последнем подсчёте нагрева (пусто - не считал)
 */
public record DistillationReport(BlockPos main, int width, int height, boolean tower, int heat,
                                 List<HeatSample> samples) {
    /** Блок под баком: что за нагрев дал Create и что получилось с поправками {@link DistillationHeat}. */
    public record HeatSample(BlockPos pos, BlockState state, float createHeat, float heat) {
    }
}
