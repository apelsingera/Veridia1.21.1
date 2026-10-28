package net.gera.veridia.common.block.entity;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.types.BuildingMaterialsBlockEntity;
import net.gera.veridia.common.block.entity.types.CabinetBlockEntity;
import net.gera.veridia.common.block.entity.types.CookingTableBlockEntity;
import net.gera.veridia.common.block.entity.types.ExtractorBlockEntity;
import net.gera.veridia.common.block.entity.types.FoodStorageBlockEntity;
import net.gera.veridia.common.block.entity.types.LumberjackLogBlockEntity;
import net.gera.veridia.common.block.entity.types.MythicalTableBlockEntity;
import net.gera.veridia.common.block.entity.types.RunicPedestalBlockEntity;
import net.gera.veridia.common.block.entity.types.SpiritualRunestoneBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Veridia.MOD_ID);

    public static final Supplier<BlockEntityType<CookingTableBlockEntity>> COOKING_TABLE_BE =
            BLOCK_ENTITIES.register("cooking_table_be", () -> BlockEntityType.Builder.of(
                    CookingTableBlockEntity::new, ModBlocks.COOKING_TABLE.get()).build(null));

    public static final Supplier<BlockEntityType<MythicalTableBlockEntity>> MYTHICAL_TABLE_BE =
            BLOCK_ENTITIES.register("mythical_table_be", () -> BlockEntityType.Builder.of(
                    MythicalTableBlockEntity::new, ModBlocks.MYTHICAL_TABLE.get()).build(null));

    public static final Supplier<BlockEntityType<RunicPedestalBlockEntity>> RUNIC_PEDESTAL_BE =
            BLOCK_ENTITIES.register("runic_pedestal_be", () -> BlockEntityType.Builder.of(
                    RunicPedestalBlockEntity::new, ModBlocks.RUNIC_PEDESTAL.get()).build(null));

    public static final Supplier<BlockEntityType<ExtractorBlockEntity>> EXTRACTOR_BE =
            BLOCK_ENTITIES.register("extractor_be", () -> BlockEntityType.Builder.of(
                    ExtractorBlockEntity::new, ModBlocks.EXTRACTOR.get()).build(null));

    public static final Supplier<BlockEntityType<LumberjackLogBlockEntity>> LUMBERJACK_LOG_BE =
            BLOCK_ENTITIES.register("lumberjack_log_be", () -> BlockEntityType.Builder.of(
                    LumberjackLogBlockEntity::new, ModBlocks.LUMBERJACK_LOG.get()).build(null));

    public static final Supplier<BlockEntityType<BuildingMaterialsBlockEntity>> BUILDING_MATERIALS_BE =
            BLOCK_ENTITIES.register("building_materials_be", () -> BlockEntityType.Builder.of(
                    BuildingMaterialsBlockEntity::new, ModBlocks.BUILDING_MATERIALS.get()).build(null));

    public static final Supplier<BlockEntityType<FoodStorageBlockEntity>> FOOD_STORAGE_BE =
            BLOCK_ENTITIES.register("food_storage_be", () -> BlockEntityType.Builder.of(
                    FoodStorageBlockEntity::new, ModBlocks.FOOD_STORAGE.get()).build(null));

    public static final Supplier<BlockEntityType<SpiritualRunestoneBlockEntity>> SPIRITUAL_RUNESTONE_BE =
            BLOCK_ENTITIES.register("spiritual_runestone_be", () -> BlockEntityType.Builder.of(
                    SpiritualRunestoneBlockEntity::new, ModBlocks.SPIRITUAL_RUNESTONE.get()).build(null));

    public static final Supplier<BlockEntityType<CabinetBlockEntity>> CABINET_BE =
            BLOCK_ENTITIES.register("cabinet_be", () -> BlockEntityType.Builder.of(
                    CabinetBlockEntity::new, ModBlocks.ASHEN_CABINET.get(), ModBlocks.CHARRED_CABINET.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        eventBus.addListener(ModBlockEntities::addBlocksToVanillaTypes);
    }

    // Наши блоки в ванильных блок-сущностях: без этого BlockEntity на них падает с "Invalid block entity"
    private static void addBlocksToVanillaTypes(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.BRUSHABLE_BLOCK, ModBlocks.SUSPICIOUS_INCINERATED_DIRT.get());
    }
}