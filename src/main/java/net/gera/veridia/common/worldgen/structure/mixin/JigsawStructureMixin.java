package net.gera.veridia.common.worldgen.structure.mixin;

import net.gera.veridia.common.worldgen.structure.StructurePieceLimits;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(JigsawStructure.class)
public class JigsawStructureMixin {

    @Inject(method = "findGenerationPoint", at = @At("HEAD"))
    private void veridia$resetPieceLimits(Structure.GenerationContext context,
                                               CallbackInfoReturnable<Optional<Structure.GenerationStub>> callback) {
        StructurePieceLimits.beginStructure();
    }
}
