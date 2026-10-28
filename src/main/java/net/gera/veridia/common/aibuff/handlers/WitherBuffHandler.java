package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.aibuff.WitherBlockBreakGoal;
import net.gera.veridia.common.aibuff.WitherMagmaSummonGoal;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Кастомный визер-скелет, два уровня: полноценный "witherbuff" (статы + ломание блоков + магма-кубы
 * + огненная аура) и урезанный "witherbreaker" (только ломание блоков - тот же WitherBlockBreakGoal,
 * что и у полного, включая пиллар-джамп - но без статов/меча/КБ-иммунитета, без магма-кубов и без
 * частиц). Тег можно выдать вручную командой /summon (читается на каждом тике независимо от
 * источника - как и раньше), а с недавних пор ЛЮБОЙ заспавнившийся визер-скелет (natural spawn в
 * крепости, яйцо спавна, спаунер) вдобавок получает шанс на один из двух тегов автоматически - см.
 * onFinalizeSpawn/AbilityConfig.WITHER_FULL_BUFF_SPAWN_CHANCE/WITHER_BREAKER_SPAWN_CHANCE (по
 * аналогии с ZombieBuffHandler.PICKAXE_ZOMBIE_SPAWN_CHANCE у обычных зомби).
 */
public class WitherBuffHandler {

    public static final String TAG = "witherbuff";
    public static final String BREAKER_TAG = "witherbreaker";
    /** Статы полного визера выданы - хранится в NBT (атрибуты и меч сохраняются вместе с мобом). */
    private static final String STATS_APPLIED_KEY = "veridia_witherbuff_stats_applied";
    /** Счётчик подряд полученных ударов от игрока - см. countPlayerHit/WitherBlockBreakGoal.FORCE_PILLAR_KEY. */
    private static final String HIT_COUNT_KEY = "veridia_witherbuff_hit_count";
    private static final String LAST_HIT_TIME_KEY = "veridia_witherbuff_last_hit_time";

    private enum Tier { NONE, BREAKER, FULL }

    private static Tier tierOf(WitherSkeleton skeleton) {
        if (skeleton.getTags().contains(TAG)) return Tier.FULL;
        return skeleton.getTags().contains(BREAKER_TAG) ? Tier.BREAKER : Tier.NONE;
    }

    /**
     * Один розыгрыш на спавн: не больше одного из двух тегов сразу (полный приоритетнее - сначала
     * проверяем его долю диапазона, потом облегчённую). Если не повезло ни с одним - обычный
     * визер-скелет без изменений, как и раньше в подавляющем большинстве случаев.
     */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton skeleton)) return;
        if (skeleton.level().isClientSide()) return;

        double roll = skeleton.getRandom().nextDouble();
        if (roll < AbilityConfig.WITHER_FULL_BUFF_SPAWN_CHANCE) {
            skeleton.addTag(TAG);
        } else if (roll < AbilityConfig.WITHER_FULL_BUFF_SPAWN_CHANCE + AbilityConfig.WITHER_BREAKER_SPAWN_CHANCE) {
            skeleton.addTag(BREAKER_TAG);
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof WitherSkeleton skeleton)) return;
        if (skeleton.level().isClientSide()) return;

        Tier tier = tierOf(skeleton);
        if (tier == Tier.NONE) return;

        if (tier == Tier.FULL) {
            applyStatsOnce(skeleton);
        }
        ensureGoals(skeleton, tier);

        // У "witherbreaker" своя, отдельная от полного, аура - душевно-синяя (SOUL), без огня, чтобы визуально
        // не путать его с полноценным (у которого магма-кубы и пламя).
        spawnAuraParticles((ServerLevel) skeleton.level(), skeleton, tier == Tier.FULL ? ParticleTypes.FLAME : ParticleTypes.SOUL);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton skeleton)) return;

        Tier tier = tierOf(skeleton);
        if (tier == Tier.NONE) return;

        if (tier == Tier.FULL && event.getSource().is(DamageTypes.EXPLOSION)) {
            // WitherMagmaSummonGoal спавнит взрывные магма-кубы в паре блоков от самого визера, а
            // их отложенный взрыв (ExplosiveMagmaCubeHandler) никак не исключает призвавшего моба
            // из зоны поражения. Без этой проверки визер сам себя ранил собственным призывом, а
            // ванильный HurtByTargetGoal (targetSelector, приоритет 1 - выше NearestAttackableTargetGoal)
            // после такого урона ретаргетил его на свой же магма-куб вместо игрока. "witherbreaker"
            // магма-кубов не призывает вообще - ему эта проблема не грозит, поэтому и полный
            // иммунитет ко взрывам ему ни к чему.
            event.setCanceled(true);
            return;
        }

        if (skeleton.getPersistentData().getBoolean(WitherBlockBreakGoal.BREAKING_KEY)) {
            event.setAmount(event.getAmount() * (1.0F - (float) AbilityConfig.WITHER_BREAK_DAMAGE_RESIST));
        }

        // Считаем только удары именно от игрока (getSource().getEntity() - это "виновник" урона: для стрелы/трезубца
        // это стрелявший игрок, а не сам снаряд) - урон от окружения (огонь, падение и т.п.) счётчик не двигает.
        if (skeleton.getTarget() != null && event.getSource().getEntity() instanceof Player) {
            countPlayerHit(skeleton);
        }
    }

    /**
     * Удары игрока "подряд": пауза между ударами дольше WITHER_PILLAR_HIT_WINDOW_TICKS начинает счёт заново. При
     * достижении порога счётчик гасится и WitherBlockBreakGoal просят полезть к игроку пилларом (FORCE_PILLAR_KEY) -
     * реально войти в этот режим решает уже сама Goal на своём следующем тике: только если игрок выше и рядом, иначе
     * флаг просто сбрасывается.
     */
    private static void countPlayerHit(WitherSkeleton skeleton) {
        CompoundTag data = skeleton.getPersistentData();
        long now = skeleton.level().getGameTime();
        int window = AbilityConfig.WITHER_PILLAR_HIT_WINDOW_TICKS;
        boolean streakBroken = window > 0 && now - data.getLong(LAST_HIT_TIME_KEY) > window;

        int hits = streakBroken ? 1 : data.getInt(HIT_COUNT_KEY) + 1;
        data.putLong(LAST_HIT_TIME_KEY, now);
        if (hits >= AbilityConfig.WITHER_PILLAR_TRIGGER_HITS) {
            data.putInt(HIT_COUNT_KEY, 0);
            data.putBoolean(WitherBlockBreakGoal.FORCE_PILLAR_KEY, true);
        } else {
            data.putInt(HIT_COUNT_KEY, hits);
        }
    }

    /** Статы полного визера выдаются один раз за жизнь: здоровье, отбрасывание и меч сохраняются в NBT моба. */
    private static void applyStatsOnce(WitherSkeleton skeleton) {
        if (skeleton.getPersistentData().getBoolean(STATS_APPLIED_KEY)) return;

        AttributeInstance maxHealth = skeleton.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(AbilityConfig.WITHER_MAX_HEALTH);
        }
        skeleton.setHealth((float) AbilityConfig.WITHER_MAX_HEALTH);

        AttributeInstance knockbackResistance = skeleton.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) {
            knockbackResistance.setBaseValue(AbilityConfig.WITHER_KNOCKBACK_RESISTANCE);
        }

        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        skeleton.getPersistentData().putBoolean(STATS_APPLIED_KEY, true);
    }

    /**
     * Цели (goals) в NBT не сохраняются: после перезагрузки чанка у моба снова ванильный набор, а флаги в его NBT
     * остаются. Поэтому правка целей делается не "один раз за жизнь" (как раньше - после перезагрузки визер терял
     * память о цели и снова боялся солнца), а каждый раз, когда нашей цели в наборе нет, то есть набор свежий.
     */
    private static void ensureGoals(WitherSkeleton skeleton, Tier tier) {
        if (!hasGoal(skeleton.goalSelector, WitherBlockBreakGoal.class)) {
            extendTargetMemory(skeleton);
            removeSunAvoidanceGoals(skeleton);
            skeleton.goalSelector.addGoal(0, new WitherBlockBreakGoal(skeleton));
        }
        if (tier == Tier.FULL && !hasGoal(skeleton.goalSelector, WitherMagmaSummonGoal.class)) {
            skeleton.goalSelector.addGoal(2, new WitherMagmaSummonGoal(skeleton));
        }
    }

    private static boolean hasGoal(GoalSelector selector, Class<? extends Goal> type) {
        for (WrappedGoal wrapped : selector.getAvailableGoals()) {
            if (type.isInstance(wrapped.getGoal())) {
                return true;
            }
        }
        return false;
    }

    /**
     * AbstractSkeleton.registerGoals() безусловно добавляет RestrictSunGoal/FleeSunGoal - без
     * шлема и днём они всегда активны (RestrictSunGoal вообще не занимает флагов, поэтому не
     * конфликтует с нашими Goal и просто держит navigation.avoidSun=true постоянно, пока светло).
     * У ванильных скелетов это осмысленно (горят на солнце), но визер-скелет солнцем не горит -
     * а avoidSun заставляет GroundPathNavigation.trimPath() обрезАть ЛЮБОЙ путь (включая наш
     * собственный, построенный WitherBlockBreakGoal) прямо перед первым шагом, откуда видно
     * небо. Игрок, забравшийся на открытую башню/столб, для визера в этом режиме принципиально
     * недостижим - маршрут к нему обрывается на подходе, и моб выглядит "равнодушным", хотя на
     * самом деле честно нашёл путь и ему просто не дают по нему пройти.
     */
    private static void removeSunAvoidanceGoals(WitherSkeleton skeleton) {
        List<Goal> toRemove = new ArrayList<>();
        for (WrappedGoal wrapped : skeleton.goalSelector.getAvailableGoals()) {
            Goal goal = wrapped.getGoal();
            if (goal instanceof RestrictSunGoal || goal instanceof FleeSunGoal) {
                toRemove.add(goal);
            }
        }
        toRemove.forEach(skeleton.goalSelector::removeGoal);
    }

    /**
     * AbstractSkeleton регистрирует NearestAttackableTargetGoal<Player> с mustSee=true - у
     * ванильного TargetGoal это значит, что если моб не видит цель дольше unseenMemoryTicks
     * (по умолчанию 60 тиков = 3 секунды), TargetGoal.canContinueToUse() возвращает false и
     * TargetGoal.stop() делает mob.setTarget(null) - цель обнуляется ПОЛНОСТЬЮ, а не просто
     * "пропадает путь". Это была настоящая причина того, что моб "мгновенно теряет" игрока и
     * бросает ломать блоки на маршрутах длиннее ~3 секунд: пока WitherBlockBreakGoal честно
     * прокладывает обход/лесенку, цель обнуляется у него из-под ног ещё до завершения плана.
     * Увеличиваем память до WITHER_TARGET_UNSEEN_MEMORY_TICKS, чтобы обхода/ломания хватало
     * времени завершиться, не теряя цель.
     */
    private static void extendTargetMemory(WitherSkeleton skeleton) {
        for (WrappedGoal wrapped : skeleton.targetSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof NearestAttackableTargetGoal<?> targetGoal) {
                targetGoal.setUnseenMemoryTicks(AbilityConfig.WITHER_TARGET_UNSEEN_MEMORY_TICKS);
            }
        }
    }

    private static void spawnAuraParticles(ServerLevel level, WitherSkeleton skeleton, ParticleOptions particle) {
        double baseAngle = (level.getGameTime() % AbilityConfig.WITHER_AURA_PERIOD_TICKS)
                * (Math.PI * 2 / AbilityConfig.WITHER_AURA_PERIOD_TICKS);
        double y = skeleton.getY() + skeleton.getBbHeight() * AbilityConfig.WITHER_AURA_HEIGHT_FACTOR;

        for (int i = 0; i < AbilityConfig.WITHER_AURA_POINTS; i++) {
            double angle = baseAngle + (Math.PI * 2 / AbilityConfig.WITHER_AURA_POINTS) * i;
            double x = skeleton.getX() + Math.cos(angle) * AbilityConfig.WITHER_AURA_RADIUS;
            double z = skeleton.getZ() + Math.sin(angle) * AbilityConfig.WITHER_AURA_RADIUS;
            level.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
