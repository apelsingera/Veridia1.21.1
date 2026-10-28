package net.gera.veridia.common.worldgen.feature;

import com.mojang.serialization.Codec;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Arrays;
import java.util.Optional;

/**
 * Большое лавовое озеро, лежащее вровень с землёй, как пол. Лава заменяет верхний слой земли в каждой колонке озера,
 * поэтому поверхность озера идёт по рельефу (где земля ступенью выше или ниже, там и лава), а ни стен, ни вала,
 * ни приподнятой плиты нет. Уровень лавы идёт по сглаженному рельефу (сетка в LavaLake), а не по каждой кочке: мелкие
 * впадины земли засыпаются, бугры срезаются, поэтому в озере нет ямок. По краю лежит магматический берег: верхний слой земли тоже вровень. В центре из лавы
 * торчат шипы из blackstone_magma.
 * <p>
 * Озеро в десятки блоков шире, чем фича может писать (свой чанк и по одному вокруг), поэтому фича ставится в каждом
 * чанке без шанса и строит только его собственные колонки; где озеро и какое оно, решает {@link LavaLakeField}, а форму
 * даёт {@link LavaLake}, и соседние чанки сходятся без швов. Земля читается в самой колонке, соседей не нужно.
 */
public class LavaLakeFeature extends Feature<NoneFeatureConfiguration> {
    // Сколько блоков над лавой должно быть чистым воздухом (убираются кусты, трава, листва, ветки)
    private static final int AIR_ABOVE = 3;
    // Сколько блоков под лавой проверяется на пещеры: пустота подсыпается чёрным камнем, чтобы лава не провалилась
    private static final int BED_DEPTH = 3;
    // Насколько глубоко (блоков) засыпается впадина земли под лавой; глубже (овраг, пещера) лава лежит на плите такой толщины
    private static final int MAX_SUPPORT_DEPTH = 6;
    // На сколько слоёв вглубь идёт лава в каждой колонке (чтобы у ступенек не просвечивал камень под лавой)
    private static final int LAVA_DEPTH = 3;
    // Доля магмы в шипах (остальное чёрный камень), в процентах
    private static final int SPIKE_MAGMA_PERCENT = 70;
    // Нет лавы в этой колонке чанка (для шипов)
    private static final int NO_LAVA = Integer.MIN_VALUE;

    public LavaLakeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        Optional<LavaLake> site = LavaLakeField.siteAt(level, context.chunkGenerator(), chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());
        if (site.isEmpty() || !site.get().touchesChunk(chunk, ScorchedGround.REACH)) {
            return false;
        }
        LavaLake lake = site.get();
        BoundingBox chunkArea = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), level.getMaxBuildHeight(), chunk.getMaxBlockZ());

        int[] lavaLevels = placeLakeColumns(level, lake, chunk);
        placeSpikes(level, lake, chunk, lavaLevels);
        placePiles(level, lake, chunk, lavaLevels);
        BlockPos middle = new BlockPos(chunk.getMiddleBlockX(), 0, chunk.getMiddleBlockZ());
        ScorchedGround.clearAround(level, middle, 8, (dx, dz) -> lake.distanceOutsideEdge(middle.getX() + dx, middle.getZ() + dz),
                chunkArea, context.random());
        return true;
    }

    // Лава и берег в колонках этого чанка. Возвращает Y лавы по колонкам чанка (NO_LAVA, где лавы нет): шипам нужно знать,
    // где в колонке была земля, а после замены верхний слой земли уже лава.
    private int[] placeLakeColumns(WorldGenLevel level, LavaLake lake, ChunkPos chunk) {
        int[] lavaLevels = new int[16 * 16];
        Arrays.fill(lavaLevels, NO_LAVA);
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                double edgeDistance = lake.distanceOutsideEdge(x, z) + lake.edgeJitter(x, z);
                if (edgeDistance > LavaLake.SHORE_WIDTH) {
                    continue;
                }
                BlockPos ground = ScorchedGround.groundAt(level, x, z);
                if (edgeDistance <= 0.0D) {
                    int lavaY = lake.lavaLevel(x, z);
                    placeLavaColumn(level, ground, lavaY);
                    lavaLevels[columnIndex(x, z)] = lavaY;
                } else {
                    placeShoreColumn(level, ground);
                }
            }
        }
        return lavaLevels;
    }

    private static int columnIndex(int x, int z) {
        return (x & 15) + ((z & 15) << 4);
    }

    // Лава на уровне lavaY (он идёт по сглаженному рельефу, а не по каждой кочке): всё над ней срезается (бугры земли, кусты,
    // листва), впадина земли под ней засыпается чёрным камнем (не глубже MAX_SUPPORT_DEPTH), поэтому поверхность озера
    // сплошная, без ямок
    private void placeLavaColumn(WorldGenLevel level, BlockPos ground, int lavaY) {
        int x = ground.getX();
        int z = ground.getZ();
        int clearTop = Math.max(ground.getY(), lavaY + AIR_ABOVE);
        for (int y = lavaY + 1; y <= clearTop; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().isEmpty() && !state.isAir()) {
                setBlock(level, pos, Blocks.AIR.defaultBlockState());
            }
        }
        // Лава идёт на LAVA_DEPTH слоёв вглубь: у ступеньки, где соседняя лава ниже, под верхним слоем виден был бы камень
        int lavaBottom = lavaY - LAVA_DEPTH + 1;
        for (int y = lavaBottom; y <= lavaY; y++) {
            setBlock(level, new BlockPos(x, y, z), Blocks.LAVA.defaultBlockState());
        }

        int supportFrom = Math.max(ground.getY() + 1, lavaBottom - MAX_SUPPORT_DEPTH);
        for (int y = supportFrom; y < lavaBottom; y++) {
            setBlock(level, new BlockPos(x, y, z), Blocks.BLACKSTONE.defaultBlockState());
        }
        boolean reachedGround = supportFrom == ground.getY() + 1;
        if (reachedGround) {
            patchCavesUnder(level, x, Math.min(lavaBottom, ground.getY() + 1) - 1, z);
        }
    }

    // Если прямо под лавой пустота (пещера у самой поверхности), она подсыпается на BED_DEPTH блоков, чтобы лава не провалилась
    private void patchCavesUnder(WorldGenLevel level, int x, int topY, int z) {
        for (int depth = 0; depth < BED_DEPTH; depth++) {
            BlockPos pos = new BlockPos(x, topY - depth, z);
            if (level.getBlockState(pos).isSolid()) {
                break;
            }
            setBlock(level, pos, Blocks.BLACKSTONE.defaultBlockState());
        }
    }

    // Берег: верхний слой земли вокруг озера становится магмой вровень с землёй (не вал и не стена)
    private void placeShoreColumn(WorldGenLevel level, BlockPos ground) {
        if (isGround(level.getBlockState(ground))) {
            setBlock(level, ground, ModBlocks.BLACKSTONE_MAGMA.get().defaultBlockState());
        }
    }

    // Лавовые шипы в центре, их части в колонках этого чанка
    private void placeSpikes(WorldGenLevel level, LavaLake lake, ChunkPos chunk, int[] lavaLevels) {
        for (LavaLake.Spike spike : lake.spikes()) {
            int centerX = lake.centerX() + spike.x();
            int centerZ = lake.centerZ() + spike.z();
            int reach = (int) Math.ceil(spike.baseRadius());
            for (int x = Math.max(centerX - reach, chunk.getMinBlockX()); x <= Math.min(centerX + reach, chunk.getMaxBlockX()); x++) {
                for (int z = Math.max(centerZ - reach, chunk.getMinBlockZ()); z <= Math.min(centerZ + reach, chunk.getMaxBlockZ()); z++) {
                    int lavaY = lavaLevels[columnIndex(x, z)];
                    if (lavaY != NO_LAVA) {
                        placeSpikeColumn(level, lake, spike, lavaY, x, z, centerX, centerZ);
                    }
                }
            }
        }
    }

    // Колонка шипа: от лавы под ним до вершины на своей высоте (сечение сужается с высотой). Высота шипа считается от
    // baseY, одинаковой для всех колонок, поэтому шип не ломается на границах чанков.
    private void placeSpikeColumn(WorldGenLevel level, LavaLake lake, LavaLake.Spike spike, int lavaY,
                                  int x, int z, int centerX, int centerZ) {
        double fromAxis = Math.hypot(x - centerX, z - centerZ);
        boolean onAxis = x == centerX && z == centerZ;
        // Лава под шипом заменяется камнем до основания шипа, иначе шип висел бы над лавой
        if (onAxis || fromAxis <= spike.radiusAt(0)) {
            for (int y = lavaY; y <= lake.baseY(); y++) {
                setBlock(level, new BlockPos(x, y, z), spikeBlock(x, y, z));
            }
        }
        for (int layer = 1; layer <= spike.height(); layer++) {
            if (onAxis || fromAxis <= spike.radiusAt(layer)) {
                BlockPos pos = new BlockPos(x, lake.baseY() + layer, z);
                if (level.getBlockState(pos).isAir() || isCarvable(level.getBlockState(pos))) {
                    setBlock(level, pos, spikeBlock(x, pos.getY(), z));
                }
            }
        }
    }

    // Кучи сырой молибденовой руды, их части в колонках этого чанка
    private void placePiles(WorldGenLevel level, LavaLake lake, ChunkPos chunk, int[] lavaLevels) {
        for (OrePile pile : lake.piles()) {
            int centerX = lake.centerX() + pile.x();
            int centerZ = lake.centerZ() + pile.z();
            int footY = lake.pileFootY(pile);
            int reach = pile.reach();
            for (int x = Math.max(centerX - reach, chunk.getMinBlockX()); x <= Math.min(centerX + reach, chunk.getMaxBlockX()); x++) {
                for (int z = Math.max(centerZ - reach, chunk.getMinBlockZ()); z <= Math.min(centerZ + reach, chunk.getMaxBlockZ()); z++) {
                    int lavaY = lavaLevels[columnIndex(x, z)];
                    if (lavaY != NO_LAVA) {
                        placePileColumn(level, pile, footY, lavaY, x, z, x - centerX, z - centerZ);
                    }
                }
            }
        }
    }

    // Колонка кучи: от лавы (или от основания кучи, если лава выше) до вершины. Слои считаются от основания кучи, одного
    // для всех колонок, поэтому куча не ломается на границах чанков. Ниже основания (где лава ниже) куча продолжает своё
    // нижнее сечение, чтобы она стояла на лаве, а не висела над ней. Лава и мелкие растения заменяются рудой.
    private void placePileColumn(WorldGenLevel level, OrePile pile, int footY, int lavaY, int x, int z, int dx, int dz) {
        BlockState ore = ModBlocks.RAW_MOLYBDENUM_BLOCK.get().naturalState();
        for (int y = Math.min(lavaY, footY); y <= footY + pile.height(); y++) {
            if (!pile.contains(dx, y - footY, dz)) {
                continue;
            }
            BlockPos pos = new BlockPos(x, y, z);
            BlockState existing = level.getBlockState(pos);
            if (existing.isAir() || !existing.getFluidState().isEmpty() || isCarvable(existing)) {
                setBlock(level, pos, ore);
            }
        }
    }

    // Магма с вкраплениями чёрного камня; какой блок, зависит только от координат, чтобы чанки сходились
    private BlockState spikeBlock(int x, int y, int z) {
        long hash = Mth.getSeed(x, y, z);
        boolean magma = Math.floorMod(hash >>> 8, 100L) < SPIKE_MAGMA_PERCENT;
        return magma ? ModBlocks.BLACKSTONE_MAGMA.get().defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
    }

    private void setBlock(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    // Земля пепельных биомов; базальт - это столбики шипов, которые могли вырасти раньше озера в соседнем чанке
    private boolean isGround(BlockState state) {
        return state.is(ModTags.Blocks.ASH_WASTES_GROUND) || state.is(Blocks.BASALT);
    }

    // Над шипом убираем и грунт, и мелкие растения (сухие кусты, трава), но не воду
    private boolean isCarvable(BlockState state) {
        return isGround(state) || (state.canBeReplaced() && state.getFluidState().isEmpty());
    }
}
