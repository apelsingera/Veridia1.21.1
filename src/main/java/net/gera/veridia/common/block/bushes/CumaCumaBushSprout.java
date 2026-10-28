package net.gera.veridia.common.block.bushes;

import com.mojang.serialization.MapCodec;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CumaCumaBushSprout extends HorizontalDirectionalBlock implements BonemealableBlock, ThornyBush {
    public static final MapCodec<CumaCumaBushSprout> CODEC = simpleCodec(CumaCumaBushSprout::new);
    public static final int RIPEN_TICK_RARITY = 15;
    public static final int MIN_GROW_LIGHT = 9;
    public static final int PICK_BERRIES_MIN = 1;
    public static final int PICK_BERRIES_MAX = 2;
    public static final double SLOWDOWN_X = 0.8;
    public static final double SLOWDOWN_Y = 0.75;
    public static final double SLOWDOWN_Z = 0.8;
    public static final float PRICK_DAMAGE = 1.0F;
    public static final float BONEMEAL_SUCCESS_CHANCE = 0.35F;

    private static final double MIN_MOVE_TO_PRICK = 0.003;

    public static final BooleanProperty BERRIES = BlockStateProperties.BERRIES;

    private static final VoxelShape SHAPE = Block.box(1.0, 1.0, 1.0, 15.0, 15.0, 15.0);

    public CumaCumaBushSprout(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BERRIES, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BERRIES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(state.getValue(FACING).getOpposite());
        return level.getBlockState(support).getBlock() instanceof CumaCumaBushBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
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
        if (level.isClientSide) {
            return;
        }
        if (Math.abs(entity.getX() - entity.xOld) >= MIN_MOVE_TO_PRICK
                || Math.abs(entity.getZ() - entity.zOld) >= MIN_MOVE_TO_PRICK) {
            entity.hurt(level.damageSources().sweetBerryBush(), PRICK_DAMAGE);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(RIPEN_TICK_RARITY) != 0) {
            return;
        }
        if (level.getRawBrightness(pos, 0) < MIN_GROW_LIGHT) {
            return;
        }
        ripen(state, level, pos);
    }

    private static void ripen(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.getValue(BERRIES)) {
            return;
        }
        level.setBlock(pos, state.setValue(BERRIES, true), Block.UPDATE_CLIENTS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(BERRIES)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        int count = PICK_BERRIES_MIN + level.random.nextInt(PICK_BERRIES_MAX - PICK_BERRIES_MIN + 1);
        popResource(level, pos, new ItemStack(ModItems.CUMACUMA_BERRY.get(), count));
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS,
                0.8F, 0.6F + level.random.nextFloat() * 0.4F);

        BlockState picked = state.setValue(BERRIES, false);
        level.setBlock(pos, picked, Block.UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return !state.getValue(BERRIES);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < BONEMEAL_SUCCESS_CHANCE;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        ripen(state, level, pos);
    }
}
