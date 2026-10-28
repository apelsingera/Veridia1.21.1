package net.gera.veridia.common.compat.graveyard;

import net.minecraft.nbt.CompoundTag;

/**
 * Блок-сущность гроба/саркофага мода Graveyard, умеющая хранить данные ловушки (Mob, PresetMob, AppearTime,
 * AppearParticle - см. {@link CoffinTrapSpec}). Интерфейс добавляет к ней миксин: сам класс чужого мода мы не правим.
 */
public interface CoffinTrapHolder {
    /** Данные ловушки: только её ключи, пустой тег - ловушки нет. Это живой тег, менять его снаружи не нужно. */
    CompoundTag getCoffinTrap();

    /** Снимает ловушку: гроб дальше работает как обычный. */
    void clearCoffinTrap();
}
