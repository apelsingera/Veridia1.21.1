package net.gera.veridia.datagen;

import net.gera.veridia.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.concurrent.CompletableFuture;

public class ModDataMapProvider extends DataMapProvider {
    protected ModDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather() {
        this.builder(NeoForgeDataMaps.FURNACE_FUELS)
                .add(ModItems.BALLS.getId(), new FurnaceFuel(1200), false);
        this.builder(NeoForgeDataMaps.COMPOSTABLES)
                .add(ModItems.GARLIC_SEEDS.getId(), new Compostable(0.25f), false)
                .add(ModItems.GARLIC.getId(), new Compostable(0.40f), false)

                .add(ModItems.DANDELIONS_ROOT.getId(), new Compostable(0.20f), false);
    }
}
