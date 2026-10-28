package net.gera.veridia.common.village.calc;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Заявки на вырубку деревьев, мешающих стене: nailer ставит заявку на дерево, лесоруб берёт ближайшую (см.
 * LumberjackChopGoal), валит его по-настоящему (Dynamic Trees: futureBreak, дропы - в склад) и закрывает заявку.
 * <p>
 * Nailer не стоит и не ждёт: пока дерево не упало, записи стены на его месте пропускаются, и он строит остальное
 * (NailerTreeInWay). Если лесоруба в деревне нет, заявка не взята за UNCLAIMED_TIMEOUT_TICKS, лесоруб потерял
 * дерево (бросил заявку, сгинул) или не смог дойти, - вердикт FELL_NOW: nailer убирает дерево сам, мгновенно, как
 * TreeCutter. Заявки живут в памяти и пересоздаются по необходимости (дерево никуда не делось - nailer снова наткнётся).
 * Ключ заявки - основание дерева (самый нижний блок скелета, см. TreeCutter.Skeleton).
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class TreeFellRequests {

    private TreeFellRequests() {
    }

    /** Сколько заявка может висеть, не будучи взятой лесорубом, прежде чем nailer срубит дерево сам (8 минут). */
    public static final long UNCLAIMED_TIMEOUT_TICKS = 20L * 60 * 8;
    /** Взятая заявка без отметок лесоруба дольше этого возвращается в очередь (лесоруб пропал). */
    public static final long CLAIM_STALE_TICKS = 20L * 60;
    /** Лесоруб в этом радиусе от дерева считается "есть в деревне". */
    public static final int LUMBERJACK_RADIUS = 128;
    /** Лесоруб берёт заявки на деревья не дальше этого от себя. */
    public static final int CLAIM_RADIUS = 160;

    public enum Verdict { WAIT, FELL_NOW }

    private enum State { PENDING, CLAIMED, FAILED }

    private static final class Request {
        final BlockPos base;
        final Set<BlockPos> wood;
        State state = State.PENDING;
        @Nullable
        UUID lumberjack;
        final long createdAt;
        long touchedAt;

        Request(BlockPos base, Set<BlockPos> wood, long now) {
            this.base = base;
            this.wood = wood;
            this.createdAt = now;
            this.touchedAt = now;
        }
    }

    private static final Map<ServerLevel, Map<BlockPos, Request>> REQUESTS = new HashMap<>();

    private static Map<BlockPos, Request> requests(ServerLevel level) {
        return REQUESTS.computeIfAbsent(level, key -> new HashMap<>());
    }

    // --- сторона nailer'а ---

    /** Заявка, чьё дерево включает блок pos (по составу скелета на момент заявки); null - такой нет. */
    @Nullable
    private static Request covering(ServerLevel level, BlockPos pos) {
        for (Request request : requests(level).values()) {
            if (request.wood.contains(pos)) {
                return request;
            }
        }
        return null;
    }

    /** true, если на блок pos уже есть живая заявка, которую ещё ждут (nailer пропускает такие записи, не рубя дерево). */
    public static boolean isWaitingAt(ServerLevel level, BlockPos pos) {
        if (!VillageBlocks.isTreePart(level.getBlockState(pos))) {
            return false;
        }
        Request request = covering(level, pos);
        return request != null && judge(level, request) == Verdict.WAIT;
    }

    /**
     * Нужна ли помощь лесоруба для дерева skeleton: WAIT - заявка стоит (создана сейчас или уже была) и её ждут;
     * FELL_NOW - ждать нечего (нет лесоруба, заявка просрочена или провалена), nailer рубит сам.
     */
    public static Verdict ask(ServerLevel level, TreeCutter.Skeleton skeleton) {
        Request existing = requests(level).get(skeleton.base());
        if (existing == null) {
            if (!lumberjackAvailable(level, skeleton.base())) {
                return Verdict.FELL_NOW;
            }
            Request created = new Request(skeleton.base(), skeleton.wood(), level.getGameTime());
            requests(level).put(skeleton.base(), created);
            NailerQueueAudit.log("заявка на дерево у {}: {} блоков скелета, ждём лесоруба", skeleton.base().toShortString(), skeleton.wood().size());
            return Verdict.WAIT;
        }
        return judge(level, existing);
    }

    private static Verdict judge(ServerLevel level, Request request) {
        long now = level.getGameTime();
        if (request.state == State.CLAIMED && now - request.touchedAt > CLAIM_STALE_TICKS) {
            NailerQueueAudit.log("лесоруб бросил дерево у {} - заявка снова в очереди", request.base.toShortString());
            request.state = State.PENDING;
            request.lumberjack = null;
        }
        if (request.state == State.FAILED) {
            return Verdict.FELL_NOW;
        }
        if (request.state == State.PENDING && now - request.createdAt > UNCLAIMED_TIMEOUT_TICKS) {
            NailerQueueAudit.log("заявка на дерево у {} не взята за {} тиков - срубит nailer", request.base.toShortString(), UNCLAIMED_TIMEOUT_TICKS);
            request.state = State.FAILED;
            return Verdict.FELL_NOW;
        }
        return Verdict.WAIT;
    }

    private static boolean lumberjackAvailable(ServerLevel level, BlockPos near) {
        AABB area = new AABB(near).inflate(LUMBERJACK_RADIUS);
        return !level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.isAlive() && villager.getVillagerData().getProfession() == ModVillagers.LUMBERJACK.value()).isEmpty();
    }

    // --- сторона лесоруба ---

    /** Ближайшая свободная (или уже его) заявка: помечает её взятой и возвращает основание дерева; null - заявок нет. */
    @Nullable
    public static BlockPos claimNearest(ServerLevel level, Villager lumberjack) {
        Request best = null;
        double bestDistance = (double) CLAIM_RADIUS * CLAIM_RADIUS;
        Iterator<Request> iterator = requests(level).values().iterator();
        while (iterator.hasNext()) {
            Request request = iterator.next();
            if (!isTreeStanding(level, request)) {
                iterator.remove();
                continue;
            }
            boolean free = request.state == State.PENDING
                    || (request.state == State.CLAIMED && lumberjack.getUUID().equals(request.lumberjack));
            double distance = request.base.distSqr(lumberjack.blockPosition());
            if (free && distance < bestDistance) {
                best = request;
                bestDistance = distance;
            }
        }
        if (best == null) {
            return null;
        }
        best.state = State.CLAIMED;
        best.lumberjack = lumberjack.getUUID();
        best.touchedAt = level.getGameTime();
        NailerQueueAudit.log("лесоруб взял заявку на дерево у {}", best.base.toShortString());
        return best.base;
    }

    private static boolean isTreeStanding(ServerLevel level, Request request) {
        if (!level.isLoaded(request.base)) {
            return true;
        }
        return request.wood.stream().anyMatch(pos -> level.isLoaded(pos) && VillageBlocks.isTreePart(level.getBlockState(pos))
                && !VillageBlocks.isCanopy(level.getBlockState(pos)));
    }

    /** Лесоруб ещё занят этим деревом - продлевает заявку. */
    public static void touch(ServerLevel level, BlockPos base, UUID lumberjack) {
        Request request = requests(level).get(base);
        if (request != null && lumberjack.equals(request.lumberjack)) {
            request.touchedAt = level.getGameTime();
        }
    }

    /** Дерево свалено - заявка закрыта. */
    public static void finish(ServerLevel level, BlockPos base) {
        if (requests(level).remove(base) != null) {
            NailerQueueAudit.log("заявка на дерево у {} выполнена", base.toShortString());
        }
    }

    /** Лесоруб не смог (не дошёл, дерево не падает) - nailer срубит сам. */
    public static void fail(ServerLevel level, BlockPos base) {
        Request request = requests(level).get(base);
        if (request != null) {
            request.state = State.FAILED;
            NailerQueueAudit.log("лесоруб не справился с деревом у {} - срубит nailer", base.toShortString());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        REQUESTS.clear();
    }
}
