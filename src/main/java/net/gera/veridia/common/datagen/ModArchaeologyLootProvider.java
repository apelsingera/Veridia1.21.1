package net.gera.veridia.common.datagen;

import net.gera.veridia.common.block.SuspiciousIncineratedDirtBlock;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Добыча кисти (археология): что находится в подозрительных блоках. Кисть берёт из таблицы один предмет (первый
 * из выпавших), поэтому в таблице один пул с одним броском, как у ванильных. Стопка, найденная кистью, выпадает
 * целиком (до 10-30 штук), так что количество предмета здесь - это сколько его выпадет.
 */
public class ModArchaeologyLootProvider implements LootTableSubProvider {
    /** Находка: предмет, вес (шанс = вес / сумма весов всех находок) и сколько штук выпадает. */
    private record Find(ItemLike item, int weight, int minCount, int maxCount) {
        Find(ItemLike item, int weight) {
            this(item, weight, 1, 1);
        }
    }

    // Подозрительная выжженная земля: остатки сгоревшего - уголь, кости, кремень, мелкий металл и редкие черепки
    // с предметами мода. Веса и количества - числа прямо здесь, меняй как нужно.
    private static final List<Find> SUSPICIOUS_INCINERATED_DIRT = List.of(
            new Find(Items.COAL, 20, 1, 3),
            new Find(Items.CHARCOAL, 18, 1, 3),
            new Find(Items.BONE, 16, 1, 2),
            new Find(Items.FLINT, 12, 1, 2),
            new Find(Items.IRON_NUGGET, 10, 2, 5),
            new Find(Items.GOLD_NUGGET, 8, 2, 4),
            new Find(Items.GUNPOWDER, 6, 1, 2),
            new Find(Items.BURN_POTTERY_SHERD, 3),
            new Find(Items.DANGER_POTTERY_SHERD, 3),
            new Find(Items.SKULL_POTTERY_SHERD, 3),
            new Find(ModItems.WITHER_BONE.get(), 3),
            new Find(ModItems.BLOODSTONE.get(), 2));

    public ModArchaeologyLootProvider(HolderLookup.Provider ignoredNone) {
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(SuspiciousIncineratedDirtBlock.LOOT_TABLE, singlePick(SUSPICIOUS_INCINERATED_DIRT));
    }

    private static LootTable.Builder singlePick(List<Find> finds) {
        LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F));
        for (Find find : finds) {
            pool.add(LootItem.lootTableItem(find.item()).setWeight(find.weight())
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(find.minCount(), find.maxCount()))));
        }
        return LootTable.lootTable().withPool(pool);
    }
}
