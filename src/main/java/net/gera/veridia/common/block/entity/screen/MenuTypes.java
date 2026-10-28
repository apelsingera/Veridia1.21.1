package net.gera.veridia.common.block.entity.screen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.villager.VillagerProfessionMenu;
import net.gera.veridia.common.entity.cinderroamer.CinderRoamerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Veridia.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CookingTableMenu>> COOKING_TABLE_MENU =
            registerMenuType("cooking_table_menu", CookingTableMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<MythicalTableMenu>> MYTHICAL_TABLE_MENU =
            registerMenuType("mythical_table_menu", MythicalTableMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<RunicPedestalMenu>> RUNIC_PEDESTAL_MENU =
            registerMenuType("runic_pedestal_menu", RunicPedestalMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<ExtractorMenu>> EXTRACTOR_MENU =
            registerMenuType("extractor_menu", ExtractorMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<BuildingMaterialsMenu>> BUILDING_MATERIALS_MENU_1 =
            registerMenuType("building_materials_menu_1", (id, inv, buf) ->
                    new BuildingMaterialsMenu(StorageLevelLayout.LEVEL_1, id, inv, buf));
    public static final DeferredHolder<MenuType<?>, MenuType<BuildingMaterialsMenu>> BUILDING_MATERIALS_MENU_2 =
            registerMenuType("building_materials_menu_2", (id, inv, buf) ->
                    new BuildingMaterialsMenu(StorageLevelLayout.LEVEL_2, id, inv, buf));
    public static final DeferredHolder<MenuType<?>, MenuType<BuildingMaterialsMenu>> BUILDING_MATERIALS_MENU_3 =
            registerMenuType("building_materials_menu_3", (id, inv, buf) ->
                    new BuildingMaterialsMenu(StorageLevelLayout.LEVEL_3, id, inv, buf));
    public static final DeferredHolder<MenuType<?>, MenuType<BuildingMaterialsMenu>> BUILDING_MATERIALS_MENU_4 =
            registerMenuType("building_materials_menu_4", (id, inv, buf) ->
                    new BuildingMaterialsMenu(StorageLevelLayout.LEVEL_4, id, inv, buf));

    public static final DeferredHolder<MenuType<?>, MenuType<FoodStorageMenu>> FOOD_STORAGE_MENU_1 =
            registerMenuType("food_storage_menu_1", (id, inv, buf) ->
                    new FoodStorageMenu(StorageLevelLayout.LEVEL_1, id, inv, buf));
    public static final DeferredHolder<MenuType<?>, MenuType<FoodStorageMenu>> FOOD_STORAGE_MENU_2 =
            registerMenuType("food_storage_menu_2", (id, inv, buf) ->
                    new FoodStorageMenu(StorageLevelLayout.LEVEL_2, id, inv, buf));
    public static final DeferredHolder<MenuType<?>, MenuType<FoodStorageMenu>> FOOD_STORAGE_MENU_3 =
            registerMenuType("food_storage_menu_3", (id, inv, buf) ->
                    new FoodStorageMenu(StorageLevelLayout.LEVEL_3, id, inv, buf));
    public static final DeferredHolder<MenuType<?>, MenuType<FoodStorageMenu>> FOOD_STORAGE_MENU_4 =
            registerMenuType("food_storage_menu_4", (id, inv, buf) ->
                    new FoodStorageMenu(StorageLevelLayout.LEVEL_4, id, inv, buf));

    @SuppressWarnings("unchecked")
    private static final DeferredHolder<MenuType<?>, MenuType<BuildingMaterialsMenu>>[] BUILDING_MATERIALS_MENUS_BY_LEVEL =
            new DeferredHolder[]{BUILDING_MATERIALS_MENU_1, BUILDING_MATERIALS_MENU_2, BUILDING_MATERIALS_MENU_3, BUILDING_MATERIALS_MENU_4};
    @SuppressWarnings("unchecked")
    private static final DeferredHolder<MenuType<?>, MenuType<FoodStorageMenu>>[] FOOD_STORAGE_MENUS_BY_LEVEL =
            new DeferredHolder[]{FOOD_STORAGE_MENU_1, FOOD_STORAGE_MENU_2, FOOD_STORAGE_MENU_3, FOOD_STORAGE_MENU_4};

    public static MenuType<BuildingMaterialsMenu> buildingMaterialsMenuType(int level) {
        return BUILDING_MATERIALS_MENUS_BY_LEVEL[Math.clamp(level, 1, 4) - 1].get();
    }

    public static MenuType<FoodStorageMenu> foodStorageMenuType(int level) {
        return FOOD_STORAGE_MENUS_BY_LEVEL[Math.clamp(level, 1, 4) - 1].get();
    }

    public static final DeferredHolder<MenuType<?>, MenuType<AncientDocumentMenu>> ANCIENT_DOCUMENT_MENU =
            registerMenuType("ancient_document_menu", (containerId, inv, buf) -> new AncientDocumentMenu(containerId, inv));

    public static final DeferredHolder<MenuType<?>, MenuType<VillagerProfessionMenu>> VILLAGER_PROFESSION_MENU =
            registerMenuType("villager_profession_menu", VillagerProfessionMenu::new);

    // Окно пеплохода: сёдла и рюкзаки (CinderRoamerEntity#openCustomInventoryScreen)
    public static final DeferredHolder<MenuType<?>, MenuType<CinderRoamerMenu>> CINDER_ROAMER_MENU =
            registerMenuType("cinder_roamer_menu", CinderRoamerMenu::new);

    private static <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name,
                                                                                                              IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}