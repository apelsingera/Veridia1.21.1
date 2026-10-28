package net.gera.veridia.common.worldgen.biome;

import com.mojang.logging.LogUtils;
import net.gera.veridia.Veridia;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
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
 * Инструмент разработчика: рисует карты расположения Пепельных земель для списка сидов, без создания мира. Раскладка земель
 * считается по сиду и климату (континентальности) мира, как при старте настоящего сервера, поэтому карты показывают то
 * же, что будет в игре. Включается файлом {@code veridia-ashmaps.txt} в папке игры (в обычной игре его нет, и ничего не
 * происходит): строка {@code out <папка>} и строка {@code seeds <сид> <сид> ...}. После карт сервер сам останавливается.
 * <p>
 * На каждой карте: суша и океан (по климату), Пепельные земли (равнины и лес), переход рельефа к равнинному, жаркий пояс,
 * спавн, круг 2000 блоков, центры пятен и сетка. Рядом с картами пишется summary.txt с цифрами и найденными странностями.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class AshLandsMapDump {
    private static final Logger LOGGER = LogUtils.getLogger();

    // ---------- Настройки ----------
    // Сторона карты в блоках (от -HALF до HALF) и размер пикселя (блоков)
    private static final int HALF_SIZE = 10000;
    private static final int BLOCKS_PER_PIXEL = 20;
    private static final int MAP_PIXELS = 2 * HALF_SIZE / BLOCKS_PER_PIXEL;
    private static final int HEADER_HEIGHT = 70;
    private static final int FOOTER_HEIGHT = 40;
    // Шаг сетки (блоков) и радиус круга вокруг спавна, внутри которого пятен быть не должно
    private static final int GRID_STEP = 5000;
    private static final int FORBIDDEN_RADIUS = 2000;
    // Нужное для "земля" значение континентальности (то же, что в AshLands) и равнинность, с которой вода внутри заменяется сушей
    private static final float LAND_MIN_CONTINENTALNESS = -0.11F;
    private static final double DRY_FLATNESS = 0.85D;
    // Порог странности: доля земель, где вода остаётся (рельеф у края ещё не плоский, океан там не заменяется сушей)
    private static final double WATER_REMAINS_WARN = 0.05D;

    private AshLandsMapDump() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        Path config = FMLPaths.GAMEDIR.get().resolve("veridia-ashmaps.txt");
        if (!Files.exists(config)) {
            return;
        }
        MinecraftServer server = event.getServer();
        try {
            dump(server.registryAccess(), config);
        } catch (Exception e) {
            LOGGER.error("Карты Пепельных земель не построены", e);
        }
        server.halt(false);
    }

    private static void dump(RegistryAccess registries, Path config) throws IOException {
        System.setProperty("java.awt.headless", "true");
        Path outDir = null;
        List<Long> seeds = new ArrayList<>();
        for (String line : Files.readAllLines(config)) {
            String[] parts = line.trim().split("\\s+");
            if (parts[0].equals("out")) {
                outDir = Path.of(line.trim().substring(3).trim());
            } else if (parts[0].equals("seeds")) {
                for (int i = 1; i < parts.length; i++) {
                    seeds.add(Long.parseLong(parts[i]));
                }
            }
        }
        if (outDir == null) {
            throw new IOException("В veridia-ashmaps.txt нет строки out");
        }
        Files.createDirectories(outDir);
        StringBuilder summary = new StringBuilder();
        for (long seed : seeds) {
            summary.append(renderSeed(registries, seed, outDir)).append("\n");
        }
        Files.writeString(outDir.resolve("summary.txt"), summary.toString());
        AshLandsWarmCompare.write(registries, seeds, outDir);
        LOGGER.info("Карты Пепельных земель записаны в {}", outDir);
    }

    private static String renderSeed(RegistryAccess registries, long seed, Path outDir) throws IOException {
        NoiseGeneratorSettings settings = registries.lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        RandomState randomState = RandomState.create(settings, registries.lookupOrThrow(Registries.NOISE), seed);
        Climate.Sampler sampler = randomState.sampler();
        AshLandsLayout layout = AshLandsLayout.create(seed, (x, z) -> continentalness(sampler, x, z) >= LAND_MIN_CONTINENTALNESS);

        MapStats stats = new MapStats();
        BufferedImage image = new BufferedImage(MAP_PIXELS, HEADER_HEIGHT + MAP_PIXELS + FOOTER_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(24, 24, 28));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());

        int[] pixels = new int[MAP_PIXELS * MAP_PIXELS];
        long[][] counters = new long[MAP_PIXELS][6];
        IntStream.range(0, MAP_PIXELS).parallel().forEach(py -> {
            for (int px = 0; px < MAP_PIXELS; px++) {
                int x = px * BLOCKS_PER_PIXEL - HALF_SIZE + BLOCKS_PER_PIXEL / 2;
                int z = py * BLOCKS_PER_PIXEL - HALF_SIZE + BLOCKS_PER_PIXEL / 2;
                pixels[py * MAP_PIXELS + px] = pixelColor(layout, sampler, x, z, counters[py]);
            }
        });
        image.setRGB(0, HEADER_HEIGHT, MAP_PIXELS, MAP_PIXELS, pixels, 0, MAP_PIXELS);
        for (long[] row : counters) {
            stats.add(row);
        }

        List<AshLandsLayout.BlobInfo> blobs = layout.blobs(HALF_SIZE).stream()
                .filter(blob -> Math.abs(blob.centerX()) <= HALF_SIZE && Math.abs(blob.centerZ()) <= HALF_SIZE).toList();
        drawOverlays(g, blobs);
        String report = describe(seed, layout, blobs, stats);
        drawHeaderAndLegend(g, seed, blobs, stats, report);
        g.dispose();
        ImageIO.write(image, "png", outDir.resolve("ashlands_seed_" + seed + ".png").toFile());
        return report;
    }

    private static double continentalness(Climate.Sampler sampler, int x, int z) {
        return sampler.continentalness().compute(new DensityFunction.SinglePointContext(x, 0, z));
    }

    // Счётчики по строке: 0 - пикселей земель, 1 - из них лес, 2 - вода внутри земель, 3 - вода, оставшаяся внутри
    // (рельеф ещё не плоский), 4 - пикселей суши всего, 5 - внутри запретного круга
    private static int pixelColor(AshLandsLayout layout, Climate.Sampler sampler, int x, int z, long[] counters) {
        double continental = continentalness(sampler, x, z);
        boolean land = continental >= LAND_MIN_CONTINENTALNESS;
        if (land) {
            counters[4]++;
        }
        double flatness = layout.flatness(x, z);
        double heat = layout.heat(x, z);
        Color color = land ? new Color(88, 108, 78) : new Color(28, 56, 112);
        if (heat > 0.0D) {
            color = blend(color, new Color(214, 176, 60), heat * 0.55D);
        }
        if (flatness > 0.0D) {
            color = blend(color, new Color(150, 90, 190), flatness * 0.45D);
        }
        if (layout.contains(x, z)) {
            counters[0]++;
            boolean forest = layout.isForest(x, z);
            if (forest) {
                counters[1]++;
            }
            if (!land) {
                counters[2]++;
            }
            boolean waterStays = !land && flatness < DRY_FLATNESS;
            if (waterStays) {
                counters[3]++;
            }
            if ((double) x * x + (double) z * z < (double) FORBIDDEN_RADIUS * FORBIDDEN_RADIUS) {
                counters[5]++;
            }
            color = waterStays ? new Color(120, 48, 96) : forest ? new Color(150, 28, 28) : new Color(238, 98, 52);
        }
        return color.getRGB();
    }

    private static Color blend(Color from, Color to, double share) {
        double t = Math.max(0.0D, Math.min(1.0D, share));
        return new Color((int) (from.getRed() + (to.getRed() - from.getRed()) * t),
                (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * t));
    }

    private static int toPixelX(double x) {
        return (int) Math.round((x + HALF_SIZE) / BLOCKS_PER_PIXEL);
    }

    private static int toPixelY(double z) {
        return HEADER_HEIGHT + (int) Math.round((z + HALF_SIZE) / BLOCKS_PER_PIXEL);
    }

    private static void drawOverlays(Graphics2D g, List<AshLandsLayout.BlobInfo> blobs) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.setStroke(new BasicStroke(1.0F));
        g.setColor(new Color(255, 255, 255, 60));
        for (int v = -HALF_SIZE; v <= HALF_SIZE; v += GRID_STEP) {
            g.drawLine(toPixelX(v), HEADER_HEIGHT, toPixelX(v), HEADER_HEIGHT + MAP_PIXELS);
            g.drawLine(0, toPixelY(v), MAP_PIXELS, toPixelY(v));
            g.drawString(String.valueOf(v), Math.min(toPixelX(v) + 3, MAP_PIXELS - 40), HEADER_HEIGHT + 12);
            g.drawString(String.valueOf(v), 3, toPixelY(v) - 3);
        }
        g.setColor(Color.WHITE);
        int radius = FORBIDDEN_RADIUS / BLOCKS_PER_PIXEL;
        g.drawOval(toPixelX(0) - radius, toPixelY(0) - radius, 2 * radius, 2 * radius);
        g.fillOval(toPixelX(0) - 4, toPixelY(0) - 4, 8, 8);
        g.drawString("спавн (0, 0), круг 2000", toPixelX(0) + 8, toPixelY(0) - 6);
        for (AshLandsLayout.BlobInfo blob : blobs) {
            g.setColor(blob.home() ? Color.CYAN : Color.YELLOW);
            int cx = toPixelX(blob.centerX());
            int cy = toPixelY(blob.centerZ());
            g.drawLine(cx - 6, cy, cx + 6, cy);
            g.drawLine(cx, cy - 6, cx, cy + 6);
            g.drawString((blob.home() ? "дом " : "дальнее ") + (int) blob.centerX() + " " + (int) blob.centerZ()
                    + " r" + (int) blob.radius(), cx + 8, cy - 8);
        }
    }

    private static void drawHeaderAndLegend(Graphics2D g, long seed, List<AshLandsLayout.BlobInfo> blobs, MapStats stats,
                                            String report) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.setColor(Color.WHITE);
        g.drawString("Пепельные земли, сид " + seed + ", " + 2 * HALF_SIZE + " x " + 2 * HALF_SIZE + " блоков, пиксель "
                + BLOCKS_PER_PIXEL + " блоков (север сверху)", 10, 22);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        String[] lines = report.split("\n");
        for (int i = 1; i < Math.min(lines.length, 4); i++) {
            g.drawString(lines[i].trim(), 10, 22 + 16 * i);
        }
        int y = HEADER_HEIGHT + MAP_PIXELS + 26;
        int x = 10;
        x = legendItem(g, x, y, new Color(88, 108, 78), "суша");
        x = legendItem(g, x, y, new Color(28, 56, 112), "океан");
        x = legendItem(g, x, y, new Color(214, 176, 60), "жаркий пояс");
        x = legendItem(g, x, y, new Color(150, 90, 190), "переход рельефа");
        x = legendItem(g, x, y, new Color(238, 98, 52), "равнины");
        x = legendItem(g, x, y, new Color(150, 28, 28), "лес");
        legendItem(g, x, y, new Color(120, 48, 96), "вода внутри земель");
    }

    private static int legendItem(Graphics2D g, int x, int y, Color color, String text) {
        g.setColor(color);
        g.fillRect(x, y - 11, 14, 14);
        g.setColor(Color.WHITE);
        g.drawString(text, x + 19, y);
        return x + 19 + g.getFontMetrics().stringWidth(text) + 22;
    }

    private static String describe(long seed, AshLandsLayout layout, List<AshLandsLayout.BlobInfo> blobs, MapStats stats) {
        AshLandsLayout.BlobInfo home = blobs.get(0);
        double homeDistance = Math.hypot(home.centerX(), home.centerZ());
        double total = (double) MAP_PIXELS * MAP_PIXELS;
        double zone = Math.max(1.0D, stats.zonePixels);
        double waterShare = stats.waterInZone / zone;
        List<String> warnings = new ArrayList<>();
        if (stats.insideForbidden > 0) {
            warnings.add("ЗЕМЛИ ВНУТРИ КРУГА 2000 БЛОКОВ: " + stats.insideForbidden + " пикселей");
        }
        if (layout.heat(0, 0) > 0.0D) {
            warnings.add("спавн в жарком поясе (жар " + String.format("%.2f", layout.heat(0, 0)) + ")");
        }
        if (layout.flatness(0, 0) > 0.0D) {
            warnings.add("спавн в зоне выравнивания рельефа");
        }
        if (stats.waterRemains / zone > WATER_REMAINS_WARN) {
            warnings.add("вода остаётся внутри у края: " + percent(stats.waterRemains / zone) + " земель");
        }
        return "сид " + seed + "\n"
                + "дом: центр " + (int) home.centerX() + " " + (int) home.centerZ() + ", расстояние " + (int) homeDistance
                + ", ближний край " + (int) (homeDistance - home.radius()) + "; дальних пятен: " + (blobs.size() - 1) + "\n"
                + "земель " + percent(stats.zonePixels / total) + " карты (лес " + percent(stats.forestPixels / zone)
                + " земель, океан по климату внутри " + percent(waterShare) + " (станет сушей, рельеф там плоский), вода у края " + percent(stats.waterRemains / zone) + "), суши на карте " + percent(stats.landPixels / total) + "\n"
                + "странности: " + (warnings.isEmpty() ? "нет" : String.join("; ", warnings));
    }

    private static String percent(double share) {
        return String.format("%.1f%%", share * 100.0D);
    }

    // Суммы счётчиков по строкам карты
    private static final class MapStats {
        long zonePixels;
        long forestPixels;
        long waterInZone;
        long waterRemains;
        long landPixels;
        long insideForbidden;

        void add(long[] row) {
            zonePixels += row[0];
            forestPixels += row[1];
            waterInZone += row[2];
            waterRemains += row[3];
            landPixels += row[4];
            insideForbidden += row[5];
        }
    }
}
