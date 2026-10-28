package net.gera.veridia.common.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        // Молибденовая ножовка: рамка из слитков, ручка из палки
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MOLYBDENUM_HACKSAW.get())
                .pattern("III")
                .pattern("I S")
                .define('I', ModItems.MOLYBDENUM_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_molybdenum_ingot", has(ModItems.MOLYBDENUM_INGOT)).save(recipeOutput);
        molybdenumRecipes(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PLANT_CLUMP_BLOCK.get())
                .pattern("AA")
                .pattern("AA")
                .define('A', ModItems.PLANT_CLUMP.get())
                .unlockedBy("has_clump", has(ModItems.PLANT_CLUMP)).save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PLANT_STRING.get())
                .pattern("AA")
                .pattern("A ")
                .define('A', ModItems.PLANT_CLUMP.get())
                .unlockedBy("has_clump", has(ModItems.PLANT_CLUMP)).save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CRUDE_HAMMER.get())
                .pattern(" BB")
                .pattern("ACB")
                .pattern("CA ")
                .define('A', ModItems.PLANT_STRING.get())
                .define('C', Items.STICK)
                .define('B', Items.COBBLESTONE)
                .unlockedBy("has_clump", has(ModItems.PLANT_CLUMP)).save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CRUDE_KNIFE.get())
                .pattern(" B ")
                .pattern("ACA")
                .pattern(" D ")
                .define('A', ModItems.PLANT_STRING.get())
                .define('C', Items.STICK)
                .define('B', Items.COBBLESTONE)
                .define('D', ModBlocks.PLANT_CLUMP_BLOCK.get())
                .unlockedBy("has_clump", has(ModItems.PLANT_CLUMP)).save(recipeOutput);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HEAVY_PROJECTILE.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" D ")
                .define('A', ModItems.FIBER_NET.get())
                .define('B', Items.LEATHER)
                .define('C', Items.COPPER_BLOCK)
                .define('D', Items.STRING)
                .unlockedBy("has_net", has(ModItems.FIBER_NET)).save(recipeOutput);
        //SHAPELESS
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.JOB_APPLICATION.get(), 1)
                .requires(ModItems.JOB_APPLICATION)
                .unlockedBy("has_clump_block", has(ModItems.JOB_APPLICATION)).save(recipeOutput, "veridia:jobapp");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.FIRING_CONTRACT.get(), 1)
                .requires(ModItems.FIRING_CONTRACT)
                .unlockedBy("has_clump_block", has(ModItems.FIRING_CONTRACT)).save(recipeOutput, "veridia:firing");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PLANT_CLUMP.get(), 4)
                .requires(ModBlocks.PLANT_CLUMP_BLOCK)
                .unlockedBy("has_clump_block", has(ModBlocks.PLANT_CLUMP_BLOCK)).save(recipeOutput, "veridia:clump_block_to_clump_1");
        stairBuilder(ModBlocks.SCULK_STAIRS.get(), Ingredient.of(ModBlocks.SCULK_PLANKS)).group("sculk_planks")
                .unlockedBy("sculk_planks", has(ModBlocks.SCULK_PLANKS)).save(recipeOutput);

        slab(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SCULK_SLAB.get(), ModBlocks.SCULK_PLANKS.get());

        buttonBuilder(ModBlocks.SCULK_BUTTON.get(), Ingredient.of(ModBlocks.SCULK_PLANKS.get())).group("sculk_planks")
                .unlockedBy("sculk_planks", has(ModBlocks.SCULK_PLANKS.get())).save(recipeOutput);

        pressurePlate(recipeOutput, ModBlocks.SCULK_PRESSURE_PLATE.get(), ModBlocks.SCULK_PLANKS.get());

        wall(recipeOutput, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SCULK_WALL.get(), ModBlocks.SCULK_PLANKS.get());

        doorBuilder(ModBlocks.SCULK_DOOR.get(), Ingredient.of(ModBlocks.SCULK_PLANKS.get())).group("sculk_planks")
                .unlockedBy("sculk_planks", has(ModBlocks.SCULK_PLANKS.get())).save(recipeOutput);

        trapdoorBuilder(ModBlocks.SCULK_TRAPDOOR.get(), Ingredient.of(ModBlocks.SCULK_PLANKS.get())).group("sculk_planks")
                .unlockedBy("sculk_planks", has(ModBlocks.SCULK_PLANKS.get())).save(recipeOutput);
        singeStoneRecipes(recipeOutput);
        charredPlanksRecipes(recipeOutput);
        ashenWoodRecipes(recipeOutput);
    }
    // Пепельное дерево, как у ванильного: 4 доски из любого бревна, древесина из 4 брёвен (3 штуки) и то же очищенное;
    // из досок дверь и люк. Обожжённые доски, бревно и древесина получаются только выплавкой пепельных
    private void ashenWoodRecipes(RecipeOutput out) {
        planksFromLogs(out, ModBlocks.ASHEN_PLANKS.get(), ModTags.Items.ASHEN_LOGS, 4);
        woodFromLogs(out, ModBlocks.ASHEN_WOOD.get(), ModBlocks.ASHEN_LOG.get());
        woodFromLogs(out, ModBlocks.STRIPPED_ASHEN_WOOD.get(), ModBlocks.STRIPPED_ASHEN_LOG.get());
        woodFromLogs(out, ModBlocks.STRIPPED_CHARRED_WOOD.get(), ModBlocks.STRIPPED_CHARRED_LOG.get());

        woodenSetRecipes(out, ModBlocks.ASHEN_PLANKS.get(), ModBlocks.ASHEN_STAIRS.get(), ModBlocks.ASHEN_SLAB.get(),
                ModBlocks.ASHEN_FENCE.get(), ModBlocks.ASHEN_FENCE_GATE.get(), ModBlocks.ASHEN_BUTTON.get(),
                ModBlocks.ASHEN_PRESSURE_PLATE.get(), ModBlocks.ASHEN_DOOR.get(), ModBlocks.ASHEN_TRAPDOOR.get());

        cabinet(out, ModBlocks.ASHEN_CABINET.get(), ModBlocks.ASHEN_SLAB.get(), ModBlocks.ASHEN_TRAPDOOR.get());
        cabinet(out, ModBlocks.CHARRED_CABINET.get(), ModBlocks.CHARRED_SLAB.get(), ModBlocks.CHARRED_TRAPDOOR.get());

        smeltAshen(out, Ingredient.of(ModBlocks.ASHEN_PLANKS.get()), ModBlocks.CHARRED_PLANKS.get());
        smeltAshen(out, Ingredient.of(ModBlocks.ASHEN_LOG.get(), ModBlocks.STRIPPED_ASHEN_LOG.get()),
                ModBlocks.STRIPPED_CHARRED_LOG.get());
        smeltAshen(out, Ingredient.of(ModBlocks.ASHEN_WOOD.get(), ModBlocks.STRIPPED_ASHEN_WOOD.get()),
                ModBlocks.STRIPPED_CHARRED_WOOD.get());
    }

    // Кладовая, как у Farmer's Delight: плиты сверху и снизу, два люка по бокам
    private void cabinet(RecipeOutput out, ItemLike cabinet, ItemLike slab, ItemLike trapdoor) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, cabinet)
                .define('_', slab).define('D', trapdoor)
                .pattern("___").pattern("D D").pattern("___")
                .group("cabinet")
                .unlockedBy(getHasName(trapdoor), has(trapdoor)).save(out);
    }

    // Выплавка пепельного дерева в обожжённое (печь, как у досок и брёвен: 200 тиков)
    private void smeltAshen(RecipeOutput out, Ingredient ashen, ItemLike charred) {
        SimpleCookingRecipeBuilder.smelting(ashen, RecipeCategory.BUILDING_BLOCKS, charred, 0.1F, 200)
                .unlockedBy("has_ashen_wood", has(ModTags.Items.ASHEN_LOGS))
                .save(out, Veridia.MOD_ID + ":" + getItemName(charred) + "_from_smelting");
    }
    // Деревянный набор обожжённых досок: рецепты те же, что у дубовых (ступени 4 из 6, плита 6 из 3,
    // забор и калитка с палками, кнопка из 1, плита нажимная из 2)
    private void charredPlanksRecipes(RecipeOutput out) {
        woodenSetRecipes(out, ModBlocks.CHARRED_PLANKS.get(), ModBlocks.CHARRED_STAIRS.get(), ModBlocks.CHARRED_SLAB.get(),
                ModBlocks.CHARRED_FENCE.get(), ModBlocks.CHARRED_FENCE_GATE.get(), ModBlocks.CHARRED_BUTTON.get(),
                ModBlocks.CHARRED_PRESSURE_PLATE.get(), ModBlocks.CHARRED_DOOR.get(), ModBlocks.CHARRED_TRAPDOOR.get());
    }

    // Набор из досок: ступени, плита, забор, калитка, кнопка, нажимная плита, дверь и люк
    private void woodenSetRecipes(RecipeOutput out, ItemLike planks, ItemLike stairs, ItemLike slab, ItemLike fence,
                                  ItemLike fenceGate, ItemLike button, ItemLike pressurePlate, ItemLike door, ItemLike trapdoor) {
        stairBuilder(stairs, Ingredient.of(planks)).group("wooden_stairs")
                .unlockedBy(getHasName(planks), has(planks)).save(out);
        slab(out, RecipeCategory.BUILDING_BLOCKS, slab, planks);
        fenceBuilder(fence, Ingredient.of(planks)).group("wooden_fence")
                .unlockedBy(getHasName(planks), has(planks)).save(out);
        fenceGateBuilder(fenceGate, Ingredient.of(planks)).group("wooden_fence_gate")
                .unlockedBy(getHasName(planks), has(planks)).save(out);
        buttonBuilder(button, Ingredient.of(planks)).group("wooden_button")
                .unlockedBy(getHasName(planks), has(planks)).save(out);
        pressurePlate(out, pressurePlate, planks);
        doorBuilder(door, Ingredient.of(planks)).group("wooden_door")
                .unlockedBy(getHasName(planks), has(planks)).save(out);
        trapdoorBuilder(trapdoor, Ingredient.of(planks)).group("wooden_trapdoor")
                .unlockedBy(getHasName(planks), has(planks)).save(out);
    }

    // Молибден, как железо: 9 слитков <-> блок, 9 самородков <-> слиток, решётка 16 штук из 6 слитков.
    // Лист - прессом Create (data/veridia/recipe/pressing/molybdenum_sheet.json), обычным крафтом его нет
    private void molybdenumRecipes(RecipeOutput out) {
        ItemLike ingot = ModItems.MOLYBDENUM_INGOT.get();
        ItemLike nugget = ModItems.MOLYBDENUM_NUGGET.get();
        ItemLike block = ModBlocks.MOLYBDENUM_BLOCK.get();
        ninePack(out, RecipeCategory.BUILDING_BLOCKS, block, ingot, null, "molybdenum_ingot_from_molybdenum_block");
        ninePack(out, RecipeCategory.MISC, ingot, nugget, "molybdenum_ingot_from_nuggets", null);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.MOLYBDENUM_BARS.get(), 16)
                .define('#', ingot).pattern("###").pattern("###")
                .unlockedBy(getHasName(ingot), has(ingot)).save(out);
    }

    // 9 штук в один (3x3) и обратно. Имя рецепта: null - по названию предмета; своё - где два рецепта дают один
    // предмет (слиток из блока и слиток из самородков). Своё имя, равное имени по умолчанию, datagen не принимает
    private void ninePack(RecipeOutput out, RecipeCategory category, ItemLike packed, ItemLike unpacked,
                          @Nullable String packName, @Nullable String unpackName) {
        save(out, ShapedRecipeBuilder.shaped(category, packed)
                .define('#', unpacked).pattern("###").pattern("###").pattern("###")
                .unlockedBy(getHasName(unpacked), has(unpacked)), packName);
        save(out, ShapelessRecipeBuilder.shapeless(category, unpacked, 9)
                .requires(packed)
                .unlockedBy(getHasName(packed), has(packed)), unpackName);
    }

    private static void save(RecipeOutput out, RecipeBuilder builder, @Nullable String name) {
        if (name == null) {
            builder.save(out);
        } else {
            builder.save(out, Veridia.MOD_ID + ":" + name);
        }
    }

    // Рецепты опалённых кирпичей, как у ванильных каменных: кирпичи 2x2 из опалённого камня, замшелые с мхом или
    // лозой, потрескавшиеся выплавкой, резной блок из двух плит; ступени/плиты/стены крафтом и в камнерезе.
    private void singeStoneRecipes(RecipeOutput out) {
        ItemLike stone = ModBlocks.SINGE_STONE.get();
        ItemLike bricks = ModBlocks.SINGE_STONE_BRICKS.get();
        ItemLike mossy = ModBlocks.MOSSY_SINGE_STONE_BRICKS.get();
        ItemLike cracked = ModBlocks.CRACKED_SINGE_STONE_BRICKS.get();
        ItemLike chiseled = ModBlocks.CHISELED_SINGE_STONE.get();

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, bricks, 4)
                .define('#', stone).pattern("##").pattern("##")
                .unlockedBy(getHasName(stone), has(stone)).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, mossy)
                .requires(bricks).requires(Items.MOSS_BLOCK)
                .unlockedBy(getHasName(bricks), has(bricks))
                .save(out, Veridia.MOD_ID + ":mossy_singe_stone_bricks_from_moss_block");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, mossy)
                .requires(bricks).requires(Items.VINE)
                .unlockedBy(getHasName(bricks), has(bricks))
                .save(out, Veridia.MOD_ID + ":mossy_singe_stone_bricks_from_vine");
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(bricks), RecipeCategory.BUILDING_BLOCKS, cracked, 0.1F, 200)
                .unlockedBy(getHasName(bricks), has(bricks)).save(out);

        brickFamilyRecipes(out, stone, ModBlocks.SINGE_STONE_STAIRS.get(),
                ModBlocks.SINGE_STONE_SLAB.get(), ModBlocks.SINGE_STONE_WALL.get());

        // Опалённый булыжник: выплавляется обратно в опалённый камень (как булыжник в камень), свои ступени/плита/стена
        ItemLike cobblestone = ModBlocks.SINGE_COBBLESTONE.get();
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(cobblestone), RecipeCategory.BUILDING_BLOCKS, stone, 0.1F, 200)
                .unlockedBy(getHasName(cobblestone), has(cobblestone))
                .save(out, Veridia.MOD_ID + ":singe_stone_from_smelting_singe_cobblestone");
        brickFamilyRecipes(out, cobblestone, ModBlocks.SINGE_COBBLESTONE_STAIRS.get(),
                ModBlocks.SINGE_COBBLESTONE_SLAB.get(), ModBlocks.SINGE_COBBLESTONE_WALL.get());
        brickFamilyRecipes(out, bricks, ModBlocks.SINGE_STONE_BRICK_STAIRS.get(),
                ModBlocks.SINGE_STONE_BRICK_SLAB.get(), ModBlocks.SINGE_STONE_BRICK_WALL.get());
        brickFamilyRecipes(out, mossy, ModBlocks.MOSSY_SINGE_STONE_BRICK_STAIRS.get(),
                ModBlocks.MOSSY_SINGE_STONE_BRICK_SLAB.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_WALL.get());
        brickFamilyRecipes(out, cracked, ModBlocks.CRACKED_SINGE_STONE_BRICK_STAIRS.get(),
                ModBlocks.CRACKED_SINGE_STONE_BRICK_SLAB.get(), ModBlocks.CRACKED_SINGE_STONE_BRICK_WALL.get());

        // Плитка: 4 из 2x2 кирпичей своего вида (как у глубинного сланца) и в камнерезе
        tilesRecipes(out, bricks, ModBlocks.SINGE_STONE_BRICK_TILES.get());
        tilesRecipes(out, cracked, ModBlocks.CRACKED_SINGE_STONE_BRICK_TILES.get());
        tilesRecipes(out, mossy, ModBlocks.MOSSY_SINGE_STONE_BRICK_TILES.get());
        tilesRecipes(out, ModBlocks.RED_MOSSY_SINGE_STONE_BRICKS.get(), ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILES.get());
        stonecutting(out, stone, ModBlocks.SINGE_STONE_BRICK_TILES.get(), 1);
        brickFamilyRecipes(out, ModBlocks.SINGE_STONE_BRICK_TILES.get(), ModBlocks.SINGE_STONE_BRICK_TILE_STAIRS.get(),
                ModBlocks.SINGE_STONE_BRICK_TILE_SLAB.get(), ModBlocks.SINGE_STONE_BRICK_TILE_WALL.get());
        brickFamilyRecipes(out, ModBlocks.MOSSY_SINGE_STONE_BRICK_TILES.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_STAIRS.get(),
                ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_SLAB.get(), ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_WALL.get());
        brickFamilyRecipes(out, ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILES.get(), ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_STAIRS.get(),
                ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_SLAB.get(), ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_WALL.get());


        ItemLike brickSlab = ModBlocks.SINGE_STONE_BRICK_SLAB.get();
        chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, chiseled, Ingredient.of(brickSlab))
                .unlockedBy(getHasName(brickSlab), has(brickSlab)).save(out);

        // Камнерез: из опалённого камня сразу кирпичи и резной блок, из кирпичей резной блок
        stonecutting(out, stone, bricks, 1);
        stonecutting(out, stone, chiseled, 1);
        stonecutting(out, bricks, chiseled, 1);
        stonecutting(out, stone, ModBlocks.SINGE_STONE_BRICK_STAIRS.get(), 1);
        stonecutting(out, stone, ModBlocks.SINGE_STONE_BRICK_SLAB.get(), 2);
        stonecutting(out, stone, ModBlocks.SINGE_STONE_BRICK_WALL.get(), 1);

        // Колонны: 2 из двух кирпичей в столбик (как кварцевая колонна) и в камнерезе; потёртая - выплавкой,
        // капитель - 2 из колонны в камнерезе
        ItemLike pillar = ModBlocks.SINGE_STONE_PILLAR.get();
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, pillar, 2)
                .define('#', bricks).pattern("#").pattern("#")
                .unlockedBy(getHasName(bricks), has(bricks)).save(out);
        stonecutting(out, stone, pillar, 1);
        stonecutting(out, bricks, pillar, 1);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(pillar), RecipeCategory.BUILDING_BLOCKS,
                        ModBlocks.WORN_SINGE_STONE_PILLAR.get(), 0.1F, 200)
                .unlockedBy(getHasName(pillar), has(pillar)).save(out);
        stonecutting(out, pillar, ModBlocks.SINGE_STONE_PILLAR_IONIC.get(), 2);
    }

    private void tilesRecipes(RecipeOutput out, ItemLike bricks, ItemLike tiles) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, tiles, 4)
                .define('#', bricks).pattern("##").pattern("##")
                .unlockedBy(getHasName(bricks), has(bricks)).save(out);
        stonecutting(out, bricks, tiles, 1);
    }

    // Ступени (4 из 6 блоков), плита (6 из 3), стена (6 из 6) крафтом и из блока в камнерезе
    private void brickFamilyRecipes(RecipeOutput out, ItemLike base, ItemLike stairs, ItemLike slab, ItemLike wall) {
        stairBuilder(stairs, Ingredient.of(base)).unlockedBy(getHasName(base), has(base)).save(out);
        slab(out, RecipeCategory.BUILDING_BLOCKS, slab, base);
        wall(out, RecipeCategory.BUILDING_BLOCKS, wall, base);
        stonecutting(out, base, stairs, 1);
        stonecutting(out, base, slab, 2);
        stonecutting(out, base, wall, 1);
    }

    // Id задаём сами, с нашим пространством имён: у ванильного stonecutterResultFromBase оно minecraft
    private static void stonecutting(RecipeOutput out, ItemLike base, ItemLike result, int count) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(base), RecipeCategory.BUILDING_BLOCKS, result, count)
                .unlockedBy(getHasName(base), has(base))
                .save(out, Veridia.MOD_ID + ":" + getItemName(result) + "_from_" + getItemName(base) + "_stonecutting");
    }

    //helper methods

    protected static void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory
                                              pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory
                                              pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTime, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput
                                                                               recipeOutput, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> factory,
                                                                       List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience,
                                                                       int pCookingTime, String pGroup, String pRecipeName) {
        for (ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(recipeOutput, Veridia.MOD_ID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }
}