package net.gera.veridia.common.village.calc;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Материал видимых filler-столбиков (заполнение между кусками стены, НЕ фундамент-подпорка под
 * ними - та по требованию игрока остаётся одними хлам-досками стиля, VillageStyle.scrapWood) по слоям высоты.
 * Раньше весь столбик был одним случайным блоком на всю высоту (см. историю FillerVariant/
 * pickFillerBlock в NailerWallQueue) - на длинной стене вперемешку с разной высотой рельефа
 * выглядело монотонно. Тут материал слоистый: низ (закопанный блок + первый видимый ряд) чаще
 * тяжёлые/плотные материалы, верх (оставшиеся 3 видимых ряда) - легче. У tier1 на фиксированной
 * высоте окошко из ROPE_FENCE, у tier2 окошек нет - вместо них редкий (~8%) вариант "бревенчатый
 * столб" (низ как обычно, верх целиком обтёсанные брёвна).
 */
public final class FillerDecoration {

    private FillerDecoration() {
    }

    private record Variant(Block block, int weight) {}

    /** farmersdelight:rope_fence - "верёвочный забор" по формулировке игрока. Уже числился в
     * NailerWallQueue.KNOWN_BLOCKS/pointCost на будущее - тут наконец реально используется. */
    public static final Block ROPE_FENCE =
            BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("farmersdelight", "rope_fence"));

    // Материалы зависят от стиля деревни (VillageStyle): хлам-доски, доски и обтёсанное бревно - породы
    // стиля (дуб на равнинах, ель в тайге), а грязь заменяется тем, что у стиля вместо неё (в снежной
    // деревне - снег и лёд, см. VillageStyle.MudSubstitutes).
    private static List<Variant> tier1Low(VillageStyle style) {
        return List.of(
                new Variant(style.scrapWood(), 4),
                new Variant(style.planks(), 8),
                new Variant(style.packedMud(), 6),
                new Variant(style.strippedLog(), 2));
    }

    private static List<Variant> tier1Top(VillageStyle style) {
        return List.of(
                new Variant(style.scrapWood(), 12),
                new Variant(style.planks(), 6),
                new Variant(style.packedMud(), 1),
                new Variant(style.strippedLog(), 2));
    }

    private static List<Variant> tier2Low(VillageStyle style) {
        List<Variant> variants = new ArrayList<>(List.of(
                new Variant(style.scrapWood(), 3),
                new Variant(style.planks(), 4),
                new Variant(style.packedMud(), 2),
                new Variant(style.strippedLog(), 1),
                new Variant(style.mud(), 6)));
        variants.addAll(brickVariants(style, 5));
        return variants;
    }

    private static List<Variant> tier2Top(VillageStyle style) {
        List<Variant> variants = new ArrayList<>(List.of(
                new Variant(style.scrapWood(), 4),
                new Variant(style.planks(), 8),
                new Variant(style.packedMud(), 1),
                new Variant(style.strippedLog(), 2),
                new Variant(style.mud(), 1)));
        variants.addAll(brickVariants(style, 2));
        return variants;
    }

    /** Кирпичи стиля: общий вес делится между их видами поровну, остаток достаётся первым (5 на два вида = 3 + 2). */
    private static List<Variant> brickVariants(VillageStyle style, int totalWeight) {
        List<Block> bricks = style.mudBricks();
        List<Variant> variants = new ArrayList<>(bricks.size());
        for (int i = 0; i < bricks.size(); i++) {
            int weight = totalWeight / bricks.size() + (i < totalWeight % bricks.size() ? 1 : 0);
            variants.add(new Variant(bricks.get(i), weight));
        }
        return variants;
    }

    /** "нижний 1 блок - низ, остальные 3 - верх" (4 видимых блока, dy=1..4) - закопанный dy=0
     * приравнен к низу заодно, отдельно игрок его не оговаривал. */
    private static final int LOW_LAYER_MAX_DY = 1;

    /** "высота 3" считая от низа ВИДИМОЙ части (видимые - dy=1..4, это 3-й из них) = dy=3. */
    private static final int WINDOW_DY = 3;

    private static final double LOG_COLUMN_CHANCE = 0.08;

    /** Один блок на каждый dy столбика (0..FILLER_HEIGHT-1), детерминированно по (cellX,cellZ) -
     * тот же принцип сида, что и everywhere else в этом пакете (NailerWallQueue.pickPieceFor и
     * т.д.), чтобы /calculate и настоящий nailer на одной и той же клетке всегда выбирали одно
     * и то же. */
    public static List<Block> columnBlocks(int cellX, int cellZ, boolean upgraded, VillageStyle style) {
        int height = VillageCalcSettings.FILLER_HEIGHT;
        long seed = (((long) cellX) << 32) ^ (cellZ & 0xffffffffL);
        Random rng = new Random(seed);
        List<Block> blocks = new ArrayList<>(height);

        if (upgraded && rng.nextDouble() < LOG_COLUMN_CHANCE) {
            for (int dy = 0; dy < height; dy++) {
                blocks.add(dy <= LOW_LAYER_MAX_DY ? pick(rng, tier2Low(style)) : style.strippedLog());
            }
            return blocks;
        }

        List<Variant> low = upgraded ? tier2Low(style) : tier1Low(style);
        List<Variant> top = upgraded ? tier2Top(style) : tier1Top(style);
        boolean windowFits = !upgraded && hasRoomForWindow(WINDOW_DY, height);
        for (int dy = 0; dy < height; dy++) {
            if (windowFits && dy == WINDOW_DY) {
                blocks.add(ROPE_FENCE);
                continue;
            }
            blocks.add(pick(rng, dy <= LOW_LAYER_MAX_DY ? low : top));
        }
        return blocks;
    }

    /** "не ставить, если вокруг меньше 2 блоков" - задел на будущее на случай, если высота
     * столбика когда-нибудь станет переменной; при нынешней фиксированной FILLER_HEIGHT=5 всегда
     * true (окошко не на самом краю столбика, и по обе стороны есть хотя бы что-то). */
    private static boolean hasRoomForWindow(int dy, int height) {
        return height >= 3 && dy > 0 && dy < height - 1;
    }

    private static Block pick(Random rng, List<Variant> variants) {
        int total = 0;
        for (Variant variant : variants) {
            total += variant.weight();
        }
        int roll = rng.nextInt(total);
        for (Variant variant : variants) {
            roll -= variant.weight();
            if (roll < 0) {
                return variant.block();
            }
        }
        return variants.getFirst().block();
    }
}
