package net.gera.veridia.server.xray;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Ветка /veridiadebug antixray bypass on|off (см. VeridiaDebugCommand). */
public final class AntiXrayCommands {

    private AntiXrayCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> debugNode() {
        return Commands.literal("antixray")
                .then(Commands.literal("bypass")
                        .then(Commands.literal("on").executes(context -> setBypass(context, true)))
                        .then(Commands.literal("off").executes(context -> setBypass(context, false))));
    }

    private static int setBypass(CommandContext<CommandSourceStack> context, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        AntiXray.setBypass(player.getUUID(), enabled);

        context.getSource().sendSuccess(() -> Component.translatable(
                enabled ? "command.veridia.antixray_bypass_on" : "command.veridia.antixray_bypass_off"), true);
        return 1;
    }
}
