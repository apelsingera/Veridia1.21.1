package net.gera.veridia.common.mana.veridite;

import net.gera.veridia.Veridia;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LaserBeamPayload(int shooterId, double endX, double endY, double endZ, int tier)
        implements CustomPacketPayload {

    public static final Type<LaserBeamPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "laser_beam"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LaserBeamPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LaserBeamPayload::shooterId,
            ByteBufCodecs.DOUBLE, LaserBeamPayload::endX,
            ByteBufCodecs.DOUBLE, LaserBeamPayload::endY,
            ByteBufCodecs.DOUBLE, LaserBeamPayload::endZ,
            ByteBufCodecs.VAR_INT, LaserBeamPayload::tier,
            LaserBeamPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
