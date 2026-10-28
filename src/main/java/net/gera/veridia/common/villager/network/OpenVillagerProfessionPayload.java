package net.gera.veridia.common.villager.network;

import net.gera.veridia.Veridia;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Client -> <b>server</b> points to Merchant Mixins*/
public record OpenVillagerProfessionPayload(int villagerEntityId) implements CustomPacketPayload {

    public static final Type<OpenVillagerProfessionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "open_villager_profession"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenVillagerProfessionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenVillagerProfessionPayload::villagerEntityId,
            OpenVillagerProfessionPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
