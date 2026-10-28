package net.gera.veridia.common.entity.ominous.rage;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * {@code /veridiadebug ominousrage start | stop | status} - ярость зловещих воинов в измерении, где выполняется команда
 * (для операторов).
 * <ul>
 *   <li>start - начинает ярость (идёт уже - начинает заново, на полную длину трека). Всё как от камня: небо, музыка,
 *       воины в ярости, только без активного камня - лучей и усиления нет.</li>
 *   <li>stop - обрывает ярость; камень, с которого она началась, гаснет и уходит на отдых, как после обычного
 *       конца.</li>
 *   <li>status - сколько ярости осталось.</li>
 * </ul>
 */
public final class OminousRageCommand {
    private static final int TICKS_PER_SECOND = 20;

    private OminousRageCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> debugNode() {
        return Commands.literal("ominousrage")
                .then(Commands.literal("start").executes(OminousRageCommand::start))
                .then(Commands.literal("stop").executes(OminousRageCommand::stop))
                .then(Commands.literal("status").executes(OminousRageCommand::status));
    }

    private static int start(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        OminousRage.start(level);
        context.getSource().sendSuccess(() -> Component.translatable("command.veridia.ominousrage.started",
                formatTime(OminousRage.DURATION_TICKS)), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        if (!OminousRage.isActive(level)) {
            context.getSource().sendFailure(Component.translatable("command.veridia.ominousrage.not_active"));
            return 0;
        }
        OminousRage.stop(level);
        context.getSource().sendSuccess(() -> Component.translatable("command.veridia.ominousrage.stopped"), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        long ticksLeft = OminousRage.ticksLeft(context.getSource().getLevel());
        Component message = ticksLeft > 0L
                ? Component.translatable("command.veridia.ominousrage.status", formatTime(ticksLeft))
                : Component.translatable("command.veridia.ominousrage.not_active");
        context.getSource().sendSuccess(() -> message, false);
        return ticksLeft > 0L ? 1 : 0;
    }

    // Минуты:секунды
    private static String formatTime(long ticks) {
        long seconds = ticks / TICKS_PER_SECOND;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
