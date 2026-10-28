package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Журнал решений {@link NailerWallQueue#build}: какие куски стены встали целиком, какие развалились на
 * столбики, какие столбики/башни/записи отброшены и ЧТО именно им помешало. Нужен диагностике
 * ({@link NailerQueueAudit}, команда /calculate nailer), в самой очереди ничего не меняет. Все методы по
 * умолчанию пустые: {@link #NONE} стоит у настоящего nailer'а и ничего не стоит.
 */
public interface QueueTrace {

    QueueTrace NONE = new QueueTrace() { };

    /** Блок (или причина без блока), из-за которого кусок не встал: pos - где, state - что там стоит, why - почему это помеха. */
    record Obstacle(BlockPos pos, BlockState state, String why) {}

    enum SegmentVerdict { WHOLE, TOO_UNEVEN, BLOCKED }

    enum ColumnVerdict { ADDED, DUPLICATE, REJECTED }

    default void segment(WallPlan.Segment segment, SegmentVerdict verdict, @Nullable Obstacle obstacle) { }

    default void column(int cellX, int cellZ, ColumnVerdict verdict, @Nullable Obstacle obstacle) { }

    /** Столбик отброшен целиком: он задевает уже спланированный кусок стены или башню (overlap - общая клетка). */
    default void columnOverlapsPiece(WallPlan.Filler filler, BlockPos overlap) { }

    /** Две записи претендовали на одну клетку: loser выброшена, winner осталась (onePerPosition). */
    default void shadowed(NailerWallQueue.Entry loser, NailerWallQueue.Entry winner) { }

    /** Связная масса записей: kept=false - мельче MIN_COMPONENT_SIZE, в очередь не попала. */
    default void component(List<NailerWallQueue.Entry> entries, boolean kept) { }

    /** obstacle == null - башня встанет. */
    default void tower(WallPlan.Tower spot, @Nullable Obstacle obstacle) { }

    /** Второй проход WallPlan.dropStrays (как в /calculate): убрано обрывков, добавлено мостов, из них отброшено по живому миру. */
    default void strays(int removed, int bridged, int bridgesRejected) { }
}
