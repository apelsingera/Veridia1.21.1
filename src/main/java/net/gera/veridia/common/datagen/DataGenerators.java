package net.gera.veridia.common.datagen;


import net.gera.veridia.Veridia;
import net.gera.veridia.common.compat.dynamictrees.DynamicTreesData;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = event.getGenerator().getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        List<LootTableProvider.SubProviderEntry> lootTables = new ArrayList<>(List.of(
                new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(ModChestLootProvider::new, LootContextParamSets.CHEST),
                new LootTableProvider.SubProviderEntry(ModArchaeologyLootProvider::new, LootContextParamSets.ARCHAEOLOGY)));
        boolean dynamicTreesLoaded = ModList.get().isLoaded("dynamictrees");
        if (dynamicTreesLoaded) {
            lootTables.add(DynamicTreesData.lootEntry(existingFileHelper));
        }
        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                lootTables, lookupProvider));
        generator.addProvider(event.includeServer(), new ModDatapackProvider(packOutput, lookupProvider));


        BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTagsProvider);
        generator.addProvider(event.includeServer(), new ModItemTagProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(),
                new AdvancementProvider(packOutput, lookupProvider, existingFileHelper, List.of(new ModAdvancementProvider())));
        generator.addProvider(event.includeClient(),new ModItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModGlobalLootModifierProvider(packOutput, lookupProvider));

        // Ресурсы деревьев Dynamic Trees: после наших провайдеров, ему нужны уже готовые модели обожжённой листвы и бревна
        if (dynamicTreesLoaded) {
            DynamicTreesData.gather(event);
        }
    }
}