package net.gera.veridia.common.villager.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Кто из жителей-работников умер или исчез. Если это случилось, пока хранилище выгружено, освободить его место
 * сразу нельзя (VillagerStorageBinding.release не дотягивается до выгруженного блока), а выгруженный живой житель
 * от мёртвого не отличим. Поэтому смерть записывается сюда, и хранилище при первой же проверке
 * (VillagerStorageBinding.pruneGhosts: открытие окна, поиск хранилища) снимает с мест всех, кто здесь числится.
 * <p>
 * Запись нужна, пока не найдётся хранилище с этим жителем, поэтому живёт ENTRY_LIFETIME_TICKS и не больше
 * MAX_ENTRIES штук (самые старые уходят первыми); дальше место освободит таймер WorkerSlots.pruneStale.
 */
public final class VillagerGraveyard extends SavedData {

    private static final String DATA_NAME = "veridia_villager_graveyard";
    private static final String ENTRIES_KEY = "dead";
    private static final String ID_KEY = "id";
    private static final String TIME_KEY = "time";

    /** Сколько игровых тиков запись хранится (30 игровых суток). */
    public static final long ENTRY_LIFETIME_TICKS = 24000L * 30;
    public static final int MAX_ENTRIES = 2048;

    /** UUID жителя -> игровое время смерти; порядок вставки, старые записи в начале. */
    private final Map<UUID, Long> diedAt = new LinkedHashMap<>();

    /** Список общий на весь мир: живёт в данных верхнего мира. */
    public static VillagerGraveyard get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VillagerGraveyard::new, VillagerGraveyard::load), DATA_NAME);
    }

    public boolean isDead(UUID villagerId) {
        return diedAt.containsKey(villagerId);
    }

    public void bury(UUID villagerId, long now) {
        diedAt.put(villagerId, now);
        forgetOldEntries(now);
        setDirty();
    }

    private void forgetOldEntries(long now) {
        Iterator<Map.Entry<UUID, Long>> oldestFirst = diedAt.entrySet().iterator();
        while (oldestFirst.hasNext()) {
            Map.Entry<UUID, Long> entry = oldestFirst.next();
            boolean tooMany = diedAt.size() > MAX_ENTRIES;
            boolean tooOld = now - entry.getValue() > ENTRY_LIFETIME_TICKS;
            if (!tooMany && !tooOld) {
                return;
            }
            oldestFirst.remove();
        }
    }

    private static VillagerGraveyard load(CompoundTag tag, HolderLookup.Provider registries) {
        VillagerGraveyard graveyard = new VillagerGraveyard();
        ListTag list = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID(ID_KEY)) {
                graveyard.diedAt.put(entry.getUUID(ID_KEY), entry.getLong(TIME_KEY));
            }
        }
        return graveyard;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        ListTag list = new ListTag();
        diedAt.forEach((id, time) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(ID_KEY, id);
            entry.putLong(TIME_KEY, time);
            list.add(entry);
        });
        tag.put(ENTRIES_KEY, list);
        return tag;
    }
}
