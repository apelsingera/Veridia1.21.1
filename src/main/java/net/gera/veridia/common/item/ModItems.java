package net.gera.veridia.common.item;

import net.gera.veridia.Veridia;
import net.minecraft.resources.ResourceLocation;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.aibuff.levelzombie.LevelHuskItems;
import net.gera.veridia.common.aibuff.levelzombie.LevelRangeHuskEggItem;
import net.gera.veridia.common.aibuff.levelzombie.LevelRangeZombieEggItem;
import net.gera.veridia.common.aibuff.levelzombie.LevelZombieItems;
import net.gera.veridia.common.compat.curios.GemSlotUnlockItem;
import net.gera.veridia.common.entity.ModEntities;
import net.gera.veridia.common.item.custom.AncientDocumentItem;
import net.gera.veridia.common.item.custom.CreativeBoneMealItem;
import net.gera.veridia.common.item.custom.CreativeNailerWandItem;
import net.gera.veridia.common.item.custom.FiringContractItem;
import net.gera.veridia.common.item.custom.EternalPassItem;
import net.gera.veridia.common.item.custom.HealthPowderItem;
import net.gera.veridia.common.item.custom.JobApplicationItem;
import net.gera.veridia.common.item.custom.LimitedPassItem;
import net.gera.veridia.common.item.custom.ModArmorItem;
import net.gera.veridia.common.item.custom.ProfundusStoneItem;
import net.gera.veridia.common.item.custom.gems.lvl1.*;
import net.gera.veridia.common.item.custom.heavy_projectile.HeavyProjectile;
import net.gera.veridia.common.item.custom.tools.CrudeKnifeItem;
import net.gera.veridia.common.item.custom.tools.CrudeHammerItem;
import net.gera.veridia.common.item.custom.tools.ElectricSwordItem;
import net.gera.veridia.common.item.custom.tools.SharpKnifeItem;
import net.gera.veridia.common.item.custom.tools.SmeltingPickaxeItem;
import net.gera.veridia.common.item.custom.tools.VeriditePickaxeItem;
import net.gera.veridia.common.mana.ManaAwakeningStoneItem;
import net.gera.veridia.common.mana.ManaCapacityFoodItem;
import net.gera.veridia.common.mana.ManaRestoreFoodItem;
import net.gera.veridia.common.mana.ModManaItems;
import net.gera.veridia.common.mana.TierToolItem;
import net.gera.veridia.common.mana.veridite.BlindingPrismItem;
import net.gera.veridia.common.sound.ModSounds;
import net.gera.veridia.common.util.ModTiers;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import net.gera.veridia.common.util.ModTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Tool;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Veridia.MOD_ID);

    public static final DeferredItem<Item> PLANT_STRING = ITEMS.register("plant_string",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NETHER_STRING = ITEMS.register("nether_string",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHAMOTTE_BRICK = ITEMS.register("chamotte_brick",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> AQUATIC_STRING = ITEMS.register("aquatic_string",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FIBER_NET = ITEMS.register("fiber_net",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TANNED_LEATHER = ITEMS.register("tanned_leather",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STRONG_CLOTH = ITEMS.register("strong_cloth",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STRONG_HANDLE = ITEMS.register("strong_handle",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALCHEMIST_UPGRADE = ITEMS.register("alchemist_upgrade",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WARRIOR_UPGRADE = ITEMS.register("warrior_upgrade",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WORKER_UPGRADE = ITEMS.register("worker_upgrade",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLANT_CLUMP = ITEMS.register("plant_clump",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WITHER_BONE = ITEMS.register("wither_bone",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRAIN = ITEMS.register("brain",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INFESTED_BRAIN = ITEMS.register("infested_brain",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRUDE_KNIFE = ITEMS.register("crude_knife",
            CrudeKnifeItem::new);
    public static final DeferredItem<Item> SS_KNIFE = ITEMS.register("super_sharp_knife",
            SharpKnifeItem::new);
    public static final DeferredItem<Item> CRUDE_HAMMER = ITEMS.register("crude_hammer",
            CrudeHammerItem::new);
    public static final DeferredItem<Item> DANDELIONS_ROOT = ITEMS.register("dandelion_root",
            () -> new Item(new Item.Properties().food(ModFoodProps.DANDELION_ROOTS)));
    public static final DeferredItem<Item> NAUTILUS_GLAND = ITEMS.register("nautilus_gland",
            () -> new Item(new Item.Properties().food(ModFoodProps.CHEESE)));
    public static final DeferredItem<Item> PRISMARINE_C_POWDER = ITEMS.register("prismarine_crystal_powder",
            () -> new Item(new Item.Properties()));
//
    public static final DeferredItem<Item> RAW_BARIUM = ITEMS.register("raw_barium",
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BARIUM_INGOT = ITEMS.register("barium_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BARIUM_POWDER= ITEMS.register("barium_powder",
            () -> new Item(new Item.Properties()));
    // Молибден пепельных земель: скрап падает из блока сырой руды (ModBlocks.RAW_MOLYBDENUM_BLOCK), слиток делается из скрапа
    public static final DeferredItem<Item> MOLYBDENUM_SCRAP = ITEMS.register("molybdenum_scrap",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MOLYBDENUM_INGOT = ITEMS.register("molybdenum_ingot",
            () -> new Item(new Item.Properties()));
    // Молибденовые самородок (9 = слиток) и лист (прессом Create из слитка)
    public static final DeferredItem<Item> MOLYBDENUM_NUGGET = ITEMS.register("molybdenum_nugget",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MOLYBDENUM_SHEET = ITEMS.register("molybdenum_sheet",
            () -> new Item(new Item.Properties()));
    // Мрачное ядро: выпадает из духовного рунного камня, когда его разбивают (SpiritualRunestoneBlock)
    public static final DeferredItem<Item> DISMAL_CORE = ITEMS.register("dismal_core",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    // Ножовка: спиливает шипы со спины прирученного пеплохода (клик - один шип), см. CinderRoamerEntity. Как инструмент
    // быстро пилит цепи, решётки, заборы, калитки, громоотвод, таблички и флаги (тег mineable/hacksaw) с добычей
    public static final float HACKSAW_MINING_SPEED = 8.0F;
    public static final DeferredItem<Item> MOLYBDENUM_HACKSAW = ITEMS.register("molybdenum_hacksaw",
            () -> new Item(new Item.Properties().durability(128).component(DataComponents.TOOL, new Tool(
                    List.of(Tool.Rule.minesAndDrops(ModTags.Blocks.MINEABLE_WITH_HACKSAW, HACKSAW_MINING_SPEED)),
                    1.0F, 1))));
    public static final DeferredItem<Item> QUARTZ_POWDER = ITEMS.register("quartz_powder",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PURPURITE = ITEMS.register("purpurite",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> R_PURPURITE = ITEMS.register("raw_purpurite",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> AQUATIC_TOPAZ = ITEMS.register("aquatic_topaz",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> R_AQUATIC_TOPAZ = ITEMS.register("raw_aquatic_topaz",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> PLASMAFITE = ITEMS.register("plasmafite",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> SCORCHING_PLASMAFITE = ITEMS.register("scorching_plasmafite",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> VERIDITE = ITEMS.register("veridite",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> R_VERDANT_FLUORITE = ITEMS.register("raw_verdant_fluorite",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> VERDANT_FLUORITE = ITEMS.register("verdant_fluorite",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> D_VERDANT_FLUORITE = ITEMS.register("dangerous_verdant_fluorite",
            () -> new DangerousVerdantFluorite(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> PROFUNDUSTONE_CHUNK = ITEMS.register("profundustone_chunk",
            () -> new ProfundusStoneItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(8)));
    public static final DeferredItem<Item> BLOODSTONE = ITEMS.register("bloodstone",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> GARLIC_SEEDS = ITEMS.register("garlic_seeds",
            () -> new ItemNameBlockItem(ModBlocks.GARLIC_CROP.get(), new Item.Properties()));
    // Семена конопли: сажаются на выжженную пашню (сундуки руин)
    public static final DeferredItem<Item> HEMP_SEEDS = ITEMS.register("hemp_seeds",
            () -> new ItemNameBlockItem(ModBlocks.HEMP_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> TOMATO_CHERRY = ITEMS.register("tomato_cherry",
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
    public static final DeferredItem<Item> CUMACUMA_BERRY = ITEMS.register("cuma_cuma_berry",
            () -> new ItemNameBlockItem(ModBlocks.CUMA_CUMA_BUSH.get(),
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
    public static final DeferredItem<Item> HORSE_MEAT = ITEMS.register("horse_meat",
            () -> new Item(new Item.Properties().food(ModFoodProps.RAW_MEAT)));
    /**
     * FISH (улов пепельных биомов, см. loot_table/gameplay/fishing/ash)
     */
    public static final DeferredItem<Item> PIRANHA = ITEMS.register("piranha",
            () -> new Item(new Item.Properties().food(ModFoodProps.PIRANHA)));
    public static final DeferredItem<Item> GOLITAH_TIGERFISH = ITEMS.register("golitah_tigerfish",
            () -> new Item(new Item.Properties().food(ModFoodProps.GOLITAH_TIGERFISH)));
    // Несъедобная: просто мусор
    public static final DeferredItem<Item> DEAD_FISH = ITEMS.register("dead_fish",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> COOKED_HORSE_MEAT = ITEMS.register("cooked_horse_meat",
            () -> new Item(new Item.Properties().food(ModFoodProps.HORSE_MEAT)));
    public static final DeferredItem<Item> BESH = ITEMS.register("beshbarmak",
            () -> new BowlItem(new Item.Properties().food(ModFoodProps.AWESOME_BOWL_HASTE)));

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
    public static final DeferredItem<Item> TASTY_MEAT_MIX = ITEMS.register("tasty_meat_mix",
            () -> new Item(new Item.Properties().food(ModFoodProps.FABULOUS_BOWL)));
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
    public static final DeferredItem<Item> BUTTER = ITEMS.register("butter",
            () -> new Item(new Item.Properties().food(ModFoodProps.BUTTER)));
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

    public static final DeferredItem<Item> MILK_N_BUTTER = ITEMS.register("milk_with_butter",
            () -> new DrinkItem(new Item.Properties()
                    .food(ModFoodProps.NONE).stacksTo(16)));

    public static final DeferredItem<Item> LIMITED_PASS = ITEMS.register("limited_nether_pass",
            () -> new LimitedPassItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> ETERNAL_PASS = ITEMS.register("eternal_nether_pass",
            () -> new EternalPassItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> HEALTH_POWDER = ITEMS.register("health_powder",
            () -> new HealthPowderItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> JOB_APPLICATION = ITEMS.register("job_application",
            () -> new JobApplicationItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CREATIVE_BONE_MEAL = ITEMS.register("creative_bone_meal",
            () -> new CreativeBoneMealItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FIRING_CONTRACT = ITEMS.register("firing_contract",
            () -> new FiringContractItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CREATIVE_NAILER_WAND = ITEMS.register("creative_nailer_wand",
            () -> new CreativeNailerWandItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<KnockbackRingItem> KNOCKBACK_RING =
            ITEMS.registerItem("knockback_ring", KnockbackRingItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<AquaticAmuletItem> AQUATIC_AMULET =
            ITEMS.registerItem("aquatic_amulet", AquaticAmuletItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    public static final DeferredItem<Item> AQUATIC_AMULET_2 = ITEMS.register("aquatic_amulet_2",
            () -> new net.gera.veridia.common.item.custom.gems.lvl2.AquaticAmuletItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> AQUATIC_AMULET_3 = ITEMS.register("aquatic_amulet_3",
            () -> new net.gera.veridia.common.item.custom.gems.lvl3.AquaticAmuletItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> AQUATIC_AMULET_4 = ITEMS.register("aquatic_amulet_4",
            () -> new net.gera.veridia.common.item.custom.gems.lvl4.AquaticAmuletItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> AQUATIC_AMULET_5 = ITEMS.register("aquatic_amulet_5",
            () -> new net.gera.veridia.common.item.custom.gems.lvl5.AquaticAmuletItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<EyeOfSauronItem> EYE_OF_SAURON =
            ITEMS.registerItem("eye_of_sauron", EyeOfSauronItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    public static final DeferredItem<Item> EYE_OF_SAURON_2 = ITEMS.register("eye_of_sauron_2",
            () -> new net.gera.veridia.common.item.custom.gems.lvl2.EyeOfSauronItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> EYE_OF_SAURON_3 = ITEMS.register("eye_of_sauron_3",
            () -> new net.gera.veridia.common.item.custom.gems.lvl3.EyeOfSauronItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> EYE_OF_SAURON_4 = ITEMS.register("eye_of_sauron_4",
            () -> new net.gera.veridia.common.item.custom.gems.lvl4.EyeOfSauronItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> EYE_OF_SAURON_5 = ITEMS.register("eye_of_sauron_5",
            () -> new net.gera.veridia.common.item.custom.gems.lvl5.EyeOfSauronItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<SlowKillerItem> SLOW_KILLER =
            ITEMS.registerItem("slow_killer", SlowKillerItem::new,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    public static final DeferredItem<Item> KNOCKBACK_RING_2 = ITEMS.register("knockback_ring_2",
            () -> new net.gera.veridia.common.item.custom.gems.lvl2.KnockbackRingItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> KNOCKBACK_RING_3 = ITEMS.register("knockback_ring_3",
            () -> new net.gera.veridia.common.item.custom.gems.lvl3.KnockbackRingItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> KNOCKBACK_RING_4 = ITEMS.register("knockback_ring_4",
            () -> new net.gera.veridia.common.item.custom.gems.lvl4.KnockbackRingItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> KNOCKBACK_RING_5 = ITEMS.register("knockback_ring_5",
            () -> new net.gera.veridia.common.item.custom.gems.lvl5.KnockbackRingItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> SLOW_KILLER_2 = ITEMS.register("slow_killer_2",
            () -> new net.gera.veridia.common.item.custom.gems.lvl2.SlowKillerItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> SLOW_KILLER_3 = ITEMS.register("slow_killer_3",
            () -> new net.gera.veridia.common.item.custom.gems.lvl3.SlowKillerItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> SLOW_KILLER_4 = ITEMS.register("slow_killer_4",
            () -> new net.gera.veridia.common.item.custom.gems.lvl4.SlowKillerItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> SLOW_KILLER_5 = ITEMS.register("slow_killer_5",
            () -> new net.gera.veridia.common.item.custom.gems.lvl5.SlowKillerItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> NEURON_CLUMP = ITEMS.register("neuron_clump",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<AncientDocumentItem> ANCIENT_DOCUMENT =
            ITEMS.registerItem("ancient_document", AncientDocumentItem::new,
                    new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> HEAVY_PROJECTILE =
            ITEMS.register("heavy_projectile",
                    () -> new HeavyProjectile(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> ELECTRIC_SWORD = ITEMS.register("electric_sword",
            () -> new ElectricSwordItem(ModTiers.ELECTRIC_TOOL,new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModTiers.ELECTRIC_TOOL, 2F, -1.8F)).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> VERIDITE_PICKAXE = ITEMS.register("veridite_pickaxe",
            () -> new VeriditePickaxeItem(ModTiers.VERIDITE_TOOL, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(ModTiers.VERIDITE_TOOL, 1.0F, -2.8F)).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> SMELTING_PICKAXE = ITEMS.register("smelting_pickaxe",
            () -> new SmeltingPickaxeItem(Tiers.IRON, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(Tiers.IRON, 1.0F, -2.8F)).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> OMEN_REPRISE_DISK = ITEMS.register("omen_reprise_music_disc",
            () -> new Item(new Item.Properties().jukeboxPlayable(ModSounds.OMEN_REPRISE_KEY).stacksTo(1)));
    public static final DeferredItem<Item> SLOT_POCKET = ITEMS.register("slot_pocket",
            () -> new GemSlotUnlockItem(new Item.Properties().stacksTo(8)));
//
    public static final DeferredItem<Item> DOUBLE_COPPER_INGOT = ITEMS.register("double_copper_ingot",
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DOUBLE_IRON_INGOT = ITEMS.register("double_iron_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DOUBLE_GOLD_INGOT = ITEMS.register("double_gold_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DOUBLE_DIAMOND = ITEMS.register("double_diamond",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> QUAD_COPPER_INGOT = ITEMS.register("quadruple_copper_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> QUAD_GOLD_INGOT = ITEMS.register("quadruple_iron_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> QUAD_IRON_INGOT = ITEMS.register("quadruple_gold_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> QUAD_DIAMOND = ITEMS.register("quadruple_diamond",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<ArmorItem> VERIDITE_HELMET = ITEMS.register("veridite_helmet",
            () -> new ModArmorItem(ModArmorMaterials.VERIDITE_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(45))));
    public static final DeferredItem<ArmorItem> VERIDITE_CHESTPLATE = ITEMS.register("veridite_chestplate",
            () -> new ArmorItem(ModArmorMaterials.VERIDITE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(45))));
    public static final DeferredItem<ArmorItem> VERIDITE_LEGGINGS = ITEMS.register("veridite_leggings",
            () -> new ArmorItem(ModArmorMaterials.VERIDITE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(45))));
    public static final DeferredItem<ArmorItem> VERIDITE_BOOTS = ITEMS.register("veridite_boots",
            () -> new ArmorItem(ModArmorMaterials.VERIDITE_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(45))));
    public static final DeferredItem<BlindingPrismItem> BLINDING_PRISM = ITEMS.register("blinding_prism",
            () -> new BlindingPrismItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<InfernalStaffItem> INFERNAL_STAFF = ITEMS.register("infernal_staff",
            () -> new InfernalStaffItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> INFERNAL_STAFF_2 = ITEMS.register("infernal_staff_2",
            () -> new net.gera.veridia.common.item.custom.gems.lvl2.InfernalStaffItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> INFERNAL_STAFF_3 = ITEMS.register("infernal_staff_3",
            () -> new net.gera.veridia.common.item.custom.gems.lvl3.InfernalStaffItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> INFERNAL_STAFF_4 = ITEMS.register("infernal_staff_4",
            () -> new net.gera.veridia.common.item.custom.gems.lvl4.InfernalStaffItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> INFERNAL_STAFF_5 = ITEMS.register("infernal_staff_5",
            () -> new net.gera.veridia.common.item.custom.gems.lvl5.InfernalStaffItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<ArmorItem> NEWBIE_MANTLE_HELMET = ITEMS.register("newbie_mantle_helmet",
            () -> new ArmorItem(ModArmorMaterials.NEWBIE_MANTLE_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));
    public static final DeferredItem<ArmorItem> NEWBIE_MANTLE_CHESTPLATE = ITEMS.register("newbie_mantle_chestplate",
            () -> new ArmorItem(ModArmorMaterials.NEWBIE_MANTLE_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(15))));
    public static final DeferredItem<ArmorItem> NEWBIE_MANTLE_BOOTS = ITEMS.register("newbie_mantle_boots",
            () -> new ArmorItem(ModArmorMaterials.NEWBIE_MANTLE_MATERIAL, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(15))));
    public static final DeferredItem<ArmorItem> EXPERIENCED_MANTLE_HELMET = ITEMS.register("experienced_mantle_helmet",
            () -> new ArmorItem(ModArmorMaterials.EXPERIENCED_MANTLE_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));
    public static final DeferredItem<ArmorItem> EXPERIENCED_MANTLE_CHESTPLATE = ITEMS.register("experienced_mantle_chestplate",
            () -> new ArmorItem(ModArmorMaterials.EXPERIENCED_MANTLE_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(15))));
    public static final DeferredItem<ArmorItem> EXPERIENCED_MANTLE_BOOTS = ITEMS.register("experienced_mantle_boots",
            () -> new ArmorItem(ModArmorMaterials.EXPERIENCED_MANTLE_MATERIAL, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(15))));

    public static final List<DeferredItem<TierToolItem>> MANA_RUNES = ModManaItems.RUNES;
    public static final DeferredItem<ManaAwakeningStoneItem> MANA_AWAKENING_STONE = ModManaItems.MANA_AWAKENING_STONE;
    public static final DeferredItem<ManaRestoreFoodItem> SUSPICIOUS_SOUP = ModManaItems.SUSPICIOUS_SOUP;
    public static final DeferredItem<ManaCapacityFoodItem> NETHER_DELICACY = ModManaItems.NETHER_DELICACY;

    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_WEAK = LevelZombieItems.WEAK_ZOMBIE_EGG;
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_DANGEROUS = LevelZombieItems.DANGEROUS_ZOMBIE_EGG;
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_30 = LevelZombieItems.ZOMBIE_EGG_30;
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_31 = LevelZombieItems.ZOMBIE_EGG_31;
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_32 = LevelZombieItems.ZOMBIE_EGG_32;
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_33 = LevelZombieItems.ZOMBIE_EGG_33;
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_34 = LevelZombieItems.ZOMBIE_EGG_34;
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_35 = LevelZombieItems.ZOMBIE_EGG_35;

    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_WEAK = LevelHuskItems.WEAK_HUSK_EGG;
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_DANGEROUS = LevelHuskItems.DANGEROUS_HUSK_EGG;
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_30 = LevelHuskItems.HUSK_EGG_30;
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_31 = LevelHuskItems.HUSK_EGG_31;
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_32 = LevelHuskItems.HUSK_EGG_32;
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_33 = LevelHuskItems.HUSK_EGG_33;
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_34 = LevelHuskItems.HUSK_EGG_34;
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_35 = LevelHuskItems.HUSK_EGG_35;
    @SuppressWarnings("deprecation")
    public static final DeferredItem<SpawnEggItem> MUTANT_SPAWN_EGG = ITEMS.registerItem("mutant_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.MUTANT.get(), 0x5fae52, 0x1c2b12, properties),
            new Item.Properties());
    @SuppressWarnings("deprecation")
    public static final DeferredItem<SpawnEggItem> OMINOUS_WARRIOR_SPAWN_EGG = ITEMS.registerItem("ominous_warrior_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.OMINOUS_WARRIOR.get(), 0x7a7468, 0x4e5a2c, properties),
            new Item.Properties());
    @SuppressWarnings("deprecation")
    public static final DeferredItem<SpawnEggItem> BRUTAL_OMINOUS_WARRIOR_SPAWN_EGG = ITEMS.registerItem("brutal_ominous_warrior_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.BRUTAL_OMINOUS_WARRIOR.get(), 0x4f4a42, 0x5c1f1f, properties),
            new Item.Properties());
    @SuppressWarnings("deprecation")
    public static final DeferredItem<SpawnEggItem> CINDER_ROAMER_SPAWN_EGG = ITEMS.registerItem("cinder_roamer_spawn_egg",
            properties -> new SpawnEggItem(ModEntities.CINDER_ROAMER.get(), 0x1f2840, 0xd8d4bf, properties),
            new Item.Properties());

    // Семена раньше назывались nettle_seeds: старое имя в сохранённых мирах и инвентарях отдаёт новые семена
    private static final ResourceLocation LEGACY_NETTLE_SEEDS =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "nettle_seeds");

    public static void register(IEventBus eventBus) {
        ITEMS.addAlias(LEGACY_NETTLE_SEEDS, ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "hemp_seeds"));
        ITEMS.register(eventBus);
    }
}
