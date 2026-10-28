package net.gera.veridia.common.aibuff.villager.lumberjack;

import com.mojang.logging.LogUtils;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.types.BuildingMaterialsBlockEntity;
import net.gera.veridia.common.block.entity.types.FoodStorageBlockEntity;
import net.gera.veridia.common.compat.dynamictrees.DynamicTreesCompat;
import net.gera.veridia.common.village.calc.TreeFellRequests;
import net.gera.veridia.common.village.calc.VillageBlocks;
import net.gera.veridia.common.village.calc.VillageCalc;
import net.gera.veridia.common.villager.ModVillagers;
import net.gera.veridia.common.villager.storage.StorageKind;
import net.gera.veridia.common.villager.storage.VillagerEmployment;
import net.gera.veridia.common.villager.storage.VillagerStorageBinding;
import net.gera.veridia.debug.logging.LumberjackDebugConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.tags.ItemTags;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;

public class LumberjackChopGoal extends Goal {

    private static final Logger LOGGER = LogUtils.getLogger();

    private enum Phase {
        FIND_TREE, WALK_TO_TREE, CHOPPING, LEAF_CLEAR, COLLECT_DROPS,
        RETURN_TO_MATERIALS, DEPOSIT_MATERIALS,
        RETURN_TO_FOOD, DEPOSIT_FOOD,
        DONE
    }

    private final Villager villager;
    private final int breakerId;

    private Phase phase = Phase.DONE;

    private BlockPos targetLog;
    /** Основание дерева, которое лесоруб валит по заявке nailer'а (TreeFellRequests); null - обычная рубка. */
    @Nullable
    private BlockPos servedRequest;
    private final Deque<BlockPos> unreachableLogs = new ArrayDeque<>();
    private List<BlockPos> treeLogs;
    private List<BlockPos> treeLeaves;
    private boolean choppingDynamicTree;
    private int chopTicksLeft;
    private int chopTicksTotal;
    private int leafClearTicksLeft;
    private BlockPos collectAnchor;
    private int collectTicks;
    private int ticksSinceLastDropSeen;
    private final Set<ItemEntity> skippedDrops = new HashSet<>();
    private int treeRescanCooldown;
    private int walkTicks;
    private int stuckTicks;
    private Vec3 lastWalkPos;
    private double bestDistance = Double.MAX_VALUE;
    private int ticksWithoutProgress;
    private BlockPos clearingLeaf;
    private int clearTicksLeft;
    private int leafApproachTicks;

    private BlockPos cachedMaterialsPos;
    private int materialsRescanCooldown;
    private BlockPos cachedFoodPos;
    private int foodRescanCooldown;

    public LumberjackChopGoal(Villager villager) {
        this.villager = villager;
        this.breakerId = villager.getId();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        if (villager.isBaby()) return false;
        if (villager.getVillagerData().getProfession() != ModVillagers.LUMBERJACK.value()) return false;
        if (!VillagerEmployment.isWorkEnabled(villager)) return false;
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        if (villager.getBrain().isActive(Activity.PANIC)) return false;

        if (phase == Phase.DONE || phase == Phase.FIND_TREE) {
            return isWorkTime(serverLevel);
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (villager.getVillagerData().getProfession() != ModVillagers.LUMBERJACK.value()) return false;
        if (!VillagerEmployment.isWorkEnabled(villager)) return false;
        return phase != Phase.DONE && !villager.getBrain().isActive(Activity.PANIC);
    }

    @Override
    public void start() {
        if (phase == Phase.DONE) {
            phase = Phase.FIND_TREE;
            unreachableLogs.clear();
        }
        resetWalkState();
        villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        villager.setPathfindingMalus(PathType.LEAVES, LumberjackVillagerConfig.LEAF_PATH_MALUS);
        if (LumberjackDebugConfig.DEBUG_CHOP_GOAL) {
            LOGGER.info("[lumberjack-debug] {} стартовал Goal, phase={}, топор в руке={}",
                    villager.getStringUUID().substring(0, 8), phase,
                    !villager.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty());
        }
    }

    @Override
    public void stop() {
        if (villager.level() instanceof ServerLevel serverLevel) {
            BlockPos crack = currentChopPos();
            if (crack != null) serverLevel.destroyBlockProgress(breakerId, crack, -1);
            if (clearingLeaf != null) serverLevel.destroyBlockProgress(breakerId, clearingLeaf, -1);
        }
        clearingLeaf = null;
        villager.getNavigation().stop();
        villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        if (LumberjackDebugConfig.DEBUG_CHOP_GOAL) {
            LOGGER.info("[lumberjack-debug] {} остановил Goal (снял топор), phase={}, паника={}",
                    villager.getStringUUID().substring(0, 8), phase, villager.getBrain().isActive(Activity.PANIC));
        }
    }

    @Override
    public void tick() {
        if (!(villager.level() instanceof ServerLevel serverLevel)) return;
        keepAxeEquipped();
        touchRequest(serverLevel);

        if ((phase == Phase.FIND_TREE || phase == Phase.WALK_TO_TREE) && shouldHeadHome(serverLevel)) {
            beginReturnTrip();
            return;
        }

        switch (phase) {
            case FIND_TREE -> tickFindTree(serverLevel);
            case WALK_TO_TREE -> tickWalkToTree(serverLevel);
            case CHOPPING -> tickChopping(serverLevel);
            case LEAF_CLEAR -> tickLeafClear(serverLevel);
            case COLLECT_DROPS -> tickCollectDrops(serverLevel);
            case RETURN_TO_MATERIALS -> tickReturnToMaterials(serverLevel);
            case DEPOSIT_MATERIALS -> tickDepositMaterials(serverLevel);
            case RETURN_TO_FOOD -> tickReturnToFood(serverLevel);
            case DEPOSIT_FOOD -> tickDepositFood(serverLevel);
            case DONE -> {
            }
        }
    }

    /** Короткое описание текущей фазы - для кнопки "?" в панели склада (WorkerPanelActions). */
    public Component statusText() {
        return switch (phase) {
            case FIND_TREE, WALK_TO_TREE -> Component.translatable("gui.veridia.worker_panel.status.lumberjack.to_tree");
            case CHOPPING -> Component.translatable("gui.veridia.worker_panel.status.lumberjack.chopping");
            case LEAF_CLEAR -> Component.translatable("gui.veridia.worker_panel.status.lumberjack.leaf_clear");
            case COLLECT_DROPS -> Component.translatable("gui.veridia.worker_panel.status.lumberjack.collecting");
            case RETURN_TO_MATERIALS, RETURN_TO_FOOD -> Component.translatable("gui.veridia.worker_panel.status.lumberjack.returning");
            case DEPOSIT_MATERIALS, DEPOSIT_FOOD -> Component.translatable("gui.veridia.worker_panel.status.lumberjack.depositing");
            case DONE -> Component.translatable("gui.veridia.worker_panel.status.idle");
        };
    }

    private void keepAxeEquipped() {
        if (!villager.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_AXE)) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    @Nullable
    private BlockPos currentChopPos() {
        return targetLog;
    }

    private boolean isWorkTime(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000L;
        return dayTime >= LumberjackUpgrades.workStartTick(villager) && dayTime < LumberjackUpgrades.workEndTick(villager);
    }

    private boolean shouldHeadHome(ServerLevel level) {
        return !isWorkTime(level) || isInventoryFull() || hasEnoughLogs();
    }

    private boolean isInventoryFull() {
        SimpleContainer inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) return false;
        }
        return true;
    }

    private boolean hasEnoughLogs() {
        return countCarriedLogs() >= LumberjackVillagerConfig.MAX_CARRIED_LOGS;
    }

    private int countCarriedLogs() {
        SimpleContainer inventory = villager.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ItemTags.LOGS)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private void beginReturnTrip() {
        if (villager.level() instanceof ServerLevel serverLevel) {
            BlockPos crack = currentChopPos();
            if (crack != null) serverLevel.destroyBlockProgress(breakerId, crack, -1);
        }
        targetLog = null;
        treeLogs = null;
        treeLeaves = null;
        collectAnchor = null;
        resetWalkState();
        boolean hasMaterials = hasCategory(LumberjackVillagerConfig::isBuildingMaterial);
        boolean hasFood = hasCategory(LumberjackVillagerConfig::isFoodStorageItem);
        phase = hasMaterials ? Phase.RETURN_TO_MATERIALS
                : hasFood ? Phase.RETURN_TO_FOOD
                : Phase.DONE;
        if (LumberjackDebugConfig.DEBUG_CHOP_GOAL) {
            LOGGER.info("[lumberjack-debug] {} идёт домой: hasMaterials={} hasFood={} -> phase={}",
                    villager.getStringUUID().substring(0, 8), hasMaterials, hasFood, phase);
        }
    }

    private boolean hasCategory(Predicate<ItemStack> predicate) {
        SimpleContainer inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (predicate.test(inventory.getItem(i))) return true;
        }
        return false;
    }


    private void tickFindTree(ServerLevel level) {
        if (treeRescanCooldown > 0) {
            treeRescanCooldown--;
            return;
        }
        treeRescanCooldown = LumberjackVillagerConfig.TREE_RESCAN_TICKS;

        if (claimRequestedTree(level)) {
            return;
        }
        BlockPos found = findNearestLog(level);
        if (LumberjackDebugConfig.DEBUG_CHOP_GOAL) {
            LOGGER.info("[lumberjack-debug] {} ищет дерево от {} (радиус {}, вверх {} вниз {} мин.Y {}): {}",
                    villager.getStringUUID().substring(0, 8), anchorPos().toShortString(),
                    LumberjackVillagerConfig.TREE_SEARCH_RADIUS, LumberjackVillagerConfig.TREE_SEARCH_UP,
                    LumberjackVillagerConfig.TREE_SEARCH_DOWN, LumberjackVillagerConfig.TREE_SEARCH_MIN_Y,
                    found != null ? found.toShortString() : "не найдено");
        }
        if (found == null) return;

        targetLog = found;
        phase = Phase.WALK_TO_TREE;
    }

    // --- заявки nailer'а на деревья, мешающие стене (TreeFellRequests) ---

    /**
     * Берёт ближайшую заявку на дерево: она важнее обычной рубки. Фильтры обычного поиска (дерево внутри структуры
     * деревни, рядом другой житель, ствол тоньше DYNAMIC_TREE_MIN_RADIUS) для заявки не действуют: nailer стоит у
     * этого дерева, оно деревне как раз мешает.
     */
    private boolean claimRequestedTree(ServerLevel level) {
        BlockPos base = TreeFellRequests.claimNearest(level, villager);
        if (base == null) {
            return false;
        }
        servedRequest = base;
        targetLog = DynamicTreesCompat.isSoil(level.getBlockState(base)) ? base.above() : base;
        phase = Phase.WALK_TO_TREE;
        return true;
    }

    private void touchRequest(ServerLevel level) {
        if (servedRequest != null && villager.tickCount % 20 == 0) {
            TreeFellRequests.touch(level, servedRequest, villager.getUUID());
        }
    }

    /** felled = true: дерево свалено (или его уже нет) - заявка выполнена; false - лесоруб не справился, срубит nailer. */
    private void closeRequest(ServerLevel level, boolean felled) {
        if (servedRequest == null) {
            return;
        }
        if (felled) {
            TreeFellRequests.finish(level, servedRequest);
        } else {
            TreeFellRequests.fail(level, servedRequest);
        }
        servedRequest = null;
    }

    private void logRequestFailure(ServerLevel level, String reason) {
        if (servedRequest != null) {
            net.gera.veridia.common.village.calc.NailerQueueAudit.log("лесоруб {} по заявке у {}: {} (стоит в {}, ходит {} тиков, без движения {}, без прогресса {})",
                    villager.getStringUUID().substring(0, 8), servedRequest.toShortString(), reason,
                    villager.blockPosition().toShortString(), walkTicks, stuckTicks, ticksWithoutProgress);
        }
    }

    private void tickWalkToTree(ServerLevel level) {
        if (targetLog == null || !LumberjackVillagerConfig.isChoppableLog(level.getBlockState(targetLog))
                || (servedRequest == null && VillageCalc.isInVillageStructure(level, targetLog))) {
            targetLog = null;
            closeRequest(level, true);
            phase = Phase.FIND_TREE;
            return;
        }
        if (!walkTowards(level, targetLog, effectiveSpeed(level, LumberjackVillagerConfig.CHOP_MOVE_SPEED), LumberjackVillagerConfig.CHOP_REACH)) {
            if (walkStuck()) {
                if (LumberjackDebugConfig.DEBUG_CHOP_GOAL) {
                    LOGGER.info("[lumberjack-debug] {} не дошёл до {} (walkTicks={} stuckTicks={}), помечаю недостижимым",
                            villager.getStringUUID().substring(0, 8), targetLog.toShortString(), walkTicks, stuckTicks);
                }
                markUnreachable(targetLog);
                targetLog = null;
                logRequestFailure(level, "не дошёл до дерева");
                closeRequest(level, false);
                resetWalkState();
                phase = Phase.FIND_TREE;
            }
            return;
        }
        // Пока шёл, рядом могли достроить дом или сменить ствол: полный разбор (обход брёвен) только здесь, не покадрово.
        if (servedRequest == null && !isWildTree(level, targetLog)) {
            targetLog = null;
            resetWalkState();
            phase = Phase.FIND_TREE;
            return;
        }
        if (LumberjackVillagerConfig.isDynamicTreesBranch(level.getBlockState(targetLog))) {
            choppingDynamicTree = true;
            targetLog = findDynamicTreeBase(level, targetLog);
            treeLogs = null;
            treeLeaves = null;
            chopTicksTotal = dynamicTreeChopTicks(level, targetLog);
            if (LumberjackDebugConfig.DEBUG_CHOP_GOAL) {
                LOGGER.info("[lumberjack-debug] {} нашёл DT-дерево, комель {}, рубить {} сек",
                        villager.getStringUUID().substring(0, 8), targetLog.toShortString(), chopTicksTotal / 20);
            }
        } else {
            choppingDynamicTree = false;
            TreeScan scan = scanTree(level, targetLog);
            treeLogs = scan.logs();
            treeLeaves = scan.leaves();
            chopTicksTotal = treeLogs.size() * LumberjackVillagerConfig.CHOP_TICKS_PER_LOG;
            if (LumberjackDebugConfig.DEBUG_CHOP_GOAL) {
                LOGGER.info("[lumberjack-debug] {} нашёл дерево: {} бревен, {} листвы, рубить {} сек",
                        villager.getStringUUID().substring(0, 8), treeLogs.size(), treeLeaves.size(),
                        treeLogs.size() * LumberjackVillagerConfig.CHOP_TICKS_PER_LOG / 20);
            }
        }
        // Апгрейд axe (LumberjackUpgrades) - ускоряет рубку, применяется к любому дереву целиком.
        chopTicksTotal = Math.max(1, Math.round(chopTicksTotal / LumberjackUpgrades.digSpeedMultiplier(villager)));
        chopTicksLeft = chopTicksTotal;
        phase = Phase.CHOPPING;
    }
    private BlockPos findDynamicTreeBase(ServerLevel level, BlockPos pos) {
        BlockPos current = pos;
        while (LumberjackVillagerConfig.isDynamicTreesBranch(level.getBlockState(current.below()))) {
            current = current.below();
        }
        return current;
    }
    private int dynamicTreeChopTicks(ServerLevel level, BlockPos pos) {
        float hardness = level.getBlockState(pos).getDestroySpeed(level, pos);
        float playerTicks = hardness * 30f / LumberjackVillagerConfig.IRON_AXE_EFFICIENCY;
        int ticks = Math.round(playerTicks * LumberjackVillagerConfig.DYNAMIC_TREE_SLOWDOWN);
        return Math.max(LumberjackVillagerConfig.DYNAMIC_TREE_MIN_CHOP_TICKS, ticks);
    }

    private record TreeScan(List<BlockPos> logs, List<BlockPos> leaves) {}

    private TreeScan scanTree(ServerLevel level, BlockPos start) {
        List<BlockPos> logs = NaturalTreeCheck.touchingLogs(level, start);
        List<BlockPos> leaves = findLeavesAround(level, logs);
        return new TreeScan(logs, leaves);
    }

    private List<BlockPos> findLeavesAround(ServerLevel level, List<BlockPos> logs) {
        Set<BlockPos> leaves = new HashSet<>();
        int r = LumberjackVillagerConfig.LEAF_SEARCH_RADIUS;
        for (BlockPos log : logs) {
            for (BlockPos pos : BlockPos.betweenClosed(log.offset(-r, -r, -r), log.offset(r, r, r))) {
                if (level.isLoaded(pos) && LumberjackVillagerConfig.isTreeLeaves(level.getBlockState(pos))) {
                    leaves.add(pos.immutable());
                }
            }
        }
        return new ArrayList<>(leaves);
    }

    private void tickChopping(ServerLevel level) {
        if (targetLog == null
                || (!choppingDynamicTree && (treeLogs == null || treeLogs.isEmpty()))
                || !LumberjackVillagerConfig.isChoppableLog(level.getBlockState(targetLog))) {
            targetLog = null;
            treeLogs = null;
            closeRequest(level, true);
            phase = Phase.FIND_TREE;
            return;
        }

        if (villager.distanceToSqr(Vec3.atCenterOf(targetLog)) > LumberjackVillagerConfig.CHOP_REACH * LumberjackVillagerConfig.CHOP_REACH) {
            walkTowards(level, targetLog, effectiveSpeed(level, LumberjackVillagerConfig.CHOP_MOVE_SPEED), LumberjackVillagerConfig.CHOP_REACH);
            if (walkStuck()) {
                // Оттолкнули от ствола, и вернуться не выходит - без этой проверки житель ходил бы тут вечно.
                level.destroyBlockProgress(breakerId, targetLog, -1);
                markUnreachable(targetLog);
                targetLog = null;
                treeLogs = null;
                logRequestFailure(level, "оттолкнули от ствола");
                closeRequest(level, false);
                resetWalkState();
                phase = Phase.FIND_TREE;
            }
            return;
        }

        holdPosition();
        lookAt(targetLog);

        if (villager.tickCount % LumberjackVillagerConfig.CHOP_HIT_PERIOD_TICKS == 0) {
            villager.swing(InteractionHand.MAIN_HAND);
            BlockState state = level.getBlockState(targetLog);
            level.playSound(null, targetLog, state.getSoundType(level, targetLog, villager).getHitSound(),
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
        }

        chopTicksLeft--;
        int stage = 9 - (int) Math.round(9.0 * chopTicksLeft / (double) chopTicksTotal);
        level.destroyBlockProgress(breakerId, targetLog, Math.max(0, Math.min(9, stage)));

        if (chopTicksLeft > 0) return;

        level.destroyBlockProgress(breakerId, targetLog, -1);
        if (choppingDynamicTree) {
            DynamicTreesCompat.fellTree(level, targetLog, villager);
            treeLeaves = null;
            beginCollectDrops();
        } else {
            for (BlockPos logPos : treeLogs) {
                if (LumberjackVillagerConfig.isChoppableLog(level.getBlockState(logPos))) {
                    breakLogAndCollect(level, logPos);
                }
            }
            treeLogs = null;
            beginLeafClearing();
        }
    }

    private void beginLeafClearing() {
        if (treeLeaves == null || treeLeaves.isEmpty()) {
            beginCollectDrops();
            return;
        }
        leafClearTicksLeft = LumberjackVillagerConfig.TREE_LEAF_CLEAR_TICKS;
        phase = Phase.LEAF_CLEAR;
    }

    private void tickLeafClear(ServerLevel level) {
        holdPosition();
        if (targetLog != null) lookAt(targetLog);
        if (villager.tickCount % LumberjackVillagerConfig.CHOP_HIT_PERIOD_TICKS == 0) {
            villager.swing(InteractionHand.MAIN_HAND);
        }

        leafClearTicksLeft--;
        if (leafClearTicksLeft > 0) return;

        if (treeLeaves != null) {
            for (BlockPos leafPos : treeLeaves) {
                if (LumberjackVillagerConfig.isTreeLeaves(level.getBlockState(leafPos))) {
                    level.destroyBlock(leafPos, true, villager);
                }
            }
        }
        treeLeaves = null;
        beginCollectDrops();
    }

    private void beginCollectDrops() {
        collectAnchor = targetLog;
        collectTicks = 0;
        ticksSinceLastDropSeen = 0;
        skippedDrops.clear();
        phase = Phase.COLLECT_DROPS;
    }

    private void tickCollectDrops(ServerLevel level) {
        magnetizeNearbyDrops(level);
        collectTicks++;
        if (isInventoryFull() || collectTicks > LumberjackVillagerConfig.COLLECT_TIMEOUT_TICKS) {
            finishTree(level);
            return;
        }

        ItemEntity nearest = findNearestDrop(level);
        if (nearest == null) {
            // DT-дерево валится не мгновенно (BranchBlock.futureBreak - отложенный, поэтапный
            // слом), новые дропы могут появляться пачками с паузами. Раньше ждали фиксированные
            // DYNAMIC_TREE_FALL_WAIT_TICKS от НАЧАЛА сбора - если дерево крупное и падает дольше,
            // жнец уходил, не дождавшись основного ствола. Теперь считаем от последнего РЕАЛЬНО
            // найденного дропа - таймер сбрасывается, пока что-то продолжает падать.
            ticksSinceLastDropSeen++;
            if (choppingDynamicTree && ticksSinceLastDropSeen < LumberjackVillagerConfig.DYNAMIC_TREE_FALL_WAIT_TICKS) {
                holdPosition();
                return;
            }
            finishTree(level);
            return;
        }
        ticksSinceLastDropSeen = 0;

        if (walkTowards(level, nearest.blockPosition(),
                effectiveSpeed(level, LumberjackVillagerConfig.COLLECT_MOVE_SPEED), LumberjackVillagerConfig.COLLECT_REACH)) {
            pickUp(nearest);
        } else if (walkStuck()) {
            skippedDrops.add(nearest);
            resetWalkState();
        }
    }
    private void magnetizeNearbyDrops(ServerLevel level) {
        AABB box = new AABB(villager.blockPosition()).inflate(LumberjackVillagerConfig.DROP_MAGNET_RADIUS);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box,
                entity -> entity.isAlive() && isWantedDrop(entity.getItem()))) {
            Vec3 toVillager = villager.position().subtract(item.position());
            double distSqr = toVillager.lengthSqr();
            if (distSqr < 1.0E-4) continue;
            Vec3 pull = toVillager.normalize().scale(LumberjackVillagerConfig.DROP_MAGNET_SPEED);
            item.setDeltaMovement(item.getDeltaMovement().add(pull));
        }
    }

    @Nullable
    private ItemEntity findNearestDrop(ServerLevel level) {
        if (collectAnchor == null) return null;
        AABB box = new AABB(villager.blockPosition()).inflate(LumberjackVillagerConfig.COLLECT_RADIUS);
        return level.getEntitiesOfClass(ItemEntity.class, box,
                        entity -> entity.isAlive() && isWantedDrop(entity.getItem()) && !skippedDrops.contains(entity))
                .stream()
                .min(Comparator.comparingDouble(villager::distanceToSqr))
                .orElse(null);
    }

    private static boolean isWantedDrop(ItemStack stack) {
        return LumberjackVillagerConfig.isBuildingMaterial(stack) || LumberjackVillagerConfig.isFoodStorageItem(stack);
    }

    private void pickUp(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        addToInventory(stack.copy());
        villager.take(itemEntity, stack.getCount());
        itemEntity.discard();
    }

    private void finishTree(ServerLevel level) {
        // Заявка закрыта удачно, только если ствол на месте цели уже не стоит (дерево реально свалено).
        closeRequest(level, targetLog == null || !LumberjackVillagerConfig.isChoppableLog(level.getBlockState(targetLog)));
        targetLog = null;
        collectAnchor = null;
        resetWalkState();
        beginNextStepAfterChop(level);
    }

    private void holdPosition() {
        villager.getNavigation().stop();
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.PATH);
    }

    private void beginNextStepAfterChop(ServerLevel level) {
        if (shouldHeadHome(level)) {
            beginReturnTrip();
        } else {
            phase = Phase.FIND_TREE;
        }
    }

    /**
     * BlockPos.withinManhattan отдаёт позиции РАСШИРЯЮЩИМИСЯ кольцами - от ближней к дальней (см.
     * его ванильную реализацию) - поэтому первая же полностью подходящая позиция уже "достаточно
     * ближняя" цель, и можно вернуть её сразу, не досканировая весь куб до конца. При радиусе 128
     * полный BlockPos.betweenClosed-перебор (как было раньше) - это ~1.4М блоков на каждый скан
     * каждые TREE_RESCAN_TICKS, даже когда ближайшее дерево буквально за углом.
     * <p>
     * Вертикальный диапазон - асимметричный относительно текущего Y жителя (TREE_SEARCH_UP/DOWN),
     * с абсолютным полом TREE_SEARCH_MIN_Y - не спускаемся в пещеры под жителем, даже если он
     * почему-то оказался низко.
     */
    @Nullable
    private BlockPos findNearestLog(ServerLevel level) {
        BlockPos origin = anchorPos();
        int rh = LumberjackVillagerConfig.TREE_SEARCH_RADIUS;
        int up = LumberjackVillagerConfig.TREE_SEARCH_UP;
        int down = LumberjackVillagerConfig.TREE_SEARCH_DOWN;
        int minY = Math.max(origin.getY() - down, LumberjackVillagerConfig.TREE_SEARCH_MIN_Y);
        int maxY = origin.getY() + up;
        int rv = Math.max(up, down);
        boolean debug = LumberjackDebugConfig.DEBUG_CHOP_GOAL;

        int seenLogBlocks = 0, notBase = 0, unreachable = 0, claimed = 0, notWild = 0, inVillage = 0;

        for (BlockPos pos : BlockPos.withinManhattan(origin, rh, rv, rh)) {
            if (pos.getY() < minY || pos.getY() > maxY) continue;
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!LumberjackVillagerConfig.isChoppableLog(state) || state.is(ModBlocks.LUMBERJACK_LOG.get())) continue;
            seenLogBlocks++;
            if (unreachableLogs.contains(pos)) { unreachable++; continue; }
            BlockState below = level.getBlockState(pos.below());
            if (LumberjackVillagerConfig.isDynamicTreesBranch(state)) {
                if (!DynamicTreesCompat.isSoil(below)) { notBase++; continue; }
            } else if (LumberjackVillagerConfig.isChoppableLog(below)) {
                notBase++;
                continue;
            }
            if (isClaimedByOtherVillager(level, pos)) { claimed++; continue; }
            if (VillageCalc.isInVillageStructure(level, pos)) { inVillage++; continue; }
            if (!isWildTree(level, pos)) { notWild++; continue; }

            BlockPos found = pos.immutable();
            if (debug) {
                LOGGER.info("[lumberjack-debug] {} нашёл {} (логов_видел={} не_комель={} недостижимо={} занято={} не_дикое={} внутри_деревни={})",
                        villager.getStringUUID().substring(0, 8), found.toShortString(),
                        seenLogBlocks, notBase, unreachable, claimed, notWild, inVillage);
            }
            return found;
        }
        if (debug) {
            LOGGER.info("[lumberjack-debug] {} разбор поиска: логов_видел={} не_комель={} недостижимо={} занято={} не_дикое={} внутри_деревни={}",
                    villager.getStringUUID().substring(0, 8), seenLogBlocks, notBase, unreachable, claimed, notWild, inVillage);
        }
        return null;
    }

    /**
     * Дерево, которое можно рубить: ствол Dynamic Trees достаточной толщины или настоящее ванильное дерево.
     * Бревно дома, стены или башни деревом не считается (см. NaturalTreeCheck).
     */
    private boolean isWildTree(ServerLevel level, BlockPos basePos) {
        BlockState state = level.getBlockState(basePos);
        if (LumberjackVillagerConfig.isDynamicTreesBranch(state)) {
            return DynamicTreesCompat.branchRadius(state) >= LumberjackVillagerConfig.DYNAMIC_TREE_MIN_RADIUS;
        }
        // Бревно на доске, камне или другом "полу" постройки - уже не дерево, и обход брёвен для него не нужен.
        if (VillageBlocks.isNaturalFloor(level.getBlockState(basePos.below()))) {
            return false;
        }
        return NaturalTreeCheck.isNaturalTree(level, basePos);
    }

    private boolean isClaimedByOtherVillager(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(LumberjackVillagerConfig.TREE_CLAIM_RADIUS);
        return !level.getEntitiesOfClass(Villager.class, box, other -> other != villager).isEmpty();
    }

    private void breakLogAndCollect(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), villager, ItemStack.EMPTY);
        level.destroyBlock(pos, false, villager);
        // Апгрейд log (LumberjackUpgrades) - шанс на бонусный выход ресурсов с этого бревна.
        if (level.random.nextFloat() < LumberjackUpgrades.bonusResourceChance(villager)) {
            float multiplier = LumberjackUpgrades.bonusResourceMultiplier(villager);
            for (ItemStack drop : drops) {
                drop.setCount(Math.max(drop.getCount() + 1, Math.round(drop.getCount() * multiplier)));
            }
        }
        drops.forEach(this::addToInventory);
    }

    private void addToInventory(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack remainder = villager.getInventory().addItem(stack);
        if (!remainder.isEmpty()) {
            Block.popResource(villager.level(), villager.blockPosition(), remainder);
        }
    }


    private void tickReturnToMaterials(ServerLevel level) {
        BlockPos target = resolveMaterialsStorage(level);
        if (target == null) {
            phase = hasCategory(LumberjackVillagerConfig::isFoodStorageItem) ? Phase.RETURN_TO_FOOD : Phase.DONE;
            return;
        }
        cachedMaterialsPos = target;
        if (walkTowards(level, target, effectiveSpeed(level, LumberjackVillagerConfig.RETURN_MOVE_SPEED), LumberjackVillagerConfig.DEPOSIT_REACH)) {
            phase = Phase.DEPOSIT_MATERIALS;
        } else if (walkStuck()) {
            if (LumberjackDebugConfig.DEBUG_CHOP_GOAL) {
                LOGGER.info("[lumberjack-debug] {} не дошёл до building_materials {} (walkTicks={} stuckTicks={})",
                        villager.getStringUUID().substring(0, 8), target.toShortString(), walkTicks, stuckTicks);
            }
            cachedMaterialsPos = null;
            materialsRescanCooldown = 0;
            resetWalkState();
            phase = hasCategory(LumberjackVillagerConfig::isFoodStorageItem) ? Phase.RETURN_TO_FOOD : Phase.DONE;
        }
    }

    private void tickDepositMaterials(ServerLevel level) {
        if (cachedMaterialsPos != null
                && level.getBlockEntity(cachedMaterialsPos) instanceof BuildingMaterialsBlockEntity blockEntity) {
            depositMatching(blockEntity.itemHandler, LumberjackVillagerConfig::isBuildingMaterial);
        }
        phase = hasCategory(LumberjackVillagerConfig::isFoodStorageItem) ? Phase.RETURN_TO_FOOD : Phase.DONE;
    }

    private void tickReturnToFood(ServerLevel level) {
        BlockPos target = resolveFoodStorage(level);
        if (target == null) {
            phase = Phase.DONE;
            return;
        }
        cachedFoodPos = target;
        if (walkTowards(level, target, effectiveSpeed(level, LumberjackVillagerConfig.RETURN_MOVE_SPEED), LumberjackVillagerConfig.DEPOSIT_REACH)) {
            phase = Phase.DEPOSIT_FOOD;
        } else if (walkStuck()) {
            if (LumberjackDebugConfig.DEBUG_CHOP_GOAL) {
                LOGGER.info("[lumberjack-debug] {} не дошёл до food_storage {} (walkTicks={} stuckTicks={})",
                        villager.getStringUUID().substring(0, 8), target.toShortString(), walkTicks, stuckTicks);
            }
            cachedFoodPos = null;
            foodRescanCooldown = 0;
            resetWalkState();
            phase = Phase.DONE;
        }
    }

    private void tickDepositFood(ServerLevel level) {
        if (cachedFoodPos != null
                && level.getBlockEntity(cachedFoodPos) instanceof FoodStorageBlockEntity blockEntity) {
            depositMatching(blockEntity.itemHandler, LumberjackVillagerConfig::isFoodStorageItem);
        }
        phase = Phase.DONE;
    }

    private void depositMatching(ItemStackHandler handler, Predicate<ItemStack> predicate) {
        SimpleContainer inventory = villager.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !predicate.test(stack)) continue;
            inventory.setItem(i, insertInto(handler, stack));
        }
    }

    private static ItemStack insertInto(ItemStackHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
        }
        return remaining;
    }

    @Nullable
    private BlockPos resolveMaterialsStorage(ServerLevel level) {
        BlockPos cached = VillagerStorageBinding.resolveCached(villager, level, StorageKind.MATERIALS,
                ModBlocks.BUILDING_MATERIALS.get(), BuildingMaterialsBlockEntity.class);
        if (cached != null) return cached;
        if (materialsRescanCooldown > 0) {
            materialsRescanCooldown--;
            return null;
        }
        materialsRescanCooldown = LumberjackVillagerConfig.HOME_RESCAN_TICKS;
        return VillagerStorageBinding.claimNew(villager, level, StorageKind.MATERIALS,
                ModBlocks.BUILDING_MATERIALS.get(), BuildingMaterialsBlockEntity.class,
                anchorPos(), LumberjackVillagerConfig.HOME_SEARCH_RADIUS,
                LumberjackVillagerConfig.HOME_SEARCH_VERTICAL_RADIUS, null);
    }

    @Nullable
    private BlockPos resolveFoodStorage(ServerLevel level) {
        BlockPos cached = VillagerStorageBinding.resolveCached(villager, level, StorageKind.FOOD,
                ModBlocks.FOOD_STORAGE.get(), FoodStorageBlockEntity.class);
        if (cached != null) return cached;
        if (foodRescanCooldown > 0) {
            foodRescanCooldown--;
            return null;
        }
        foodRescanCooldown = LumberjackVillagerConfig.HOME_RESCAN_TICKS;
        return VillagerStorageBinding.claimNew(villager, level, StorageKind.FOOD,
                ModBlocks.FOOD_STORAGE.get(), FoodStorageBlockEntity.class,
                anchorPos(), LumberjackVillagerConfig.HOME_SEARCH_RADIUS,
                LumberjackVillagerConfig.HOME_SEARCH_VERTICAL_RADIUS, null);
    }

    private BlockPos anchorPos() {
        return villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                .filter(globalPos -> globalPos.dimension() == villager.level().dimension())
                .map(GlobalPos::pos)
                .orElseGet(villager::blockPosition);
    }

    /** Апгрейд speed (LumberjackUpgrades) - множитель ко всем "скоростям ходьбы" отсюда. */
    private double effectiveSpeed(ServerLevel level, double baseSpeed) {
        return baseSpeed * LumberjackUpgrades.walkSpeedMultiplier(villager);
    }

    private boolean walkTowards(ServerLevel level, BlockPos target, double speed, double reach) {
        if (clearingLeaf != null) {
            walkTicks++;
            tickClearLeaf(level);
            return false;
        }

        lookAt(target);

        double distanceSqr = villager.distanceToSqr(Vec3.atCenterOf(target));
        if (distanceSqr > reach * reach) {
            walkTicks++;
            trackProgress(Math.sqrt(distanceSqr));
            if (updateStuckTracking()) {
                BlockPos leaf = findBlockingLeaf(level, target);
                if (leaf != null) {
                    beginClearLeaf(leaf);
                    return false;
                }
            }
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(target, (float) speed, Math.max(1, (int) reach)));
            return false;
        }

        resetWalkState();
        holdPosition();
        return true;
    }
    @Deprecated(forRemoval = true)
    private boolean walkStuck() {
        return stuckTicks >= LumberjackVillagerConfig.GIVE_UP_STUCK_TICKS
                || ticksWithoutProgress >= LumberjackVillagerConfig.GIVE_UP_NO_PROGRESS_TICKS
                || walkTicks > LumberjackVillagerConfig.WALK_TIMEOUT_TICKS;
    }

    /** Ходит, но ближе к цели не становится (кружит) - считаем тики без нового лучшего расстояния. */
    private void trackProgress(double distance) {
        if (distance < bestDistance - LumberjackVillagerConfig.WALK_PROGRESS_MIN_GAIN) {
            bestDistance = distance;
            ticksWithoutProgress = 0;
        } else {
            ticksWithoutProgress++;
        }
    }

    private void resetWalkState() {
        walkTicks = 0;
        stuckTicks = 0;
        lastWalkPos = null;
        bestDistance = Double.MAX_VALUE;
        ticksWithoutProgress = 0;
        clearingLeaf = null;
        leafApproachTicks = 0;
    }
    private void markUnreachable(BlockPos pos) {
        unreachableLogs.remove(pos);
        unreachableLogs.addLast(pos);
        while (unreachableLogs.size() > LumberjackVillagerConfig.UNREACHABLE_LOG_MEMORY) {
            unreachableLogs.removeFirst();
        }
    }

    private boolean updateStuckTracking() {
        Vec3 pos = villager.position();
        if (lastWalkPos != null && pos.distanceToSqr(lastWalkPos) > LumberjackVillagerConfig.STUCK_MOVE_EPSILON_SQR) {
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }
        lastWalkPos = pos;
        return stuckTicks >= LumberjackVillagerConfig.CLEAR_LEAF_STUCK_TICKS;
    }

    @Nullable
    private BlockPos findBlockingLeaf(ServerLevel level, BlockPos target) {
        Vec3 from = villager.position();
        Vec3 flatDir = new Vec3(Vec3.atCenterOf(target).x - from.x, 0.0, Vec3.atCenterOf(target).z - from.z);
        if (flatDir.lengthSqr() < 1.0E-4) return null;
        flatDir = flatDir.normalize();

        for (double dist = 1.0; dist <= 2.0; dist += 1.0) {
            double x = from.x + flatDir.x * dist;
            double z = from.z + flatDir.z * dist;
            for (double dy : new double[] {0.1, 0.9, 1.6}) {
                BlockPos pos = BlockPos.containing(x, from.y + dy, z);
                if (LumberjackVillagerConfig.isTreeLeaves(level.getBlockState(pos))) {
                    return pos;
                }
            }
        }
        return null;
    }

    private void beginClearLeaf(BlockPos leafPos) {
        clearingLeaf = leafPos;
        clearTicksLeft = LumberjackVillagerConfig.CLEAR_LEAF_TICKS;
        leafApproachTicks = 0;
        stuckTicks = 0;
        lastWalkPos = null;
    }

    /** До листа не подойти - сдаёмся. stuckTicks выставляется так, чтобы walkStuck() стал true, и
     * вызвавшая фаза бросила цель как недостижимую (раньше тут был вечный цикл подхода к листу). */
    private void abandonLeaf(ServerLevel level) {
        level.destroyBlockProgress(breakerId, clearingLeaf, -1);
        clearingLeaf = null;
        leafApproachTicks = 0;
        stuckTicks = LumberjackVillagerConfig.GIVE_UP_STUCK_TICKS;
    }

    private void tickClearLeaf(ServerLevel level) {
        BlockState state = level.getBlockState(clearingLeaf);
        if (!LumberjackVillagerConfig.isTreeLeaves(state)) {
            clearingLeaf = null;
            return;
        }

        if (villager.distanceToSqr(Vec3.atCenterOf(clearingLeaf)) > LumberjackVillagerConfig.CHOP_REACH * LumberjackVillagerConfig.CHOP_REACH) {
            lookAt(clearingLeaf);
            if (++leafApproachTicks > LumberjackVillagerConfig.CLEAR_LEAF_APPROACH_TIMEOUT_TICKS) {
                abandonLeaf(level);
                return;
            }
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(clearingLeaf, (float) effectiveSpeed(level, LumberjackVillagerConfig.CHOP_MOVE_SPEED), 1));
            return;
        }
        leafApproachTicks = 0;

        lookAt(clearingLeaf);
        holdPosition();
        if (villager.tickCount % LumberjackVillagerConfig.CHOP_HIT_PERIOD_TICKS == 0) {
            villager.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, clearingLeaf, state.getSoundType(level, clearingLeaf, villager).getHitSound(),
                    SoundSource.NEUTRAL, 0.6F, 1.4F);
        }

        clearTicksLeft--;
        int stage = 9 - (int) Math.round(9.0 * clearTicksLeft / (double) LumberjackVillagerConfig.CLEAR_LEAF_TICKS);
        level.destroyBlockProgress(breakerId, clearingLeaf, Math.max(0, Math.min(9, stage)));

        if (clearTicksLeft > 0) return;

        level.destroyBlockProgress(breakerId, clearingLeaf, -1);
        level.destroyBlock(clearingLeaf, false, villager);
        clearingLeaf = null;
        ticksWithoutProgress = 0;
    }

    private void lookAt(BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        villager.getLookControl().setLookAt(center.x, center.y, center.z);
        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
    }
}