package net.gera.veridiafoods.item;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VeridiaFoods.MOD_ID);

    public static final Supplier<CreativeModeTab> PTAB = CREATIVE_MODE_TAB.register("ptab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.PLANT_CLUMP.get()))
                    .title(Component.translatable("ptab.veridiafoods.ptab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.PLANT_STRING);
                        output.accept(ModItems.FIBER_NET);
                        output.accept(ModItems.PLANT_CLUMP);
                        output.accept(ModItems.DANDELIONS_ROOT);
                        output.accept(ModItems.TOMATO_CHERRY);
                        output.accept(ModItems.RED_ONION);
                        output.accept(ModItems.GARLIC);
                        output.accept(ModItems.GARLIC_SEEDS);
                        output.accept(ModItems.RED_BELL_PEPPER);
                        output.accept(ModItems.RED_BELL_PEPPER_SLICE);
                        output.accept(ModItems.BELL_PEPPER_SEEDS);
                        output.accept(ModItems.BLUE_BERRIES);
                        output.accept(ModItems.GOOSE_BERRIES);
                        output.accept(ModItems.CRANBERRIES);

                        output.accept(ModItems.BLUE_BERRY_JUICE);
                        output.accept(ModItems.SUPER_BLUE_BERRY_JUICE);
                        output.accept(ModItems.CRANBERRY_JUICE);
                        output.accept(ModItems.SUPER_CRANBERRY_JUICE);
                        output.accept(ModItems.GOOSE_BERRY_JUICE);
                        output.accept(ModItems.SUPER_GOOSE_BERRY_JUICE);

                        output.accept(ModItems.PORKCHOP_SLICE);
                        output.accept(ModItems.GROUND_PORK);
                        output.accept(ModItems.PORK_PATTY);
                        output.accept(ModItems.COOKED_PORK_PATTY);
                        output.accept(ModItems.GROUND_BEEF);
                        output.accept(ModItems.BEEF_PATTY);
                        output.accept(ModItems.COOKED_BEEF_PATTY);

                        output.accept(ModItems.UNFINISHED_STUFFED_PEPPER);
                        output.accept(ModItems.STUFFED_PEPPER);
                        output.accept(ModItems.BEEF_BURGER);
                        output.accept(ModItems.CHICKEN_LEGS);
                        output.accept(ModItems.BEEF_W_POTATOES);
                        output.accept(ModItems.PIQUANT_ELITE_FISH_DISH);
                        output.accept(ModItems.NICE_BREAKFAST);

                        output.accept(ModItems.BLUEBERRY_PIE);
                        output.accept(ModItems.BLUEBERRY_PIE_SLICE);
                        output.accept(ModItems.GOOSEBERRY_PIE);
                        output.accept(ModItems.GOOSEBERRY_PIE_SLICE);
                        output.accept(ModItems.CRANBERRY_PIE);
                        output.accept(ModItems.CRANBERRY_PIE_SLICE);

                        output.accept(ModItems.WHEAT_FLOUR);
                        output.accept(ModItems.DOUGH);
                        output.accept(ModItems.PASTA);
                        output.accept(ModItems.TASTY_PASTA);
                        output.accept(ModItems.PASTA_BUCKET);
                        output.accept(ModItems.COOKED_PASTA_BUCKET);
                        output.accept(ModItems.CARROT_PASTA);
                        output.accept(ModItems.RONALDO_PASTA);
                        output.accept(ModItems.POTATO_PUREE);
                        /// - /// - /// - /// - /// - /// - /// - ///
                        output.accept(ModItems.MILK_N_BUTTER);
                        output.accept(ModItems.BUTTER);
                        output.accept(ModItems.CHEESE);
                        output.accept(ModItems.BOILED_EGG);
                        output.accept(ModItems.EGG_BUCKET);
                        output.accept(ModItems.BOILED_EGG_BUCKET);


                        output.accept(ModItems.CRUDE_HAMMER);
                        output.accept(ModItems.CRUDE_KNIFE);
                        output.accept(ModItems.SS_KNIFE);

                        output.accept(ModItems.EXOTIC_SEASONING1);
                        output.accept(ModItems.EXOTIC_SEASONING2);
                        output.accept(ModItems.EXOTIC_SEASONING3);

                        output.accept(ModBlocks.COOKING_TABLE);
                        output.accept(ModBlocks.SCULK_LOG);
                        output.accept(ModBlocks.STRIPPED_SCULK_LOG);
                        output.accept(ModBlocks.SCULK_WOOD);
                        output.accept(ModBlocks.STRIPPED_SCULK_WOOD);
                        output.accept(ModBlocks.SCULK_PLANKS);
                        output.accept(ModBlocks.SCULK_LEAVES);
                        output.accept(ModBlocks.SCULK_SPROUT);
                    }).build());
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
