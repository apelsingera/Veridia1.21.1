package net.gera.veridia.common.debug;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.levelzombie.LevelZombieCommands;
import net.gera.veridia.common.compat.tfmg.DistillationDebugCommand;
import net.gera.veridia.common.entity.ominous.rage.OminousRageCommand;
import net.gera.veridia.common.heat.HeatCommands;
import net.gera.veridia.common.mana.ModCommands;
import net.gera.veridia.server.discord.DiscordGate;
import net.gera.veridia.server.xray.AntiXrayCommands;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Все отладочные команды мода под одной: /veridiadebug (только операторы, уровень прав 2, как /gamemode).
 * <ul>
 *   <li>mana regen &lt;n&gt; | god on|off | level &lt;n&gt;</li>
 *   <li>cooldown clear - снять все кулдауны способностей</li>
 *   <li>antixray bypass on|off</li>
 *   <li>discord set &lt;ссылка&gt; | get | on | off | status - настройки окна про Discord</li>
 *   <li>ominousrage start | stop | status</li>
 *   <li>heat lakes on|off | status - жар у лавовых озёр</li>
 *   <li>finditems &lt;часть id&gt; - поиск предметов по id</li>
 *   <li>distillation - что сервер знает о колонне TFMG, на которую смотришь (нагрев, блоки под баком)</li>
 * </ul>
 * Сами ветки описаны рядом со своими системами, здесь только сборка.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class VeridiaDebugCommand {
    private static final int PERMISSION_LEVEL = 2;

    private VeridiaDebugCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("veridiadebug")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .then(ModCommands.manaNode())
                .then(ModCommands.cooldownNode())
                .then(AntiXrayCommands.debugNode())
                .then(DiscordGate.debugNode())
                .then(OminousRageCommand.debugNode())
                .then(HeatCommands.debugNode())
                .then(LevelZombieCommands.debugNode())
                .then(DistillationDebugCommand.debugNode()));
    }
}
