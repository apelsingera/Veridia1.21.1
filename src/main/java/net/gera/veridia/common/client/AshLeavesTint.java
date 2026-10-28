package net.gera.veridia.common.client;

import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.jetbrains.annotations.Nullable;

/**
 * Цвет обожжённой листвы: плавный градиент от серо-красного до более яркого красного (но всё ещё серо-красного). Цвет
 * зависит от места: сглаженный шум по координатам даёт большие пятна, которые перетекают друг в друга без резких
 * границ, поэтому одно дерево и соседние выглядят по-разному, но без пестроты. Тот же цвет получает и листва Dynamic
 * Trees: он берёт цвет у блока обожжённой листвы.
 * <p>
 * Текстура еловой листвы серая, и итоговый цвет - она, умноженная на цвет отсюда.
 */
public final class AshLeavesTint {
    // Концы градиента: серо-красный и более яркий красный. Подгоняются здесь
    private static final int GRAY_RED = 0x9C7C7C;
    public static final int BRIGHT_RED = 0xC85252;

    // Размер пятен одного цвета в блоках (больше - медленнее переходы), по горизонтали и по вертикали
    private static final double PATCH_SIZE_HORIZONTAL = 22.0;
    private static final double PATCH_SIZE_VERTICAL = 14.0;
    // Вторая, мелкая составляющая шума: чуть разнообразит цвет внутри пятна; её доля в итоговом значении
    private static final double DETAIL_PATCH_SIZE = 6.0;
    private static final double DETAIL_WEIGHT = 0.25;
    // Растяжение шума: простой шум редко доходит до краёв, с множителем градиент используется целиком
    private static final double NOISE_GAIN = 1.25;

    private static final long NOISE_SEED = 0x4153485F4C454156L;
    private static final SimplexNoise NOISE = new SimplexNoise(new XoroshiroRandomSource(NOISE_SEED));

    private AshLeavesTint() {
    }

    /** Цвет блока листвы в точке pos (BlockColor); без точки (предмет, частицы) - середина градиента. */
    public static int blockColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        return pos == null ? midColor() : colorAt(pos);
    }

    /**
     * Цвет ванильной лозы: в пепельных биомах такой же, как у обожжённой листвы в этой точке, в остальных - обычный
     * цвет листвы биома, как у ванили.
     */
    public static int vineColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        if (level == null || pos == null) {
            return FoliageColor.getDefaultColor();
        }
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel != null && ModBiomes.isAshBiome(clientLevel.getBiome(pos))) {
            return colorAt(pos);
        }
        return BiomeColors.getAverageFoliageColor(level, pos);
    }

    /** Цвет предмета листвы в инвентаре (ItemColor). */
    public static int itemColor(ItemStack stack, int tintIndex) {
        return midColor();
    }

    private static int colorAt(BlockPos pos) {
        double x = pos.getX() / PATCH_SIZE_HORIZONTAL;
        double y = pos.getY() / PATCH_SIZE_VERTICAL;
        double z = pos.getZ() / PATCH_SIZE_HORIZONTAL;
        double large = NOISE.getValue(x, y, z);
        double detail = NOISE.getValue(pos.getX() / DETAIL_PATCH_SIZE, pos.getY() / DETAIL_PATCH_SIZE, pos.getZ() / DETAIL_PATCH_SIZE);
        double noise = large * (1.0 - DETAIL_WEIGHT) + detail * DETAIL_WEIGHT;
        double t = Mth.clamp(0.5 + noise * 0.5 * NOISE_GAIN, 0.0, 1.0);
        return gradient(t * t * (3.0 - 2.0 * t));
    }

    private static int midColor() {
        return gradient(0.5);
    }

    // Точка градиента: 0 - серо-красный, 1 - яркий красный
    private static int gradient(double t) {
        int red = channel(GRAY_RED >> 16, BRIGHT_RED >> 16, t);
        int green = channel(GRAY_RED >> 8, BRIGHT_RED >> 8, t);
        int blue = channel(GRAY_RED, BRIGHT_RED, t);
        return (red << 16) | (green << 8) | blue;
    }

    private static int channel(int from, int to, double t) {
        return (int) Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
    }
}
