package net.gera.veridia.common.block.entity.types;

import net.gera.veridia.common.block.entity.ModBlockEntities;
import net.gera.veridia.common.block.entity.screen.RunicPedestalMenu;
import net.gera.veridia.common.mana.ManaHandler;
import net.gera.veridia.common.mana.ManaProgression;
import net.gera.veridia.common.recipe.ModRecipes;
import net.gera.veridia.common.recipe.mythicaltable.SlotIngredient;
import net.gera.veridia.common.recipe.runicpedestal.RunicPedestalRecipe;
import net.gera.veridia.common.recipe.runicpedestal.RunicPedestalRecipeInput;
import net.gera.veridia.common.recipe.runicpedestal.RunicPedestalRingRecipe;
import net.gera.veridia.common.sound.ModSounds;
import net.gera.veridia.annotations.Persisted;
import net.gera.veridia.annotations.PersistedFields;
import net.gera.veridia.annotations.PersistedPrefix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Крафтит одноразовую руну (см. TierToolItem/ModManaItems.itemForStep) для ТЕКУЩЕГО шага
 * прогресса игрока, открывшего меню. Материалы и опыт списываются со слотов пьедестала
 * (не из инвентаря игрока), рецепт ищется через RecipeManager по RunicPedestalRecipe с полем
 * step == ManaProgression.progress(player) - руны применяются строго по порядку.
 */
@PersistedPrefix("runic_pedestal")
public class RunicPedestalBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TOTAL_SLOTS = 5;

    public final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    public static final int MATERIAL_SLOT_1 = 0;
    public static final int MATERIAL_SLOT_2 = 1;
    public static final int MATERIAL_SLOT_3 = 2;
    public static final int MATERIAL_SLOT_4 = 3;
    public static final int OUTPUT_SLOT = 4;
    private static final int MATERIAL_SLOT_COUNT = 4;

    protected final ContainerData data;
    @Persisted("progress")
    private int progress = 0;
    @Persisted("max_progress")
    private int maxProgress = 1500;

    @Nullable
    @Persisted("active_player")
    private UUID activePlayerId;

    public RunicPedestalBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RUNIC_PEDESTAL_BE.get(), pos, blockState);
        data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> RunicPedestalBlockEntity.this.progress;
                    case 1 -> RunicPedestalBlockEntity.this.maxProgress;
                    case 2 -> RunicPedestalBlockEntity.this.currentRecipeXpLevels();
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int value) {
                switch (i) {
                    case 0 -> RunicPedestalBlockEntity.this.progress = value;
                    case 1 -> RunicPedestalBlockEntity.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.veridia.runic_pedestal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        activePlayerId = player.getUUID();
        return new RunicPedestalMenu(containerId, inventory, this, this.data);
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
        PersistedFields.save(this, tag);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        if (itemHandler.getSlots() != TOTAL_SLOTS) {
            int oldSlots = itemHandler.getSlots();
            ItemStack[] preserved = new ItemStack[oldSlots];
            for (int i = 0; i < oldSlots; i++) {
                preserved[i] = itemHandler.getStackInSlot(i);
            }
            itemHandler.setSize(TOTAL_SLOTS);
            for (int i = 0; i < Math.min(oldSlots, TOTAL_SLOTS); i++) {
                itemHandler.setStackInSlot(i, preserved[i]);
            }
        }
        PersistedFields.load(this, tag);
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
        advancePulseAnimation(level);

        if (!isCrafting()) {
            return;
        }

        ServerPlayer player = resolveActivePlayer(level);
        if (player == null || !ManaHandler.isUnlocked(player)) {
            resetProgress();
            return;
        }

        int step = ManaProgression.progress(player);
        Optional<RunicPedestalRecipe> recipe = currentRecipe(level, step);
        if (recipe.isEmpty() || !materialsPresent(level, recipe.get()) || !outputHasRoom(level, recipe.get())) {
            resetProgress();
            return;
        }

        increaseCraftingProgress();
        setChanged(level, pos, state);

        if ((progress - 1) % PULSE_INTERVAL_TICKS == 0) {
            playRandomCraftingSound(level);
            startPulseAnimation();
        }

        if (hasCraftingFinished()) {
            finishCraft(level, recipe.get());
            resetProgress();
        }
    }

    public boolean requestCraft(Level level, ServerPlayer player) {
        Optional<RunicPedestalRingRecipe> ringRecipe = currentRingRecipe(level, player);
        if (ringRecipe.isPresent()) {
            return craftRingRecipe(level, player, ringRecipe.get());
        }

        if (!ManaHandler.isUnlocked(player)) {
            player.displayClientMessage(Component.translatable("message.veridia.runic_pedestal_mana_locked"), true);
            return false;
        }

        if (isCrafting()) {
            player.displayClientMessage(Component.translatable("message.veridia.runic_pedestal_already_crafting"), true);
            return false;
        }

        int step = ManaProgression.progress(player);
        Optional<RunicPedestalRecipe> recipeOpt = currentRecipe(level, step);
        if (recipeOpt.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.veridia.runic_pedestal_no_recipe", step), true);
            return false;
        }

        RunicPedestalRecipe recipe = recipeOpt.get();
        if (!materialsPresent(level, recipe)) {
            player.displayClientMessage(Component.translatable("message.veridia.runic_pedestal_missing_materials"), true);
            return false;
        }

        if (!outputHasRoom(level, recipe)) {
            player.displayClientMessage(Component.translatable("message.veridia.runic_pedestal_output_full"), true);
            return false;
        }

        if (player.experienceLevel < recipe.xpLevels()) {
            player.displayClientMessage(Component.translatable("message.veridia.runic_pedestal_not_enough_xp", recipe.xpLevels()), true);
            return false;
        }

        player.giveExperienceLevels(-recipe.xpLevels());
        progress = 1;
        setChanged();
        player.displayClientMessage(Component.translatable("message.veridia.runic_pedestal_started"), true);
        return true;
    }

    @Nullable
    private ServerPlayer resolveActivePlayer(Level level) {
        if (activePlayerId == null) return null;
        Player player = level.getPlayerByUUID(activePlayerId);
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private RunicPedestalRecipeInput buildRecipeInput() {
        NonNullList<ItemStack> inputs = NonNullList.withSize(MATERIAL_SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < MATERIAL_SLOT_COUNT; i++) {
            inputs.set(i, itemHandler.getStackInSlot(i));
        }
        return new RunicPedestalRecipeInput(inputs);
    }

    private Optional<RunicPedestalRecipe> currentRecipe(Level level, int step) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.RUNIC_PEDESTAL_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.step() == step)
                .findFirst();
    }

    private boolean materialsPresent(Level level, RunicPedestalRecipe recipe) {
        return recipe.matches(buildRecipeInput(), level);
    }

    private Optional<RunicPedestalRingRecipe> currentRingRecipe(Level level, ServerPlayer player) {
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.RUNIC_PEDESTAL_RING_TYPE.get()).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.isUnlockedFor(player))
                .filter(recipe -> recipe.matches(buildRecipeInput(), level))
                .findFirst();
    }
    private boolean craftRingRecipe(Level level, ServerPlayer player, RunicPedestalRingRecipe recipe) {
        if (isCrafting()) {
            player.displayClientMessage(Component.translatable("message.veridia.runic_pedestal_already_crafting"), true);
            return false;
        }

        ItemStack result = recipe.getResultItem(level.registryAccess());
        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        boolean roomForOutput = output.isEmpty() || (output.is(result.getItem()) && output.getCount() < output.getMaxStackSize());
        if (!roomForOutput) {
            player.displayClientMessage(Component.translatable("message.veridia.runic_pedestal_output_full"), true);
            return false;
        }

        List<SlotIngredient> materials = recipe.materials();
        for (int i = 0; i < materials.size() && i < MATERIAL_SLOT_COUNT; i++) {
            itemHandler.extractItem(i, materials.get(i).count(), false);
        }

        if (output.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
        }

        startPulseAnimation();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, worldPosition, ModSounds.RUNIC_PEDESTAL_SFX_1.get(), SoundSource.BLOCKS, 5.0F, 1.2F);
        }
        player.displayClientMessage(Component.translatable("message.veridia.runic_pedestal_ring_crafted",
                Component.translatable(result.getDescriptionId())), true);
        return true;
    }

    private int currentRecipeXpLevels() {
        if (level == null) return 0;
        ServerPlayer player = resolveActivePlayer(level);
        if (player == null || !ManaHandler.isUnlocked(player)) return 0;
        int step = ManaProgression.progress(player);
        return currentRecipe(level, step).map(RunicPedestalRecipe::xpLevels).orElse(0);
    }

    private boolean outputHasRoom(Level level, RunicPedestalRecipe recipe) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        return output.isEmpty() || (output.is(result.getItem()) && output.getCount() < output.getMaxStackSize());
    }

    private void finishCraft(Level level, RunicPedestalRecipe recipe) {
        RunicPedestalRecipeInput input = buildRecipeInput();

        List<SlotIngredient> materials = recipe.materials();
        for (int i = 0; i < materials.size() && i < MATERIAL_SLOT_COUNT; i++) {
            itemHandler.extractItem(i, materials.get(i).count(), false);
        }

        ItemStack result = recipe.assemble(input, level.registryAccess());

        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
        }

        startPulseAnimation();
    }

    private static final int PULSE_INTERVAL_TICKS = 40; // 2 секунды при 20 тиках/сек
    private static final int PULSE_ANIMATION_TICKS = 8; // длительность роста самого кольца (0.5 сек)
    private static final double PULSE_MIN_RADIUS = 0.2;
    private static final double PULSE_MAX_RADIUS = 1.8;
    private static final int PULSE_POINTS = 40;

    /** -1 = кольцо сейчас не анимируется, иначе - номер текущего кадра роста (см. advancePulseAnimation). */
    private int pulseAnimationTick = -1;

    /** Один из трёх звуков пьедестала с равной (1/3) вероятностью - раз в PULSE_INTERVAL_TICKS во время крафта. */
    private void playRandomCraftingSound(Level level) {
        SoundEvent[] sounds = {
                ModSounds.RUNIC_PEDESTAL_SFX_1.get(),
                ModSounds.RUNIC_PEDESTAL_SFX_2.get(),
                ModSounds.RUNIC_PEDESTAL_SFX_3.get()
        };
        SoundEvent chosen = sounds[level.random.nextInt(sounds.length)];
        level.playSound(null, worldPosition, chosen, SoundSource.BLOCKS, 5.0F, 0.8F);
    }

    private void startPulseAnimation() {
        pulseAnimationTick = 0;
    }

    /** Рисует один кадр растущего плоского кольца (буквой "O"), если анимация сейчас активна. */
    private void advancePulseAnimation(Level level) {
        if (pulseAnimationTick < 0) {
            return;
        }
        spawnRingFrame(level, pulseAnimationTick);
        pulseAnimationTick++;
        if (pulseAnimationTick > PULSE_ANIMATION_TICKS) {
            pulseAnimationTick = -1;
        }
    }

    private static final double GROUND_RING_MIN_RADIUS = 1.0;
    private static final double GROUND_RING_MAX_RADIUS = 4.5;

    /**
     * Два фиолетовых плоских кольца (все точки на одной высоте Y, без разброса по вертикали и
     * без скорости у частиц), растущие синхронно за PULSE_ANIMATION_TICKS кадров - маленькое
     * у верхушки пьедестала (как раньше) и широкое на уровне земли, на блок ниже первого.
     */
    private void spawnRingFrame(Level level, int animationTick) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        double t = (double) animationTick / PULSE_ANIMATION_TICKS;
        double centerX = worldPosition.getX() + 0.5;
        double centerZ = worldPosition.getZ() + 0.5;
        double topY = worldPosition.getY() + 1.1;

        double topRadius = PULSE_MIN_RADIUS + (PULSE_MAX_RADIUS - PULSE_MIN_RADIUS) * t;
        spawnRing(serverLevel, centerX, topY, centerZ, topRadius, PULSE_POINTS);

        double groundRadius = GROUND_RING_MIN_RADIUS + (GROUND_RING_MAX_RADIUS - GROUND_RING_MIN_RADIUS) * t;
        spawnRing(serverLevel, centerX, topY - 1.0, centerZ, groundRadius, PULSE_POINTS * 2);
    }

    private void spawnRing(ServerLevel level, double centerX, double y, double centerZ, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double px = centerX + Math.cos(angle) * radius;
            double pz = centerZ + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.WITCH, px, y, pz, 0, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void resetProgress() {
        progress = 0;
    }

    private boolean hasCraftingFinished() {
        return this.progress >= this.maxProgress;
    }

    private void increaseCraftingProgress() {
        progress++;
    }

    public boolean isCrafting() {
        return progress > 0;
    }
}