package net.gera.veridia.common.compat.tfmg;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

/**
 * {@code /veridiadebug distillation} - что сервер знает о ректификационной колонне TFMG, на блок которой смотрит
 * игрок (стальной бак или контроллер колонны рядом с ним): главный блок бака, признан ли бак колонной, нагрев и что
 * TFMG увидел под баком при последнем подсчёте (нагрев от Create и с поправками {@link DistillationHeat}).
 */
public final class DistillationDebugCommand {
    private static final double REACH = 20.0;

    private DistillationDebugCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> debugNode() {
        return Commands.literal("distillation").executes(DistillationDebugCommand::report);
    }

    private static int report(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        HitResult hit = player.pick(REACH, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS) {
            say(player, "Посмотри на стальной бак или контроллер колонны", ChatFormatting.RED);
            return 0;
        }
        BlockPos tankPos = findTank(level, blockHit.getBlockPos());
        if (tankPos == null) {
            say(player, "Ни здесь, ни рядом нет стального бака TFMG", ChatFormatting.RED);
            return 0;
        }
        DistillationTank tank = (DistillationTank) level.getBlockEntity(tankPos);
        DistillationReport main = mainReport(level, tank.getDistillationReport());
        if (main == null) {
            say(player, "Главного блока бака нет (бак перестраивается?)", ChatFormatting.RED);
            return 0;
        }
        sayTank(player, tankPos, tank, main);
        saySamples(player, level, main);
        return 1;
    }

    // Блок, на который смотрят, или соседний (смотрят на контроллер колонны), если это стальной бак
    @Nullable
    private static BlockPos findTank(ServerLevel level, BlockPos looked) {
        if (level.getBlockEntity(looked) instanceof DistillationTank) {
            return looked;
        }
        for (Direction direction : Direction.values()) {
            BlockPos next = looked.relative(direction);
            if (level.getBlockEntity(next) instanceof DistillationTank) {
                return next;
            }
        }
        return null;
    }

    @Nullable
    private static DistillationReport mainReport(ServerLevel level, DistillationReport report) {
        return level.getBlockEntity(report.main()) instanceof DistillationTank main ? main.getDistillationReport()
                : null;
    }

    private static void sayTank(ServerPlayer player, BlockPos tankPos, DistillationTank tank, DistillationReport main) {
        say(player, "Бак: главный блок " + main.main().toShortString() + ", ширина " + main.width()
                + ", высота " + main.height(), ChatFormatting.GOLD);
        say(player, "Колонна: " + (main.tower() ? "да" : "НЕТ (TFMG не нашёл контроллер колонны у бака)")
                + ", нагрев главного блока " + main.heat(), main.tower() ? ChatFormatting.GREEN : ChatFormatting.RED);
        say(player, "Нагрев у блока " + tankPos.toShortString() + " (его берёт контроллер колонны): "
                + tank.getDistillationHeat(), ChatFormatting.YELLOW);
    }

    private static void saySamples(ServerPlayer player, ServerLevel level, DistillationReport main) {
        if (main.samples().isEmpty()) {
            say(player, "TFMG нагрев не считал. Сейчас под главным блоком:", ChatFormatting.RED);
            for (int x = 0; x < main.width(); x++) {
                for (int z = 0; z < main.width(); z++) {
                    BlockPos below = main.main().offset(x, -1, z);
                    say(player, "  " + below.toShortString() + " " + describe(level.getBlockState(below)),
                            ChatFormatting.GRAY);
                }
            }
            return;
        }
        say(player, "Последний подсчёт нагрева (под баком):", ChatFormatting.GOLD);
        for (DistillationReport.HeatSample sample : main.samples()) {
            say(player, "  " + sample.pos().toShortString() + " " + describe(sample.state()) + ": Create "
                    + sample.createHeat() + " -> " + sample.heat(), ChatFormatting.GRAY);
        }
    }

    // createdieselgenerators:burner[axis=x,blaze=none,lit=true]
    private static String describe(BlockState state) {
        String properties = state.getValues().entrySet().stream()
                .map(entry -> entry.getKey().getName() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return properties.isEmpty() ? id : id + "[" + properties + "]";
    }

    private static void say(ServerPlayer player, String text, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(text).withStyle(color));
    }
}
