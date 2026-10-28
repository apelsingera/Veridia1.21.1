package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.Veridia;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Полная копия LevelZombieItems, но для Кадавра (Husk) - см. LevelHuskConfig/LevelHuskHandler. */
public class LevelHuskItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Veridia.MOD_ID);

    public static final DeferredItem<LevelRangeHuskEggItem> WEAK_HUSK_EGG = ITEMS.registerItem(
            "husk_egg_weak", properties -> new LevelRangeHuskEggItem(1, 14, properties),
            new Item.Properties().rarity(Rarity.COMMON));

    public static final DeferredItem<LevelRangeHuskEggItem> DANGEROUS_HUSK_EGG = ITEMS.registerItem(
            "husk_egg_dangerous", properties -> new LevelRangeHuskEggItem(15, 29, properties),
            new Item.Properties().rarity(Rarity.RARE));

    // Тир 4 (30-35) не спавнится естественно (см. LevelHuskConfig.LEVEL_SPAWN_WEIGHT) - отдельное
    // яйцо на КАЖДЫЙ уровень (не общий диапазон) - equipChance/minLevel==maxLevel даёт фиксированный уровень.
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_30 = ITEMS.registerItem(
            "husk_egg_30", properties -> new LevelRangeHuskEggItem(30, 30, properties),
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_31 = ITEMS.registerItem(
            "husk_egg_31", properties -> new LevelRangeHuskEggItem(31, 31, properties),
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_32 = ITEMS.registerItem(
            "husk_egg_32", properties -> new LevelRangeHuskEggItem(32, 32, properties),
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_33 = ITEMS.registerItem(
            "husk_egg_33", properties -> new LevelRangeHuskEggItem(33, 33, properties),
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_34 = ITEMS.registerItem(
            "husk_egg_34", properties -> new LevelRangeHuskEggItem(34, 34, properties),
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<LevelRangeHuskEggItem> HUSK_EGG_35 = ITEMS.registerItem(
            "husk_egg_35", properties -> new LevelRangeHuskEggItem(35, 35, properties),
            new Item.Properties().rarity(Rarity.EPIC));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
