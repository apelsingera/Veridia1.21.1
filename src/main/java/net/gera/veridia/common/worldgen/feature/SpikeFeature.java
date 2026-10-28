package net.gera.veridia.common.worldgen.feature;

import com.mojang.serialization.Codec;
import net.gera.veridia.common.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Группа каменных шипов. Шип - массивная у основания колонна, которая сужается к игольчатой вершине:
 * сечение неровное (волнистый край), каждый слой чуть шершавый, ось слегка наклонена. Основание
 * достраивается вниз по неровной земле, а у подножия рассыпаны камни. Origin - твёрдый блок земли
 * (тег ASH_WASTES_GROUND). Вокруг главного шипа ставятся шипы поменьше: их земля берётся по карте высот.
 * Рядом с лавовыми озёрами (LAKE_CLEARANCE блоков от края) обычные шипы не растут: там свои, лавовые шипы.
 */
public class SpikeFeature extends Feature<SpikeConfiguration> {
    // Радиус основания: доля высоты, минимум и разброс (доля в обе стороны)
    private static final double BASE_RADIUS_PER_HEIGHT = 0.22D;
    private static final double MIN_BASE_RADIUS = 2.0D;
    private static final double BASE_RADIUS_VARIATION = 0.15D;
    // Острота сужения: 1 - ровный конус, больше - тоньше к вершине и вогнутые бока
    private static final double TAPER_POWER = 1.0D;
    // Неровность сечения (амплитуда волнистого края) и шершавость слоёв (разброс радиуса слоя в обе стороны)
    private static final double CROSS_SECTION_ROUGHNESS = 0.06D;
    private static final double LAYER_ROUGHNESS = 0.04D;
    // Наклон: на сколько блоков вершина уходит в сторону, максимум на блок высоты
    private static final double MAX_LEAN_PER_HEIGHT = 0.0D;
    // Насколько глубоко основание достраивается вниз по склону
    private static final int ANCHOR_DEPTH = 4;

    // Шипы поменьше: расстояние до главного шипа, доля его высоты и минимальная высота
    private static final int SATELLITE_MIN_DISTANCE = 3;
    private static final int SATELLITE_MAX_DISTANCE = 8;
    private static final double SATELLITE_MIN_HEIGHT_SHARE = 0.3D;
    private static final double SATELLITE_MAX_HEIGHT_SHARE = 0.75D;
    private static final int SATELLITE_MIN_HEIGHT = 4;

    // Камни у подножия шипа: сколько штук и на какую ширину они рассыпаны от края основания
    private static final int RUBBLE_COUNT = 8;
    private static final double RUBBLE_SPREAD = 3.0D;

    // Обычные шипы не растут у лавовых озёр: не ближе стольких блоков от края озера (в озере свои, лавовые шипы)
    private static final double LAKE_CLEARANCE = 10.0D;

    public SpikeFeature(Codec<SpikeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SpikeConfiguration> context) {
        WorldGenLevel level = context.level();
        SpikeConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos ground = context.origin();
        Predicate<BlockPos> nearLake = nearLake(context);
        if (!isFreeGround(level, ground, nearLake)) {
            return false;
        }

        int height = between(random, config.minHeight(), config.maxHeight());
        placeSpike(level, config.stateProvider(), ground, height, nearLake, random);

        int satellites = between(random, config.minSatellites(), config.maxSatellites());
        for (int i = 0; i < satellites; i++) {
            placeSatellite(level, config.stateProvider(), ground, height, nearLake, random);
        }
        return true;
    }

    // Лежит ли блок у лавового озера (озеро квадрата, где стоит шип, см. LavaLakeField)
    private Predicate<BlockPos> nearLake(FeaturePlaceContext<SpikeConfiguration> context) {
        BlockPos origin = context.origin();
        Optional<LavaLake> lake = LavaLakeField.siteAt(context.level(), context.chunkGenerator(), origin.getX(), origin.getZ());
        return pos -> lake.isPresent() && lake.get().distanceOutsideEdge(pos.getX(), pos.getZ()) <= LAKE_CLEARANCE;
    }

    private void placeSatellite(WorldGenLevel level, BlockStateProvider stones, BlockPos mainGround,
                                int mainHeight, Predicate<BlockPos> nearLake, RandomSource random) {
        double distance = SATELLITE_MIN_DISTANCE
                + random.nextDouble() * (SATELLITE_MAX_DISTANCE - SATELLITE_MIN_DISTANCE);
        BlockPos ground = groundAround(level, mainGround, random.nextDouble() * Math.TAU, distance);
        if (!isFreeGround(level, ground, nearLake)) {
            return;
        }
        double share = SATELLITE_MIN_HEIGHT_SHARE
                + random.nextDouble() * (SATELLITE_MAX_HEIGHT_SHARE - SATELLITE_MIN_HEIGHT_SHARE);
        int height = Math.max(SATELLITE_MIN_HEIGHT, (int) Math.round(mainHeight * share));
        placeSpike(level, stones, ground, height, nearLake, random);
    }

    private void placeSpike(WorldGenLevel level, BlockStateProvider stones, BlockPos ground,
                            int height, Predicate<BlockPos> nearLake, RandomSource random) {
        Shape shape = Shape.random(height, random);
        for (int layer = 0; layer < height; layer++) {
            double progress = (double) layer / height;
            boolean anchored = layer == 0;
            placeLayer(level, stones, ground.above(layer + 1), shape, progress, anchored, random);
        }
        scatterRubble(level, stones, ground, shape.baseRadius(), nearLake, random);
    }

    // Слой - неровный круг вокруг оси шипа; сама ось входит всегда, чтобы вершина не рвалась
    private void placeLayer(WorldGenLevel level, BlockStateProvider stones, BlockPos layerOrigin,
                            Shape shape, double progress, boolean anchored, RandomSource random) {
        double roughness = 1.0D + (random.nextDouble() * 2.0D - 1.0D) * LAYER_ROUGHNESS;
        double radius = shape.radiusAt(progress) * roughness;
        double centerX = shape.centerXAt(progress);
        double centerZ = shape.centerZAt(progress);
        int axisX = (int) Math.round(centerX);
        int axisZ = (int) Math.round(centerZ);
        int reach = (int) Math.ceil(radius * (1.0D + CROSS_SECTION_ROUGHNESS));
        for (int x = axisX - reach; x <= axisX + reach; x++) {
            for (int z = axisZ - reach; z <= axisZ + reach; z++) {
                boolean onAxis = x == axisX && z == axisZ;
                if (onAxis || isInsideSection(shape, radius, x - centerX, z - centerZ)) {
                    placeColumn(level, stones, layerOrigin.offset(x, 0, z), anchored, random);
                }
            }
        }
    }

    private boolean isInsideSection(Shape shape, double radius, double fromAxisX, double fromAxisZ) {
        return Math.hypot(fromAxisX, fromAxisZ) <= radius * shape.lobes().scaleAt(fromAxisX, fromAxisZ);
    }

    private void placeColumn(WorldGenLevel level, BlockStateProvider stones, BlockPos pos,
                             boolean anchored, RandomSource random) {
        if (level.isOutsideBuildHeight(pos) || !canPlaceAt(level.getBlockState(pos))) {
            return;
        }
        setStone(level, stones, pos, random);
        if (anchored) {
            anchorDownwards(level, stones, pos, random);
        }
    }

    // Пустоты под основанием (склон ниже) засыпаются камнем, пока не упрёмся в землю
    private void anchorDownwards(WorldGenLevel level, BlockStateProvider stones, BlockPos pos, RandomSource random) {
        for (int depth = 1; depth <= ANCHOR_DEPTH; depth++) {
            BlockPos below = pos.below(depth);
            if (!isEmptySpace(level.getBlockState(below))) {
                return;
            }
            setStone(level, stones, below, random);
        }
    }

    // Одиночные камни на земле вокруг основания, чтобы шип не стоял на гладком месте
    private void scatterRubble(WorldGenLevel level, BlockStateProvider stones, BlockPos ground,
                               double baseRadius, Predicate<BlockPos> nearLake, RandomSource random) {
        for (int i = 0; i < RUBBLE_COUNT; i++) {
            double distance = baseRadius + random.nextDouble() * RUBBLE_SPREAD;
            BlockPos spot = groundAround(level, ground, random.nextDouble() * Math.TAU, distance);
            if (isFreeGround(level, spot, nearLake)) {
                setStone(level, stones, spot.above(), random);
            }
        }
    }

    // Земля (твёрдый блок под первым воздухом по карте высот) в точке на заданном расстоянии и угле от center
    private BlockPos groundAround(WorldGenLevel level, BlockPos center, double angle, double distance) {
        int x = center.getX() + (int) Math.round(Math.cos(angle) * distance);
        int z = center.getZ() + (int) Math.round(Math.sin(angle) * distance);
        return new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1, z);
    }

    private void setStone(WorldGenLevel level, BlockStateProvider stones, BlockPos pos, RandomSource random) {
        level.setBlock(pos, stones.getState(random, pos), Block.UPDATE_CLIENTS);
    }

    private boolean isFreeGround(WorldGenLevel level, BlockPos ground, Predicate<BlockPos> nearLake) {
        return level.getBlockState(ground).is(ModTags.Blocks.ASH_WASTES_GROUND)
                && level.getBlockState(ground.above()).isAir()
                && !nearLake.test(ground);
    }

    // Воздух и мелкие растения (сухие кусты, трава), но не вода и не лава
    private boolean isEmptySpace(BlockState state) {
        return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty());
    }

    private boolean canPlaceAt(BlockState state) {
        return isEmptySpace(state) || state.is(ModTags.Blocks.ASH_WASTES_GROUND);
    }

    private static int between(RandomSource random, int min, int max) {
        return min + random.nextInt(Math.max(max - min, 0) + 1);
    }

    /**
     * Форма шипа: радиус основания, наклон вершины и неровность сечения.
     * progress - доля высоты от 0 (низ) до 1 (вершина).
     */
    private record Shape(double baseRadius, double leanX, double leanZ, Lobes lobes) {
        static Shape random(int height, RandomSource random) {
            double variation = 1.0D + (random.nextDouble() * 2.0D - 1.0D) * BASE_RADIUS_VARIATION;
            double baseRadius = Math.max(MIN_BASE_RADIUS, height * BASE_RADIUS_PER_HEIGHT * variation);
            double leanAngle = random.nextDouble() * Math.TAU;
            double leanDistance = height * MAX_LEAN_PER_HEIGHT * random.nextDouble();
            return new Shape(baseRadius, Math.cos(leanAngle) * leanDistance, Math.sin(leanAngle) * leanDistance,
                    Lobes.random(random, CROSS_SECTION_ROUGHNESS));
        }

        double radiusAt(double progress) {
            return baseRadius * Math.pow(1.0D - progress, TAPER_POWER);
        }

        double centerXAt(double progress) {
            return leanX * progress;
        }

        double centerZAt(double progress) {
            return leanZ * progress;
        }
    }
}
