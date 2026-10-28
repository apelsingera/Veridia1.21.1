package net.gera.veridia.common.aibuff.levelzombie;
import net.gera.veridia.Veridia;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
public class LevelZombieItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Veridia.MOD_ID);
    public static final DeferredItem<LevelRangeZombieEggItem> WEAK_ZOMBIE_EGG = ITEMS.registerItem(
            "zombie_egg_weak", properties -> new LevelRangeZombieEggItem(1, 14, properties),
            new Item.Properties().rarity(Rarity.COMMON));
    public static final DeferredItem<LevelRangeZombieEggItem> DANGEROUS_ZOMBIE_EGG = ITEMS.registerItem(
            "zombie_egg_dangerous", properties -> new LevelRangeZombieEggItem(15, 29, properties),
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_30 = ITEMS.registerItem(
            "zombie_egg_30", properties -> new LevelRangeZombieEggItem(30, 30, properties),
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_31 = ITEMS.registerItem(
            "zombie_egg_31", properties -> new LevelRangeZombieEggItem(31, 31, properties),
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_32 = ITEMS.registerItem(
            "zombie_egg_32", properties -> new LevelRangeZombieEggItem(32, 32, properties),
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_33 = ITEMS.registerItem(
            "zombie_egg_33", properties -> new LevelRangeZombieEggItem(33, 33, properties),
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_34 = ITEMS.registerItem(
            "zombie_egg_34", properties -> new LevelRangeZombieEggItem(34, 34, properties),
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<LevelRangeZombieEggItem> ZOMBIE_EGG_35 = ITEMS.registerItem(
            "zombie_egg_35", properties -> new LevelRangeZombieEggItem(35, 35, properties),
            new Item.Properties().rarity(Rarity.EPIC));
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
