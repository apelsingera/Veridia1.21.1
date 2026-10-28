package net.gera.veridia.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Кровавый рунный камень: стоит в два блока высотой (как дверь: нижняя и верхняя половины) и поворачивается
 * на 8 сторон с шагом 45°, как череп (у черепа 16 сторон). Поворот при установке выбирается так, чтобы передняя
 * сторона камня смотрела на игрока. Саму модель рисует нижняя половина (она выше своего блока), у верхней
 * модель пустая. Ставится только на твёрдый пол, но дальше от опоры не зависит. Пока блок ничего не делает.
 */
public class BloodyRunestoneBlock extends Block {
    public static final MapCodec<BloodyRunestoneBlock> CODEC = simpleCodec(BloodyRunestoneBlock::new);

    public static final int ROTATION_STEPS = 8;
    private static final float DEGREES_PER_STEP = 360.0F / ROTATION_STEPS;
    // Поворот 0 - передняя сторона камня смотрит на восток, то есть на игрока, который смотрит на запад (yaw 90)
    private static final float FRONT_YAW_AT_ROTATION_ZERO = 90.0F;

    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, ROTATION_STEPS - 1);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    // Одна простая коробка на обе половины: от поворота не зависит
    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);

    public BloodyRunestoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION, HALF);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        // Ставится только на твёрдый пол (проверка лишь при установке: потом опору можно убрать, камень останется)
        boolean solidFloor = level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)
                || !solidFloor) {
            return null;
        }
        return defaultBlockState()
                .setValue(ROTATION, rotationFacing(context.getRotation()))
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    // Номер поворота, при котором передняя сторона смотрит на игрока, взгляд которого задан углом yaw
    private static int rotationFacing(float playerYaw) {
        return Math.floorMod(Math.round((playerYaw - FRONT_YAW_AT_ROTATION_ZERO) / DEGREES_PER_STEP), ROTATION_STEPS);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Нижней половине опора не нужна: камень стоит и в воздухе. Верхней нужна нижняя
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return true;
        }
        BlockState belowState = level.getBlockState(pos.below());
        return belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    // Как у двери: половина пропадает, если пропала вторая (опора под камнем не нужна)
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean towardsOtherHalf = direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP);
        if (!towardsOtherHalf) {
            boolean lowerLostSupport = half == DoubleBlockHalf.LOWER && direction == Direction.DOWN
                    && !state.canSurvive(level, pos);
            return lowerLostSupport ? Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
        boolean otherHalfIntact = neighborState.is(this) && neighborState.getValue(HALF) != half;
        return otherHalfIntact ? state : Blocks.AIR.defaultBlockState();
    }

    // Выпадение: лут нижней половины (см. ModBlockLootTableProvider). Если ломают верхнюю в творчестве или без
    // подходящего инструмента, нижняя убирается без выпадения, иначе она выбросила бы камни и "голой рукой".
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        boolean upperHalf = state.getValue(HALF) == DoubleBlockHalf.UPPER;
        if (!level.isClientSide && upperHalf && (player.isCreative() || !player.hasCorrectToolForDrops(state))) {
            removeLowerHalfWithoutDrops(level, pos, state, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void removeLowerHalfWithoutDrops(Level level, BlockPos upperPos, BlockState upperState, Player player) {
        BlockPos lowerPos = upperPos.below();
        BlockState lowerState = level.getBlockState(lowerPos);
        if (lowerState.is(upperState.getBlock()) && lowerState.getValue(HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            level.levelEvent(player, 2001, lowerPos, Block.getId(lowerState));
        }
    }

    // Красные частицы у камня: animateTick вызывается для случайных блоков рядом с игроком (клиент), поэтому на
    // каждый вызов выпускается пачка частиц; обе половины камня светятся своей частью
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.8F, 0.02F, 0.02F), 1.0F);
    private static final int PARTICLES_PER_CALL = 3;
    private static final double PARTICLE_RISE_SPEED = 0.03D;

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < PARTICLES_PER_CALL; i++) {
            double x = pos.getX() + 0.25D + random.nextDouble() * 0.5D;
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + 0.25D + random.nextDouble() * 0.5D;
            level.addParticle(RED_DUST, x, y, z, 0.0D, PARTICLE_RISE_SPEED, 0.0D);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), ROTATION_STEPS));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), ROTATION_STEPS));
    }
}
