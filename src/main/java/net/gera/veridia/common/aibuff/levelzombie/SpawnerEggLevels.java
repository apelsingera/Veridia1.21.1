package net.gera.veridia.common.aibuff.levelzombie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.OptionalInt;

/**
 * Яйца уровневых зомби и кадавров на спавнере, как ванильные яйца: ПКМ яйцом по спавнеру настраивает его на этого моба.
 * Диапазон уровней яйца (например 1-14 у слабого) записывается в данные моба спавнера, а при каждом спавне
 * обработчик уровней (LevelZombieHandler / LevelHuskHandler.onFinalizeSpawn) берёт оттуда случайный уровень из
 * диапазона вместо обычного взвешенного. У яйца на 30-35 диапазон из одного уровня, то есть уровень строго этот.
 * <p>
 * Данные лежат в NeoForgeData моба: это единственное место NBT, которое моб после загрузки не теряет.
 */
final class SpawnerEggLevels {
    private static final String MIN_LEVEL_KEY = "veridia_spawner_min_level";
    private static final String MAX_LEVEL_KEY = "veridia_spawner_max_level";
    private static final String PERSISTENT_DATA_TAG = "NeoForgeData";
    private static final String SPAWN_DATA_TAG = "SpawnData";
    private static final String SPAWN_POTENTIALS_TAG = "SpawnPotentials";
    private static final String ENTITY_TAG = "entity";

    private SpawnerEggLevels() {
    }

    /**
     * Яйцо применено к блоку: если это спавнер, настраивает его на моба type с уровнями minLevel-maxLevel и тратит яйцо.
     *
     * @return true - блок был спавнером и настроен (больше с яйцом ничего делать не надо)
     */
    static boolean tryConfigureSpawner(UseOnContext context, EntityType<?> type, int minLevel, int maxLevel) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)) {
            return false;
        }
        CompoundTag spawnerNbt = spawner.saveCustomOnly(level.registryAccess());
        spawnerNbt.put(SPAWN_DATA_TAG, spawnDataFor(type, minLevel, maxLevel));
        // Без списка вариантов спавнер при загрузке оставляет единственным вариантом то, что в SpawnData
        spawnerNbt.remove(SPAWN_POTENTIALS_TAG);
        spawner.loadCustomOnly(spawnerNbt, level.registryAccess());
        spawner.setChanged();

        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        level.gameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, pos);
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return true;
    }

    private static CompoundTag spawnDataFor(EntityType<?> type, int minLevel, int maxLevel) {
        CompoundTag levels = new CompoundTag();
        levels.putInt(MIN_LEVEL_KEY, minLevel);
        levels.putInt(MAX_LEVEL_KEY, maxLevel);
        CompoundTag entity = new CompoundTag();
        entity.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        entity.put(PERSISTENT_DATA_TAG, levels);
        CompoundTag spawnData = new CompoundTag();
        spawnData.put(ENTITY_TAG, entity);
        return spawnData;
    }

    /**
     * Уровень для только что заспавненного спавнером моба, если спавнер настроен яйцом: случайный из записанного
     * диапазона. Метки при этом стираются, чтобы моб их не носил. Пустой результат - спавнер обычный, уровень
     * выбирается как всегда.
     */
    static OptionalInt takeLevel(Entity mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(MIN_LEVEL_KEY) || !data.contains(MAX_LEVEL_KEY)) {
            return OptionalInt.empty();
        }
        int minLevel = data.getInt(MIN_LEVEL_KEY);
        int maxLevel = data.getInt(MAX_LEVEL_KEY);
        data.remove(MIN_LEVEL_KEY);
        data.remove(MAX_LEVEL_KEY);
        return OptionalInt.of(minLevel + mob.getRandom().nextInt(maxLevel - minLevel + 1));
    }
}
