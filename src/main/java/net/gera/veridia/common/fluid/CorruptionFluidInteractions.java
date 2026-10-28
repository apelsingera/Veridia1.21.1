package net.gera.veridia.common.fluid;

import net.gera.veridia.Veridia;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry.InteractionInformation;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class CorruptionFluidInteractions {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> FluidInteractionRegistry.addInteraction(
                ModFluids.CORRUPTION_FLUID_TYPE.value(),
                new InteractionInformation(NeoForgeMod.WATER_TYPE.value(), Blocks.DEEPSLATE.defaultBlockState())));
    }
}