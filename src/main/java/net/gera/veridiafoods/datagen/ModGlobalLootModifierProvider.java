package net.gera.veridiafoods.datagen;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.item.ModItems;
import net.gera.veridiafoods.loot.AddItemModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;

import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {
    public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, VeridiaFoods.MOD_ID);
    }

    @Override
    protected void start() {
        this.add("garlic_seeds_from_fern",
                new AddItemModifier(new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.FERN).build(),
                        LootItemRandomChanceCondition.randomChance(0.2f).build() }, ModItems.GARLIC_SEEDS.get()));
        this.add("plant_clump_from_grass",
                new AddItemModifier(new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.SHORT_GRASS).build(),
                        LootItemRandomChanceCondition.randomChance(0.4f).build() }, ModItems.PLANT_CLUMP.get()));

    }
}
