package net.gera.veridia.common.heat;

import net.minecraft.server.level.ServerPlayer;

/**
 * Урон от жара без отдачи и тряски камеры: обычный {@code hurt} толкает игрока и качает экран, а жар должен просто
 * отнимать здоровье. Поэтому здоровье уменьшается напрямую. Смертельный удар идёт обычным путём, чтобы игрок умер по
 * правилам игры (сообщение о смерти, выпадение вещей).
 */
final class HeatDamage {
    private HeatDamage() {
    }

    static void deal(ServerPlayer player, float amount) {
        float health = player.getHealth();
        if (amount < health) {
            player.setHealth(health - amount);
        } else {
            player.hurt(ModDamageTypes.heat(player.level()), amount);
        }
    }
}
