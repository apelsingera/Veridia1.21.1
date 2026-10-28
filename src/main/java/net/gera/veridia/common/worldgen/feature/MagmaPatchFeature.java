package net.gera.veridia.common.worldgen.feature;

import com.mojang.serialization.Codec;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Куча blackstone_magma на поверхности земли. Origin - твёрдый блок поверхности. Поверхность в круге
 * выравнивается по его высоте (ямки до FILL_DEPTH засыпаются, над ним вырезается до CARVE_HEIGHT блоков),
 * внутри лежат проплешины из чёрного камня, а в центре лавовое озерцо неправильной формы. Лавовые колонки
 * всегда окружены кольцом магмы на том же уровне, поэтому лава не вытекает.
 */
public class MagmaPatchFeature extends Feature<MagmaPatchConfiguration> {
    // Размытие края кучи, чтобы она не была идеальным диском
    private static final float EDGE_JITTER = 1.5F;
    // Волнистость края кучи: доля радиуса, на которую край "гуляет" по углу
    private static final double PATCH_LOBE_AMPLITUDE = 0.2D;
    // Сколько блоков над поверхностью вырезается, чтобы выровнять кучу
    private static final int CARVE_HEIGHT = 3;
    // Насколько глубокую ямку (воздух под уровнем кучи) можно засыпать чёрным камнем
    private static final int FILL_DEPTH = 3;
    // Ширина кольца магмы вокруг лавы, которую обязательно нужно построить (иначе куча отменяется)
    private static final double REQUIRED_RIM = 1.5D;

    // Форма озера: доля от максимальной (lavaRadiusFraction) для случайного размера, сдвиг центра
    // относительно центра кучи (доля радиуса) и волнистость края озера
    private static final double LAKE_MIN_SIZE_SHARE = 0.45D;
    private static final double LAKE_MAX_OFFSET = 0.15D;
    private static final double LAKE_LOBE_AMPLITUDE = 0.35D;
    // Шанс, что в куче лавы нет вовсе (просто магма)
    private static final float NO_LAKE_CHANCE = 0.2F;

    // Проплешины: внутри кучи часть колонок остаётся чёрным камнем. Пятна проплешин задаёт сумма трёх синусов
    // (случайные частоты в этих пределах, радианы на блок); чем выше порог, тем меньше проплешин.
    private static final double HOLE_MIN_FREQUENCY = 0.45D;
    private static final double HOLE_MAX_FREQUENCY = 0.9D;
    private static final double HOLE_THRESHOLD = 0.4D;
    // Минимальная ширина магмы между лавой и проплешиной
    private static final double LAKE_MAGMA_BORDER = 1.0D;

    public MagmaPatchFeature(Codec<MagmaPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<MagmaPatchConfiguration> context) {
        WorldGenLevel level = context.level();
        MagmaPatchConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!isReplaceable(level.getBlockState(origin)) || !level.getBlockState(origin.above()).isAir()) {
            return false;
        }

        int radius = config.minRadius() + random.nextInt(config.maxRadius() - config.minRadius() + 1);
        Lake lake = createLake(config, radius, random);
        Lobes patchLobes = Lobes.random(random, PATCH_LOBE_AMPLITUDE);
        Holes holes = Holes.random(random);

        List<Column> columns = planColumns(level, origin, radius, patchLobes, lake, holes, random);
        if (columns == null) {
            return false;
        }
        columns.forEach(column -> placeColumn(level, column));
        if (lake.exists()) {
            int span = radius + (int) Math.ceil(EDGE_JITTER + radius * PATCH_LOBE_AMPLITUDE) + ScorchedGround.REACH;
            ScorchedGround.clearAround(level, origin, span, lake::distanceOutsideEdge, WriteRange.around(origin), random);
        }
        return !columns.isEmpty();
    }

    private Lake createLake(MagmaPatchConfiguration config, int patchRadius, RandomSource random) {
        if (config.lavaRadiusFraction() <= 0.0F || random.nextFloat() < NO_LAKE_CHANCE) {
            return Lake.NONE;
        }
        double share = LAKE_MIN_SIZE_SHARE + random.nextDouble() * (1.0D - LAKE_MIN_SIZE_SHARE);
        double radius = patchRadius * config.lavaRadiusFraction() * share;
        double offsetX = (random.nextDouble() * 2.0D - 1.0D) * patchRadius * LAKE_MAX_OFFSET;
        double offsetZ = (random.nextDouble() * 2.0D - 1.0D) * patchRadius * LAKE_MAX_OFFSET;
        return new Lake(offsetX, offsetZ, radius, Lobes.random(random, LAKE_LOBE_AMPLITUDE));
    }

    /** Возвращает колонки кучи или null, если лавовую часть нельзя окружить магмой. */
    private List<Column> planColumns(WorldGenLevel level, BlockPos origin, int radius,
                                     Lobes patchLobes, Lake lake, Holes holes, RandomSource random) {
        List<Column> columns = new ArrayList<>();
        int reach = radius + (int) Math.ceil(EDGE_JITTER + radius * PATCH_LOBE_AMPLITUDE);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double patchEdge = radius * patchLobes.scaleAt(dx, dz);
                double jitter = (random.nextFloat() * 2.0F - 1.0F) * EDGE_JITTER;
                boolean inPatch = Math.hypot(dx, dz) + jitter <= patchEdge;
                boolean mustExist = lake.exists() && lake.distanceOutsideEdge(dx, dz) <= REQUIRED_RIM;
                if (!inPatch && !mustExist) {
                    continue;
                }
                BlockPos pos = origin.offset(dx, 0, dz);
                if (!canBuildAt(level, pos)) {
                    if (mustExist) {
                        return null;
                    }
                    continue;
                }
                columns.add(new Column(pos, surfaceKind(dx, dz, lake, holes)));
            }
        }
        return columns;
    }

    private SurfaceKind surfaceKind(int dx, int dz, Lake lake, Holes holes) {
        if (lake.contains(dx, dz)) {
            return SurfaceKind.LAVA;
        }
        boolean nearLake = lake.exists() && lake.distanceOutsideEdge(dx, dz) <= LAKE_MAGMA_BORDER;
        return !nearLake && holes.contains(dx, dz) ? SurfaceKind.BLACKSTONE : SurfaceKind.MAGMA;
    }

    // Колонка годится, если на уровне кучи заменяемый блок, либо воздух над заменяемым блоком не глубже FILL_DEPTH
    private boolean canBuildAt(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isReplaceable(state)) {
            return true;
        }
        if (!state.isAir()) {
            return false;
        }
        for (int depth = 1; depth <= FILL_DEPTH; depth++) {
            BlockState below = level.getBlockState(pos.below(depth));
            if (isReplaceable(below)) {
                return true;
            }
            if (!below.isAir()) {
                return false;
            }
        }
        return false;
    }

    private void placeColumn(WorldGenLevel level, Column column) {
        BlockPos surface = column.surface();
        for (int i = 1; i <= CARVE_HEIGHT; i++) {
            BlockPos above = surface.above(i);
            if (isCarvable(level.getBlockState(above))) {
                setBlock(level, above, Blocks.AIR.defaultBlockState());
            }
        }
        for (int i = 1; i <= FILL_DEPTH; i++) {
            BlockPos below = surface.below(i);
            if (!level.getBlockState(below).isAir()) {
                break;
            }
            setBlock(level, below, Blocks.BLACKSTONE.defaultBlockState());
        }
        setBlock(level, surface, column.kind().state());
    }

    private void setBlock(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private boolean isReplaceable(BlockState state) {
        return state.is(ModTags.Blocks.ASH_WASTES_GROUND);
    }

    // Над кучей убираем и грунт, и мелкие растения (сухие кусты, трава), но не воду
    private boolean isCarvable(BlockState state) {
        return isReplaceable(state) || (state.canBeReplaced() && state.getFluidState().isEmpty());
    }

    private enum SurfaceKind {
        LAVA, MAGMA, BLACKSTONE;

        BlockState state() {
            return switch (this) {
                case LAVA -> Blocks.LAVA.defaultBlockState();
                case MAGMA -> ModBlocks.BLACKSTONE_MAGMA.get().defaultBlockState();
                case BLACKSTONE -> Blocks.BLACKSTONE.defaultBlockState();
            };
        }
    }

    private record Column(BlockPos surface, SurfaceKind kind) {
    }

    /** Пятна проплешин: сумма трёх синусов со случайными направлениями, частотами и фазами. */
    private record Holes(double[] frequencyX, double[] frequencyZ, double[] phase) {
        static Holes random(RandomSource random) {
            int waves = 3;
            double[] frequencyX = new double[waves];
            double[] frequencyZ = new double[waves];
            double[] phase = new double[waves];
            for (int i = 0; i < waves; i++) {
                double frequency = HOLE_MIN_FREQUENCY + random.nextDouble() * (HOLE_MAX_FREQUENCY - HOLE_MIN_FREQUENCY);
                double direction = random.nextDouble() * Math.TAU;
                frequencyX[i] = Math.cos(direction) * frequency;
                frequencyZ[i] = Math.sin(direction) * frequency;
                phase[i] = random.nextDouble() * Math.TAU;
            }
            return new Holes(frequencyX, frequencyZ, phase);
        }

        boolean contains(int dx, int dz) {
            double sum = 0.0D;
            for (int i = 0; i < phase.length; i++) {
                sum += Math.sin(dx * frequencyX[i] + dz * frequencyZ[i] + phase[i]);
            }
            return sum > HOLE_THRESHOLD;
        }
    }

    /** Лавовое озеро: смещённый центр, радиус и волнистый край. */
    private record Lake(double centerX, double centerZ, double radius, Lobes lobes) {
        static final Lake NONE = new Lake(0.0D, 0.0D, 0.0D, new Lobes(2, 0.0D, 3, 0.0D, 0.0D));

        boolean exists() {
            return radius > 0.0D;
        }

        boolean contains(int dx, int dz) {
            return exists() && distanceOutsideEdge(dx, dz) <= 0.0D;
        }

        /** Расстояние от колонки до края озера: меньше 0 внутри озера. */
        double distanceOutsideEdge(int dx, int dz) {
            double fromCenterX = dx - centerX;
            double fromCenterZ = dz - centerZ;
            return Math.hypot(fromCenterX, fromCenterZ) - radius * lobes.scaleAt(fromCenterX, fromCenterZ);
        }
    }
}
