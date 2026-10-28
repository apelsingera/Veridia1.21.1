package net.gera.veridia.common.worldgen.feature;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Куча сырой молибденовой руды в лавовом озере: высокая неровная башня, широкая у основания, суженная в середине (шея) и с
 * утолщением сверху (голова), чуть свисающим на одну сторону. Форма - объединение шаров (комков), поэтому она получается
 * бугристой, а любой блок мира легко проверить на принадлежность куче, не строя её целиком: куча считается по числам и
 * больше ни от чего не зависит, поэтому каждый чанк строит свою часть сам и части сходятся.
 * <p>
 * Координаты - от оси кучи: x и z в блоках, высота (слой) от 0 - основания кучи.
 *
 * @param x      смещение кучи от центра озера по X
 * @param z      смещение кучи от центра озера по Z
 * @param height высота кучи (слоёв над основанием)
 * @param lumps  комки, из которых куча состоит
 */
record OrePile(int x, int z, int height, List<Lump> lumps) {
    // ---------- Настройки формы ----------
    // Высота кучи (блоков, от и до)
    private static final int MIN_HEIGHT = 16;
    private static final int MAX_HEIGHT = 21;
    // Основание: сколько комков, их радиус (блоков, от и до) и на сколько комки разбросаны вокруг оси
    private static final int BASE_LUMPS = 3;
    private static final double BASE_RADIUS_MIN = 2.0D;
    private static final double BASE_RADIUS_MAX = 2.7D;
    private static final double BASE_SPREAD = 1.2D;
    // Шея: расстояние между комками по высоте, их радиус (от и до) и насколько ось шеи может уйти в сторону от оси кучи
    private static final double NECK_FROM_LAYER = 3.0D;
    private static final double NECK_STEP = 2.5D;
    private static final double NECK_RADIUS_MIN = 2.0D;
    private static final double NECK_RADIUS_MAX = 2.6D;
    private static final double NECK_WANDER = 1.8D;
    private static final double NECK_STEP_SHIFT = 0.6D;
    // Голова: сколько комков, на сколько слоёв ниже верха кучи лежит её середина, радиус (от и до) и вынос комков в сторону от оси шеи
    private static final int HEAD_LUMPS = 3;
    private static final double HEAD_BELOW_TOP = 4.2D;
    private static final double HEAD_HEIGHT_SPREAD = 0.6D;
    private static final double HEAD_RADIUS_MIN = 2.8D;
    private static final double HEAD_RADIUS_MAX = 3.6D;
    private static final double HEAD_OVERHANG = 2.6D;
    // Макушка: комок над головой
    private static final double TOP_BELOW_TOP = 1.8D;
    private static final double TOP_RADIUS = 1.8D;

    /** Один комок кучи: шар. */
    record Lump(double x, double y, double z, double radius) {
        boolean contains(double px, double py, double pz) {
            double dx = px - x;
            double dy = py - y;
            double dz = pz - z;
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }
    }

    /** Новая куча со смещением (x, z) от центра озера и случайной формой. */
    static OrePile create(RandomSource random, int x, int z) {
        int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
        List<Lump> lumps = new ArrayList<>();
        addBase(random, lumps);
        double[] neckAxis = addNeck(random, lumps, height);
        addHead(random, lumps, height, neckAxis[0], neckAxis[1]);
        lumps.add(new Lump(neckAxis[0], height - TOP_BELOW_TOP, neckAxis[1], TOP_RADIUS));
        return new OrePile(x, z, height, List.copyOf(lumps));
    }

    /** Входит ли блок в кучу: dx и dz - от оси кучи, layer - слой над основанием (ниже основания считается как 0). */
    boolean contains(int dx, int layer, int dz) {
        if (layer > height) {
            return false;
        }
        for (Lump lump : lumps) {
            if (lump.contains(dx, Math.max(layer, 0), dz)) {
                return true;
            }
        }
        return false;
    }

    /** Самое дальнее, куда куча доходит от своей оси по каждой горизонтальной оси (блоков, с запасом). */
    int reach() {
        double reach = 0.0D;
        for (Lump lump : lumps) {
            reach = Math.max(reach, Math.max(Math.abs(lump.x()), Math.abs(lump.z())) + lump.radius());
        }
        return (int) Math.ceil(reach);
    }

    // Широкое основание: несколько комков у самого низа, разбросанных вокруг оси
    private static void addBase(RandomSource random, List<Lump> lumps) {
        for (int i = 0; i < BASE_LUMPS; i++) {
            double radius = Mth.lerp(random.nextDouble(), BASE_RADIUS_MIN, BASE_RADIUS_MAX);
            lumps.add(new Lump(spread(random, BASE_SPREAD), 1.0D + random.nextDouble() * 0.8D, spread(random, BASE_SPREAD), radius));
        }
    }

    // Узкая шея из комков одна над другой, ось которой плавно уходит то в одну, то в другую сторону.
    // Возвращает, где ось шеи оказалась наверху (x и z), - от неё растёт голова.
    private static double[] addNeck(RandomSource random, List<Lump> lumps, int height) {
        double axisX = 0.0D;
        double axisZ = 0.0D;
        double headStart = height - HEAD_BELOW_TOP;
        for (double y = NECK_FROM_LAYER; y < headStart; y += NECK_STEP) {
            axisX = Mth.clamp(axisX + spread(random, NECK_STEP_SHIFT), -NECK_WANDER, NECK_WANDER);
            axisZ = Mth.clamp(axisZ + spread(random, NECK_STEP_SHIFT), -NECK_WANDER, NECK_WANDER);
            double radius = Mth.lerp(random.nextDouble(), NECK_RADIUS_MIN, NECK_RADIUS_MAX);
            lumps.add(new Lump(axisX, y, axisZ, radius));
        }
        return new double[]{axisX, axisZ};
    }

    // Голова: комки вокруг верха шеи, вынесенные в разные стороны, отчего голова свисает
    private static void addHead(RandomSource random, List<Lump> lumps, int height, double axisX, double axisZ) {
        double headY = height - HEAD_BELOW_TOP;
        for (int i = 0; i < HEAD_LUMPS; i++) {
            double heading = random.nextDouble() * Math.TAU;
            double offset = random.nextDouble() * HEAD_OVERHANG;
            double radius = Mth.lerp(random.nextDouble(), HEAD_RADIUS_MIN, HEAD_RADIUS_MAX);
            lumps.add(new Lump(axisX + Math.cos(heading) * offset, headY + spread(random, HEAD_HEIGHT_SPREAD),
                    axisZ + Math.sin(heading) * offset, radius));
        }
    }

    // Случайное число от -limit до limit
    private static double spread(RandomSource random, double limit) {
        return (random.nextDouble() * 2.0D - 1.0D) * limit;
    }
}
