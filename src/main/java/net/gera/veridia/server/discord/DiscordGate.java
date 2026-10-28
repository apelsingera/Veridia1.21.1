package net.gera.veridia.server.discord;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class DiscordGate {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!DiscordGateConfig.ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (DiscordPopupPreferences.get(player.server).isDisabled(player.getUUID())) return;

        player.sendSystemMessage(Component.literal(
                "Если не хочешь видеть окно про наш Discord при подключении - напиши /discord nopopups"));
    }

    // /discord nopopups - для всех игроков, поэтому остаётся отдельной командой без прав оператора
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("discord")
                .then(Commands.literal("nopopups")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        DiscordPopupPreferences.get(ctx.getSource().getServer())
                                .setDisabled(player.getUUID(), true);
                        ctx.getSource().sendSuccess(
                                () -> Component.literal("Окно про Discord при подключении больше не будет показываться."), false);
                        return 1;
                    }))
        );
    }

    /** Ветка /veridiadebug discord set|get|on|off|status (бывшая /discordlink, см. VeridiaDebugCommand). */
    public static LiteralArgumentBuilder<CommandSourceStack> debugNode() {
        return Commands.literal("discord")
                .then(Commands.literal("set")
                    .then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String url = StringArgumentType.getString(ctx, "url");
                            DiscordGateConfig.DISCORD_LINK.set(url);
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("Ссылка обновлена: " + url), true);
                            return 1;
                        })))
                .then(Commands.literal("get")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("Текущая ссылка: " + DiscordGateConfig.DISCORD_LINK.get()), false);
                        return 1;
                    }))
                .then(Commands.literal("on")
                    .executes(ctx -> {
                        DiscordGateConfig.ENABLED.set(true);
                        ctx.getSource().sendSuccess(() -> Component.literal("Проверка Discord включена."), true);
                        return 1;
                    }))
                .then(Commands.literal("off")
                    .executes(ctx -> {
                        DiscordGateConfig.ENABLED.set(false);
                        ctx.getSource().sendSuccess(() -> Component.literal("Проверка Discord выключена."), true);
                        return 1;
                    }))
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        boolean on = DiscordGateConfig.ENABLED.get();
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "Проверка Discord: " + (on ? "включена" : "выключена")), false);
                        return 1;
                    }));
    }
}
