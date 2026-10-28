package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Что nailer делает с деревом, которое стоит на месте записи стены. Раньше он ставил блок поверх ветки, и Dynamic
 * Trees обрушивал дерево целиком (см. DynamicTreesCompat.withoutBranchCascade), оставляя вдоль стены обрубки.
 * Теперь дерево с СТВОЛОМ/ВЕТКОЙ в объёме стены валится целиком: либо лесорубом по заявке (TreeFellRequests), либо,
 * если лесоруб не поможет, самим nailer'ом мгновенно. Одна листва в объёме стены деревом "на пути" не считается:
 * блок стены просто встаёт на её место.
 */
public final class NailerTreeInWay {

    private NailerTreeInWay() {
    }

    public enum Outcome {
        /** На клетке нет древесины - обычная запись. */
        CLEAR,
        /** Дерево ждёт лесоруба - запись пока пропускается. */
        WAITING,
        /** Дерево только что срублено самим nailer'ом - клетка свободна. */
        FELLED
    }

    private static final Recorder NO_RECORD = (pos, state) -> { };

    /** Разбирается с деревом на клетке pos: просит лесоруба или (если тот не поможет) рубит сразу. */
    public static Outcome resolve(ServerLevel level, BlockPos pos) {
        if (!isWood(level, pos)) {
            return Outcome.CLEAR;
        }
        if (TreeFellRequests.isWaitingAt(level, pos)) {
            return Outcome.WAITING;
        }
        TreeCutter.Skeleton skeleton = TreeCutter.skeletonAt(level, pos);
        if (skeleton == null) {
            return Outcome.CLEAR;
        }
        if (TreeFellRequests.ask(level, skeleton) == TreeFellRequests.Verdict.WAIT) {
            return Outcome.WAITING;
        }
        TreeCutter.cut(level, pos, NO_RECORD);
        return Outcome.FELLED;
    }

    /** Заявки сразу на все деревья, мешающие очереди, чтобы лесоруб начал, пока nailer строит другие куски. */
    public static void requestAll(ServerLevel level, List<NailerWallQueue.Piece> pieces) {
        Set<BlockPos> handled = new HashSet<>();
        for (NailerWallQueue.Piece piece : pieces) {
            for (NailerWallQueue.Entry entry : piece.entries()) {
                BlockPos pos = entry.pos();
                if (handled.contains(pos) || !isWood(level, pos)) {
                    continue;
                }
                TreeCutter.Skeleton skeleton = TreeCutter.skeletonAt(level, pos);
                if (skeleton != null) {
                    handled.addAll(skeleton.wood());
                    TreeFellRequests.ask(level, skeleton);
                }
            }
        }
    }

    /** Есть ли в очереди запись, перед которой ещё стоит ждущее лесоруба дерево. */
    public static boolean anyWaiting(ServerLevel level, List<NailerWallQueue.Piece> pieces) {
        for (NailerWallQueue.Piece piece : pieces) {
            for (NailerWallQueue.Entry entry : piece.entries()) {
                if (TreeFellRequests.isWaitingAt(level, entry.pos())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isWood(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return VillageBlocks.isTreePartAt(level, pos, state) && !VillageBlocks.isCanopy(state);
    }
}
