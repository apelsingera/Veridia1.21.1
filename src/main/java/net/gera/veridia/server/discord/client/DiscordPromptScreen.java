package net.gera.veridia.server.discord.client;

import net.gera.veridia.server.discord.network.DiscordConfirmPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordPromptScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordPromptScreen.class);
    private final String link;
    private final ICommonPacketListener listener;

    public DiscordPromptScreen(String link, ICommonPacketListener listener) {
        super(Component.literal("Discord"));
        this.link = link;
        this.listener = listener;
    }

    @Override
    protected void init() {
        LOGGER.info("[DiscordGate] DiscordPromptScreen.init() вызван, width={}, height={}", this.width, this.height);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.literal(link), button -> {
                    LOGGER.info("[DiscordGate] Кнопка со ссылкой нажата");
                    Util.getPlatform().openUri(link);
                })
                .bounds(centerX - 100, centerY - 10, 200, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Да"), button -> {
                    LOGGER.info("[DiscordGate] Кнопка ДА нажата, отправляю подтверждение");
                    listener.send(new DiscordConfirmPayload());
                    this.onClose();
                })
                .bounds(centerX - 105, centerY + 20, 100, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Нет"), button -> {
                    LOGGER.info("[DiscordGate] Кнопка НЕТ нажата, рву соединение");
                    listener.disconnect(Component.literal("Возврат к списку серверов"));
                    LOGGER.info("[DiscordGate] disconnect() вызван, переключаю экран");
                    Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(new TitleScreen()));
                    LOGGER.info("[DiscordGate] setScreen() вызван");
                })
                .bounds(centerX + 5, centerY + 20, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font,
                Component.literal("Установили ли вы моды с нашего Discord-сервера?"),
                this.width / 2, this.height / 2 - 40, 0xFFFFFF);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
