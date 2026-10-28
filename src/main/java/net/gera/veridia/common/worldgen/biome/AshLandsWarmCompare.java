package net.gera.veridia.common.worldgen.biome;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Часть инструмента карт ({@link AshLandsMapDump}): сравнивает долю тёплой суши в мире без Пепельных земель и с ними.
 * Считается по климату, без построения биомов:
 * <ul>
 *   <li>тёплая суша - температура климата не ниже WARM_TEMPERATURE (жаркий пояс ванили: пустыни, бесплодные земли,
 *       саванны, джунгли); жаркая и сухая - ещё и влажность не выше DRY_HUMIDITY;</li>
 *   <li>с землями: внутри пятен тёплых биомов нет (там пепельные), а в жарком поясе вокруг климат сдвигается к
 *       жаркому и сухому ({@link AshLands#heatedClimate}), так что тёплой суши там больше.</li>
 * </ul>
 */
final class AshLandsWarmCompare {
    private static final int HALF_SIZE = 10000;
    private static final int BLOCKS_PER_SAMPLE = 40;
    private static final double WARM_TEMPERATURE = 0.55D;
    private static final double DRY_HUMIDITY = -0.1D;
    // Те же числа, что у AshLands: куда климат сдвигается в жарком поясе
    private static final double HEAT_TEMPERATURE = 0.8D;
    private static final double HEAT_HUMIDITY = -0.5D;
    private static final float LAND_MIN_CONTINENTALNESS = -0.11F;

    private AshLandsWarmCompare() {
    }

    private record Row(long seed, long land, long warmWithout, long warmWith, long dryWithout, long dryWith,
                       long lostInZone, long gainedInBelt) {
    }

    static void write(RegistryAccess registries, List<Long> seeds, Path outDir) throws IOException {
        List<Row> rows = new ArrayList<>();
        for (long seed : seeds) {
            rows.add(row(registries, seed));
        }
        StringBuilder text = new StringBuilder();
        text.append("Тёплая суша по климату, без Пепельных земель и с ними (карта 20000 x 20000, шаг ")
                .append(BLOCKS_PER_SAMPLE).append(" блоков)\n");
        text.append("тёплая: температура >= ").append(WARM_TEMPERATURE).append("; жаркая и сухая: ещё и влажность <= ")
                .append(DRY_HUMIDITY).append("\n\n");
        double sumWithout = 0.0D;
        double sumWith = 0.0D;
        double sumDryWithout = 0.0D;
        double sumDryWith = 0.0D;
        for (Row row : rows) {
            double land = Math.max(1, row.land());
            sumWithout += row.warmWithout() / land;
            sumWith += row.warmWith() / land;
            sumDryWithout += row.dryWithout() / land;
            sumDryWith += row.dryWith() / land;
            text.append(String.format("сид %d: тёплой суши без земель %s, с землями %s (%+.1f п.п.); жаркой и сухой %s -> %s; "
                            + "потеряно в пятнах %s суши, прибавилось в поясе %s%n",
                    row.seed(), percent(row.warmWithout() / land), percent(row.warmWith() / land),
                    100.0D * (row.warmWith() - row.warmWithout()) / land, percent(row.dryWithout() / land),
                    percent(row.dryWith() / land), percent(row.lostInZone() / land), percent(row.gainedInBelt() / land)));
        }
        text.append(String.format("%nв среднем по %d сидам: тёплой суши без земель %s, с землями %s; жаркой и сухой %s -> %s%n",
                rows.size(), percent(sumWithout / rows.size()), percent(sumWith / rows.size()),
                percent(sumDryWithout / rows.size()), percent(sumDryWith / rows.size())));
        Files.writeString(outDir.resolve("warm_biomes_comparison.txt"), text.toString());
        drawChart(rows, outDir.resolve("warm_biomes_comparison.png"));
    }

    private static Row row(RegistryAccess registries, long seed) {
        NoiseGeneratorSettings settings = registries.lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        Climate.Sampler sampler = RandomState.create(settings, registries.lookupOrThrow(Registries.NOISE), seed).sampler();
        AshLandsLayout layout = AshLandsLayout.create(seed, (x, z) -> continentalness(sampler, x, z) >= LAND_MIN_CONTINENTALNESS);
        int cells = 2 * HALF_SIZE / BLOCKS_PER_SAMPLE;
        long[][] perRow = new long[cells][7];
        IntStream.range(0, cells).parallel().forEach(rowIndex -> {
            for (int column = 0; column < cells; column++) {
                count(layout, sampler, column * BLOCKS_PER_SAMPLE - HALF_SIZE, rowIndex * BLOCKS_PER_SAMPLE - HALF_SIZE,
                        perRow[rowIndex]);
            }
        });
        long[] total = new long[7];
        for (long[] counters : perRow) {
            for (int i = 0; i < total.length; i++) {
                total[i] += counters[i];
            }
        }
        return new Row(seed, total[0], total[1], total[2], total[3], total[4], total[5], total[6]);
    }

    // Счётчики: 0 - суши, 1 - тёплой без земель, 2 - с землями, 3 - жаркой и сухой без, 4 - с, 5 - тёплой, оказавшейся
    // внутри пятен, 6 - тёплой, которой без земель не было (появилась в жарком поясе)
    private static void count(AshLandsLayout layout, Climate.Sampler sampler, int x, int z, long[] counters) {
        if (continentalness(sampler, x, z) < LAND_MIN_CONTINENTALNESS) {
            return;
        }
        counters[0]++;
        DensityFunction.FunctionContext context = new DensityFunction.SinglePointContext(x, 0, z);
        double temperature = sampler.temperature().compute(context);
        double humidity = sampler.humidity().compute(context);
        boolean warmWithout = temperature >= WARM_TEMPERATURE;
        boolean dryWithout = warmWithout && humidity <= DRY_HUMIDITY;
        double heat = layout.heat(x, z);
        boolean inside = layout.contains(x, z);
        double shiftedTemperature = temperature + (HEAT_TEMPERATURE - temperature) * heat;
        double shiftedHumidity = humidity + (HEAT_HUMIDITY - humidity) * heat;
        boolean warmWith = !inside && shiftedTemperature >= WARM_TEMPERATURE;
        boolean dryWith = warmWith && shiftedHumidity <= DRY_HUMIDITY;
        counters[1] += warmWithout ? 1 : 0;
        counters[2] += warmWith ? 1 : 0;
        counters[3] += dryWithout ? 1 : 0;
        counters[4] += dryWith ? 1 : 0;
        counters[5] += warmWithout && inside ? 1 : 0;
        counters[6] += warmWith && !warmWithout ? 1 : 0;
    }

    private static double continentalness(Climate.Sampler sampler, int x, int z) {
        return sampler.continentalness().compute(new DensityFunction.SinglePointContext(x, 0, z));
    }

    // Столбики по сидам: доля тёплой суши без земель и с ними
    private static void drawChart(List<Row> rows, Path file) throws IOException {
        int width = 960;
        int height = 420;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(24, 24, 28));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.drawString("Доля тёплой суши (температура >= " + WARM_TEMPERATURE + "): без Пепельных земель и с ними", 20, 26);
        double max = 0.01D;
        for (Row row : rows) {
            double land = Math.max(1, row.land());
            max = Math.max(max, Math.max(row.warmWithout(), row.warmWith()) / land);
        }
        int chartTop = 70;
        int chartBottom = height - 60;
        int group = (width - 60) / rows.size();
        int bar = group / 3;
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            double land = Math.max(1, row.land());
            int x = 40 + i * group;
            int without = (int) ((chartBottom - chartTop) * (row.warmWithout() / land) / max);
            int with = (int) ((chartBottom - chartTop) * (row.warmWith() / land) / max);
            g.setColor(new Color(214, 176, 60));
            g.fillRect(x, chartBottom - without, bar, without);
            g.setColor(new Color(238, 98, 52));
            g.fillRect(x + bar + 2, chartBottom - with, bar, with);
            g.setColor(Color.WHITE);
            g.drawString(percent(row.warmWithout() / land), x - 2, chartBottom - without - 4);
            g.drawString(percent(row.warmWith() / land), x + bar + 2, chartBottom - with - 18);
            g.drawString(String.valueOf(row.seed()), x - 4, chartBottom + 16);
        }
        g.setColor(new Color(214, 176, 60));
        g.fillRect(40, height - 30, 14, 14);
        g.setColor(Color.WHITE);
        g.drawString("без Пепельных земель", 60, height - 18);
        g.setColor(new Color(238, 98, 52));
        g.fillRect(240, height - 30, 14, 14);
        g.setColor(Color.WHITE);
        g.drawString("с Пепельными землями (пятна и жаркий пояс)", 260, height - 18);
        g.dispose();
        ImageIO.write(image, "png", file.toFile());
    }

    private static String percent(double share) {
        return String.format("%.1f%%", share * 100.0D);
    }
}
