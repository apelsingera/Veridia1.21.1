package net.gera.veridia.common.compat.cdg.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Горелка Create Diesel Generators обновляет состояние блока (уровень жара и LIT) только когда меняется уровень жара.
 * На слабом огне (жар от 0 до 1: обычное топливо при вентиле, открытом на 0.2) уровень остаётся NONE, как у потухшей
 * горелки, и LIT так и не становится true: горелка горит, а для Create (котлы, колонна TFMG) её нет - нагрев
 * Diesel Generators сообщает только горящей (LIT) горелке. Здесь LIT держится по жару и без смены уровня.
 * Цель задана строкой: Diesel Generators при сборке мода не нужен. Наследуем BlockEntity (он и так суперкласс цели),
 * чтобы видеть level.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(targets = "com.jesz.createdieselgenerators.content.burner.BurnerBlockEntity", remap = false)
public abstract class BurnerBlockEntityMixin extends BlockEntity {
    @Shadow(remap = false)
    public float heat;

    // Конструктор для компилятора: миксин сам по себе никто не создаёт
    private BurnerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // После своего tick: уровень жара (и LIT вместе с ним) горелка уже обновила, если он сменился
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void veridia$litByHeat(CallbackInfo ci) {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean burning = heat > 0.0F;
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) != burning) {
            level.setBlockAndUpdate(worldPosition, state.setValue(BlockStateProperties.LIT, burning));
        }
    }
}
