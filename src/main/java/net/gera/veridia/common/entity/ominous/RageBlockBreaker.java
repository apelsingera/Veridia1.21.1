package net.gera.veridia.common.entity.ominous;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Ломает блоки на пути разъярённого воина, пока он упирается в препятствие по дороге к цели. Ломает по одному блоку
 * из столба перед собой (воин ростом в три блока), снизу вверх, с трещинами, как игрок; время зависит от твёрдости
 * блока, выпадает он тоже как у игрока. Ставить блоки воин не умеет.
 * <p>
 * Не трогает: слишком твёрдые ({@link #MAX_HARDNESS}: обсидиан, рунные камни), неразрушимые, блоки с содержимым
 * (сундуки, печи и прочие блок-сущности), жидкости. При выключенном правиле mobGriefing ничего не ломает.
 */
final class RageBlockBreaker {
    // Самый твёрдый блок, который ещё ломается: камень 1.5, глубинный сланец 3, железный блок 5, обсидиан 50
    private static final float MAX_HARDNESS = 20.0F;
    private static final int BASE_BREAK_TICKS = 10;
    private static final float BREAK_TICKS_PER_HARDNESS = 8.0F;
    private static final int COLUMN_HEIGHT = 3;
    private static final int HIT_SOUND_INTERVAL_TICKS = 4;

    private final Mob mob;
    @Nullable
    private BlockPos breakingPos;
    private int breakingTicks;
    private int breakingDuration;

    RageBlockBreaker(Mob mob) {
        this.mob = mob;
    }

    /** Ещё тик ломания препятствия на пути к точке towards. */
    void tick(Vec3 towards) {
        if (!(mob.level() instanceof ServerLevel level) || !EventHooks.canEntityGrief(level, mob)) {
            stop();
            return;
        }
        BlockPos obstacle = findObstacle(level, towards);
        if (obstacle == null) {
            stop();
            return;
        }
        BlockState state = level.getBlockState(obstacle);
        if (!obstacle.equals(breakingPos)) {
            stop();
            breakingPos = obstacle;
            breakingDuration = BASE_BREAK_TICKS + Math.round(state.getDestroySpeed(level, obstacle) * BREAK_TICKS_PER_HARDNESS);
        }
        breakingTicks++;
        if (breakingTicks >= breakingDuration) {
            finishBreaking(level, obstacle, state);
            return;
        }
        level.destroyBlockProgress(mob.getId(), obstacle, breakingTicks * 10 / breakingDuration);
        if (breakingTicks % HIT_SOUND_INTERVAL_TICKS == 0) {
            level.playSound(null, obstacle, state.getSoundType(level, obstacle, mob).getHitSound(), SoundSource.HOSTILE, 0.8F, 0.6F);
        }
    }

    void stop() {
        if (breakingPos != null && mob.level() instanceof ServerLevel level) {
            level.destroyBlockProgress(mob.getId(), breakingPos, -1);
        }
        breakingPos = null;
        breakingTicks = 0;
    }

    private void finishBreaking(ServerLevel level, BlockPos pos, BlockState state) {
        level.destroyBlockProgress(mob.getId(), pos, -1);
        if (EventHooks.onEntityDestroyBlock(mob, pos, state)) {
            level.destroyBlock(pos, true, mob);
        }
        breakingPos = null;
        breakingTicks = 0;
    }

    // Столб блоков прямо перед мобом в сторону цели: первый (снизу), который мешает пройти и который можно сломать
    @Nullable
    private BlockPos findObstacle(ServerLevel level, Vec3 towards) {
        Vec3 direction = towards.subtract(mob.position());
        Direction facing = Direction.getNearest(direction.x, 0.0D, direction.z);
        BlockPos front = mob.blockPosition().relative(facing);
        for (int dy = 0; dy < COLUMN_HEIGHT; dy++) {
            BlockPos pos = front.above(dy);
            if (canBreak(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private boolean canBreak(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()
                || state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0.0F && hardness <= MAX_HARDNESS && state.canEntityDestroy(level, pos, mob);
    }
}
