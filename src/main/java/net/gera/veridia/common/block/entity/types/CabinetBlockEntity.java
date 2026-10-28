package net.gera.veridia.common.block.entity.types;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.CabinetBlock;
import net.gera.veridia.common.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * Содержимое кладовой ({@link CabinetBlock}): 27 слотов, окно сундука на 3 ряда. Устроено как ванильная бочка: счётчик
 * смотрящих открывает дверцу и играет звук. Звуки - кладовой Farmer's Delight, если он установлен, иначе бочки.
 * Воронки, трубы и прочие моды достают предметы через обычный обработчик предметов.
 */
public class CabinetBlockEntity extends RandomizableContainerBlockEntity {
    private static final int SLOT_COUNT = 27;
    private static final ResourceLocation FD_OPEN_SOUND = ResourceLocation.fromNamespaceAndPath("farmersdelight", "block.cabinet.open");
    private static final ResourceLocation FD_CLOSE_SOUND = ResourceLocation.fromNamespaceAndPath("farmersdelight", "block.cabinet.close");
    private static final float SOUND_VOLUME = 0.5F;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            playSound(state, sound(FD_OPEN_SOUND, SoundEvents.BARREL_OPEN));
            setOpen(state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            playSound(state, sound(FD_CLOSE_SOUND, SoundEvents.BARREL_CLOSE));
            setOpen(state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previousCount, int newCount) {
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == CabinetBlockEntity.this;
        }
    };

    public CabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABINET_BE.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    // В заголовке окна - название самой кладовой (пепельная, обожжённая)
    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }

    @Override
    public void startOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            openersCounter.incrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            openersCounter.decrementOpeners(player, getLevel(), getBlockPos(), getBlockState());
        }
    }

    public void recheckOpen() {
        if (!this.remove) {
            openersCounter.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
        }
    }

    private void setOpen(BlockState state, boolean open) {
        if (level != null) {
            level.setBlock(getBlockPos(), state.setValue(CabinetBlock.OPEN, open), 3);
        }
    }

    // Звук у дверцы, а не в центре блока
    private void playSound(BlockState state, SoundEvent sound) {
        if (level == null) {
            return;
        }
        Vec3i facing = state.getValue(CabinetBlock.FACING).getNormal();
        double x = worldPosition.getX() + 0.5D + facing.getX() / 2.0D;
        double y = worldPosition.getY() + 0.5D + facing.getY() / 2.0D;
        double z = worldPosition.getZ() + 0.5D + facing.getZ() / 2.0D;
        level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, SOUND_VOLUME, level.random.nextFloat() * 0.1F + 0.9F);
    }

    private static SoundEvent sound(ResourceLocation farmersDelightSound, SoundEvent fallback) {
        return BuiltInRegistries.SOUND_EVENT.getOptional(farmersDelightSound).orElse(fallback);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, items, registries);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, items, registries);
        }
    }

    @EventBusSubscriber(modid = Veridia.MOD_ID)
    public static final class CapabilityRegistration {
        private CapabilityRegistration() {
        }

        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CABINET_BE.get(),
                    (cabinet, side) -> new InvWrapper(cabinet));
        }
    }
}
