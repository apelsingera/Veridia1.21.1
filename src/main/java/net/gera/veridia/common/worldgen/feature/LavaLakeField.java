package net.gera.veridia.common.worldgen.feature;

import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Где стоят большие лавовые озёра. Мир режется на квадраты CELL_SIZE; в каждом с шансом CELL_CHANCE есть одно озеро,
 * если для него нашлось место: Пепельные равнины и достаточно ровная земля (по шуму мира, а не по уже построенному
 * рельефу). Выбор зависит только от сида и шума мира, поэтому любой чанк, в какой бы последовательности ни строился
 * мир, получает один и тот же ответ, а озеро, размером в десятки чанков, сходится на границах чанков.
 * <p>
 * Центр озера держится от границ квадрата дальше его максимального выноса, поэтому озеро лежит в своём квадрате
 * целиком, а чанку нужно знать про один квадрат.
 */
final class LavaLakeField {
    // ---------- Настройки ----------
    // Размер квадрата мира (блоков) и шанс озера в квадрате
    private static final int CELL_SIZE = 360;
    private static final float CELL_CHANCE = 0.9F;
    // Сколько мест перебирается в квадрате, пока не найдётся подходящее
    private static final int ATTEMPTS_PER_CELL = 16;
    // Самый большой перепад высот земли в области озера (блоков), при котором озеро ещё ставится
    private static final int MAX_RELIEF = 8;
    // Озеро не ставится у воды: земля не ниже уровня моря на столько блоков
    private static final int SEA_LEVEL_MARGIN = 3;
    // Проверка местности: точек от центра до края области по каждой оси (всего примерно (2n+1)^2 внутри круга) и
    // какую часть радиуса озера занимает проверяемая область
    private static final int TERRAIN_PROBE_STEPS = 3;
    private static final double TERRAIN_PROBE_SHARE = 0.9D;
    // Пепельные равнины: сколько из точек (центр и 8 по кругу на PLAINS_PROBE_SHARE радиуса) должно в них лежать
    private static final int PLAINS_PROBES_ON_RING = 8;
    private static final double PLAINS_PROBE_SHARE = 0.7D;
    private static final double PLAINS_MIN_SHARE = 0.6D;

    private static final long CELL_SALT = 0x4C41564C414B4531L;

    private record CellKey(long seed, int cellX, int cellZ) {
    }

    // Озёра уже просмотренных квадратов (пусто - в квадрате озера нет); посчитанное не меняется, пока жив мир
    private static final Map<CellKey, Optional<LavaLake>> SITES = new ConcurrentHashMap<>();

    private LavaLakeField() {
    }

    /** Озеро квадрата, в котором лежит блок (x, z), или пусто, если там озера нет. */
    static Optional<LavaLake> siteAt(WorldGenLevel level, ChunkGenerator generator, int x, int z) {
        CellKey key = new CellKey(level.getSeed(), Math.floorDiv(x, CELL_SIZE), Math.floorDiv(z, CELL_SIZE));
        // Не computeIfAbsent: поиск места тяжёлый и обращается к рельефу и биомам мира, которые сами могут спросить
        // раскладку озёр (computeIfAbsent тогда падает с "Recursive update" или держит других). Результат
        // детерминирован, так что лишний пересчёт при гонке потоков безвреден.
        Optional<LavaLake> cached = SITES.get(key);
        if (cached == null) {
            cached = findSite(level, generator, key);
            Optional<LavaLake> existing = SITES.putIfAbsent(key, cached);
            if (existing != null) {
                cached = existing;
            }
        }
        return cached;
    }

    private static Optional<LavaLake> findSite(WorldGenLevel level, ChunkGenerator generator, CellKey cell) {
        RandomSource random = RandomSource.create(cell.seed() ^ CELL_SALT ^ Mth.getSeed(cell.cellX(), 0, cell.cellZ()));
        if (random.nextFloat() >= CELL_CHANCE) {
            return Optional.empty();
        }
        RandomState randomState = level.getLevel().getChunkSource().randomState();
        int margin = LavaLake.maxReach() + 1;
        int room = CELL_SIZE - 2 * margin;
        for (int attempt = 0; attempt < ATTEMPTS_PER_CELL; attempt++) {
            int centerX = cell.cellX() * CELL_SIZE + margin + random.nextInt(room);
            int centerZ = cell.cellZ() * CELL_SIZE + margin + random.nextInt(room);
            LavaLake candidate = LavaLake.create(random, centerX, centerZ);
            Optional<Integer> baseY = suitableBaseY(level, generator, randomState, candidate);
            if (baseY.isPresent()) {
                LavaLake placed = candidate.withBaseY(baseY.get());
                return Optional.of(placed.withLevels(levelGrid(level, generator, randomState, placed)));
            }
        }
        return Optional.empty();
    }

    // Высоты земли по шуму мира в узлах сетки под озером: уровни лавы. Шум мира, а не построенный рельеф, чтобы все
    // чанки озера получили одно и то же, как бы ни строился мир.
    private static int[] levelGrid(WorldGenLevel level, ChunkGenerator generator, RandomState randomState, LavaLake lake) {
        int size = lake.levelGridSize();
        int[] levels = new int[size * size];
        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                levels[j * size + i] = groundHeight(level, generator, randomState,
                        lake.levelGridCoordinate(lake.centerX(), i), lake.levelGridCoordinate(lake.centerZ(), j));
            }
        }
        return levels;
    }

    // Уровень лавы для озера (середина высот земли под ним) или пусто, если место не годится: не равнины, неровно или у воды
    private static Optional<Integer> suitableBaseY(WorldGenLevel level, ChunkGenerator generator,
                                                   RandomState randomState, LavaLake lake) {
        if (!isMostlyPlains(level, generator, randomState, lake)) {
            return Optional.empty();
        }
        List<Integer> heights = terrainHeights(level, generator, randomState, lake);
        int lowest = Collections.min(heights);
        int highest = Collections.max(heights);
        boolean flat = highest - lowest <= MAX_RELIEF;
        int median = heights.get(heights.size() / 2);
        boolean aboveWater = median >= generator.getSeaLevel() + SEA_LEVEL_MARGIN;
        return flat && aboveWater ? Optional.of(median) : Optional.empty();
    }

    // Высоты земли (Y верхнего твёрдого блока) по сетке внутри круга под озером, по возрастанию
    private static List<Integer> terrainHeights(WorldGenLevel level, ChunkGenerator generator,
                                                RandomState randomState, LavaLake lake) {
        double span = lake.radius() * TERRAIN_PROBE_SHARE;
        List<Integer> heights = new ArrayList<>();
        for (int i = -TERRAIN_PROBE_STEPS; i <= TERRAIN_PROBE_STEPS; i++) {
            for (int j = -TERRAIN_PROBE_STEPS; j <= TERRAIN_PROBE_STEPS; j++) {
                double dx = span * i / TERRAIN_PROBE_STEPS;
                double dz = span * j / TERRAIN_PROBE_STEPS;
                if (dx * dx + dz * dz <= span * span) {
                    heights.add(groundHeight(level, generator, randomState,
                            lake.centerX() + (int) dx, lake.centerZ() + (int) dz));
                }
            }
        }
        Collections.sort(heights);
        return heights;
    }

    private static boolean isMostlyPlains(WorldGenLevel level, ChunkGenerator generator,
                                          RandomState randomState, LavaLake lake) {
        int plains = isPlains(level, generator, randomState, lake.centerX(), lake.centerZ()) ? 1 : 0;
        double ring = lake.radius() * PLAINS_PROBE_SHARE;
        for (int i = 0; i < PLAINS_PROBES_ON_RING; i++) {
            double angle = Math.TAU * i / PLAINS_PROBES_ON_RING;
            int x = lake.centerX() + (int) Math.round(Math.cos(angle) * ring);
            int z = lake.centerZ() + (int) Math.round(Math.sin(angle) * ring);
            if (isPlains(level, generator, randomState, x, z)) {
                plains++;
            }
        }
        return plains >= Math.ceil((PLAINS_PROBES_ON_RING + 1) * PLAINS_MIN_SHARE);
    }

    private static boolean isPlains(WorldGenLevel level, ChunkGenerator generator, RandomState randomState, int x, int z) {
        int y = groundHeight(level, generator, randomState, x, z);
        return generator.getBiomeSource()
                .getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), randomState.sampler())
                .is(ModBiomes.ASHEN_PLAINS);
    }

    // Y верхнего твёрдого блока земли в колонке по шуму мира
    private static int groundHeight(WorldGenLevel level, ChunkGenerator generator, RandomState randomState, int x, int z) {
        return generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState) - 1;
    }
}
