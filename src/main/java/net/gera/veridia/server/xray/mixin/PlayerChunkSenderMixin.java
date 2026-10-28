package net.gera.veridia.server.xray.mixin;

import net.gera.veridia.server.xray.AntiXray;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin {

    @Inject(
            method = "sendChunk",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void antiXray$sendObfuscated(
            ServerGamePacketListenerImpl packetListener,
            ServerLevel level,
            LevelChunk chunk,
            CallbackInfo callbackInfo
    ) {
        LevelChunkSection[] obfuscated = AntiXray.obfuscateSections(level, chunk, packetListener.player);
        if (obfuscated == null) {
            // Прятать нечего (руды не нашлось/выключено/bypass) - без этой проверки chunk на миг
            // оставался вообще БЕЗ секций (accessor.setSections$AntiXray(null)), и сборка пакета
            // падала с NPE в calculateChunkSize ("Cannot read the array length"). Просто не отменяем
            // ванильный sendChunk.
            return;
        }

        ChunkAccessAccessor accessor = (ChunkAccessAccessor) chunk;
        LevelChunkSection[] original = accessor.getSections$AntiXray();
        try {
            accessor.setSections$AntiXray(obfuscated);
            packetListener.send(chunk.getAuxLightManager(chunk.getPos())
                    .sendLightDataTo(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null)));
        } finally {
            accessor.setSections$AntiXray(original);
        }

        ChunkPos chunkPos = chunk.getPos();
        DebugPackets.sendPoiPacketsForChunk(level, chunkPos);
        EventHooks.fireChunkSent(packetListener.player, chunk, level);

        callbackInfo.cancel();
    }
}
