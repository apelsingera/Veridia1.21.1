package net.gera.veridia.common.entity.cinderroamer;

import net.gera.veridia.common.block.entity.screen.MenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import static net.gera.veridia.common.entity.cinderroamer.CinderRoamerEntity.BACKPACK_SLOTS;
import static net.gera.veridia.common.entity.cinderroamer.CinderRoamerEntity.INVENTORY_SIZE;
import static net.gera.veridia.common.entity.cinderroamer.CinderRoamerEntity.MAX_BACKPACKS;
import static net.gera.veridia.common.entity.cinderroamer.CinderRoamerEntity.SADDLE_SLOTS;

/**
 * Окно пеплохода (раскладка как у лошади): слева слоты переднего и заднего седла, справа 15 ячеек открытого рюкзака,
 * снизу инвентарь игрока.
 * <p>
 * Ячейки всех трёх рюкзаков в окне есть всегда, но видны и доступны только ячейки открытого (Slot#isActive). Какой
 * рюкзак открыт, экран сообщает серверу кнопкой меню ({@link #clickMenuButton}) - вкладки 1-3 в CinderRoamerScreen.
 */
public class CinderRoamerMenu extends AbstractContainerMenu {
    // Раскладка окна лошади
    private static final int SADDLE_SLOT_X = 8;
    private static final int FRONT_SADDLE_Y = 18;
    private static final int REAR_SADDLE_Y = 36;
    private static final int BACKPACK_X = 80;
    private static final int BACKPACK_Y = 18;
    private static final int BACKPACK_COLUMNS = 5;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final double MAX_DISTANCE = 4.0D;
    /** Номер кнопки меню «Лечь/Встать» (вкладки рюкзаков - 0..2). */
    public static final int LIE_DOWN_BUTTON = 100;

    // Номера ячеек в окне: сначала все ячейки пеплохода (как в его инвентаре), потом игрока
    private static final int PLAYER_SLOTS_START = INVENTORY_SIZE;
    private static final int HOTBAR_START = PLAYER_SLOTS_START + 27;
    private static final int SLOTS_END = HOTBAR_START + 9;

    @Nullable
    private final CinderRoamerEntity roamer;
    private final Container roamerInventory;
    private int openBackpack;

    /** Сервер: настоящий инвентарь пеплохода. */
    public CinderRoamerMenu(int containerId, Inventory playerInventory, CinderRoamerEntity roamer) {
        this(containerId, playerInventory, roamer, roamer.getInventory());
    }

    /** Клиент: пеплоход по id из пакета, содержимое ячеек придёт с сервера. */
    public CinderRoamerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, readRoamer(playerInventory, buf), new SimpleContainer(INVENTORY_SIZE));
    }

    private CinderRoamerMenu(int containerId, Inventory playerInventory, @Nullable CinderRoamerEntity roamer,
                             Container roamerInventory) {
        super(MenuTypes.CINDER_ROAMER_MENU.get(), containerId);
        this.roamer = roamer;
        this.roamerInventory = roamerInventory;
        roamerInventory.startOpen(playerInventory.player);
        addSlot(new SaddleSlot(0, FRONT_SADDLE_Y));
        addSlot(new SaddleSlot(1, REAR_SADDLE_Y));
        addBackpackSlots();
        addPlayerSlots(playerInventory);
    }

    @Nullable
    private static CinderRoamerEntity readRoamer(Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        Entity entity = playerInventory.player.level().getEntity(buf.readVarInt());
        return entity instanceof CinderRoamerEntity roamer ? roamer : null;
    }

    private void addBackpackSlots() {
        for (int backpack = 0; backpack < MAX_BACKPACKS; backpack++) {
            for (int index = 0; index < BACKPACK_SLOTS; index++) {
                int x = BACKPACK_X + index % BACKPACK_COLUMNS * SLOT_SIZE;
                int y = BACKPACK_Y + index / BACKPACK_COLUMNS * SLOT_SIZE;
                addSlot(new BackpackSlot(backpack, CinderRoamerEntity.backpackSlot(backpack, index), x, y));
            }
        }
    }

    private void addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + column, 8 + column * SLOT_SIZE,
                        PLAYER_INVENTORY_Y + row * SLOT_SIZE));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * SLOT_SIZE, HOTBAR_Y));
        }
    }

    @Nullable
    public CinderRoamerEntity getRoamer() {
        return roamer;
    }

    /** Сколько рюкзаков висит (столько вкладок). */
    public int backpackCount() {
        return roamer == null ? 0 : roamer.getBackpacks();
    }

    public int getOpenBackpack() {
        return openBackpack;
    }

    /** Экран переключает рюкзак у себя сразу, не дожидаясь сервера. */
    public void openBackpack(int backpack) {
        if (backpack >= 0 && backpack < backpackCount()) {
            openBackpack = backpack;
        }
    }

    // Кнопки окна: вкладки рюкзаков (0..2) и «Лечь/Встать»
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == LIE_DOWN_BUTTON) {
            if (roamer != null) {
                roamer.toggleLyingByOwner(player);
            }
            return true;
        }
        if (id < 0 || id >= backpackCount()) {
            return false;
        }
        openBackpack = id;
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < INVENTORY_SIZE
                ? moveItemStackTo(stack, PLAYER_SLOTS_START, SLOTS_END, true)
                : moveIntoRoamer(stack) || moveWithinPlayer(stack, index);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    // Седло - в первый слот седла, куда его можно положить; остальное - в открытый рюкзак
    private boolean moveIntoRoamer(ItemStack stack) {
        for (int saddle = 0; saddle < SADDLE_SLOTS; saddle++) {
            Slot saddleSlot = this.slots.get(saddle);
            if (!saddleSlot.hasItem() && saddleSlot.mayPlace(stack)) {
                return moveItemStackTo(stack, saddle, saddle + 1, false);
            }
        }
        if (openBackpack >= backpackCount()) {
            return false;
        }
        int start = CinderRoamerEntity.backpackSlot(openBackpack, 0);
        return moveItemStackTo(stack, start, start + BACKPACK_SLOTS, false);
    }

    // Как в обычных окнах: из инвентаря на панель быстрого доступа и обратно
    private boolean moveWithinPlayer(ItemStack stack, int index) {
        return index < HOTBAR_START
                ? moveItemStackTo(stack, HOTBAR_START, SLOTS_END, false)
                : moveItemStackTo(stack, PLAYER_SLOTS_START, HOTBAR_START, false);
    }

    @Override
    public boolean stillValid(Player player) {
        return roamer != null && roamer.isAlive() && roamer.isTame() && roamerInventory.stillValid(player)
                && (roamer.hasPassenger(player) || player.canInteractWithEntity(roamer, MAX_DISTANCE));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        roamerInventory.stopOpen(player);
    }

    /**
     * Седло кладётся только на спину без шипов и по порядку: заднее - после переднего. Снимается с конца (переднее -
     * после заднего) и пока не висят рюкзаки: они держатся на сёдлах.
     */
    private final class SaddleSlot extends Slot {
        private final int order;

        SaddleSlot(int order, int y) {
            super(roamerInventory, order, SADDLE_SLOT_X, y);
            this.order = order;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Items.SADDLE) && roamer != null && roamer.isTame() && !roamer.hasBackSpikes()
                    && (order == 0 || roamerInventory.getItem(order - 1).is(Items.SADDLE));
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPickup(Player player) {
            return backpackCount() == 0
                    && (order == SADDLE_SLOTS - 1 || roamerInventory.getItem(order + 1).isEmpty());
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            super.setByPlayer(newStack, oldStack);
            if (newStack.is(Items.SADDLE) && roamer != null) {
                roamer.playEquipSound();
            }
        }
    }

    /** Ячейка рюкзака: видна и принимает предметы, только если этот рюкзак висит и открыт. */
    private final class BackpackSlot extends Slot {
        private final int backpack;

        BackpackSlot(int backpack, int containerSlot, int x, int y) {
            super(roamerInventory, containerSlot, x, y);
            this.backpack = backpack;
        }

        @Override
        public boolean isActive() {
            return backpack == openBackpack && backpack < backpackCount();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive();
        }
    }
}
