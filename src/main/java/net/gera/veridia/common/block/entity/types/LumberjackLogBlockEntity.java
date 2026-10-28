package net.gera.veridia.common.block.entity.types;

import net.gera.veridia.common.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Не хранит данных - существует только чтобы у блока был рендерер
 * (LumberjackLogBlockEntityRenderer рисует настоящий iron_axe поверх бревна).
 */
public class LumberjackLogBlockEntity extends BlockEntity {
    public LumberjackLogBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.LUMBERJACK_LOG_BE.get(), pos, blockState);
    }
}