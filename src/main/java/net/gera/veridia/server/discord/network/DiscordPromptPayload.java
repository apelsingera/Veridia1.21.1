package net.gera.veridia.server.discord.network;

import net.gera.veridia.Veridia;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DiscordPromptPayload(String link) implements CustomPacketPayload {

    public static final Type<DiscordPromptPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "discord_prompt"));

    public static final StreamCodec<FriendlyByteBuf, DiscordPromptPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DiscordPromptPayload::link,
            DiscordPromptPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}