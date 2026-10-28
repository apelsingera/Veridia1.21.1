package net.gera.veridia.common.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import net.gera.veridia.Veridia;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * Регион Верхнего мира Veridia: ванильные биомы как есть плюс пепельные биомы, которые до них никогда не
 * доберутся. Сами пепельные земли ставит не климат, а {@link AshLands} (подмена биома по расстоянию от спавна),
 * но источнику биомов нужно знать о пепельных биомах заранее: по его набору строится порядок генерации фич,
 * и фичи биомов, которых в наборе нет, не ставятся.
 * <p>
 * Чтобы климатический поиск их не выбирал, точки ставятся с огромным offset: он добавляется к "расстоянию" точки
 * до искомого климата, и любая ванильная точка оказывается ближе.
 */
public class AshLandsRegion extends Region {
    // Вес региона роли не играет (он копия ванильного); минимальный, чтобы не менять раскладку регионов
    public static final int REGION_WEIGHT = 1;
    // Штраф точке (в единицах климата, от -2 до 2 по каждому параметру): больше любого расстояния до ванильных точек
    private static final float UNREACHABLE_OFFSET = 5.0F;

    public AshLandsRegion() {
        super(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "overworld_region"), RegionType.OVERWORLD, REGION_WEIGHT);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        // Пустой builder без замен отдаёт ванильную таблицу климата как есть
        addModifiedVanillaOverworldBiomes(mapper, builder -> { });
        ModBiomes.ASH_BIOMES.forEach(biome -> addUnreachableBiome(mapper, biome));
    }

    private void addUnreachableBiome(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper,
                                     ResourceKey<Biome> biome) {
        Climate.Parameter zero = Climate.Parameter.point(0.0F);
        addBiome(mapper, zero, zero, zero, zero, zero, zero, UNREACHABLE_OFFSET, biome);
    }
}
