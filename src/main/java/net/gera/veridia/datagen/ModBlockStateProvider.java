package net.gera.veridia.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.block.ModBlocks;
import net.gera.veridia.block.bushes.BlueBerryBush;
import net.gera.veridia.block.crop.GarlicCrop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Veridia.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.PLANT_CLUMP_BLOCK);
        blockWithItem(ModBlocks.COPPER_ORE);
        blockWithItem(ModBlocks.DEEPSLATE_COPPER_ORE);
        blockWithItem(ModBlocks.GRANITE_COPPER_ORE);
        blockWithItem(ModBlocks.ANDESITE_COPPER_ORE);
        blockWithItem(ModBlocks.DIORITE_COPPER_ORE);
        blockWithItem(ModBlocks.TUFF_COPPER_ORE);
        blockWithItem(ModBlocks.TIN_BLOCK);
        blockWithItem(ModBlocks.GRANITE_TIN_ORE);
        blockWithItem(ModBlocks.ANDESITE_TIN_ORE);
        blockWithItem(ModBlocks.DIORITE_TIN_ORE);
        blockWithItem(ModBlocks.TUFF_TIN_ORE);
        blockWithItem(ModBlocks.TIN_ORE);
        blockWithItem(ModBlocks.DEEPSLATE_TIN_ORE);
        blockWithItem(ModBlocks.AZOTIC_TOPAZ_END_ORE);
        blockWithItem(ModBlocks.DATOLITE_NETHER_ORE);

        logBlock(((RotatedPillarBlock) ModBlocks.SCULK_LOG.get()));
        axisBlock(((RotatedPillarBlock) ModBlocks.SCULK_WOOD.get()), blockTexture(ModBlocks.SCULK_LOG.get()), blockTexture(ModBlocks.SCULK_LOG.get()));

        logBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_SCULK_LOG.get()));
        axisBlock(((RotatedPillarBlock) ModBlocks.STRIPPED_SCULK_WOOD.get()), blockTexture(ModBlocks.STRIPPED_SCULK_LOG.get()), blockTexture(ModBlocks.STRIPPED_SCULK_LOG.get()));

        blockItem(ModBlocks.SCULK_LOG);
        blockItem(ModBlocks.STRIPPED_SCULK_LOG);
        blockItem(ModBlocks.SCULK_WOOD);
        blockItem(ModBlocks.STRIPPED_SCULK_WOOD);

        blockWithItem(ModBlocks.SCULK_PLANKS);
        leavesBlock(ModBlocks.SCULK_LEAVES);
        saplingBlock(ModBlocks.SCULK_SPROUT);
        /// *** \\\


        stairsBlock(ModBlocks.TIN_STAIRS.get(), blockTexture(ModBlocks.TIN_BLOCK.get()));
        slabBlock(ModBlocks.TIN_SLAB.get(), blockTexture(ModBlocks.TIN_BLOCK.get()), blockTexture(ModBlocks.TIN_BLOCK.get()));
        buttonBlock(ModBlocks.TIN_BUTTON.get(), blockTexture(ModBlocks.TIN_BLOCK.get()));
        pressurePlateBlock(ModBlocks.TIN_PRESSURE_PLATE.get(), blockTexture(ModBlocks.TIN_BLOCK.get()));
        fenceBlock(ModBlocks.TIN_FENCE.get(), blockTexture(ModBlocks.TIN_BLOCK.get()));
        fenceGateBlock(ModBlocks.TIN_FENCE_GATE.get(), blockTexture(ModBlocks.TIN_BLOCK.get()));
        wallBlock(ModBlocks.TIN_WALL.get(), blockTexture(ModBlocks.TIN_BLOCK.get()));

        doorBlockWithRenderType(ModBlocks.TIN_DOOR.get(), modLoc("block/tin_door_bottom"), modLoc("block/tin_door_top"),"cutout");
        trapdoorBlockWithRenderType(ModBlocks.TIN_TRAPDOOR.get(), modLoc("block/tin_trapdoor"), true, "cutout");

        blockItem(ModBlocks.TIN_STAIRS);
        blockItem(ModBlocks.TIN_SLAB);
        blockItem(ModBlocks.TIN_PRESSURE_PLATE);
        blockItem(ModBlocks.TIN_FENCE_GATE);
        blockItem(ModBlocks.TIN_TRAPDOOR, "_bottom");

        /// crops
        makeCrop(((CropBlock) ModBlocks.GARLIC_CROP.get()),"garlic_crop_stage", "garlic_crop_stage");
        makeBush(((SweetBerryBushBlock)ModBlocks.BLUE_BERRY_BUSH.get()), "blue_berry_bush_stage", "blue_berry_bush_stage");
    }
    private void saplingBlock(DeferredBlock<Block> blockRegistryObject) {
        simpleBlock(blockRegistryObject.get(),
                models().cross(BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath(), blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }

    private void leavesBlock(DeferredBlock<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(),
                models().singleTexture(BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath(), ResourceLocation.parse("minecraft:block/leaves"),
                        "all", blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }

    public void makeCrop(CropBlock block, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> states(state, block, modelName, textureName);

        getVariantBuilder(block).forAllStates(function);
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
    private ConfiguredModel[] states(BlockState state, CropBlock block, String modelName, String textureName) {
        ConfiguredModel[] models = new ConfiguredModel[1];
        models[0] = new ConfiguredModel(models().crop(modelName + state.getValue(((GarlicCrop) block).getAgeProperty()),
                ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "block/" + textureName + state.getValue(((GarlicCrop) block).getAgeProperty()))).renderType("cutout"));

        return models;
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("veridia:block/" + deferredBlock.getId().getPath()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock, String appendix) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("veridia:block/" + deferredBlock.getId().getPath() + appendix));
    }
}