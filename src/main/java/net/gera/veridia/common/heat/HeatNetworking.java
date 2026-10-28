package net.gera.veridia.common.heat;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.heat.client.HeatClientState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class HeatNetworking {
    private HeatNetworking() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                HeatPayload.TYPE,
                HeatPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> HeatClientState.receive(payload.percent()))
        );
    }
}
