package net.gera.veridia.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Капитель колонны (singe_stone_pillar_ionic): ставится только на боковую грань блока, как лестница или настенный
 * факел, и отваливается без опоры. FACING смотрит от стены (это грань, по которой кликнули).
 * Форма - три коробки из модели block/singe_stone_pillar_ionic.json; в модели стена с юга (z = 16), то есть
 * модель смотрит на север, и для остальных сторон коробки поворачиваются вокруг вертикали.
 */
public class PillarCapitalBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PillarCapitalBlock> CODEC = simpleCodec(PillarCapitalBlock::new);

    // Коробки из JSON (from/to в пикселях) для FACING = NORTH; верх капители вровень с верхом блока
    private static final Box[] MODEL_BOXES = {
            new Box(0, 6, 15, 16, 8, 16),
            new Box(0, 14, 8, 16, 16, 16),
            new Box(0, 8, 10, 16, 14, 16)
    };
    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

    public PillarCapitalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            return null;
        }
        BlockState state = defaultBlockState().setValue(FACING, face);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private static Map<Direction, VoxelShape> buildShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (Box box : MODEL_BOXES) {
                shape = Shapes.or(shape, box.facing(facing).toShape());
            }
            shapes.put(facing, shape.optimize());
        }
        return shapes;
    }

    /** Коробка в пикселях блока (0..16), как from/to в JSON модели. */
    private record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) { 
        Box facing(Direction facing) {
            return switch (facing) {
                case EAST -> new Box(16 - maxZ, minY, minX, 16 - minZ, maxY, maxX);
                case SOUTH -> new Box(16 - maxX, minY, 16 - maxZ, 16 - minX, maxY, 16 - minZ);
                case WEST -> new Box(minZ, minY, 16 - maxX, maxZ, maxY, 16 - minX);
                default -> this;
            };
        }

        VoxelShape toShape() {
            return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
