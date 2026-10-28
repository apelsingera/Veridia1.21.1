package net.gera.veridia.common.compat.curios.network;

import net.gera.veridia.Veridia;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActivateCurioPayload() implements CustomPacketPayload {

    public static final Type<ActivateCurioPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "activate_curio"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateCurioPayload> STREAM_CODEC =
            StreamCodec.unit(new ActivateCurioPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}