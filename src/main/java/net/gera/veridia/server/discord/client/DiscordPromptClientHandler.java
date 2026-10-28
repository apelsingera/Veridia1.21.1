package net.gera.veridia.server.discord.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordPromptClientHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordPromptClientHandler.class);

    public static void open(String link, ICommonPacketListener listener) {
        LOGGER.info("[DiscordGate] open() вызван, текущий экран: {}",
                Minecraft.getInstance().screen == null ? "null" : Minecraft.getInstance().screen.getClass().getName());
        Minecraft.getInstance().setScreen(new DiscordPromptScreen(link, listener));
    }
}
