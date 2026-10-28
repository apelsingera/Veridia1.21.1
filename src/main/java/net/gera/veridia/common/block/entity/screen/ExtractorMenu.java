package net.gera.veridia.common.block.entity.screen;

import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.types.ExtractorBlockEntity;
import net.gera.veridia.common.util.ModTags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ExtractorMenu extends AbstractContainerMenu {
    public final ExtractorBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Позиции слотов машины (левый-верхний угол слота, в координатах GUI).
    // Левый столбец: предмет-в-жидкость над баком. Правый столбец: доп. ингредиент сверху, output снизу.
    public static final int SLOT_MELT_X = 65, SLOT_MELT_Y = 13;       // предмет -> жидкость во входной танк
    public static final int SLOT_INGREDIENT_X = 94, SLOT_INGREDIENT_Y = 13; // доп. ингредиент рецепта
    public static final int SLOT_OUTPUT_X = 94, SLOT_OUTPUT_Y = 58;   // результат
    public static final int SLOT_UPGRADE_X = 128, SLOT_UPGRADE_Y = 35; // апгрейд (не расходуется)

    public ExtractorMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, MenuExtraData.resolveBlockEntity(inv, extraData, ExtractorBlockEntity.class), new SimpleContainerData(2));
    }

    public ExtractorMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(MenuTypes.EXTRACTOR_MENU.get(), containerId);
        this.blockEntity = (ExtractorBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // Порядок addSlot задаёт индексы меню 36..39 = SLOT_INGREDIENT / MELT / OUTPUT / UPGRADE.
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(),
                ExtractorBlockEntity.SLOT_INGREDIENT, SLOT_INGREDIENT_X, SLOT_INGREDIENT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.is(ModTags.Items.EXTRACTOR_UPGRADES); // чтобы shift-click апгрейда шёл в свой слот
            }
        });
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(),
                ExtractorBlockEntity.SLOT_MELT, SLOT_MELT_X, SLOT_MELT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.is(ModTags.Items.EXTRACTOR_UPGRADES);
            }
        });
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(),
                ExtractorBlockEntity.SLOT_OUTPUT, SLOT_OUTPUT_X, SLOT_OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(),
                ExtractorBlockEntity.SLOT_UPGRADE, SLOT_UPGRADE_X, SLOT_UPGRADE_Y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addDataSlots(data);
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    /**
     * Вертикальный progress-бар (текстура {@code extractor_progress.png}).
     * Возвращает высоту заполненной части в пикселях: {@code 0..texHeight}.
     * Заполнение сверху вниз рисуется в Screen; здесь только масштаб прогресса.
     */
    public int getScaledProgress(int texHeight) {
        int progress = data.get(0);
        int max = data.get(1);
        if (max <= 0 || progress <= 0) {
            return 0;
        }
        int filled = progress * texHeight / max;
        return Math.max(0, Math.min(texHeight, filled));
    }

    // --- boilerplate: shift-click, слоты игрока ---
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT = ExtractorBlockEntity.SLOT_COUNT;

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX,
                    TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX,
                    VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
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
        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.EXTRACTOR.get());
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