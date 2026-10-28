package net.gera.veridia.common.block.spawner;

import net.gera.veridia.common.block.BloodyRunestoneBlock;
import net.gera.veridia.common.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;

/**
 * Улучшение спавнера кровавыми рунными камнями рядом с ним. Каждый камень в радиусе RADIUS блоков добавляет
 * свой прирост (первый - первую строку STEPS, второй - вторую и так далее), приросты складываются; камней
 * больше, чем строк в STEPS, ничего не добавляют. Двухблочный камень считается один раз (по нижней половине).
 * <p>
 * Прирост даёт: прочность блока спавнера ({@link SpawnerHardnessHandler}) и максимум мобов рядом со спавнером
 * (BaseSpawnerMixin: к ванильному MaxNearbyEntities).
 */
public final class RunestoneSpawnerBoost {
    /**
     * Прибавка за один камень: к прочности блока спавнера, к максимуму мобов вокруг него и к скорости спавна
     * (spawnSpeed - на сколько быстрее идёт отсчёт паузы между спавнами: 0.25 - на четверть быстрее, 1.0 - вдвое;
     * spawnCount - на сколько больше мобов спавнится за одну попытку, ванильный спавнер: 4).
     */
    public record Boost(float hardness, int maxMobs, float spawnSpeed, int spawnCount) {
        public static final Boost NONE = new Boost(0.0F, 0, 0.0F, 0);

        /** Есть ли рядом хотя бы один камень (любой камень даёт какой-то прирост). */
        public boolean isActive() {
            return hardness > 0.0F || maxMobs > 0 || spawnSpeed > 0.0F || spawnCount > 0;
        }

        Boost plus(Boost other) {
            return new Boost(hardness + other.hardness, maxMobs + other.maxMobs,
                    spawnSpeed + other.spawnSpeed, spawnCount + other.spawnCount);
        }
    }

    // Радиус действия камня (блоков, по прямой от центра спавнера до центра камня)
    public static final int RADIUS = 9;

    // Прирост по порядку камней (прочность блока / макс. мобов / скорость спавна / мобов за попытку):
    // 1-й +4, +1, +25%, +1; 2-й +6, +2, +25%, +0; 3-й +6, +1, +25%, +1; 4-й +6, +1, +25%, +0.
    // Итого за 1 / 2 / 3 / 4 камня: прочность +4/+10/+16/+22 (ваниль 5), мобов вокруг +1/+3/+4/+5 (ваниль 6),
    // скорость спавна x1.25 / x1.5 / x1.75 / x2 (пауза между спавнами 10-40 секунд в ванили),
    // мобов за попытку +1/+1/+2/+2 (ваниль 4, то есть не больше 6). Лимит мобов вокруг всё равно не даёт им накопиться.
    private static final List<Boost> STEPS = List.of(
            new Boost(4.0F, 1, 0.25F, 1),
            new Boost(6.0F, 2, 0.25F, 0),
            new Boost(6.0F, 1, 0.25F, 1),
            new Boost(6.0F, 1, 0.25F, 0));

    private RunestoneSpawnerBoost() {
    }

    /** Суммарное улучшение спавнера в точке pos по числу камней рядом. */
    public static Boost forSpawner(Level level, BlockPos pos) {
        return forStones(countStones(level, pos));
    }

    static Boost forStones(int stones) {
        Boost total = Boost.NONE;
        for (int i = 0; i < Math.min(stones, STEPS.size()); i++) {
            total = total.plus(STEPS.get(i));
        }
        return total;
    }

    // Нижние половины камней в шаре радиуса RADIUS; незагруженные чанки пропускаем, чтобы подсчёт их не грузил
    private static int countStones(Level level, BlockPos center) {
        int stones = 0;
        int radiusSquared = RADIUS * RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS), center.offset(RADIUS, RADIUS, RADIUS))) {
            if (pos.distSqr(center) > radiusSquared || !level.hasChunkAt(pos)) {
                continue;
            }
            if (isRunestoneBase(level.getBlockState(pos))) {
                stones++;
            }
        }
        return stones;
    }

    private static boolean isRunestoneBase(BlockState state) {
        return state.is(ModBlocks.BLOODY_RUNESTONE.get()) && state.getValue(BloodyRunestoneBlock.HALF) == DoubleBlockHalf.LOWER;
    }
}
