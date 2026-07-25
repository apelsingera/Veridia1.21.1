package net.gera.veridiafoods.item;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.block.ModBlocks;
import net.gera.veridiafoods.item.custom.tools.CrudeKnifeItem;
import net.gera.veridiafoods.item.custom.tools.CrudeHammerItem;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VeridiaFoods.MOD_ID);

    public static final DeferredItem<Item> PLANT_STRING = ITEMS.register("plant_string",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLANT_CLUMP = ITEMS.register("plant_clump",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRUDE_KNIFE = ITEMS.register("crude_knife",
            CrudeKnifeItem::new);
    public static final DeferredItem<Item> CRUDE_HAMMER = ITEMS.register("crude_hammer",
            CrudeHammerItem::new);
    public static final DeferredItem<Item> DANDELIONS_ROOT = ITEMS.register("dandelion_root",
            () -> new Item(new Item.Properties().food(ModFoodProps.DANDELION_ROOTS)));

    public static final DeferredItem<Item> GARLIC_SEEDS = ITEMS.register("garlic_seeds",
            () -> new ItemNameBlockItem(ModBlocks.GARLIC_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> TOMATO = ITEMS.register("tomato",
            () -> new Item(new Item.Properties().food(ModFoodProps.GENERIC_VEGETABLE)));
    public static final DeferredItem<Item> RED_ONION = ITEMS.register("red_onion",
            () -> new Item(new Item.Properties().food(ModFoodProps.GENERIC_VEGETABLE)));
    public static final DeferredItem<Item> GARLIC = ITEMS.register("garlic",
            () -> new Item(new Item.Properties().food(ModFoodProps.GENERIC_VEGETABLE)));
    public static final DeferredItem<Item> BELL_PEPPER_SEEDS = ITEMS.register("bell_pepper_seeds",
            () -> new ItemNameBlockItem(ModBlocks.BELL_PEPPER_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> RED_BELL_PEPPER = ITEMS.register("red_bell_pepper",
            () -> new Item(new Item.Properties().food(ModFoodProps.RED_BELL_PEPPER  )));
    public static final DeferredItem<Item> RED_BELL_PEPPER_SLICE = ITEMS.register("red_bell_pepper_slice",
            () -> new Item(new Item.Properties().food(ModFoodProps.RED_BELL_PEPPER_SLICE  )));

    public static final DeferredItem<Item> WHEAT_FLOUR = ITEMS.register("wheat_flour",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DOUGH = ITEMS.register("dough",
            () -> new Item(new Item.Properties().food(ModFoodProps.RAW_EGG)));
    public static final DeferredItem<Item> BOILED_EGG = ITEMS.register("boiled_egg",
            () -> new Item(new Item.Properties().food(ModFoodProps.EGG)));
    public static final DeferredItem<Item> EGG_BUCKET = ITEMS.register("bucket_with_egg",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BOILED_EGG_BUCKET = ITEMS.register("boiled_egg_bucket",
            () -> new Item(new Item.Properties()));
    /**
    PASTA
     **/
    public static final DeferredItem<Item> PASTA = ITEMS.register("pasta",
            () -> new Item(new Item.Properties().food(ModFoodProps.RAW_EGG)));
    public static final DeferredItem<Item> TASTY_PASTA = ITEMS.register("tasty_pasta",
            () -> new BowlItem(new Item.Properties().food(ModFoodProps.NICE_BOWL)));
    public static final DeferredItem<Item> PASTA_BUCKET = ITEMS.register("pasta_bucket",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COOKED_PASTA_BUCKET = ITEMS.register("cooked_pasta_bucket",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CARROT_PASTA = ITEMS.register("carrot_pasta",
            () -> new BowlItem(new Item.Properties().food(ModFoodProps.NICE_BOWL_HASTE)));
    public static final DeferredItem<Item> RONALDO_PASTA = ITEMS.register("ronaldo_pasta",
            () -> new BowlItem(new Item.Properties().food(ModFoodProps.FABULOUS_BOWL_HASTE)));
        /**
        PLANT RELATED
         */
        public static final DeferredItem<Item> POTATO_PUREE = ITEMS.register("potato_puree",
                () -> new BowlItem(new Item.Properties().food(ModFoodProps.NICE_BOWL_REGEN)));

        public static final DeferredItem<Item> BLUE_BERRIES = ITEMS.register("blue_berry",
            () -> new ItemNameBlockItem(ModBlocks.BLUE_BERRY_BUSH.get(),
                    new Item.Properties().food(ModFoodProps.GENERIC_BERRY)));
    public static final DeferredItem<Item> GOOSE_BERRIES = ITEMS.register("goose_berry",
            () -> new ItemNameBlockItem(ModBlocks.GOOSE_BERRY_BUSH.get(),
                    new Item.Properties().food(ModFoodProps.GENERIC_BERRY)));
    public static final DeferredItem<Item> CRANBERRIES = ITEMS.register("cranberries",
            () -> new ItemNameBlockItem(ModBlocks.CRANBERRY_BUSH.get(),
                    new Item.Properties().food(ModFoodProps.GENERIC_BERRY)));
    public static final DeferredItem<Item> EXOTIC_SEASONING1 = ITEMS.register("exotic_seasoning1",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> EXOTIC_SEASONING2 = ITEMS.register("exotic_seasoning2",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> EXOTIC_SEASONING3 = ITEMS.register("exotic_seasoning3",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BLUEBERRY_PIE = ITEMS.register("blueberry_pie",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BLUEBERRY_PIE_SLICE = ITEMS.register("blueberry_pie_slice",
            () -> new Item(new Item.Properties().food(ModFoodProps.PIE)));
    public static final DeferredItem<Item> GOOSEBERRY_PIE = ITEMS.register("gooseberry_pie",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOOSEBERRY_PIE_SLICE = ITEMS.register("gooseberry_pie_slice",
            () -> new Item(new Item.Properties().food(ModFoodProps.PIE)));
    public static final DeferredItem<Item> CRANBERRY_PIE = ITEMS.register("cranberry_pie",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRANBERRY_PIE_SLICE = ITEMS.register("cranberry_pie_slice",
            () -> new Item(new Item.Properties().food(ModFoodProps.PIE)));
    /**
     * MEAT
     */
    public static final DeferredItem<Item> GROUND_PORK = ITEMS.register("ground_pork",
            () -> new Item(new Item.Properties().food(ModFoodProps.RAW_MEAT)));
    public static final DeferredItem<Item> PORK_PATTY = ITEMS.register("pork_patty",
            () -> new Item(new Item.Properties().food(ModFoodProps.RAW_MEAT)));
    public static final DeferredItem<Item> COOKED_PORK_PATTY = ITEMS.register("cooked_pork_patty",
            () -> new Item(new Item.Properties().food(ModFoodProps.PATTY)));
    public static final DeferredItem<Item> PORKCHOP_SLICE = ITEMS.register("porkchop_slice",
            () -> new Item(new Item.Properties().food(ModFoodProps.RAW_MEAT)));
    public static final DeferredItem<Item> GROUND_BEEF = ITEMS.register("ground_beef",
            () -> new Item(new Item.Properties().food(ModFoodProps.RAW_MEAT)));
    public static final DeferredItem<Item> BEEF_PATTY = ITEMS.register("beef_patty",
            () -> new Item(new Item.Properties().food(ModFoodProps.RAW_MEAT)));
    public static final DeferredItem<Item> COOKED_BEEF_PATTY = ITEMS.register("cooked_beef_patty",
            () -> new Item(new Item.Properties().food(ModFoodProps.PATTY)));

    public static final DeferredItem<Item> UNFINISHED_STUFFED_PEPPER = ITEMS.register("unfinished_stuffed_pepper",
            () -> new Item(new Item.Properties().food(ModFoodProps.RAW_MEAT)));
    public static final DeferredItem<Item> STUFFED_PEPPER = ITEMS.register("stuffed_pepper",
            () -> new Item(new Item.Properties().food(ModFoodProps.STUFFED_PEPPER)));
    public static final DeferredItem<Item> BEEF_BURGER = ITEMS.register("beef_burger",
            () -> new Item(new Item.Properties().food(ModFoodProps.BURGER)));
    public static final DeferredItem<Item> CHICKEN_LEGS = ITEMS.register("chicken_legs",
            () -> new BowlItem(new Item.Properties().food(ModFoodProps.AWESOME_BOWL)));
    public static final DeferredItem<Item> BEEF_W_POTATOES = ITEMS.register("beef_with_mashed_potatoes",
            () -> new BowlItem(new Item.Properties().food(ModFoodProps.AWESOME_BOWL_REGEN)));
    public static final DeferredItem<Item> PIQUANT_ELITE_FISH_DISH = ITEMS.register("piquant_elite_fish_dish",
            () -> new BowlItem(new Item.Properties().food(ModFoodProps.FABULOUS_BOWL_REGEN)));
    public static final DeferredItem<Item> NICE_BREAKFAST = ITEMS.register("nice_breakfast",
            () -> new Item(new Item.Properties().food(ModFoodProps.AWESOME_BOWL_HASTE)));
    /**
     *  DAIRY
     */
    public static final DeferredItem<Item> CHEESE = ITEMS.register("cheese",
            () -> new Item(new Item.Properties().food(ModFoodProps.CHEESE)));
    /**
     * DRINKS
     */


    public static final DeferredItem<Item> BLUE_BERRY_JUICE = ITEMS.register("blue_berry_juice",
            () -> new DrinkItem(new Item.Properties()
                    .food(ModFoodProps.JUICE).stacksTo(16)));

    public static final DeferredItem<Item> SUPER_BLUE_BERRY_JUICE = ITEMS.register("blue_berry_juice_regeneration",
            () -> new DrinkItem(new Item.Properties()
                    .food(ModFoodProps.BLUE_BERRY_JUICE_REGEN).stacksTo(16)));



    public static final DeferredItem<Item> GOOSE_BERRY_JUICE = ITEMS.register("goose_berry_juice",
            () -> new DrinkItem(new Item.Properties()
                    .food(ModFoodProps.JUICE).stacksTo(16)));

    public static final DeferredItem<Item> SUPER_GOOSE_BERRY_JUICE = ITEMS.register("goose_berry_juice_strength",
            () -> new DrinkItem(new Item.Properties()
                    .food(ModFoodProps.GOOSE_BERRY_JUICE_STRENGTH).stacksTo(16)));



    public static final DeferredItem<Item> CRANBERRY_JUICE = ITEMS.register("cranberry_juice",
            () -> new DrinkItem(new Item.Properties()
                    .food(ModFoodProps.JUICE).stacksTo(16)));

    public static final DeferredItem<Item> SUPER_CRANBERRY_JUICE = ITEMS.register("cranberry_juice_speed",
            () -> new DrinkItem(new Item.Properties()
                    .food(ModFoodProps.CRANBERRY_JUICE_SPEED).stacksTo(16)));

    public static final DeferredItem<Item> MILK_BOTTLE = ITEMS.register("bottle_of_milk",
            () -> new DrinkItem(new Item.Properties()
                    .food(ModFoodProps.MILK).stacksTo(16)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
