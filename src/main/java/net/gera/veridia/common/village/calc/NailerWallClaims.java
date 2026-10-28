package net.gera.veridia.common.village.calc;

import net.gera.veridia.Veridia;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Кто из nailer'ов сейчас достраивает какой NailerWallQueue.Piece - если в деревне их несколько,
 * не даём двум взяться за один и тот же недостроенный кусок стены и растащить стройку кусками
 * по всему периметру. Ключ - мир + якорная позиция куска (Piece.anchor, стабильна между
 * пересчётами плана для одного и того же состояния территории).
 *
 * Захват "протухает" сам через STALE_TICKS без обновления - на случай, если nailer пропал без
 * вызова stop() (смерть, принудительная выгрузка чанка и т.п.), чтобы кусок не завис навечно.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class NailerWallClaims {
    private NailerWallClaims() {
    }

    private static final long STALE_TICKS = 20L * 20L;

    private record ClaimKey(ServerLevel level, BlockPos anchor) {}

    private record Claim(UUID nailerId, long renewedAt) {}

    private static final Map<ClaimKey, Claim> CLAIMS = new HashMap<>();

    /** true, если кусок свободен, уже наш, или просрочен - и в этих случаях сразу продлевает захват. */
    public static boolean claimOrRenew(ServerLevel level, BlockPos anchor, UUID nailerId) {
        ClaimKey key = new ClaimKey(level, anchor);
        Claim current = CLAIMS.get(key);
        long now = level.getGameTime();
        if (current == null || current.nailerId().equals(nailerId) || now - current.renewedAt() > STALE_TICKS) {
            CLAIMS.put(key, new Claim(nailerId, now));
            return true;
        }
        return false;
    }

    public static void release(ServerLevel level, BlockPos anchor, UUID nailerId) {
        ClaimKey key = new ClaimKey(level, anchor);
        Claim current = CLAIMS.get(key);
        if (current != null && current.nailerId().equals(nailerId)) {
            CLAIMS.remove(key);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        CLAIMS.clear();
    }
}
