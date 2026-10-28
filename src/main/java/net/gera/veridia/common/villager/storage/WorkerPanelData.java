package net.gera.veridia.common.villager.storage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Данные панели жителей, которые хранилище отдаёт клиенту через ContainerData (по одному числу на поле): для каждого
 * места - состояние, профессия и номер имени. Раскладка общая для обоих хранилищ и их меню: индексы 0 и 1 заняты
 * прокачкой (progress/maxProgress), дальше три блока по MAX_WORKERS чисел.
 */
public final class WorkerPanelData {

    private WorkerPanelData() {
    }

    public static final int FIRST_INDEX = 2;
    public static final int TOTAL = FIRST_INDEX + 3 * WorkerSlots.MAX_WORKERS;

    /** slotState: пусто. */
    public static final int STATE_EMPTY = 0;
    public static final int STATE_ENABLED = 1;
    public static final int STATE_DISABLED = 2;
    public static final int UNKNOWN = -1;

    public static int stateIndex(int slot) {
        return FIRST_INDEX + slot;
    }

    public static int professionIndex(int slot) {
        return FIRST_INDEX + WorkerSlots.MAX_WORKERS + slot;
    }

    public static int nameIndex(int slot) {
        return FIRST_INDEX + 2 * WorkerSlots.MAX_WORKERS + slot;
    }

    /** Значение поля с индексом index >= FIRST_INDEX (сервер, по живому миру). */
    public static int read(@Nullable Level level, WorkerSlots workers, int index) {
        int field = index - FIRST_INDEX;
        int slot = field % WorkerSlots.MAX_WORKERS;
        UUID id = workers.get(slot);
        if (field < WorkerSlots.MAX_WORKERS) {
            return state(level, id);
        }
        if (id == null) {
            return UNKNOWN;
        }
        Villager villager = loaded(level, id);
        if (field < 2 * WorkerSlots.MAX_WORKERS) {
            return villager == null ? storedProfession(workers.professionAt(slot))
                    : BuiltInRegistries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession());
        }
        return villager == null ? UNKNOWN : WorkerNaming.nameIndex(villager);
    }

    /** Профессия, которую хранилище запомнило, - её видно в панели и у жителя, который сейчас не загружен. */
    private static int storedProfession(@Nullable ResourceLocation key) {
        if (key == null) {
            return UNKNOWN;
        }
        return BuiltInRegistries.VILLAGER_PROFESSION.getOptional(key).map(BuiltInRegistries.VILLAGER_PROFESSION::getId).orElse(UNKNOWN);
    }

    /** Место занято, но житель не загружен (мёртвых хранилище снимает с мест при проверке, см. pruneGhosts): считаем работающим. */
    private static int state(@Nullable Level level, @Nullable UUID id) {
        if (id == null) {
            return STATE_EMPTY;
        }
        Villager villager = loaded(level, id);
        if (villager == null) {
            return STATE_ENABLED;
        }
        return VillagerEmployment.isWorkEnabled(villager) ? STATE_ENABLED : STATE_DISABLED;
    }

    @Nullable
    private static Villager loaded(@Nullable Level level, UUID id) {
        return level instanceof ServerLevel serverLevel && serverLevel.getEntity(id) instanceof Villager villager ? villager : null;
    }
}
