package net.gera.veridia.common.worldgen.biome.mixin;

import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Лоза не растёт в пепельных землях: случайный тик лозы (рост вниз и в стороны) там пропускается. Поставленная руками
 * лоза остаётся как есть.
 */
@Mixin(VineBlock.class)
public class VineBlockAshMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void veridia$noGrowthInAshLands(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                            CallbackInfo ci) {
        if (ModBiomes.isAshBiome(level.getBiome(pos))) {
            ci.cancel();
        }
    }
}
