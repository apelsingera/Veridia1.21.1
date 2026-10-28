package net.gera.veridia.common.block.entity.types;

import net.gera.veridia.common.block.entity.ModBlockEntities;
import net.gera.veridia.common.block.entity.screen.ExtractorMenu;
import net.gera.veridia.common.recipe.ModRecipes;
import net.gera.veridia.common.recipe.extractor.ExtractorFluidRecipe;
import net.gera.veridia.common.recipe.extractor.ExtractorFluidRecipeInput;
import net.gera.veridia.common.recipe.extractor.ExtractorRecipe;
import net.gera.veridia.common.recipe.extractor.ExtractorRecipeInput;
import net.gera.veridia.common.util.ModTags;
import net.gera.veridia.annotations.Persisted;
import net.gera.veridia.annotations.PersistedFields;
import net.gera.veridia.annotations.PersistedPrefix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@PersistedPrefix("extractor")
public class ExtractorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INGREDIENT = 0; // верхний слот - доп. ингредиент рецепта
    public static final int SLOT_MELT = 1;       // средний слот - предмет, который разжижается в танк
    public static final int SLOT_OUTPUT = 2;     // нижний-правый слот - результат
    public static final int SLOT_UPGRADE = 3;    // слот (128,35) - апгрейд, не расходуется
    public static final int SLOT_COUNT = 4;

    public static final int TANK_CAPACITY = 8000;
    /** Сколько тиков уходит на разжижение одного предмета из среднего слота. */
    private static final int MELT_TIME = 40;

    public final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_OUTPUT) {
                return false;
            }
            if (slot == SLOT_UPGRADE) {
                return stack.is(ModTags.Items.EXTRACTOR_UPGRADES);
            }
            return true;
        }
    };

    private final FluidTank inputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    @Persisted("progress")
    private int progress = 0;
    @Persisted("max_progress")
    private int maxProgress = ExtractorRecipe.DEFAULT_TIME;
    @Persisted("melt_progress")
    private int meltProgress = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int value) {
            switch (i) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTRACTOR_BE.get(), pos, state);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public FluidTank getInputTank() {
        return inputTank;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.veridia.extractor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ExtractorMenu(containerId, inventory, this, this.data);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        if (level != null) {
            Containers.dropContents(level, worldPosition, inventory);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.put("fluid", inputTank.writeToNBT(registries, new CompoundTag()));
        PersistedFields.save(this, tag);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        if (itemHandler.getSlots() != SLOT_COUNT) {
            itemHandler.setSize(SLOT_COUNT); // миграция со старого сейва (было 3 слота)
        }
        inputTank.readFromNBT(registries, tag.getCompound("fluid"));
        PersistedFields.load(this, tag);
        if (maxProgress <= 0) {
            maxProgress = ExtractorRecipe.DEFAULT_TIME;
        }
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

    public void tick(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;
        changed |= tickDrainContainer();
        changed |= tickMelting(level);
        changed |= tickExtracting(level);
        if (changed) {
            setChanged(level, pos, state);
        }
    }

    /**
     * Ведро / бутылка / любой контейнер жидкости в среднем слоте - сливаем жидкость во входной танк,
     * опустошённый контейнер остаётся в том же слоте (игрок забирает его сам). Работает до
     * рецептов разжижения: на ведре {@code getFluidHandler} есть, а {@code extractor_melting} - нет.
     */
    private boolean tickDrainContainer() {
        ItemStack stack = itemHandler.getStackInSlot(SLOT_MELT);
        if (stack.isEmpty() || stack.getCount() != 1) {
            return false;
        }

        ItemStack work = stack.copy();
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(work).orElse(null);
        if (handler == null) {
            return false; // не контейнер жидкости -> пусть пробует tickMelting
        }

        FluidStack available = handler.drain(inputTank.getSpace(), IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty() || inputTank.fill(available, IFluidHandler.FluidAction.SIMULATE) <= 0) {
            return false;
        }

        FluidStack drained = handler.drain(inputTank.getSpace(), IFluidHandler.FluidAction.EXECUTE);
        inputTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        itemHandler.setStackInSlot(SLOT_MELT, handler.getContainer());
        return true;
    }

    /** Средний слот -> жидкость во входной танк. Прогресс копится, только пока в танке есть место. */
    private boolean tickMelting(Level level) {
        ItemStack in = itemHandler.getStackInSlot(SLOT_MELT);
        if (in.isEmpty()) {
            if (meltProgress != 0) {
                meltProgress = 0;
                return true;
            }
            return false;
        }

        Optional<ExtractorFluidRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(ModRecipes.EXTRACTOR_MELTING_TYPE.get(), new ExtractorFluidRecipeInput(in), level)
                .map(RecipeHolder::value);
        if (recipe.isEmpty()) {
            if (meltProgress != 0) {
                meltProgress = 0;
                return true;
            }
            return false;
        }

        FluidStack out = recipe.get().result();
        if (inputTank.fill(out.copy(), IFluidHandler.FluidAction.SIMULATE) < out.getAmount()) {
            return false; // нет места в танке - ждём, прогресс не трогаем
        }

        meltProgress++;
        if (meltProgress < MELT_TIME) {
            return true;
        }
        meltProgress = 0;
        inputTank.fill(out.copy(), IFluidHandler.FluidAction.EXECUTE);
        itemHandler.extractItem(SLOT_MELT, 1, false);
        return true;
    }

    /** Жидкость из танка (+ доп. ингредиент) -> предмет в выходной слот. */
    private boolean tickExtracting(Level level) {
        ExtractorRecipeInput input = new ExtractorRecipeInput(
                itemHandler.getStackInSlot(SLOT_INGREDIENT),
                itemHandler.getStackInSlot(SLOT_UPGRADE),
                inputTank.getFluid());

        Optional<ExtractorRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(ModRecipes.EXTRACTING_TYPE.get(), input, level)
                .map(RecipeHolder::value);

        if (recipe.isEmpty() || !canOutput(recipe.get())) {
            if (progress != 0) {
                progress = 0;
                return true;
            }
            return false;
        }

        maxProgress = recipe.get().time();
        progress++;
        if (progress < maxProgress) {
            return true;
        }

        craft(recipe.get());
        progress = 0;
        return true;
    }

    private boolean canOutput(ExtractorRecipe recipe) {
        ItemStack result = recipe.result();
        ItemStack out = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void craft(ExtractorRecipe recipe) {
        inputTank.drain(recipe.fluid().amount(), IFluidHandler.FluidAction.EXECUTE);
        if (recipe.consumesIngredient()) {
            itemHandler.extractItem(SLOT_INGREDIENT, 1, false);
        }
        ItemStack result = recipe.result().copy();
        ItemStack out = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            out.grow(result.getCount());
        }
    }
}