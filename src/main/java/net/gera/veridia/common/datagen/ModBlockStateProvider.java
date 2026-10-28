package net.gera.veridia.common.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.CabinetBlock;
import net.gera.veridia.common.block.FoodStorageBlock;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.bushes.BlueBerryBush;
import net.gera.veridia.common.block.bushes.CumaCumaBush;
import net.gera.veridia.common.block.bushes.CumaCumaBushBlock;
import net.gera.veridia.common.block.bushes.CumaCumaBushSprout;
import net.gera.veridia.common.block.crop.BellPepperCrop;
import net.gera.veridia.common.block.crop.HempCrop;
import net.gera.veridia.common.block.crop.GarlicCrop;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.gera.veridia.common.block.BloodyRunestoneBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.ModelProvider;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;
import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Veridia.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.PLANT_CLUMP_BLOCK);
        logBlock(((RotatedPillarBlock) ModBlocks.SCULK_LOG.get()));
        axisBlock(((RotatedPillarBlock) ModBlocks.SCULK_WOOD.get()), blockTexture(ModBlocks.SCULK_LOG.get()), blockTexture(ModBlocks.SCULK_LOG.get()));

        logBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_SCULK_LOG.get()));
        axisBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_SCULK_WOOD.get()), blockTexture(ModBlocks.STRIPPED_SCULK_LOG.get()), blockTexture(ModBlocks.STRIPPED_SCULK_LOG.get()));

        blockItem(ModBlocks.SCULK_LOG);
        blockItem(ModBlocks.STRIPPED_SCULK_LOG);
        blockItem(ModBlocks.SCULK_WOOD);
        blockItem(ModBlocks.STRIPPED_SCULK_WOOD);
        blockWithItem(ModBlocks.PURPURITE_ORE);
        blockWithItem(ModBlocks.VERDANT_FLUORITE_ORE);
        blockWithItem(ModBlocks.NETHER_BARIUM_ORE);
        blockWithItem(ModBlocks.PROFUNDUSTONE);
        blockWithItem(ModBlocks.INCINERATED_DIRT);
        suspiciousBlock(ModBlocks.SUSPICIOUS_INCINERATED_DIRT);
        blockWithItem(ModBlocks.SINGE_STONE);
stairsBlock(ModBlocks.SINGE_STONE_STAIRS.get(), blockTexture(ModBlocks.SINGE_STONE.get()));        slabBlock(ModBlocks.SINGE_STONE_SLAB.get(), blockTexture(ModBlocks.SINGE_STONE.get()), blockTexture(ModBlocks.SINGE_STONE.get()));        wallBlock(ModBlocks.SINGE_STONE_WALL.get(), blockTexture(ModBlocks.SINGE_STONE.get()));        blockItem(ModBlocks.SINGE_STONE_STAIRS);        blockItem(ModBlocks.SINGE_STONE_SLAB);
        stoneBrickFamily(ModBlocks.SINGE_COBBLESTONE, ModBlocks.SINGE_COBBLESTONE_STAIRS,
                ModBlocks.SINGE_COBBLESTONE_SLAB, ModBlocks.SINGE_COBBLESTONE_WALL);
        blockWithItem(ModBlocks.SNOW_BRICKS);
        blockWithItem(ModBlocks.PACKED_ICE_BRICKS);
        blockWithItem(ModBlocks.SINGED_COAL_ORE);
        blockWithItem(ModBlocks.SINGED_IRON_ORE);
        blockWithItem(ModBlocks.SINGED_COPPER_ORE);
        blockWithItem(ModBlocks.SINGED_LAPIS_ORE);
        stoneBrickFamily(ModBlocks.SINGE_STONE_BRICKS, ModBlocks.SINGE_STONE_BRICK_STAIRS,
                ModBlocks.SINGE_STONE_BRICK_SLAB, ModBlocks.SINGE_STONE_BRICK_WALL);
        stoneBrickFamily(ModBlocks.MOSSY_SINGE_STONE_BRICKS, ModBlocks.MOSSY_SINGE_STONE_BRICK_STAIRS,
                ModBlocks.MOSSY_SINGE_STONE_BRICK_SLAB, ModBlocks.MOSSY_SINGE_STONE_BRICK_WALL);
        stoneBrickFamily(ModBlocks.CRACKED_SINGE_STONE_BRICKS, ModBlocks.CRACKED_SINGE_STONE_BRICK_STAIRS,
                ModBlocks.CRACKED_SINGE_STONE_BRICK_SLAB, ModBlocks.CRACKED_SINGE_STONE_BRICK_WALL);
        blockWithItem(ModBlocks.CHISELED_SINGE_STONE);
        singeStonePillars();
        blockWithItem(ModBlocks.RED_MOSSY_SINGE_STONE_BRICKS);
        blockWithItem(ModBlocks.SINGE_STONE_BRICK_TILES);
        blockWithItem(ModBlocks.CRACKED_SINGE_STONE_BRICK_TILES);
        blockWithItem(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILES);
        blockWithItem(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILES);
        tileFamily(ModBlocks.SINGE_STONE_BRICK_TILES, ModBlocks.SINGE_STONE_BRICK_TILE_STAIRS, ModBlocks.SINGE_STONE_BRICK_TILE_SLAB, ModBlocks.SINGE_STONE_BRICK_TILE_WALL);
        tileFamily(ModBlocks.MOSSY_SINGE_STONE_BRICK_TILES, ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_STAIRS, ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_SLAB, ModBlocks.MOSSY_SINGE_STONE_BRICK_TILE_WALL);
        tileFamily(ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILES, ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_STAIRS, ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_SLAB, ModBlocks.RED_MOSSY_SINGE_STONE_BRICK_TILE_WALL);

        bloodyRunestoneBlock();
        blockWithItem(ModBlocks.BLACKSTONE_MAGMA);
        rawMolybdenumBlock();
        blockWithItem(ModBlocks.MOLYBDENUM_BLOCK);
        molybdenumBars();
        incineratedFarmland();
        hempCrop();

        blockWithItem(ModBlocks.SCULK_PLANKS);
        blockWithItem(ModBlocks.CHARRED_PLANKS);
        ashenTree();
        stairsBlock(ModBlocks.CHARRED_STAIRS.get(), blockTexture(ModBlocks.CHARRED_PLANKS.get()));
        slabBlock(ModBlocks.CHARRED_SLAB.get(), blockTexture(ModBlocks.CHARRED_PLANKS.get()), blockTexture(ModBlocks.CHARRED_PLANKS.get()));
        buttonBlock(ModBlocks.CHARRED_BUTTON.get(), blockTexture(ModBlocks.CHARRED_PLANKS.get()));
        pressurePlateBlock(ModBlocks.CHARRED_PRESSURE_PLATE.get(), blockTexture(ModBlocks.CHARRED_PLANKS.get()));
        fenceBlock(ModBlocks.CHARRED_FENCE.get(), blockTexture(ModBlocks.CHARRED_PLANKS.get()));
        fenceGateBlock(ModBlocks.CHARRED_FENCE_GATE.get(), blockTexture(ModBlocks.CHARRED_PLANKS.get()));
        blockItem(ModBlocks.CHARRED_STAIRS);
        blockItem(ModBlocks.CHARRED_SLAB);
        blockItem(ModBlocks.CHARRED_PRESSURE_PLATE);
        blockItem(ModBlocks.CHARRED_FENCE_GATE);
        doorBlockWithRenderType(ModBlocks.CHARRED_DOOR.get(), modLoc("block/charred_door_bottom"), modLoc("block/charred_door_top"), "cutout");
        trapdoorBlockWithRenderType(ModBlocks.CHARRED_TRAPDOOR.get(), modLoc("block/charred_trapdoor"), true, "cutout");
        simpleBlockItem(ModBlocks.CHARRED_TRAPDOOR.get(),
                new ModelFile.UncheckedModelFile("veridia:block/" + ModBlocks.CHARRED_TRAPDOOR.getId().getPath() + "_bottom"));
        blockWithItem(ModBlocks.ASHEN_PLANKS);
        doorBlockWithRenderType(ModBlocks.ASHEN_DOOR.get(), modLoc("block/ashen_door_bottom"), modLoc("block/ashen_door_top"), "cutout");
        trapdoorBlockWithRenderType(ModBlocks.ASHEN_TRAPDOOR.get(), modLoc("block/ashen_trapdoor"), true, "cutout");
        simpleBlockItem(ModBlocks.ASHEN_TRAPDOOR.get(),
                new ModelFile.UncheckedModelFile("veridia:block/" + ModBlocks.ASHEN_TRAPDOOR.getId().getPath() + "_bottom"));
        stairsBlock(ModBlocks.ASHEN_STAIRS.get(), blockTexture(ModBlocks.ASHEN_PLANKS.get()));
        slabBlock(ModBlocks.ASHEN_SLAB.get(), blockTexture(ModBlocks.ASHEN_PLANKS.get()), blockTexture(ModBlocks.ASHEN_PLANKS.get()));
        buttonBlock(ModBlocks.ASHEN_BUTTON.get(), blockTexture(ModBlocks.ASHEN_PLANKS.get()));
        pressurePlateBlock(ModBlocks.ASHEN_PRESSURE_PLATE.get(), blockTexture(ModBlocks.ASHEN_PLANKS.get()));
        fenceBlock(ModBlocks.ASHEN_FENCE.get(), blockTexture(ModBlocks.ASHEN_PLANKS.get()));
        fenceGateBlock(ModBlocks.ASHEN_FENCE_GATE.get(), blockTexture(ModBlocks.ASHEN_PLANKS.get()));
        blockItem(ModBlocks.ASHEN_STAIRS);
        blockItem(ModBlocks.ASHEN_SLAB);
        blockItem(ModBlocks.ASHEN_PRESSURE_PLATE);
        blockItem(ModBlocks.ASHEN_FENCE_GATE);
        cabinet(ModBlocks.ASHEN_CABINET);
        cabinet(ModBlocks.CHARRED_CABINET);
        makeScrapWood("oak", ModBlocks.OAK_SCRAP_WOOD);
        makeScrapWood("spruce", ModBlocks.SPRUCE_SCRAP_WOOD);
        makeScrapWood("ashen", ModBlocks.ASHEN_SCRAP_WOOD);
        makeScrapWood("charred", ModBlocks.CHARRED_SCRAP_WOOD);
        leavesBlock();
        saplingBlock(ModBlocks.SCULK_SPROUT);
        ashenGrass();
        saplingBlock(ModBlocks.SCULK_MUSHROOM);
        /// *** \\\
        /// crops
        makeCrop(((CropBlock) ModBlocks.GARLIC_CROP.get()), GarlicCrop.AGE, "garlic_crop_stage", "garlic_crop_stage");
        makeCrop(((CropBlock) ModBlocks.BELL_PEPPER_CROP.get()), BellPepperCrop.AGE, "bell_pepper_crop_stage", "bell_pepper_crop_stage");
        makeBush(((SweetBerryBushBlock)ModBlocks.BLUE_BERRY_BUSH.get()), "blue_berry_bush_stage", "blue_berry_bush_stage");
        makeBush(((SweetBerryBushBlock)ModBlocks.GOOSE_BERRY_BUSH.get()), "goose_berry_bush_stage", "goose_berry_bush_stage");
        makeBush(((SweetBerryBushBlock)ModBlocks.CRANBERRY_BUSH.get()), "cranberry_bush_stage", "cranberry_bush_stage");
        makeCumaCumaBush();
        stairsBlock(ModBlocks.SCULK_STAIRS.get(), blockTexture(ModBlocks.SCULK_PLANKS.get()));
        slabBlock(ModBlocks.SCULK_SLAB.get(), blockTexture(ModBlocks.SCULK_PLANKS.get()), blockTexture(ModBlocks.SCULK_PLANKS.get()));
        buttonBlock(ModBlocks.SCULK_BUTTON.get(), blockTexture(ModBlocks.SCULK_PLANKS.get()));
        pressurePlateBlock(ModBlocks.SCULK_PRESSURE_PLATE.get(), blockTexture(ModBlocks.SCULK_PLANKS.get()));
        fenceBlock(ModBlocks.SCULK_FENCE.get(), blockTexture(ModBlocks.SCULK_PLANKS.get()));
        fenceGateBlock(ModBlocks.SCULK_FENCE_GATE.get(), blockTexture(ModBlocks.SCULK_PLANKS.get()));
        wallBlock(ModBlocks.SCULK_WALL.get(), blockTexture(ModBlocks.SCULK_PLANKS.get()));

        doorBlockWithRenderType(ModBlocks.SCULK_DOOR.get(), modLoc("block/sculk_door_bottom"), modLoc("block/sculk_door_top"),"cutout");
        trapdoorBlockWithRenderType(ModBlocks.SCULK_TRAPDOOR.get(), modLoc("block/sculk_trapdoor"), true, "cutout");

        blockItem(ModBlocks.SCULK_STAIRS);
        blockItem(ModBlocks.SCULK_SLAB);
        blockItem(ModBlocks.SCULK_PRESSURE_PLATE);
        blockItem(ModBlocks.SCULK_FENCE_GATE);
        blockItem();
        horizontalBlock(ModBlocks.COOKING_TABLE.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/cooking_table")));
        horizontalBlock(ModBlocks.MYTHICAL_TABLE.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/mythical_table")));
        horizontalBlock(ModBlocks.RUNIC_PEDESTAL.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/runic_pedestal")));
        horizontalBlock(ModBlocks.EXTRACTOR.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/extractor")));
        simpleBlock(ModBlocks.STROP.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/strop")));
        horizontalBlock(ModBlocks.BUILDING_MATERIALS.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/building_materials")));
        horizontalBlock(ModBlocks.LUMBERJACK_LOG.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/lumberjack_log")));
        makeFoodStorage();
    }

    private void makeFoodStorage() {
        ModelFile closed = new ModelFile.UncheckedModelFile(modLoc("block/food_storage"));
        ModelFile open = new ModelFile.UncheckedModelFile(modLoc("block/food_storage_open"));
        getVariantBuilder(ModBlocks.FOOD_STORAGE.get()).forAllStates(state -> {
            int yRot = switch (state.getValue(FoodStorageBlock.FACING)) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0;
            };
            ModelFile model = state.getValue(FoodStorageBlock.OPEN) ? open : closed;
            return new ConfiguredModel[]{new ConfiguredModel(model, 0, yRot, false)};
        });
    }
    private static final int SCRAP_WOOD_ALT_ONE_IN = 10;

    /** Хлам-доски породы wood: основная текстура и редкая вторая (1 из SCRAP_WOOD_ALT_ONE_IN), случайно по
     * позиции блока - файлы textures/block/wood/&lt;wood&gt;_scrap_wood.png и ...1.png. */
    private void makeScrapWood(String wood, DeferredBlock<Block> block) {
        String name = wood + "_scrap_wood";
        ModelFile plain = models().cubeAll(name, modLoc("block/wood/" + name));
        ModelFile alt = models().cubeAll(name + "1", modLoc("block/wood/" + name + "1"));
        getVariantBuilder(block.get()).partialState().setModels(
                new ConfiguredModel(plain, 0, 0, false, SCRAP_WOOD_ALT_ONE_IN - 1),
                new ConfiguredModel(alt, 0, 0, false, 1));
        blockItem(block);
    }

    /// *** cuma cuma *** \\\

    private static final float CUMA_FRAME_INSET = 2.0F;
    private static final int CUMA_SPROUT_ROTATION_X = 90;
    private static final int CUMA_SPROUT_ROTATION_Y_OFFSET = 180;

    private void makeCumaCumaBush() {
        getVariantBuilder(ModBlocks.CUMA_CUMA_BUSH.get()).forAllStates(state -> {
            int age = state.getValue(CumaCumaBush.AGE);
            return new ConfiguredModel[]{new ConfiguredModel(
                    models().cross("cuma_cuma_bush_stage" + age,
                            modLoc("block/bush/cc_berry_bush_stage" + age)).renderType("cutout"))};
        });

        ModelFile trunk = cumaFrameModel("cuma_cuma_bush_block", modLoc("block/bush/cc_berry_bushblock1"));
        ModelFile trunkBerries = cumaFrameModel("cuma_cuma_bush_block_berries", modLoc("block/bush/cc_berry_bushblock2"));
        getVariantBuilder(ModBlocks.CUMA_CUMA_BUSH_BLOCK.get()).forAllStates(state ->
                new ConfiguredModel[]{new ConfiguredModel(
                        state.getValue(CumaCumaBushBlock.BERRIES) ? trunkBerries : trunk)});

        ModelFile sprout = cumaSproutModel("cuma_cuma_bush_sprout",
                modLoc("block/bush/cc_berry_bushsprout1"));
        ModelFile sproutBerries = cumaSproutModel("cuma_cuma_bush_sprout_berries",
                modLoc("block/bush/cc_berry_bushsprout2"));
        getVariantBuilder(ModBlocks.CUMA_CUMA_BUSH_SPROUT.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(CumaCumaBushSprout.BERRIES) ? sproutBerries : sprout)
                .rotationX(CUMA_SPROUT_ROTATION_X)
                .rotationY((((int) state.getValue(CumaCumaBushSprout.FACING).toYRot()) + CUMA_SPROUT_ROTATION_Y_OFFSET) % 360)
                .build());
    }
    private ModelFile cumaFrameModel(String name, ResourceLocation texture) {
        float near = CUMA_FRAME_INSET;
        float far = 16.0F - CUMA_FRAME_INSET;
        return models().withExistingParent(name, mcLoc("block/block"))
                .texture("particle", texture)
                .texture("wall", texture)
                .renderType("cutout")
                .ao(false)
                .element().from(0, 0, near).to(16, 16, near)
                    .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#wall").end()
                    .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#wall").end()
                .end()
                .element().from(0, 0, far).to(16, 16, far)
                    .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#wall").end()
                    .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#wall").end()
                .end()
                .element().from(near, 0, 0).to(near, 16, 16)
                    .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#wall").end()
                    .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#wall").end()
                .end()
                .element().from(far, 0, 0).to(far, 16, 16)
                    .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#wall").end()
                    .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#wall").end()
                .end();
    }

    private ModelFile cumaSproutModel(String name, ResourceLocation texture) {
        return models().withExistingParent(name, mcLoc("block/block"))
                .texture("particle", texture)
                .texture("plant", texture)
                .renderType("cutout")
                .ao(false)
                .element().from(0, 0, 8).to(16, 16, 8)
                    .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#plant").end()
                    .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#plant").end()
                .end()
                .element().from(8, 0, 0).to(8, 16, 16)
                    .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#plant").end()
                    .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#plant").end()
                .end();
    }

    private void saplingBlock(DeferredBlock<Block> blockRegistryObject) {
        simpleBlock(blockRegistryObject.get(),
                models().cross(BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath(), blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }

    // Пепельная трава: крест с tintindex 0 (как ванильная трава), цвет задаёт client/AshGrassTint; текстура серая
    private void ashenGrass() {
        simpleBlock(ModBlocks.ASHEN_GRASS.get(),
                models().singleTexture("ashen_grass", mcLoc("block/tinted_cross"), "cross",
                        blockTexture(ModBlocks.ASHEN_GRASS.get())).renderType("cutout"));
    }

    private void leavesBlock() {
        simpleBlockWithItem(ModBlocks.SCULK_LEAVES.get(),
                models().singleTexture(BuiltInRegistries.BLOCK.getKey(ModBlocks.SCULK_LEAVES.get()).getPath(), ResourceLocation.parse("minecraft:block/leaves"),
                        "all", blockTexture(ModBlocks.SCULK_LEAVES.get())).renderType("cutout"));
    }


    public void makeBush(SweetBerryBushBlock block, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> states(state, modelName, textureName);

        getVariantBuilder(block).forAllStates(function);
    }
    private ConfiguredModel[] states(BlockState state, String modelName, String textureName) {
        ConfiguredModel[] models = new ConfiguredModel[1];
        models[0] = new ConfiguredModel(models().cross(modelName + state.getValue(BlueBerryBush.AGE),
                ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "block/" + textureName + state.getValue(BlueBerryBush.AGE))).renderType("cutout"));

        return models;
    }
    public void makeCrop(CropBlock block, IntegerProperty ageProperty, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> states(state, ageProperty, modelName, textureName);
        getVariantBuilder(block).forAllStates(function);
    }

    private ConfiguredModel[] states(BlockState state, IntegerProperty ageProperty, String modelName, String textureName) {
        ConfiguredModel[] models = new ConfiguredModel[1];
        models[0] = new ConfiguredModel(models().crop(modelName + state.getValue(ageProperty),
                ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "block/" + textureName + state.getValue(ageProperty))).renderType("cutout"));
        return models;
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    // Кирпичный блок и его ступени, плита и стена: все модели берут текстуру самого блока. Предмет стены
    // (инвентарная модель) делает ModItemModelProvider.
    private void stoneBrickFamily(DeferredBlock<Block> base, DeferredBlock<StairBlock> stairs,
                                  DeferredBlock<SlabBlock> slab, DeferredBlock<WallBlock> wall) {
        blockWithItem(base);
        stairsBlock(stairs.get(), blockTexture(base.get()));
        slabBlock(slab.get(), blockTexture(base.get()), blockTexture(base.get()));
        wallBlock(wall.get(), blockTexture(base.get()));
        blockItem(stairs);
        blockItem(slab);
    }

    // Ступени, плита и стена из плитки: модели на текстуре самой плитки (сам блок плитки уже есть), предметы ступеней и
    // плиты берут модель блока, предмет стены делает ModItemModelProvider
    private void tileFamily(DeferredBlock<Block> tiles, DeferredBlock<StairBlock> stairs, DeferredBlock<SlabBlock> slab,
                            DeferredBlock<WallBlock> wall) {
        stairsBlock(stairs.get(), blockTexture(tiles.get()));
        slabBlock(slab.get(), blockTexture(tiles.get()), blockTexture(tiles.get()));
        wallBlock(wall.get(), blockTexture(tiles.get()));
        blockItem(stairs);
        blockItem(slab);
    }

    // Выжженная пашня: сухая и влажная (влажность 7) модели на шаблоне ванильной пашни, земля - выжженная земля
    private void incineratedFarmland() {
        ModelFile dry = incineratedFarmlandModel("incinerated_farmland", "block/incinerated_farmland");
        ModelFile moist = incineratedFarmlandModel("incinerated_farmland_moist", "block/incinerated_farmland_moist");
        getVariantBuilder(ModBlocks.INCINERATED_FARMLAND.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(state.getValue(FarmBlock.MOISTURE) == FarmBlock.MAX_MOISTURE ? moist : dry).build());
        simpleBlockItem(ModBlocks.INCINERATED_FARMLAND.get(), dry);
    }

    private ModelFile incineratedFarmlandModel(String name, String topTexture) {
        return models().withExistingParent(name, mcLoc("block/template_farmland"))
                .texture("dirt", blockTexture(ModBlocks.INCINERATED_DIRT.get()))
                .texture("top", modLoc(topTexture));
    }

    // Конопля: возрасты 0-4 одним блоком (block/hemp/hemp_N), с TALL_FROM_AGE нижняя и верхняя половины (hemp_N_bottom и
    // _top). Модель - две плоскости по осям X и Z через центр блока, как у ростка кума-кумы. Верхняя половина младших
    // возрастов не бывает, ей достаётся модель нижней.
    private void hempCrop() {
        getVariantBuilder(ModBlocks.HEMP_CROP.get()).forAllStates(state -> {
            int age = state.getValue(HempCrop.AGE);
            boolean upper = state.getValue(HempCrop.HALF) == DoubleBlockHalf.UPPER;
            String name = "hemp_" + age;
            if (age >= HempCrop.TALL_FROM_AGE) {
                name += upper ? "_top" : "_bottom";
            }
            return ConfiguredModel.builder()
                    .modelFile(cumaSproutModel("hemp_crop_" + name.substring(5), modLoc("block/hemp/" + name))).build();
        });
    }

    // Сырая молибденовая руда: оба состояния (natural true и false) указывают на один обычный куб; торчащий скрап и
    // кубик рисует клиент поверх модели выросшего блока (common/block/ore/client). Текстура - block/raw_molybdenum.
    private void rawMolybdenumBlock() {
        ModelFile cube = models().cubeAll("raw_molybdenum_block", modLoc("block/raw_molybdenum"));
        getVariantBuilder(ModBlocks.RAW_MOLYBDENUM_BLOCK.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(cube).build());
        simpleBlockItem(ModBlocks.RAW_MOLYBDENUM_BLOCK.get(), cube);
    }

    // Молибденовая решётка: ванильные модели железной решётки (post_ends, post, cap, cap_alt, side, side_alt) со своей
    // текстурой и та же раскладка по сторонам, что в ванильном blockstate iron_bars
    private void molybdenumBars() {
        ResourceLocation texture = modLoc("block/molybdenum_bars");
        Function<String, ModelFile> part = name -> models()
                .withExistingParent("molybdenum_bars_" + name, mcLoc("block/iron_bars_" + name))
                .texture("particle", texture).texture("bars", texture).texture("edge", texture)
                .renderType("cutout_mipped");
        ModelFile postEnds = part.apply("post_ends");
        ModelFile post = part.apply("post");
        ModelFile cap = part.apply("cap");
        ModelFile capAlt = part.apply("cap_alt");
        ModelFile side = part.apply("side");
        ModelFile sideAlt = part.apply("side_alt");
        MultiPartBlockStateBuilder builder = getMultipartBuilder(ModBlocks.MOLYBDENUM_BARS.get());
        builder.part().modelFile(postEnds).addModel().end();
        builder.part().modelFile(post).addModel()
                .condition(IronBarsBlock.NORTH, false).condition(IronBarsBlock.EAST, false)
                .condition(IronBarsBlock.SOUTH, false).condition(IronBarsBlock.WEST, false).end();
        barsCap(builder, cap, 0, IronBarsBlock.NORTH);
        barsCap(builder, cap, 90, IronBarsBlock.EAST);
        barsCap(builder, capAlt, 0, IronBarsBlock.SOUTH);
        barsCap(builder, capAlt, 90, IronBarsBlock.WEST);
        builder.part().modelFile(side).addModel().condition(IronBarsBlock.NORTH, true).end();
        builder.part().modelFile(side).rotationY(90).addModel().condition(IronBarsBlock.EAST, true).end();
        builder.part().modelFile(sideAlt).addModel().condition(IronBarsBlock.SOUTH, true).end();
        builder.part().modelFile(sideAlt).rotationY(90).addModel().condition(IronBarsBlock.WEST, true).end();
    }

    // Наконечник решётки: соединена только с одной стороной
    private static void barsCap(MultiPartBlockStateBuilder builder, ModelFile model, int rotationY, BooleanProperty side) {
        builder.part().modelFile(model).rotationY(rotationY).addModel()
                .condition(IronBarsBlock.NORTH, side == IronBarsBlock.NORTH)
                .condition(IronBarsBlock.EAST, side == IronBarsBlock.EAST)
                .condition(IronBarsBlock.SOUTH, side == IronBarsBlock.SOUTH)
                .condition(IronBarsBlock.WEST, side == IronBarsBlock.WEST).end();
    }

    // Рунные камни (кровавый и духовный): нижняя половина рисует всю двухблочную модель (чётные повороты - прямая
    // модель, нечётные - диагональная, повёрнутая на 45), верхняя - пустая модель. Число молитв у духовного камня
    // на модель не влияет. Предмет делает ModItemModelProvider.
    private void bloodyRunestoneBlock() {
        runestoneBlock(ModBlocks.BLOODY_RUNESTONE.get(), "bloody_runestone");
        runestoneBlock(ModBlocks.SPIRITUAL_RUNESTONE.get(), "spiritual_runestone");
    }

    private void runestoneBlock(Block block, String modelName) {
        ModelFile straight = new ModelFile.UncheckedModelFile(modLoc("block/" + modelName));
        ModelFile diagonal = new ModelFile.UncheckedModelFile(modLoc("block/" + modelName + "_diagonal"));
        ModelFile empty = new ModelFile.UncheckedModelFile(modLoc("block/" + modelName + "_upper"));
        getVariantBuilder(block).forAllStates(state -> {
            if (state.getValue(BloodyRunestoneBlock.HALF) == DoubleBlockHalf.UPPER) {
                return ConfiguredModel.builder().modelFile(empty).build();
            }
            int rotation = state.getValue(BloodyRunestoneBlock.ROTATION);
            boolean isDiagonal = rotation % 2 == 1;
            return ConfiguredModel.builder()
                    .modelFile(isDiagonal ? diagonal : straight)
                    .rotationY(rotation / 2 * 90)
                    .build();
        });
    }

    // Обожжённое дерево: у бревна и очищенного бревна боковая текстура и торец (_top), у древесины боковая на всех гранях.
    // Листва - ванильная модель листвы с текстурой еловой; цвет ей задаёт client/AshLeavesTint (tintindex 0 из модели).
    private void ashenTree() {
        logBlock((RotatedPillarBlock) ModBlocks.ASHEN_LOG.get());
        logBlock((RotatedPillarBlock) ModBlocks.STRIPPED_ASHEN_LOG.get());
        ResourceLocation logSide = blockTexture(ModBlocks.ASHEN_LOG.get());
        ResourceLocation strippedLogSide = blockTexture(ModBlocks.STRIPPED_ASHEN_LOG.get());
        axisBlock((RotatedPillarBlock) ModBlocks.ASHEN_WOOD.get(), logSide, logSide);
        axisBlock((RotatedPillarBlock) ModBlocks.STRIPPED_ASHEN_WOOD.get(), strippedLogSide, strippedLogSide);
        blockItem(ModBlocks.ASHEN_LOG);
        blockItem(ModBlocks.STRIPPED_ASHEN_LOG);
        blockItem(ModBlocks.ASHEN_WOOD);
        blockItem(ModBlocks.STRIPPED_ASHEN_WOOD);
        // Очищенные обожжённые бревно и древесина (из пепельных выплавкой)
        logBlock((RotatedPillarBlock) ModBlocks.STRIPPED_CHARRED_LOG.get());
        ResourceLocation strippedCharredSide = blockTexture(ModBlocks.STRIPPED_CHARRED_LOG.get());
        axisBlock((RotatedPillarBlock) ModBlocks.STRIPPED_CHARRED_WOOD.get(), strippedCharredSide, strippedCharredSide);
        blockItem(ModBlocks.STRIPPED_CHARRED_LOG);
        blockItem(ModBlocks.STRIPPED_CHARRED_WOOD);
        simpleBlockWithItem(ModBlocks.ASHEN_LEAVES.get(),
                models().singleTexture("ashen_leaves", mcLoc("block/leaves"), "all", mcLoc("block/spruce_leaves"))
                        .renderType("cutout_mipped"));
    }

    // Колонны: торец <имя>_top, бок <имя>. Капитель - готовая модель из Blockbench (смотрит на север, стена с юга),
    // повёрнутая по FACING как обычный horizontalBlock.
    private void singeStonePillars() {
        for (DeferredBlock<RotatedPillarBlock> pillar : List.of(ModBlocks.SINGE_STONE_PILLAR, ModBlocks.WORN_SINGE_STONE_PILLAR)) {
            ResourceLocation side = blockTexture(pillar.get());
            axisBlock(pillar.get(), side, side.withSuffix("_top"));
            blockItem(pillar);
        }
        ModelFile capital = new ModelFile.UncheckedModelFile(modLoc("block/singe_stone_pillar_ionic"));
        horizontalBlock(ModBlocks.SINGE_STONE_PILLAR_IONIC.get(), capital);
        simpleBlockItem(ModBlocks.SINGE_STONE_PILLAR_IONIC.get(), capital);
    }

    // Кладовая, как у Farmer's Delight: ванильная модель orientable (дверца спереди), текстуры <имя>_front,
    // <имя>_front_open, <имя>_side, <имя>_top; дверца смотрит по FACING
    private void cabinet(DeferredBlock<CabinetBlock> cabinet) {
        String name = cabinet.getId().getPath();
        ResourceLocation side = modLoc("block/" + name + "_side");
        ResourceLocation top = modLoc("block/" + name + "_top");
        ModelFile closed = models().orientable(name, side, modLoc("block/" + name + "_front"), top);
        ModelFile open = models().orientable(name + "_open", side, modLoc("block/" + name + "_front_open"), top);
        getVariantBuilder(cabinet.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(CabinetBlock.OPEN) ? open : closed)
                .rotationY(((int) state.getValue(CabinetBlock.FACING).toYRot() + 180) % 360)
                .build());
        simpleBlockItem(cabinet.get(), closed);
    }

    // Подозрительный блок: по модели на каждую степень расчистки кистью (dusted 0-3, как у подозрительного гравия).
    // Текстура степени - <имя блока>_<степень>.png; если такого файла нет, берётся текстура предыдущей степени
    // (сейчас нет _3 - четвёртая степень выглядит как _2). Предмет берёт модель нулевой степени.
    private static final int DUSTED_STAGES = 4;

    private void suspiciousBlock(DeferredBlock<?> deferredBlock) {
        String name = deferredBlock.getId().getPath();
        ModelFile[] stageModels = new ModelFile[DUSTED_STAGES];
        ResourceLocation texture = modLoc("block/" + name + "_0");
        for (int stage = 0; stage < DUSTED_STAGES; stage++) {
            ResourceLocation stageTexture = modLoc("block/" + name + "_" + stage);
            if (models().existingFileHelper.exists(stageTexture, ModelProvider.TEXTURE)) {
                texture = stageTexture;
            }
            stageModels[stage] = models().cubeAll(name + "_" + stage, texture);
        }
        getVariantBuilder(deferredBlock.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(stageModels[state.getValue(BlockStateProperties.DUSTED)]).build());
        simpleBlockItem(deferredBlock.get(), stageModels[0]);
    }

    private void blockItem(DeferredBlock<?> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("veridia:block/" + deferredBlock.getId().getPath()));
    }
    private void blockItem() {
        simpleBlockItem(((DeferredBlock<?>) ModBlocks.SCULK_TRAPDOOR).get(),
                new ModelFile.UncheckedModelFile("veridia:block/" + ModBlocks.SCULK_TRAPDOOR.getId().getPath() + "_bottom"));
    }

}