package net.gera.veridia.common.block.entity.types;

import net.gera.veridia.common.block.FoodStorageBlock;
import net.gera.veridia.common.block.entity.ModBlockEntities;
import net.gera.veridia.common.block.entity.screen.FoodStorageMenu;
import net.gera.veridia.common.block.entity.screen.StorageLevelLayout;
import net.gera.veridia.common.villager.storage.StorageKind;
import net.gera.veridia.common.villager.storage.VillagerEmployment;
import net.gera.veridia.common.villager.storage.VillagerStorageBinding;
import net.gera.veridia.common.villager.storage.WorkerPanelData;
import net.gera.veridia.common.villager.storage.WorkerSlots;
import net.gera.veridia.common.villager.storage.WorkerStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 6-слотовый контейнер еды. Качается 1..4 уровня от полученных саженцов (см. StorageLevelConfig) -
 * progress/maxProgress отражают прогресс до следующего уровня и рисуются как villager_xp_bar.
 */
public class FoodStorageBlockEntity extends BlockEntity implements MenuProvider, WorkerStorage {
    public static final int TOTAL_SLOTS = 6;

    private UUID storageId = UUID.randomUUID();
    private final WorkerSlots workers = new WorkerSlots();

    @Override
    public UUID getStorageId() {
        return storageId;
    }

    @Override
    public StorageKind kind() {
        return StorageKind.FOOD;
    }

    @Override
    public WorkerSlots getWorkers() {
        return workers;
    }

    public final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            ItemStack remainder = super.insertItem(slot, stack, simulate);
            if (!simulate && isGrowthItem(stack)) {
                int accepted = stack.getCount() - remainder.getCount();
                if (accepted > 0) {
                    addGrowth(accepted);
                }
            }
            return remainder;
        }
    };

    private int storageLevel = 1;
    private int totalSaplingsReceived = 0;

    private int progress = 0;
    private int maxProgress = StorageLevelConfig.nextThreshold(1);

    private static boolean isGrowthItem(ItemStack stack) {
        return stack.is(ItemTags.SAPLINGS);
    }

    private void addGrowth(int amount) {
        if (storageLevel >= StorageLevelConfig.MAX_LEVEL) return;
        totalSaplingsReceived += amount;
        storageLevel = StorageLevelConfig.levelFor(totalSaplingsReceived);
        updateProgressData();
        setChanged();
    }

    private void updateProgressData() {
        if (storageLevel >= StorageLevelConfig.MAX_LEVEL) {
            progress = 1;
            maxProgress = 1;
            return;
        }
        progress = totalSaplingsReceived - StorageLevelConfig.previousThreshold(storageLevel);
        maxProgress = StorageLevelConfig.nextThreshold(storageLevel) - StorageLevelConfig.previousThreshold(storageLevel);
    }

    public int getStorageLevel() {
        return storageLevel;
    }

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= WorkerPanelData.FIRST_INDEX) {
                return WorkerPanelData.read(level, workers, index);
            }
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return WorkerPanelData.TOTAL;
        }
    };

    public FoodStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOOD_STORAGE_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.veridia.food_storage");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            VillagerStorageBinding.pruneGhosts(serverLevel, this);
        }
        StorageLevelLayout layout = StorageLevelLayout.forLevel(storageLevel);
        return new FoodStorageMenu(layout, containerId, inventory, this, data);
    }

    /** Считает открытые GUI - модель food_storage_open, пока их хотя бы один. */
    private int openCount = 0;

    public void onOpen(Level level, BlockPos pos, BlockState state) {
        openCount++;
        if (openCount == 1 && state.hasProperty(FoodStorageBlock.OPEN) && !state.getValue(FoodStorageBlock.OPEN)) {
            level.setBlock(pos, state.setValue(FoodStorageBlock.OPEN, true), Block.UPDATE_CLIENTS);
        }
    }

    public void onClose(Level level, BlockPos pos, BlockState state) {
        openCount = Math.max(0, openCount - 1);
        if (openCount == 0 && state.hasProperty(FoodStorageBlock.OPEN) && state.getValue(FoodStorageBlock.OPEN)) {
            level.setBlock(pos, state.setValue(FoodStorageBlock.OPEN, false), Block.UPDATE_CLIENTS);
        }
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, inventory);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("storageLevel", storageLevel);
        tag.putInt("totalSaplingsReceived", totalSaplingsReceived);
        tag.putUUID("storageId", storageId);
        tag.put("workers", workers.save());
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        storageLevel = Math.clamp(tag.getInt("storageLevel"), 1, StorageLevelConfig.MAX_LEVEL);
        totalSaplingsReceived = tag.getInt("totalSaplingsReceived");
        if (tag.hasUUID("storageId")) {
            storageId = tag.getUUID("storageId");
        }
        workers.load(tag.getCompound("workers"));
        updateProgressData();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
