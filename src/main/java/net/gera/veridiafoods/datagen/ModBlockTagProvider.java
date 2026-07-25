package net.gera.veridiafoods.datagen;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, VeridiaFoods.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(ModBlocks.COOKING_TABLE.get())
                .add(ModBlocks.SCULK_LOG.get())
                .add(ModBlocks.SCULK_WOOD.get())
                .add(ModBlocks.STRIPPED_SCULK_LOG.get())
                .add(ModBlocks.STRIPPED_SCULK_WOOD.get())
                .add(ModBlocks.SCULK_PLANKS.get());

        tag(BlockTags.MINEABLE_WITH_HOE)
                .add(ModBlocks.SCULK_LEAVES.get());
    }
}
