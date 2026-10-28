package net.gera.veridia.common.entity.ominous;

import net.gera.veridia.common.block.RunestonePoiTypes;
import net.gera.veridia.common.block.SpiritualRunestoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Молитва у духовного рунного камня. Воин без цели (и не в ярости), который давно не молился
 * ({@link OminousWarriorEntity#canPrayNow}), находит ближайший камень в {@link #SEARCH_RADIUS} блоках, который сейчас
 * принимает молитвы ({@link SpiritualRunestoneBlock#acceptsPrayers}), подходит,
 * встаёт к нему лицом и проигрывает анимацию pray. Дослушал молитву до конца - камень получает молитву.
 * Если за это время появилась цель, молитва прерывается и не засчитывается.
 */
class OminousWarriorPrayGoal extends Goal {
    private static final int SEARCH_RADIUS = 24;
    private static final int SEARCH_INTERVAL_TICKS = 40;
    // Встаёт на соседнюю с камнем клетку со своей стороны; дошёл - если центр камня ближе этого (по горизонтали)
    private static final double ARRIVE_DISTANCE = 2.6D;
    private static final double WALK_SPEED_MODIFIER = 1.0D;
    private static final int REPATH_INTERVAL_TICKS = 20;
    // Не дошёл за это время (застрял) - бросает и пробует снова не раньше, чем через RETRY_DELAY_TICKS
    private static final int APPROACH_TIMEOUT_TICKS = 400;
    private static final int RETRY_DELAY_TICKS = 1200;
    // Длина анимации pray (1.5 с)
    static final int PRAY_DURATION_TICKS = 30;
    private static final float FACE_TURN_DEGREES_PER_TICK = 30.0F;

    private final OminousWarriorEntity warrior;
    @Nullable
    private BlockPos stonePos;
    private boolean praying;
    private int ticks;
    private boolean finished;
    private long nextSearchGameTime;

    OminousWarriorPrayGoal(OminousWarriorEntity warrior) {
        this.warrior = warrior;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(warrior.level() instanceof ServerLevel level) || !isFree() || level.getGameTime() < nextSearchGameTime) {
            return false;
        }
        nextSearchGameTime = level.getGameTime() + SEARCH_INTERVAL_TICKS;
        // Только камни, которые сейчас принимают молитвы (не во время ярости и не на отдыхе после неё)
        Optional<BlockPos> stone = level.getPoiManager().findClosest(
                type -> type.is(RunestonePoiTypes.SPIRITUAL_RUNESTONE.getKey()),
                pos -> SpiritualRunestoneBlock.acceptsPrayers(level, pos),
                warrior.blockPosition(), SEARCH_RADIUS, PoiManager.Occupancy.ANY);
        stone.ifPresent(pos -> stonePos = pos);
        return stone.isPresent();
    }

    // Свободен: жив, без цели, не в ярости и давно не молился
    private boolean isFree() {
        return warrior.isAlive() && warrior.getTarget() == null && !warrior.isRaging() && warrior.canPrayNow();
    }

    @Override
    public boolean canContinueToUse() {
        return !finished && stonePos != null && isFree() && stoneAcceptsPrayers();
    }

    @Override
    public void start() {
        praying = false;
        finished = false;
        ticks = 0;
        walkToStone();
    }

    @Override
    public void stop() {
        warrior.getNavigation().stop();
        stonePos = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        if (praying) {
            tickPraying();
        } else {
            tickApproach();
        }
    }

    private void tickApproach() {
        if (horizontalDistanceToStone() <= ARRIVE_DISTANCE) {
            beginPraying();
            return;
        }
        if (ticks >= APPROACH_TIMEOUT_TICKS) {
            nextSearchGameTime = warrior.level().getGameTime() + RETRY_DELAY_TICKS;
            finished = true;
            return;
        }
        if (ticks % REPATH_INTERVAL_TICKS == 0 || warrior.getNavigation().isDone()) {
            walkToStone();
        }
    }

    private void beginPraying() {
        praying = true;
        ticks = 0;
        warrior.getNavigation().stop();
        warrior.turnTowards(stoneCenter(), 180.0F);
        warrior.playPrayAnimation();
    }

    private void tickPraying() {
        warrior.getNavigation().stop();
        warrior.turnTowards(stoneCenter(), FACE_TURN_DEGREES_PER_TICK);
        if (ticks >= PRAY_DURATION_TICKS) {
            if (warrior.level() instanceof ServerLevel level && stonePos != null
                    && SpiritualRunestoneBlock.addPrayer(level, stonePos)) {
                warrior.markPrayed();
            }
            finished = true;
        }
    }

    // Путь ровно до соседней клетки (с точностью 1 навигация останавливалась на клетку дальше и не доходила);
    // если туда не пройти - хотя бы рядом с ней
    private void walkToStone() {
        Vec3 away = warrior.position().subtract(stoneCenter());
        BlockPos spot = stonePos.relative(Direction.getNearest(away.x, 0.0D, away.z));
        Path path = warrior.getNavigation().createPath(spot, 0);
        if (path == null) {
            path = warrior.getNavigation().createPath(spot, 1);
        }
        warrior.getNavigation().moveTo(path, WALK_SPEED_MODIFIER);
    }

    private boolean stoneAcceptsPrayers() {
        return warrior.level() instanceof ServerLevel level && SpiritualRunestoneBlock.acceptsPrayers(level, stonePos);
    }

    private Vec3 stoneCenter() {
        return Vec3.atBottomCenterOf(stonePos);
    }

    private double horizontalDistanceToStone() {
        Vec3 center = stoneCenter();
        double dx = center.x - warrior.getX();
        double dz = center.z - warrior.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
