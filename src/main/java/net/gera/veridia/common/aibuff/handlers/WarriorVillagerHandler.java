package net.gera.veridia.common.aibuff.handlers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import org.slf4j.Logger;
import net.gera.veridia.common.aibuff.LongReachAttackGoal;
import net.gera.veridia.common.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorBowAttackGoal;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorMeleeAttackGoal;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorVillager;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorVillagerConfig;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorVillagerTargetGoal;
import net.gera.veridia.common.entity.ominous.OminousWarriorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class WarriorVillagerHandler {

    /**
     * Условие, которое у жителя не выполнится никогда: ADMIRING_ITEM - память пиглина,
     * в Villager.MEMORY_TYPES её нет. Brain.checkMemory для незарегистрированной памяти
     * возвращает false, поэтому активность с таким требованием не запускается.
     */
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Строп, за которым воин закреплён, - чтобы вернуть его после PoiCompetitorScan. */
    private static final String JOB_SITE_POS_KEY = "veridia_warrior_jobsite_pos";
    private static final String JOB_SITE_DIM_KEY = "veridia_warrior_jobsite_dim";
    /** Последнее залогированное состояние памяти - чтобы писать только изменения. */
    private static final String TRACE_KEY = "veridia_warrior_trace";
    /** Строп, на который житель взял билет через POTENTIAL_JOB_SITE. */
    private static final String POTENTIAL_POS_KEY = "veridia_warrior_potential_pos";

    private static final Set<Pair<MemoryModuleType<?>, MemoryStatus>> PANIC_NEVER =
            ImmutableSet.<Pair<MemoryModuleType<?>, MemoryStatus>>of(
                    Pair.of(MemoryModuleType.ADMIRING_ITEM, MemoryStatus.VALUE_PRESENT));

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;
        if (!WarriorVillager.isWarrior(villager)) {
            demote(villager);
            return;
        }

        WarriorVillager.ensureStats(villager);
        WarriorVillager.ensureGear(villager);
        WarriorVillager.syncArmorGear(villager);
        WarriorVillager.syncSpeedBonus(villager);
        WarriorVillager.tickBrainHeal(villager);
        // Тег/атрибут дистанции пика могла потерять при перезаходе - подтверждаем каждый тик, это дёшево.
        WarriorVillager.applyLongReach(villager);
        blockPanicActivity(villager);
        installGoals(villager);
    }

    /**
     * Затыкает панику в корне, а не разгребает её последствия.
     * <p>
     * Стирать NEAREST_HOSTILE/HURT_BY снаружи бесполезно: Brain.tick сначала прогоняет сенсоры,
     * VillagerHostilesSensor заново кладёт NEAREST_HOSTILE, и уже в этом же тике VillagerPanicTrigger
     * (он в Activity.CORE, то есть работает всегда) вызывает setActiveActivityIfPossible(PANIC).
     * Наша чистка в EntityTickEvent.Post приходит после того, как SetWalkTargetAwayFrom уже
     * назначил цель убегания - отсюда и рывки в сторону.
     * <p>
     * Поэтому подменяем требования самой активности: addActivityAndRemoveMemoriesWhenStopped делает
     * activityRequirements.put(activity, ...), то есть ЗАМЕНЯЕТ набор условий. С невыполнимым условием
     * setActiveActivityIfPossible уходит в useDefaultActivity(), и PANIC не включается никогда,
     * а её поведения (они привязаны к активности) не получают шанса отработать.
     * <p>
     * Делается каждый тик, потому что мозг пересоздаётся при смене профессии и при перезаходе в мир.
     */
    private static void blockPanicActivity(Villager villager) {
        if (!WarriorVillagerConfig.SUPPRESS_PANIC) return;

        villager.getBrain().addActivityWithConditions(Activity.PANIC, ImmutableList.of(), PANIC_NEVER);
    }

    @SubscribeEvent
    public static void onTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;

        // До проверки профессии: интересны как раз безработные, которые станок не берут.
        debugJobSite(villager);
        traceJobSiteMemory(villager);
        releaseAbandonedTicket(villager);
        repairStropTickets(villager);

        if (!WarriorVillager.isWarrior(villager)) return;

        WarriorVillager.keepWeaponEquipped(villager);
        keepJobSite(villager);
        keepGrudgeTarget(villager);
        suppressPanic(villager);
    }

    /**
     * Возвращает билет в тот же тик, когда ваниль его сожгла.
     * <p>
     * ValidateNearbyPoi (Activity.CORE, приоритет 0, каждый тик) стирает POTENTIAL_JOB_SITE, если
     * POI не проходит валидацию, и билет при этом НЕ возвращает - release там есть только в ветке
     * с занятой кроватью. Житель берёт билет через AcquirePoi, теряет память и уходит ни с чем,
     * а билет сгорает навсегда.
     * <p>
     * Периодическая починка стропов это разгребала, но раз в 100 тиков, а сгорало за доли секунды -
     * отсюда и "работает, но медленно". Здесь ловим ровно момент пропажи: если POTENTIAL_JOB_SITE
     * исчез и НЕ превратился в JOB_SITE той же позиции, значит билет сгорел зря, возвращаем сразу.
     */
    private static void releaseAbandonedTicket(Villager villager) {
        if (!WarriorVillagerConfig.SHARE_JOB_SITE) return;
        if (!(villager.level() instanceof ServerLevel level)) return;

        Brain<Villager> brain = villager.getBrain();
        CompoundTag data = villager.getPersistentData();
        Optional<GlobalPos> potential = brain.getMemory(MemoryModuleType.POTENTIAL_JOB_SITE);

        if (potential.isPresent()) {
            data.putLong(POTENTIAL_POS_KEY, potential.get().pos().asLong());
            return;
        }

        if (!data.contains(POTENTIAL_POS_KEY)) return;
        BlockPos pos = BlockPos.of(data.getLong(POTENTIAL_POS_KEY));
        data.remove(POTENTIAL_POS_KEY);

        // Память переехала в JOB_SITE - билет потрачен по делу, возвращать нечего.
        GlobalPos jobSite = GlobalPos.of(level.dimension(), pos);
        if (brain.getMemory(MemoryModuleType.JOB_SITE).filter(jobSite::equals).isPresent()) return;

        // release кидает IllegalStateException, если записи POI нет, - проверяем заранее.
        if (!level.isLoaded(pos)) return;
        if (!level.getPoiManager().exists(pos, holder -> holder.value() == ModVillagers.STROP_POI.value())) return;

        level.getPoiManager().release(pos);
    }

    /**
     * Пишет в лог каждое изменение POTENTIAL_JOB_SITE и JOB_SITE, без троттлинга.
     * <p>
     * Раз в 100 тиков разглядеть жизненный цикл памяти невозможно: житель успевает взять
     * POTENTIAL_JOB_SITE и потерять его между двумя строками. Здесь видно последовательность:
     * появился potential -> исчез (и появился ли при этом job). Если potential исчезает, а job
     * не появляется - память стирает валидатор, и билет сгорает.
     */
    private static void traceJobSiteMemory(Villager villager) {
        if (!WarriorVillagerConfig.DEBUG_JOB_SITE) return;

        Brain<Villager> brain = villager.getBrain();
        CompoundTag data = villager.getPersistentData();

        String potential = brain.getMemory(MemoryModuleType.POTENTIAL_JOB_SITE)
                .map(pos -> pos.pos().toShortString()).orElse("-");
        String job = brain.getMemory(MemoryModuleType.JOB_SITE)
                .map(pos -> pos.pos().toShortString()).orElse("-");
        String state = potential + " | " + job;

        if (state.equals(data.getString(TRACE_KEY))) return;
        data.putString(TRACE_KEY, state);

        LOGGER.info("[warrior-trace] {} проф={} potential={} job={}",
                villager.getStringUUID().substring(0, 8),
                villager.getVillagerData().getProfession().name(), potential, job);
    }

    /**
     * Чинит счётчик свободных билетов у стропов.
     * <p>
     * Ванильный PoiCompetitorScan стирает память JOB_SITE у проигравшего, но билет обратно
     * НЕ возвращает - releaseTicket вызывается только из Villager.releaseAllPois и ValidateNearbyPoi.
     * В ванили это не всплывает: при maxTickets=1 второй житель билет вообще не получит, и
     * конкурировать некому. У нас maxTickets=3, поэтому трое разбирали билеты, двоих вышибала
     * конкуренция, и билеты сгорали навсегда. Строп доходил до нуля свободных, после чего
     * AcquirePoi с Occupancy.HAS_SPACE переставал его видеть - и профессию не мог взять уже никто.
     * <p>
     * Поэтому раз в TICKET_REPAIR_PERIOD_TICKS сверяем счётчик с реальностью: сколько воинов
     * действительно держат этот строп в памяти JOB_SITE, столько билетов и должно быть занято.
     * Недостающие возвращаем через PoiManager.release.
     * <p>
     * Проверка разнесена по жителям через их entity id, чтобы не сканировать всех в одном тике.
     */
    // PoiManager.getFreeTickets помечен @Deprecated @VisibleForDebug: ваниль не рассчитывает, что
    // счётчик читают из геймплейного кода. Замены нет - поле freeTickets приватное,
    // acquireTicket/releaseTicket защищённые, а публично доступны только булевы hasSpace/isOccupied.
    // Нам нужно именно ЧИСЛО: чинить счётчик надо ровно на столько билетов, на сколько он разошёлся
    // с реальностью. Поэтому подавляем предупреждение здесь, а не подгоняем логику под лог.
    @SuppressWarnings("deprecation")
    private static void repairStropTickets(Villager villager) {
        if (!WarriorVillagerConfig.SHARE_JOB_SITE) return;
        if (!(villager.level() instanceof ServerLevel level)) return;
        if ((level.getGameTime() + villager.getId()) % WarriorVillagerConfig.TICKET_REPAIR_PERIOD_TICKS != 0L) return;

        PoiManager poiManager = level.getPoiManager();
        List<BlockPos> strops = poiManager.getInRange(
                        holder -> holder.value() == ModVillagers.STROP_POI.value(),
                        villager.blockPosition(), WarriorVillagerConfig.TICKET_REPAIR_RADIUS,
                        PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .toList();

        for (BlockPos pos : strops) {
            int holders = countJobSiteHolders(level, pos);
            int expectedFree = Math.max(0, ModVillagers.STROP_MAX_WORKERS - holders);
            int before = poiManager.getFreeTickets(pos);
            if (before >= expectedFree) continue;

            boolean lastRelease = false;
            int guard = ModVillagers.STROP_MAX_WORKERS + 1;
            while (poiManager.getFreeTickets(pos) < expectedFree && guard-- > 0) {
                lastRelease = poiManager.release(pos);
                if (!lastRelease) break;
            }

            if (WarriorVillagerConfig.DEBUG_JOB_SITE) {
                int maxTickets = poiManager.getType(pos).map(holder -> holder.value().maxTickets()).orElse(-1);
                // Ровно та проверка, которой ValidateNearbyPoi решает, стирать ли память
                // безработного жителя (у профессии NONE предикат - тег acquirable_job_site).
                boolean validForUnemployed = poiManager.exists(pos, VillagerProfession.NONE.acquirableJobSite());
                boolean inTag = poiManager.getType(pos)
                        .map(holder -> holder.is(PoiTypeTags.ACQUIRABLE_JOB_SITE)).orElse(false);
                LOGGER.info("[warrior-repair] строп {} было={} стало={} держат={} нужно={} maxTickets={} release={} валиден={} вТеге={}",
                        pos.toShortString(), before, poiManager.getFreeTickets(pos),
                        holders, expectedFree, maxTickets, lastRelease, validForUnemployed, inTag);
            }
        }
    }

    /** Сколько жителей рядом реально держат этот строп как рабочее место. */
    private static int countJobSiteHolders(ServerLevel level, BlockPos pos) {
        GlobalPos jobSite = GlobalPos.of(level.dimension(), pos);
        AABB box = new AABB(pos).inflate(WarriorVillagerConfig.TICKET_HOLDER_RADIUS);

        int holders = 0;
        for (Villager nearby : level.getEntitiesOfClass(Villager.class, box)) {
            if (nearby.getBrain().getMemory(MemoryModuleType.JOB_SITE).filter(jobSite::equals).isPresent()) {
                holders++;
            }
        }
        return holders;
    }

    /**
     * Временная диагностика проблемы "строп берёт только один житель".
     * <p>
     * Показывает, на каком шаге всё встаёт:
     * potential=- и job=- у всех, кроме одного -> жители даже не находят строп (билеты или путь);
     * potential=есть, job=- -> не доходит до присвоения профессии (нужно подойти на 2 блока);
     * job то появляется, то пропадает -> подтверждается PoiCompetitorScan.
     * Столбец strops показывает свободные билеты каждого стропа в радиусе 16.
     */
    // PoiRecord.getFreeTickets - тот же @VisibleForDebug, и здесь это ровно его назначение: строка
    // идёт только в диагностический лог под флагом DEBUG_JOB_SITE.
    @SuppressWarnings("deprecation")
    private static void debugJobSite(Villager villager) {
        if (!WarriorVillagerConfig.DEBUG_JOB_SITE) return;
        if (!(villager.level() instanceof ServerLevel level)) return;
        if (level.getGameTime() % WarriorVillagerConfig.DEBUG_JOB_SITE_PERIOD_TICKS != 0L) return;

        Brain<Villager> brain = villager.getBrain();
        String potential = brain.getMemory(MemoryModuleType.POTENTIAL_JOB_SITE)
                .map(pos -> pos.pos().toShortString()).orElse("-");
        String job = brain.getMemory(MemoryModuleType.JOB_SITE)
                .map(pos -> pos.pos().toShortString()).orElse("-");

        String strops = level.getPoiManager()
                .getInRange(holder -> holder.value() == ModVillagers.STROP_POI.value(),
                        villager.blockPosition(), 16, PoiManager.Occupancy.ANY)
                .map(record -> record.getPos().toShortString() + " билетов=" + record.getFreeTickets())
                .reduce((a, b) -> a + "; " + b)
                .orElse("нет рядом");

        LOGGER.info("[warrior-debug] {} проф={} ребёнок={} xp={} potential={} job={} | стропы: {}",
                villager.getStringUUID().substring(0, 8),
                villager.getVillagerData().getProfession().name(),
                villager.isBaby(),
                villager.getVillagerXp(),
                potential, job, strops);
    }

    /**
     * Житель перестал быть воином - снимаем всё, что навесил мод.
     * <p>
     * Реролла снаряжения это не даёт: розыгрыш детерминирован по UUID жителя, так что вернув
     * профессию, он получит ровно тот же комплект. Ломать и переставлять строп ради алмазного
     * набора бесполезно.
     */
    private static void demote(Villager villager) {
        if (!WarriorVillager.hasWarriorGear(villager)) return;

        WarriorVillager.stripGear(villager);
        villager.setTarget(null);

        Predicate<Goal> ours = goal -> goal instanceof WarriorVillagerTargetGoal
                || goal instanceof WarriorMeleeAttackGoal
                || goal instanceof WarriorBowAttackGoal;
        removeGoals(villager, ours);
        removeTargetGoals(villager, ours);

        CompoundTag data = villager.getPersistentData();
        data.remove(JOB_SITE_POS_KEY);
        data.remove(JOB_SITE_DIM_KEY);
    }

    /**
     * Возвращает воину рабочее место, отобранное ванильным PoiCompetitorScan.
     * <p>
     * Это поведение сидит в Activity.CORE и работает всегда: для каждого жителя с памятью JOB_SITE
     * оно ищет соседей, у которых JOB_SITE указывает на ТУ ЖЕ позицию и профессия подходит этому
     * POI, и стирает память у всех, кроме одного - победителя по villagerXp. То есть ваниль жёстко
     * держит одного работника на блок, и maxTickets у PoiType на это никак не влияет: билеты
     * раздаются всем троим, а потом конкуренция вышибает лишних.
     * <p>
     * Поэтому запоминаем строп в persistentData и возвращаем память обратно после мозга. Блок
     * проверяем: если стропа там больше нет - забываем, чтобы воин не цеплялся за пустоту.
     */
    private static void keepJobSite(Villager villager) {
        if (!WarriorVillagerConfig.SHARE_JOB_SITE) return;

        Brain<Villager> brain = villager.getBrain();
        CompoundTag data = villager.getPersistentData();
        Optional<GlobalPos> current = brain.getMemory(MemoryModuleType.JOB_SITE);

        if (current.isPresent()) {
            data.putLong(JOB_SITE_POS_KEY, current.get().pos().asLong());
            data.putString(JOB_SITE_DIM_KEY, current.get().dimension().location().toString());
            return;
        }

        if (!data.contains(JOB_SITE_POS_KEY)) return;

        ResourceLocation dimension = ResourceLocation.tryParse(data.getString(JOB_SITE_DIM_KEY));
        if (!villager.level().dimension().location().equals(dimension)) return;

        BlockPos pos = BlockPos.of(data.getLong(JOB_SITE_POS_KEY));
        // Чанк мог выгрузиться: getBlockState заставил бы его грузиться прямо в тике сущности.
        if (!villager.level().isLoaded(pos)) return;

        if (!villager.level().getBlockState(pos).is(ModBlocks.STROP.get())) {
            data.remove(JOB_SITE_POS_KEY);
            data.remove(JOB_SITE_DIM_KEY);
            return;
        }

        brain.setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(villager.level().dimension(), pos));
    }
    private static void suppressPanic(Villager villager) {
        if (!WarriorVillagerConfig.SUPPRESS_PANIC) return;

        Brain<Villager> brain = villager.getBrain();
        brain.eraseMemory(MemoryModuleType.NEAREST_HOSTILE);
        brain.eraseMemory(MemoryModuleType.HURT_BY);
        brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);

        if (!brain.isActive(Activity.PANIC)) return;

        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.PATH);
        if (villager.level() instanceof ServerLevel serverLevel) {
            brain.updateActivityFromSchedule(serverLevel.getDayTime(), serverLevel.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        Entity attacker = event.getSource().getEntity();

        // Воина ударил монстр - сбрасываем таймер "тишины" для пассивной регенерации узла BRAIN
        // (tickBrainHeal), независимо от того, есть ли у воина уже цель.
        if (victim instanceof Villager hitVillager
                && WarriorVillager.isWarrior(hitVillager)
                && attacker instanceof Mob hostileHitter
                && hostileHitter instanceof Enemy) {
            WarriorVillager.registerMonsterHit(hitVillager);
        }

        if (WarriorVillagerConfig.FRIENDLY_FIRE_IMMUNE
                && (victim instanceof Villager || victim instanceof IronGolem)
                && attacker instanceof Villager shooter
                && WarriorVillager.isWarrior(shooter)
                && shooter != victim
                // Исключение: по своему обидчику воин бьёт по-настоящему, иначе ответка
                // на голема была бы беззубой - он махал бы оружием без урона.
                && !isGrudgeTargetOf(shooter, victim)) {
            event.setCanceled(true);
            return;
        }

        // Воин реально бьёт (не отменено дружественным огнём выше) - кровотечение/поджог по узлам
        // дерева (WarriorUpgrades), независимо от того, какое оружие ему выпало. Зловещему воину
        // жители почти ничего не могут сделать (OminousWarriorVillagerBalance) - и это на него не действует.
        if (attacker instanceof Villager attackerVillager
                && WarriorVillager.isWarrior(attackerVillager)
                && attackerVillager != victim
                && !(victim instanceof OminousWarriorEntity)) {
            WarriorVillager.applyCombatUpgrades(attackerVillager, victim);
        }

        if (victim instanceof Villager warrior
                && WarriorVillager.isWarrior(warrior)
                && warrior.getTarget() == null
                && attacker instanceof Mob hostile
                && hostile instanceof Enemy) {
            warrior.setTarget(hostile);
            return;
        }

        // Игрок и голем - не Enemy, поэтому им отдельная ветка: сдачи дают не сразу,
        // а только после GRUDGE_HITS_TO_ANGER ударов подряд.
        if (victim instanceof Villager warrior
                && WarriorVillager.isWarrior(warrior)
                && isGrudgeWorthy(attacker)
                && WarriorVillager.registerGrudgeHit(warrior, (LivingEntity) attacker)) {
            warrior.setTarget((LivingEntity) attacker);
        }
    }

    private static boolean isGrudgeTargetOf(Villager warrior, LivingEntity victim) {
        return WarriorVillager.isHoldingGrudge(warrior)
                && WarriorVillager.resolveGrudgeTarget(warrior) == victim;
    }

    /** Обидчики, которых воин запоминает: игрок и, если включено, железный голем. */
    private static boolean isGrudgeWorthy(@Nullable Entity attacker) {
        if (attacker instanceof Player player) {
            return !player.isSpectator() && !player.isCreative();
        }
        return WarriorVillagerConfig.GRUDGE_INCLUDE_GOLEMS && attacker instanceof IronGolem;
    }

    /**
     * Держит обидчика целью, пока обида не остынет.
     * <p>
     * Без этого воин ударил бы один раз и забыл: WarriorVillagerTargetGoal в stop() чистит цель,
     * а его canUse() ищет только Enemy, среди которых ни игрока, ни голема нет. Поэтому цель
     * переставляем сами, пока идёт GRUDGE_ANGER_TICKS.
     */
    private static void keepGrudgeTarget(Villager villager) {
        if (!WarriorVillager.isHoldingGrudge(villager)) {
            // Обида остыла - отпускаем цель, но только если воин злился именно на неё.
            LivingEntity stale = WarriorVillager.resolveGrudgeTarget(villager);
            if (villager.getTarget() == stale) {
                villager.setTarget(null);
            }
            return;
        }

        LivingEntity offender = WarriorVillager.resolveGrudgeTarget(villager);

        // Враждебный моб рядом важнее старой обиды - его цель не перебиваем.
        LivingEntity current = villager.getTarget();
        if (current instanceof Enemy && current.isAlive()) return;

        double giveUp = WarriorVillagerConfig.TARGET_GIVE_UP_RADIUS;
        if (villager.distanceToSqr(offender) > giveUp * giveUp) return;

        if (current != offender) {
            villager.setTarget(offender);
        }
    }

    private static void installGoals(Villager villager) {
        if (hasGoals(villager.targetSelector.getAvailableGoals(), WarriorVillagerTargetGoal.class)) {
            villager.targetSelector.addGoal(1, new WarriorVillagerTargetGoal(villager));
        }

        if (WarriorVillager.usesBow(villager)) {
            removeGoals(villager, goal -> goal instanceof LongReachAttackGoal);
            if (hasGoals(villager.goalSelector.getAvailableGoals(), WarriorBowAttackGoal.class)) {
                villager.goalSelector.addGoal(2, new WarriorBowAttackGoal(villager));
            }
            return;
        }

        removeGoals(villager, goal -> goal instanceof LongReachAttackGoal && !(goal instanceof WarriorMeleeAttackGoal));

        if (hasGoals(villager.goalSelector.getAvailableGoals(), WarriorMeleeAttackGoal.class)) {
            villager.goalSelector.addGoal(2, new WarriorMeleeAttackGoal(villager));
        }
    }

    private static void removeGoals(Villager villager, Predicate<Goal> filter) {
        villager.goalSelector.getAvailableGoals().stream()
                .map(WrappedGoal::getGoal)
                .filter(filter)
                .toList()
                .forEach(villager.goalSelector::removeGoal);
    }

    private static void removeTargetGoals(Villager villager, Predicate<Goal> filter) {
        villager.targetSelector.getAvailableGoals().stream()
                .map(WrappedGoal::getGoal)
                .filter(filter)
                .toList()
                .forEach(villager.targetSelector::removeGoal);
    }

    private static boolean hasGoals(Set<WrappedGoal> goals, Class<? extends Goal> type) {
        return goals.stream().noneMatch(wrapped -> type.isInstance(wrapped.getGoal()));
    }
}