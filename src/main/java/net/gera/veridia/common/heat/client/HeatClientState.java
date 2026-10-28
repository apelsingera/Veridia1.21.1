package net.gera.veridia.common.heat.client;

import net.minecraft.util.Mth;

/**
 * Сила жара, которую видит клиент: сервер присылает целевое значение (см. HeatPayload), а здесь оно плавно догоняется
 * каждый тик, чтобы виньетка и угол обзора не прыгали. Только клиентский поток.
 */
public final class HeatClientState {
    // Какую долю оставшейся разницы клиент догоняет за тик
    private static final float CATCH_UP = 0.3F;
    // Ближе этого к цели значение прыгает прямо в цель
    private static final float SNAP_DISTANCE = 0.005F;

    private static float target;
    private static float current;
    private static float previous;

    private HeatClientState() {
    }

    /** Пришла сила эффекта от сервера в процентах (0-100). */
    public static void receive(int percent) {
        target = Mth.clamp(percent / 100.0F, 0.0F, 1.0F);
    }

    static void tick() {
        previous = current;
        current += (target - current) * CATCH_UP;
        if (Math.abs(target - current) < SNAP_DISTANCE) {
            current = target;
        }
    }

    /** Сила эффекта от 0 до 1 между тиками (partialTick - доля до следующего тика). */
    public static float intensity(float partialTick) {
        return Mth.lerp(partialTick, previous, current);
    }

    static void reset() {
        target = 0.0F;
        current = 0.0F;
        previous = 0.0F;
    }
}
