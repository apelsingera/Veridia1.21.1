package net.gera.veridia.common.block.ore.client;

import org.joml.Matrix4f;

import java.util.List;

/**
 * Что торчит из одного выросшего блока руды: куски скрапа и (иногда) повёрнутый кубик. План зависит только от положения блока
 * и соседей, поэтому блок всегда выглядит одинаково, сколько бы раз ни перестраивался кусок мира.
 *
 * @param pieces торчащие предметы
 */
record ScrapPlan(List<Piece> pieces) {
    static final ScrapPlan EMPTY = new ScrapPlan(List.of());

    enum Kind {
        /** Кусок скрапа: заготовка {@link ScrapTemplate}. */
        SHARD,
        /** Маленький кубик - копия самого блока. */
        CUBE
    }

    /**
     * @param kind      что это
     * @param transform куда переносит заготовку (кусок скрапа или единичный куб от 0 до 1) в координаты блока
     * @param light     освещение там, где предмет торчит наружу (освещение клетки за гранью, из которой он торчит)
     */
    record Piece(Kind kind, Matrix4f transform, int light) {
    }
}
