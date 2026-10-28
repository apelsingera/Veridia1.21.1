package net.gera.veridia.common.block.bushes;

import com.mojang.serialization.MapCodec;
import net.gera.veridia.common.block.ModBlocks;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CumaCumaBushBlock extends BushBlock implements BonemealableBlock, ThornyBush {
    public static final MapCodec<CumaCumaBushBlock> CODEC = simpleCodec(CumaCumaBushBlock::new);

    public static final int MAX_HEIGHT = 3;
    public static final int GROWTH_TICK_RARITY = 4;
    public static final int GROW_UP_CHANCE = 2;
    public static final int SPROUT_CHANCE = 3;
    /** Ванильный sweet berry bush зреет с шансом 1/5 за рандом-тик, втрое медленнее - 1/15. */
    public static final int BERRY_TICK_RARITY = 15;
    public static final int MIN_GROW_LIGHT = 9;
    public static final int PICK_BERRIES_MIN = 1;
    public static final int PICK_BERRIES_MAX = 3;
    public static final double SLOWDOWN_X = 0.8;
    public static final double SLOWDOWN_Y = 0.75;
    public static final double SLOWDOWN_Z = 0.8;
    /** Урон за шаг внутри куста в полусердцах. 0 = не колется. */
    public static final float PRICK_DAMAGE = 1.0F;
    public static final float BONEMEAL_SUCCESS_CHANCE = 0.35F;

    private static final double MIN_MOVE_TO_PRICK = 0.003;

    public static final BooleanProperty BERRIES = BlockStateProperties.BERRIES;

    public CumaCumaBushBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BERRIES, false));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BERRIES);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof CumaCumaBushBlock || super.mayPlaceOn(state, level, pos);
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
        if (level.getRawBrightness(pos, 0) < MIN_GROW_LIGHT) {
            return;
        }
        if (random.nextInt(BERRY_TICK_RARITY) == 0 && tryRipen(state, level, pos)) {
            return;
        }
        if (random.nextInt(GROWTH_TICK_RARITY) == 0) {
            growStep(state, level, pos, random, false);
        }
    }

    private static void growStep(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, boolean forced) {
        if ((forced || random.nextInt(GROW_UP_CHANCE) == 0) && tryGrowUp(level, pos)) {
            return;
        }
        if ((forced || random.nextInt(SPROUT_CHANCE) == 0) && trySprout(level, pos, random)) {
            return;
        }
        if (forced) {
            tryRipen(state, level, pos);
        }
    }

    private static boolean tryGrowUp(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        if (!level.getBlockState(above).isAir() || columnHeight(level, pos) >= MAX_HEIGHT) {
            return false;
        }
        level.setBlockAndUpdate(above, ModBlocks.CUMA_CUMA_BUSH_BLOCK.get().defaultBlockState());
        return true;
    }

    private static boolean trySprout(ServerLevel level, BlockPos pos, RandomSource random) {
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos target = pos.relative(direction);
        if (!level.getBlockState(target).isAir()) {
            return false;
        }
        BlockState sprout = ModBlocks.CUMA_CUMA_BUSH_SPROUT.get().defaultBlockState()
                .setValue(CumaCumaBushSprout.FACING, direction);
        if (!sprout.canSurvive(level, target)) {
            return false;
        }
        level.setBlockAndUpdate(target, sprout);
        return true;
    }

    private static boolean tryRipen(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.getValue(BERRIES)) {
            return false;
        }
        level.setBlock(pos, state.setValue(BERRIES, true), Block.UPDATE_CLIENTS);
        return true;
    }

    private static int columnHeight(BlockGetter level, BlockPos pos) {
        int height = 1;
        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (height < MAX_HEIGHT && level.getBlockState(cursor.move(Direction.DOWN)).getBlock() instanceof CumaCumaBushBlock) {
            height++;
        }
        return height;
    }

    private static boolean hasRoomToGrow(LevelReader level, BlockPos pos) {
        if (level.getBlockState(pos.above()).isAir() && columnHeight(level, pos) < MAX_HEIGHT) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).isAir()) {
                return true;
            }
        }
        return false;
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
        return !state.getValue(BERRIES) || hasRoomToGrow(level, pos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < BONEMEAL_SUCCESS_CHANCE;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        growStep(state, level, pos, random, true);
    }
}
