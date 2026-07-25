package net.gera.veridiafoods.datagen;

import net.gera.veridiafoods.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.concurrent.CompletableFuture;

public class ModDataMapProvider extends DataMapProvider {
    protected ModDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather() {
        this.builder(NeoForgeDataMaps.COMPOSTABLES)
                .add(ModItems.GARLIC_SEEDS.getId(), new Compostable(0.25f), false)
                .add(ModItems.GARLIC.getId(), new Compostable(0.40f), false)
                .add(ModItems.BELL_PEPPER_SEEDS.getId(), new Compostable(0.20f), false)
                .add(ModItems.RED_BELL_PEPPER.getId(), new Compostable(0.40f), false)

                .add(ModItems.BLUE_BERRIES.getId(), new Compostable(0.40f), false)
                .add(ModItems.GOOSE_BERRIES.getId(), new Compostable(0.40f), false)

                .add(ModItems.DANDELIONS_ROOT.getId(), new Compostable(0.20f), false);


    }
}
