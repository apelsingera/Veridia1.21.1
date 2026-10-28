package net.gera.veridia.common.fluid;

import net.gera.veridia.Veridia;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;

public class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Veridia.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Veridia.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Veridia.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Veridia.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> TANNIN_FLUID_TYPE = FLUID_TYPES.register("tannin",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.veridia.tannin")
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<Fluid, FlowingFluid> TANNIN_STILL = FLUIDS.register("tannin",
            () -> new BaseFlowingFluid.Source(tanninProperties()));

    public static final DeferredHolder<Fluid, FlowingFluid> TANNIN_FLOWING = FLUIDS.register("flowing_tannin",
            () -> new BaseFlowingFluid.Flowing(tanninProperties()));

    public static final DeferredBlock<LiquidBlock> TANNIN_BLOCK = BLOCKS.register("tannin",
            () -> new LiquidBlock(TANNIN_STILL.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                    .mapColor(MapColor.DIRT)
                    .noLootTable()));

    public static final DeferredItem<Item> TANNIN_BUCKET = ITEMS.registerItem("tannin_bucket",
            props -> new BucketItem(TANNIN_STILL.get(), props),
            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    private static final CauldronInteraction.InteractionMap TANNIN_CAULDRON_INTERACTIONS =
            CauldronInteraction.newInteractionMap("tannin");
    public static final DeferredBlock<LayeredCauldronBlock> TANNIN_CAULDRON = BLOCKS.register("tannin_cauldron",
            () -> new LayeredCauldronBlock(Biome.Precipitation.NONE, TANNIN_CAULDRON_INTERACTIONS,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).noLootTable()));

    public static void registerCauldronInteractions() {
        Map<Item, CauldronInteraction> tanninMap = TANNIN_CAULDRON_INTERACTIONS.map();
        CauldronInteraction.addDefaultInteractions(tanninMap);
        tanninMap.put(Items.BUCKET, (state, level, pos, player, hand, stack) -> CauldronInteraction.fillBucket(
                state, level, pos, player, hand, stack, new ItemStack(TANNIN_BUCKET.get()),
                s -> s.getValue(LayeredCauldronBlock.LEVEL) == 3, SoundEvents.BUCKET_FILL));

        CauldronInteraction.EMPTY.map().put(TANNIN_BUCKET.get(),
                (state, level, pos, player, hand, stack) -> CauldronInteraction.emptyBucket(
                        level, pos, player, hand, stack,
                        TANNIN_CAULDRON.get().defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
                        SoundEvents.BUCKET_EMPTY));
    }

    private static BaseFlowingFluid.Properties tanninProperties() {
        return new BaseFlowingFluid.Properties(TANNIN_FLUID_TYPE, TANNIN_STILL, TANNIN_FLOWING)
                .block(TANNIN_BLOCK)
                .bucket(TANNIN_BUCKET);
    }

    public static final DeferredHolder<FluidType, FluidType> CORRUPTION_FLUID_TYPE = FLUID_TYPES.register("corruption",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.veridia.corruption")
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<Fluid, FlowingFluid> CORRUPTION_STILL = FLUIDS.register("corruption",
            () -> new BaseFlowingFluid.Source(corruptionProperties()));

    public static final DeferredHolder<Fluid, FlowingFluid> CORRUPTION_FLOWING = FLUIDS.register("flowing_corruption",
            () -> new BaseFlowingFluid.Flowing(corruptionProperties()));

    public static final DeferredBlock<CorruptionLiquidBlock> CORRUPTION_BLOCK = BLOCKS.register("corruption",
            () -> new CorruptionLiquidBlock(CORRUPTION_STILL.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                    .mapColor(MapColor.COLOR_BLACK)
                    .noLootTable()));

    public static final DeferredItem<Item> CORRUPTION_BUCKET = ITEMS.registerItem("corruption_bucket",
            props -> new BucketItem(CORRUPTION_STILL.get(), props),
            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));

    private static BaseFlowingFluid.Properties corruptionProperties() {
        return new BaseFlowingFluid.Properties(CORRUPTION_FLUID_TYPE, CORRUPTION_STILL, CORRUPTION_FLOWING)
                .block(CORRUPTION_BLOCK)
                .bucket(CORRUPTION_BUCKET)
                .tickRate(15)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2);
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}