package net.gera.veridia.common.worldgen.structure.mixin;

import net.gera.veridia.common.worldgen.structure.StructurePieceLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Отметка о принятом куске. Этот конструктор вызывается уже после проверки пересечений,
 * то есть только для кусков, которые реально вошли в структуру.
 */
@Mixin(PoolElementStructurePiece.class)
public class PoolElementStructurePieceMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/world/level/levelgen/structure/pools/StructurePoolElement;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/level/block/Rotation;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/levelgen/structure/templatesystem/LiquidSettings;)V",
            at = @At("TAIL"))
    private void veridia$countPlaced(StructureTemplateManager templateManager, StructurePoolElement element,
                                          BlockPos position, int groundLevelDelta, Rotation rotation,
                                          BoundingBox boundingBox, LiquidSettings liquidSettings,
                                          CallbackInfo callback) {
        StructurePieceLimits.onPiecePlaced(element);
    }
}
