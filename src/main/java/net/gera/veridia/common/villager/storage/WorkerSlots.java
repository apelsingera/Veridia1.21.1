package net.gera.veridia.common.villager.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Места жителей одного хранилища. Места идут подряд без пропусков: когда житель уходит (умер, уволен), следующие за
 * ним сдвигаются вверх, и панель показывает ровно занятые места.
 * <p>
 * Кроме UUID место помнит профессию жителя (её видно в панели и когда житель не загружен, и по ней считается
 * предел {@link #MAX_PER_PROFESSION}) и отметку seenAt - игровое время, когда житель последний раз подтвердил связь
 * (загружен и жив, или сам прошёл проверку привязки). По ней {@link #pruneStale} освобождает "призраков": место
 * жителя, который умер или пропал, пока хранилище не знало (событие смерти обрабатывается отдельно, см.
 * VillagerBindingCleanup и VillagerGraveyard, но хранилище могло быть выгружено).
 */
public class WorkerSlots {
    public static final int MAX_WORKERS = 5;
    /** Сколько жителей одной профессии вмещает хранилище; остальные места достаются другим профессиям. */
    public static final int MAX_PER_PROFESSION = 3;

    /** seenAt ещё не известно (старые сохранения без отметки): при первой проверке получает текущее время. */
    private static final long SEEN_UNKNOWN = -1L;

    /** Профессия null - не известна (старые сохранения), пока житель не окажется загруженным. */
    private record Worker(UUID id, long seenAt, @Nullable ResourceLocation profession) {
        Worker seenAt(long now) {
            return new Worker(id, now, profession);
        }
    }

    private final Worker[] slots = new Worker[MAX_WORKERS];

    @Nullable
    public UUID get(int index) {
        return slots[index] == null ? null : slots[index].id();
    }

    @Nullable
    public ResourceLocation professionAt(int index) {
        return slots[index] == null ? null : slots[index].profession();
    }

    /** UUID занятых мест по порядку - копия, по ней можно освобождать места на ходу. */
    public List<UUID> ids() {
        List<UUID> ids = new ArrayList<>();
        for (Worker worker : slots) {
            if (worker != null) {
                ids.add(worker.id());
            }
        }
        return ids;
    }

    public boolean contains(UUID villagerId) {
        return indexOf(villagerId) >= 0;
    }

    private int indexOf(UUID villagerId) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && slots[i].id().equals(villagerId)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isFull() {
        return slots[MAX_WORKERS - 1] != null;
    }

    /** Сколько мест занято жителями этой профессии (жители с неизвестной профессией не считаются). */
    public int countOf(@Nullable ResourceLocation profession) {
        int count = 0;
        for (Worker worker : slots) {
            if (worker != null && worker.profession() != null && Objects.equals(worker.profession(), profession)) {
                count++;
            }
        }
        return count;
    }

    /** Примет ли хранилище жителя этой профессии (WRONG_PROFESSION здесь не бывает - это решает StorageKind). */
    public BindVerdict admission(UUID villagerId, @Nullable ResourceLocation profession) {
        if (contains(villagerId)) {
            return BindVerdict.OK;
        }
        if (isFull()) {
            return BindVerdict.FULL;
        }
        return countOf(profession) >= MAX_PER_PROFESSION ? BindVerdict.PROFESSION_LIMIT : BindVerdict.OK;
    }

    public boolean tryClaim(UUID villagerId, @Nullable ResourceLocation profession, long now) {
        if (contains(villagerId)) {
            touch(villagerId, profession, now);
            return true;
        }
        if (admission(villagerId, profession) != BindVerdict.OK) {
            return false;
        }
        slots[firstFreeIndex()] = new Worker(villagerId, now, profession);
        return true;
    }

    private int firstFreeIndex() {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                return i;
            }
        }
        throw new IllegalStateException("нет свободного места, а admission разрешил");
    }

    /**
     * Житель на связи: отметка seenAt обновляется, а известная профессия запоминается (null - профессия не известна,
     * прежняя остаётся). true - запомненная профессия поменялась и хранилище надо сохранить.
     */
    public boolean touch(UUID villagerId, @Nullable ResourceLocation profession, long now) {
        int index = indexOf(villagerId);
        if (index < 0) {
            return false;
        }
        ResourceLocation known = profession != null ? profession : slots[index].profession();
        boolean changed = !Objects.equals(known, slots[index].profession());
        slots[index] = new Worker(villagerId, now, known);
        return changed;
    }

    public void release(UUID villagerId) {
        int index = indexOf(villagerId);
        if (index >= 0) {
            slots[index] = null;
            compact();
        }
    }

    /**
     * Освобождает места жителей, которых нет уже дольше maxAgeTicks. isPresent - "житель сейчас загружен и жив":
     * такому отметка обновляется. Выгруженный житель не отличим от мёртвого, поэтому ждём maxAgeTicks - вернувшийся
     * позже сам заново займёт место (VillagerStorageBinding). Возвращает число освобождённых мест.
     */
    public int pruneStale(long now, long maxAgeTicks, Predicate<UUID> isPresent) {
        int removed = 0;
        for (int i = 0; i < slots.length; i++) {
            Worker worker = slots[i];
            if (worker == null) continue;
            if (isPresent.test(worker.id()) || worker.seenAt() == SEEN_UNKNOWN) {
                slots[i] = worker.seenAt(now);
            } else if (now - worker.seenAt() > maxAgeTicks) {
                slots[i] = null;
                removed++;
            }
        }
        compact();
        return removed;
    }

    /** Освобождает места жителей, про которых точно известно, что их больше нет. Возвращает число освобождённых мест. */
    public int releaseWhere(Predicate<UUID> isGone) {
        int removed = 0;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && isGone.test(slots[i].id())) {
                slots[i] = null;
                removed++;
            }
        }
        compact();
        return removed;
    }

    /** Сдвигает занятые места вверх, чтобы пустых между ними не было. */
    private void compact() {
        int write = 0;
        for (int read = 0; read < slots.length; read++) {
            if (slots[read] != null) {
                slots[write++] = slots[read];
            }
        }
        while (write < slots.length) {
            slots[write++] = null;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Worker worker : slots) {
            CompoundTag entry = new CompoundTag();
            if (worker != null) {
                entry.putUUID("id", worker.id());
                entry.putLong("seen", worker.seenAt());
                if (worker.profession() != null) {
                    entry.putString("profession", worker.profession().toString());
                }
            }
            list.add(entry);
        }
        tag.put("slots", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        Arrays.fill(slots, null);
        ListTag list = tag.getList("slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < slots.length; i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("id")) {
                long seen = entry.contains("seen") ? entry.getLong("seen") : SEEN_UNKNOWN;
                ResourceLocation profession = entry.contains("profession") ? ResourceLocation.tryParse(entry.getString("profession")) : null;
                slots[i] = new Worker(entry.getUUID("id"), seen, profession);
            }
        }
        compact();
    }
}
