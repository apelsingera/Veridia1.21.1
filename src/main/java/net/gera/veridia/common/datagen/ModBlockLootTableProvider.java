package net.gera.veridia.common.datagen;

import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.bushes.CumaCumaBushBlock;
import net.gera.veridia.common.block.bushes.CumaCumaBushSprout;
import net.gera.veridia.common.block.crop.BellPepperCrop;
import net.gera.veridia.common.block.crop.HempCrop;
import net.gera.veridia.common.block.crop.GarlicCrop;
import net.gera.veridia.common.block.BloodyRunestoneBlock;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
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
        dropSelf(ModBlocks.BLACKSTONE_MAGMA.get());
        rawMolybdenumDrops(ModBlocks.RAW_MOLYBDENUM_BLOCK.get());
        dropSelf(ModBlocks.MOLYBDENUM_BLOCK.get());
        dropSelf(ModBlocks.MOLYBDENUM_BARS.get());
        dropOther(ModBlocks.INCINERATED_FARMLAND.get(), ModBlocks.INCINERATED_DIRT.get());
        hempCropDrops(ModBlocks.HEMP_CROP.get());
        dropSelf(ModBlocks.INCINERATED_DIRT.get());
        // Как у ванильных подозрительных блоков: сам блок ничего не роняет, добыча - в таблице кисти
        add(ModBlocks.SUSPICIOUS_INCINERATED_DIRT.get(), noDrop());
        singeStoneDrops(ModBlocks.SINGE_STONE.get());
        bloodyRunestoneDrops(ModBlocks.BLOODY_RUNESTONE.get());
        // Мрачное ядро - не здесь: выпадает только при разломе во время ярости (SpiritualRunestoneBlock.shatter)
        bloodyRunestoneDrops(ModBlocks.SPIRITUAL_RUNESTONE.get());
        dropSelf(ModBlocks.SINGE_STONE_STAIRS.get());
        add(ModBlocks.SINGE_STONE_SLAB.get(), block -> createSlabItemTable(ModBlocks.SINGE_STONE_SLAB.get()));
        dropSelf(ModBlocks.SINGE_STONE_WALL.get());
        stoneBrickFamilyDrops(ModBlocks.SINGE_COBBLESTONE, ModBlocks.SINGE_COBBLESTONE_STAIRS,
                ModBlocks.SINGE_COBBLESTONE_SLAB, ModBlocks.SINGE_COBBLESTONE_WALL);
        stoneBrickFamilyDrops(ModBlocks.SINGE_STONE_BRICKS, ModBlocks.SINGE_STONE_BRICK_STAIRS,
                ModBlocks.SINGE_STONE_BRICK_SLAB, ModBlocks.SINGE_STONE_BRICK_WALL);
        stoneBrickFamilyDrops(ModBlocks.MOSSY_SINGE_STONE_BRICKS, ModBlocks.MOSSY_SINGE_STONE_BRICK_STAIRS,
                ModBlocks.MOSSY_SINGE_STONE_BRICK_SLAB, ModBlocks.MOSSY_SINGE_STONE_BRICK_WALL);
        stoneBrickFamilyDrops(ModBlocks.CRACKED_SINGE_STONE_BRICKS, ModBlocks.CRACKED_SINGE_STONE_BRICK_STAIRS,
                ModBlocks.CRACKED_SINGE_STONE_BRICK_SLAB, ModBlocks.CRACKED_SINGE_STONE_BRICK_WALL);
        dropSelf(ModBlocks.CHISELED_SINGE_STONE.get());
        dropSelf(ModBlocks.SINGE_STONE_PILLAR.get());
        dropSelf(ModBlocks.WORN_SINGE_STONE_PILLAR.get());
        dropSelf(ModBlocks.SINGE_STONE_PILLAR_IONIC.get());
        dropSelf(ModBlocks.RED_MOSSY_SINGE_STONE_BRICKS.get());
        dropSelf(ModBlocks.SINGE_STONE_BRICK_TILES.get());
        dropSelf(ModBlocks.CRACKED_SINGE_STONE_BRICK_TILES.get());
        dropSelf(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILES.get());
        dropSelf(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILES.get());
        dropSelf(ModBlocks.SINGE_STONE_BRICK_TILE_STAIRS.get());
        add(ModBlocks.SINGE_STONE_BRICK_TILE_SLAB.get(), block -> createSlabItemTable(ModBlocks.SINGE_STONE_BRICK_TILE_SLAB.get()));
        dropSelf(ModBlocks.SINGE_STONE_BRICK_TILE_WALL.get());
        dropSelf(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_STAIRS.get());
        add(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_SLAB.get(), block -> createSlabItemTable(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_SLAB.get()));
        dropSelf(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_WALL.get());
        dropSelf(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_STAIRS.get());
        add(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_SLAB.get(), block -> createSlabItemTable(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_SLAB.get()));
        dropSelf(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_WALL.get());

        add(ModBlocks.SINGED_COAL_ORE.get(), block -> createOreDrop(block, Items.COAL));
        add(ModBlocks.SINGED_IRON_ORE.get(), block -> createOreDrop(block, Items.RAW_IRON));
        add(ModBlocks.SINGED_COPPER_ORE.get(), this::createCopperOreDrops);
        add(ModBlocks.SINGED_LAPIS_ORE.get(), this::createLapisOreDrops);
        //wood
        dropSelf(ModBlocks.SCULK_STAIRS.get());
        add(ModBlocks.SCULK_SLAB.get(), block -> createSlabItemTable(ModBlocks.SCULK_SLAB.get()));
        dropSelf(ModBlocks.SCULK_BUTTON.get());
        dropSelf(ModBlocks.SCULK_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.SCULK_FENCE.get());
        dropSelf(ModBlocks.SCULK_FENCE_GATE.get());
        dropSelf(ModBlocks.SCULK_WALL.get());
        dropSelf(ModBlocks.SCULK_TRAPDOOR.get());
        dropSelf(ModBlocks.SCULK_LOG.get());
        dropSelf(ModBlocks.STRIPPED_SCULK_LOG.get());
        dropSelf(ModBlocks.SCULK_WOOD.get());
        dropSelf(ModBlocks.STRIPPED_SCULK_WOOD.get());
        dropSelf(ModBlocks.SCULK_PLANKS.get());
        dropSelf(ModBlocks.CHARRED_PLANKS.get());
        dropSelf(ModBlocks.ASHEN_LOG.get());
        dropSelf(ModBlocks.ASHEN_WOOD.get());
        dropSelf(ModBlocks.STRIPPED_ASHEN_LOG.get());
        dropSelf(ModBlocks.STRIPPED_ASHEN_WOOD.get());
        ashenLeavesDrops(ModBlocks.ASHEN_LEAVES.get());
        dropSelf(ModBlocks.CHARRED_STAIRS.get());
        add(ModBlocks.CHARRED_SLAB.get(), block -> createSlabItemTable(ModBlocks.CHARRED_SLAB.get()));
        dropSelf(ModBlocks.CHARRED_FENCE.get());
        dropSelf(ModBlocks.CHARRED_FENCE_GATE.get());
        dropSelf(ModBlocks.CHARRED_BUTTON.get());
        dropSelf(ModBlocks.CHARRED_PRESSURE_PLATE.get());
        add(ModBlocks.CHARRED_DOOR.get(), block -> createDoorTable(ModBlocks.CHARRED_DOOR.get()));
        dropSelf(ModBlocks.CHARRED_TRAPDOOR.get());
        dropSelf(ModBlocks.ASHEN_PLANKS.get());
        dropSelf(ModBlocks.ASHEN_STAIRS.get());
        add(ModBlocks.ASHEN_SLAB.get(), block -> createSlabItemTable(ModBlocks.ASHEN_SLAB.get()));
        dropSelf(ModBlocks.ASHEN_FENCE.get());
        dropSelf(ModBlocks.ASHEN_FENCE_GATE.get());
        dropSelf(ModBlocks.ASHEN_BUTTON.get());
        dropSelf(ModBlocks.ASHEN_PRESSURE_PLATE.get());
        add(ModBlocks.ASHEN_DOOR.get(), block -> createDoorTable(ModBlocks.ASHEN_DOOR.get()));
        dropSelf(ModBlocks.ASHEN_TRAPDOOR.get());
        // Кладовые: выпадают сами с именем с наковальни (содержимое высыпается отдельно, как у бочки)
        add(ModBlocks.ASHEN_CABINET.get(), this::createNameableBlockEntityTable);
        add(ModBlocks.CHARRED_CABINET.get(), this::createNameableBlockEntityTable);
        dropSelf(ModBlocks.STRIPPED_CHARRED_LOG.get());
        dropSelf(ModBlocks.STRIPPED_CHARRED_WOOD.get());
        dropSelf(ModBlocks.SNOW_BRICKS.get());
        dropSelf(ModBlocks.PACKED_ICE_BRICKS.get());
        dropSelf(ModBlocks.OAK_SCRAP_WOOD.get());
        dropSelf(ModBlocks.SPRUCE_SCRAP_WOOD.get());
        dropSelf(ModBlocks.ASHEN_SCRAP_WOOD.get());
        dropSelf(ModBlocks.CHARRED_SCRAP_WOOD.get());
        this.add(ModBlocks.SCULK_LEAVES.get(), block ->
                createLeavesDrops(block, ModBlocks.SCULK_SPROUT.get(), NORMAL_LEAVES_SAPLING_CHANCES));
        dropSelf(ModBlocks.SCULK_SPROUT.get());
        add(ModBlocks.ASHEN_GRASS.get(), this::createGrassDrops);
        dropSelf(ModBlocks.SCULK_MUSHROOM.get());
        dropSelf(ModBlocks.COOKING_TABLE.get());
        dropSelf(ModBlocks.MYTHICAL_TABLE.get());
        dropSelf(ModBlocks.RUNIC_PEDESTAL.get());
        dropSelf(ModBlocks.EXTRACTOR.get());
        dropSelf(ModBlocks.STROP.get());
        dropSelf(ModBlocks.BUILDING_MATERIALS.get());
        dropSelf(ModBlocks.LUMBERJACK_LOG.get());
        dropSelf(ModBlocks.FOOD_STORAGE.get());
        add(ModBlocks.SCULK_DOOR.get(), block -> createDoorTable(ModBlocks.SCULK_DOOR.get()));

        LootItemCondition.Builder garlicDropCondition = LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.GARLIC_CROP.get())
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(GarlicCrop.AGE, 7));
        this.add(ModBlocks.GARLIC_CROP.get(), this.createCropDrops(ModBlocks.GARLIC_CROP.get(),
                ModItems.GARLIC.get(), ModItems.GARLIC_SEEDS.get(), garlicDropCondition));
        LootItemCondition.Builder bellPepperDropCondition = LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.BELL_PEPPER_CROP.get())
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(BellPepperCrop.AGE, 7));
        this.add(ModBlocks.BELL_PEPPER_CROP.get(), this.createCropDrops(ModBlocks.BELL_PEPPER_CROP.get(),
                ModItems.RED_BELL_PEPPER.get(), ModItems.BELL_PEPPER_SEEDS.get(), bellPepperDropCondition));
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
        this.add(ModBlocks.GOOSE_BERRY_BUSH.get(), block -> this.applyExplosionDecay(
                block, LootTable.lootTable().withPool(LootPool.lootPool().when(
                                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.GOOSE_BERRY_BUSH.get())
                                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SweetBerryBushBlock.AGE, 3))
                                ).add(LootItem.lootTableItem(ModItems.GOOSE_BERRIES.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
                ).withPool(LootPool.lootPool().when(
                                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.GOOSE_BERRY_BUSH.get())
                                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SweetBerryBushBlock.AGE, 2))
                                ).add(LootItem.lootTableItem(ModItems.GOOSE_BERRIES.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
                )));
        this.add(ModBlocks.CRANBERRY_BUSH.get(), block -> this.applyExplosionDecay(
                block, LootTable.lootTable().withPool(LootPool.lootPool().when(
                                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.CRANBERRY_BUSH.get())
                                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SweetBerryBushBlock.AGE, 3))
                                ).add(LootItem.lootTableItem(ModItems.CRANBERRIES.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 5.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
                ).withPool(LootPool.lootPool().when(
                                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.CRANBERRY_BUSH.get())
                                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SweetBerryBushBlock.AGE, 2))
                                ).add(LootItem.lootTableItem(ModItems.CRANBERRIES.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 3.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
                )));
        this.add(ModBlocks.CUMA_CUMA_BUSH.get(), block -> this.applyExplosionDecay(
                block, LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.CUMACUMA_BERRY.get())))));
        this.add(ModBlocks.CUMA_CUMA_BUSH_BLOCK.get(), block -> this.applyExplosionDecay(
                block, LootTable.lootTable().withPool(LootPool.lootPool().when(
                                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.CUMA_CUMA_BUSH_BLOCK.get())
                                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CumaCumaBushBlock.BERRIES, true))
                                ).add(LootItem.lootTableItem(ModItems.CUMACUMA_BERRY.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
                )));
        this.add(ModBlocks.CUMA_CUMA_BUSH_SPROUT.get(), block -> this.applyExplosionDecay(
                block, LootTable.lootTable().withPool(LootPool.lootPool().when(
                                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.CUMA_CUMA_BUSH_SPROUT.get())
                                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CumaCumaBushSprout.BERRIES, true))
                                ).add(LootItem.lootTableItem(ModItems.CUMACUMA_BERRY.get()))
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                                .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
                )));
        this.add(ModBlocks.PURPURITE_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(Blocks.STONE).when(LootItemRandomChanceCondition.randomChance(0.1F)),
                                LootItem.lootTableItem(ModItems.R_PURPURITE.get()).when(LootItemRandomChanceCondition.randomChance(0.9F))))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );
        this.add(ModBlocks.VERDANT_FLUORITE_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(Items.QUARTZ).when(LootItemRandomChanceCondition.randomChance(0.1F)),
                                LootItem.lootTableItem(ModItems.R_VERDANT_FLUORITE.get()).when(LootItemRandomChanceCondition.randomChance(0.9F))))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );
        this.add(ModBlocks.NETHER_BARIUM_ORE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(Items.NETHERRACK).when(LootItemRandomChanceCondition.randomChance(0.1F)),
                                LootItem.lootTableItem(ModItems.RAW_BARIUM.get()).when(LootItemRandomChanceCondition.randomChance(0.9F))))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );
        this.add(ModBlocks.PROFUNDUSTONE.get(), block ->
                LootTable.lootTable().withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(ModItems.PROFUNDUSTONE_CHUNK).when(LootItemRandomChanceCondition.randomChance(0.5F)),
                                LootItem.lootTableItem(Blocks.BLACKSTONE).when(LootItemRandomChanceCondition.randomChance(0.5F))))
                        .apply(ApplyExplosionDecay.explosionDecay())
                )
        );
    }
    // Конопля (всё растение считает нижняя половина): семя 1 всегда, у спелой ещё 1-2 семени. Верхняя половина ничего не роняет.
    private static final float HEMP_BONUS_SEEDS_MIN = 1.0F;
    private static final float HEMP_BONUS_SEEDS_MAX = 2.0F;

    private void hempCropDrops(Block block) {
        LootItemCondition.Builder lower = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                .setProperties(StatePropertiesPredicate.Builder.properties()
                        .hasProperty(HempCrop.HALF, DoubleBlockHalf.LOWER));
        LootItemCondition.Builder matureLower = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                .setProperties(StatePropertiesPredicate.Builder.properties()
                        .hasProperty(HempCrop.HALF, DoubleBlockHalf.LOWER)
                        .hasProperty(HempCrop.AGE, HempCrop.MAX_AGE));
        add(block, applyExplosionDecay(block, LootTable.lootTable()
                .withPool(LootPool.lootPool().when(lower).setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.HEMP_SEEDS.get())))
                .withPool(LootPool.lootPool().when(matureLower).setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.HEMP_SEEDS.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(HEMP_BONUS_SEEDS_MIN, HEMP_BONUS_SEEDS_MAX)))))));
    }

    // Сырая молибденовая руда: с шёлковым касанием сам блок, иначе один молибденовый скрап с шансом RAW_MOLYBDENUM_SCRAP_CHANCE
    private static final float RAW_MOLYBDENUM_SCRAP_CHANCE = 0.5F;

    private void rawMolybdenumDrops(Block block) {
        add(block, createSilkTouchDispatchTable(block,
                LootItem.lootTableItem(ModItems.MOLYBDENUM_SCRAP.get())
                        .when(LootItemRandomChanceCondition.randomChance(RAW_MOLYBDENUM_SCRAP_CHANCE))));
    }

    // Кровавый рунный камень: 5-6 опалённого камня; выпадает только с нижней половины (верхняя не даёт ничего)
    private static final float RUNESTONE_MIN_DROP = 5.0F;
    private static final float RUNESTONE_MAX_DROP = 6.0F;

    private void bloodyRunestoneDrops(Block block) {
        add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool())
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(ModBlocks.SINGE_STONE.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(RUNESTONE_MIN_DROP, RUNESTONE_MAX_DROP)))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(BloodyRunestoneBlock.HALF, DoubleBlockHalf.LOWER))))));
    }

    // Обожжённая листва, как ванильная, только без саженца (у деревьев Dynamic Trees свои семена): сама листва с
    // ножницами или шёлковым касанием, иначе палки с обычными шансами листвы
    // Шансы палок по уровню удачи - те же, что у ванильной листвы
    private static final float[] LEAVES_STICK_CHANCES = {0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};

    private void ashenLeavesDrops(Block block) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        add(block, createSilkTouchOrShearsDispatchTable(block, EmptyLootItem.emptyItem())
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                        .when(HAS_SHEARS.or(hasSilkTouch()).invert())
                        .add(applyExplosionDecay(block, LootItem.lootTableItem(Items.STICK)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))))
                                .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                        enchantments.getOrThrow(Enchantments.FORTUNE), LEAVES_STICK_CHANCES)))));
    }

    // Опалённый камень: опалённый булыжник (9 из 10) или туф (1 из 10); с шёлковым касанием - сам камень, как у
    // ванильного. Шансы - веса COBBLESTONE_WEIGHT и TUFF_WEIGHT.
    private static final int COBBLESTONE_WEIGHT = 9;
    private static final int TUFF_WEIGHT = 1;

    private void singeStoneDrops(Block block) {
        add(block, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                        .when(hasSilkTouch())
                        .add(LootItem.lootTableItem(block)))
                .withPool(applyExplosionCondition(block, LootPool.lootPool()).setRolls(ConstantValue.exactly(1.0F))
                        .when(doesNotHaveSilkTouch())
                        .add(LootItem.lootTableItem(ModBlocks.SINGE_COBBLESTONE.get()).setWeight(COBBLESTONE_WEIGHT))
                        .add(LootItem.lootTableItem(Items.TUFF).setWeight(TUFF_WEIGHT))));
    }

    // Кирпичный блок, ступени и стена выпадают сами, плита - по ванильной таблице плит (две при двойной плите)
    private void stoneBrickFamilyDrops(DeferredBlock<Block> base, DeferredBlock<StairBlock> stairs,
                                       DeferredBlock<SlabBlock> slab, DeferredBlock<WallBlock> wall) {
        dropSelf(base.get());
        dropSelf(stairs.get());
        add(slab.get(), block -> createSlabItemTable(slab.get()));
        dropSelf(wall.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}