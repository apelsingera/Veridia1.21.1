package net.gera.veridia.server.discord.network;

import net.gera.veridia.Veridia;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DiscordConfirmPayload() implements CustomPacketPayload {

    public static final Type<DiscordConfirmPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "discord_confirm"));

    public static final StreamCodec<FriendlyByteBuf, DiscordConfirmPayload> STREAM_CODEC =
            StreamCodec.unit(new DiscordConfirmPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}