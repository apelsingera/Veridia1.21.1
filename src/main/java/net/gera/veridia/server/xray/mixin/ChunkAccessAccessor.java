package net.gera.veridia.server.xray.mixin;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ChunkAccess.class)
public interface ChunkAccessAccessor {
    @Accessor("sections") LevelChunkSection[] getSections$AntiXray();
    @Mutable @Accessor("sections") void setSections$AntiXray(LevelChunkSection[] sections);
}
