package net.gera.veridia.common.aibuff.villager.nailer;

import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.types.BuildingMaterialsBlockEntity;
import net.gera.veridia.common.particle.ModParticles;
import net.gera.veridia.common.village.calc.NailerQueueAudit;
import net.gera.veridia.common.village.calc.NailerTreeInWay;
import net.gera.veridia.common.village.calc.TreeFellRequests;
import net.gera.veridia.common.village.calc.NailerWallClaims;
import net.gera.veridia.common.village.calc.NailerWallQueue;
import net.gera.veridia.common.village.calc.VillageAutoPregen;
import net.gera.veridia.common.villager.ModVillagers;
import net.gera.veridia.common.villager.storage.StorageKind;
import net.gera.veridia.common.villager.storage.VillagerEmployment;
import net.gera.veridia.common.villager.storage.VillagerStorageBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NailerBuildGoal extends Goal {

    private enum Phase { FIND_TARGET, WALK_TO_STORAGE, WITHDRAW, WALK_TO_TARGET, BUILD, DONE }

    private final Villager villager;
    private Phase phase = Phase.DONE;

    @Nullable
    private List<NailerWallQueue.Piece> pieces;
    private int pieceIndex = -1;
    private int entryCursor;
    private int queueRecheckCooldown;
    private final Map<BlockPos, Long> unreachableEntries = new LinkedHashMap<>();

    private BlockPos cachedMaterialsPos;
    private int materialsRescanCooldown;
    private int emptyStorageWaitTicks;

    private NailerWallQueue.Entry currentTarget;
    private int buildTicksLeft;

    private int walkTicks;
    private int stuckTicks;
    private Vec3 lastWalkPos;

    public NailerBuildGoal(Villager villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        if (villager.isBaby()) return false;
        if (villager.getVillagerData().getProfession() != ModVillagers.NAILER.value()) return false;
        if (!VillagerEmployment.isWorkEnabled(villager)) return false;
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        if (villager.getBrain().isActive(Activity.PANIC)) return false;

        if (phase == Phase.DONE) {
            if (queueRecheckCooldown > 0) {
                queueRecheckCooldown--;
                return false;
            }
            return isWorkTime(serverLevel);
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (villager.getVillagerData().getProfession() != ModVillagers.NAILER.value()) return false;
        if (!VillagerEmployment.isWorkEnabled(villager)) return false;
        return phase != Phase.DONE && !villager.getBrain().isActive(Activity.PANIC);
    }

    @Override
    public void start() {
        if (phase == Phase.DONE) {
            phase = Phase.FIND_TARGET;
            unreachableEntries.clear();
        }
        resetWalkState();
    }

    @Override
    public void stop() {
        villager.getNavigation().stop();
        villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        if (villager.level() instanceof ServerLevel level) {
            releaseCurrentPiece(level);
        }
    }

    @Override
    public void tick() {
        if (!(villager.level() instanceof ServerLevel level)) return;

        if (phase != Phase.BUILD && !isWorkTime(level)) {
            logShiftEnded(level);
            phase = Phase.DONE;
            return;
        }

        switch (phase) {
            case FIND_TARGET -> tickFindTarget(level);
            case WALK_TO_STORAGE -> tickWalkToStorage(level);
            case WITHDRAW -> tickWithdraw(level);
            case WALK_TO_TARGET -> tickWalkToTarget(level);
            case BUILD -> tickBuild(level);
            case DONE -> {
            }
        }
    }

    private boolean isWorkTime(ServerLevel level) {
        // Wand-debug - dev-инструмент для обкатки планировки в любое время суток, а не только в
        // рабочие часы: без этого жезл включал бесконечный reach/очки/мгновенную стройку, но
        // villager всё равно просто стоял (canUse()/tick() глушат Goal вне WORK_START/END_TICK),
        // что выглядело как "debug ничего не делает".
        if (NailerDebugMode.isEnabled(villager)) return true;
        long dayTime = level.getDayTime() % 24000L;
        return dayTime >= NailerUpgrades.workStartTick(villager)
                && dayTime < NailerUpgrades.workEndTick(villager);
    }

    /** Короткое описание текущей фазы - для кнопки "?" в панели склада (WorkerPanelActions). */
    public Component statusText() {
        return switch (phase) {
            case FIND_TARGET -> Component.translatable("gui.veridia.worker_panel.status.nailer.find_target");
            case WALK_TO_STORAGE -> Component.translatable("gui.veridia.worker_panel.status.nailer.to_storage");
            case WITHDRAW -> Component.translatable("gui.veridia.worker_panel.status.nailer.withdraw");
            case WALK_TO_TARGET -> Component.translatable("gui.veridia.worker_panel.status.nailer.to_target");
            case BUILD -> Component.translatable("gui.veridia.worker_panel.status.nailer.building");
            case DONE -> Component.translatable("gui.veridia.worker_panel.status.idle");
        };
    }

    // --- выбор следующего блока стены ---

    private void tickFindTarget(ServerLevel level) {
        if (pieces == null) {
            pieces = resolveQueue(level);
        }
        if (pieces == null || pieces.isEmpty()) {
            giveUpForNow(level);
            return;
        }

        // Каждый кусок (wallpiece/entrance/столбик) достраивается целиком - см. NailerWallClaims,
        // прежде чем nailer возьмётся за следующий. Максимум pieces.size()+1 проходов - по разу на
        // каждый кусок (вдруг он уже готов или занят другим nailer'ом) плюс финальный выход.
        for (int attempts = 0; attempts <= pieces.size(); attempts++) {
            if (pieceIndex < 0 && !claimNextPiece(level)) {
                giveUpForNow(level);
                return;
            }

            List<NailerWallQueue.Entry> entries = pieces.get(pieceIndex).entries();
            while (entryCursor < entries.size() && canSkip(level, entries.get(entryCursor))) {
                entryCursor++;
            }

            if (entryCursor < entries.size()) {
                NailerWallClaims.claimOrRenew(level, pieces.get(pieceIndex).anchor(), villager.getUUID());
                currentTarget = entries.get(entryCursor);
                phase = currentTarget.pointCost() > 0 && countPoints() < currentTarget.pointCost()
                        ? Phase.WALK_TO_STORAGE : Phase.WALK_TO_TARGET;
                return;
            }

            // Этот кусок готов целиком - освобождаем и пробуем следующий незанятый в этом же тике.
            releaseCurrentPiece(level);
        }
        giveUpForNow(level);
    }

    private boolean claimNextPiece(ServerLevel level) {
        UUID id = villager.getUUID();
        for (int i = 0; i < pieces.size(); i++) {
            NailerWallQueue.Piece piece = pieces.get(i);
            if (isPieceComplete(level, piece)) continue;
            if (!NailerWallClaims.claimOrRenew(level, piece.anchor(), id)) continue;
            pieceIndex = i;
            entryCursor = 0;
            NailerQueueAudit.log("взял массу #{} из {}: {} блоков, якорь {}", i, pieces.size(), piece.entries().size(), piece.anchor().toShortString());
            return true;
        }
        return false;
    }

    /** Недостижимый блок считается "закрытым вопросом", а не недостроенным - иначе claimNextPiece
     * (скан всегда с индекса 0) снова и снова находил бы этот же кусок первым из-за одного
     * непоставимого блока, ни разу не добравшись до следующих кусков в очереди. */
    /**
     * Запись в этом проходе пропускается: блок уже стоит, недостижим, или на её месте дерево, которое валит лесоруб
     * (NailerTreeInWay: если лесоруб не поможет, дерево тут же рубится самим nailer'ом, и запись больше не пропускается).
     */
    private boolean canSkip(ServerLevel level, NailerWallQueue.Entry entry) {
        if (statesMatch(level.getBlockState(entry.pos()), entry.state()) || isUnreachable(level, entry.pos())) {
            return true;
        }
        return NailerTreeInWay.resolve(level, entry.pos()) == NailerTreeInWay.Outcome.WAITING;
    }

    private boolean isPieceComplete(ServerLevel level, NailerWallQueue.Piece piece) {
        for (NailerWallQueue.Entry entry : piece.entries()) {
            if (isUnreachable(level, entry.pos()) || TreeFellRequests.isWaitingAt(level, entry.pos())) continue;
            if (!statesMatch(level.getBlockState(entry.pos()), entry.state())) return false;
        }
        return true;
    }

    /** См. NailerWallQueue.statesMatch - почему заборы/стенки сравниваются без north/south/east/west/up. */
    private static boolean statesMatch(BlockState actual, BlockState expected) {
        return NailerWallQueue.statesMatch(actual, expected);
    }

    private void releaseCurrentPiece(ServerLevel level) {
        if (pieceIndex >= 0) {
            NailerWallClaims.release(level, pieces.get(pieceIndex).anchor(), villager.getUUID());
        }
        pieceIndex = -1;
        entryCursor = 0;
    }

    private void giveUpForNow(ServerLevel level) {
        // "Второй уровень" (level2, см. NailerUpgrades.markWallComplete/isLevel2Unlocked) больше не
        // покупается за ресурсы вручную - открывается сам, как только nailer реально достроил ВСЮ
        // текущую очередь стены (та же лениентность к "недостижимым" блокам, что и isPieceComplete -
        // иначе постоянно недостижимый блок навсегда заблокировал бы разблокировку).
        logQueueFinished(level);
        if (pieces != null && !pieces.isEmpty() && isWallFullyBuilt(level)) {
            NailerUpgrades.markWallComplete(villager);
        }
        pieces = null;
        pieceIndex = -1;
        entryCursor = 0;
        queueRecheckCooldown = NailerVillagerConfig.QUEUE_RECHECK_COOLDOWN_TICKS;
        phase = Phase.DONE;
    }

    private boolean isWallFullyBuilt(ServerLevel level) {
        if (NailerTreeInWay.anyWaiting(level, pieces)) return false;
        for (NailerWallQueue.Piece piece : pieces) {
            if (!isPieceComplete(level, piece)) return false;
        }
        return true;
    }

    @Nullable
    private List<NailerWallQueue.Piece> resolveQueue(ServerLevel level) {
        List<VillageAutoPregen.VillagePlan> plans = VillageAutoPregen.plans(level);
        if (plans.isEmpty()) return null;

        BlockPos here = anchorPos();
        VillageAutoPregen.VillagePlan nearest = plans.stream()
                .min(Comparator.comparingDouble(plan -> plan.anchor().distSqr(here)))
                .orElse(null);
        if (nearest == null) return null;

        double maxDist = (double) NailerVillagerConfig.MAX_VILLAGE_SEARCH_DISTANCE
                * NailerVillagerConfig.MAX_VILLAGE_SEARCH_DISTANCE;
        if (nearest.anchor().distSqr(here) > maxDist) return null;

        List<NailerWallQueue.Piece> built = NailerWallQueue.build(level, nearest, villager);
        NailerTreeInWay.requestAll(level, built);
        NailerQueueAudit.log("очередь собрана для деревни у {}: масс {}, блоков {}, tech1={}, tech2={}, tech3={}; в мире {}",
                nearest.anchor().toShortString(), built.size(), built.stream().mapToInt(piece -> piece.entries().size()).sum(),
                NailerUpgrades.isTechUnlocked(villager), NailerUpgrades.isTech2Unlocked(villager),
                NailerUpgrades.isTech3Unlocked(villager), NailerQueueAudit.progressOf(level, built).line());
        return built;
    }

    // --- диагностика (NailerQueueAudit.LOG_ENABLED): в latest.log с меткой [NAILER-DEBUG] ---

    private void logQueueFinished(ServerLevel level) {
        if (!NailerQueueAudit.LOG_ENABLED || pieces == null || pieces.isEmpty()) return;
        NailerQueueAudit.Progress progress = NailerQueueAudit.progressOf(level, pieces);
        NailerQueueAudit.log("очередь пройдена до конца: сделано {}; помечено недостижимыми сейчас: {}", progress.line(), unreachableEntries.size());
        for (String line : NailerQueueAudit.firstUnfinished(level, pieces, pos -> isUnreachable(level, pos), 12)) {
            NailerQueueAudit.log("  осталось: {}", line);
        }
    }

    private void logShiftEnded(ServerLevel level) {
        if (!NailerQueueAudit.LOG_ENABLED || pieces == null || phase == Phase.DONE) return;
        NailerQueueAudit.log("рабочий день кончился: масса #{} курсор {}; сделано {}", pieceIndex, entryCursor,
                NailerQueueAudit.progressOf(level, pieces).line());
    }

    private void logUnreachable(ServerLevel level, NailerWallQueue.Entry target) {
        BlockPos pos = target.pos();
        BlockState actual = level.getBlockState(pos);
        NailerQueueAudit.log("не добраться до {} (нужен {}, сейчас {} / {}): житель в {}, по горизонтали {} блоков, по высоте {}, "
                        + "ходит {} тиков, без движения {} тиков, навигация {}",
                pos.toShortString(), target.state().getBlock().getDescriptionId(), actual.getBlock().getDescriptionId(),
                NailerQueueAudit.statusOf(level, target), villager.blockPosition().toShortString(),
                Math.round(Math.sqrt(horizontalDistanceSqr(pos))), pos.getY() - villager.getBlockY(),
                walkTicks, stuckTicks, villager.getNavigation().isDone() ? "закончила (пути нет)" : "идёт");
    }

    private BlockPos anchorPos() {
        return villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                .filter(globalPos -> globalPos.dimension() == villager.level().dimension())
                .map(GlobalPos::pos)
                .orElseGet(villager::blockPosition);
    }

    // --- склад: забор брёвен и крафт в доски ---

    private void tickWalkToStorage(ServerLevel level) {
        if (emptyStorageWaitTicks > 0) {
            emptyStorageWaitTicks--;
            return;
        }
        BlockPos target = resolveMaterialsStorage(level);
        if (target == null) return;
        cachedMaterialsPos = target;

        double reach = NailerDebugMode.isEnabled(villager) ? Double.MAX_VALUE : NailerVillagerConfig.DEPOSIT_REACH;
        if (walkTowards(level, target, effectiveSpeed(level, NailerVillagerConfig.RETURN_MOVE_SPEED), reach)) {
            phase = Phase.WITHDRAW;
        } else if (walkStuck()) {
            resetWalkState();
        }
    }

    private void tickWithdraw(ServerLevel level) {
        if (cachedMaterialsPos == null
                || !(level.getBlockEntity(cachedMaterialsPos) instanceof BuildingMaterialsBlockEntity be)
                || !restock(be)) {
            emptyStorageWaitTicks = NailerVillagerConfig.EMPTY_STORAGE_WAIT_TICKS;
            phase = Phase.FIND_TARGET;
            return;
        }
        resetWalkState();
        phase = Phase.FIND_TARGET;
    }

    private static final String POINTS_KEY = "veridia_nailer_points";

    /**
     * Пополняет абстрактный счёт очков (не предметы в инвентаре - см. NailerVillagerConfig.
     * POINTS_PER_LOG/PLANK/STICK) до POINTS_RESTOCK_TARGET: сначала готовые доски, которые сам
     * игрок положил в склад (любой породы дерева), потом брёвна, и в последнюю очередь палки
     * (лесоруб их тоже сносит на склад как строительный материал). Обмен фиксированный - тарифы
     * по материалам постройки (pointCost) дешевеют с деревом BRAIN, а курс лог/доска/палка - нет.
     */
    private boolean restock(BuildingMaterialsBlockEntity be) {
        ItemStackHandler handler = be.itemHandler;
        boolean withdrewAny = false;

        while (countPoints() < NailerVillagerConfig.POINTS_RESTOCK_TARGET) {
            int slot = findMatchingSlot(handler, ItemTags.PLANKS);
            if (slot < 0) break;
            int planksNeeded = ceilDiv(NailerVillagerConfig.POINTS_RESTOCK_TARGET - countPoints(), NailerVillagerConfig.POINTS_PER_PLANK);
            ItemStack taken = handler.extractItem(slot, planksNeeded, false);
            if (taken.isEmpty()) break;
            addPoints(taken.getCount() * NailerVillagerConfig.POINTS_PER_PLANK);
            withdrewAny = true;
        }
        while (countPoints() < NailerVillagerConfig.POINTS_RESTOCK_TARGET) {
            int slot = findMatchingSlot(handler, ItemTags.LOGS);
            if (slot < 0) break;
            ItemStack taken = handler.extractItem(slot, 1, false);
            if (taken.isEmpty()) break;
            addPoints(NailerVillagerConfig.POINTS_PER_LOG);
            withdrewAny = true;
        }
        while (countPoints() < NailerVillagerConfig.POINTS_RESTOCK_TARGET) {
            int slot = findItemSlot(handler, Items.STICK);
            if (slot < 0) break;
            int sticksNeeded = NailerVillagerConfig.POINTS_RESTOCK_TARGET - countPoints();
            ItemStack taken = handler.extractItem(slot, sticksNeeded, false);
            if (taken.isEmpty()) break;
            addPoints(taken.getCount() * NailerVillagerConfig.POINTS_PER_STICK);
            withdrewAny = true;
        }
        return withdrewAny;
    }

    private static int ceilDiv(int amount, int divisor) {
        return (amount + divisor - 1) / divisor;
    }

    private static int findMatchingSlot(ItemStackHandler handler, TagKey<Item> tag) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(tag)) return slot;
        }
        return -1;
    }

    private static int findItemSlot(ItemStackHandler handler, Item item) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(item)) return slot;
        }
        return -1;
    }

    private int countPoints() {
        if (NailerDebugMode.isEnabled(villager)) return Integer.MAX_VALUE;
        return villager.getPersistentData().getInt(POINTS_KEY);
    }

    private void addPoints(int amount) {
        CompoundTag data = villager.getPersistentData();
        data.putInt(POINTS_KEY, data.getInt(POINTS_KEY) + amount);
    }

    private boolean consumePoints(int amount) {
        if (NailerDebugMode.isEnabled(villager)) return true;
        CompoundTag data = villager.getPersistentData();
        int have = data.getInt(POINTS_KEY);
        if (have < amount) return false;
        data.putInt(POINTS_KEY, have - amount);
        return true;
    }

    /** Привязан к конкретному building_materials - см. VillagerStorageBinding, не бегает по чужим складам. */
    @Nullable
    private BlockPos resolveMaterialsStorage(ServerLevel level) {
        BlockPos cached = VillagerStorageBinding.resolveCached(villager, level, StorageKind.MATERIALS,
                ModBlocks.BUILDING_MATERIALS.get(), BuildingMaterialsBlockEntity.class);
        if (cached != null) return cached;
        if (materialsRescanCooldown > 0) {
            materialsRescanCooldown--;
            return null;
        }
        materialsRescanCooldown = NailerVillagerConfig.HOME_RESCAN_TICKS;
        return VillagerStorageBinding.claimNew(villager, level, StorageKind.MATERIALS,
                ModBlocks.BUILDING_MATERIALS.get(), BuildingMaterialsBlockEntity.class,
                anchorPos(), NailerVillagerConfig.HOME_SEARCH_RADIUS, NailerVillagerConfig.HOME_SEARCH_VERTICAL_RADIUS, null);
    }

    // --- ходьба к блоку и постройка ---

    private void tickWalkToTarget(ServerLevel level) {
        if (currentTarget == null) {
            phase = Phase.FIND_TARGET;
            return;
        }
        boolean debug = NailerDebugMode.isEnabled(villager);
        double reach = debug ? Double.MAX_VALUE : NailerVillagerConfig.BUILD_REACH;
        if (walkTowards(level, currentTarget.pos(), effectiveSpeed(level, NailerVillagerConfig.BUILD_MOVE_SPEED), reach)) {
            if (debug) {
                buildTicksLeft = NailerVillagerConfig.DEBUG_BUILD_TICKS;
            } else {
                int rolledTicks = NailerVillagerConfig.BUILD_TICKS_MIN + level.random.nextInt(
                        NailerVillagerConfig.BUILD_TICKS_MAX - NailerVillagerConfig.BUILD_TICKS_MIN + 1);
                buildTicksLeft = Math.max(1, (int) Math.round(rolledTicks / NailerUpgrades.buildSpeedMultiplier(villager)));
            }
            phase = Phase.BUILD;
        } else if (walkStuck()) {
            logUnreachable(level, currentTarget);
            markUnreachable(level, currentTarget.pos());
            entryCursor++;
            resetWalkState();
            phase = Phase.FIND_TARGET;
        }
    }

    private boolean isUnreachable(ServerLevel level, BlockPos pos) {
        Long markedAtTick = unreachableEntries.get(pos);
        if (markedAtTick == null) return false;
        if (level.getGameTime() - markedAtTick >= NailerVillagerConfig.UNREACHABLE_RETRY_COOLDOWN_TICKS) {
            unreachableEntries.remove(pos);
            return false;
        }
        return true;
    }

    private void markUnreachable(ServerLevel level, BlockPos pos) {
        unreachableEntries.remove(pos);
        unreachableEntries.put(pos, level.getGameTime());
        Iterator<BlockPos> oldest = unreachableEntries.keySet().iterator();
        while (unreachableEntries.size() > NailerVillagerConfig.UNREACHABLE_ENTRY_MEMORY && oldest.hasNext()) {
            oldest.next();
            oldest.remove();
        }
    }

    private void tickBuild(ServerLevel level) {
        if (currentTarget == null) {
            phase = Phase.FIND_TARGET;
            return;
        }
        ensureToolInHand();
        holdPosition();
        lookAt(currentTarget.pos());
        if (villager.tickCount % NailerVillagerConfig.BUILD_HIT_PERIOD_TICKS == 0) {
            villager.swing(InteractionHand.MAIN_HAND);
        }
        if (villager.tickCount % NailerVillagerConfig.BUILD_SPARK_PERIOD_TICKS == 0) {
            Vec3 center = Vec3.atCenterOf(currentTarget.pos());
            level.sendParticles(ModParticles.SPARK.get(), center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        buildTicksLeft--;
        if (buildTicksLeft > 0) return;

        boolean placed = placeCurrentTarget(level);
        if (placed) entryCursor++;
        currentTarget = null;
        resetWalkState();
        phase = Phase.FIND_TARGET;
    }

    /** Доска в руке - чисто косметика анимации удара, больше не привязана к очкам (см. POINTS_KEY). */
    private void ensureToolInHand() {
        if (!villager.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.OAK_PLANKS)) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.OAK_PLANKS));
            villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    private boolean placeCurrentTarget(ServerLevel level) {
        BlockPos pos = currentTarget.pos();
        BlockState state = currentTarget.state();
        int cost = currentTarget.pointCost();
        if (NailerTreeInWay.resolve(level, pos) == NailerTreeInWay.Outcome.WAITING) {
            return false;
        }
        if (cost > 0 && !consumePoints(cost)) {
            NailerQueueAudit.log("не хватило очков на {}: нужно {}, есть {}", pos.toShortString(), cost, countPoints());
            return false;
        }

        if (level.getBlockState(pos).equals(state)) {
            return true;
        }
        if (state.isAir()) {
            // Земляные работы под дом (HouseEarthworks) - "поставить воздух" = срезать блок: звук и
            // частицы разрушения, без дропа (земля не копится в мире кучей предметов).
            level.destroyBlock(pos, false, villager);
        } else {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            level.playSound(null, pos, state.getSoundType(level, pos, villager).getPlaceSound(),
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        sparkAt(level, pos);
        return true;
    }

    /** Вспышка veridia:spark на месте блока, который житель только что поставил (или срезал). */
    private static void sparkAt(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        double spread = NailerVillagerConfig.PLACE_SPARK_SPREAD;
        level.sendParticles(ModParticles.SPARK.get(), center.x, center.y, center.z,
                NailerVillagerConfig.PLACE_SPARK_COUNT, spread, spread, spread, NailerVillagerConfig.PLACE_SPARK_SPEED);
    }

    // --- общая ходьба (см. LumberjackChopGoal/FarmerPlantGoal - тот же паттерн) ---

    private double effectiveSpeed(ServerLevel level, double baseSpeed) {
        return baseSpeed * NailerUpgrades.walkSpeedMultiplier(villager);
    }

    private boolean walkTowards(ServerLevel level, BlockPos target, double speed, double reach) {
        lookAt(target);
        if (horizontalDistanceSqr(target) > reach * reach) {
            walkTicks++;
            updateStuckTracking();
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(target, (float) speed, Math.max(1, (int) reach)));
            return false;
        }
        resetWalkState();
        holdPosition();
        return true;
    }

    private double horizontalDistanceSqr(BlockPos target) {
        double dx = villager.getX() - (target.getX() + 0.5);
        double dz = villager.getZ() - (target.getZ() + 0.5);
        return dx * dx + dz * dz;
    }

    private boolean walkStuck() {
        return stuckTicks >= NailerVillagerConfig.GIVE_UP_STUCK_TICKS
                || walkTicks > NailerVillagerConfig.WALK_TIMEOUT_TICKS;
    }

    private void resetWalkState() {
        walkTicks = 0;
        stuckTicks = 0;
        lastWalkPos = null;
    }

    private void updateStuckTracking() {
        Vec3 pos = villager.position();
        if (lastWalkPos != null && pos.distanceToSqr(lastWalkPos) > NailerVillagerConfig.STUCK_MOVE_EPSILON_SQR) {
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }
        lastWalkPos = pos;
    }

    private void holdPosition() {
        villager.getNavigation().stop();
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.PATH);
    }

    private void lookAt(BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        villager.getLookControl().setLookAt(center.x, center.y, center.z);
        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
    }
}
