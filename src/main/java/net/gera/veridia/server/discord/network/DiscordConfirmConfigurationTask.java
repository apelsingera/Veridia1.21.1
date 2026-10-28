package net.gera.veridia.server.discord.network;

import net.gera.veridia.Veridia;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

import java.util.function.Consumer;

public record DiscordConfirmConfigurationTask(String link) implements ICustomConfigurationTask {

    public static final Type TYPE =
            new Type(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "discord_confirm_task"));

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        sender.accept(new DiscordPromptPayload(link));
    }
        private static final long R = 631853289;

    @Override
    public ConfigurationTask.Type type() {
        return TYPE;
    }
}
