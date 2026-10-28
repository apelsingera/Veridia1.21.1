package net.gera.veridia.server.discord;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DiscordGateConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED =
            BUILDER.comment("Включена ли проверка Discord при входе на этот сервер")
                    .define("enabled", false);

    public static final ModConfigSpec.ConfigValue<String> DISCORD_LINK =
            BUILDER.comment("Ссылка на Discord, показывается игроку при входе")
                    .define("discordLink", "https://discord.gg/7jYSYu7pm");

    public static final ModConfigSpec SPEC = BUILDER.build();
}
