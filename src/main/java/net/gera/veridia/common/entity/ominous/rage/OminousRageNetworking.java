package net.gera.veridia.common.entity.ominous.rage;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.entity.ominous.rage.client.OminousRageClient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Регистрация пакета ярости (рассылку при входе игрока делает {@link OminousRagePlayerSync}). */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class OminousRageNetworking {
    private OminousRageNetworking() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                OminousRagePayload.TYPE,
                OminousRagePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> OminousRageClient.receive(payload.endGameTime()))
        );
    }
}
