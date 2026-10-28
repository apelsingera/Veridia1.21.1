package net.gera.veridia.common.fluid;

import net.gera.veridia.Veridia;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public class ModFluidClientEvents {

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new TanninFluidClientExtensions(), ModFluids.TANNIN_FLUID_TYPE);
        event.registerFluidType(new CorruptionFluidClientExtensions(), ModFluids.CORRUPTION_FLUID_TYPE);
    }
}