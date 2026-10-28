package net.gera.veridia.common.worldgen.feature;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Одно большое лавовое озеро: чисто математическое описание (центр, радиус, край, лавовые шипы). Озеро лежит вровень с
 * землёй, как пол: лава заменяет верхний слой земли в каждой колонке, поэтому рельеф и ступени получаются сами из
 * рельефа, а не из выдуманных уровней. Форма озера и шипов считается по этим числам и больше ни от чего не зависит,
 * поэтому каждый чанк строит свою часть сам, а соседние части сходятся. Где стоят озёра, решает {@link LavaLakeField}.
 *
 * @param baseY  Y земли под серединой озера (медиана по области); от него отсчитывается высота шипов
 * @param piles  кучи сырой молибденовой руды, стоящие в лаве между шипами (см. {@link OrePile})
 * @param levels Y лавы в узлах сетки с шагом LEVEL_GRID_STEP вокруг центра (сглаженная высота земли по шуму рельефа);
 *               между узлами уровень интерполируется, поэтому поверхность непрерывна, без ямок на мелких впадинах земли
 */
record LavaLake(int centerX, int centerZ, int radius, int baseY, long seed, Lobes lobes, List<Spike> spikes,
                List<OrePile> piles, int[] levels) {
    // ---------- Настройки ----------
    // Радиус озера (блоков); озеро в поперечнике вдвое больше
    static final int MIN_RADIUS = 32;
    static final int MAX_RADIUS = 46;
    // Волнистость края озера (доля радиуса) и размытие края (блоков)
    static final double LOBE_AMPLITUDE = 0.2D;
    static final double EDGE_JITTER = 1.0D;
    // Ширина магматического берега вокруг лавы (блоков): верхний слой земли у края озера становится магмой вровень с землёй
    static final double SHORE_WIDTH = 2.0D;
    // Шаг сетки уровней лавы (блоков): рельеф плавный, и мелкие бугры и впадины в уровень не попадают
    static final int LEVEL_GRID_STEP = 4;

    // Лавовые шипы в центре: главный шип посередине и шипы поменьше вокруг (высота над baseY, блоков)
    private static final int MAIN_SPIKE_MIN_HEIGHT = 9;
    private static final int MAIN_SPIKE_MAX_HEIGHT = 14;
    private static final int SATELLITES_MIN = 3;
    private static final int SATELLITES_MAX = 5;
    private static final int SATELLITE_MIN_HEIGHT = 4;
    private static final int SATELLITE_MAX_HEIGHT = 8;
    // Шипы поменьше стоят от центра не ближе SATELLITE_MIN_DISTANCE блоков и не дальше этой доли радиуса озера
    private static final int SATELLITE_MIN_DISTANCE = 6;
    private static final double SATELLITE_MAX_RADIUS_SHARE = 0.45D;
    // Радиус основания шипа: доля высоты и минимум (блоков); попыток подобрать место шипу поменьше
    private static final double SPIKE_BASE_RADIUS_PER_HEIGHT = 0.28D;
    private static final double SPIKE_MIN_BASE_RADIUS = 1.5D;
    private static final int SATELLITE_PLACEMENT_ATTEMPTS = 8;

    // Кучи молибденовой руды: не больше одной на озеро и с шансом PILE_CHANCE; ближе скольки блоков к центру озера и дальше какой доли радиуса
    // озера их не ставят, сколько раз пробуется место для каждой, и запас между кучей и соседним шипом или кучей
    private static final double PILE_CHANCE = 0.6D;
    private static final int PILE_MIN_DISTANCE = 12;
    private static final double PILE_MAX_RADIUS_SHARE = 0.55D;
    private static final int PILE_PLACEMENT_ATTEMPTS = 12;
    private static final double PILE_CLEARANCE = 1.0D;
    // Кучи считаются от отдельного генератора (от сида озера), чтобы их появление не сдвигало форму озера и шипов
    private static final long PILE_SALT = 0x4F52455049L;

    /** Лавовый шип: колонна, сужающаяся к вершине. */
    record Spike(int x, int z, int height, double baseRadius) {
        /** Радиус сечения на слое layer (0 - основание). */
        double radiusAt(int layer) {
            return baseRadius * (1.0D - (double) layer / height);
        }
    }

    /** Новое озеро с центром (centerX, centerZ); baseY пока неизвестен (см. {@link #withBaseY}). */
    static LavaLake create(RandomSource random, int centerX, int centerZ) {
        int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        long seed = random.nextLong();
        List<Spike> spikes = createSpikes(random, radius);
        Lobes lobes = Lobes.random(random, LOBE_AMPLITUDE);
        return new LavaLake(centerX, centerZ, radius, 0, seed, lobes, spikes, createPiles(seed, radius, spikes), new int[0]);
    }

    LavaLake withBaseY(int newBaseY) {
        return new LavaLake(centerX, centerZ, radius, newBaseY, seed, lobes, spikes, piles, levels);
    }

    LavaLake withLevels(int[] newLevels) {
        return new LavaLake(centerX, centerZ, radius, baseY, seed, lobes, spikes, piles, newLevels);
    }

    /** Y основания кучи: уровень лавы там, где стоит ось кучи (выше и ниже куча идёт от этого уровня). */
    int pileFootY(OrePile pile) {
        return lavaLevel(centerX + pile.x(), centerZ + pile.z());
    }

    /** На сколько блоков от центра по каждой оси простирается сетка уровней (кратно шагу, с запасом). */
    int levelGridHalf() {
        return (reach() / LEVEL_GRID_STEP + 1) * LEVEL_GRID_STEP;
    }

    /** Сколько узлов сетки уровней по одной оси. */
    int levelGridSize() {
        return 2 * levelGridHalf() / LEVEL_GRID_STEP + 1;
    }

    /** Координата (x или z) узла сетки с номером index по оси, у которой центр озера center. */
    int levelGridCoordinate(int center, int index) {
        return center - levelGridHalf() + index * LEVEL_GRID_STEP;
    }

    /** Y лавы в колонке (x, z): билинейно между узлами сетки, округлено до блока. */
    int lavaLevel(int x, int z) {
        int size = levelGridSize();
        double gridX = Mth.clamp((x - (centerX - levelGridHalf())) / (double) LEVEL_GRID_STEP, 0.0D, size - 1.0D);
        double gridZ = Mth.clamp((z - (centerZ - levelGridHalf())) / (double) LEVEL_GRID_STEP, 0.0D, size - 1.0D);
        int i = Math.min((int) gridX, size - 2);
        int j = Math.min((int) gridZ, size - 2);
        double tx = gridX - i;
        double tz = gridZ - j;
        double near = Mth.lerp(tx, levels[j * size + i], levels[j * size + i + 1]);
        double far = Mth.lerp(tx, levels[(j + 1) * size + i], levels[(j + 1) * size + i + 1]);
        return (int) Math.round(Mth.lerp(tz, near, far));
    }

    // Шипы в координатах от центра озера: главный посередине, остальные вокруг без наложений
    private static List<Spike> createSpikes(RandomSource random, int radius) {
        List<Spike> spikes = new ArrayList<>();
        spikes.add(spike(random, 0, 0, MAIN_SPIKE_MIN_HEIGHT, MAIN_SPIKE_MAX_HEIGHT));
        int satellites = SATELLITES_MIN + random.nextInt(SATELLITES_MAX - SATELLITES_MIN + 1);
        double maxDistance = radius * SATELLITE_MAX_RADIUS_SHARE;
        for (int i = 0; i < satellites; i++) {
            for (int attempt = 0; attempt < SATELLITE_PLACEMENT_ATTEMPTS; attempt++) {
                double distance = SATELLITE_MIN_DISTANCE + random.nextDouble() * (maxDistance - SATELLITE_MIN_DISTANCE);
                double angle = random.nextDouble() * Math.TAU;
                Spike candidate = spike(random, (int) Math.round(Math.cos(angle) * distance),
                        (int) Math.round(Math.sin(angle) * distance), SATELLITE_MIN_HEIGHT, SATELLITE_MAX_HEIGHT);
                if (spikes.stream().noneMatch(other -> overlaps(other, candidate))) {
                    spikes.add(candidate);
                    break;
                }
            }
        }
        return spikes;
    }

    // Кучи руды в координатах от центра озера: на свободных местах между шипами, не у края озера
    private static List<OrePile> createPiles(long lakeSeed, int radius, List<Spike> spikes) {
        RandomSource random = RandomSource.create(lakeSeed ^ PILE_SALT);
        int count = random.nextDouble() < PILE_CHANCE ? 1 : 0;
        double maxDistance = radius * PILE_MAX_RADIUS_SHARE;
        List<OrePile> piles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < PILE_PLACEMENT_ATTEMPTS; attempt++) {
                double distance = PILE_MIN_DISTANCE + random.nextDouble() * (maxDistance - PILE_MIN_DISTANCE);
                double angle = random.nextDouble() * Math.TAU;
                OrePile candidate = OrePile.create(random, (int) Math.round(Math.cos(angle) * distance),
                        (int) Math.round(Math.sin(angle) * distance));
                if (isClearOfSpikesAndPiles(candidate, spikes, piles)) {
                    piles.add(candidate);
                    break;
                }
            }
        }
        return List.copyOf(piles);
    }

    private static boolean isClearOfSpikesAndPiles(OrePile candidate, List<Spike> spikes, List<OrePile> piles) {
        boolean clearOfSpikes = spikes.stream().noneMatch(spike ->
                Math.hypot(candidate.x() - spike.x(), candidate.z() - spike.z())
                        < candidate.reach() + spike.baseRadius() + PILE_CLEARANCE);
        boolean clearOfPiles = piles.stream().noneMatch(other ->
                Math.hypot(candidate.x() - other.x(), candidate.z() - other.z())
                        < candidate.reach() + other.reach() + PILE_CLEARANCE);
        return clearOfSpikes && clearOfPiles;
    }

    private static Spike spike(RandomSource random, int x, int z, int minHeight, int maxHeight) {
        int height = minHeight + random.nextInt(maxHeight - minHeight + 1);
        return new Spike(x, z, height, Math.max(SPIKE_MIN_BASE_RADIUS, height * SPIKE_BASE_RADIUS_PER_HEIGHT));
    }

    private static boolean overlaps(Spike first, Spike second) {
        double distance = Math.hypot(first.x() - second.x(), first.z() - second.z());
        return distance < first.baseRadius() + second.baseRadius() + 1.0D;
    }

    /** Самое дальнее, куда озеро с краем и берегом может доходить от центра по каждой оси (для любого радиуса). */
    static int maxReach() {
        return reachFor(MAX_RADIUS);
    }

    int reach() {
        return reachFor(radius);
    }

    private static int reachFor(int radius) {
        return radius + (int) Math.ceil(EDGE_JITTER + radius * LOBE_AMPLITUDE + SHORE_WIDTH);
    }

    /** Расстояние от колонки (x, z) до края озера: меньше 0 внутри озера. */
    double distanceOutsideEdge(int x, int z) {
        int dx = x - centerX;
        int dz = z - centerZ;
        return Math.hypot(dx, dz) - radius * lobes.scaleAt(dx, dz);
    }

    /** Размытие края в колонке: детерминированный "случайный" сдвиг от -EDGE_JITTER до EDGE_JITTER. */
    double edgeJitter(int x, int z) {
        long hash = Mth.getSeed(x, 0, z) ^ seed;
        double unit = ((hash >>> 16) & 0xFFFFL) / 65535.0D;
        return (unit * 2.0D - 1.0D) * EDGE_JITTER;
    }

    /** Касается ли озеро (с запасом extra блоков вокруг) этого чанка. */
    boolean touchesChunk(ChunkPos chunk, int extra) {
        double dx = Math.max(Math.max(chunk.getMinBlockX() - centerX, centerX - chunk.getMaxBlockX()), 0);
        double dz = Math.max(Math.max(chunk.getMinBlockZ() - centerZ, centerZ - chunk.getMaxBlockZ()), 0);
        return Math.hypot(dx, dz) <= reach() + extra;
    }
}
