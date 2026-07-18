package net.gera.veridia.datagen;

import net.gera.veridia.block.ModBlocks;
import net.gera.veridia.block.crop.GarlicCrop;
import net.gera.veridia.item.ModItems;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.PLANT_CLUMP_BLOCK.get());
        dropSelf(ModBlocks.TIN_BLOCK.get());
        //
        dropSelf(ModBlocks.TIN_STAIRS.get());
        add(ModBlocks.TIN_SLAB.get(), block -> createSlabItemTable(ModBlocks.TIN_SLAB.get()));
        dropSelf(ModBlocks.TIN_BUTTON.get());
        dropSelf(ModBlocks.TIN_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.TIN_FENCE.get());
        dropSelf(ModBlocks.TIN_FENCE_GATE.get());
        dropSelf(ModBlocks.TIN_WALL.get());
        dropSelf(ModBlocks.TIN_TRAPDOOR.get());
        //wood
        dropSelf(ModBlocks.SCULK_LOG.get());
        dropSelf(ModBlocks.STRIPPED_SCULK_LOG.get());
        dropSelf(ModBlocks.SCULK_WOOD.get());
        dropSelf(ModBlocks.STRIPPED_SCULK_WOOD.get());
        dropSelf(ModBlocks.SCULK_PLANKS.get());
        this.add(ModBlocks.SCULK_LEAVES.get(), block ->
                createLeavesDrops(block, ModBlocks.SCULK_SPROUT.get(), NORMAL_LEAVES_SAPLING_CHANCES));
        dropSelf(ModBlocks.SCULK_SPROUT.get());


        LootItemCondition.Builder lootItemConditionBuilder = LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.GARLIC_CROP.get())
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(GarlicCrop.AGE, 7));
        this.add(ModBlocks.GARLIC_CROP.get(), this.createCropDrops(ModBlocks.GARLIC_CROP.get(),
                ModItems.GARLIC.get(), ModItems.GARLIC_SEEDS.get(), lootItemConditionBuilder));
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        ///                         ///                         ///                         ///                         ///
        this.add(ModBlocks.BLUE_BERRY_BUSH.get(), block -> this.applyExplosionDecay(
                block, LootTable.lootTable().withPool(LootPool.lootPool().when(
                                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BLUE_BERRY_BUSH.get())
                                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SweetBerryBushBlock.AGE, 3))
                                ).add(LootItem.lootTableItem(ModItems.BLUE_BERRIES.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
                ).withPool(LootPool.lootPool().when(
                                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BLUE_BERRY_BUSH.get())
                                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SweetBerryBushBlock.AGE, 2))
                                ).add(LootItem.lootTableItem(ModItems.BLUE_BERRIES.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
                )));
        add(ModBlocks.TIN_DOOR.get(), block -> createDoorTable(ModBlocks.TIN_DOOR.get()));

        add(ModBlocks.COPPER_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_COPPER_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.2F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );

        add(ModBlocks.DEEPSLATE_COPPER_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_COPPER_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.1F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.4F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );

        add(ModBlocks.GRANITE_COPPER_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_COPPER_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.2F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );

        add(ModBlocks.ANDESITE_COPPER_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_COPPER_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.2F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );

        add(ModBlocks.DIORITE_COPPER_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_COPPER_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.2F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );

        add(ModBlocks.TUFF_COPPER_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_COPPER_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.2F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(ModItems.RAW_COPPER_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );

// --- TIN (ОЛОВО) ---
        add(ModBlocks.TIN_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_TIN_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.2F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );
        add(ModBlocks.DEEPSLATE_TIN_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_TIN_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.2F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );
        add(ModBlocks.GRANITE_TIN_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_TIN_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.2F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );

        add(ModBlocks.ANDESITE_TIN_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_TIN_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.2F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );

        add(ModBlocks.DIORITE_TIN_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_TIN_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.2F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );
        this.add(ModBlocks.TUFF_TIN_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.RAW_TIN_BIG.get()).when(LootItemRandomChanceCondition.randomChance(0.2F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_MEDIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(ModItems.RAW_TIN_SMALL.get())))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );
        //GEMS
        add(ModBlocks.AZOTIC_TOPAZ_END_ORE.get(),
                block -> createMultipleOreDrops(ModBlocks.AZOTIC_TOPAZ_END_ORE.get(), ModItems.AZOTIC_TOPAZ.get(), 1, 1));
        add(ModBlocks.DATOLITE_NETHER_ORE.get(),
                block -> createMultipleOreDrops(ModBlocks.DATOLITE_NETHER_ORE.get(), ModItems.DATOLITE.get(), 1, 1));

    }

    protected LootTable.Builder createMultipleOreDrops(Block pBlock, Item item, float minDrops, float maxDrops) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(pBlock,
                this.applyExplosionDecay(pBlock, LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops)))
                        .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
