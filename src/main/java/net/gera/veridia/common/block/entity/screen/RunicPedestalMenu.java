package net.gera.veridia.common.block.entity.screen;

import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.types.RunicPedestalBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class RunicPedestalMenu extends AbstractContainerMenu {
    /** Id кнопки крафта в GUI (x114,y37) - см. RunicPedestalScreen.mouseClicked. */
    public static final int CRAFT_BUTTON_ID = 0;

    private final RunicPedestalBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public RunicPedestalMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, MenuExtraData.resolveBlockEntity(inv, extraData, RunicPedestalBlockEntity.class), new SimpleContainerData(3));
    }

    public RunicPedestalMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(MenuTypes.RUNIC_PEDESTAL_MENU.get(), pContainerId);
        this.blockEntity = ((RunicPedestalBlockEntity) entity);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, RunicPedestalBlockEntity.MATERIAL_SLOT_1, 40, 35));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, RunicPedestalBlockEntity.MATERIAL_SLOT_2, 58, 14));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, RunicPedestalBlockEntity.MATERIAL_SLOT_3, 80, 6));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, RunicPedestalBlockEntity.MATERIAL_SLOT_4, 102, 14));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, RunicPedestalBlockEntity.OUTPUT_SLOT, 80, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addDataSlots(data);
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    /** 0, если для текущего шага прогресса игрока сейчас нет рецепта. */
    public int getCurrentRecipeXpLevels() {
        return data.get(2);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == CRAFT_BUTTON_ID && player instanceof ServerPlayer serverPlayer) {
            return blockEntity.requestCraft(level, serverPlayer);
        }
        return super.clickMenuButton(player, id);
    }

    /**
     * Считаем из ContainerData (data.get), а НЕ из blockEntity напрямую - поля прогресса
     * в самом block entity синкаются на клиент только вместе с NBT блока (редко, при
     * onContentsChanged слотов), тогда как ContainerData синкается каждый тик через
     * addDataSlots/broadcastChanges. Из-за этого раньше здесь всегда читался 0.
     */
    public int getScaledArrowProgress() {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }
        int arrowPixelSize = 42;
        int scaled = (progress * arrowPixelSize + maxProgress - 1) / maxProgress;
        return Math.min(arrowPixelSize, Math.max(1, scaled));
    }

    // CREDIT GOES TO: diesieben07 | https://github.com/diesieben07/SevenCommons
    // must assign a slot number to each of the slots used by the GUI.
    // For this container, we can see both the tile inventory's slots as well as the player inventory slots and the hotbar.
    // Each time we add a Slot to the container, it automatically increases the slotIndex, which means
    //  0 - 8 = hotbar slots (which will map to the InventoryPlayer slot numbers 0 - 8)
    //  9 - 35 = player inventory slots (which map to the InventoryPlayer slot numbers 9 - 35)
    //  36 - 44 = TileInventory slots, which map to our TileEntity slot numbers 0 - 8)
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 5;  // must be the number of slots you have!
    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (pIndex < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                    + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + pIndex);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.RUNIC_PEDESTAL.get());
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