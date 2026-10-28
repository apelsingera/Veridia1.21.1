package net.gera.veridia.common.aibuff.handlers;

import com.mojang.logging.LogUtils;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.villager.lumberjack.LumberjackChopGoal;
import net.gera.veridia.debug.logging.LumberjackDebugConfig;
import net.gera.veridia.common.aibuff.villager.lumberjack.LumberjackVillagerConfig;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class LumberjackVillagerHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String JOB_SITE_POS_KEY = "veridia_lumberjack_jobsite_pos";
    private static final String JOB_SITE_DIM_KEY = "veridia_lumberjack_jobsite_dim";

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;

        debugProfession(villager);
        keepLumberjackProfession(villager);

        if (villager.getVillagerData().getProfession() != ModVillagers.LUMBERJACK.value()) return;

        keepJobSiteMemory(villager);
        rememberJobSite(villager);
        ensureFollowRange(villager);

        boolean hasChopGoal = villager.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof LumberjackChopGoal);
        if (!hasChopGoal) {
            villager.goalSelector.addGoal(2, new LumberjackChopGoal(villager));
            if (LumberjackDebugConfig.DEBUG_HANDLER) {
                LOGGER.info("[lumberjack-debug] {} получил LumberjackChopGoal",
                        villager.getStringUUID().substring(0, 8));
            }
        }
    }

    /**
     * ResetProfession/ValidateNearbyPoi вызывают refreshBrain(), даже если саму смену профессии
     * заблокировал VillagerProfessionLockMixin - а refreshBrain пересобирает мозг с нуля и стирает
     * JOB_SITE. Восстанавливаем память сразу, пока профессия ещё лесоруб, чтобы anchorPos() в
     * LumberjackChopGoal не съезжал на текущую позицию жителя.
     -------------------------------------------------------------------------------------------------
     * INTERACTION_TARGET запускает ShowTradesToPlayer (см. LumberjackChopGoal.keepAxeEquipped) -
     * то же поведение, из-за которого пропадал топор, каждый тик ещё и переставляет LOOK_TARGET
     * на игрока поверх нашего (см. LumberjackChopGoal.lookAt), поэтому житель "пялится" на игрока
     * вместо дерева. Стираем именно эту память - HURT_BY/NEAREST_HOSTILE не трогаем, паника при
     * реальной атаке отрабатывает как обычно (см. LumberjackChopGoal.canUse/canContinueToUse).
     */
    @SubscribeEvent
    public static void onTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;
        if (villager.getVillagerData().getProfession() != ModVillagers.LUMBERJACK.value()) return;

        villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
    }

    private static void keepJobSiteMemory(Villager villager) {
        if (villager.getBrain().hasMemoryValue(MemoryModuleType.JOB_SITE)) return;
        CompoundTag data = villager.getPersistentData();
        if (!data.contains(JOB_SITE_POS_KEY)) return;
        if (!(villager.level() instanceof ServerLevel level)) return;

        ResourceLocation dimension = ResourceLocation.tryParse(data.getString(JOB_SITE_DIM_KEY));
        if (!level.dimension().location().equals(dimension)) return;

        // Чанк может быть просто не загружен - не повод считать job site потерянным, см. keepLumberjackProfession.
        BlockPos pos = BlockPos.of(data.getLong(JOB_SITE_POS_KEY));
        if (level.isLoaded(pos) && !level.getBlockState(pos).is(ModBlocks.LUMBERJACK_LOG.get())) return;

        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), pos));
    }

    /**
     * TREE_SEARCH_RADIUS сам по себе не заставляет жителя ходить дальше - ванильный FOLLOW_RANGE
     * ограничивает ещё и дальность построения пути (PathNavigation), независимо от того, что
     * дерево уже найдено сканом. Поднимаем базовое значение, только если оно ниже нужного - как
     * WarriorVillager.applyLongReach с ENTITY_INTERACTION_RANGE.
     */
    private static void ensureFollowRange(Villager villager) {
        AttributeInstance followRange = villager.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null && followRange.getBaseValue() < LumberjackVillagerConfig.FOLLOW_RANGE) {
            followRange.setBaseValue(LumberjackVillagerConfig.FOLLOW_RANGE);
        }
    }

    /** Уволили лесоруба - незачем таскать на нём увеличенную дальность пути дальше. */
    private static void resetFollowRange(Villager villager) {
        AttributeInstance followRange = villager.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(followRange.getAttribute().value().getDefaultValue());
        }
    }

    private static void rememberJobSite(Villager villager) {
        villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).ifPresent(jobSite -> {
            CompoundTag data = villager.getPersistentData();
            data.putLong(JOB_SITE_POS_KEY, jobSite.pos().asLong());
            data.putString(JOB_SITE_DIM_KEY, jobSite.dimension().location().toString());
        });
    }

    private static void keepLumberjackProfession(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        if (!data.contains(JOB_SITE_POS_KEY)) return;
        if (!(villager.level() instanceof ServerLevel level)) return;

        ResourceLocation dimension = ResourceLocation.tryParse(data.getString(JOB_SITE_DIM_KEY));
        if (!level.dimension().location().equals(dimension)) return;

        // Чанк с job site может быть не загружен - тогда мы просто не можем проверить блок и доверяем
        // сохранённым данным. Увольняем только когда чанк загружен и мы точно видим, что блока
        // lumberjack_log там больше нет - это проверяем ПЕРВЫМ, независимо от текущей профессии,
        // чтобы не ждать, пока ванильная логика сама сообразит снять профессию (а до этого момента
        // LumberjackChopGoal продолжал бы рубить деревья с топором как ни в чём не бывало).
        BlockPos pos = BlockPos.of(data.getLong(JOB_SITE_POS_KEY));
        if (level.isLoaded(pos) && !level.getBlockState(pos).is(ModBlocks.LUMBERJACK_LOG.get())) {
            data.remove(JOB_SITE_POS_KEY);
            data.remove(JOB_SITE_DIM_KEY);
            if (villager.getVillagerData().getProfession() == ModVillagers.LUMBERJACK.value()) {
                villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.NONE));
                villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
                resetFollowRange(villager);
                if (LumberjackDebugConfig.DEBUG_HANDLER) {
                    LOGGER.info("[lumberjack-debug] {} - станок на {} пропал, уволил лесоруба",
                            villager.getStringUUID().substring(0, 8), pos.toShortString());
                }
            }
            return;
        }

        if (villager.getVillagerData().getProfession() == ModVillagers.LUMBERJACK.value()) return;

        VillagerData current = villager.getVillagerData();
        villager.setVillagerData(current.setProfession(ModVillagers.LUMBERJACK.value()));
        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), pos));

        if (LumberjackDebugConfig.DEBUG_HANDLER) {
            LOGGER.info("[lumberjack-debug] {} - профессия слетела, восстановил лесоруба на {}",
                    villager.getStringUUID().substring(0, 8), pos.toShortString());
        }
    }

    private static void debugProfession(Villager villager) {
        if (!LumberjackDebugConfig.DEBUG_HANDLER) return;
        if (!(villager.level() instanceof ServerLevel level)) return;
        if ((level.getGameTime() + villager.getId()) % LumberjackDebugConfig.DEBUG_PERIOD_TICKS != 0L) return;

        String profession = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession())
                + "";
        String jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                .map(globalPos -> globalPos.pos().toShortString()).orElse("-");
        long dayTime = level.getDayTime() % 24000L;
        boolean workWindow = dayTime >= LumberjackVillagerConfig.WORK_START_TICK
                && dayTime < LumberjackVillagerConfig.WORK_END_TICK;

        LOGGER.info("[lumberjack-debug] {} профессия={} jobSite={} dayTime={} рабочееОкно={}",
                villager.getStringUUID().substring(0, 8), profession, jobSite, dayTime, workWindow);
    }
}
