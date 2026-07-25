package net.gera.veridiafoods.datagen;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.block.ModBlocks;
import net.gera.veridiafoods.block.bushes.BlueBerryBush;
import net.gera.veridiafoods.block.crop.BellPepperCrop;
import net.gera.veridiafoods.block.crop.GarlicCrop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, VeridiaFoods.MOD_ID, exFileHelper);
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

        blockWithItem(ModBlocks.SCULK_PLANKS);
        leavesBlock(ModBlocks.SCULK_LEAVES);
        saplingBlock(ModBlocks.SCULK_SPROUT);
        /// *** \\\
        /// crops
        makeCrop(((CropBlock) ModBlocks.GARLIC_CROP.get()), GarlicCrop.AGE, "garlic_crop_stage", "garlic_crop_stage");
        makeCrop(((CropBlock) ModBlocks.BELL_PEPPER_CROP.get()), BellPepperCrop.AGE, "bell_pepper_crop_stage", "bell_pepper_crop_stage");
        makeBush(((SweetBerryBushBlock)ModBlocks.BLUE_BERRY_BUSH.get()), "blue_berry_bush_stage", "blue_berry_bush_stage");
        makeBush(((SweetBerryBushBlock)ModBlocks.GOOSE_BERRY_BUSH.get()), "goose_berry_bush_stage", "goose_berry_bush_stage");
        makeBush(((SweetBerryBushBlock)ModBlocks.CRANBERRY_BUSH.get()), "cranberry_bush_stage", "cranberry_bush_stage");

        horizontalBlock(ModBlocks.COOKING_TABLE.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/cooking_table")));
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


    public void makeBush(SweetBerryBushBlock block, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> states(state, modelName, textureName);

        getVariantBuilder(block).forAllStates(function);
    }
    private ConfiguredModel[] states(BlockState state, String modelName, String textureName) {
        ConfiguredModel[] models = new ConfiguredModel[1];
        models[0] = new ConfiguredModel(models().cross(modelName + state.getValue(BlueBerryBush.AGE),
                ResourceLocation.fromNamespaceAndPath(VeridiaFoods.MOD_ID, "block/" + textureName + state.getValue(BlueBerryBush.AGE))).renderType("cutout"));

        return models;
    }
    public void makeCrop(CropBlock block, IntegerProperty ageProperty, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> states(state, ageProperty, modelName, textureName);
        getVariantBuilder(block).forAllStates(function);
    }

    private ConfiguredModel[] states(BlockState state, IntegerProperty ageProperty, String modelName, String textureName) {
        ConfiguredModel[] models = new ConfiguredModel[1];
        models[0] = new ConfiguredModel(models().crop(modelName + state.getValue(ageProperty),
                ResourceLocation.fromNamespaceAndPath(VeridiaFoods.MOD_ID, "block/" + textureName + state.getValue(ageProperty))).renderType("cutout"));
        return models;
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("veridiafoods:block/" + deferredBlock.getId().getPath()));
    }
}