package net.gera.veridia.common.mana.veridite;

import net.gera.veridia.Veridia;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayLaserShakePayload(int durationTicks) implements CustomPacketPayload {

    public static final Type<PlayLaserShakePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "play_laser_shake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayLaserShakePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayLaserShakePayload::durationTicks,
            PlayLaserShakePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
