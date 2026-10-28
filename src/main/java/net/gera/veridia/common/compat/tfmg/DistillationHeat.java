package net.gera.veridia.common.compat.tfmg;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Нагрев ректификационной колонны TFMG. TFMG берёт нагрев блоков под стальным баком у Create
 * ({@code BoilerHeater.findHeat}), считает только положительный и складывает его в целое число, отбрасывая дробь.
 * Create за горелку в слабом режиме (SMOULDERING) даёт 0, а горелка Create Diesel Generators сообщает свой жар как
 * есть, часто дробный (0.6 на слабом топливе или с прикрытым вентилем), и колонне от неё доставалось 0.
 * Поэтому любой положительный нагрев считается хотя бы слабым, а нулевой - по уровню жара горелки: любой блок со
 * свойством уровня жара (BlazeBurnerBlock.HeatLevel), а если у него есть LIT - только пока он горит.
 * Миксин: compat/tfmg/mixin/SteelTankBlockEntityMixin.
 */
public final class DistillationHeat {
    // Нагрев за уровень жара (у Create нагретая горелка даёт 1, раскалённая 2; колонна работает со скоростью нагрев/2)
    private static final float LOW_HEAT = 1.0F;
    private static final float HEATED = 1.0F;
    private static final float SUPERHEATED = 2.0F;
    private static final String HEAT_LEVEL_ENUM = "HeatLevel";

    private DistillationHeat() {
    }

    /** Нагрев блока под баком: что дал Create (не меньше слабого), а если ничего - по уровню жара горелки. */
    public static float heat(float createHeat, BlockState state) {
        if (createHeat > 0.0F) {
            return Math.max(createHeat, LOW_HEAT);
        }
        boolean hasFlame = state.hasProperty(BlockStateProperties.LIT);
        if (hasFlame && !state.getValue(BlockStateProperties.LIT)) {
            return createHeat;
        }
        String heatLevel = burnerHeatLevel(state);
        if (heatLevel == null) {
            return createHeat;
        }
        return switch (heatLevel) {
            // Горелка Diesel Generators на слабом топливе горит, но уровень у неё NONE (жар ниже 1.0) - тоже слабый
            // нагрев; у пустой горелки Create (без LIT) NONE - нагрева нет
            case "NONE" -> hasFlame ? LOW_HEAT : createHeat;
            case "SMOULDERING", "FADING" -> LOW_HEAT;
            case "KINDLED" -> HEATED;
            case "SEETHING" -> SUPERHEATED;
            default -> createHeat;
        };
    }

    // Уровень жара горелки по имени (SMOULDERING, KINDLED...), если у блока есть такое свойство; иначе null
    private static String burnerHeatLevel(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            Object value = state.getValue(property);
            if (value instanceof Enum<?> level && level.getDeclaringClass().getSimpleName().equals(HEAT_LEVEL_ENUM)) {
                return level.name();
            }
        }
        return null;
    }
}
