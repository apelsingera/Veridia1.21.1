package net.gera.veridia.block;

import net.gera.veridia.Veridia;
import net.gera.veridia.block.bushes.BlueBerryBush;
import net.gera.veridia.block.crop.GarlicCrop;
import net.gera.veridia.block.tree.LogBlock;
import net.gera.veridia.item.ModItems;
import net.gera.veridia.worldgen.trees.TreeGrowers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Veridia.MOD_ID);


    public static final DeferredBlock<Block> PLANT_CLUMP_BLOCK = registerBlock("plant_clump_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.GRASS)
                    .mapColor(MapColor.GRASS)
                    .strength(0.4f)));
    public static final DeferredBlock<Block> COPPER_ORE = registerBlock("copper_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> DEEPSLATE_COPPER_ORE = registerBlock("deepslate_copper_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> GRANITE_COPPER_ORE = registerBlock("granite_copper_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> ANDESITE_COPPER_ORE = registerBlock("andesite_copper_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> DIORITE_COPPER_ORE = registerBlock("diorite_copper_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> TUFF_COPPER_ORE = registerBlock("tuff_copper_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.TUFF)));
    //
    public static final DeferredBlock<Block> GRANITE_TIN_ORE = registerBlock("granite_tin_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> ANDESITE_TIN_ORE = registerBlock("andesite_tin_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> DIORITE_TIN_ORE = registerBlock("diorite_tin_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> TUFF_TIN_ORE = registerBlock("tuff_tin_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.TUFF)));
    public static final DeferredBlock<Block> TIN_ORE = registerBlock("tin_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.TUFF)));
    public static final DeferredBlock<Block> DEEPSLATE_TIN_ORE = registerBlock("deepslate_tin_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.TUFF)));

    //
    public static final DeferredBlock<Block> TIN_BLOCK = registerBlock("tin_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(4f)));

    public static final DeferredBlock<StairBlock> TIN_STAIRS = registerBlock("tin_stairs",
            () -> new StairBlock(ModBlocks.TIN_BLOCK.get().defaultBlockState(),
                    BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.COPPER)
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<SlabBlock> TIN_SLAB = registerBlock("tin_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.COPPER)
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<PressurePlateBlock> TIN_PRESSURE_PLATE = registerBlock("tin_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.GOLD, BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.COPPER)
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<ButtonBlock> TIN_BUTTON = registerBlock("tin_button",
            () -> new ButtonBlock(BlockSetType.IRON, 20,
                    BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.COPPER)
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .noCollission()
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<FenceBlock> TIN_FENCE = registerBlock("tin_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.COPPER)
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<FenceGateBlock> TIN_FENCE_GATE = registerBlock("tin_fence_gate",
            () -> new FenceGateBlock(WoodType.BAMBOO, BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.COPPER)
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<WallBlock> TIN_WALL = registerBlock("tin_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.COPPER)
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<DoorBlock> TIN_DOOR = registerBlock("tin_door",
            () -> new DoorBlock(BlockSetType.IRON,
                    BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.COPPER)
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .noOcclusion()
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<TrapDoorBlock> TIN_TRAPDOOR = registerBlock("tin_trapdoor",
            () -> new TrapDoorBlock(BlockSetType.IRON,
                    BlockBehaviour.Properties.of()
                            .strength(4f)
                            .sound(SoundType.COPPER)
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .noOcclusion()
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> AZOTIC_TOPAZ_END_ORE = registerBlock("azotic_topaz_end_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6),
                    BlockBehaviour.Properties.of()
                            .strength(7f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.AMETHYST)));
    public static final DeferredBlock<Block> DATOLITE_NETHER_ORE = registerBlock("datolite_nether_ore",
            () -> new DropExperienceBlock(UniformInt.of(0, 0),
                    BlockBehaviour.Properties.of()
                            .strength(5f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.NETHERRACK)));
    //
      /// CROPS /// CROPS ///
      public static final DeferredBlock<Block> GARLIC_CROP = BLOCKS.register("garlic_crop",
              () -> new GarlicCrop(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
      //bush
    public static final DeferredBlock<Block> BLUE_BERRY_BUSH = BLOCKS.register("blue_berry_bush",
              () -> new BlueBerryBush(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)));

    //wood
    public static final DeferredBlock<Block> SCULK_LOG = registerBlock("sculk_log",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
    public static final DeferredBlock<Block> SCULK_WOOD = registerBlock("sculk_wood",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));
    public static final DeferredBlock<Block> STRIPPED_SCULK_LOG = registerBlock("stripped_sculk_log",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));
    public static final DeferredBlock<Block> STRIPPED_SCULK_WOOD = registerBlock("stripped_sculk_wood",
            () -> new LogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)));

    public static final DeferredBlock<Block> SCULK_PLANKS = registerBlock("sculk_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    public static final DeferredBlock<Block> SCULK_LEAVES = registerBlock("sculk_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    public static final DeferredBlock<Block> SCULK_SPROUT = registerBlock("sculk_sprout",
            () -> new SaplingBlock(TreeGrowers.SCULK_TREE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING))
    );


    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
