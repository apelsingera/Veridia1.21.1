package net.gera.veridia.common.block.bushes;

import com.mojang.serialization.MapCodec;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CumaCumaBush extends BushBlock implements BonemealableBlock, ThornyBush {
    public static final MapCodec<CumaCumaBush> CODEC = simpleCodec(CumaCumaBush::new);

    public static final int MAX_AGE = 1;
    public static final int GROWTH_TICK_RARITY = 6;
    public static final int MIN_GROW_LIGHT = 9;
    public static final double SLOWDOWN_X = 0.9;
    public static final double SLOWDOWN_Y = 0.9;
    public static final double SLOWDOWN_Z = 0.9;
    public static final float PRICK_DAMAGE = 1.0F;
    public static final float BONEMEAL_SUCCESS_CHANCE = 0.35F;

    private static final double MIN_MOVE_TO_PRICK = 0.003;

    public static final IntegerProperty AGE = BlockStateProperties.AGE_1;

    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(4.0, 0.0, 4.0, 12.0, 5.0, 12.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 11.0, 14.0)
    };

    public CumaCumaBush(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[state.getValue(AGE)];
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.CUMACUMA_BERRY.get());
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        entity.makeStuckInBlock(state, new Vec3(SLOWDOWN_X, SLOWDOWN_Y, SLOWDOWN_Z));
        if (level.isClientSide || state.getValue(AGE) == 0) {
            return;
        }
        if (Math.abs(entity.getX() - entity.xOld) >= MIN_MOVE_TO_PRICK
                || Math.abs(entity.getZ() - entity.zOld) >= MIN_MOVE_TO_PRICK) {
            entity.hurt(level.damageSources().sweetBerryBush(), PRICK_DAMAGE);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(GROWTH_TICK_RARITY) != 0) {
            return;
        }
        if (level.getRawBrightness(pos, 0) < MIN_GROW_LIGHT) {
            return;
        }
        grow(state, level, pos);
    }

    private static void grow(BlockState state, ServerLevel level, BlockPos pos) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        } else {
            level.setBlockAndUpdate(pos, ModBlocks.CUMA_CUMA_BUSH_BLOCK.get().defaultBlockState());
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < BONEMEAL_SUCCESS_CHANCE;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        grow(state, level, pos);
    }
}
