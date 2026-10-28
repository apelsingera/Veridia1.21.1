package net.gera.veridia.common.heat;

/** Состояние жара одного игрока на сервере (см. {@link HeatHandler}). */
final class HeatState {
    /** Сколько тиков подряд игрок стоит у источника жара; когда он ушёл, спадает быстрее, чем росло. */
    int exposureTicks;
    /** Насколько сильно эффект сейчас, от 0 до 1: нарастает и спадает плавно, а не скачком. */
    float intensity;
    /** Урон за раз у источника жара, у которого игрок стоит сейчас (0 - источника рядом нет). */
    float damage;
    /** Какая интенсивность в процентах уже отправлена клиенту. */
    int sentPercent;

    void reset() {
        exposureTicks = 0;
        intensity = 0.0F;
        damage = 0.0F;
    }
}
