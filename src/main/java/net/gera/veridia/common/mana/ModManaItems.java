package net.gera.veridia.common.mana;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.item.ModFoodProps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public class ModManaItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Veridia.MOD_ID);

    public static final DeferredItem<ManaAwakeningStoneItem> MANA_AWAKENING_STONE = ITEMS.registerItem(
            "mana_awakening_stone", ManaAwakeningStoneItem::new,
            new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<ManaRestoreFoodItem> SUSPICIOUS_SOUP = ITEMS.registerItem(
            "suspicious_soup", properties -> new ManaRestoreFoodItem(15, properties),
            new Item.Properties().food(ModFoodProps.MANA_SNACK).rarity(Rarity.COMMON));

    public static final DeferredItem<ManaCapacityFoodItem> NETHER_DELICACY = ITEMS.registerItem(
            "nether_delicacy", properties -> new ManaCapacityFoodItem(20, 20 * 30, properties),
            new Item.Properties().food(ModFoodProps.MANA_SNACK).rarity(Rarity.COMMON));
    public static final List<DeferredItem<TierToolItem>> RUNES = registerRunes();

    private static List<DeferredItem<TierToolItem>> registerRunes() {
        List<DeferredItem<TierToolItem>> items = new ArrayList<>(ManaProgression.STEPS.size());
        for (int i = 0; i < ManaProgression.STEPS.size(); i++) {
            String registryName = ManaProgression.STEPS.get(i).registryName();
            int stepIndex = i;
            items.add(ITEMS.registerItem(registryName,
                    properties -> new TierToolItem(stepIndex, properties),
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
        }
        return items;
    }

    public static DeferredItem<TierToolItem> itemForStep(int stepIndex) {
        return RUNES.get(stepIndex);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
