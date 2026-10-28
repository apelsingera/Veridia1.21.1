package net.gera.veridia.common.client;

import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Цвет пепельной травы. Текстура серая, итоговый цвет - она, умноженная на цвет отсюда: на равнинах самый яркий цвет
 * обожжённой листвы (серо-красный, см. {@link AshLeavesTint#BRIGHT_RED}), в лесу и у реки зелёный, как у прежней текстуры.
 */
public final class AshGrassTint {
    // Зелёный множитель: серая текстура с ним близка к прежней оливково-зелёной (ashen_grass_before.png). Подгоняется здесь
    private static final int GREEN = 0xFFFFB0;

    private AshGrassTint() {
    }

    public static int blockColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        if (pos == null || Minecraft.getInstance().level == null) {
            return GREEN;
        }
        boolean plains = Minecraft.getInstance().level.getBiome(pos).is(ModBiomes.ASHEN_PLAINS);
        return plains ? AshLeavesTint.BRIGHT_RED : GREEN;
    }
}
