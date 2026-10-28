package net.gera.veridia.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import org.jetbrains.annotations.Nullable;

/**
 * Выжженная пашня: ванильная пашня (влажность, топтание, полив водой рядом), но при высыхании, топтании и если сверху
 * поставили сплошной блок, она превращается не в землю, а в выжженную землю. На ней растёт огнеупорная крапива.
 * Получается мотыгой из выжженной земли (см. {@link IncineratedDirtBlock}).
 */
public class IncineratedFarmlandBlock extends FarmBlock {
    // Радиус, в котором вода поливает пашню (как у ванильной): по сторонам и на один блок вверх
    private static final int WATER_REACH = 4;
    // Без воды рядом пашня сохнет быстрее ванильной: за одно случайное обновление влажность падает сразу на столько
    // (у ванильной на 1). С водой рядом или под дождём - как обычно
    private static final int DRY_STEP_WITHOUT_WATER = 3;

    public IncineratedFarmlandBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState dirt = ModBlocks.INCINERATED_DIRT.get().defaultBlockState();
        return !defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())
                ? dirt : super.getStateForPlacement(context);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            turnToIncineratedDirt(null, state, level, pos);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int moisture = state.getValue(MOISTURE);
        if (!isNearWater(level, pos) && !level.isRainingAt(pos.above())) {
            if (moisture > 0) {
                level.setBlock(pos, state.setValue(MOISTURE, Math.max(0, moisture - DRY_STEP_WITHOUT_WATER)), 2);
            } else if (!level.getBlockState(pos.above()).is(BlockTags.MAINTAINS_FARMLAND)) {
                turnToIncineratedDirt(null, state, level, pos);
            }
        } else if (moisture < MAX_MOISTURE) {
            level.setBlock(pos, state.setValue(MOISTURE, MAX_MOISTURE), 2);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        BlockState dirt = ModBlocks.INCINERATED_DIRT.get().defaultBlockState();
        if (!level.isClientSide && CommonHooks.onFarmlandTrample(level, pos, dirt, fallDistance, entity)) {
            turnToIncineratedDirt(entity, state, level, pos);
        }
        // Мимо FarmBlock.fallOn: он превратил бы пашню в обычную землю. Урон от падения как у обычного блока
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    private static void turnToIncineratedDirt(@Nullable Entity entity, BlockState state, Level level, BlockPos pos) {
        BlockState dirt = pushEntitiesUp(state, ModBlocks.INCINERATED_DIRT.get().defaultBlockState(), level, pos);
        level.setBlockAndUpdate(pos, dirt);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, dirt));
    }

    private static boolean isNearWater(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-WATER_REACH, 0, -WATER_REACH), pos.offset(WATER_REACH, 1, WATER_REACH))) {
            if (state.canBeHydrated(level, pos, level.getFluidState(near), near)) {
                return true;
            }
        }
        return FarmlandWaterManager.hasBlockWaterTicket(level, pos);
    }
}
