package net.gera.veridia.common.block.entity.types;

import net.gera.veridia.common.block.entity.ModBlockEntities;
import net.gera.veridia.common.block.entity.screen.MythicalTableMenu;
import net.gera.veridia.common.recipe.ModRecipes;
import net.gera.veridia.common.recipe.mythicaltable.MythicalTableRecipe;
import net.gera.veridia.common.recipe.mythicaltable.MythicalTableRecipeInput;
import net.gera.veridia.common.recipe.mythicaltable.SlotIngredient;
import net.gera.veridia.annotations.Persisted;
import net.gera.veridia.annotations.PersistedFields;
import net.gera.veridia.annotations.PersistedPrefix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@PersistedPrefix("mythical_table")
public class MythicalTableBlockEntity extends BlockEntity implements MenuProvider {
    public final ItemStackHandler itemHandler = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private static final int INPUT_LAPIS = 0;
    private static final int INPUT_GEMSTONE = 1;
    private static final int INPUT_SLOT = 2;
    private static final int INPUT_SLOT2 = 3;
    private static final int OUTPUT_SLOT = 4;

    protected final ContainerData data;
    @Persisted("progress")
    private int progress = 0;
    @Persisted("max_progress")
    private int maxProgress = 72;

    public MythicalTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MYTHICAL_TABLE_BE.get(), pos, blockState);
        data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> MythicalTableBlockEntity.this.progress;
                    case 1 -> MythicalTableBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int value) {
                // Исправлен switch!
                switch (i) {
                    case 0 -> MythicalTableBlockEntity.this.progress = value;
                    case 1 -> MythicalTableBlockEntity.this.maxProgress = value;
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
        return Component.translatable("block.veridia.mythical_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new MythicalTableMenu(i, inventory, this, this.data);
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
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        pTag.put("inventory", itemHandler.serializeNBT(pRegistries));
        PersistedFields.save(this, pTag);

        super.saveAdditional(pTag, pRegistries);
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

    private void craftItem() {
        Optional<RecipeHolder<MythicalTableRecipe>> recipeHolder = getCurrentRecipe();
        if (recipeHolder.isEmpty()) {
            return;
        }
        MythicalTableRecipe recipe = recipeHolder.get().value();

        consumeSlot(INPUT_LAPIS, recipe.lapis());
        consumeSlot(INPUT_GEMSTONE, recipe.gemstone());
        consumeSlot(INPUT_SLOT, recipe.primary());
        consumeSlot(INPUT_SLOT2, recipe.secondary());

        ItemStack output = recipe.output();
        itemHandler.setStackInSlot(OUTPUT_SLOT, new ItemStack(output.getItem(),
                itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + output.getCount()));
    }

    /** durability=0 - как раньше, просто вычитаем count из стака. durability>0 - предмет не
     * потребляется по count, вместо этого теряет прочность (см. SlotIngredient.matches - туда же
     * вынесена проверка "хватает ли прочности", здесь она уже гарантированно хватает).
     * hurtAndBreak сам ломает/обнуляет стак при достижении maxDamage и учитывает Unbreaking. */
    private void consumeSlot(int slot, Optional<SlotIngredient> ingredient) {
        ingredient.ifPresent(slotIngredient -> {
            if (slotIngredient.durability() > 0) {
                if (level instanceof ServerLevel serverLevel) {
                    ItemStack damaged = itemHandler.getStackInSlot(slot).copy();
                    damaged.hurtAndBreak(slotIngredient.durability(), serverLevel, null, item -> {});
                    itemHandler.setStackInSlot(slot, damaged);
                }
            } else {
                itemHandler.extractItem(slot, slotIngredient.count(), false);
            }
        });
    }

    private void resetProgress() {
        progress = 0;
        maxProgress = 72;
    }

    private boolean hasCraftingFinished() {
        return this.progress >= this.maxProgress;
    }

    private void increaseCraftingProgress() {
        progress++;
    }

    private boolean hasRecipe() {
        Optional<RecipeHolder<MythicalTableRecipe>> recipeHolder = getCurrentRecipe();
        if (recipeHolder.isEmpty()) {
            return false;
        }

        ItemStack output = recipeHolder.get().value().output();
        return canInsertAmountIntoOutputSlot(output.getCount()) && canInsertItemIntoOutputSlot(output);
    }

    private MythicalTableRecipeInput buildRecipeInput() {
        return new MythicalTableRecipeInput(
                itemHandler.getStackInSlot(INPUT_LAPIS),
                itemHandler.getStackInSlot(INPUT_GEMSTONE),
                itemHandler.getStackInSlot(INPUT_SLOT),
                itemHandler.getStackInSlot(INPUT_SLOT2)
        );
    }

    private Optional<RecipeHolder<MythicalTableRecipe>> getCurrentRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(ModRecipes.MYTHICAL_TABLE_TYPE.get(), buildRecipeInput(), level);
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

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);

        itemHandler.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
        PersistedFields.load(this, pTag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}