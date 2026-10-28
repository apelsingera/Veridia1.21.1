package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WallSnapshot extends SavedData {

    private static final String DATA_NAME = "veridia_wall_snapshot";
    private static final String BLOCK_ENTITY_KEY = "blockEntity";

    // Откат кладёт блоки ровно такими, какими они были: клиентам - обновление, соседям - ничего. Без
    // UPDATE_KNOWN_SHAPE каждый возвращённый блок дёргал соседей (листва пересчитывала расстояние, ветки
    // Dynamic Trees реагировали на ещё не возвращённых соседей), и часть дерева успевала погибнуть.
    private static final int RESTORE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Блок как он был: состояние и данные его блок-сущности (null, если блок-сущности нет). Данные нужны,
     * например, корню Dynamic Trees - в блок-сущности хранится порода дерева, а без неё корень при первом
     * тике превращается в обычную землю, и дерево гибнет. */
    private record Saved(BlockState state, @Nullable CompoundTag blockEntity) {}

    private final Map<BlockPos, Saved> entries = new LinkedHashMap<>();

    // Мир нужен только затем, чтобы при записи прочитать блок-сущность: запись всегда идёт ДО замены блока,
    // а Recorder.record(pos, state) мира не передаёт. Выставляется в get().
    @Nullable
    private ServerLevel level;

    public static WallSnapshot get(ServerLevel level) {
        WallSnapshot snapshot = level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WallSnapshot::new, WallSnapshot::load), DATA_NAME);
        snapshot.level = level;
        return snapshot;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    /** Блок на этой позиции менялся расчётом (стена, башня, дом, земляные работы) - в мире он не оригинальный. */
    public boolean isRecorded(BlockPos pos) {
        return entries.containsKey(pos);
    }

    public void record(BlockPos pos, BlockState state) {
        BlockPos key = pos.immutable();
        if (entries.containsKey(key)) {
            return;
        }
        entries.put(key, new Saved(state, blockEntityTag(key, state)));
        setDirty();
    }

    @Nullable
    private CompoundTag blockEntityTag(BlockPos pos, BlockState state) {
        if (level == null || !state.hasBlockEntity()) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess());
    }

    public int restoreAll(ServerLevel level) {
        if (entries.isEmpty()) {
            return 0;
        }
        List<Map.Entry<BlockPos, Saved>> list = new ArrayList<>(entries.entrySet());
        for (int i = list.size() - 1; i >= 0; i--) {
            Map.Entry<BlockPos, Saved> entry = list.get(i);
            restore(level, entry.getKey(), entry.getValue());
        }
        int restored = entries.size();
        entries.clear();
        setDirty();
        return restored;
    }

    private static void restore(ServerLevel level, BlockPos pos, Saved saved) {
        level.setBlock(pos, saved.state(), RESTORE_FLAGS);
        if (saved.blockEntity() == null) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.loadWithComponents(saved.blockEntity(), level.registryAccess());
            blockEntity.setChanged();
        }
    }

    private static WallSnapshot load(CompoundTag tag, HolderLookup.Provider provider) {
        WallSnapshot data = new WallSnapshot();
        HolderGetter<Block> blocks = provider.lookupOrThrow(Registries.BLOCK);
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            BlockState state = NbtUtils.readBlockState(blocks, entry.getCompound("state"));
            CompoundTag blockEntity = entry.contains(BLOCK_ENTITY_KEY, Tag.TAG_COMPOUND)
                    ? entry.getCompound(BLOCK_ENTITY_KEY) : null;
            data.entries.put(pos, new Saved(state, blockEntity));
        }
        return data;
    }

    @SuppressWarnings("all")
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, Saved> entry : entries.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putInt("x", entry.getKey().getX());
            c.putInt("y", entry.getKey().getY());
            c.putInt("z", entry.getKey().getZ());
            c.put("state", NbtUtils.writeBlockState(entry.getValue().state()));
            if (entry.getValue().blockEntity() != null) {
                c.put(BLOCK_ENTITY_KEY, entry.getValue().blockEntity());
            }
            list.add(c);
        }
        tag.put("entries", list);
        return tag;
    }
}
