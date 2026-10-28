package net.gera.veridia.common.villager.storage;

import java.util.UUID;

public interface WorkerStorage {
    UUID getStorageId();

    /** Вид хранилища: от него зависит, какие профессии сюда можно привязать. */
    StorageKind kind();

    WorkerSlots getWorkers();
}
