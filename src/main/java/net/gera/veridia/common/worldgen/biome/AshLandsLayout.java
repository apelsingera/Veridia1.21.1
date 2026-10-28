package net.gera.veridia.common.worldgen.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Где лежат Пепельные земли в мире с данным сидом. Это круглые области ("пятна") с неровным краем; всё, что внутри,
 * заменяется на пепельные биомы (см. {@link AshLands}). Два вида пятен:
 * <ul>
 *   <li>домашнее - одно, всегда есть: его ближайший к началу координат край ровно в MIN_DISTANCE_FROM_SPAWN блоках;
 *       из HOME_ANGLE_CANDIDATES направлений выбирается то, где внутри пятна больше всего суши, чтобы оно не
 *       оказалось в океане;</li>
 *   <li>дальние - редкие: мир режется на квадраты FAR_CELL_SIZE, в каждом с шансом FAR_CHANCE есть одно пятно;
 *       пятна, чей край ближе MIN_DISTANCE_FROM_SPAWN к началу координат, и пятна, где суши меньше
 *       FAR_MIN_LAND_SHARE, отбрасываются.</li>
 * </ul>
 * Краю добавлена неровность шумом: он только отступает внутрь круга, поэтому ближе MIN_DISTANCE_FROM_SPAWN к спавну
 * пятно не подходит никогда. Спавн мира берётся как начало координат (настоящий спавн ищется около него и зависит
 * от самих биомов, поэтому заранее неизвестен).
 * <p>
 * Внутри пятен равнины и лес лежат секциями: лес там, где второй шум выше FOREST_NOISE_MIN.
 */
public final class  AshLandsLayout {
    // ---------- Настройки ----------
    // Ближайший край любого пятна не ближе стольких блоков к началу координат
    private static final int MIN_DISTANCE_FROM_SPAWN = 2000;
    // Радиус домашнего пятна (блоков); пятно в поперечнике вдвое больше
    private static final int HOME_RADIUS = 1400;
    // Сколько направлений проверяется для домашнего пятна
    private static final int HOME_ANGLE_CANDIDATES = 24;
    // Дальние пятна: размер квадрата мира, шанс пятна в квадрате, пределы радиуса, нужная доля суши
    private static final int FAR_CELL_SIZE = 24000;
    private static final float FAR_CHANCE = 0.5F;
    private static final int FAR_RADIUS_MIN = 3000;
    private static final int FAR_RADIUS_MAX = 6000;
    private static final double FAR_MIN_LAND_SHARE = 0.35D;
    // На какую долю радиуса край пятна может отступить внутрь (0 - ровный круг) и размер неровностей (блоков)
    private static final double EDGE_SHRINK_MAX = 0.3D;
    private static final double EDGE_NOISE_SCALE = 500.0D;
    // У больших пятен неровности крупнее: размер неровностей не меньше этой доли радиуса пятна
    private static final double EDGE_NOISE_RADIUS_SHARE = 0.3D;
    // Секции леса: размер (блоков) и порог шума, выше которого лес (от -1 до 1; выше порог - лесов меньше)
    private static final double FOREST_NOISE_SCALE = 700.0D;
    private static final double FOREST_NOISE_MIN = 0.1D;
    // Плавный переход к равнинному рельефу (блоков): за сколько до края земель он начинается и на каком расстоянии
    // внутри от края становится полным
    private static final double FLATTEN_OUTSIDE = 160.0D;
    private static final double FLATTEN_INSIDE = 128.0D;
    // Жаркий пояс вокруг земель (блоков): на таком расстоянии от края климат начинает смещаться к жаркому и сухому, а у
    // самого края и внутри он полностью такой. Так рядом с пепельными землями лежат пустыни и бесплодные земли, а снежные
    // и холодные биомы отодвигаются, переходя через умеренные.
    private static final double HEAT_BELT_MIN = 600.0D;
    // У больших пятен пояс шире: не меньше этой доли радиуса пятна (чем больше пятно, тем шире жаркая кайма и больше
    // жаркой и сухой суши вокруг)
    private static final double HEAT_BELT_RADIUS_SHARE = 0.5D;
    // Оценка суши в пятне: точек от центра до края по каждой оси (всего примерно (2n+1)^2 внутри круга)
    private static final int LAND_PROBE_STEPS = 4;

    // Соли сида: чтобы шумы и расположение не повторяли друг друга
    private static final long HOME_SALT = 0x4153484C414E4431L;
    private static final long FAR_SALT = 0x4153484C414E4432L;
    private static final long EDGE_SALT = 0x4153484C414E4433L;
    private static final long FOREST_SALT = 0x4153484C414E4434L;

    /** Суша ли в блоке (x, z): по климату мира, без построения самого рельефа. */
    @FunctionalInterface
    public interface LandProbe {
        boolean isLand(int x, int z);
    }

    /** Круглое пятно: центр и радиус в блоках. */
    private record Blob(double centerX, double centerZ, double radius) {
        double distanceSquaredTo(double x, double z) {
            double dx = x - centerX;
            double dz = z - centerZ;
            return dx * dx + dz * dz;
        }
    }

    private final long seed;
    private final LandProbe landProbe;
    private final Blob home;
    private final SimplexNoise edgeNoise;
    private final SimplexNoise forestNoise;
    // Дальние пятна по квадратам мира; пустое значение - в квадрате пятна нет
    private final Map<Long, Optional<Blob>> farBlobs = new ConcurrentHashMap<>();

    private AshLandsLayout(long seed, LandProbe landProbe) {
        this.seed = seed;
        this.landProbe = landProbe;
        this.home = homeBlob();
        this.edgeNoise = new SimplexNoise(RandomSource.create(seed ^ EDGE_SALT));
        this.forestNoise = new SimplexNoise(RandomSource.create(seed ^ FOREST_SALT));
    }

    public static AshLandsLayout create(long seed, LandProbe landProbe) {
        return new AshLandsLayout(seed, landProbe);
    }

    /** Центр домашнего пятна (блоки): куда идти от спавна, чтобы точно найти Пепельные земли. */
    public BlockPos homeCenter() {
        return BlockPos.containing(home.centerX(), 0, home.centerZ());
    }

    /** Описание пятна для карт и отладки: центр, радиус (блоки) и домашнее ли оно. */
    public record BlobInfo(double centerX, double centerZ, double radius, boolean home) {
    }

    /** Все пятна, центры которых лежат в квадрате со стороной 2 * half вокруг начала координат (домашнее первым). */
    public java.util.List<BlobInfo> blobs(int half) {
        java.util.List<BlobInfo> result = new java.util.ArrayList<>();
        result.add(new BlobInfo(home.centerX(), home.centerZ(), home.radius(), true));
        for (int cellX = Math.floorDiv(-half, FAR_CELL_SIZE); cellX <= Math.floorDiv(half, FAR_CELL_SIZE); cellX++) {
            for (int cellZ = Math.floorDiv(-half, FAR_CELL_SIZE); cellZ <= Math.floorDiv(half, FAR_CELL_SIZE); cellZ++) {
                int x = cellX * FAR_CELL_SIZE;
                int z = cellZ * FAR_CELL_SIZE;
                farBlobAt(x, z).ifPresent(blob -> result.add(new BlobInfo(blob.centerX(), blob.centerZ(), blob.radius(), false)));
            }
        }
        return result;
    }

    /** Лежит ли блок (x, z) в Пепельных землях. */
    public boolean contains(int x, int z) {
        // Почти весь мир отсекается сразу: ближе MIN_DISTANCE_FROM_SPAWN пятен нет
        if ((double) x * x + (double) z * z < (double) MIN_DISTANCE_FROM_SPAWN * MIN_DISTANCE_FROM_SPAWN) {
            return false;
        }
        return covers(home, x, z) || farBlobAt(x, z).map(blob -> covers(blob, x, z)).orElse(false);
    }

    /**
     * Насколько рельеф в блоке (x, z) должен быть равнинным: 0 - обычный рельеф мира, 1 - ровные равнины. Растёт
     * плавно: начинается за FLATTEN_OUTSIDE блоков до края Пепельных земель, у самого края около 0.6, а внутри на
     * расстоянии FLATTEN_INSIDE от края достигает 1. Поэтому у границы равнины переходят в обычные горы склоном, а не
     * обрывом.
     */
    public double flatness(int x, int z) {
        double homeWeight = flatnessAround(home, x, z);
        return Math.max(homeWeight, farBlobAt(x, z).map(blob -> flatnessAround(blob, x, z)).orElse(0.0D));
    }

    private double flatnessAround(Blob blob, int x, int z) {
        double distance = Math.sqrt(blob.distanceSquaredTo(x, z));
        if (distance > blob.radius() + FLATTEN_OUTSIDE) {
            return 0.0D;
        }
        double reach = blob.radius() * (1.0D - EDGE_SHRINK_MAX * edgeShrink01(blob, x, z));
        double progress = (reach - distance + FLATTEN_OUTSIDE) / (FLATTEN_OUTSIDE + FLATTEN_INSIDE);
        double clamped = Mth.clamp(progress, 0.0D, 1.0D);
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    /**
     * Насколько климат в блоке (x, z) смещён к жаркому и сухому: 0 - обычный климат мира, 1 - полностью жаркий и сухой.
     * Равен 1 у края Пепельных земель и внутри и плавно спадает до 0 за ширину пояса (heatBelt) блоков от края.
     */
    public double heat(int x, int z) {
        double homeHeat = heatAround(home, x, z);
        return Math.max(homeHeat, farBlobAt(x, z).map(blob -> heatAround(blob, x, z)).orElse(0.0D));
    }

    // Ширина жаркого пояса вокруг пятна радиуса radius (блоков)
    private static double heatBelt(double radius) {
        return Math.max(HEAT_BELT_MIN, radius * HEAT_BELT_RADIUS_SHARE);
    }

    private double heatAround(Blob blob, int x, int z) {
        double distance = Math.sqrt(blob.distanceSquaredTo(x, z));
        double belt = heatBelt(blob.radius());
        if (distance > blob.radius() + belt) {
            return 0.0D;
        }
        double reach = blob.radius() * (1.0D - EDGE_SHRINK_MAX * edgeShrink01(blob, x, z));
        double clamped = Mth.clamp((reach - distance + belt) / belt, 0.0D, 1.0D);
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    /** Лес ли в этой точке Пепельных земель (иначе равнины). */
    public boolean isForest(int x, int z) {
        return forestNoise.getValue(x / FOREST_NOISE_SCALE, z / FOREST_NOISE_SCALE) > FOREST_NOISE_MIN;
    }

    private boolean covers(Blob blob, int x, int z) {
        double distanceSquared = blob.distanceSquaredTo(x, z);
        if (distanceSquared > blob.radius() * blob.radius()) {
            return false;
        }
        double reach = blob.radius() * (1.0D - EDGE_SHRINK_MAX * edgeShrink01(blob, x, z));
        return distanceSquared <= reach * reach;
    }

    // Насколько край отступает в этой точке: от 0 (круг) до 1 (на EDGE_SHRINK_MAX радиуса)
    private double edgeShrink01(Blob blob, int x, int z) {
        double scale = Math.max(EDGE_NOISE_SCALE, blob.radius() * EDGE_NOISE_RADIUS_SHARE);
        double noise = edgeNoise.getValue(x / scale, z / scale);
        return Mth.clamp((noise + 1.0D) / 2.0D, 0.0D, 1.0D);
    }

    // Домашнее пятно: из направлений, равномерно расставленных от случайного начального, берётся то, где больше суши
    private Blob homeBlob() {
        double startAngle = RandomSource.create(seed ^ HOME_SALT).nextDouble() * Math.PI * 2.0D;
        double centerDistance = MIN_DISTANCE_FROM_SPAWN + HOME_RADIUS;
        Blob best = null;
        double bestShare = -1.0D;
        for (int i = 0; i < HOME_ANGLE_CANDIDATES; i++) {
            double angle = startAngle + Math.PI * 2.0D * i / HOME_ANGLE_CANDIDATES;
            Blob candidate = new Blob(Math.cos(angle) * centerDistance, Math.sin(angle) * centerDistance, HOME_RADIUS);
            double share = landShare(candidate);
            if (share > bestShare) {
                best = candidate;
                bestShare = share;
            }
        }
        return best;
    }

    // Доля суши в пятне: по сетке точек внутри круга
    private double landShare(Blob blob) {
        int land = 0;
        int total = 0;
        for (int i = -LAND_PROBE_STEPS; i <= LAND_PROBE_STEPS; i++) {
            for (int j = -LAND_PROBE_STEPS; j <= LAND_PROBE_STEPS; j++) {
                double dx = blob.radius() * i / LAND_PROBE_STEPS;
                double dz = blob.radius() * j / LAND_PROBE_STEPS;
                if (dx * dx + dz * dz > blob.radius() * blob.radius()) {
                    continue;
                }
                total++;
                if (landProbe.isLand((int) (blob.centerX() + dx), (int) (blob.centerZ() + dz))) {
                    land++;
                }
            }
        }
        return (double) land / total;
    }

    private Optional<Blob> farBlobAt(int x, int z) {
        int cellX = Math.floorDiv(x, FAR_CELL_SIZE);
        int cellZ = Math.floorDiv(z, FAR_CELL_SIZE);
        long key = (cellX & 0xFFFFFFFFL) | ((long) cellZ << 32);
        // Не computeIfAbsent: создание пятна обращается к климату мира, а он снова может спросить это же пятно
        // (computeIfAbsent тогда падает с "Recursive update"). Пятно детерминировано, так что лишний пересчёт безвреден.
        Optional<Blob> cached = farBlobs.get(key);
        if (cached == null) {
            cached = createFarBlob(cellX, cellZ);
            Optional<Blob> existing = farBlobs.putIfAbsent(key, cached);
            if (existing != null) {
                cached = existing;
            }
        }
        return cached;
    }

    // Пятно квадрата (или ничего): центр внутри квадрата с отступом на максимальный радиус и на самый широкий пояс
    // вокруг (жар или выравнивание рельефа), чтобы ничто из этого не вылезало в соседние квадраты, где пятно не ищется.
    // Отбрасываются пятна ближе MIN_DISTANCE_FROM_SPAWN и почти без суши.
    private Optional<Blob> createFarBlob(int cellX, int cellZ) {
        RandomSource random = RandomSource.create(seed ^ FAR_SALT ^ Mth.getSeed(cellX, 0, cellZ));
        if (random.nextFloat() >= FAR_CHANCE) {
            return Optional.empty();
        }
        double radius = FAR_RADIUS_MIN + random.nextDouble() * (FAR_RADIUS_MAX - FAR_RADIUS_MIN);
        double margin = FAR_RADIUS_MAX + Math.max(heatBelt(FAR_RADIUS_MAX), FLATTEN_OUTSIDE);
        double room = FAR_CELL_SIZE - 2.0D * margin;
        double centerX = (double) cellX * FAR_CELL_SIZE + margin + random.nextDouble() * room;
        double centerZ = (double) cellZ * FAR_CELL_SIZE + margin + random.nextDouble() * room;
        Blob blob = new Blob(centerX, centerZ, radius);
        boolean tooClose = Math.hypot(centerX, centerZ) - radius < MIN_DISTANCE_FROM_SPAWN;
        boolean tooWet = !tooClose && landShare(blob) < FAR_MIN_LAND_SHARE;
        return tooClose || tooWet ? Optional.empty() : Optional.of(blob);
    }
}
