package net.gera.veridia.common.aibuff.villager.angry;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

/**
 * Состояние "злости" жителя с тегом TAG: таймер прощения (VILLAGER_ANGER_DURATION_TICKS) и
 * перезарядка heavy_projectile (VILLAGER_PROJECTILE_COOLDOWN_TICKS), хранятся в persistentData -
 * по конвенции aibuff (см. WitherBuffHandler/PullAttackGoal), без отдельного Attachment.
 * Целью может быть как игрок, так и враждебный моб (см. AngryVillagerHandler) - отсюда LivingEntity,
 * а не Player.
 */
public final class AngryVillager {

    public static final String TAG = "angryvillager";

    private static final String ANGRY_UNTIL_KEY = "veridia_angryvillager_angry_until";
    private static final String TARGET_KEY = "veridia_angryvillager_target";
    private static final String PROJECTILE_COOLDOWN_KEY = "veridia_angryvillager_projectile_cooldown";
    private static final String HOE_EQUIPPED_KEY = "veridia_angryvillager_hoe_equipped";
    private static final String RETREAT_UNTIL_KEY = "veridia_angryvillager_retreat_until";

    private AngryVillager() {}

    public static boolean hasTag(Villager villager) {
        return villager.getTags().contains(TAG);
    }

    /** Жителя ударил игрок или враждебный моб - запоминаем обидчика и продлеваем злость ещё на VILLAGER_ANGER_DURATION_TICKS. */
    public static void makeAngry(Villager villager, LivingEntity attacker) {
        CompoundTag data = villager.getPersistentData();
        long angryUntil = villager.level().getGameTime() + AbilityConfig.VILLAGER_ANGER_DURATION_TICKS;
        data.putLong(ANGRY_UNTIL_KEY, angryUntil);
        data.putUUID(TARGET_KEY, attacker.getUUID());
    }

    public static boolean isAngry(Villager villager) {
        return villager.getPersistentData().getLong(ANGRY_UNTIL_KEY) > villager.level().getGameTime();
    }

    /** Обидчик (игрок или моб), если он ещё жив и загружен в мире, иначе null. */
    public static LivingEntity resolveTarget(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        if (!data.hasUUID(TARGET_KEY)) return null;
        if (!(villager.level() instanceof ServerLevel serverLevel)) return null;

        Entity entity = serverLevel.getEntity(data.getUUID(TARGET_KEY));
        if (entity instanceof LivingEntity living && living.isAlive()
                && !(living instanceof Player player && player.isSpectator())) {
            return living;
        }
        return null;
    }

    public static boolean isProjectileCooldownReady(Villager villager) {
        long last = villager.getPersistentData().getLong(PROJECTILE_COOLDOWN_KEY);
        return villager.level().getGameTime() - last >= AbilityConfig.VILLAGER_PROJECTILE_COOLDOWN_TICKS;
    }

    public static void setProjectileCooldown(Villager villager) {
        villager.getPersistentData().putLong(PROJECTILE_COOLDOWN_KEY, villager.level().getGameTime());
    }

    public static boolean hasEquippedHoe(Villager villager) {
        return villager.getPersistentData().getBoolean(HOE_EQUIPPED_KEY);
    }

    public static void markHoeEquipped(Villager villager) {
        villager.getPersistentData().putBoolean(HOE_EQUIPPED_KEY, true);
    }

    /**
     * После удара мотыгой житель на VILLAGER_RETREAT_DURATION_TICKS отбегает, а не стоит вплотную.
     * Заодно сбрасываем перезарядку heavy_projectile - иначе цикл "подошёл-ударил-отбежал-кинул"
     * часто ломался на последнем шаге: 40-секундный кулдаун почти всегда ещё не истёк с прошлого
     * броска, и после отступления жителю просто нечем было стрелять.
     */
    public static void startRetreat(Villager villager) {
        long until = villager.level().getGameTime() + AbilityConfig.VILLAGER_RETREAT_DURATION_TICKS;
        villager.getPersistentData().putLong(RETREAT_UNTIL_KEY, until);
        villager.getPersistentData().putLong(PROJECTILE_COOLDOWN_KEY,
                villager.level().getGameTime() - AbilityConfig.VILLAGER_PROJECTILE_COOLDOWN_TICKS);
    }

    public static boolean isRetreating(Villager villager) {
        return villager.getPersistentData().getLong(RETREAT_UNTIL_KEY) > villager.level().getGameTime();
    }
}
