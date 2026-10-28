package net.gera.veridia.common.block.entity;

import net.gera.veridia.Veridia;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;


@EventBusSubscriber(modid = Veridia.MOD_ID)
public class ExtractorCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.EXTRACTOR_BE.get(),
                (blockEntity, side) -> blockEntity.getInputTank());

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.EXTRACTOR_BE.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());
    }
}