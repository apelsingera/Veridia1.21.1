package net.gera.veridia.common.aibuff.levelzombie;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;

public class LevelZombieCommands {

    /** Ветка /veridiadebug finditems <часть id> - поиск предметов по id (бывшая /levelzombie finditems). */
    public static LiteralArgumentBuilder<CommandSourceStack> debugNode() {
        return Commands.literal("finditems")
                .then(Commands.argument("search", StringArgumentType.string())
                        .executes(LevelZombieCommands::findItems));
    }

    private static int findItems(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String search = StringArgumentType.getString(context, "search").toLowerCase(Locale.ROOT);

        List<ResourceLocation> matches = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.toString().toLowerCase(Locale.ROOT).contains(search))
                .sorted()
                .toList();

        if (matches.isEmpty()) {
            player.sendSystemMessage(Component.literal("Ничего не найдено по \"" + search + "\"")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        player.sendSystemMessage(Component.literal("Найдено " + matches.size() + ":")
                .withStyle(ChatFormatting.GOLD));

        int shown = Math.min(matches.size(), 50);
        for (int i = 0; i < shown; i++) {
            player.sendSystemMessage(Component.literal("  " + matches.get(i)).withStyle(ChatFormatting.GRAY));
        }
        if (matches.size() > shown) {
            player.sendSystemMessage(Component.literal("  ... и ещё " + (matches.size() - shown) + " - сузь поиск")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        return matches.size();
    }
}
