package net.gera.veridia.common.preset;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.gera.veridia.Veridia;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

/**
 * /preset create &lt;имя&gt; &lt;сущность&gt; [&lt;pos&gt;] [&lt;nbt&gt;] - сохранить моба под именем (аргументы те же, что у /summon,
 * позиция принимается, но не используется); /preset remove &lt;имя&gt;; /preset list. Сохранённого моба можно звать по
 * имени, например в гробе: PresetMob:"имя". См. {@link MobPresets}.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class PresetCommand {
    private static final String NAME = "name";
    private static final String ENTITY = "entity";
    private static final String POS = "pos";
    private static final String NBT = "nbt";

    private PresetCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("preset")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("create")
                        .then(Commands.argument(NAME, StringArgumentType.word())
                                .then(Commands.argument(ENTITY, ResourceArgument.resource(event.getBuildContext(), Registries.ENTITY_TYPE))
                                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                        .executes(context -> create(context, new CompoundTag()))
                                        .then(Commands.argument(POS, Vec3Argument.vec3())
                                                .executes(context -> create(context, new CompoundTag()))
                                                .then(Commands.argument(NBT, CompoundTagArgument.compoundTag())
                                                        .executes(context -> create(context, CompoundTagArgument.getCompoundTag(context, NBT))))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument(NAME, StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(MobPresets.customNames(), builder))
                                .executes(PresetCommand::remove)))
                .then(Commands.literal("list").executes(PresetCommand::list)));
    }

    private static int create(CommandContext<CommandSourceStack> context, CompoundTag nbt) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, NAME);
        Holder.Reference<EntityType<?>> type = ResourceArgument.getSummonableEntityType(context, ENTITY);
        CompoundTag preset = nbt.copy();
        preset.putString("id", type.key().location().toString());
        try {
            MobPresets.saveCustom(name, preset);
        } catch (IllegalArgumentException e) {
            throw new SimpleCommandExceptionType(Component.literal("Пресет не сохранён: " + e.getMessage())).create();
        }
        context.getSource().sendSuccess(() -> Component.literal("Пресет «" + name + "» сохранён. В гробе: PresetMob:\"" + name + "\""), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, NAME);
        if (!MobPresets.removeCustom(name)) {
            throw new SimpleCommandExceptionType(Component.literal(MobPresets.isBuiltIn(name)
                    ? "«" + name + "» - встроенный пресет, его удалить нельзя"
                    : "Нет своего пресета «" + name + "»")).create();
        }
        context.getSource().sendSuccess(() -> Component.literal("Пресет «" + name + "» удалён"), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        Collection<String> custom = MobPresets.customNames();
        context.getSource().sendSuccess(() -> Component.literal("Встроенные: " + String.join(", ", MobPresets.builtInSummary()))
                .withStyle(ChatFormatting.GOLD), false);
        context.getSource().sendSuccess(() -> Component.literal(custom.isEmpty()
                ? "Своих пока нет (создать: /preset create)"
                : "Свои: " + String.join(", ", custom)).withStyle(ChatFormatting.GRAY), false);
        return custom.size();
    }
}
