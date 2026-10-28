package net.gera.veridia.server.discord.network;

import net.gera.veridia.Veridia;
import net.gera.veridia.server.discord.DiscordGateConfig;
import net.gera.veridia.server.discord.DiscordPopupPreferences;
import net.gera.veridia.server.discord.client.DiscordPromptClientHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class DiscordNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.configurationToClient(
                DiscordPromptPayload.TYPE,
                net.gera.veridia.server.discord.network.DiscordPromptPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> DiscordPromptClientHandler.open(payload.link(), context.listener()))
        );

        registrar.configurationToServer(
                net.gera.veridia.server.discord.network.DiscordConfirmPayload.TYPE,
                net.gera.veridia.server.discord.network.DiscordConfirmPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        context.finishCurrentTask(net.gera.veridia.server.discord.network.DiscordConfirmConfigurationTask.TYPE))
        );
    }

    @SubscribeEvent
    public static void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        if (!DiscordGateConfig.ENABLED.get()) return;

        if (event.getListener() instanceof ServerConfigurationPacketListenerImpl listener) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && DiscordPopupPreferences.get(server).isDisabled(listener.getOwner().getId())) {
                return;
            }
        }

        event.register(new net.gera.veridia.server.discord.network.DiscordConfirmConfigurationTask(DiscordGateConfig.DISCORD_LINK.get()));
    }
}