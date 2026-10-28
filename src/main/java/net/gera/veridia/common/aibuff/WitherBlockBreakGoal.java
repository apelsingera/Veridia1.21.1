package net.gera.veridia.common.aibuff;

import net.gera.veridia.common.village.calc.VillageWallLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;

/**
 * Цель "пробиться к игроку": когда моб стоит на месте и не может достать цель, WitherPathfinder строит маршрут
 * (в обход или с ломанием блоков), а моб идёт по нему и ломает блоки в очереди. Если маршрута нет совсем, а цель
 * выше, моб поднимается "пилларом": прыгает и ставит блок под себя. Используют визер-скелет, зомби-шахтёры и
 * уровневые зомби/хаски (с разной скоростью ломания).
 */
public class WitherBlockBreakGoal extends Goal {

    public static final String BREAKING_KEY = "veridia_witherbuff_breaking";

    public static final String FORCE_PILLAR_KEY = "veridia_witherbuff_force_pillar";

    private static final ItemStack VIRTUAL_PICKAXE = new ItemStack(Items.WOODEN_PICKAXE);

    /** Как у ванили: делитель скорости ломания с подходящим инструментом и без него. */
    private static final float VANILLA_DIVISOR_CORRECT_TOOL = 30f;
    private static final float VANILLA_DIVISOR_WRONG_TOOL = 100f;
    /** Стадий трещин у ломаемого блока (0..9). */
    private static final int CRACK_STAGES = 9;
    /** Какая клетка над ногами считается потолком, который ломают отдельно от A* (см. findCeilingObstruction). */
    private static final int CEILING_CELLS_ABOVE_FEET = 2;
    /** На сколько клеток сверх WITHER_PILLAR_MAX_GAP цель может уйти от строящегося столба, прежде чем план строится заново. */
    private static final int PILLAR_DRIFT_GAP = 2;

    /**
     * Подъём пилларом: прыжок, потом блок на место, откуда прыгнули. Один объект на цель, чтобы не держать семь
     * отдельных полей и сбрасывать их одним вызовом.
     */
    private static final class Pillar {
        /** Подъём идёт прямо сейчас. */
        boolean active;
        /** Принудительный (после ударов игрока): высота не зависит от положения цели. */
        boolean forced;
        /** Лимит блоков или времени исчерпан: новый подъём в этот заход не начинается. */
        boolean exhausted;
        int blocksPlaced;
        /** Тиков подряд без нового блока - защита от прыжков на месте под потолком или в воде. */
        int stalledTicks;
        /** Клетка, из которой прыгнули: на её место встанет блок. */
        BlockPos origin;
        double goalY;

        void begin(double goalY, boolean forced) {
            this.active = true;
            this.forced = forced;
            this.goalY = goalY;
            this.origin = null;
            this.stalledTicks = 0;
        }

        void end() {
            active = false;
            forced = false;
            origin = null;
        }

        void reset() {
            end();
            exhausted = false;
            blocksPlaced = 0;
            stalledTicks = 0;
        }
    }

    private final Mob mob;
    private final int breakerId;
    private final double breakSpeedMultiplier;
    /** Во сколько раз ещё медленнее ломается блок стены жителей (см. VillageWallLookup). */
    private final double villageWallSlowdown;

    private final Deque<BlockPos> breakQueue = new ArrayDeque<>();
    private final Pillar pillar = new Pillar();
    private BlockPos targetPos;
    private double progress;
    private int stuckTicks;
    private LivingEntity currentTarget;

    private int finalApproachTicks;
    private int idleRenavigateCooldown;
    /** Моб идёт к ближайшей к недостижимой цели клетке (см. approachOrPillar), а не по маршруту к самой цели. */
    private boolean approaching;
    /** Сколько тиков моб уже реально ломал блоки в этом заходе (см. WITHER_DIG_GIVE_UP_TICKS). */
    private int diggingTicks;
    /** Копать надоело (слишком долго, а цель выше): дальше при любой возможности строиться вверх, а не прорубаться. */
    private boolean preferBuilding;

    /** Для какого блока посчитано замедление стены (кеш: VillageWallLookup обходит все планы деревень, а блок ломается десятки тиков). */
    private BlockPos slowdownCheckedFor;
    private double slowdownOfChecked = 1.0;

    public WitherBlockBreakGoal(Mob mob) {
        this(mob, AbilityConfig.WITHER_BREAK_SLOWNESS_MULTIPLIER);
    }

    public WitherBlockBreakGoal(Mob mob, double breakSpeedMultiplier) {
        this(mob, breakSpeedMultiplier, AbilityConfig.VILLAGE_WALL_SLOWDOWN_BREAKER);
    }

    public WitherBlockBreakGoal(Mob mob, double breakSpeedMultiplier, double villageWallSlowdown) {
        this.mob = mob;
        this.breakerId = mob.getId();
        this.breakSpeedMultiplier = breakSpeedMultiplier;
        this.villageWallSlowdown = villageWallSlowdown;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    // ------------------------------------------------------------------ жизненный цикл цели

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            stuckTicks = 0;
            return false;
        }

        if (!mob.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
        if (mob.getPersistentData().getBoolean(FORCE_PILLAR_KEY)) {
            if (isWorthPillaring(mob.blockPosition(), target)) return true;
            mob.getPersistentData().putBoolean(FORCE_PILLAR_KEY, false); // бьют не сверху и не рядом - подниматься незачем
        }
        if (mob.hurtTime > 0) return false;
        if (canAttackNow(target)) {
            stuckTicks = 0;
            return false;
        }

        Vec3 v = mob.getDeltaMovement();
        double horizSpeedSq = v.x * v.x + v.z * v.z;
        if (horizSpeedSq > 0.0025) {
            stuckTicks = 0;
            return false;
        }

        stuckTicks++;
        return stuckTicks > AbilityConfig.WITHER_BREAK_STUCK_TICKS;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();

        if (target == null || !target.isAlive()) return false;
        if (currentTarget != null && target != currentTarget) return false;
        if (pillar.active) return true;

        if (canAttackNow(target)) return false;
        if (mob.hurtTime > 0) return false;
        if (targetPos != null || !breakQueue.isEmpty()) return true;
        return finalApproachTicks < AbilityConfig.WITHER_BREAK_FINAL_APPROACH_TICKS;
    }

    @Override
    public void start() {
        currentTarget = mob.getTarget();
        progress = 0;
        finalApproachTicks = 0;
        idleRenavigateCooldown = 0;
        targetPos = null;
        approaching = false;
        diggingTicks = 0;
        preferBuilding = false;
        pillar.reset();
        refreshPlan(currentTarget);
        maybeBeginForcedPillar();
    }

    @Override
    public void stop() {
        clearTarget();
        currentTarget = null;
        breakQueue.clear();
        stuckTicks = 0;
        finalApproachTicks = 0;
        idleRenavigateCooldown = 0;
        approaching = false;
        diggingTicks = 0;
        preferBuilding = false;
        pillar.reset();
        setBreaking(false);
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        maybeBeginForcedPillar();

        if (pillar.active) {
            tickPillar();
            return;
        }

        if (targetPos == null) {
            targetPos = breakQueue.poll();
        }
        if (targetPos == null) {
            idleApproach();
            return;
        }

        finalApproachTicks = 0;
        tickBreaking();
    }

    /** Моб достаёт цель в ближнем бою и видит её - ломать нечего, пусть работает обычная атака. */
    private boolean canAttackNow(LivingEntity target) {
        return mob.isWithinMeleeAttackRange(target) && mob.getSensing().hasLineOfSight(target);
    }

    // ------------------------------------------------------------------ ломание

    private void tickBreaking() {
        Level level = mob.level();
        BlockState state = level.getBlockState(targetPos);

        if (!isMineable(state) || mob.blockPosition().equals(targetPos.above())) {
            clearTarget();
            setBreaking(false);
            return;
        }

        double reach = AbilityConfig.WITHER_BREAK_REACH;
        if (mob.position().distanceToSqr(Vec3.atCenterOf(targetPos)) > reach * reach) {
            setBreaking(false);
            if (currentTarget != null) {
                mob.getLookControl().setLookAt(currentTarget, 30.0F, 30.0F);
            }
            maybeReplanIfStalled();
            return;
        }

        setBreaking(true);
        mob.getLookControl().setLookAt(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                10.0F, (float) mob.getMaxHeadXRot());

        mob.swing(InteractionHand.MAIN_HAND);
        progress += calculateProgressPerTick(state, level, targetPos);
        level.destroyBlockProgress(breakerId, targetPos, (int) (Math.min(progress, 1.0) * CRACK_STAGES));

        if (progress >= 1.0) {
            level.destroyBlock(targetPos, false, mob);
            clearTarget();
        }
        diggingTicks++;
        giveUpDiggingIfTooLong();
    }

    /**
     * Моб копает слишком долго, а цель выше: надо было не прорубаться (оценка плана ошиблась, цель ушла выше), а строиться.
     * С этого момента при любой возможности он строит столб вверх (см. shouldBuildInsteadOfDig).
     */
    private void giveUpDiggingIfTooLong() {
        if (preferBuilding || currentTarget == null || diggingTicks <= AbilityConfig.WITHER_DIG_GIVE_UP_TICKS
                || pillar.exhausted || !isUpward(currentTarget)) {
            return;
        }
        preferBuilding = true;
        clearTarget();
        refreshPlan(currentTarget);
    }

    /** Бросает текущий блок: снимает трещины и сбрасывает прогресс. */
    private void clearTarget() {
        if (targetPos != null) {
            mob.level().destroyBlockProgress(breakerId, targetPos, -1);
        }
        targetPos = null;
        progress = 0;
        slowdownCheckedFor = null;
    }

    private void setBreaking(boolean breaking) {
        mob.getPersistentData().putBoolean(BREAKING_KEY, breaking);
    }

    private double calculateProgressPerTick(BlockState state, Level level, BlockPos pos) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) return 0;

        float toolSpeed = VIRTUAL_PICKAXE.getDestroySpeed(state);
        boolean correctTool = !state.requiresCorrectToolForDrops()
                || VIRTUAL_PICKAXE.isCorrectToolForDrops(state);
        float divisor = correctTool ? VANILLA_DIVISOR_CORRECT_TOOL : VANILLA_DIVISOR_WRONG_TOOL;

        double vanillaProgressPerTick = toolSpeed / hardness / divisor;
        return vanillaProgressPerTick / breakSpeedMultiplier / wallSlowdown(level, pos, state);
    }

    /** Во сколько раз блок ломается медленнее из-за стены жителей; считается один раз на ломаемый блок. */
    private double wallSlowdown(Level level, BlockPos pos, BlockState state) {
        if (!pos.equals(slowdownCheckedFor)) {
            slowdownCheckedFor = pos;
            boolean wall = level instanceof ServerLevel serverLevel && VillageWallLookup.isVillageWall(serverLevel, pos, state);
            slowdownOfChecked = wall ? villageWallSlowdown : 1.0;
        }
        return slowdownOfChecked;
    }

    private boolean isMineable(@NotNull BlockState state) {
        if (state.isAir()) return false;
        return AbilityConfig.isPickaxeWhitelisted(state);
    }

    // ------------------------------------------------------------------ подход и план

    private void idleApproach() {
        setBreaking(false);
        finalApproachTicks++;
        if (currentTarget == null) return;

        if (mob.getNavigation().isDone()) {
            if (idleRenavigateCooldown > 0) {
                idleRenavigateCooldown--;
            } else {
                idleRenavigateCooldown = AbilityConfig.WITHER_BREAK_IDLE_REPLAN_TICKS;
                if (approaching) {
                    refreshPlan(currentTarget); // дошли до ближайшей к цели клетки - теперь решаем, поднимаемся ли отсюда
                } else {
                    mob.getNavigation().moveTo(currentTarget, AbilityConfig.WITHER_BREAK_MOVE_SPEED);
                }
            }
        }
        mob.getLookControl().setLookAt(currentTarget, 30.0F, 30.0F);
    }

    private void maybeReplanIfStalled() {
        if (!mob.getNavigation().isDone()) return;

        if (idleRenavigateCooldown > 0) {
            idleRenavigateCooldown--;
            return;
        }
        idleRenavigateCooldown = AbilityConfig.WITHER_BREAK_IDLE_REPLAN_TICKS;

        if (currentTarget != null && refreshPlan(currentTarget)) {
            targetPos = breakQueue.poll();
        }
    }

    /** Строит план заново: очередь блоков на слом и путь для навигации; если цель не достать - подход к ней и подъём. */
    private boolean refreshPlan(LivingEntity target) {
        breakQueue.clear();
        pillar.end();
        approaching = false;

        BlockPos ceiling = findCeilingObstruction(target);
        if (ceiling != null) {
            breakQueue.add(ceiling);
            return true;
        }

        WitherPathfinder.Approach approach = WitherPathfinder.findApproach(mob, target);
        if (!approach.reachesGoal()) {
            return approachOrPillar(target, approach);
        }
        if (shouldBuildInsteadOfDig(target, approach.steps()) && buildUpTowards(target)) {
            return true;
        }
        return followRoute(approach.steps());
    }

    /**
     * Путь к цели есть, но через ломание. Если цель выше и копать придётся слишком долго (WITHER_DIG_TOO_LONG_TICKS, или
     * моб уже копал дольше WITHER_DIG_GIVE_UP_TICKS), быстрее и разумнее построить столб рядом, а не прорубать "лестницу"
     * в толще блоков (по 3 блока на шаг у визера).
     */
    private boolean shouldBuildInsteadOfDig(LivingEntity target, List<WitherPathfinder.PlannedStep> route) {
        if (pillar.exhausted || !isUpward(target)) {
            return false;
        }
        return preferBuilding || estimateDigTicks(route) > AbilityConfig.WITHER_DIG_TOO_LONG_TICKS;
    }

    private boolean isUpward(LivingEntity target) {
        return target.getY() - mob.getY() >= AbilityConfig.WITHER_PILLAR_MIN_HEIGHT_DIFF;
    }

    /** Сколько тиков заняло бы ломание всех блоков маршрута: инструмент моба, твёрдость и стена деревни учтены. */
    private double estimateDigTicks(List<WitherPathfinder.PlannedStep> route) {
        Level level = mob.level();
        double ticks = 0;
        for (WitherPathfinder.PlannedStep step : route) {
            for (BlockPos pos : step.breakTargets()) {
                double perTick = calculateProgressPerTick(level.getBlockState(pos), level, pos);
                if (perTick <= 0) {
                    return Double.POSITIVE_INFINITY;
                }
                ticks += 1.0 / perTick;
            }
        }
        slowdownCheckedFor = null; // оценка прошла по чужим блокам: кеш замедления стены относился к текущему
        return ticks;
    }

    /**
     * Идёт к ближайшей к цели клетке, куда можно дойти пешком, и строит столб оттуда. false - строиться негде (основание
     * далеко от цели): тогда остаётся прорубаться.
     */
    private boolean buildUpTowards(LivingEntity target) {
        WitherPathfinder.Approach walk = WitherPathfinder.findWalkingApproach(mob, target);
        if (walk.reachesGoal() || !isWorthPillaring(walk.end(), target)) {
            return false;
        }
        return approachOrPillar(target, walk);
    }

    private boolean followRoute(List<WitherPathfinder.PlannedStep> steps) {
        if (steps.isEmpty()) {
            return false;
        }
        for (WitherPathfinder.PlannedStep step : steps) {
            breakQueue.addAll(step.breakTargets());
        }
        mob.getNavigation().moveTo(buildPath(steps), AbilityConfig.WITHER_BREAK_MOVE_SPEED);
        return true;
    }

    /**
     * Цели не достать ни пешком, ни с ломанием. Сначала моб идёт к ближайшей к ней клетке (без ломания блоков), и только
     * оттуда, если цель выше и рядом, поднимается пилларом: так столб строится у игрока, а не там, где моб остановился.
     */
    private boolean approachOrPillar(LivingEntity target, WitherPathfinder.Approach approach) {
        if (!approach.steps().isEmpty()) {
            approaching = true;
            mob.getNavigation().moveTo(buildPath(approach.steps()), AbilityConfig.WITHER_BREAK_MOVE_SPEED);
            return true;
        }
        return beginPillarIfUseful(target);
    }

    private static Path buildPath(List<WitherPathfinder.PlannedStep> steps) {
        List<Node> nodes = new ArrayList<>(steps.size());
        for (WitherPathfinder.PlannedStep step : steps) {
            BlockPos pos = step.pos();
            Node node = new Node(pos.getX(), pos.getY(), pos.getZ());
            node.type = PathType.WALKABLE;
            nodes.add(node);
        }
        return new Path(nodes, steps.get(steps.size() - 1).pos(), true);
    }

    /**
     * Блок над головой моба, когда цель выше: его ломают отдельно от A*. Клетка на 2 выше ног - это "над головой" только
     * у зомби (рост 1.95). У визер-скелета (2.4, три клетки тела) это ещё клетка самого моба, там блока быть не может,
     * поэтому у него эта проверка ничего не находит и потолок ломать не пытается; следующая клетка выше дальше
     * WITHER_BREAK_REACH от его ног, так что просто сдвинуть проверку нельзя (моб застрял бы на недосягаемом блоке).
     */
    private BlockPos findCeilingObstruction(LivingEntity target) {
        if (target.getY() - mob.getY() < AbilityConfig.WITHER_BREAK_CEILING_MIN_TARGET_HEIGHT_DIFF) return null;

        BlockPos ceiling = mob.blockPosition().above(CEILING_CELLS_ABOVE_FEET);
        BlockState state = mob.level().getBlockState(ceiling);
        return isMineable(state) ? ceiling : null;
    }

    // ------------------------------------------------------------------ пиллар

    /**
     * Подниматься от клетки base к цели имеет смысл, когда цель выше хотя бы на WITHER_PILLAR_MIN_HEIGHT_DIFF и стоит
     * не дальше WITHER_PILLAR_MAX_GAP клеток по горизонтали: тогда на высоте цели она рядом. Столб в стороне ничего не даёт.
     */
    private boolean isWorthPillaring(BlockPos base, LivingEntity target) {
        return target.getY() - base.getY() >= AbilityConfig.WITHER_PILLAR_MIN_HEIGHT_DIFF
                && horizontalGap(base, target.blockPosition()) <= AbilityConfig.WITHER_PILLAR_MAX_GAP;
    }

    private static int horizontalGap(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
    }

    private boolean beginPillarIfUseful(LivingEntity target) {
        if (pillar.exhausted || !isWorthPillaring(mob.blockPosition(), target)) {
            return false;
        }
        pillar.begin(target.getY(), false);
        mob.getNavigation().stop();
        return true;
    }

    /**
     * Принудительный подъём (после ударов игрока, см. WitherBuffHandler.onIncomingDamage): игрок бьёт сверху и рядом,
     * моб не ждёт маршрута, а лезет к нему. Если игрок не выше или не рядом, запрос просто сбрасывается.
     */
    private void maybeBeginForcedPillar() {
        if (!mob.getPersistentData().getBoolean(FORCE_PILLAR_KEY)) return;
        mob.getPersistentData().putBoolean(FORCE_PILLAR_KEY, false);

        LivingEntity target = currentTarget != null ? currentTarget : mob.getTarget();
        if (target == null || !isWorthPillaring(mob.blockPosition(), target)) return;

        clearTarget();
        breakQueue.clear();

        pillar.reset();
        pillar.begin(target.getY(), true);
        mob.getNavigation().stop();
    }

    private void tickPillar() {
        setBreaking(false);
        if (currentTarget == null) {
            pillar.end();
            return;
        }
        mob.getLookControl().setLookAt(currentTarget, 30.0F, 30.0F);

        if (canAttackNow(currentTarget)) {
            pillar.end(); // уже достаёт игрока - достраивать столб не нужно
            return;
        }
        if (horizontalGap(mob.blockPosition(), currentTarget.blockPosition()) > AbilityConfig.WITHER_PILLAR_MAX_GAP + PILLAR_DRIFT_GAP) {
            refreshPlan(currentTarget); // игрок ушёл от столба - план заново
            return;
        }

        pillar.goalY = currentTarget.getY();
        pillar.stalledTicks++;
        if (pillar.blocksPlaced >= AbilityConfig.WITHER_PILLAR_MAX_BLOCKS
                || mob.getY() >= pillar.goalY
                || pillar.stalledTicks > AbilityConfig.WITHER_PILLAR_STALL_TICKS) {
            pillar.exhausted = true;
            refreshPlan(currentTarget);
            return;
        }

        if (mob.onGround()) {
            pillar.origin = mob.blockPosition();
            mob.getJumpControl().jump();
            return;
        }

        if (pillar.origin != null && mob.blockPosition().getY() > pillar.origin.getY()) {
            placePillarBlock();
        }
    }

    private void placePillarBlock() {
        mob.level().setBlockAndUpdate(pillar.origin, AbilityConfig.WITHER_PILLAR_BLOCK.defaultBlockState());
        mob.swing(InteractionHand.OFF_HAND);
        pillar.blocksPlaced++;
        pillar.stalledTicks = 0;
        pillar.origin = null;
        if (!pillar.forced && canWalkOutNow(currentTarget)) {
            refreshPlan(currentTarget);
        }
    }

    /** Лучший маршрут к цели теперь идёт пешком, без единого сломанного блока: пиллар можно заканчивать. */
    private boolean canWalkOutNow(LivingEntity target) {
        List<WitherPathfinder.PlannedStep> plan = WitherPathfinder.findPlan(mob, target);
        return plan != null && plan.stream().allMatch(step -> step.breakTargets().isEmpty());
    }
}
