package net.gera.veridia.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.block.ModBlocks;
import net.gera.veridia.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.Mod;
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
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.COPPER_ORE.get())
                .add(ModBlocks.DEEPSLATE_COPPER_ORE.get())
                .add(ModBlocks.GRANITE_COPPER_ORE.get())
                .add(ModBlocks.ANDESITE_COPPER_ORE.get())
                .add(ModBlocks.DIORITE_COPPER_ORE.get())
                .add(ModBlocks.TUFF_COPPER_ORE.get())
                .add(ModBlocks.TIN_BLOCK.get())
                .add(ModBlocks.GRANITE_TIN_ORE.get())
                .add(ModBlocks.ANDESITE_TIN_ORE.get())
                .add(ModBlocks.DIORITE_TIN_ORE.get())
                .add(ModBlocks.TUFF_TIN_ORE.get())
                .add(ModBlocks.TIN_ORE.get())
                .add(ModBlocks.DEEPSLATE_TIN_ORE.get())
                .add(ModBlocks.AZOTIC_TOPAZ_END_ORE.get())
                .add(ModBlocks.DATOLITE_NETHER_ORE.get());
        tag(BlockTags.INCORRECT_FOR_STONE_TOOL)
                //COPPER
                .add(ModBlocks.COPPER_ORE.get())
                .add(ModBlocks.DEEPSLATE_COPPER_ORE.get())
                .add(ModBlocks.GRANITE_COPPER_ORE.get())
                .add(ModBlocks.ANDESITE_COPPER_ORE.get())
                .add(ModBlocks.DIORITE_COPPER_ORE.get())
                .add(ModBlocks.TUFF_COPPER_ORE.get())
                //TIN
                .add(ModBlocks.TIN_BLOCK.get())
                //
                .add(ModBlocks.TIN_ORE.get())
                .add(ModBlocks.DEEPSLATE_TIN_ORE.get())
                .add(ModBlocks.GRANITE_TIN_ORE.get())
                .add(ModBlocks.ANDESITE_TIN_ORE.get())
                .add(ModBlocks.DIORITE_TIN_ORE.get())
                .add(ModBlocks.TUFF_TIN_ORE.get());
        tag(ModTags.Blocks.NEEDS_COPPER_TOOL)
                //copper
                .add(ModBlocks.COPPER_ORE.get())
                .add(ModBlocks.DEEPSLATE_COPPER_ORE.get())
                .add(ModBlocks.GRANITE_COPPER_ORE.get())
                .add(ModBlocks.ANDESITE_COPPER_ORE.get())
                .add(ModBlocks.DIORITE_COPPER_ORE.get())
                .add(ModBlocks.TUFF_COPPER_ORE.get())
                //TIN
                .add(ModBlocks.TIN_BLOCK.get())
                .add(ModBlocks.GRANITE_TIN_ORE.get())
                .add(ModBlocks.ANDESITE_TIN_ORE.get())
                .add(ModBlocks.DIORITE_TIN_ORE.get())
                .add(ModBlocks.TUFF_TIN_ORE.get())
                .add(ModBlocks.TIN_ORE.get())
                .add(ModBlocks.DEEPSLATE_TIN_ORE.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.AZOTIC_TOPAZ_END_ORE.get())
                .add(ModBlocks.DATOLITE_NETHER_ORE.get());



        tag(BlockTags.FENCES).add(ModBlocks.TIN_FENCE.get());
        tag(BlockTags.FENCE_GATES).add(ModBlocks.TIN_FENCE_GATE.get());
        tag(BlockTags.WALLS).add(ModBlocks.TIN_WALL.get());

        tag(ModTags.Blocks.NEEDS_COPPER_TOOL)
                .addTag(BlockTags.NEEDS_IRON_TOOL);

        tag(ModTags.Blocks.INCORRECT_FOR_COPPER_TOOL)
                .addTag(BlockTags.INCORRECT_FOR_STONE_TOOL)
                .remove(ModTags.Blocks.NEEDS_COPPER_TOOL);

        tag(ModTags.Blocks.INCORRECT_FOR_BRONZE_TOOL)
                .addTag(BlockTags.INCORRECT_FOR_STONE_TOOL)
                .addTag(ModTags.Blocks.INCORRECT_FOR_COPPER_TOOL)
                .remove(ModTags.Blocks.NEEDS_BRONZE_TOOL);

    }
}
