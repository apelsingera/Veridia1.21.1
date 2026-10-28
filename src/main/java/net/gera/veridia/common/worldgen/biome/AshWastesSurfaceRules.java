package net.gera.veridia.common.worldgen.biome;

import net.gera.veridia.common.block.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

/**
 * Поверхность Пепельных равнин, Пепельного леса и пепельной реки: выжженная земля, как ванильная земля (верхний слой и слои
 * под ним). Под водой вся земля выжженная. На суше по шуму PATCH лежат пятна чёрного камня - их заметно меньше,
 * чем земли. Блоки blackstone_magma и лаву кладёт фича MagmaPatchFeature (она же убирает землю у лавовых озёр),
 * траву - фича в ModBiomes, а не правила поверхности.
 * Земля и чёрный камень - только на настоящей поверхности (abovePreliminarySurface): в пещерах пол и стены без них.
 * Весь остальной камень биома (склоны гор, где камень выходит из-под слоя земли, и пещеры) - опалённый камень
 * вместо обычного: правила действуют только на обычный камень и только выше MIN_Y.
 */
public class AshWastesSurfaceRules {
    // Правила действуют только выше этой высоты: ниже обычный камень и сланец, как в ваниле
    private static final int MIN_Y = 50;
    // Порог шума PATCH для пятен чёрного камня: выше порог - меньше камня (0.22 - около 20% суши, 0.34 - около 10%)
    private static final double BLACKSTONE_PATCH_NOISE_MIN = 0.22D;

    public static SurfaceRules.RuleSource create() {
        SurfaceRules.RuleSource incineratedDirt = SurfaceRules.state(ModBlocks.INCINERATED_DIRT.get().defaultBlockState());
        SurfaceRules.RuleSource blackstone = SurfaceRules.state(Blocks.BLACKSTONE.defaultBlockState());
        SurfaceRules.RuleSource singeStone = SurfaceRules.state(ModBlocks.SINGE_STONE.get().defaultBlockState());

        SurfaceRules.RuleSource ground = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.waterBlockCheck(-1, 0)), incineratedDirt),
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.PATCH, BLACKSTONE_PATCH_NOISE_MIN), blackstone),
                incineratedDirt);
        SurfaceRules.RuleSource biomeRule = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(ModBiomes.ASHEN_PLAINS, ModBiomes.ASHEN_FOREST, ModBiomes.ASHEN_RIVER),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), surfaceGround(ground)),
                        singeStone));
        return SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(MIN_Y), 0), biomeRule);
    }

    /** Верхний слой и слои под ним: так же, как у ванильной земли. */
    private static SurfaceRules.RuleSource surfaceGround(SurfaceRules.RuleSource ground) {
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, ground),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, ground));
    }
}
