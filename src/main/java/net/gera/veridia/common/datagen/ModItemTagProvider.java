package net.gera.veridia.common.datagen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
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
        tag(ModTags.Items.KNIVES)
                .add(ModItems.CRUDE_KNIFE.get())
                .add(ModItems.SS_KNIFE.get());
        tag(ModTags.Items.EXTRACTOR_UPGRADES)
                .add(ModItems.ALCHEMIST_UPGRADE.get())
                .add(ModItems.WARRIOR_UPGRADE.get())
                .add(ModItems.WORKER_UPGRADE.get());
        tag(ModTags.Items.GEM)
                .add(ModItems.AQUATIC_AMULET.get())
                .add(ModItems.SLOW_KILLER.get())
                .add(ModItems.KNOCKBACK_RING.get())
                .add(ModItems.EYE_OF_SAURON.get())
                .add(ModItems.INFERNAL_STAFF.get())
                .add(ModItems.BLINDING_PRISM.get());
        this.tag(ItemTags.TRIMMABLE_ARMOR)
                .add(ModItems.VERIDITE_HELMET.get())
                .add(ModItems.VERIDITE_CHESTPLATE.get())
                .add(ModItems.VERIDITE_LEGGINGS.get())
                .add(ModItems.VERIDITE_BOOTS.get());

        this.copy(ModTags.Blocks.ASHEN_LOGS, ModTags.Items.ASHEN_LOGS);
        this.copy(ModTags.Blocks.CHARRED_LOGS, ModTags.Items.CHARRED_LOGS);
        this.copy(BlockTags.LOGS, ItemTags.LOGS);
        this.copy(BlockTags.LEAVES, ItemTags.LEAVES);
        this.copy(BlockTags.PLANKS, ItemTags.PLANKS);
        this.copy(BlockTags.STAIRS, ItemTags.STAIRS);
        this.copy(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS);
        this.copy(BlockTags.SLABS, ItemTags.SLABS);
        this.copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
        this.copy(BlockTags.FENCES, ItemTags.FENCES);
        this.copy(BlockTags.WOODEN_FENCES, ItemTags.WOODEN_FENCES);
        this.copy(BlockTags.FENCE_GATES, ItemTags.FENCE_GATES);
        this.copy(BlockTags.BUTTONS, ItemTags.BUTTONS);
        this.copy(BlockTags.WOODEN_BUTTONS, ItemTags.WOODEN_BUTTONS);
        this.copy(BlockTags.WOODEN_PRESSURE_PLATES, ItemTags.WOODEN_PRESSURE_PLATES);
        this.copy(BlockTags.DOORS, ItemTags.DOORS);
        this.copy(BlockTags.WOODEN_DOORS, ItemTags.WOODEN_DOORS);
        this.copy(BlockTags.TRAPDOORS, ItemTags.TRAPDOORS);
        this.copy(BlockTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_TRAPDOORS);
        this.copy(ModTags.Blocks.FARMERSDELIGHT_WOODEN_CABINETS, ModTags.Items.FARMERSDELIGHT_WOODEN_CABINETS);
        this.copy(BlockTags.WALLS, ItemTags.WALLS);

        // Опалённый булыжник как обычный: общий тег булыжника и ванильные теги, по которым крафтятся печь, варочная
        // стойка, каменные инструменты и т. п.
        this.copy(Tags.Blocks.COBBLESTONES, Tags.Items.COBBLESTONES);
        this.copy(Tags.Blocks.STONES, Tags.Items.STONES);
        this.tag(ItemTags.STONE_CRAFTING_MATERIALS).add(ModBlocks.SINGE_COBBLESTONE.asItem());
        this.tag(ItemTags.STONE_TOOL_MATERIALS).add(ModBlocks.SINGE_COBBLESTONE.asItem());

        // Молибден в общих тегах (c:ingots/molybdenum и т. д.), блок - копией блочного тега
        this.tag(ModTags.Items.MOLYBDENUM_INGOTS).add(ModItems.MOLYBDENUM_INGOT.get());
        this.tag(Tags.Items.INGOTS).addTag(ModTags.Items.MOLYBDENUM_INGOTS);
        this.tag(ModTags.Items.MOLYBDENUM_NUGGETS).add(ModItems.MOLYBDENUM_NUGGET.get());
        this.tag(Tags.Items.NUGGETS).addTag(ModTags.Items.MOLYBDENUM_NUGGETS);
        this.tag(ModTags.Items.MOLYBDENUM_PLATES).add(ModItems.MOLYBDENUM_SHEET.get());
        this.copy(ModTags.Blocks.MOLYBDENUM_STORAGE_BLOCKS, ModTags.Items.MOLYBDENUM_STORAGE_BLOCKS);
        this.tag(ItemTags.BEACON_PAYMENT_ITEMS).add(ModItems.MOLYBDENUM_INGOT.get());
    }
}