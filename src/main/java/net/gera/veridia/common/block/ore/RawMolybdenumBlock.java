package net.gera.veridia.common.block.ore;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Блок сырой молибденовой руды. Кучи руды в лавовых озёрах (см. LavaLakeFeature) ставятся с NATURAL = true: у такого
 * блока клиентская модель ({@code common/block/ore/client}) рисует торчащие куски скрапа и иногда повёрнутый кубик.
 * Блок, который поставил игрок, имеет NATURAL = false и выглядит как обычный куб. Добыча: см. ModBlockLootTableProvider.
 */
public class RawMolybdenumBlock extends Block {
    public static final MapCodec<RawMolybdenumBlock> CODEC = simpleCodec(RawMolybdenumBlock::new);
    /** Блок появился при генерации мира, а не поставлен игроком: только такие блоки украшены скрапом. */
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");

    public RawMolybdenumBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NATURAL, false));
    }

    /** Состояние для генерации мира: блок с украшениями. */
    public BlockState naturalState() {
        return defaultBlockState().setValue(NATURAL, true);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(NATURAL);
    }

    @Override
    protected @NotNull MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
