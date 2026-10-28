package net.gera.veridia.common.heat;

import net.gera.veridia.Veridia;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Сервер сообщает клиенту, насколько сильно игрок страдает от жара (для красной виньетки и сужения угла обзора). */
public record HeatPayload(int percent) implements CustomPacketPayload {

    public static final Type<HeatPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "heat"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HeatPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HeatPayload::percent,
            HeatPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
