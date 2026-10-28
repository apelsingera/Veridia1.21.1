package net.gera.veridia.common.villager.storage;

import com.mojang.logging.LogUtils;
import net.gera.veridia.debug.logging.WorkerPanelDebugConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Predicate;

public final class VillagerStorageBinding {

    private static final Logger LOGGER = LogUtils.getLogger();

    private VillagerStorageBinding() {
    }

    /** Сколько хранилище ждёт жителя, которого не видно, прежде чем освободить его место (3 игровых дня). */
    public static final long GHOST_GRACE_TICKS = 24000L * 3;

    @Nullable
    public static ResourceLocation professionKey(Villager villager) {
        return BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession());
    }

    /** Можно ли привязать жителя к этому хранилищу: профессия подходит, есть место, жителей его профессии меньше предела. */
    public static BindVerdict verdict(Villager villager, StorageKind kind, WorkerStorage storage) {
        if (!kind.accepts(villager.getVillagerData().getProfession())) {
            return BindVerdict.WRONG_PROFESSION;
        }
        return storage.getWorkers().admission(villager.getUUID(), professionKey(villager));
    }

    /**
     * Приводит места хранилища в порядок: освобождает места умерших (VillagerGraveyard) и пропавших дольше
     * GHOST_GRACE_TICKS (WorkerSlots.pruneStale), выселяет загруженных жителей, которые уже не работают с этим
     * хранилищем (потеряли профессию), и обновляет профессии загруженных. Следующие места при этом сдвигаются вверх.
     * true, если что-то поменялось и хранилище надо сохранить.
     */
    public static boolean pruneGhosts(ServerLevel level, WorkerStorage storage) {
        WorkerSlots workers = storage.getWorkers();
        long now = level.getGameTime();
        boolean changed = false;
        for (UUID id : workers.ids()) {
            changed |= refreshOrEvict(level, storage, loadedVillager(level, id), id, now);
        }
        int removed = workers.releaseWhere(VillagerGraveyard.get(level)::isDead)
                + workers.pruneStale(now, GHOST_GRACE_TICKS, id -> loadedVillager(level, id) != null);
        changed |= removed > 0;
        if (changed && storage instanceof BlockEntity blockEntity) {
            blockEntity.setChanged();
        }
        return changed;
    }

    /** Загруженному жителю обновляется профессия, а если она больше не подходит - место освобождается. */
    private static boolean refreshOrEvict(ServerLevel level, WorkerStorage storage, @Nullable Villager villager, UUID id, long now) {
        if (villager == null) {
            return false;
        }
        if (storage.kind().accepts(villager.getVillagerData().getProfession())) {
            return storage.getWorkers().touch(id, professionKey(villager), now);
        }
        storage.getWorkers().release(id);
        clear(villager.getPersistentData(), storage.kind());
        return true;
    }

    @Nullable
    private static Villager loadedVillager(ServerLevel level, UUID id) {
        return level.getEntity(id) instanceof Villager villager && villager.isAlive() ? villager : null;
    }

    @Nullable
    public static <T extends BlockEntity & WorkerStorage> BlockPos resolveCached(
            Villager villager, ServerLevel level, StorageKind kind, Block blockType, Class<T> beClass) {
        return readCachedValid(villager, level, kind, blockType, beClass);
    }

    @Nullable
    public static <T extends BlockEntity & WorkerStorage> BlockPos claimNew(
            Villager villager, ServerLevel level, StorageKind kind,
            Block blockType, Class<T> beClass, BlockPos origin, int radiusH, int radiusV,
            @Nullable Predicate<T> extraFilter) {

        UUID villagerId = villager.getUUID();
        BlockPos best = findCandidate(villager, level, kind, blockType, beClass, origin, radiusH, radiusV, extraFilter);
        if (best == null) return null;

        BlockEntity be = level.getBlockEntity(best);
        if (!beClass.isInstance(be)) return null;
        T storage = beClass.cast(be);
        boolean claimed = storage.getWorkers().tryClaim(villagerId, professionKey(villager), level.getGameTime());
        storage.setChanged();
        WorkerNaming.ensureName(villager);

        CompoundTag data = villager.getPersistentData();
        data.putLong(kind.posKey(), best.asLong());
        data.putString(kind.dimKey(), level.dimension().location().toString());
        data.putUUID(kind.idKey(), storage.getStorageId());

        if (WorkerPanelDebugConfig.DEBUG) {
            LOGGER.info("[worker-panel-debug] {} захватил слот в {} ({}) claimed={} storageId={}",
                    villagerId.toString().substring(0, 8), best.toShortString(), kind, claimed, storage.getStorageId());
        }
        return best;
    }
    @Nullable
    public static <T extends BlockEntity & WorkerStorage> BlockPos previewNearest(
            Villager villager, ServerLevel level, StorageKind kind, Block blockType, Class<T> beClass,
            BlockPos origin, int radiusH, int radiusV) {
        return findCandidate(villager, level, kind, blockType, beClass, origin, radiusH, radiusV, null);
    }

    /** Ближайшее хранилище, которое примет этого жителя (профессия подходит, место есть, предел профессии не выбран). */
    @Nullable
    private static <T extends BlockEntity & WorkerStorage> BlockPos findCandidate(
            Villager villager, ServerLevel level, StorageKind kind, Block blockType, Class<T> beClass,
            BlockPos origin, int radiusH, int radiusV, @Nullable Predicate<T> extraFilter) {
        BlockPos best = null;
        double bestDistSqr = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radiusH, -radiusV, -radiusH),
                origin.offset(radiusH, radiusV, radiusH))) {
            if (!level.isLoaded(pos)) continue;
            if (!level.getBlockState(pos).is(blockType)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (!beClass.isInstance(be)) continue;
            T storage = beClass.cast(be);
            pruneGhosts(level, storage);
            if (verdict(villager, kind, storage) != BindVerdict.OK) continue;
            if (extraFilter != null && !extraFilter.test(storage)) continue;

            double distSqr = villager.distanceToSqr(Vec3.atCenterOf(pos));
            if (distSqr < bestDistSqr) {
                bestDistSqr = distSqr;
                best = pos.immutable();
            }
        }
        return best;
    }

    /** Привязка по заявлению: проверяет профессию, место и предел профессии, и только при OK привязывает. */
    public static <T extends BlockEntity & WorkerStorage> BindVerdict bindDirect(
            Villager villager, ServerLevel level, StorageKind kind, BlockPos pos, T storage) {
        BindVerdict verdict = verdict(villager, kind, storage);
        if (verdict != BindVerdict.OK) {
            return verdict;
        }
        storage.getWorkers().tryClaim(villager.getUUID(), professionKey(villager), level.getGameTime());
        storage.setChanged();
        WorkerNaming.ensureName(villager);

        CompoundTag data = villager.getPersistentData();
        data.putLong(kind.posKey(), pos.asLong());
        data.putString(kind.dimKey(), level.dimension().location().toString());
        data.putUUID(kind.idKey(), storage.getStorageId());
        return BindVerdict.OK;
    }

    @Nullable
    private static <T extends BlockEntity & WorkerStorage> BlockPos readCachedValid(
            Villager villager, ServerLevel level, StorageKind kind, Block blockType, Class<T> beClass) {
        CompoundTag data = villager.getPersistentData();
        if (!data.contains(kind.posKey())) return null;

        ResourceLocation dimension = ResourceLocation.tryParse(data.getString(kind.dimKey()));
        if (!level.dimension().location().equals(dimension)) return null;

        BlockPos pos = BlockPos.of(data.getLong(kind.posKey()));
        if (!level.isLoaded(pos)) return pos;

        BlockEntity be = level.getBlockEntity(pos);
        if (!level.getBlockState(pos).is(blockType) || !beClass.isInstance(be)) {
            clear(data, kind);
            return null;
        }
        T storage = beClass.cast(be);
        UUID savedId = data.hasUUID(kind.idKey()) ? data.getUUID(kind.idKey()) : null;
        if (!storage.getStorageId().equals(savedId) || !storage.getWorkers().contains(villager.getUUID())) {
            clear(data, kind);
            return null;
        }
        storage.getWorkers().touch(villager.getUUID(), professionKey(villager), level.getGameTime());
        return pos;
    }

    public static boolean hasAnyBinding(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        return data.contains(StorageKind.MATERIALS.posKey()) || data.contains(StorageKind.FOOD.posKey());
    }

    public static void clear(CompoundTag data, StorageKind kind) {
        data.remove(kind.posKey());
        data.remove(kind.dimKey());
        data.remove(kind.idKey());
    }

    public static <T extends BlockEntity & WorkerStorage> void release(
            Villager villager, ServerLevel level, StorageKind kind, Block blockType, Class<T> beClass) {
        CompoundTag data = villager.getPersistentData();
        if (!data.contains(kind.posKey())) return;

        ResourceLocation dimension = ResourceLocation.tryParse(data.getString(kind.dimKey()));
        if (level.dimension().location().equals(dimension)) {
            BlockPos pos = BlockPos.of(data.getLong(kind.posKey()));
            BlockEntity be = level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
            if (beClass.isInstance(be) && level.getBlockState(pos).is(blockType)) {
                T storage = beClass.cast(be);
                storage.getWorkers().release(villager.getUUID());
                storage.setChanged();
            }
        }
        clear(data, kind);
    }
}
