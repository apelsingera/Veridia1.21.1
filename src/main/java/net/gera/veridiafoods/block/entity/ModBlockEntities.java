package net.gera.veridiafoods.block.entity;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.block.ModBlocks;
import net.gera.veridiafoods.block.entity.types.cookingtable.CookingTableBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, VeridiaFoods.MOD_ID);

    public static final Supplier<BlockEntityType<CookingTableBlockEntity>> COOKING_TABLE_BE =
            BLOCK_ENTITIES.register("growth_chamber_be", () -> BlockEntityType.Builder.of(
                    CookingTableBlockEntity::new, ModBlocks.COOKING_TABLE.get()).build(null));
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}