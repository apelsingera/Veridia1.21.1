package net.gera.veridia.common.block.entity.screen;

/** Что экрану хранилища нужно от меню: данные панели жителей, полоса прокачки и раскладка (текстура/слоты) уровня. */
public interface WorkerPanelMenuAccess {
    boolean isSlotBound(int slot);
    boolean isSlotEnabled(int slot);
    int slotProfessionId(int slot);

    /** Номер имени жителя от мода (WorkerNaming), 0 или меньше - имени нет или житель не загружен. */
    int slotNameIndex(int slot);

    int getScaledProgress(int barPixelWidth);

    StorageLevelLayout layout();
}
