package net.gera.veridia.common.heat;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Ветка /veridiadebug heat (см. VeridiaDebugCommand): lakes on|off - жар у больших лавовых озёр на весь мир,
 * status - что сейчас включено. Жар Незера не трогается.
 */
public final class HeatCommands {

    private HeatCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> debugNode() {
        return Commands.literal("heat")
                .then(Commands.literal("lakes")
                        .then(Commands.literal("on").executes(context -> setLakeHeat(context, true)))
                        .then(Commands.literal("off").executes(context -> setLakeHeat(context, false))))
                .then(Commands.literal("status").executes(HeatCommands::status));
    }

    private static int setLakeHeat(CommandContext<CommandSourceStack> context, boolean enabled) {
        HeatSettings.get(context.getSource().getServer()).setLakeHeat(enabled);
        context.getSource().sendSuccess(() -> Component.translatable(
                enabled ? "command.veridia.heat.lakes_on" : "command.veridia.heat.lakes_off"), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        boolean enabled = HeatSettings.get(context.getSource().getServer()).lakeHeat();
        context.getSource().sendSuccess(() -> Component.translatable(
                enabled ? "command.veridia.heat.status_on" : "command.veridia.heat.status_off"), false);
        return enabled ? 1 : 0;
    }
}
