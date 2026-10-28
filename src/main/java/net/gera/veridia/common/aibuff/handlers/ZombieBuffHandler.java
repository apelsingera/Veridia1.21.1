package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.aibuff.WitherBlockBreakGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Обычный зомби (EntityType.ZOMBIE - не халк/утопленник/зомби-житель/зомбификат) при спавне
 * разыгрывает один из двух тегов: "strongpickaxezombie" (STRONG_PICKAXE_ZOMBIE_SPAWN_CHANCE) или
 * обычный "pickaxezombie" (PICKAXE_ZOMBIE_SPAWN_CHANCE) - не больше одного сразу. Оба получают в
 * руку кирку и переиспользуют WitherBlockBreakGoal/WitherPathfinder - тот же A* с ломанием блоков,
 * что и у визера-скелета. Усиленный дополнительно крепче (STRONG_PICKAXE_ZOMBIE_MAX_HEALTH), не
 * отбрасывается (STRONG_PICKAXE_ZOMBIE_KNOCKBACK_RESISTANCE) и ломает без искусственного
 * замедления (STRONG_PICKAXE_ZOMBIE_BREAK_SLOWNESS_MULTIPLIER=1.0 против обычных x2 - см.
 * WitherBlockBreakGoal(Mob, double)). Никаких дополнительных способностей (призывов и т.п.) у
 * них нет - только обход препятствий.
 * WitherBlockBreakGoal регистрируется с приоритетом 0, что строго выше как ZombieAttackGoal
 * (приоритет 2), так и родного ванильного breakDoorGoal зомби (приоритет 1, только на Hard) -
 * конфликтов за MOVE-флаг с ними нет по той же причине, что и у визера (см. WitherBuffHandler).
 * С дверями наш Goal не пересекается вовсе: дерево/двери не входят в наш pickaxe-whitelist
 * (AbilityConfig.isPickaxeWhitelisted), поэтому маршрут через закрытую дверь для нашего A*
 * выглядит непроходимым, и мы просто не находим план через неё - ломание двери как и раньше
 * полностью на родном breakDoorGoal.
 */
public class ZombieBuffHandler {

    public static final String TAG = "pickaxezombie";
    public static final String STRONG_TAG = "strongpickaxezombie";
    private static final String STRONG_STATS_APPLIED_KEY = "veridia_strongpickaxezombie_stats_applied";

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.level().isClientSide()) return;
        if (zombie.getType() != EntityType.ZOMBIE) return;

        double roll = zombie.getRandom().nextDouble();
        if (roll < AbilityConfig.STRONG_PICKAXE_ZOMBIE_SPAWN_CHANCE) {
            zombie.addTag(STRONG_TAG);
            equipPickaxe(zombie);
        } else if (roll < AbilityConfig.STRONG_PICKAXE_ZOMBIE_SPAWN_CHANCE + AbilityConfig.PICKAXE_ZOMBIE_SPAWN_CHANCE) {
            zombie.addTag(TAG);
            equipPickaxe(zombie);
        }
    }

    private static void equipPickaxe(Zombie zombie) {
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.level().isClientSide()) return;

        boolean strong = zombie.getTags().contains(STRONG_TAG);
        boolean normal = !strong && zombie.getTags().contains(TAG);
        if (!strong && !normal) return;

        if (strong && !zombie.getPersistentData().getBoolean(STRONG_STATS_APPLIED_KEY)) {
            applyStrongStats(zombie);
            zombie.getPersistentData().putBoolean(STRONG_STATS_APPLIED_KEY, true);
        }

        ensureBreakGoal(zombie, strong);
    }

    /**
     * Цели (goals) в NBT не сохраняются: после перезагрузки чанка у зомби снова ванильный набор, а флаги в его NBT
     * остаются. Поэтому память о цели продлевается не "один раз за жизнь" (как раньше - после перезагрузки зомби-шахтёр
     * снова терял игрока через 3 секунды без видимости и не доламывал стену), а каждый раз, когда нашей цели в наборе
     * нет, то есть набор свежий.
     */
    private static void ensureBreakGoal(Zombie zombie, boolean strong) {
        boolean hasBreakGoal = zombie.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof WitherBlockBreakGoal);
        if (hasBreakGoal) return;

        extendTargetMemory(zombie);
        double breakSpeedMultiplier = strong
                ? AbilityConfig.STRONG_PICKAXE_ZOMBIE_BREAK_SLOWNESS_MULTIPLIER
                : AbilityConfig.WITHER_BREAK_SLOWNESS_MULTIPLIER;
        zombie.goalSelector.addGoal(0, new WitherBlockBreakGoal(zombie, breakSpeedMultiplier));
    }

    private static void applyStrongStats(Zombie zombie) {
        AttributeInstance maxHealth = zombie.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(AbilityConfig.STRONG_PICKAXE_ZOMBIE_MAX_HEALTH);
        }
        zombie.setHealth((float) AbilityConfig.STRONG_PICKAXE_ZOMBIE_MAX_HEALTH);

        AttributeInstance knockbackResistance = zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) {
            knockbackResistance.setBaseValue(AbilityConfig.STRONG_PICKAXE_ZOMBIE_KNOCKBACK_RESISTANCE);
        }
    }

    /** Zombie тоже регистрирует NearestAttackableTargetGoal<Player> с mustSee=true - см. подробное объяснение в WitherBuffHandler.extendTargetMemory. */
    private static void extendTargetMemory(Zombie zombie) {
        for (WrappedGoal wrapped : zombie.targetSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof NearestAttackableTargetGoal<?> targetGoal) {
                targetGoal.setUnseenMemoryTicks(AbilityConfig.WITHER_TARGET_UNSEEN_MEMORY_TICKS);
            }
        }
    }
}
