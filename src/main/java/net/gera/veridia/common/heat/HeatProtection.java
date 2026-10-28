package net.gera.veridia.common.heat;

import net.minecraft.world.entity.player.Player;

/**
 * Защита игрока от жара (см. HeatHandler). Обхода жара пока нет совсем: способ (броня, зелье, эффект и т.п.) будет
 * решён позже, и добавлять его нужно только сюда.
 */
public final class HeatProtection {
    private HeatProtection() {
    }

    public static boolean isProtected(Player player) {
        return false;
    }
}
