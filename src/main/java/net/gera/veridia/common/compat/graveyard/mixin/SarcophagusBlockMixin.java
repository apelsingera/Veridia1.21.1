package net.gera.veridia.common.compat.graveyard.mixin;

import net.gera.veridia.common.compat.graveyard.CoffinTrap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Гробы и саркофаг Graveyard - один класс SarcophagusBlock, поэтому ловушка (см. {@link CoffinTrap}) работает у всех
 * сразу. Цель задана строкой: Graveyard нужен только при запуске, при сборке мода он не нужен.
 */
@Mixin(targets = "com.moomba.graveyard.blocks.SarcophagusBlock", remap = false)
public abstract class SarcophagusBlockMixin {
    // Перед обычным открытием: если в гробе ловушка, запускаем вылезание моба вместо меню
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void veridia$mobTrapOnOpen(BlockState state, Level level, BlockPos pos, Player player,
                                       BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (level instanceof ServerLevel serverLevel && CoffinTrap.onOpenAttempt(serverLevel, pos, state, player)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
