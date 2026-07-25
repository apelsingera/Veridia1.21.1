package net.gera.veridiafoods.block;

import com.mojang.serialization.MapCodec;
import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.block.bushes.BlueBerryBush;
import net.gera.veridiafoods.block.bushes.CranBerryBush;
import net.gera.veridiafoods.block.bushes.GooseBerryBush;
import net.gera.veridiafoods.block.crop.BellPepperCrop;
import net.gera.veridiafoods.block.crop.GarlicCrop;
import net.gera.veridiafoods.block.tree.LogBlock;
import net.gera.veridiafoods.item.ModItems;
import net.gera.veridiafoods.worldgen.trees.TreeGrowers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VeridiaFoods.MOD_ID);


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
      //bush
    public static final DeferredBlock<Block> BLUE_BERRY_BUSH = BLOCKS.register("blue_berry_bush",
              () -> new BlueBerryBush(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)));
    public static final DeferredBlock<Block> GOOSE_BERRY_BUSH = BLOCKS.register("goose_berry_bush",
            () -> new GooseBerryBush(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)));
    public static final DeferredBlock<Block> CRANBERRY_BUSH = BLOCKS.register("cranberry_bush",
            () -> new CranBerryBush(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH)));

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
            () -> new SaplingBlock(TreeGrowers.SCULK_TREE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    public static final DeferredBlock<Block> COOKING_TABLE = registerBlock("cooking_table",
            () -> new net.gera.veridiafoods.block.entity.types.cookingtable.
                    CookingTable(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));

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
