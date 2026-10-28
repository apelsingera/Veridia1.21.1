package net.gera.veridia.common.compat.tfmg.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Контроллер ректификационной колонны TFMG перегоняет и на клиенте. Раньше нагрев бака на клиенте всегда был 0 и
 * перегонка там не шла; теперь клиент знает нагрев (SteelTankBlockEntityMixin, для очков), поэтому перегоняет только
 * сервер, а жидкости приходят клиенту с его данными.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(targets = "com.drmangotea.tfmg.content.machinery.oil_processing.distillation_tower.controller.DistillationControllerBlockEntity", remap = false)
public abstract class DistillationControllerBlockEntityMixin extends BlockEntity {
    // Конструктор для компилятора: миксин сам по себе никто не создаёт
    private DistillationControllerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "manageRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void veridia$serverOnly(CallbackInfo ci) {
        if (level != null && level.isClientSide) {
            ci.cancel();
        }
    }
}
