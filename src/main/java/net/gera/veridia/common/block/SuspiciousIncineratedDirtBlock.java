package net.gera.veridia.common.block;

import net.gera.veridia.Veridia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

/**
 * Подозрительная выжженная земля: подозрительный блок вроде подозрительного гравия (чистится кистью, падает без
 * опоры) с собственной добычей. Блок-сущность общая с ванильными подозрительными блоками (BRUSHABLE_BLOCK, этот блок
 * добавлен к её допустимым в {@link net.gera.veridia.common.block.entity.ModBlockEntities}), поэтому и отрисовка
 * при чистке ванильная.
 * <p>
 * Добыча берётся из таблицы {@link #LOOT_TABLE}: она выдаётся каждому новому блоку при установке. Структура или
 * команда может заменить её своей (тег LootTable в NBT блока, в структурах - блок-процессор append_loot).
 */
public class SuspiciousIncineratedDirtBlock extends BrushableBlock {
    public static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "archaeology/suspicious_incinerated_dirt"));

    // Сид 0 - добыча не привязана к месту, а выбирается заново при каждой чистке
    private static final long RANDOM_LOOT_SEED = 0L;

    public SuspiciousIncineratedDirtBlock(Properties properties, Block turnsInto) {
        super(turnsInto, SoundEvents.BRUSH_GRAVEL, SoundEvents.BRUSH_GRAVEL_COMPLETED, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BrushableBlockEntity entity = new BrushableBlockEntity(pos, state);
        entity.setLootTable(LOOT_TABLE, RANDOM_LOOT_SEED);
        return entity;
    }
}
