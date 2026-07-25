package net.gera.veridiafoods.block.entity.types.cookingtable;

import net.gera.veridiafoods.block.entity.ModBlockEntities;
import net.gera.veridiafoods.block.entity.screen.CookingTableMenu;
import net.gera.veridiafoods.item.ModItems;
import net.gera.veridiafoods.recipe.ModRecipes;
import net.gera.veridiafoods.recipe.cookingtable.CookingTableRecipe;
import net.gera.veridiafoods.recipe.cookingtable.CookingTableRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CookingTableBlockEntity extends BlockEntity implements MenuProvider {
    public final ItemStackHandler itemHandler = new ItemStackHandler(12) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            assert level != null;
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == TOOL_INPUT1) {
                return stack.is(ModItems.CRUDE_KNIFE.get());
            }
            if (slot == TOOL_INPUT2) {
                return stack.is(ModItems.CRUDE_HAMMER.get());
            }
            return true;
        }
    };
    //INPUT INT
    public static final int INPUT_SLOT1 = 0;
    public static final int INPUT_SLOT2 = 1;
    public static final int INPUT_SLOT3 = 2;
    public static final int INPUT_SLOT4 = 3;
    public static final int INPUT_SLOT5 = 4;
    public static final int INPUT_SLOT6 = 5;
    public static final int INPUT_SLOT7 = 6;
    public static final int INPUT_SLOT8 = 7;
    public static final int INPUT_SLOT9 = 8;
    public static final int TOOL_INPUT1 = 9;  // knife
    public static final int TOOL_INPUT2 = 10; // hammer
    public static final int OUTPUT_SLOT = 11;

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 72;

    public CookingTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COOKING_TABLE_BE.get(), pos, blockState);
        data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> CookingTableBlockEntity.this.progress;
                    case 1 -> CookingTableBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int value) {
                switch (i) {
                    case 0:
                        CookingTableBlockEntity.this.progress = value;
                        break;
                    case 1:
                        CookingTableBlockEntity.this.maxProgress = value;
                        break;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.veridiafoods.cooking_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new CookingTableMenu(i, inventory, this, this.data);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        assert this.level != null;
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        pTag.put("inventory", itemHandler.serializeNBT(pRegistries));
        pTag.putInt("cooking_table.progress", progress);
        pTag.putInt("cooking_table.max_progress", maxProgress);

        super.saveAdditional(pTag, pRegistries);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);

        itemHandler.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
        progress = pTag.getInt("cooking_table.progress");
        maxProgress = pTag.getInt("cooking_table.max_progress");
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (hasRecipe()) {
            increaseCraftingProgress();
            setChanged(level, blockPos, blockState);

            if (hasCraftingFinished()) {
                craftItem();
                resetProgress();
            }

        } else {
            resetProgress();
        }
    }

    ///
    private CookingTableRecipeInput buildRecipeInput() {
        NonNullList<ItemStack> inputs = NonNullList.withSize(11, ItemStack.EMPTY);
        for (int i = INPUT_SLOT1; i <= TOOL_INPUT2; i++) {
            inputs.set(i, itemHandler.getStackInSlot(i));
        }
        return new CookingTableRecipeInput(inputs);
    }

    private void craftItem() {
        Optional<RecipeHolder<CookingTableRecipe>> recipeHolder = getCurrentRecipe();
        if (recipeHolder.isEmpty()) {
            return;
        }
        CookingTableRecipe recipe = recipeHolder.get().value();
        ItemStack output = recipe.output();

        CookingTableRecipeInput inputSnapshot = buildRecipeInput();

        List<Integer> usedSlots = new ArrayList<>();
        for (Ingredient ingredient : recipe.ingredients()) {
            for (int slot = INPUT_SLOT1; slot <= INPUT_SLOT9; slot++) {
                if (usedSlots.contains(slot)) continue;
                if (ingredient.test(itemHandler.getStackInSlot(slot))) {
                    itemHandler.extractItem(slot, 1, false);
                    usedSlots.add(slot);
                    break;
                }
            }
        }

        if (!recipe.consumeContainers()) {
            NonNullList<ItemStack> remaining = recipe.getRemainingItems(inputSnapshot);
            for (int slot : usedSlots) {
                ItemStack remainder = remaining.get(slot);
                if (remainder.isEmpty()) continue;

                ItemStack current = itemHandler.getStackInSlot(slot);
                if (current.isEmpty()) {
                    itemHandler.setStackInSlot(slot, remainder.copy());
                } else if (ItemStack.isSameItemSameComponents(current, remainder)
                        && current.getCount() + remainder.getCount() <= current.getMaxStackSize()) {
                    current.grow(remainder.getCount());
                } else if (level != null && !level.isClientSide()) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                            worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, remainder.copy());
                }
            }
        }

        itemHandler.setStackInSlot(OUTPUT_SLOT, new ItemStack(output.getItem(),
                itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + output.getCount()));
    }

    private void resetProgress() {
        progress = 0;
        maxProgress = 60;
    }

    private boolean hasCraftingFinished() {
        return this.progress >= this.maxProgress;
    }

    private void increaseCraftingProgress() {
        progress++;
    }

    private boolean hasRecipe() {
        Optional<RecipeHolder<CookingTableRecipe>> recipe = getCurrentRecipe();
        if (recipe.isEmpty()) {
            return false;
        }

        ItemStack output = recipe.get().value().output();
        return canInsertAmountIntoOutputSlot(output.getCount()) &&
                canInsertItemIntoOutputSlot(output);
    }

    private Optional<RecipeHolder<CookingTableRecipe>> getCurrentRecipe() {
        assert this.level != null;
        return this.level.getRecipeManager().getRecipeFor(ModRecipes.COOKING_TABLE_TYPE.get(),
                buildRecipeInput(), level);
    }

    private boolean canInsertItemIntoOutputSlot(ItemStack output) {
        return itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() ||
                itemHandler.getStackInSlot(OUTPUT_SLOT).getItem() == output.getItem();
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        int maxCount = itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() ? 64 : itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
        int currentCount = itemHandler.getStackInSlot(OUTPUT_SLOT).getCount();

        return maxCount >= currentCount + count;
    }
    ///
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}