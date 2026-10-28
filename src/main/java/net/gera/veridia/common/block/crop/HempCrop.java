package net.gera.veridia.common.block.crop;

import net.gera.veridia.common.block.IncineratedFarmlandBlock;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Конопля. Растёт на выжженной пашне; сначала один блок (возраст 0-4), с возраста TALL_FROM_AGE
 * двухблочная: нижняя и верхняя половины, у обеих один возраст. Урожай (семена) считает таблица
 * добычи нижней половины; у верхней она пустая. Ломается любая половина - пропадает всё растение.
 * <p>
 * Текстуры: block/hemp/hemp_0..4 (один блок), hemp_5..7_bottom и _top. Модель - две плоскости по осям X и Z через центр блока.
 */
public class HempCrop extends CropBlock {
    public static final int MAX_AGE = 7;
    /** С какого возраста растение двухблочное. */
    public static final int TALL_FROM_AGE = 5;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX_AGE);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    // Высота выделения по возрасту (пикселей): нижней половины и верхней (у одноблочных возрастов верхней нет)
    private static final double[] LOWER_HEIGHT = {2.0, 4.0, 6.0, 8.0, 12.0, 16.0, 16.0, 16.0};
    private static final double[] UPPER_HEIGHT = {0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 10.0, 14.0};

    public HempCrop(Properties properties) {
        super(properties);
        // У EnumProperty первое значение - UPPER, поэтому нижняя половина задаётся явно
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.HEMP_SEEDS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double[] heights = isUpper(state) ? UPPER_HEIGHT : LOWER_HEIGHT;
        return Block.box(4.0, 0.0, 4.0, 12.0, heights[getAge(state)], 12.0);
    }

    // Растёт только на выжженной пашне
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof IncineratedFarmlandBlock;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (isUpper(state)) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && !isUpper(below);
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
                                     BlockPos pos, BlockPos facingPos) {
        if (isUpper(state)) {
            boolean lowerIntact = facing != Direction.DOWN || (facingState.is(this) && !isUpper(facingState));
            return lowerIntact ? state : Blocks.AIR.defaultBlockState();
        }
        // Высокой нижней половине нужна верхняя над ней
        boolean upperMissing = facing == Direction.UP && getAge(state) >= TALL_FROM_AGE
                && !(facingState.is(this) && isUpper(facingState));
        if (upperMissing) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    // Верхняя половина не растёт сама: рост идёт от нижней
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !isUpper(state) && super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        syncUpperHalf(level, pos);
    }

    // Костная мука действует на всё растение, на какую бы половину ни нажали
    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        BlockPos lowerPos = isUpper(state) ? pos.below() : pos;
        BlockState lower = level.getBlockState(lowerPos);
        if (lower.is(this)) {
            super.growCrops(level, lowerPos, lower);
            syncUpperHalf(level, lowerPos);
        }
    }

    // В творческом режиме ломание верхней половины не должно ронять предметы нижней (она исчезает вслед за ней)
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && isUpper(state)) {
            BlockPos lowerPos = pos.below();
            if (level.getBlockState(lowerPos).is(this)) {
                level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // Верхняя половина должна быть там и того же возраста, где нижняя уже высокая; если места нет, нижняя остаётся низкой
    private void syncUpperHalf(Level level, BlockPos lowerPos) {
        BlockState lower = level.getBlockState(lowerPos);
        if (!lower.is(this) || getAge(lower) < TALL_FROM_AGE) {
            return;
        }
        int age = getAge(lower);
        BlockPos upperPos = lowerPos.above();
        BlockState upper = level.getBlockState(upperPos);
        if (upper.is(this)) {
            level.setBlock(upperPos, upper.setValue(AGE, age), Block.UPDATE_CLIENTS);
        } else if (upper.isAir() || upper.canBeReplaced()) {
            level.setBlock(upperPos, defaultBlockState().setValue(AGE, age).setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        } else {
            level.setBlock(lowerPos, lower.setValue(AGE, TALL_FROM_AGE - 1), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isUpper(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER;
    }
}
