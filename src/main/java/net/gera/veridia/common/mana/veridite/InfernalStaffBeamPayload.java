package net.gera.veridia.common.mana.veridite;

import net.gera.veridia.Veridia;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record InfernalStaffBeamPayload(int playerId, int beamCount, float beamLength,
                                       float radiansPerTick, int tier) implements CustomPacketPayload {

    public static final Type<InfernalStaffBeamPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "infernal_staff_beams"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfernalStaffBeamPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, InfernalStaffBeamPayload::playerId,
                    ByteBufCodecs.VAR_INT, InfernalStaffBeamPayload::beamCount,
                    ByteBufCodecs.FLOAT, InfernalStaffBeamPayload::beamLength,
                    ByteBufCodecs.FLOAT, InfernalStaffBeamPayload::radiansPerTick,
                    ByteBufCodecs.VAR_INT, InfernalStaffBeamPayload::tier,
                    InfernalStaffBeamPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
