package net.gera.veridia.common.item;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.fluid.ModFluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Veridia.MOD_ID);

    public static final Supplier<CreativeModeTab> FTAB = CREATIVE_MODE_TAB.register("ftab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.PLANT_CLUMP.get()))
                    .title(Component.translatable("ftab.veridia.ftab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.PLANT_STRING);
                        output.accept(ModItems.FIBER_NET);
                        output.accept(ModFluids.TANNIN_BUCKET);
                        output.accept(ModFluids.CORRUPTION_BUCKET);
                        output.accept(ModItems.TANNED_LEATHER);
                        output.accept(ModItems.STRONG_CLOTH);
                        output.accept(ModItems.STRONG_HANDLE);
                        output.accept(ModItems.PLANT_CLUMP);
                        output.accept(ModItems.DANDELIONS_ROOT);
                        output.accept(ModItems.TOMATO_CHERRY);
                        output.accept(ModItems.RED_ONION);
                        output.accept(ModItems.GARLIC);
                        output.accept(ModItems.GARLIC_SEEDS);
                        output.accept(ModItems.HEMP_SEEDS);
                        output.accept(ModItems.RED_BELL_PEPPER);
                        output.accept(ModItems.RED_BELL_PEPPER_SLICE);
                        output.accept(ModItems.BELL_PEPPER_SEEDS);
                        output.accept(ModItems.BLUE_BERRIES);
                        output.accept(ModItems.GOOSE_BERRIES);
                        output.accept(ModItems.CRANBERRIES);
                        output.accept(ModItems.CUMACUMA_BERRY);

                        output.accept(ModItems.BLUE_BERRY_JUICE);
                        output.accept(ModItems.SUPER_BLUE_BERRY_JUICE);
                        output.accept(ModItems.CRANBERRY_JUICE);
                        output.accept(ModItems.SUPER_CRANBERRY_JUICE);
                        output.accept(ModItems.GOOSE_BERRY_JUICE);
                        output.accept(ModItems.SUPER_GOOSE_BERRY_JUICE);

                        output.accept(ModItems.HORSE_MEAT);
                        output.accept(ModItems.COOKED_HORSE_MEAT);
                        output.accept(ModItems.PIRANHA);
                        output.accept(ModItems.GOLITAH_TIGERFISH);
                        output.accept(ModItems.DEAD_FISH);
                        output.accept(ModItems.PORKCHOP_SLICE);
                        output.accept(ModItems.GROUND_PORK);
                        output.accept(ModItems.PORK_PATTY);
                        output.accept(ModItems.COOKED_PORK_PATTY);
                        output.accept(ModItems.GROUND_BEEF);
                        output.accept(ModItems.BEEF_PATTY);
                        output.accept(ModItems.COOKED_BEEF_PATTY);

                        output.accept(ModItems.UNFINISHED_STUFFED_PEPPER);
                        output.accept(ModItems.STUFFED_PEPPER);
                        output.accept(ModItems.TASTY_MEAT_MIX);
                        output.accept(ModItems.BEEF_BURGER);
                        output.accept(ModItems.CHICKEN_LEGS);
                        output.accept(ModItems.BEEF_W_POTATOES);
                        output.accept(ModItems.PIQUANT_ELITE_FISH_DISH);
                        output.accept(ModItems.NICE_BREAKFAST);
                        output.accept(ModItems.BESH);

                        output.accept(ModItems.BLUEBERRY_PIE);
                        output.accept(ModItems.BLUEBERRY_PIE_SLICE);
                        output.accept(ModItems.GOOSEBERRY_PIE);
                        output.accept(ModItems.GOOSEBERRY_PIE_SLICE);
                        output.accept(ModItems.CRANBERRY_PIE);
                        output.accept(ModItems.CRANBERRY_PIE_SLICE);

                        output.accept(ModItems.WHEAT_FLOUR);
                        output.accept(ModItems.DOUGH);
                        output.accept(ModItems.PASTA);
                        output.accept(ModItems.TASTY_PASTA);
                        output.accept(ModItems.PASTA_BUCKET);
                        output.accept(ModItems.COOKED_PASTA_BUCKET);
                        output.accept(ModItems.CARROT_PASTA);
                        output.accept(ModItems.RONALDO_PASTA);
                        output.accept(ModItems.POTATO_PUREE);
                        /// - /// - /// - /// - /// - /// - /// - ///
                        output.accept(ModItems.MILK_N_BUTTER);
                        output.accept(ModItems.BUTTER);
                        output.accept(ModItems.CHEESE);
                        output.accept(ModItems.BOILED_EGG);
                        output.accept(ModItems.EGG_BUCKET);
                        output.accept(ModItems.BOILED_EGG_BUCKET);

                        output.accept(ModItems.EXOTIC_SEASONING1);
                        output.accept(ModItems.EXOTIC_SEASONING2);
                        output.accept(ModItems.EXOTIC_SEASONING3);

                        output.accept(ModBlocks.COOKING_TABLE);
                        output.accept(ModBlocks.STROP);
                        output.accept(ModBlocks.BUILDING_MATERIALS);
                        output.accept(ModBlocks.LUMBERJACK_LOG);
                        output.accept(ModBlocks.FOOD_STORAGE);
                    }).build());
    // Вкладка со строительными и природными блоками мода (дерево, земля, опалённый камень и его кирпичи, руды, растения)
    public static final Supplier<CreativeModeTab> BTAB = CREATIVE_MODE_TAB.register("btab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.SINGE_STONE_BRICKS.get()))
                    .title(Component.translatable("btab.veridia.btab"))
                    .displayItems((itemDisplayParameters, output) -> {
                        // Дерево: скалк, затем пепельное и обожжённое (доски по очереди, наборы в одном порядке)
                        output.accept(ModBlocks.SCULK_LOG);
                        output.accept(ModBlocks.STRIPPED_SCULK_LOG);
                        output.accept(ModBlocks.SCULK_WOOD);
                        output.accept(ModBlocks.STRIPPED_SCULK_WOOD);
                        output.accept(ModBlocks.SCULK_PLANKS);
                        output.accept(ModBlocks.SCULK_LEAVES);
                        output.accept(ModBlocks.ASHEN_LOG);
                        output.accept(ModBlocks.STRIPPED_ASHEN_LOG);
                        output.accept(ModBlocks.ASHEN_WOOD);
                        output.accept(ModBlocks.STRIPPED_ASHEN_WOOD);
                        output.accept(ModBlocks.STRIPPED_CHARRED_LOG);
                        output.accept(ModBlocks.STRIPPED_CHARRED_WOOD);
                        output.accept(ModBlocks.ASHEN_LEAVES);
                        output.accept(ModBlocks.ASHEN_PLANKS);
                        output.accept(ModBlocks.ASHEN_STAIRS);
                        output.accept(ModBlocks.ASHEN_SLAB);
                        output.accept(ModBlocks.ASHEN_FENCE);
                        output.accept(ModBlocks.ASHEN_FENCE_GATE);
                        output.accept(ModBlocks.ASHEN_BUTTON);
                        output.accept(ModBlocks.ASHEN_PRESSURE_PLATE);
                        output.accept(ModBlocks.ASHEN_DOOR);
                        output.accept(ModBlocks.ASHEN_TRAPDOOR);
                        output.accept(ModBlocks.ASHEN_CABINET);
                        output.accept(ModBlocks.CHARRED_PLANKS);
                        output.accept(ModBlocks.CHARRED_STAIRS);
                        output.accept(ModBlocks.CHARRED_SLAB);
                        output.accept(ModBlocks.CHARRED_FENCE);
                        output.accept(ModBlocks.CHARRED_FENCE_GATE);
                        output.accept(ModBlocks.CHARRED_BUTTON);
                        output.accept(ModBlocks.CHARRED_PRESSURE_PLATE);
                        output.accept(ModBlocks.CHARRED_DOOR);
                        output.accept(ModBlocks.CHARRED_TRAPDOOR);
                        output.accept(ModBlocks.CHARRED_CABINET);
                        output.accept(ModBlocks.OAK_SCRAP_WOOD);
                        output.accept(ModBlocks.SPRUCE_SCRAP_WOOD);
                        output.accept(ModBlocks.ASHEN_SCRAP_WOOD);
                        output.accept(ModBlocks.CHARRED_SCRAP_WOOD);
                        // Земля и растения
                        output.accept(ModBlocks.INCINERATED_DIRT);
                        output.accept(ModBlocks.INCINERATED_FARMLAND);
                        output.accept(ModBlocks.SUSPICIOUS_INCINERATED_DIRT);
                        output.accept(ModBlocks.ASHEN_GRASS);
                        output.accept(ModBlocks.SCULK_SPROUT);
                        output.accept(ModBlocks.SCULK_MUSHROOM);
                        // Опалённый камень, кирпичи и плитка (у каждого вида своя ступень, плита и стена, кроме потрескавшейся плитки)
                        output.accept(ModBlocks.SINGE_STONE);
                        output.accept(ModBlocks.SINGE_STONE_STAIRS);
                        output.accept(ModBlocks.SINGE_STONE_SLAB);
                        output.accept(ModBlocks.SINGE_STONE_WALL);
                        output.accept(ModBlocks.SINGE_COBBLESTONE);
                        output.accept(ModBlocks.SINGE_COBBLESTONE_STAIRS);
                        output.accept(ModBlocks.SINGE_COBBLESTONE_SLAB);
                        output.accept(ModBlocks.SINGE_COBBLESTONE_WALL);
                        output.accept(ModBlocks.SINGE_STONE_BRICKS);
                        output.accept(ModBlocks.SINGE_STONE_BRICK_STAIRS);
                        output.accept(ModBlocks.SINGE_STONE_BRICK_SLAB);
                        output.accept(ModBlocks.SINGE_STONE_BRICK_WALL);
                        output.accept(ModBlocks.MOSSY_SINGE_STONE_BRICKS);
                        output.accept(ModBlocks.MOSSY_SINGE_STONE_BRICK_STAIRS);
                        output.accept(ModBlocks.MOSSY_SINGE_STONE_BRICK_SLAB);
                        output.accept(ModBlocks.MOSSY_SINGE_STONE_BRICK_WALL);
                        output.accept(ModBlocks.CRACKED_SINGE_STONE_BRICKS);
                        output.accept(ModBlocks.CRACKED_SINGE_STONE_BRICK_STAIRS);
                        output.accept(ModBlocks.CRACKED_SINGE_STONE_BRICK_SLAB);
                        output.accept(ModBlocks.CRACKED_SINGE_STONE_BRICK_WALL);
                        output.accept(ModBlocks.RED_MOSSY_SINGE_STONE_BRICKS);
                        output.accept(ModBlocks.CHISELED_SINGE_STONE);
                        output.accept(ModBlocks.SINGE_STONE_PILLAR);
                        output.accept(ModBlocks.WORN_SINGE_STONE_PILLAR);
                        output.accept(ModBlocks.SINGE_STONE_PILLAR_IONIC);
                        output.accept(ModBlocks.SINGE_STONE_BRICK_TILES);
                        output.accept(ModBlocks.SINGE_STONE_BRICK_TILE_STAIRS);
                        output.accept(ModBlocks.SINGE_STONE_BRICK_TILE_SLAB);
                        output.accept(ModBlocks.SINGE_STONE_BRICK_TILE_WALL);
                        output.accept(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILES);
                        output.accept(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_STAIRS);
                        output.accept(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_SLAB);
                        output.accept(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_WALL);
                        output.accept(ModBlocks.CRACKED_SINGE_STONE_BRICK_TILES);
                        output.accept(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILES);
                        output.accept(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_STAIRS);
                        output.accept(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_SLAB);
                        output.accept(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_WALL);
                        output.accept(ModBlocks.BLOODY_RUNESTONE);
                        output.accept(ModBlocks.SPIRITUAL_RUNESTONE);
                        // Прочие блоки
                        output.accept(ModBlocks.SNOW_BRICKS);
                        output.accept(ModBlocks.PACKED_ICE_BRICKS);
                        output.accept(ModBlocks.SINGED_COAL_ORE);
                        output.accept(ModBlocks.SINGED_IRON_ORE);
                        output.accept(ModBlocks.SINGED_COPPER_ORE);
                        output.accept(ModBlocks.SINGED_LAPIS_ORE);
                    }).build());

                        public static final Supplier<CreativeModeTab> WTAB = CREATIVE_MODE_TAB.register("wtab",
                                () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.AQUATIC_TOPAZ.get()))
                                        .title(Component.translatable("wtab.veridia.wtab"))
                                        .displayItems((itemDisplayParameters, output) -> {
                                            output.accept(ModItems.WITHER_BONE);
                                            output.accept(ModItems.BRAIN);
                                            output.accept(ModItems.INFESTED_BRAIN);
                                            output.accept(ModItems.PRISMARINE_C_POWDER);
                                            output.accept(ModItems.NAUTILUS_GLAND);
                                            output.accept(ModItems.NETHER_STRING);
                                            output.accept(ModItems.CHAMOTTE_BRICK);
                                            output.accept(ModItems.AQUATIC_STRING);
                                            output.accept(ModBlocks.MYTHICAL_TABLE);
                                            output.accept(ModBlocks.PURPURITE_ORE);
                                            output.accept(ModBlocks.VERDANT_FLUORITE_ORE);
                                            output.accept(ModItems.PURPURITE);
                                            output.accept(ModItems.R_PURPURITE);
                                            output.accept(ModItems.KNOCKBACK_RING);
                                            output.accept(ModItems.KNOCKBACK_RING_2);
                                            output.accept(ModItems.KNOCKBACK_RING_3);
                                            output.accept(ModItems.KNOCKBACK_RING_4);
                                            output.accept(ModItems.KNOCKBACK_RING_5);
                                            output.accept(ModItems.NEURON_CLUMP);
                                            output.accept(ModItems.ANCIENT_DOCUMENT);
                                            output.accept(ModItems.AQUATIC_TOPAZ);
                                            output.accept(ModItems.R_AQUATIC_TOPAZ);
                                            output.accept(ModItems.AQUATIC_AMULET);
                                            output.accept(ModItems.AQUATIC_AMULET_2);
                                            output.accept(ModItems.AQUATIC_AMULET_3);
                                            output.accept(ModItems.AQUATIC_AMULET_4);
                                            output.accept(ModItems.AQUATIC_AMULET_5);
                                            output.accept(ModItems.PLASMAFITE);
                                            output.accept(ModItems.SCORCHING_PLASMAFITE);
                                            output.accept(ModItems.EYE_OF_SAURON);
                                            output.accept(ModItems.EYE_OF_SAURON_2);
                                            output.accept(ModItems.EYE_OF_SAURON_3);
                                            output.accept(ModItems.EYE_OF_SAURON_4);
                                            output.accept(ModItems.EYE_OF_SAURON_5);
                                            output.accept(ModItems.R_VERDANT_FLUORITE);
                                            output.accept(ModItems.VERDANT_FLUORITE);
                                            output.accept(ModItems.D_VERDANT_FLUORITE);
                                            output.accept(ModBlocks.PROFUNDUSTONE.get());
                                            output.accept(ModBlocks.BLACKSTONE_MAGMA.get());
                                            output.accept(ModItems.PROFUNDUSTONE_CHUNK);
                                            output.accept(ModItems.SLOW_KILLER);
                                            output.accept(ModItems.SLOW_KILLER_2);
                                            output.accept(ModItems.SLOW_KILLER_3);
                                            output.accept(ModItems.SLOW_KILLER_4);
                                            output.accept(ModItems.SLOW_KILLER_5);
                                            output.accept(ModItems.VERIDITE);
                                            output.accept(ModItems.VERIDITE_PICKAXE);
                                            output.accept(ModItems.SMELTING_PICKAXE);
                                            output.accept(ModItems.CRUDE_HAMMER);
                                            output.accept(ModItems.CRUDE_KNIFE);
                                            output.accept(ModItems.SS_KNIFE);
                                            output.accept(ModItems.HEAVY_PROJECTILE);
                                            output.accept(ModItems.ELECTRIC_SWORD);
                                            output.accept(ModItems.OMEN_REPRISE_DISK);
                                            output.accept(ModItems.SLOT_POCKET);
                                            output.accept(ModItems.LIMITED_PASS);
                                            output.accept(ModItems.ETERNAL_PASS);
                                            output.accept(ModItems.HEALTH_POWDER);
                                            output.accept(ModItems.JOB_APPLICATION);
                                            output.accept(ModItems.CREATIVE_BONE_MEAL);
                                            output.accept(ModItems.FIRING_CONTRACT);
                                            output.accept(ModItems.CREATIVE_NAILER_WAND);

                                            output.accept(ModItems.VERIDITE_HELMET);
                                            output.accept(ModItems.VERIDITE_CHESTPLATE);
                                            output.accept(ModItems.VERIDITE_LEGGINGS);
                                            output.accept(ModItems.VERIDITE_BOOTS);
                                            output.accept(ModItems.BLINDING_PRISM);
                                            output.accept(ModItems.BLOODSTONE);
                                            output.accept(ModItems.INFERNAL_STAFF);
                                            output.accept(ModItems.INFERNAL_STAFF_2);
                                            output.accept(ModItems.INFERNAL_STAFF_3);
                                            output.accept(ModItems.INFERNAL_STAFF_4);
                                            output.accept(ModItems.INFERNAL_STAFF_5);

                                            output.accept(ModItems.RAW_BARIUM);
                                            output.accept(ModItems.BARIUM_INGOT);
                                            output.accept(ModItems.BARIUM_POWDER);
                                            output.accept(ModBlocks.RAW_MOLYBDENUM_BLOCK);
                                            output.accept(ModItems.MOLYBDENUM_SCRAP);
                                            output.accept(ModItems.MOLYBDENUM_INGOT);
                                            output.accept(ModItems.MOLYBDENUM_NUGGET);
                                            output.accept(ModItems.MOLYBDENUM_SHEET);
                                            output.accept(ModBlocks.MOLYBDENUM_BLOCK);
                                            output.accept(ModBlocks.MOLYBDENUM_BARS);
                                            output.accept(ModItems.MOLYBDENUM_HACKSAW);
                                            output.accept(ModItems.DISMAL_CORE);
                                            output.accept(ModItems.QUARTZ_POWDER);

                                            output.accept(ModBlocks.EXTRACTOR);
                                            output.accept(ModItems.ALCHEMIST_UPGRADE);
                                            output.accept(ModItems.WARRIOR_UPGRADE);
                                            output.accept(ModItems.WORKER_UPGRADE);

                                            output.accept(ModItems.NEWBIE_MANTLE_HELMET);
                                            output.accept(ModItems.NEWBIE_MANTLE_CHESTPLATE);
                                            output.accept(ModItems.NEWBIE_MANTLE_BOOTS);
                                            output.accept(ModItems.EXPERIENCED_MANTLE_HELMET);
                                            output.accept(ModItems.EXPERIENCED_MANTLE_CHESTPLATE);
                                            output.accept(ModItems.EXPERIENCED_MANTLE_BOOTS);


                                            output.accept(ModItems.DOUBLE_COPPER_INGOT);
                                            output.accept(ModItems.DOUBLE_IRON_INGOT);
                                            output.accept(ModItems.DOUBLE_GOLD_INGOT);
                                            output.accept(ModItems.DOUBLE_DIAMOND);
                                            output.accept(ModItems.QUAD_COPPER_INGOT);
                                            output.accept(ModItems.QUAD_IRON_INGOT);
                                            output.accept(ModItems.QUAD_GOLD_INGOT);
                                            output.accept(ModItems.QUAD_DIAMOND);

                                            output.accept(ModItems.MANA_AWAKENING_STONE);
                                            output.accept(ModItems.SUSPICIOUS_SOUP);
                                            output.accept(ModItems.NETHER_DELICACY);
                                            output.accept(ModBlocks.RUNIC_PEDESTAL);

                                        }).build());

    public static final Supplier<CreativeModeTab> SUMMONS_RUNES_TAB = CREATIVE_MODE_TAB.register("summons_runes",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.MUTANT_SPAWN_EGG.get()))
                    .title(Component.translatable("tab.veridia.summons_runes"))
                    .displayItems((parameters, output) ->
                            SummonsRunesCategory.active.emit(output::accept))
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
