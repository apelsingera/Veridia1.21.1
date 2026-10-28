package net.gera.veridia.common.worldgen.structure.mixin;

import net.gera.veridia.common.worldgen.structure.StructurePieceLimits;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Кандидаты на подстановку: выкидываем куски, чей лимит в текущей структуре уже выбран. */
@Mixin(StructureTemplatePool.class)
public class StructureTemplatePoolMixin {

    @Inject(method = "getShuffledTemplates", at = @At("RETURN"), cancellable = true)
    private void veridia$dropExhausted(CallbackInfoReturnable<List<StructurePoolElement>> callback) {
        List<StructurePoolElement> candidates = callback.getReturnValue();
        List<StructurePoolElement> allowed = StructurePieceLimits.filterExhausted(candidates);
        if (allowed != candidates) {
            callback.setReturnValue(allowed);
        }
    }
}
