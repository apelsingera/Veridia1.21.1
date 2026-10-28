package net.gera.veridia.common.aibuff.villager.farmer;

import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.types.FoodStorageBlockEntity;
import net.gera.veridia.common.village.calc.VillageAutoPregen;
import net.gera.veridia.common.village.calc.VillageCalcSettings;
import net.gera.veridia.common.village.calc.VillageScan;
import net.gera.veridia.common.village.calc.WallPlan;
import net.gera.veridia.common.villager.storage.StorageKind;
import net.gera.veridia.common.villager.storage.VillagerEmployment;
import net.gera.veridia.common.villager.storage.VillagerStorageBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** Ванильный фермер: берёт саженец из food_storage и сажает его за забором деревни. */
public class FarmerPlantGoal extends Goal {

    private enum Phase { WALK_TO_STORAGE, WITHDRAW, FIND_SPOT, WALK_TO_SPOT, PLANT, DONE }

    private final Villager villager;
    private Phase phase = Phase.DONE;

    private BlockPos cachedFoodPos;
    private int foodRescanCooldown;
    /** Отдельный throttle на случай "склад привязан, но саженцев сейчас нет" - чтобы не устраивать
     * бесконечный цикл WALK_TO_STORAGE/WITHDRAW в один тик, раз фермер теперь не мечется по другим
     * складам, а ждёт у своего. */
    private int emptyStorageWaitTicks;

    private ItemStack heldSapling = ItemStack.EMPTY;

    private BlockPos plantSpot;
    private int plantCooldown;
    private int plantTicksLeft;

    private int walkTicks;
    private int stuckTicks;
    private Vec3 lastWalkPos;

    public FarmerPlantGoal(Villager villager) {
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
        if (villager.getVillagerData().getProfession() != VillagerProfession.FARMER) return false;
        if (!VillagerEmployment.isWorkEnabled(villager)) return false;
        if (!(villager.level() instanceof ServerLevel serverLevel)) return false;
        if (villager.getBrain().isActive(Activity.PANIC)) return false;

        if (phase == Phase.DONE) {
            return isWorkTime(serverLevel);
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (villager.getVillagerData().getProfession() != VillagerProfession.FARMER) return false;
        if (!VillagerEmployment.isWorkEnabled(villager)) return false;
        return phase != Phase.DONE && !villager.getBrain().isActive(Activity.PANIC);
    }

    @Override
    public void start() {
        if (phase == Phase.DONE) {
            phase = Phase.WALK_TO_STORAGE;
        }
        resetWalkState();
        if (!heldSapling.isEmpty()) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, heldSapling.copy());
            villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
        setTravelingToPlant(villager, true);
    }

    @Override
    public void stop() {
        villager.getNavigation().stop();
        villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        setTravelingToPlant(villager, false);
    }

    private static final String TRAVELING_KEY = "veridia_farmer_planting_travel";

    /** true, пока Goal реально ведёт фермера (склад/поиск места/дорога/посадка) - см.
     * FarmerVillagerHandler.blockWorkWhileTraveling: пока это true, ванильная Activity.WORK
     * (грядки/компостер) блокируется, чтобы не перехватывала WALK_TARGET посреди похода за саженцем. */
    public static boolean isTravelingToPlant(Villager villager) {
        return villager.getPersistentData().getBoolean(TRAVELING_KEY);
    }

    private static void setTravelingToPlant(Villager villager, boolean traveling) {
        villager.getPersistentData().putBoolean(TRAVELING_KEY, traveling);
    }

    @Override
    public void tick() {
        if (!(villager.level() instanceof ServerLevel level)) return;

        if (phase != Phase.PLANT && !isWorkTime(level)) {
            phase = Phase.DONE;
            return;
        }

        switch (phase) {
            case WALK_TO_STORAGE -> tickWalkToStorage(level);
            case WITHDRAW -> tickWithdraw(level);
            case FIND_SPOT -> tickFindSpot(level);
            case WALK_TO_SPOT -> tickWalkToSpot(level);
            case PLANT -> tickPlant(level);
            case DONE -> {
            }
        }
    }

    private boolean isWorkTime(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000L;
        return dayTime >= FarmerUpgrades.workStartTick(villager) && dayTime < FarmerUpgrades.workEndTick(villager);
    }

    /** Короткое описание текущей фазы - для кнопки "?" в панели склада (WorkerPanelActions). */
    public Component statusText() {
        return switch (phase) {
            case WALK_TO_STORAGE -> Component.translatable("gui.veridia.worker_panel.status.farmer.to_storage");
            case WITHDRAW -> Component.translatable("gui.veridia.worker_panel.status.farmer.withdraw");
            case FIND_SPOT -> Component.translatable("gui.veridia.worker_panel.status.farmer.find_spot");
            case WALK_TO_SPOT -> Component.translatable("gui.veridia.worker_panel.status.farmer.to_spot");
            case PLANT -> Component.translatable("gui.veridia.worker_panel.status.farmer.planting");
            case DONE -> Component.translatable("gui.veridia.worker_panel.status.idle");
        };
    }

    // --- забор food_storage с саженцем ---

    private void tickWalkToStorage(ServerLevel level) {
        if (emptyStorageWaitTicks > 0) {
            emptyStorageWaitTicks--;
            return;
        }
        BlockPos target = resolveFoodStorage(level);
        if (target == null) return;
        cachedFoodPos = target;

        if (walkTowards(level, target, effectiveSpeed(level, FarmerVillagerConfig.FOOD_MOVE_SPEED), FarmerVillagerConfig.FOOD_REACH)) {
            phase = Phase.WITHDRAW;
        } else if (walkStuck()) {
            resetWalkState();
        }
    }

    private void tickWithdraw(ServerLevel level) {
        if (cachedFoodPos == null || !(level.getBlockEntity(cachedFoodPos) instanceof FoodStorageBlockEntity be)
                || !withdrawSapling(be)) {
            emptyStorageWaitTicks = FarmerVillagerConfig.SEED_CHECK_COOLDOWN_TICKS;
            phase = Phase.WALK_TO_STORAGE;
            return;
        }
        villager.setItemSlot(EquipmentSlot.MAINHAND, heldSapling.copy());
        villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        resetWalkState();
        phase = Phase.FIND_SPOT;
    }

    private boolean withdrawSapling(FoodStorageBlockEntity be) {
        ItemStackHandler handler = be.itemHandler;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (FarmerVillagerConfig.isPlantable(handler.getStackInSlot(slot))) {
                heldSapling = handler.extractItem(slot, 1, false);
                return !heldSapling.isEmpty();
            }
        }
        return false;
    }

    /** Привязан к конкретному food_storage - см. VillagerStorageBinding, не бегает по чужим складам. */
    @Nullable
    private BlockPos resolveFoodStorage(ServerLevel level) {
        BlockPos cached = VillagerStorageBinding.resolveCached(villager, level, StorageKind.FOOD,
                ModBlocks.FOOD_STORAGE.get(), FoodStorageBlockEntity.class);
        if (cached != null) return cached;
        if (foodRescanCooldown > 0) {
            foodRescanCooldown--;
            return null;
        }
        foodRescanCooldown = FarmerVillagerConfig.HOME_RESCAN_TICKS;
        return VillagerStorageBinding.claimNew(villager, level, StorageKind.FOOD,
                ModBlocks.FOOD_STORAGE.get(), FoodStorageBlockEntity.class,
                villager.blockPosition(), FarmerVillagerConfig.HOME_SEARCH_RADIUS,
                FarmerVillagerConfig.HOME_SEARCH_VERTICAL_RADIUS, null);
    }

    // --- поиск точки посадки за стеной деревни ---

    private void tickFindSpot(ServerLevel level) {
        if (heldSapling.isEmpty()) {
            phase = Phase.WALK_TO_STORAGE;
            return;
        }
        if (plantCooldown > 0) {
            plantCooldown--;
            return;
        }
        BlockPos spot = findPlantSpot(level);
        if (spot == null) {
            plantCooldown = FarmerVillagerConfig.PLANT_RESCAN_TICKS;
            return;
        }
        plantSpot = spot;
        phase = Phase.WALK_TO_SPOT;
    }

    @Nullable
    private BlockPos findPlantSpot(ServerLevel level) {
        List<VillageAutoPregen.VillagePlan> plans = VillageAutoPregen.plans(level);
        if (plans.isEmpty()) return null;

        BlockPos here = villager.blockPosition();
        VillageAutoPregen.VillagePlan nearestVillage = plans.stream()
                .min(Comparator.comparingDouble(plan -> plan.anchor().distSqr(here)))
                .orElse(null);
        if (nearestVillage == null) return null;
        double maxDist = (double) FarmerVillagerConfig.MAX_VILLAGE_SEARCH_DISTANCE
                * FarmerVillagerConfig.MAX_VILLAGE_SEARCH_DISTANCE;
        if (nearestVillage.anchor().distSqr(here) > maxDist) return null;

        VillageScan scan = nearestVillage.scan();
        List<WallPlan.Segment> segments = nearestVillage.plan().segments();
        if (segments.isEmpty()) return null;

        List<WallPlan.Segment> nearestSegments = segments.stream()
                .sorted(Comparator.comparingDouble(segment -> segmentDistSqr(scan, segment)))
                .limit(FarmerVillagerConfig.PLANT_SPOT_ATTEMPTS)
                .toList();

        for (WallPlan.Segment segment : nearestSegments) {
            BlockPos spot = candidateSpot(level, scan, segment);
            if (spot != null) return spot;
        }
        return null;
    }

    private double segmentDistSqr(VillageScan scan, WallPlan.Segment segment) {
        int half = VillageCalcSettings.PIECE_LENGTH / 2;
        int cx = segment.cellX() + segment.along().getStepX() * half;
        int cz = segment.cellZ() + segment.along().getStepZ() * half;
        return villager.distanceToSqr(scan.worldX(cx), villager.getY(), scan.worldZ(cz));
    }

    @Nullable
    private BlockPos candidateSpot(ServerLevel level, VillageScan scan, WallPlan.Segment segment) {
        int half = VillageCalcSettings.PIECE_LENGTH / 2;
        int midCx = segment.cellX() + segment.along().getStepX() * half;
        int midCz = segment.cellZ() + segment.along().getStepZ() * half;
        int outCx = midCx + segment.outward().getStepX() * FarmerVillagerConfig.WALL_OFFSET_BLOCKS;
        int outCz = midCz + segment.outward().getStepZ() * FarmerVillagerConfig.WALL_OFFSET_BLOCKS;

        // Если точка всё ещё внутри известной территории деревни - забор в этом месте не тот, что нужен.
        if (scan.inBounds(outCx, outCz) && scan.isTerritory(outCx, outCz)) {
            return null;
        }

        int worldX = scan.worldX(outCx);
        int worldZ = scan.worldZ(outCz);
        if (!level.hasChunkAt(worldX, worldZ)) return null;

        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ) - 1;
        BlockPos plantAt = new BlockPos(worldX, groundY + 1, worldZ);

        if (!level.getBlockState(plantAt).isAir()) return null;
        if (!(heldSapling.getItem() instanceof BlockItem blockItem)) return null;
        BlockState toPlace = blockItem.getBlock().defaultBlockState();
        if (!toPlace.canSurvive(level, plantAt)) return null;

        return plantAt;
    }

    // --- ходьба к точке и посадка ---

    private void tickWalkToSpot(ServerLevel level) {
        if (plantSpot == null || heldSapling.isEmpty()) {
            phase = Phase.WALK_TO_STORAGE;
            return;
        }
        if (walkTowards(level, plantSpot, effectiveSpeed(level, FarmerVillagerConfig.PLANT_MOVE_SPEED), FarmerVillagerConfig.PLANT_REACH)) {
            plantTicksLeft = FarmerUpgrades.plantHoldTicks(villager);
            phase = Phase.PLANT;
        } else if (walkStuck()) {
            plantSpot = null;
            resetWalkState();
            phase = Phase.FIND_SPOT;
        }
    }

    private void tickPlant(ServerLevel level) {
        holdPosition();
        if (plantSpot != null) lookAt(plantSpot);

        if (villager.tickCount % FarmerVillagerConfig.PLANT_HIT_PERIOD_TICKS == 0) {
            villager.swing(InteractionHand.MAIN_HAND);
        }

        plantTicksLeft--;
        if (plantTicksLeft > 0) return;

        plantSapling(level);
        resetWalkState();
        phase = isWorkTime(level) ? Phase.WALK_TO_STORAGE : Phase.DONE;
    }

    private void plantSapling(ServerLevel level) {
        if (plantSpot != null && !heldSapling.isEmpty() && heldSapling.getItem() instanceof BlockItem blockItem) {
            BlockState state = blockItem.getBlock().defaultBlockState();
            if (level.getBlockState(plantSpot).isAir() && state.canSurvive(level, plantSpot)) {
                level.setBlock(plantSpot, state, Block.UPDATE_ALL);
                level.playSound(null, plantSpot, SoundEvents.CROP_PLANTED, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        }
        heldSapling = ItemStack.EMPTY;
        villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        plantSpot = null;
    }

    // --- общая ходьба ---

    /** Апгрейд speed (FarmerUpgrades) - множитель ко всем "скоростям ходьбы" отсюда. */
    private double effectiveSpeed(ServerLevel level, double baseSpeed) {
        return baseSpeed * FarmerUpgrades.walkSpeedMultiplier(villager);
    }

    private boolean walkTowards(ServerLevel level, BlockPos target, double speed, double reach) {
        lookAt(target);
        if (villager.distanceToSqr(Vec3.atCenterOf(target)) > reach * reach) {
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

    private boolean walkStuck() {
        return stuckTicks >= FarmerVillagerConfig.GIVE_UP_STUCK_TICKS
                || walkTicks > FarmerVillagerConfig.WALK_TIMEOUT_TICKS;
    }

    private void resetWalkState() {
        walkTicks = 0;
        stuckTicks = 0;
        lastWalkPos = null;
    }

    private void updateStuckTracking() {
        Vec3 pos = villager.position();
        if (lastWalkPos != null && pos.distanceToSqr(lastWalkPos) > FarmerVillagerConfig.STUCK_MOVE_EPSILON_SQR) {
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
