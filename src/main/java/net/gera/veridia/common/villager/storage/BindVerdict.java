package net.gera.veridia.common.villager.storage;

/** Можно ли привязать жителя к хранилищу, а если нет - почему (по этому ответу игроку пишется причина). */
public enum BindVerdict {
    OK,
    /** Профессия жителя не работает с этим видом хранилища (см. {@link StorageKind#accepts}). */
    WRONG_PROFESSION,
    /** Все места заняты. */
    FULL,
    /** Свободные места есть, но жителей этой профессии уже {@link WorkerSlots#MAX_PER_PROFESSION}. */
    PROFESSION_LIMIT
}
