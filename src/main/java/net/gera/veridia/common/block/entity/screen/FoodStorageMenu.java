package net.gera.veridia.common.block.entity.screen;

import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.types.FoodStorageBlockEntity;
import net.gera.veridia.common.villager.storage.WorkerPanelData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class FoodStorageMenu extends AbstractContainerMenu implements WorkerPanelMenuAccess {
    public final FoodStorageBlockEntity blockEntity;
    public final StorageLevelLayout layout;
    private final Level level;
    private final ContainerData data;
    private final int storageSlotCount;

    public FoodStorageMenu(StorageLevelLayout layout, int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(layout, containerId, inv,
                MenuExtraData.resolveBlockEntity(inv, extraData, FoodStorageBlockEntity.class),
                new SimpleContainerData(WorkerPanelData.TOTAL));
    }

    public FoodStorageMenu(StorageLevelLayout layout, int containerId, Inventory inv,
                            BlockEntity entity, ContainerData data) {
        super(MenuTypes.foodStorageMenuType(layout.level()), containerId);
        this.blockEntity = (FoodStorageBlockEntity) entity;
        this.layout = layout;
        this.level = inv.player.level();
        this.data = data;
        this.storageSlotCount = layout.slotCount();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        for (int i = 0; i < storageSlotCount; i++) {
            this.addSlot(new SlotItemHandler(blockEntity.itemHandler, i, layout.slotX(i), layout.slotY(i)));
        }

        addDataSlots(data);
    }

    @Override
    public StorageLevelLayout layout() {
        return layout;
    }

    @Override
    public int getScaledProgress(int barPixelWidth) {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }
        return Math.min(barPixelWidth, progress * barPixelWidth / maxProgress);
    }

    @Override
    public boolean isSlotBound(int slot) {
        return data.get(WorkerPanelData.stateIndex(slot)) != WorkerPanelData.STATE_EMPTY;
    }

    @Override
    public boolean isSlotEnabled(int slot) {
        return data.get(WorkerPanelData.stateIndex(slot)) == WorkerPanelData.STATE_ENABLED;
    }

    @Override
    public int slotProfessionId(int slot) {
        return data.get(WorkerPanelData.professionIndex(slot));
    }

    @Override
    public int slotNameIndex(int slot) {
        return data.get(WorkerPanelData.nameIndex(slot));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (WorkerPanelActions.handleClick(blockEntity, player, level, id)) {
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX,
                    TE_INVENTORY_FIRST_SLOT_INDEX + storageSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + storageSlotCount) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!level.isClientSide()) {
            blockEntity.onClose(level, blockEntity.getBlockPos(), blockEntity.getBlockState());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.FOOD_STORAGE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
