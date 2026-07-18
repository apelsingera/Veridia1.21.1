package net.gera.veridia.item;

import net.gera.veridia.Veridia;
import net.gera.veridia.block.ModBlocks;
import net.gera.veridia.item.custom.tools.CrudeKnifeItem;
import net.gera.veridia.item.custom.Fuel;
import net.gera.veridia.item.custom.ModFoodProps;
import net.gera.veridia.tier.ModTiers;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Veridia.MOD_ID);

    public static final DeferredItem<Item> PLANT_STRING = ITEMS.register("plant_string",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLANT_CLUMP = ITEMS.register("plant_clump",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRUDE_KNIFE = ITEMS.register("crude_knife",
            CrudeKnifeItem::new);
    public static final DeferredItem<Item> DANDELIONS_ROOT = ITEMS.register("dandelions_root",
            () -> new Item(new Item.Properties().food(ModFoodProps.DANDELION_ROOTS)));
    /// METALS
    //COPPER
    public static final DeferredItem<Item> RAW_COPPER_SMALL = ITEMS.register("raw_copper_small",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_COPPER_MEDIUM = ITEMS.register("raw_copper_medium",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_COPPER_BIG = ITEMS.register("raw_copper_big",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IMPURE_COPPER_INGOT = ITEMS.register("impure_copper_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_INGOT = ITEMS.register("copper_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_ROD = ITEMS.register("copper_rod",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DOUBLED_COPPER_INGOT = ITEMS.register("doubled_copper_ingot",
            () -> new Item(new Item.Properties()));

    //tool
    public static final DeferredItem<SwordItem> COPPER_SWORD = ITEMS.register("copper_sword",
            () -> new SwordItem(ModTiers.COPPER, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModTiers.COPPER, 4, -2.2f))));

    public static final DeferredItem<PickaxeItem> COPPER_PICKAXE = ITEMS.register("copper_pickaxe",
            () -> new PickaxeItem(ModTiers.COPPER, new Item.Properties()

                    .attributes(PickaxeItem.createAttributes(ModTiers.COPPER, 1.0F, -2.4f))));
    public static final DeferredItem<AxeItem> COPPER_AXE = ITEMS.register("copper_axe",
            () -> new AxeItem(ModTiers.COPPER, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModTiers.COPPER, 9.0F, -3.2f))));

    public static final DeferredItem<ShovelItem> COPPER_SHOVEL = ITEMS.register("copper_shovel",
            () -> new ShovelItem(ModTiers.COPPER, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(ModTiers.COPPER, 1.5F, -2.4f))));

    public static final DeferredItem<HoeItem> COPPER_HOE = ITEMS.register("copper_hoe",
            () -> new HoeItem(ModTiers.COPPER, new Item.Properties()
                    .attributes(HoeItem.createAttributes(ModTiers.COPPER, 1.5F, -2.4f))));
    //armor
    public static final DeferredItem<ArmorItem> COPPER_HELMET = ITEMS.register("copper_helmet", () ->
            new ArmorItem(ModArmorMaterials.COPPER_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));
    public static final DeferredItem<ArmorItem> COPPER_CHESTPLATE = ITEMS.register("copper_chestplate", () ->
            new ArmorItem(ModArmorMaterials.COPPER_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(15))));
    public static final DeferredItem<ArmorItem> COPPER_LEGGINGS = ITEMS.register("copper_leggings", () ->
            new ArmorItem(ModArmorMaterials.COPPER_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(15))));
    public static final DeferredItem<ArmorItem> COPPER_BOOTS = ITEMS.register("copper_boots", () ->
            new ArmorItem(ModArmorMaterials.COPPER_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(15))));
    //horse
    public static final DeferredItem<Item> COPPER_HORSE_ARMOR = ITEMS.register("copper_horse_armor",
            () -> new AnimalArmorItem(ModArmorMaterials.COPPER_ARMOR_MATERIAL, AnimalArmorItem.BodyType.EQUESTRIAN,
                    false, new Item.Properties().stacksTo(1)));
    //TIN
    public static final DeferredItem<Item> RAW_TIN_SMALL = ITEMS.register("raw_tin_small",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_TIN_MEDIUM = ITEMS.register("raw_tin_medium",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_TIN_BIG = ITEMS.register("raw_tin_big",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IMPURE_TIN_INGOT = ITEMS.register("impure_tin_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.register("tin_ingot",
            () -> new Item(new Item.Properties()));
    //BRONZE
    public static final DeferredItem<Item> IMPURE_BRONZE_INGOT = ITEMS.register("impure_bronze_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_INGOT = ITEMS.register("bronze_ingot",
            () -> new Item(new Item.Properties()));
    //tools
    public static final DeferredItem<SwordItem> BRONZE_SWORD = ITEMS.register("bronze_sword",
            () -> new SwordItem(ModTiers.BRONZE, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModTiers.COPPER, 4.5F, -1.8f))));

    public static final DeferredItem<PickaxeItem> BRONZE_PICKAXE = ITEMS.register("bronze_pickaxe",
            () -> new PickaxeItem(ModTiers.BRONZE, new Item.Properties()

                    .attributes(PickaxeItem.createAttributes(ModTiers.COPPER, 1.0F, -2.4f))));
    public static final DeferredItem<AxeItem> BRONZE_AXE = ITEMS.register("bronze_axe",
            () -> new AxeItem(ModTiers.BRONZE, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModTiers.COPPER, 9.0F, -3.1f))));

    public static final DeferredItem<ShovelItem> BRONZE_SHOVEL = ITEMS.register("bronze_shovel",
            () -> new ShovelItem(ModTiers.BRONZE, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(ModTiers.COPPER, 1.5F, -2.4f))));

    public static final DeferredItem<HoeItem> BRONZE_HOE = ITEMS.register("bronze_hoe",
            () -> new HoeItem(ModTiers.BRONZE, new Item.Properties()
                    .attributes(HoeItem.createAttributes(ModTiers.COPPER, 1.5F, -2.4f))));
    //armor
    public static final DeferredItem<ArmorItem> BRONZE_HELMET = ITEMS.register("bronze_helmet", () ->
            new ArmorItem(ModArmorMaterials.BRONZE_ARMOR_MATERIAL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(16))));
    public static final DeferredItem<ArmorItem> BRONZE_CHESTPLATE = ITEMS.register("bronze_chestplate", () ->
            new ArmorItem(ModArmorMaterials.BRONZE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(16))));
    public static final DeferredItem<ArmorItem> BRONZE_LEGGINGS = ITEMS.register("bronze_leggings", () ->
            new ArmorItem(ModArmorMaterials.BRONZE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(16))));
    public static final DeferredItem<ArmorItem> BRONZE_BOOTS = ITEMS.register("bronze_boots", () ->
            new ArmorItem(ModArmorMaterials.BRONZE_ARMOR_MATERIAL, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(16))));
    /// WROUGHT IRON
    public static final DeferredItem<Item> IMPURE_WROUGHT_IRON_INGOT = ITEMS.register("impure_wrought_iron_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WROUGHT_IRON_INGOT = ITEMS.register("wrought_iron_ingot",
            () -> new Item(new Item.Properties()));
    /// GEMSTONES
    public static final DeferredItem<Item> AZOTIC_TOPAZ = ITEMS.register("azotic_topaz",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DATOLITE = ITEMS.register("datolite",
            () -> new Item(new Item.Properties()));


    /// crops /// crops
        //garlic
    public static final DeferredItem<Item> GARLIC_SEEDS = ITEMS.register("garlic_seeds",
            () -> new ItemNameBlockItem(ModBlocks.GARLIC_CROP.get(), new Item.Properties()));
    public static final DeferredItem<Item> GARLIC = ITEMS.register("garlic",
            () -> new Item(new Item.Properties().food(ModFoodProps.GARLIC)));
        //Bushes
        //and
        //Fruits
    public static final DeferredItem<Item> BLUE_BERRIES = ITEMS.register("blue_berry",
            () -> new ItemNameBlockItem(ModBlocks.BLUE_BERRY_BUSH.get(),
                    new Item.Properties().food(ModFoodProps.BLUE_BERRY)));

/// misc /// ingredients ///
    public static final DeferredItem<Item> BALLS = ITEMS.register("balls",
        () -> new Fuel(new Item.Properties(), 999999999));















    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
