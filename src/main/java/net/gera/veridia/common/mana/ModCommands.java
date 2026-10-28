package net.gera.veridia.common.mana;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.recipe.knowledge.RecipeKnowledge;
import net.gera.veridia.common.mana.veridite.VeriditeLaserAbility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Отладочные команды системы маны внутри /veridiadebug (см. VeridiaDebugCommand): mana regen|god|level и
 * cooldown clear. Права проверяет корень /veridiadebug.
 */
public final class ModCommands {

    private ModCommands() {
    }

    /** mana regen &lt;n&gt; | god on|off | level &lt;n&gt; */
    public static LiteralArgumentBuilder<CommandSourceStack> manaNode() {
        return Commands.literal("mana")
                .then(Commands.literal("regen")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ModCommands::regenMana)))
                .then(Commands.literal("god")
                        .then(Commands.literal("on").executes(context -> setGodMode(context, true)))
                        .then(Commands.literal("off").executes(context -> setGodMode(context, false))))
                .then(Commands.literal("level")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ModCommands::addManaLevel)));
    }

    /** cooldown clear - снять кулдауны со всех предметов со способностями */
    public static LiteralArgumentBuilder<CommandSourceStack> cooldownNode() {
        return Commands.literal("cooldown")
                .then(Commands.literal("clear").executes(ModCommands::removeAllCooldowns));
    }

    private static int regenMana(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(context, "amount");

        int max = ManaProgression.getMaxMana(player);
        int current = player.getData(ModAttachments.MANA);
        int updated = (int) Math.min(max, (long) current + (long) amount);
        player.setData(ModAttachments.MANA, updated);

        context.getSource().sendSuccess(
                () -> Component.translatable("command.veridia.mana_regen", updated, max), true);
        return updated;
    }

    /** /veridiadebug mana level <n> - продвигает прогресс маны на n шагов вперёд без надобности крафтить/юзать руны. */
    private static int addManaLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(context, "amount");

        int current = ManaProgression.progress(player);
        int updated = Math.min(ManaProgression.STEPS.size(), current + amount);
        player.setData(ModAttachments.MANA_UPGRADE_PROGRESS, updated);
        ManaProgression.reconcileAttributes(player);

        context.getSource().sendSuccess(() -> Component.translatable(
                "command.veridia.mana_level", updated, ManaProgression.STEPS.size()), true);
        return updated;
    }

    private static int setGodMode(CommandContext<CommandSourceStack> context, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.setData(ModAttachments.MANA_GOD_MODE, enabled);
        if (enabled) {
            player.setData(ModAttachments.MANA, ManaProgression.getMaxMana(player));
        }

        context.getSource().sendSuccess(() -> Component.translatable(
                enabled ? "command.veridia.mana_god_on" : "command.veridia.mana_god_off"), true);
        return 1;
    }

    /** Снимает кулдаун со всех предметов мода со способностями, включая внутренний таймер лазера. */
    private static int removeAllCooldowns(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        player.getCooldowns().removeCooldown(ModItems.ELECTRIC_SWORD.get());
        player.getCooldowns().removeCooldown(ModItems.EYE_OF_SAURON.get());
        player.getCooldowns().removeCooldown(ModItems.AQUATIC_AMULET.get());
        player.getCooldowns().removeCooldown(ModItems.KNOCKBACK_RING.get());
        player.getCooldowns().removeCooldown(ModItems.SLOW_KILLER.get());
        player.getCooldowns().removeCooldown(ModItems.INFERNAL_STAFF.get());
        // Промежуточные тиры гемов (eye_of_sauron_2..5, aquatic_amulet_2..5, slow_killer_2..5,
        // knockback_ring_2..5, infernal_staff_2..5) - берём их из деревьев знаний, чтобы список
        // не приходилось вести вручную.
        for (List<RecipeKnowledge.KnowledgeTier> tree : RecipeKnowledge.TREES) {
            for (RecipeKnowledge.KnowledgeTier tier : tree) {
                player.getCooldowns().removeCooldown(tier.resultItem());
            }
        }
        VeriditeLaserAbility.clearCooldown(player);

        context.getSource().sendSuccess(() -> Component.translatable("command.veridia.cooldowns_cleared"), true);
        return 1;
    }
}
