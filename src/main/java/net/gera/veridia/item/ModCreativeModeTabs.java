package net.gera.veridia.item;

import net.gera.veridia.Veridia;
import net.gera.veridia.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Veridia.MOD_ID);

    public static final Supplier<CreativeModeTab> PTAB = CREATIVE_MODE_TAB.register("ptab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.PLANT_CLUMP.get()))
                    .title(Component.translatable("ptab.veridia.ptab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.PLANT_STRING);
                        output.accept(ModItems.PLANT_CLUMP);
                        output.accept(ModItems.DANDELIONS_ROOT);
                        output.accept(ModItems.GARLIC);
                        output.accept(ModItems.GARLIC_SEEDS);
                        output.accept(ModItems.BLUE_BERRIES);
                    }).build());
    public static final Supplier<CreativeModeTab> PTABM = CREATIVE_MODE_TAB.register("ptabm",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.COPPER_INGOT.get()))
                    .title(Component.translatable("ptabm.veridia.ptabm"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.COPPER_INGOT);
                        output.accept(ModItems.COPPER_ROD);
                        output.accept(ModItems.DOUBLED_COPPER_INGOT);
                        output.accept(ModItems.IMPURE_COPPER_INGOT);
                        output.accept(ModItems.RAW_COPPER_SMALL);
                        output.accept(ModItems.RAW_COPPER_MEDIUM);
                        output.accept(ModItems.RAW_COPPER_BIG);

                        output.accept(ModItems.COPPER_SWORD);
                        output.accept(ModItems.COPPER_PICKAXE);
                        output.accept(ModItems.COPPER_AXE);
                        output.accept(ModItems.COPPER_SHOVEL);
                        output.accept(ModItems.COPPER_HOE);
                        output.accept(ModItems.COPPER_HELMET);
                        output.accept(ModItems.COPPER_CHESTPLATE);
                        output.accept(ModItems.COPPER_LEGGINGS);
                        output.accept(ModItems.COPPER_BOOTS);
                        output.accept(ModItems.COPPER_HORSE_ARMOR);
                        //
                        output.accept(ModItems.RAW_TIN_SMALL);
                        output.accept(ModItems.RAW_TIN_MEDIUM);
                        output.accept(ModItems.RAW_TIN_BIG);
                        output.accept(ModItems.TIN_INGOT);
                        output.accept(ModItems.IMPURE_TIN_INGOT);
                        output.accept(ModBlocks.TIN_BLOCK);
                        output.accept(ModBlocks.TIN_STAIRS); output.accept(ModBlocks.TIN_SLAB);
                        //
                        output.accept(ModBlocks.TIN_FENCE);  output.accept(ModBlocks.TIN_FENCE_GATE); output.accept(ModBlocks.TIN_WALL);
                        //
                        output.accept(ModBlocks.TIN_DOOR); output.accept(ModBlocks.TIN_TRAPDOOR);
                        output.accept(ModBlocks.TIN_PRESSURE_PLATE); output.accept(ModBlocks.TIN_BUTTON);
                        //
                        output.accept(ModItems.BRONZE_INGOT);
                        output.accept(ModItems.IMPURE_BRONZE_INGOT);
                        output.accept(ModItems.BRONZE_SWORD);
                        output.accept(ModItems.BRONZE_PICKAXE);
                        output.accept(ModItems.BRONZE_AXE);
                        output.accept(ModItems.BRONZE_SHOVEL);
                        output.accept(ModItems.BRONZE_HOE);
                        output.accept(ModItems.BRONZE_HELMET);
                        output.accept(ModItems.BRONZE_CHESTPLATE);
                        output.accept(ModItems.BRONZE_LEGGINGS);
                        output.accept(ModItems.BRONZE_BOOTS);
                        //
                        output.accept(ModItems.WROUGHT_IRON_INGOT);
                        output.accept(ModItems.IMPURE_WROUGHT_IRON_INGOT);
                    }).build());
                        public static final Supplier<CreativeModeTab> PTABG = CREATIVE_MODE_TAB.register("ptabg",
                       () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.AZOTIC_TOPAZ.get()))
                       .title(Component.translatable("ptab.veridia.ptab")).displayItems((itemDisplayParameters, output) -> {
                             output.accept(ModItems.AZOTIC_TOPAZ);
                             output.accept(ModItems.DATOLITE);
                             output.accept(ModBlocks.AZOTIC_TOPAZ_END_ORE);
                             output.accept(ModBlocks.DATOLITE_NETHER_ORE);
                         }).build());



    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
