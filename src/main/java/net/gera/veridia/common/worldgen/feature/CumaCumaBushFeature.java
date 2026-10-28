package net.gera.veridia.common.worldgen.feature;

import com.mojang.serialization.Codec;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.bushes.CumaCumaBushBlock;
import net.gera.veridia.common.block.bushes.CumaCumaBushSprout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CumaCumaBushFeature extends Feature<NoneFeatureConfiguration> {

    public static final int MIN_HEIGHT = 2;
    public static final int MAX_HEIGHT = CumaCumaBushBlock.MAX_HEIGHT;
    public static final int SPROUT_CHANCE = 3;

    public CumaCumaBushFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        BlockState trunk = ModBlocks.CUMA_CUMA_BUSH_BLOCK.get().defaultBlockState();
        if (!level.getBlockState(origin).isAir() || !trunk.canSurvive(level, origin)) {
            return false;
        }

        int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
        int placed = 0;
        for (int i = 0; i < height; i++) {
            BlockPos pos = origin.above(i);
            if (!level.getBlockState(pos).isAir()) {
                break;
            }
            level.setBlock(pos, trunk, Block.UPDATE_CLIENTS);
            placed++;
            placeSprouts(level, pos, random);
        }
        return placed > 0;
    }

    private static void placeSprouts(WorldGenLevel level, BlockPos trunkPos, RandomSource random) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (random.nextInt(SPROUT_CHANCE) != 0) {
                continue;
            }
            BlockPos target = trunkPos.relative(direction);
            if (!level.getBlockState(target).isAir()) {
                continue;
            }
            BlockState sprout = ModBlocks.CUMA_CUMA_BUSH_SPROUT.get().defaultBlockState()
                    .setValue(CumaCumaBushSprout.FACING, direction);
            level.setBlock(target, sprout, Block.UPDATE_CLIENTS);
        }
    }
}
