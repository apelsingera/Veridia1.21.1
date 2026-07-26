package net.gera.veridiafoods.datagen;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.block.ModBlocks;
import net.gera.veridiafoods.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;


public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, VeridiaFoods.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.DANDELIONS_ROOT.get());
        basicItem(ModItems.PLANT_CLUMP.get());
        basicItem(ModItems.FIBER_NET.get());
        basicItem(ModItems.PLANT_STRING.get());
        handheldItem(ModItems.CRUDE_KNIFE.get());
        handheldItem(ModItems.SS_KNIFE.get());
        handheldItem(ModItems.CRUDE_HAMMER.get());
        basicItem(ModItems.GARLIC_SEEDS.get());
        basicItem(ModItems.TOMATO_CHERRY.get());
        basicItem(ModItems.RED_ONION.get());
        basicItem(ModItems.GARLIC.get());
        basicItem(ModItems.BELL_PEPPER_SEEDS.get());
        basicItem(ModItems.RED_BELL_PEPPER.get());
        basicItem(ModItems.RED_BELL_PEPPER_SLICE.get());
        basicItem(ModItems.BLUE_BERRIES.get());
        basicItem(ModItems.GOOSE_BERRIES.get());
        basicItem(ModItems.CRANBERRIES.get());
        basicItem(ModItems.GROUND_PORK.get());
        basicItem(ModItems.PORK_PATTY.get());
        basicItem(ModItems.COOKED_PORK_PATTY.get());
        basicItem(ModItems.PORKCHOP_SLICE.get());
        basicItem(ModItems.GROUND_BEEF.get());
        basicItem(ModItems.BEEF_PATTY.get());
        basicItem(ModItems.COOKED_BEEF_PATTY.get());

        basicItem(ModItems.UNFINISHED_STUFFED_PEPPER.get());
        basicItem(ModItems.STUFFED_PEPPER.get());
        basicItem(ModItems.BEEF_BURGER.get());
        basicItem(ModItems.CHICKEN_LEGS.get());
        basicItem(ModItems.BEEF_W_POTATOES.get());
        basicItem(ModItems.PIQUANT_ELITE_FISH_DISH.get());
        basicItem(ModItems.NICE_BREAKFAST.get());

        basicItem(ModItems.CHEESE.get());
        basicItem(ModItems.BUTTER.get());
        basicItem(ModItems.BOILED_EGG.get());
        basicItem(ModItems.EGG_BUCKET.get());
        basicItem(ModItems.BOILED_EGG_BUCKET.get());

        basicItem(ModItems.EXOTIC_SEASONING1.get());
        basicItem(ModItems.EXOTIC_SEASONING2.get());
        basicItem(ModItems.EXOTIC_SEASONING3.get());

        basicItem(ModItems.BLUEBERRY_PIE.get());
        basicItem(ModItems.BLUEBERRY_PIE_SLICE.get());
        basicItem(ModItems.GOOSEBERRY_PIE.get());
        basicItem(ModItems.GOOSEBERRY_PIE_SLICE.get());
        basicItem(ModItems.CRANBERRY_PIE.get());
        basicItem(ModItems.CRANBERRY_PIE_SLICE.get());

        basicItem(ModItems.WHEAT_FLOUR.get());
        basicItem(ModItems.DOUGH.get());
        basicItem(ModItems.PASTA.get());
        basicItem(ModItems.TASTY_PASTA.get());
        basicItem(ModItems.PASTA_BUCKET.get());
        basicItem(ModItems.COOKED_PASTA_BUCKET.get());
        basicItem(ModItems.CARROT_PASTA.get());
        basicItem(ModItems.RONALDO_PASTA.get());

        basicItem(ModItems.POTATO_PUREE.get());

        basicItem(ModItems.MILK_N_BUTTER.get());
        basicItem(ModItems.BLUE_BERRY_JUICE.get());
        basicItem(ModItems.SUPER_BLUE_BERRY_JUICE.get());
        basicItem(ModItems.CRANBERRY_JUICE.get());
        basicItem(ModItems.SUPER_CRANBERRY_JUICE.get());
        basicItem(ModItems.GOOSE_BERRY_JUICE.get());
        basicItem(ModItems.SUPER_GOOSE_BERRY_JUICE.get());


        saplingItem(ModBlocks.SCULK_SPROUT);
    }


    private ItemModelBuilder saplingItem(DeferredBlock<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(VeridiaFoods.MOD_ID,"block/" + item.getId().getPath()));
    }
    public void buttonItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/button_inventory"))
                .texture("texture",  ResourceLocation.fromNamespaceAndPath(VeridiaFoods.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }

    public void fenceItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/fence_inventory"))
                .texture("texture",  ResourceLocation.fromNamespaceAndPath(VeridiaFoods.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }

    public void wallItem(DeferredBlock<?> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/wall_inventory"))
                .texture("wall",  ResourceLocation.fromNamespaceAndPath(VeridiaFoods.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }
    private ItemModelBuilder handheldItem(DeferredItem<?> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(VeridiaFoods.MOD_ID,"item/" + item.getId().getPath()));
    }
}
