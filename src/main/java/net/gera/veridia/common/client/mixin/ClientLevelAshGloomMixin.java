package net.gera.veridia.common.client.mixin;

import net.gera.veridia.common.entity.ominous.rage.client.OminousRageSky;
import net.gera.veridia.common.worldgen.biome.client.AshGloom;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Затемнение пепельных биомов (см. {@link AshGloom}): после ванильного расчёта цвета неба, цвета облаков
 * и освещения от неба результат подкручивается на текущую степень затемнения. Поверх затемнения, пока идёт
 * ярость зловещих воинов, небо и облака красятся в фиолетовый ({@link OminousRageSky}).
 */
@Mixin(ClientLevel.class)
public class ClientLevelAshGloomMixin {

    // Цвет небесного купола; тот же цвет подмешивается и в дальний туман (FogRenderer)
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void veridia$gloomSkyColor(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Vec3 color = cir.getReturnValue();
        float gloom = AshGloom.amount(partialTick);
        if (gloom > 0.0F) {
            color = AshGloom.darkenSky(color, gloom);
        }
        float rage = OminousRageSky.amount(partialTick);
        if (rage > 0.0F) {
            color = OminousRageSky.tintSky(color, rage);
        }
        cir.setReturnValue(color);
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void veridia$gloomCloudColor(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Vec3 color = cir.getReturnValue();
        float gloom = AshGloom.amount(partialTick);
        if (gloom > 0.0F) {
            color = AshGloom.tintClouds(color, gloom);
        }
        float rage = OminousRageSky.amount(partialTick);
        if (rage > 0.0F) {
            color = OminousRageSky.tintClouds(color, rage);
        }
        cir.setReturnValue(color);
    }

    // Читается только при обновлении карты освещения (LightTexture): именно оно темнит землю ночью
    @Inject(method = "getSkyDarken(F)F", at = @At("RETURN"), cancellable = true)
    private void veridia$gloomWorldLight(float partialTick, CallbackInfoReturnable<Float> cir) {
        float amount = AshGloom.amount(partialTick);
        if (amount > 0.0F) {
            cir.setReturnValue(AshGloom.darkenWorldLight(cir.getReturnValue(), amount));
        }
    }
}
