package net.gera.veridia.common.entity.ominous.rage;

import net.gera.veridia.Veridia;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Сервер сообщает клиенту, до какого времени мира (getGameTime) в его измерении идёт ярость зловещих воинов. */
public record OminousRagePayload(long endGameTime) implements CustomPacketPayload {

    public static final Type<OminousRagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "ominous_rage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OminousRagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, OminousRagePayload::endGameTime,
            OminousRagePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
