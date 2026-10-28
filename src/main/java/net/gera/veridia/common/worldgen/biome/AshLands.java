package net.gera.veridia.common.worldgen.biome;

import com.mojang.logging.LogUtils;
import net.gera.veridia.Veridia;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Пепельные земли поверх обычной генерации биомов: в областях из {@link AshLandsLayout} любой наземный биом Верхнего
 * мира (в том числе горные) заменяется Пепельными равнинами или Пепельным лесом, река - Пепельной рекой.
 * Пещерные биомы остаются ванильными. Океаны, реки и пляжи заменяются там, где рельеф уже полностью равнинный (см.
 * DRY_FLATNESS), а ближе к краю остаются, чтобы вода не висела в воздухе. Вокруг земель лежит жаркий пояс (HEAT_BELT_MIN):
 * климат смещается к жаркому и сухому, чтобы рядом не было снега. Замену делает миксин на MultiNoiseBiomeSource, а
 * расположение областей считается при старте сервера по сиду и климату мира. Рельеф биом не задаёт: поэтому внутри земель
 * его выравнивает функция плотности ash_flatness (см. {@link AshLandsLayout#flatness}).
 * <p>
 * Пепельные биомы должны быть в наборе биомов источника (иначе их фичи не генерируются), поэтому их заводит регион
 * {@link AshLandsRegion}; естественным образом они там не выпадают.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class AshLands {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Пещерные биомы Верхнего мира: их не трогаем, чтобы пещеры внутри пепельных земель остались ванильными
    private static final List<ResourceKey<Biome>> CAVE_BIOMES = List.of(
            Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK);

    // Ниже этой континентальности климата океан и берег, выше - внутренняя суша
    private static final float LAND_MIN_CONTINENTALNESS = -0.11F;

    // С какой степени равнинности рельефа (0..1) вода и берега внутри земель тоже заменяются сушей
    private static final double DRY_FLATNESS = 0.85D;

    // Климат жаркого пояса вокруг земель (от -1 до 1): температура 0.8 - жаркий пояс ванильной таблицы (пустыни, бесплодные
    // земли), влажность -0.5 - сухой. Выше температура и суше - меньше умеренных биомов между пеплом и обычным миром.
    private static final float HEAT_TEMPERATURE = 0.8F;
    private static final float HEAT_HUMIDITY = -0.5F;

    // null, пока мир не запущен: тогда ничего не заменяется
    private static volatile AshLandsLayout layout;

    private AshLands() {
    }

    // Сервер уже создал уровни (у Верхнего мира есть климат), но нужные пепельным землям чанки далеко от спавна,
    // поэтому до этого момента ничего заменять не пришлось.
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        Climate.Sampler sampler = event.getServer().overworld().getChunkSource().randomState().sampler();
        long seed = event.getServer().getWorldData().worldGenOptions().seed();
        layout = AshLandsLayout.create(seed, (x, z) -> isLand(sampler, x, z));
        LOGGER.info("Пепельные земли: домашнее пятно с центром около {} {}", layout.homeCenter().getX(), layout.homeCenter().getZ());
    }

    // Суша ли в блоке: по континентальности климата, без построения рельефа. Берётся только функция континентальности,
    // а не весь Sampler.sample: тот считает ещё и depth, а depth зависит от рельефа Пепельных земель (ash_flatness), то есть
    // от самой раскладки земель, которую мы тут как раз считаем, и вышел бы замкнутый круг.
    private static boolean isLand(Climate.Sampler sampler, int x, int z) {
        double continentalness = sampler.continentalness().compute(new DensityFunction.SinglePointContext(x, 0, z));
        return continentalness >= LAND_MIN_CONTINENTALNESS;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        layout = null;
    }

    /** Лежит ли блок (x, z) в Пепельных землях. Дешёвая проверка: почти весь мир отсекается сразу. */
    public static boolean isInside(int x, int z) {
        AshLandsLayout current = layout;
        return current != null && current.contains(x, z);
    }

    /**
     * Насколько рельеф в блоке (x, z) должен быть равнинным: 0 - обычный, 1 - ровные равнины (см.
     * {@link AshLandsLayout#flatness}). Читает функция плотности ash_flatness, она же задаёт рельеф мира, поэтому
     * вызывается очень часто и из потоков генерации.
     */
    public static double flatness(int x, int z) {
        AshLandsLayout current = layout;
        return current == null ? 0.0D : current.flatness(x, z);
    }

    /**
     * Какой пепельный биом должен стоять вместо исходного в блоке (x, z), или null, если заменять не надо.
     *
     * @param original биом, который выбрала обычная генерация
     */
    @Nullable
    public static ResourceKey<Biome> replacement(Holder<Biome> original, int x, int z) {
        AshLandsLayout current = layout;
        if (current == null || !current.contains(x, z) || !original.is(BiomeTags.IS_OVERWORLD)
                || CAVE_BIOMES.stream().anyMatch(original::is)) {
            return null;
        }
        // Там, где рельеф уже полностью равнинный, воды нет: океаны, реки и пляжи тоже становятся сушей. У края, где
        // рельеф ещё переходит в обычный, вода остаётся как есть (реки - пепельные), чтобы вода и берега не висели в воздухе.
        boolean flatEnough = current.flatness(x, z) >= DRY_FLATNESS;
        if (original.is(BiomeTags.IS_RIVER) && !flatEnough) {
            return ModBiomes.ASHEN_RIVER;
        }
        boolean shore = original.is(BiomeTags.IS_OCEAN) || original.is(BiomeTags.IS_BEACH);
        if (shore && !flatEnough) {
            return null;
        }
        return current.isForest(x, z) ? ModBiomes.ASHEN_FOREST : ModBiomes.ASHEN_PLAINS;
    }

    /**
     * Насколько климат в блоке (x, z) смещён к жаркому и сухому (см. {@link AshLandsLayout#heat}): 0 - обычный,
     * 1 - полностью. Вокруг Пепельных земель лежит жаркий пояс, чтобы рядом не оказывалось снега.
     */
    public static double heat(int x, int z) {
        AshLandsLayout current = layout;
        return current == null ? 0.0D : current.heat(x, z);
    }

    /** Климат точки, смещённый к жаркому и сухому на долю heat (от 0 до 1). */
    public static Climate.TargetPoint heatedClimate(Climate.TargetPoint target, double heat) {
        long temperature = Math.round(Mth.lerp(heat, target.temperature(), Climate.quantizeCoord(HEAT_TEMPERATURE)));
        long humidity = Math.round(Mth.lerp(heat, target.humidity(), Climate.quantizeCoord(HEAT_HUMIDITY)));
        return new Climate.TargetPoint(temperature, humidity, target.continentalness(), target.erosion(),
                target.depth(), target.weirdness());
    }

    /** Пепельные биомы из набора биомов источника: Holder нужен, чтобы подставить его как результат. */
    public static Map<ResourceKey<Biome>, Holder<Biome>> findAshHolders(Set<Holder<Biome>> possibleBiomes) {
        Map<ResourceKey<Biome>, Holder<Biome>> holders = new HashMap<>();
        for (Holder<Biome> biome : possibleBiomes) {
            biome.unwrapKey()
                    .filter(ModBiomes.ASH_BIOMES::contains)
                    .ifPresent(key -> holders.put(key, biome));
        }
        return holders;
    }
}
