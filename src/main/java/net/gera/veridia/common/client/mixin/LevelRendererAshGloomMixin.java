package net.gera.veridia.common.client.mixin;

import net.gera.veridia.common.worldgen.biome.client.AshGloom;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Пепельный биом в {@link LevelRenderer}:
 * <ul>
 *   <li>В {@code renderSky} единственный вызов {@code level.getRainLevel(partialTick)} - это прозрачность солнца и луны
 *       ({@code 1.0F - rain}), заодно на неё умножается яркость звёзд. Пока идёт затемнение пепельного биома, выдаём
 *       "как в сильный дождь", и небесные тела тают: солнце на чёрном небе выглядело бы нелепо.</li>
 *   <li>{@code renderClouds} рисуется с ванильной дальностью тумана, а не с дымкой: туман считает и расстояние по
 *       вертикали, и облака над головой иначе почти целиком утонули бы в дымке.</li>
 * </ul>
 */
@Mixin(LevelRenderer.class)
public class LevelRendererAshGloomMixin {

    @Redirect(
            method = "renderSky",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"),
            remap = false
    )
    private float veridia$hideSkyBodies(ClientLevel level, float partialTick) {
        return Math.max(level.getRainLevel(partialTick), AshGloom.amount(partialTick));
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), remap = false)
    private void veridia$cloudsWithoutHaze(CallbackInfo ci) {
        AshGloom.useVanillaFogForClouds();
    }

    @Inject(method = "renderClouds", at = @At("RETURN"), remap = false)
    private void veridia$restoreHazeAfterClouds(CallbackInfo ci) {
        AshGloom.restoreFogAfterClouds();
    }
}
