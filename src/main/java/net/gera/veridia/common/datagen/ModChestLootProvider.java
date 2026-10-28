package net.gera.veridia.common.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

import static net.gera.veridia.common.util.Ids.item;

public class ModChestLootProvider implements LootTableSubProvider {

    public static final ResourceKey<LootTable> WARRIOR_HOUSE = key("warrior_house");
    public static final ResourceKey<LootTable> ASH_RUINS = key("ash_ruins");
    public static final ResourceKey<LootTable> CITIZEN = key("citizen");
    public static final ResourceKey<LootTable> FACTORY1 = key("factory1");
    // Бедная версия сундука руин: меньше предметов, нет редкого и ценного, только 7% на семена конопли
    public static final ResourceKey<LootTable> ASH_RUINS_POOR = key("ash_ruins_poor");
    public static final ResourceKey<LootTable> COFFIN = key("coffin");

    // Гроб Graveyard: 54 слота, как у двойного сундука, поэтому бросков заметно больше, чем у обычных таблиц - лут
    // раскидывается по слотам случайно, и при малых бросках гроб выглядел бы почти пустым. Три пула: останки,
    // погребальные вещи и редкое. У редкого пула броски 0-2, то есть редкого может не быть вовсе (шанс пустого броска -
    // доля EMPTY от суммы весов).
    private static final float COFFIN_REMAINS_ROLLS_MIN = 4.0F;
    private static final float COFFIN_REMAINS_ROLLS_MAX = 7.0F;
    private static final float COFFIN_GOODS_ROLLS_MIN = 4.0F;
    private static final float COFFIN_GOODS_ROLLS_MAX = 8.0F;
    private static final float COFFIN_RARE_ROLLS_MIN = 0.0F;
    private static final float COFFIN_RARE_ROLLS_MAX = 2.0F;

    // Руины пепельных пустошей. Обычный пул: сколько предметов (бросков) в сундуке. Редкий пул: бросков 0-1,
    // то есть редкая добавка есть не в каждом сундуке (шанс = доля EMPTY_WEIGHT от суммы весов).
    private static final float RUINS_COMMON_ROLLS_MIN = 4.0F;
    private static final float RUINS_COMMON_ROLLS_MAX = 7.0F;
    private static final float RUINS_RARE_ROLLS_MIN = 0.0F;
    private static final float RUINS_RARE_ROLLS_MAX = 1.0F;

    // Семена огнеупорной конопли в сундуках руин: шанс (0..1), что в сундуке они есть, и сколько штук. Отдельный пул,
    // поэтому шанс не зависит от остального лута.
    private static final float RUINS_SEED_CHANCE = 0.10F;
    private static final float RUINS_POOR_SEED_CHANCE = 0.07F;
    private static final float RUINS_SEED_COUNT_MIN = 1.0F;
    private static final float RUINS_SEED_COUNT_MAX = 3.0F;
    // Бедный сундук руин: сколько предметов (бросков)
    private static final float RUINS_POOR_ROLLS_MIN = 2.0F;
    private static final float RUINS_POOR_ROLLS_MAX = 4.0F;
    // Сундук жителя: сколько предметов (бросков). Игра ещё и дробит стопки по пустым слотам, так что бросков нужно
    // заметно больше, чтобы сундук не выглядел пустым
    private static final float CITIZEN_ROLLS_MIN = 6.0F;
    private static final float CITIZEN_ROLLS_MAX = 10.0F;
    private static final float FACTORY_ROLLS_MIN = 4.0F;
    private static final float FACTORY_ROLLS_MAX = 6.0F;

    private static final float FOOD_ROLLS_MIN = 2.0F;
    private static final float FOOD_ROLLS_MAX = 4.0F;
    private static final float EMERALD_ROLLS_MIN = 0.0F;
    private static final float EMERALD_ROLLS_MAX = 1.0F;
    private static final float EMERALD_COUNT_MIN = 1.0F;
    private static final float EMERALD_COUNT_MAX = 5.0F;
    private static final float ARMOR_ROLLS = 1.0F;
    private static final int ARMOR_EMPTY_WEIGHT = 8;
    private static final float ARMOR_DAMAGE_MIN = 0.9F;
    private static final float ARMOR_DAMAGE_MAX = 1F;

    public ModChestLootProvider(HolderLookup.Provider ignoredNone) {
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(WARRIOR_HOUSE, warriorHouse());
        output.accept(ASH_RUINS, ashRuins());
        output.accept(CITIZEN, citizen());
        output.accept(FACTORY1, factory1());
        output.accept(ASH_RUINS_POOR, ashRuinsPoor());
        output.accept(COFFIN, coffin());
    }

    // Гроб: останки (кости, гниль, нитки), погребальные вещи (самородки, слитки, свечи) и редкая находка.
    // Веса и количества - числа прямо здесь, меняй как нужно.
    private LootTable.Builder coffin() {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(COFFIN_REMAINS_ROLLS_MIN, COFFIN_REMAINS_ROLLS_MAX))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(18)
                                .apply(count(4, 8)))
                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(16)
                                .apply(count(2, 6)))
                        .add(LootItem.lootTableItem(Items.BONE_MEAL).setWeight(10)
                                .apply(count(2, 6)))
                        .add(LootItem.lootTableItem(Items.STRING).setWeight(8)
                                .apply(count(3, 6)))
                        .add(LootItem.lootTableItem(Items.LEATHER).setWeight(6)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.SPIDER_EYE).setWeight(4)
                                .apply(count(1, 2))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(COFFIN_GOODS_ROLLS_MIN, COFFIN_GOODS_ROLLS_MAX))
                        .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(14)
                                .apply(count(3, 8)))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(12)
                                .apply(count(3, 8)))
                        .add(LootItem.lootTableItem(Items.ARROW).setWeight(10)
                                .apply(count(3, 8)))
                        .add(LootItem.lootTableItem(Items.CANDLE).setWeight(8)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(7)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(5)
                                .apply(count(1, 2)))
                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(4)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(3)
                                .apply(count(1, 3))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(COFFIN_RARE_ROLLS_MIN, COFFIN_RARE_ROLLS_MAX))
                        .add(LootItem.lootTableItem(ModItems.WITHER_BONE.get()).setWeight(8)
                                .apply(count(1, 2)))
                        .add(LootItem.lootTableItem(ModItems.ANCIENT_DOCUMENT.get()).setWeight(5))
                        .add(LootItem.lootTableItem(ModItems.BLOODSTONE.get()).setWeight(4))
                        .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(3))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(2))
                        .add(EmptyLootItem.emptyItem().setWeight(14)));
    }

    // Руины из опалённого камня: ресурсы пепельного мира (уголь, железо, кость, опалённый камень) и редкие предметы мода.
    // Веса и количества - константы-числа прямо здесь, меняй как нужно.
    private LootTable.Builder ashRuins() {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(RUINS_COMMON_ROLLS_MIN, RUINS_COMMON_ROLLS_MAX))
                        .add(LootItem.lootTableItem(ModBlocks.SINGE_STONE.get()).setWeight(14)
                                .apply(count(3, 8)))
                        .add(LootItem.lootTableItem(Items.COAL).setWeight(17)
                                .apply(count(2, 6)))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(12)
                                .apply(count(2, 5)))
                        .add(LootItem.lootTableItem(Items.ARROW).setWeight(10)
                                .apply(count(3, 8)))
                        .add(LootItem.lootTableItem(Items.RAW_IRON).setWeight(9)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(7)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.QUARTZ).setWeight(8)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.MAGMA_CREAM).setWeight(6)
                                .apply(count(1, 2)))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(6)
                                .apply(count(2, 6)))
                        .add(LootItem.lootTableItem(ModItems.PLASMAFITE).setWeight(1)
                                .apply(count(2, 5))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(RUINS_RARE_ROLLS_MIN, RUINS_RARE_ROLLS_MAX))
                        .add(LootItem.lootTableItem(ModItems.WITHER_BONE.get()).setWeight(12)
                                .apply(count(1, 2)))
                        .add(LootItem.lootTableItem(ModItems.BLOODSTONE.get()).setWeight(6))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(5))
                        .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(3))
                        .add(EmptyLootItem.emptyItem().setWeight(5)))
                .withPool(hempSeeds(RUINS_SEED_CHANCE));
    }

    // Бедный сундук руин: только простые ресурсы в малом количестве, без железных слитков, кварца, магмового крема,
    // плазмафита и редких находок; семена конопли реже (RUINS_POOR_SEED_CHANCE)
    private LootTable.Builder ashRuinsPoor() {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(RUINS_POOR_ROLLS_MIN, RUINS_POOR_ROLLS_MAX))
                        .add(LootItem.lootTableItem(ModBlocks.SINGE_STONE.get()).setWeight(16)
                                .apply(count(2, 5)))
                        .add(LootItem.lootTableItem(Items.COAL).setWeight(16)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(14)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.ARROW).setWeight(10)
                                .apply(count(1, 4)))
                        .add(LootItem.lootTableItem(Items.RAW_IRON).setWeight(5)
                                .apply(count(1, 2)))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(4)
                                .apply(count(1, 3))))
                .withPool(hempSeeds(RUINS_POOR_SEED_CHANCE));
    }

    // Сундук жителя: доски, уголь, кости, солома (Farmer's Delight), арматура (TFMG), рыба мода
    private LootTable.Builder citizen() {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(CITIZEN_ROLLS_MIN, CITIZEN_ROLLS_MAX))
                        .add(LootItem.lootTableItem(ModBlocks.ASHEN_PLANKS.get()).setWeight(16)
                                .apply(count(5, 10)))
                        .add(LootItem.lootTableItem(Items.COAL).setWeight(10)
                                .apply(count(5, 13)))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(14)
                                .apply(count(3, 6)))
                        .add(LootItem.lootTableItem(item("farmersdelight:straw")).setWeight(5)
                                .apply(count(4, 8)))
                        .add(LootItem.lootTableItem(item("tfmg:rebar")).setWeight(6)
                                .apply(count(4, 5)))
                        .add(LootItem.lootTableItem(ModItems.DEAD_FISH).setWeight(5)
                                .apply(count(6, 11)))
                        .add(LootItem.lootTableItem(ModItems.GOLITAH_TIGERFISH).setWeight(5)
                                .apply(count(5, 7)))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(4)
                                .apply(count(4, 6))));
    }
    private LootTable.Builder factory1() {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(FACTORY_ROLLS_MIN, FACTORY_ROLLS_MAX))
                        .add(LootItem.lootTableItem(item("create:zinc_nugget")).setWeight(16)
                                .apply(count(7, 10)))
                        .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(10)
                                .apply(count(5, 10)))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(5)
                                .apply(count(4, 8)))
                        .add(LootItem.lootTableItem(item("tfmg:steel_nugget")).setWeight(7)
                                .apply(count(4, 8)))
                        .add(LootItem.lootTableItem(item("tfmg:steel_ingot")).setWeight(4)
                                .apply(count(4, 5)))
                        .add(LootItem.lootTableItem(item("tfmg:slag")).setWeight(14)
                                .apply(count(6, 11)))
                        .add(LootItem.lootTableItem(ModBlocks.ASHEN_LOG).setWeight(5)
                                .apply(count(4, 7)))
                        .add(LootItem.lootTableItem(item("steel_mechanism")).setWeight(2)
                                .apply(count(1, 1))));
    }

    // Пул семян конопли: с шансом chance один бросок, 1-3 семени
    private static LootPool.Builder hempSeeds(float chance) {
        return LootPool.lootPool()
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(ModItems.HEMP_SEEDS.get())
                        .apply(count(RUINS_SEED_COUNT_MIN, RUINS_SEED_COUNT_MAX)));
    }

    private LootTable.Builder warriorHouse() {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(FOOD_ROLLS_MIN, FOOD_ROLLS_MAX))
                        .add(LootItem.lootTableItem(Items.BREAD).setWeight(12)
                                .apply(count(1, 4)))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(11)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.BAKED_POTATO).setWeight(9)
                                .apply(count(1, 4)))
                        .add(LootItem.lootTableItem(Items.APPLE).setWeight(9)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.COOKED_BEEF).setWeight(6)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.COOKED_CHICKEN).setWeight(6)
                                .apply(count(1, 3)))
                        .add(LootItem.lootTableItem(Items.COOKED_PORKCHOP).setWeight(5)
                                .apply(count(1, 2)))
                        .add(LootItem.lootTableItem(Items.PUMPKIN_PIE).setWeight(3))
                        .add(EmptyLootItem.emptyItem().setWeight(0)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(EMERALD_ROLLS_MIN, EMERALD_ROLLS_MAX))
                        .add(LootItem.lootTableItem(Items.EMERALD)
                                .apply(count(EMERALD_COUNT_MIN, EMERALD_COUNT_MAX))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(ARMOR_ROLLS))
                        .add(LootItem.lootTableItem(Items.LEATHER_HELMET).setWeight(10)
                                .apply(worn()))
                        .add(LootItem.lootTableItem(Items.LEATHER_BOOTS).setWeight(10)
                                .apply(worn()))
                        .add(LootItem.lootTableItem(Items.LEATHER_CHESTPLATE).setWeight(8)
                                .apply(worn()))
                        .add(LootItem.lootTableItem(Items.LEATHER_LEGGINGS).setWeight(8)
                                .apply(worn()))
                        .add(LootItem.lootTableItem(Items.CHAINMAIL_HELMET).setWeight(6)
                                .apply(worn()))
                        .add(LootItem.lootTableItem(Items.CHAINMAIL_CHESTPLATE).setWeight(4)
                                .apply(worn()))
                        .add(LootItem.lootTableItem(Items.IRON_HELMET).setWeight(3)
                                .apply(worn()))
                        .add(LootItem.lootTableItem(Items.IRON_CHESTPLATE).setWeight(2)
                                .apply(worn()))
                        .add(EmptyLootItem.emptyItem().setWeight(ARMOR_EMPTY_WEIGHT)));
    }

    private static LootItemConditionalFunction.Builder<?> count(float min, float max) {
        return SetItemCountFunction.setCount(UniformGenerator.between(min, max));
    }

    private static LootItemConditionalFunction.Builder<?> worn() {
        return SetItemDamageFunction.setDamage(UniformGenerator.between(ARMOR_DAMAGE_MIN, ARMOR_DAMAGE_MAX));
    }

    private static ResourceKey<LootTable> key(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "chests/" + name));
    }
}