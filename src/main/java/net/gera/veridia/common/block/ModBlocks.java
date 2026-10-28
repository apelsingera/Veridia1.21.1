package net.gera.veridia.common.block;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.bushes.BlueBerryBush;
import net.gera.veridia.common.block.bushes.CranBerryBush;
import net.gera.veridia.common.block.bushes.CumaCumaBush;
import net.gera.veridia.common.block.bushes.CumaCumaBushBlock;
import net.gera.veridia.common.block.bushes.CumaCumaBushSprout;
import net.gera.veridia.common.block.bushes.GooseBerryBush;
import net.gera.veridia.common.block.crop.BellPepperCrop;
import net.gera.veridia.common.block.crop.HempCrop;
import net.gera.veridia.common.block.crop.GarlicCrop;
import net.gera.veridia.common.block.entity.types.CookingTableBlock;
import net.gera.veridia.common.block.entity.types.ExtractorBlock;
import net.gera.veridia.common.block.entity.types.MythicalTableBlock;
import net.gera.veridia.common.block.entity.types.RunicPedestalBlock;
import net.gera.veridia.common.block.entity.types.RunicPedestalBlockItem;
import net.gera.veridia.common.block.ore.RawMolybdenumBlock;
import net.gera.veridia.common.block.wood.LogBlock;
import net.gera.veridia.common.block.wood.ScrapWoodBlock;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.worldgen.trees.TreeGrowers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Veridia.MOD_ID);


    public static final DeferredBlock<Block> PLANT_CLUMP_BLOCK = registerBlock("plant_clump_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.GRASS)
                    .mapColor(MapColor.GRASS)
                    .strength(0.4f)));
      /// CROPS /// CROPS ///
      public static final DeferredBlock<Block> GARLIC_CROP = BLOCKS.register("garlic_crop",
              () -> new GarlicCrop(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    public static final DeferredBlock<Block> BELL_PEPPER_CROP = BLOCKS.register("bell_pepper_crop",
            () -> new BellPepperCrop(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    // Конопля: двухблочная культура на выжженной пашне (текстуры block/hemp/hemp_*)
    public static final DeferredBlock<Block> HEMP_CROP = BLOCKS.register("hemp_crop",
            () -> new HempCrop(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
      //bush
    public static final DeferredBlock<Block> BLUE_BERRY_BUSH = BLOCKS.register("blue_berry_bush",
              () -> new BlueBerryBush(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)));
    public static final DeferredBlock<Block> GOOSE_BERRY_BUSH = BLOCKS.register("goose_berry_bush",
            () -> new GooseBerryBush(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)));
    public static final DeferredBlock<Block> CRANBERRY_BUSH = BLOCKS.register("cranberry_bush",
            () -> new CranBerryBush(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)));
    public static final DeferredBlock<Block> CUMA_CUMA_BUSH = BLOCKS.register("cuma_cuma_bush",
            () -> new CumaCumaBush(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)
                    .noCollission()
                    .destroyTime(0.1F)));
    public static final DeferredBlock<Block> CUMA_CUMA_BUSH_BLOCK = BLOCKS.register("cuma_cuma_bush_block",
            () -> new CumaCumaBushBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)
                    .noCollission()
                    .destroyTime(0.2F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> CUMA_CUMA_BUSH_SPROUT = BLOCKS.register("cuma_cuma_bush_sprout",
            () -> new CumaCumaBushSprout(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)
                    .noCollission()
                    .destroyTime(0.2F)
                    .noOcclusion()));
    public static final DeferredBlock<Block> PURPURITE_ORE = registerBlock("purpurite_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of()
                            .strength(4f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> VERDANT_FLUORITE_ORE = registerBlock("verdant_fluorite_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of()
                            .strength(4f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> NETHER_BARIUM_ORE = registerBlock("nether_barium_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6),
                    BlockBehaviour.Properties.of()
                            .strength(3.5f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.NETHER_GOLD_ORE)));
    // Сырая молибденовая руда: кучи в лавовых озёрах пепельных равнин; добыча даёт скрап (см. ModBlockLootTableProvider).
    // Текстура - block/raw_molybdenum. Выросшие при генерации блоки украшены скрапом (см. RawMolybdenumBlock)
    public static final DeferredBlock<RawMolybdenumBlock> RAW_MOLYBDENUM_BLOCK = registerBlock("raw_molybdenum_block",
            () -> new RawMolybdenumBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
    // Молибденовый блок (9 слитков, как железный блок) и молибденовая решётка (как железная)
    public static final DeferredBlock<Block> MOLYBDENUM_BLOCK = registerBlock("molybdenum_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_LIGHT_BLUE)));
    public static final DeferredBlock<IronBarsBlock> MOLYBDENUM_BARS = registerBlock("molybdenum_bars",
            () -> new IronBarsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS)));
    public static final DeferredBlock<Block> BLACKSTONE_MAGMA = registerBlock("blackstone_magma",
            () -> new BlackstoneMagmaBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 3)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.GILDED_BLACKSTONE)
                    .isValidSpawn((state, level, pos, type) -> type.fireImmune())
                    .hasPostProcess((state, level, pos) -> true)
                    .emissiveRendering((state, level, pos) -> true)));
    public static final DeferredBlock<Block> PROFUNDUSTONE = registerBlock("profundustone",
            () -> new DropExperienceBlock(UniformInt.of(8, 10),
                    BlockBehaviour.Properties.of()
                            .strength(15f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.TUFF)));
    //wood
    public static final DeferredBlock<Block> SCULK_LOG = registerBlock("sculk_log",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
    public static final DeferredBlock<Block> SCULK_WOOD = registerBlock("sculk_wood",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));
    public static final DeferredBlock<Block> STRIPPED_SCULK_LOG = registerBlock("stripped_sculk_log",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));
    public static final DeferredBlock<Block> STRIPPED_SCULK_WOOD = registerBlock("stripped_sculk_wood",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)));

    // Обожжённые доски: обычный блок досок (свойства как у дубовых), в теге planks, поэтому все рецепты досок работают
    public static final DeferredBlock<Block> CHARRED_PLANKS = registerWoodBlock("charred_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    // Остальной деревянный набор обожжённых досок: свойства и типы (звук, нажатие, соединение заборов) как у дубовых
    public static final DeferredBlock<StairBlock> CHARRED_STAIRS = registerWoodBlock("charred_stairs",
            () -> new StairBlock(CHARRED_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS)));
    public static final DeferredBlock<SlabBlock> CHARRED_SLAB = registerWoodBlock("charred_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)));
    public static final DeferredBlock<FenceBlock> CHARRED_FENCE = registerWoodBlock("charred_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)));
    public static final DeferredBlock<FenceGateBlock> CHARRED_FENCE_GATE = registerWoodBlock("charred_fence_gate",
            () -> new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)));
    public static final DeferredBlock<ButtonBlock> CHARRED_BUTTON = registerWoodBlock("charred_button",
            () -> new ButtonBlock(BlockSetType.OAK, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)));
    public static final DeferredBlock<PressurePlateBlock> CHARRED_PRESSURE_PLATE = registerWoodBlock("charred_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)));
    public static final DeferredBlock<DoorBlock> CHARRED_DOOR = registerWoodBlock("charred_door",
            () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR)));
    public static final DeferredBlock<TrapDoorBlock> CHARRED_TRAPDOOR = registerWoodBlock("charred_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)));
    // Обожжённое дерево пепельных пустошей: бревно, древесина и их очищенные версии, листва. Свойства как у еловых.
    // Не горят (как и обожжённые доски). Листва красится по положению, см. client/AshLeavesTint.
    public static final DeferredBlock<Block> ASHEN_LOG = registerWoodBlock("ashen_log",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LOG)));
    public static final DeferredBlock<Block> ASHEN_WOOD = registerWoodBlock("ashen_wood",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_WOOD)));
    public static final DeferredBlock<Block> STRIPPED_ASHEN_LOG = registerWoodBlock("stripped_ashen_log",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_LOG)));
    public static final DeferredBlock<Block> STRIPPED_ASHEN_WOOD = registerWoodBlock("stripped_ashen_wood",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD)));
    public static final DeferredBlock<Block> ASHEN_LEAVES = registerWoodBlock("ashen_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES)));
    // Очищенные обожжённые бревно и древесина: получаются выплавкой пепельных (см. ModRecipeProvider.ashenWoodRecipes)
    public static final DeferredBlock<Block> STRIPPED_CHARRED_LOG = registerWoodBlock("stripped_charred_log",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_LOG)));
    public static final DeferredBlock<Block> STRIPPED_CHARRED_WOOD = registerWoodBlock("stripped_charred_wood",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_SPRUCE_WOOD)));
    // Пепельные доски, дверь и люк: доски крафтятся из пепельных брёвен, выплавкой из них получаются обожжённые доски
    public static final DeferredBlock<Block> ASHEN_PLANKS = registerWoodBlock("ashen_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<DoorBlock> ASHEN_DOOR = registerWoodBlock("ashen_door",
            () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR)));
    public static final DeferredBlock<TrapDoorBlock> ASHEN_TRAPDOOR = registerWoodBlock("ashen_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)));
    public static final DeferredBlock<StairBlock> ASHEN_STAIRS = registerWoodBlock("ashen_stairs",
            () -> new StairBlock(ASHEN_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS)));
    public static final DeferredBlock<SlabBlock> ASHEN_SLAB = registerWoodBlock("ashen_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)));
    public static final DeferredBlock<FenceBlock> ASHEN_FENCE = registerWoodBlock("ashen_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)));
    public static final DeferredBlock<FenceGateBlock> ASHEN_FENCE_GATE = registerWoodBlock("ashen_fence_gate",
            () -> new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE)));
    public static final DeferredBlock<ButtonBlock> ASHEN_BUTTON = registerWoodBlock("ashen_button",
            () -> new ButtonBlock(BlockSetType.OAK, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)));
    public static final DeferredBlock<PressurePlateBlock> ASHEN_PRESSURE_PLATE = registerWoodBlock("ashen_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)));
    // Кладовые пепельного и обожжённого дерева (как кладовые Farmer's Delight): ящик на 27 слотов, свойства бочки
    public static final DeferredBlock<CabinetBlock> ASHEN_CABINET = registerWoodBlock("ashen_cabinet",
            () -> new CabinetBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)));
    public static final DeferredBlock<CabinetBlock> CHARRED_CABINET = registerWoodBlock("charred_cabinet",
            () -> new CabinetBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)));
    public static final DeferredBlock<Block> SCULK_PLANKS = registerBlock("sculk_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    // Хлам-доски двух пород: ведут себя одинаково (один класс ScrapWoodBlock, одни свойства), отличаются
    // только текстурой. Старое имя блока - scrap_wood, см. алиасы в register().
    public static final DeferredBlock<Block> OAK_SCRAP_WOOD = registerBlock("oak_scrap_wood",
            () -> new ScrapWoodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .strength(ScrapWoodBlock.HARDNESS, ScrapWoodBlock.RESISTANCE)));
    public static final DeferredBlock<Block> SPRUCE_SCRAP_WOOD = registerBlock("spruce_scrap_wood",
            () -> new ScrapWoodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .strength(ScrapWoodBlock.HARDNESS, ScrapWoodBlock.RESISTANCE)));
    // Хлам-доски пепельного и обожжённого дерева: такие же, но не топливо и не горят
    public static final DeferredBlock<Block> ASHEN_SCRAP_WOOD = registerWoodBlock("ashen_scrap_wood",
            () -> new ScrapWoodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .strength(ScrapWoodBlock.HARDNESS, ScrapWoodBlock.RESISTANCE)));
    public static final DeferredBlock<Block> CHARRED_SCRAP_WOOD = registerWoodBlock("charred_scrap_wood",
            () -> new ScrapWoodBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .strength(ScrapWoodBlock.HARDNESS, ScrapWoodBlock.RESISTANCE)));
    public static final DeferredBlock<Block> SCULK_LEAVES = registerBlock("sculk_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    public static final DeferredBlock<Block> SCULK_SPROUT = registerBlock("sculk_sprout",
            () -> new SaplingBlock(TreeGrowers.SCULK_TREE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    // Снежные кирпичи: прочность и инструмент как у кирпичей из грязи, звук снега
    public static final DeferredBlock<Block> SNOW_BRICKS = registerBlock("snow_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.MUD_BRICKS).sound(SoundType.SNOW)));
    // Кирпичи из плотного льда: как кирпичи из грязи, но на PACKED_ICE_BRICKS_EXTRA_STRENGTH прочнее, звук плотного льда
    public static final float PACKED_ICE_BRICKS_EXTRA_STRENGTH = 0.25F;
    public static final DeferredBlock<Block> PACKED_ICE_BRICKS = registerBlock("packed_ice_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.MUD_BRICKS)
                    .strength(Blocks.MUD_BRICKS.defaultDestroyTime() + PACKED_ICE_BRICKS_EXTRA_STRENGTH,
                            Blocks.MUD_BRICKS.getExplosionResistance() + PACKED_ICE_BRICKS_EXTRA_STRENGTH)
                    .sound(SoundType.GLASS)));
    // Опалённый камень (как stone): камень пепельных биомов вместо обычного, см. AshWastesSurfaceRules
    public static final DeferredBlock<Block> SINGE_STONE = registerBlock("singe_stone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<StairBlock> SINGE_STONE_STAIRS = registerBlock("singe_stone_stairs",
            () -> new StairBlock(SINGE_STONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS)));
    public static final DeferredBlock<SlabBlock> SINGE_STONE_SLAB = registerBlock("singe_stone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_SLAB)));
    public static final DeferredBlock<WallBlock> SINGE_STONE_WALL = registerBlock("singe_stone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_WALL)));
    // Опалённый булыжник (как cobblestone): выпадает из опалённого камня, в тегах булыжника - годится для печи,
    // каменных инструментов и прочих рецептов с булыжником
    public static final DeferredBlock<Block> SINGE_COBBLESTONE = registerBlock("singe_cobblestone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)));
    public static final DeferredBlock<StairBlock> SINGE_COBBLESTONE_STAIRS = registerBlock("singe_cobblestone_stairs",
            () -> new StairBlock(SINGE_COBBLESTONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_STAIRS)));
    public static final DeferredBlock<SlabBlock> SINGE_COBBLESTONE_SLAB = registerBlock("singe_cobblestone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_SLAB)));
    public static final DeferredBlock<WallBlock> SINGE_COBBLESTONE_WALL = registerBlock("singe_cobblestone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_WALL)));
    // Кровавый рунный камень: два блока в высоту, 8 поворотов; пока ничего не делает (см. BloodyRunestoneBlock)
    public static final DeferredBlock<Block> BLOODY_RUNESTONE = registerBlock("bloody_runestone",
            () -> new BloodyRunestoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(9.5F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 9)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));
    // Духовный рунный камень: та же форма, копит молитвы зловещих воинов (см. SpiritualRunestoneBlock).
    // Прочнее обсидиана (у него 50) и ломать надо трижды (SpiritualRunestoneBlock.CRACKS); стойкость к взрывам как у
    // обсидиана; светится тем ярче, чем больше молитв и трещин
    public static final DeferredBlock<Block> SPIRITUAL_RUNESTONE = registerBlock("spiritual_runestone",
            () -> new SpiritualRunestoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(70.0F, 1200.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(SpiritualRunestoneBlock::lightLevel)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    // Опалённые кирпичи (как каменные): обычные, замшелые, потрескавшиеся и резной блок. У первых трёх есть ступени,
    // плита и стена, у резного нет. Текстуры ступеней, плит и стен берутся у своего кирпичного блока.
    public static final DeferredBlock<Block> SINGE_STONE_BRICKS = registerBlock("singe_stone_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)));
    public static final DeferredBlock<Block> MOSSY_SINGE_STONE_BRICKS = registerBlock("mossy_singe_stone_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICKS)));
    // Опалённые кирпичи с серо-красным мхом (текстура red_mossy_singe_stone_bricks): отдельный блок, как замшелые
    public static final DeferredBlock<Block> RED_MOSSY_SINGE_STONE_BRICKS = registerBlock("red_mossy_singe_stone_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICKS)));
    // Плитка опалённых кирпичей (как deepslate_tiles): обычная, потрескавшаяся, замшелая и с красным мхом
    public static final DeferredBlock<Block> SINGE_STONE_BRICK_TILES = registerBlock("singe_stone_brick_tiles",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)));
    public static final DeferredBlock<Block> CRACKED_SINGE_STONE_BRICK_TILES = registerBlock("cracked_singe_stone_brick_tiles",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_STONE_BRICKS)));
    public static final DeferredBlock<Block> MOSSY_SINGE_STONE_BRICK_TILES = registerBlock("mossy_singe_stone_brick_tiles",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICKS)));
    public static final DeferredBlock<Block> RED_MOSSY_SINGE_STONE_BRICK_TILES = registerBlock("red_mossy_singe_stone_brick_tiles",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICKS)));
    // Ступени, плиты и стены из плитки (кроме потрескавшейся)
    public static final DeferredBlock<StairBlock> SINGE_STONE_BRICK_TILE_STAIRS = registerBlock("singe_stone_brick_tile_stairs",
            () -> new StairBlock(SINGE_STONE_BRICK_TILES.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_STAIRS)));
    public static final DeferredBlock<SlabBlock> SINGE_STONE_BRICK_TILE_SLAB = registerBlock("singe_stone_brick_tile_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_SLAB)));
    public static final DeferredBlock<WallBlock> SINGE_STONE_BRICK_TILE_WALL = registerBlock("singe_stone_brick_tile_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_WALL)));
    public static final DeferredBlock<StairBlock> MOSSY_SINGE_STONE_BRICK_TILE_STAIRS = registerBlock("mossy_singe_stone_brick_tile_stairs",
            () -> new StairBlock(MOSSY_SINGE_STONE_BRICK_TILES.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICK_STAIRS)));
    public static final DeferredBlock<SlabBlock> MOSSY_SINGE_STONE_BRICK_TILE_SLAB = registerBlock("mossy_singe_stone_brick_tile_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICK_SLAB)));
    public static final DeferredBlock<WallBlock> MOSSY_SINGE_STONE_BRICK_TILE_WALL = registerBlock("mossy_singe_stone_brick_tile_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICK_WALL)));
    public static final DeferredBlock<StairBlock> RED_MOSSY_SINGE_STONE_BRICK_TILE_STAIRS = registerBlock("red_mossy_singe_stone_brick_tile_stairs",
            () -> new StairBlock(RED_MOSSY_SINGE_STONE_BRICK_TILES.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICK_STAIRS)));
    public static final DeferredBlock<SlabBlock> RED_MOSSY_SINGE_STONE_BRICK_TILE_SLAB = registerBlock("red_mossy_singe_stone_brick_tile_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICK_SLAB)));
    public static final DeferredBlock<WallBlock> RED_MOSSY_SINGE_STONE_BRICK_TILE_WALL = registerBlock("red_mossy_singe_stone_brick_tile_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICK_WALL)));
    public static final DeferredBlock<Block> CRACKED_SINGE_STONE_BRICKS = registerBlock("cracked_singe_stone_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_STONE_BRICKS)));
    public static final DeferredBlock<Block> CHISELED_SINGE_STONE = registerBlock("chiseled_singe_stone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_STONE_BRICKS)));
    // Колонны из опалённого камня (как кварцевая колонна: боковая текстура + торец _top) и капитель, которая
    // вешается только на бок блока (PillarCapitalBlock)
    public static final DeferredBlock<RotatedPillarBlock> SINGE_STONE_PILLAR = registerBlock("singe_stone_pillar",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)));
    public static final DeferredBlock<RotatedPillarBlock> WORN_SINGE_STONE_PILLAR = registerBlock("worn_singe_stone_pillar",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_STONE_BRICKS)));
    public static final DeferredBlock<PillarCapitalBlock> SINGE_STONE_PILLAR_IONIC = registerBlock("singe_stone_pillar_ionic",
            () -> new PillarCapitalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)
                    .noOcclusion().pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<StairBlock> SINGE_STONE_BRICK_STAIRS = registerBlock("singe_stone_brick_stairs",
            () -> new StairBlock(SINGE_STONE_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_STAIRS)));
    public static final DeferredBlock<SlabBlock> SINGE_STONE_BRICK_SLAB = registerBlock("singe_stone_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_SLAB)));
    public static final DeferredBlock<WallBlock> SINGE_STONE_BRICK_WALL = registerBlock("singe_stone_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_WALL)));

    public static final DeferredBlock<StairBlock> MOSSY_SINGE_STONE_BRICK_STAIRS = registerBlock("mossy_singe_stone_brick_stairs",
            () -> new StairBlock(MOSSY_SINGE_STONE_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICK_STAIRS)));
    public static final DeferredBlock<SlabBlock> MOSSY_SINGE_STONE_BRICK_SLAB = registerBlock("mossy_singe_stone_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICK_SLAB)));
    public static final DeferredBlock<WallBlock> MOSSY_SINGE_STONE_BRICK_WALL = registerBlock("mossy_singe_stone_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSSY_STONE_BRICK_WALL)));

    public static final DeferredBlock<StairBlock> CRACKED_SINGE_STONE_BRICK_STAIRS = registerBlock("cracked_singe_stone_brick_stairs",
            () -> new StairBlock(CRACKED_SINGE_STONE_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_STAIRS)));
    public static final DeferredBlock<SlabBlock> CRACKED_SINGE_STONE_BRICK_SLAB = registerBlock("cracked_singe_stone_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_SLAB)));
    public static final DeferredBlock<WallBlock> CRACKED_SINGE_STONE_BRICK_WALL = registerBlock("cracked_singe_stone_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_WALL)));

    // Опалённые руды: растут только в опалённом камне (см. ModConfiguredFeatures), свойства и дроп как у ванильных
    public static final DeferredBlock<Block> SINGED_COAL_ORE = registerBlock("singed_coal_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 2), BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE)));
    public static final DeferredBlock<Block> SINGED_IRON_ORE = registerBlock("singed_iron_ore",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)));
    public static final DeferredBlock<Block> SINGED_COPPER_ORE = registerBlock("singed_copper_ore",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE)));
    public static final DeferredBlock<Block> SINGED_LAPIS_ORE = registerBlock("singed_lapis_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_ORE)));
    // Выжженная земля (как dirt): основа поверхности пепельных биомов; в теге dirt, чтобы на ней росла трава
    public static final DeferredBlock<Block> INCINERATED_DIRT = registerBlock("incinerated_dirt",
            () -> new IncineratedDirtBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)));
    // Выжженная пашня (как farmland): из выжженной земли мотыгой; на ней растёт огнеупорная крапива
    public static final DeferredBlock<Block> INCINERATED_FARMLAND = registerBlock("incinerated_farmland",
            () -> new IncineratedFarmlandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND)));
    // Подозрительная выжженная земля: свойства ванильного подозрительного гравия (чистится кистью, падает без опоры),
    // после чистки превращается в выжженную землю; добыча - archaeology/suspicious_incinerated_dirt
    public static final DeferredBlock<Block> SUSPICIOUS_INCINERATED_DIRT = registerBlock("suspicious_incinerated_dirt",
            () -> new SuspiciousIncineratedDirtBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SUSPICIOUS_GRAVEL),
                    INCINERATED_DIRT.get()));
    // Обычная трава (как short_grass), только пепельная
    public static final DeferredBlock<Block> ASHEN_GRASS = registerBlock("ashen_grass",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS)));
    public static final DeferredBlock<Block> SCULK_MUSHROOM = registerBlock("sculk_shroom",
            () -> new SculkMushroomBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM)));
    public static final DeferredBlock<Block> COOKING_TABLE = registerBlock("cooking_table",
            () -> new CookingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
    public static final DeferredBlock<Block> MYTHICAL_TABLE = registerBlock("mythical_table",
            () -> new MythicalTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE).noOcclusion()));
    public static final DeferredBlock<Block> RUNIC_PEDESTAL = registerBlock("runic_pedestal",
            () -> new RunicPedestalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE).noOcclusion()),
            RunicPedestalBlockItem::new);
    public static final DeferredBlock<Block> EXTRACTOR = registerBlock("extractor",
            () -> new ExtractorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK)
                    .noOcclusion()
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> STROP = registerBlock("strop",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE)));
    public static final DeferredBlock<Block> BUILDING_MATERIALS = registerBlock("building_materials",
            () -> new BuildingMaterialsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE).noOcclusion()));
    public static final DeferredBlock<Block> LUMBERJACK_LOG = registerBlock("lumberjack_log",
            () -> new LumberjackLogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)
                    // ofFullCopy тащит из OAK_LOG динамический mapColor(state -> state.getValue(AXIS)...),
                    // а у нас не RotatedPillarBlock - свойства AXIS нет, поэтому падало на создании стейтов.
                    // Заменяем на постоянный цвет.
                    .mapColor(MapColor.WOOD)));
    public static final DeferredBlock<Block> FOOD_STORAGE = registerBlock("food_storage",
            () -> new FoodStorageBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)));

    public static final DeferredBlock<StairBlock> SCULK_STAIRS = registerBlock("sculk_stairs",
            () -> new StairBlock(ModBlocks.SCULK_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.NETHER_WOOD)
                            .mapColor(MapColor.COLOR_BLUE)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<SlabBlock> SCULK_SLAB = registerBlock("sculk_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .strength(4f)
                    .sound(SoundType.NETHER_WOOD)
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<PressurePlateBlock> SCULK_PRESSURE_PLATE = registerBlock("sculk_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .strength(4f)
                    .sound(SoundType.NETHER_WOOD)
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<ButtonBlock> SCULK_BUTTON = registerBlock("sculk_button",
            () -> new ButtonBlock(BlockSetType.IRON, 20,
                    BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.NETHER_WOOD)
                            .mapColor(MapColor.COLOR_BLUE)
                            .noCollission()
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<FenceBlock> SCULK_FENCE = registerBlock("sculk_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.of()
                    .strength(4f)
                    .sound(SoundType.NETHER_WOOD)
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<FenceGateBlock> SCULK_FENCE_GATE = registerBlock("sculk_fence_gate",
            () -> new FenceGateBlock(WoodType.BAMBOO, BlockBehaviour.Properties.of()
                    .strength(4f)
                    .sound(SoundType.NETHER_WOOD)
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<WallBlock> SCULK_WALL = registerBlock("sculk_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .strength(4f)
                    .sound(SoundType.NETHER_WOOD)
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<DoorBlock> SCULK_DOOR = registerBlock("sculk_door",
            () -> new DoorBlock(BlockSetType.OAK,
                    BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.NETHER_WOOD)
                            .mapColor(MapColor.COLOR_BLUE)
                            .noOcclusion()
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<TrapDoorBlock> SCULK_TRAPDOOR = registerBlock("sculk_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.OAK,
                    BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.NETHER_WOOD)
                            .mapColor(MapColor.COLOR_BLUE)
                            .noOcclusion()
                            .requiresCorrectToolForDrops()));
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        return registerBlock(name, block, BlockItem::new);
    }

    // Пепельное и обожжённое дерево не горит, поэтому и не топливо (см. NoFuelBlockItem)
    private static <T extends Block> DeferredBlock<T> registerWoodBlock(String name, Supplier<T> block) {
        return registerBlock(name, block, NoFuelBlockItem::new);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block,
                                                                      BiFunction<T, Item.Properties, BlockItem> itemFactory) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> itemFactory.apply(toReturn.get(), new Item.Properties()));
        return toReturn;
    }

    // До переименования блок назывался veridia:scrap_wood - это имя записано в NBT готовых стен, башен и
    // домов, в чанках уже построенных миров и в инвентарях. Алиас заставляет реестр отдавать на старое имя
    // новый блок и предмет.
    private static final ResourceLocation LEGACY_SCRAP_WOOD =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "scrap_wood");
    private static final ResourceLocation OAK_SCRAP_WOOD_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "oak_scrap_wood");

    // Бревно, древесина и листва пепельных лесов раньше назывались charred_*; старые имена в сохранённых мирах и
    // инвентарях алиасом отдают новые блоки и предметы. У очищенных stripped_charred_log и stripped_charred_wood
    // теперь свои блоки (выплавка), поэтому алиаса у них нет.
    private static final List<String> RENAMED_TO_ASHEN = List.of("log", "wood", "leaves");

    private static void addAshenAliases() {
        for (String part : RENAMED_TO_ASHEN) {
            ResourceLocation oldId = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "charred_" + part);
            ResourceLocation newId = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "ashen_" + part);
            BLOCKS.addAlias(oldId, newId);
            ModItems.ITEMS.addAlias(oldId, newId);
        }
    }

    public static void register(IEventBus eventBus) {
        addAshenAliases();
        BLOCKS.addAlias(LEGACY_SCRAP_WOOD, OAK_SCRAP_WOOD_ID);
        ModItems.ITEMS.addAlias(LEGACY_SCRAP_WOOD, OAK_SCRAP_WOOD_ID);
        BLOCKS.register(eventBus);
    }
}