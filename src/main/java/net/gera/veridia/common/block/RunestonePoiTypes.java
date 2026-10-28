package net.gera.veridia.common.block;

import com.google.common.collect.ImmutableSet;
import net.gera.veridia.Veridia;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/**
 * Точки интереса рунных камней: мир сам ведёт список таких блоков, и моб находит ближайший без перебора блоков вокруг
 * (так жители ищут кровати и рабочие места). Точка - нижняя половина камня.
 */
public final class RunestonePoiTypes {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, Veridia.MOD_ID);

    public static final DeferredHolder<PoiType, PoiType> SPIRITUAL_RUNESTONE = POI_TYPES.register("spiritual_runestone",
            () -> new PoiType(lowerHalfStates(), 0, 1));

    private RunestonePoiTypes() {
    }

    private static Set<BlockState> lowerHalfStates() {
        return ModBlocks.SPIRITUAL_RUNESTONE.get().getStateDefinition().getPossibleStates().stream()
                .filter(state -> state.getValue(BloodyRunestoneBlock.HALF) == DoubleBlockHalf.LOWER)
                .collect(ImmutableSet.toImmutableSet());
    }

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
    }
}
