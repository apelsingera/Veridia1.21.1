package net.gera.veridia.common.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Veridia.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Молибденовая ножовка: цепи, медные и железные решётки, заборы, калитки, громоотвод, таблички, флаги
        tag(ModTags.Blocks.MINEABLE_WITH_HACKSAW)
                .add(Blocks.CHAIN)
                .add(Blocks.IRON_BARS)
                .add(Blocks.LIGHTNING_ROD)
                .add(Blocks.COPPER_GRATE, Blocks.EXPOSED_COPPER_GRATE, Blocks.WEATHERED_COPPER_GRATE,
                        Blocks.OXIDIZED_COPPER_GRATE, Blocks.WAXED_COPPER_GRATE, Blocks.WAXED_EXPOSED_COPPER_GRATE,
                        Blocks.WAXED_WEATHERED_COPPER_GRATE, Blocks.WAXED_OXIDIZED_COPPER_GRATE)
                .addTag(BlockTags.FENCES)
                .addTag(BlockTags.FENCE_GATES)
                .addTag(BlockTags.ALL_SIGNS)
                .addTag(BlockTags.BANNERS);

        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(ModBlocks.COOKING_TABLE.get())
                .add(ModBlocks.SCULK_LOG.get())
                .add(ModBlocks.SCULK_WOOD.get())
                .add(ModBlocks.STRIPPED_SCULK_LOG.get())
                .add(ModBlocks.STRIPPED_SCULK_WOOD.get())
                .add(ModBlocks.SCULK_PLANKS.get())
                .add(ModBlocks.OAK_SCRAP_WOOD.get())
                .add(ModBlocks.SPRUCE_SCRAP_WOOD.get())
                .add(ModBlocks.ASHEN_SCRAP_WOOD.get())
                .add(ModBlocks.CHARRED_SCRAP_WOOD.get())
                .add(ModBlocks.SCULK_STAIRS.get())
                .add(ModBlocks.SCULK_SLAB.get())
                .add(ModBlocks.SCULK_FENCE.get())
                .add(ModBlocks.SCULK_FENCE_GATE.get())
                .add(ModBlocks.SCULK_PRESSURE_PLATE.get())
                .add(ModBlocks.SCULK_BUTTON.get())
                .add(ModBlocks.SCULK_TRAPDOOR.get())
                .add(ModBlocks.SCULK_DOOR.get())
                .add(ModBlocks.BUILDING_MATERIALS.get())
                .add(ModBlocks.STROP.get())
                .add(ModBlocks.LUMBERJACK_LOG.get())
                .add(ModBlocks.FOOD_STORAGE.get());

        // Обожжённое дерево: бревна в теге logs (держат листву и горят в печи), листва в leaves (иначе не считается листвой)
        tag(ModTags.Blocks.ASHEN_LOGS)
                .add(ModBlocks.ASHEN_LOG.get())
                .add(ModBlocks.ASHEN_WOOD.get())
                .add(ModBlocks.STRIPPED_ASHEN_LOG.get())
                .add(ModBlocks.STRIPPED_ASHEN_WOOD.get());
        tag(BlockTags.LOGS).addTag(ModTags.Blocks.ASHEN_LOGS);
        tag(BlockTags.MINEABLE_WITH_AXE).addTag(ModTags.Blocks.ASHEN_LOGS);
        tag(ModTags.Blocks.CHARRED_LOGS)
                .add(ModBlocks.STRIPPED_CHARRED_LOG.get())
                .add(ModBlocks.STRIPPED_CHARRED_WOOD.get());
        tag(BlockTags.LOGS).addTag(ModTags.Blocks.CHARRED_LOGS);
        tag(BlockTags.MINEABLE_WITH_AXE).addTag(ModTags.Blocks.CHARRED_LOGS);
        tag(BlockTags.LEAVES).add(ModBlocks.ASHEN_LEAVES.get());

        tag(BlockTags.MINEABLE_WITH_HOE)
                .add(ModBlocks.SCULK_LEAVES.get())
                .add(ModBlocks.ASHEN_LEAVES.get());
        tag(BlockTags.PLANKS)
                .add(ModBlocks.CHARRED_PLANKS.get())
                .add(ModBlocks.SCULK_PLANKS.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.PURPURITE_ORE.get())
                .add(ModBlocks.VERDANT_FLUORITE_ORE.get())
                .add(ModBlocks.NETHER_BARIUM_ORE.get())
                .add(ModBlocks.PROFUNDUSTONE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.BLACKSTONE_MAGMA.get())
                .add(ModBlocks.PURPURITE_ORE.get())
                .add(ModBlocks.VERDANT_FLUORITE_ORE.get())
                .add(ModBlocks.PROFUNDUSTONE.get())
                .add(ModBlocks.NETHER_BARIUM_ORE.get())
                .add(ModBlocks.EXTRACTOR.get());
        tag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)
                .add(ModBlocks.PROFUNDUSTONE.get());
        // Сырой молибден: железная кирка
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.RAW_MOLYBDENUM_BLOCK.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.RAW_MOLYBDENUM_BLOCK.get());
        // Молибденовый блок как железный: кирка (каменная и лучше), основание маяка, общий тег блоков молибдена.
        // Решётка как железная: кирка и ножовка (тег mineable/hacksaw)
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.MOLYBDENUM_BLOCK.get(), ModBlocks.MOLYBDENUM_BARS.get());
        tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.MOLYBDENUM_BLOCK.get());
        tag(BlockTags.BEACON_BASE_BLOCKS).add(ModBlocks.MOLYBDENUM_BLOCK.get());
        tag(ModTags.Blocks.MOLYBDENUM_STORAGE_BLOCKS).add(ModBlocks.MOLYBDENUM_BLOCK.get());
        tag(ModTags.Blocks.MINEABLE_WITH_HACKSAW).add(ModBlocks.MOLYBDENUM_BARS.get());
        // Выжженная пашня копается лопатой, а конопля не даёт ей высохнуть до земли
        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.INCINERATED_FARMLAND.get());
        tag(BlockTags.MAINTAINS_FARMLAND).add(ModBlocks.HEMP_CROP.get());

        // Опалённые кирпичи: кирка, группы ступеней/плит/стен (стены по тегу соединяются с соседями)
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.SINGE_STONE_BRICKS.get(), ModBlocks.MOSSY_SINGE_STONE_BRICKS.get(),
                        ModBlocks.CRACKED_SINGE_STONE_BRICKS.get(), ModBlocks.CHISELED_SINGE_STONE.get(),
                        ModBlocks.RED_MOSSY_SINGE_STONE_BRICKS.get(), ModBlocks.SINGE_STONE_BRICK_TILES.get(),
                        ModBlocks.CRACKED_SINGE_STONE_BRICK_TILES.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_TILES.get(),
                        ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILES.get())
                .add(ModBlocks.SINGE_STONE_PILLAR.get(), ModBlocks.WORN_SINGE_STONE_PILLAR.get(),
                        ModBlocks.SINGE_STONE_PILLAR_IONIC.get())
                .add(ModBlocks.SINGE_STONE_BRICK_STAIRS.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_STAIRS.get(),
                        ModBlocks.CRACKED_SINGE_STONE_BRICK_STAIRS.get())
                .add(ModBlocks.SINGE_STONE_BRICK_SLAB.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_SLAB.get(),
                        ModBlocks.CRACKED_SINGE_STONE_BRICK_SLAB.get())
                .add(ModBlocks.SINGE_STONE_BRICK_WALL.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_WALL.get(),
                        ModBlocks.CRACKED_SINGE_STONE_BRICK_WALL.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.SINGE_STONE_STAIRS.get(), ModBlocks.SINGE_STONE_SLAB.get(), ModBlocks.SINGE_STONE_WALL.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.BLOODY_RUNESTONE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.SPIRITUAL_RUNESTONE.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(ModBlocks.SPIRITUAL_RUNESTONE.get());
        tag(BlockTags.STAIRS)
                .add(ModBlocks.SINGE_STONE_STAIRS.get())
                .add(ModBlocks.SINGE_STONE_BRICK_STAIRS.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_STAIRS.get(),
                        ModBlocks.CRACKED_SINGE_STONE_BRICK_STAIRS.get());
        tag(BlockTags.SLABS)
                .add(ModBlocks.SINGE_STONE_SLAB.get())
                .add(ModBlocks.SINGE_STONE_BRICK_SLAB.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_SLAB.get(),
                        ModBlocks.CRACKED_SINGE_STONE_BRICK_SLAB.get());
        tag(BlockTags.WALLS)
                .add(ModBlocks.SINGE_STONE_WALL.get())
                .add(ModBlocks.SINGE_STONE_BRICK_WALL.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_WALL.get(),
                        ModBlocks.CRACKED_SINGE_STONE_BRICK_WALL.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.CHARRED_PLANKS.get());
        // Обожжённые доски: топор и деревянные группы (по ним заборы соединяются с дубовыми, стоит топливо и т. д.)
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(ModBlocks.CHARRED_STAIRS.get(), ModBlocks.CHARRED_SLAB.get(), ModBlocks.CHARRED_FENCE.get(),
                        ModBlocks.CHARRED_FENCE_GATE.get(), ModBlocks.CHARRED_BUTTON.get(), ModBlocks.CHARRED_PRESSURE_PLATE.get());
        tag(BlockTags.STAIRS).add(ModBlocks.CHARRED_STAIRS.get());
        tag(BlockTags.WOODEN_STAIRS).add(ModBlocks.CHARRED_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.CHARRED_SLAB.get());
        tag(BlockTags.WOODEN_SLABS).add(ModBlocks.CHARRED_SLAB.get());
        tag(BlockTags.FENCES).add(ModBlocks.CHARRED_FENCE.get());
        tag(BlockTags.WOODEN_FENCES).add(ModBlocks.CHARRED_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.CHARRED_FENCE_GATE.get());
        tag(BlockTags.BUTTONS).add(ModBlocks.CHARRED_BUTTON.get());
        tag(BlockTags.WOODEN_BUTTONS).add(ModBlocks.CHARRED_BUTTON.get());
        tag(BlockTags.PRESSURE_PLATES).add(ModBlocks.CHARRED_PRESSURE_PLATE.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(ModBlocks.CHARRED_PRESSURE_PLATE.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.CHARRED_DOOR.get(), ModBlocks.CHARRED_TRAPDOOR.get());
        tag(BlockTags.DOORS).add(ModBlocks.CHARRED_DOOR.get());
        tag(BlockTags.WOODEN_DOORS).add(ModBlocks.CHARRED_DOOR.get());
        tag(BlockTags.TRAPDOORS).add(ModBlocks.CHARRED_TRAPDOOR.get());
        tag(BlockTags.WOODEN_TRAPDOORS).add(ModBlocks.CHARRED_TRAPDOOR.get());
        // Пепельные доски, дверь и люк: как обожжённые
        tag(BlockTags.PLANKS).add(ModBlocks.ASHEN_PLANKS.get());
        // Ступени, плиты и стены из плитки (кроме потрескавшейся)
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.SINGE_STONE_BRICK_TILE_STAIRS.get(), ModBlocks.SINGE_STONE_BRICK_TILE_SLAB.get(), ModBlocks.SINGE_STONE_BRICK_TILE_WALL.get());
        tag(BlockTags.STAIRS).add(ModBlocks.SINGE_STONE_BRICK_TILE_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.SINGE_STONE_BRICK_TILE_SLAB.get());
        tag(BlockTags.WALLS).add(ModBlocks.SINGE_STONE_BRICK_TILE_WALL.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_STAIRS.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_SLAB.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_WALL.get());
        tag(BlockTags.STAIRS).add(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_SLAB.get());
        tag(BlockTags.WALLS).add(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_WALL.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_STAIRS.get(), ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_SLAB.get(), ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_WALL.get());
        tag(BlockTags.STAIRS).add(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_SLAB.get());
        tag(BlockTags.WALLS).add(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_WALL.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.ASHEN_PLANKS.get(), ModBlocks.ASHEN_DOOR.get(), ModBlocks.ASHEN_TRAPDOOR.get());
        tag(BlockTags.DOORS).add(ModBlocks.ASHEN_DOOR.get());
        tag(BlockTags.WOODEN_DOORS).add(ModBlocks.ASHEN_DOOR.get());
        tag(BlockTags.TRAPDOORS).add(ModBlocks.ASHEN_TRAPDOOR.get());
        tag(BlockTags.WOODEN_TRAPDOORS).add(ModBlocks.ASHEN_TRAPDOOR.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.ASHEN_STAIRS.get(), ModBlocks.ASHEN_SLAB.get(), ModBlocks.ASHEN_FENCE.get(),
                ModBlocks.ASHEN_FENCE_GATE.get(), ModBlocks.ASHEN_BUTTON.get(), ModBlocks.ASHEN_PRESSURE_PLATE.get());
        tag(BlockTags.STAIRS).add(ModBlocks.ASHEN_STAIRS.get());
        tag(BlockTags.WOODEN_STAIRS).add(ModBlocks.ASHEN_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.ASHEN_SLAB.get());
        tag(BlockTags.WOODEN_SLABS).add(ModBlocks.ASHEN_SLAB.get());
        tag(BlockTags.FENCES).add(ModBlocks.ASHEN_FENCE.get());
        tag(BlockTags.WOODEN_FENCES).add(ModBlocks.ASHEN_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.ASHEN_FENCE_GATE.get());
        tag(BlockTags.BUTTONS).add(ModBlocks.ASHEN_BUTTON.get());
        tag(BlockTags.WOODEN_BUTTONS).add(ModBlocks.ASHEN_BUTTON.get());
        tag(BlockTags.PRESSURE_PLATES).add(ModBlocks.ASHEN_PRESSURE_PLATE.get());
        tag(BlockTags.WOODEN_PRESSURE_PLATES).add(ModBlocks.ASHEN_PRESSURE_PLATE.get());
        // Кладовые: топор и тег деревянных кладовых Farmer's Delight (тег безвреден и без самого мода)
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.ASHEN_CABINET.get(), ModBlocks.CHARRED_CABINET.get());
        tag(ModTags.Blocks.FARMERSDELIGHT_WOODEN_CABINETS).add(ModBlocks.ASHEN_CABINET.get(), ModBlocks.CHARRED_CABINET.get());
        // Опалённые руды: те же теги, что у ванильных (кирка, уровень инструмента, группы руд)
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.SINGED_COAL_ORE.get(), ModBlocks.SINGED_IRON_ORE.get(),
                        ModBlocks.SINGED_COPPER_ORE.get(), ModBlocks.SINGED_LAPIS_ORE.get());
        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(ModBlocks.SINGED_IRON_ORE.get(), ModBlocks.SINGED_COPPER_ORE.get(), ModBlocks.SINGED_LAPIS_ORE.get());
        tag(BlockTags.COAL_ORES).add(ModBlocks.SINGED_COAL_ORE.get());
        tag(BlockTags.IRON_ORES).add(ModBlocks.SINGED_IRON_ORE.get());
        tag(BlockTags.COPPER_ORES).add(ModBlocks.SINGED_COPPER_ORE.get());
        tag(BlockTags.LAPIS_ORES).add(ModBlocks.SINGED_LAPIS_ORE.get());
        // Опалённый камень - обычный камень для мира: его ломают киркой и режут пещеры. В stone_ore_replaceables его
        // НЕТ намеренно: ванильные руды и гранит с диоритом не должны попадать в него, руды тут только опалённые
        // (см. ModConfiguredFeatures.singedOre).
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.SINGE_STONE.get());
        tag(BlockTags.BASE_STONE_OVERWORLD)
                .add(ModBlocks.SINGE_STONE.get());
        // Опалённый булыжник: кирка, общий тег булыжника (c:cobblestones), ступени/плита/стена в своих группах
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.SINGE_COBBLESTONE.get(), ModBlocks.SINGE_COBBLESTONE_STAIRS.get(),
                        ModBlocks.SINGE_COBBLESTONE_SLAB.get(), ModBlocks.SINGE_COBBLESTONE_WALL.get());
        tag(Tags.Blocks.COBBLESTONES).add(ModBlocks.SINGE_COBBLESTONE.get());
        // Опалённый камень - в общем теге камня (c:stones), по нему рецепты модов принимают любой камень
        tag(Tags.Blocks.STONES).add(ModBlocks.SINGE_STONE.get());
        tag(BlockTags.STAIRS).add(ModBlocks.SINGE_COBBLESTONE_STAIRS.get());
        tag(BlockTags.SLABS).add(ModBlocks.SINGE_COBBLESTONE_SLAB.get());
        tag(BlockTags.WALLS).add(ModBlocks.SINGE_COBBLESTONE_WALL.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.SNOW_BRICKS.get())
                .add(ModBlocks.PACKED_ICE_BRICKS.get());
        // Выжженная земля - обычная земля для игры: на ней растут растения, её копают лопатой
        tag(BlockTags.DIRT)
                .add(ModBlocks.INCINERATED_DIRT.get());
        tag(BlockTags.MINEABLE_WITH_SHOVEL)
                .add(ModBlocks.INCINERATED_DIRT.get())
                .add(ModBlocks.SUSPICIOUS_INCINERATED_DIRT.get());
        tag(ModTags.Blocks.ASH_WASTES_GROUND)
                .addTag(BlockTags.SAND)
                .addTag(BlockTags.DIRT)
                .addTag(BlockTags.TERRACOTTA)
                .addTag(BlockTags.BASE_STONE_OVERWORLD)
                .add(Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.GRAVEL, Blocks.BLACKSTONE, Blocks.SMOOTH_BASALT)
                .add(ModBlocks.BLACKSTONE_MAGMA.get());

        tag(ModTags.Blocks.COPPER_BLOCKS)
                .add(Blocks.COPPER_BLOCK, Blocks.EXPOSED_COPPER, Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER)
                .add(Blocks.WAXED_COPPER_BLOCK, Blocks.WAXED_EXPOSED_COPPER, Blocks.WAXED_WEATHERED_COPPER, Blocks.WAXED_OXIDIZED_COPPER)
                .add(Blocks.CUT_COPPER, Blocks.EXPOSED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER)
                .add(Blocks.WAXED_CUT_COPPER, Blocks.WAXED_EXPOSED_CUT_COPPER, Blocks.WAXED_WEATHERED_CUT_COPPER, Blocks.WAXED_OXIDIZED_CUT_COPPER)
                .add(Blocks.CUT_COPPER_STAIRS, Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.WEATHERED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_CUT_COPPER_STAIRS)
                .add(Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS, Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS)
                .add(Blocks.CUT_COPPER_SLAB, Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.OXIDIZED_CUT_COPPER_SLAB)
                .add(Blocks.WAXED_CUT_COPPER_SLAB, Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB)
                .add(Blocks.CHISELED_COPPER, Blocks.EXPOSED_CHISELED_COPPER, Blocks.WEATHERED_CHISELED_COPPER, Blocks.OXIDIZED_CHISELED_COPPER)
                .add(Blocks.WAXED_CHISELED_COPPER, Blocks.WAXED_EXPOSED_CHISELED_COPPER, Blocks.WAXED_WEATHERED_CHISELED_COPPER, Blocks.WAXED_OXIDIZED_CHISELED_COPPER)
                .add(Blocks.COPPER_GRATE, Blocks.EXPOSED_COPPER_GRATE, Blocks.WEATHERED_COPPER_GRATE, Blocks.OXIDIZED_COPPER_GRATE)
                .add(Blocks.WAXED_COPPER_GRATE, Blocks.WAXED_EXPOSED_COPPER_GRATE, Blocks.WAXED_WEATHERED_COPPER_GRATE, Blocks.WAXED_OXIDIZED_COPPER_GRATE)
                .add(Blocks.COPPER_BULB, Blocks.EXPOSED_COPPER_BULB, Blocks.WEATHERED_COPPER_BULB, Blocks.OXIDIZED_COPPER_BULB)
                .add(Blocks.WAXED_COPPER_BULB, Blocks.WAXED_EXPOSED_COPPER_BULB, Blocks.WAXED_WEATHERED_COPPER_BULB, Blocks.WAXED_OXIDIZED_COPPER_BULB)
                .add(Blocks.COPPER_DOOR, Blocks.EXPOSED_COPPER_DOOR, Blocks.WEATHERED_COPPER_DOOR, Blocks.OXIDIZED_COPPER_DOOR)
                .add(Blocks.WAXED_COPPER_DOOR, Blocks.WAXED_EXPOSED_COPPER_DOOR, Blocks.WAXED_WEATHERED_COPPER_DOOR, Blocks.WAXED_OXIDIZED_COPPER_DOOR)
                .add(Blocks.COPPER_TRAPDOOR, Blocks.EXPOSED_COPPER_TRAPDOOR, Blocks.WEATHERED_COPPER_TRAPDOOR, Blocks.OXIDIZED_COPPER_TRAPDOOR)
                .add(Blocks.WAXED_COPPER_TRAPDOOR, Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR, Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR, Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR)
                .add(Blocks.RAW_COPPER_BLOCK)
                .add(Blocks.LIGHTNING_ROD);
        //this s### was annoying as hell
        tag(ModTags.Blocks.BUSHES)
                .add(ModBlocks.GOOSE_BERRY_BUSH.getKey())
                .add(ModBlocks.CRANBERRY_BUSH.getKey())
                .add(ModBlocks.BLUE_BERRY_BUSH.getKey())
                .add(ModBlocks.CUMA_CUMA_BUSH.getKey())
                .add(ModBlocks.CUMA_CUMA_BUSH_BLOCK.getKey())
                .add(ModBlocks.CUMA_CUMA_BUSH_SPROUT.getKey());
    }
}