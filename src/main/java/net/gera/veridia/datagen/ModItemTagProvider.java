package net.gera.veridia.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.block.ModBlocks;
import net.gera.veridia.item.ModItems;
import net.gera.veridia.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Veridia.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Items.TRANSFORMABLE_ITEMS)
                .add(ModItems.CRUDE_KNIFE.get())
                .add(Items.COAL)
                .add(Items.STICK)
                .add(Items.COMPASS);

        tag(ItemTags.SWORDS)
                .add(ModItems.COPPER_SWORD.get())
                .add(ModItems.BRONZE_SWORD.get());
        tag(ItemTags.PICKAXES)
                .add(ModItems.COPPER_PICKAXE.get())
                .add(ModItems.BRONZE_PICKAXE.get());
        tag(ItemTags.AXES)
                .add(ModItems.COPPER_AXE.get())
                .add(ModItems.BRONZE_AXE.get());
        tag(ItemTags.SHOVELS)
                .add(ModItems.COPPER_SHOVEL.get())
                .add(ModItems.BRONZE_SHOVEL.get());
        tag(ItemTags.HOES)
                .add(ModItems.COPPER_HOE.get())
                .add(ModItems.BRONZE_HOE.get());

        //armor
        this.tag(ItemTags.TRIMMABLE_ARMOR)
                //COPPER
        .add(ModItems.COPPER_HELMET.get())
        .add(ModItems.COPPER_CHESTPLATE.get())
        .add(ModItems.COPPER_LEGGINGS.get())
        .add(ModItems.COPPER_BOOTS.get())
                //BRONZE
                .add(ModItems.BRONZE_HELMET.get())
                .add(ModItems.BRONZE_CHESTPLATE.get())
                .add(ModItems.BRONZE_LEGGINGS.get())
                .add(ModItems.BRONZE_BOOTS.get());
        this.tag(ItemTags.PLANKS)
                .add(ModBlocks.SCULK_PLANKS.asItem());
    }
}
