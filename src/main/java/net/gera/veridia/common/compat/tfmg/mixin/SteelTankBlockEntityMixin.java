package net.gera.veridia.common.compat.tfmg.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.gera.veridia.common.compat.tfmg.DistillationHeat;
import net.gera.veridia.common.compat.tfmg.DistillationReport;
import net.gera.veridia.common.compat.tfmg.DistillationTank;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Стальной бак TFMG как ректификационная колонна.
 * <ul>
 *     <li>к нагреву от Create добавляются горелки в слабом режиме и горелка Create Diesel Generators
 *     (см. {@link DistillationHeat});</li>
 *     <li>TFMG ищет контроллер колонны рядом с баком только при сборке бака и нигде не хранит результат: после
 *     перезахода в мир (а на клиенте - всегда) бак не считается колонной и нагрев остаётся 0. Главный блок бака
 *     проверяет это заново каждый lazyTick;</li>
 *     <li>контроллер колонны берёт нагрев у соседнего блока бака, а считает его только главный блок: остальные блоки
 *     копируют нагрев главного;</li>
 *     <li>подсказка очков обращается к главному блоку бака без проверки, а пока бак перестраивается (ломают блок),
 *     на клиенте его может не быть - игра вылетала.</li>
 * </ul>
 * Цели заданы строками: TFMG и Create при сборке мода не нужны. Наследуем BlockEntity (он и так суперкласс цели),
 * чтобы видеть level.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(targets = "com.drmangotea.tfmg.content.decoration.tanks.steel.SteelTankBlockEntity", remap = false)
public abstract class SteelTankBlockEntityMixin extends BlockEntity implements DistillationTank {
    @Shadow(remap = false)
    public int activeHeat;

    @Shadow(remap = false)
    public boolean isDistillationTower;

    // Что TFMG увидел под баком при последнем подсчёте нагрева - для /veridiadebug distillation
    @Unique
    private final List<DistillationReport.HeatSample> veridia$heatSamples = new ArrayList<>();

    // Конструктор для компилятора: миксин сам по себе никто не создаёт
    private SteelTankBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow(remap = false)
    public abstract boolean isController();

    @Shadow(remap = false)
    public abstract BlockPos getController();

    @Shadow(remap = false)
    public abstract void updateBoilerState();

    @Shadow(remap = false)
    public abstract boolean evaluate();

    @Shadow(remap = false)
    public abstract int getWidth();

    @Shadow(remap = false)
    public abstract int getHeight();

    @Inject(method = "updateTemperature", at = @At("HEAD"), remap = false)
    private void veridia$forgetHeatSamples(CallbackInfo ci) {
        veridia$heatSamples.clear();
    }

    @WrapOperation(method = "updateTemperature", remap = false, at = @At(value = "INVOKE", remap = false,
            target = "Lcom/simibubi/create/api/boiler/BoilerHeater;findHeat(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)F"))
    private float veridia$burnerHeat(Level level, BlockPos pos, BlockState state, Operation<Float> original) {
        float createHeat = original.call(level, pos, state);
        float heat = DistillationHeat.heat(createHeat, state);
        veridia$heatSamples.add(new DistillationReport.HeatSample(pos, state, createHeat, heat));
        return heat;
    }

    // До ванильного lazyTick: он считает нагрев, только если бак уже признан колонной
    @Inject(method = "lazyTick", at = @At("HEAD"), remap = false)
    private void veridia$refreshDistillation(CallbackInfo ci) {
        if (level == null) {
            return;
        }
        if (!isController()) {
            activeHeat = level.getBlockEntity(getController()) instanceof DistillationTank main
                    ? main.getDistillationHeat() : 0;
        } else if (level.isClientSide) {
            // На клиенте только сам признак колонны (для очков), окна и прочие блоки бака меняет сервер
            evaluate();
        } else {
            updateBoilerState();
        }
    }

    // Главного блока нет (getControllerBE() вернул бы null) - подсказки нет
    @Inject(method = "addToGoggleTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private void veridia$noTooltipWithoutController(List<Component> tooltip, boolean isPlayerSneaking,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (level == null || !isController() && !(level.getBlockEntity(getController()) instanceof DistillationTank)) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public int getDistillationHeat() {
        return activeHeat;
    }

    @Override
    public DistillationReport getDistillationReport() {
        return new DistillationReport(getController(), getWidth(), getHeight(), isDistillationTower, activeHeat,
                List.copyOf(veridia$heatSamples));
    }
}
