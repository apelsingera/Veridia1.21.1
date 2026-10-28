package net.gera.veridia.common.block.ore.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Решает, что и как торчит из блока руды. Всё случайное берётся из положения блока, поэтому вид блока не меняется.
 * <ul>
 *   <li>Кусков скрапа от SHARDS_MIN до SHARDS_MAX; у каждого своя грань блока, место на ней, наклон, поворот вокруг
 *       своей оси и длина. Остриё куска всегда уходит внутрь блока, а тупой конец торчит наружу.</li>
 *   <li>С шансом CUBE_CHANCE торчит ещё и маленький кубик (копия блока), повёрнутый по всем трём осям и вдавленный в блок.</li>
 * </ul>
 * Торчит только из открытых граней: тех, где сосед не закрывает блок и не жидкость (в лаве торчащих предметов не видно).
 */
final class ScrapPlanner {
    // ---------- Настройки ----------
    // Сколько кусков скрапа торчит из одного блока (от и до, случайно для каждого блока)
    private static final int SHARDS_MIN = 2;
    private static final int SHARDS_MAX = 4;
    // Шанс (0..1), что из блока торчит ещё и маленький повёрнутый кубик
    private static final float CUBE_CHANCE = 0.25F;

    // Кусок скрапа: длина от острия до дальнего конца (долей блока)
    private static final float SHARD_LENGTH_MIN = 0.42F;
    private static final float SHARD_LENGTH_MAX = 0.70F;
    // На сколько кусок может отклониться от перпендикуляра к грани (градусы)
    private static final float SHARD_TILT_MAX_DEGREES = 40.0F;
    // На сколько острие уходит под поверхность блока (долей блока, от и до)
    private static final float SHARD_EMBED_MIN = 0.10F;
    private static final float SHARD_EMBED_MAX = 0.20F;
    // Насколько от середины грани может сместиться место куска (долей блока в каждую сторону)
    private static final float SHARD_FACE_SPREAD = 0.30F;

    // Кубик: сторона (долей блока, от и до), насколько центр поднят над поверхностью (долей стороны) и смещение от середины грани
    private static final float CUBE_SIZE_MIN = 0.26F;
    private static final float CUBE_SIZE_MAX = 0.40F;
    private static final float CUBE_LIFT = 0.12F;
    private static final float CUBE_FACE_SPREAD = 0.22F;

    private static final long SEED_SALT = 0x5343524150L;
    private static final Vector3f AXIS_X = new Vector3f(1.0F, 0.0F, 0.0F);
    private static final Vector3f AXIS_Y = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f AXIS_Z = new Vector3f(0.0F, 0.0F, 1.0F);

    private ScrapPlanner() {
    }

    static ScrapPlan plan(BlockAndTintGetter level, BlockPos pos, BlockState state, ScrapShape shape) {
        List<Direction> exposed = exposedFaces(level, pos, state);
        if (exposed.isEmpty()) {
            return ScrapPlan.EMPTY;
        }
        RandomSource random = RandomSource.create(Mth.getSeed(pos) ^ SEED_SALT);
        List<ScrapPlan.Piece> pieces = new ArrayList<>();
        int shards = SHARDS_MIN + random.nextInt(SHARDS_MAX - SHARDS_MIN + 1);
        for (int i = 0; i < shards; i++) {
            pieces.add(shard(level, pos, randomFace(exposed, random), random, shape));
        }
        if (random.nextFloat() < CUBE_CHANCE) {
            pieces.add(cube(level, pos, randomFace(exposed, random), random));
        }
        return new ScrapPlan(List.copyOf(pieces));
    }

    // Грани, которые видно: сосед не закрывает грань и не жидкость
    private static List<Direction> exposedFaces(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        List<Direction> exposed = new ArrayList<>(6);
        for (Direction face : Direction.values()) {
            BlockPos neighbourPos = pos.relative(face);
            boolean visible = Block.shouldRenderFace(state, level, pos, face, neighbourPos);
            if (visible && level.getBlockState(neighbourPos).getFluidState().isEmpty()) {
                exposed.add(face);
            }
        }
        return exposed;
    }

    private static Direction randomFace(List<Direction> faces, RandomSource random) {
        return faces.get(random.nextInt(faces.size()));
    }

    // Кусок скрапа, торчащий из грани face
    private static ScrapPlan.Piece shard(BlockAndTintGetter level, BlockPos pos, Direction face, RandomSource random,
                                         ScrapShape shape) {
        Vector3f normal = face.step();
        Vector3f[] tangents = tangents(face);
        Vector3f surface = surfacePoint(face, tangents, random, SHARD_FACE_SPREAD);
        float embed = Mth.lerp(random.nextFloat(), SHARD_EMBED_MIN, SHARD_EMBED_MAX);
        Vector3f tip = surface.fma(-embed, normal, new Vector3f());

        // Ось куска (от острия наружу) слегка наклонена от нормали грани; плоскость куска повёрнута вокруг оси случайно
        Vector3f up = tiltedAxis(normal, tangents, random);
        Vector3f flatNormal = perpendicular(up, random);
        Vector3f across = up.cross(flatNormal, new Vector3f());
        Matrix4f orientation = new Matrix4f().set(new Matrix3f()
                .setColumn(0, across).setColumn(1, up).setColumn(2, flatNormal));

        float length = Mth.lerp(random.nextFloat(), SHARD_LENGTH_MIN, SHARD_LENGTH_MAX);
        // Спрайт кладётся так: остриё в точку tip, его длинная ось вдоль оси куска, центр толщины спрайта (z = 0.5) на плоскости куска
        Matrix4f transform = new Matrix4f()
                .translate(tip)
                .mul(orientation)
                .rotateZ(Mth.HALF_PI - shape.axisAngle())
                .scale(length / shape.length())
                .translate(-shape.tip().x, -shape.tip().y, -0.5F);
        return new ScrapPlan.Piece(ScrapPlan.Kind.SHARD, transform, outsideLight(level, pos, face));
    }

    // Маленький кубик, вдавленный в грань face и повёрнутый по всем трём осям
    private static ScrapPlan.Piece cube(BlockAndTintGetter level, BlockPos pos, Direction face, RandomSource random) {
        Vector3f normal = face.step();
        Vector3f surface = surfacePoint(face, tangents(face), random, CUBE_FACE_SPREAD);
        float size = Mth.lerp(random.nextFloat(), CUBE_SIZE_MIN, CUBE_SIZE_MAX);
        Vector3f center = surface.fma(size * CUBE_LIFT, normal, new Vector3f());

        float turnX = random.nextFloat() * Mth.TWO_PI;
        float turnY = random.nextFloat() * Mth.TWO_PI;
        float turnZ = random.nextFloat() * Mth.TWO_PI;
        // Единичный куб от 0 до 1 сжимается до стороны size, поворачивается вокруг своего центра и ставится в center
        Matrix4f transform = new Matrix4f()
                .translate(center)
                .rotateXYZ(turnX, turnY, turnZ)
                .scale(size)
                .translate(-0.5F, -0.5F, -0.5F);
        return new ScrapPlan.Piece(ScrapPlan.Kind.CUBE, transform, outsideLight(level, pos, face));
    }

    // Освещение клетки за гранью: сам блок сплошной и тёмный, а торчащее наружу должно светиться как его окружение
    private static int outsideLight(BlockAndTintGetter level, BlockPos pos, Direction face) {
        return LevelRenderer.getLightColor(level, pos.relative(face));
    }

    // Точка на поверхности блока (грань face), смещённая от середины грани не больше чем на spread вдоль каждой стороны
    private static Vector3f surfacePoint(Direction face, Vector3f[] tangents, RandomSource random, float spread) {
        Vector3f point = new Vector3f(0.5F, 0.5F, 0.5F).fma(0.5F, face.step());
        point.fma((random.nextFloat() * 2.0F - 1.0F) * spread, tangents[0]);
        point.fma((random.nextFloat() * 2.0F - 1.0F) * spread, tangents[1]);
        return point;
    }

    // Две единичные оси вдоль грани
    private static Vector3f[] tangents(Direction face) {
        return switch (face.getAxis()) {
            case X -> new Vector3f[]{AXIS_Y, AXIS_Z};
            case Y -> new Vector3f[]{AXIS_X, AXIS_Z};
            case Z -> new Vector3f[]{AXIS_X, AXIS_Y};
        };
    }

    // Нормаль грани, отклонённая на случайный угол (до SHARD_TILT_MAX_DEGREES) в случайную сторону
    private static Vector3f tiltedAxis(Vector3f normal, Vector3f[] tangents, RandomSource random) {
        float tilt = random.nextFloat() * (float) Math.toRadians(SHARD_TILT_MAX_DEGREES);
        float heading = random.nextFloat() * Mth.TWO_PI;
        Vector3f sideways = new Vector3f(tangents[0]).mul(Mth.cos(heading)).fma(Mth.sin(heading), tangents[1]);
        return new Vector3f(normal).mul(Mth.cos(tilt)).fma(Mth.sin(tilt), sideways);
    }

    // Случайный единичный вектор, перпендикулярный axis
    private static Vector3f perpendicular(Vector3f axis, RandomSource random) {
        Vector3f reference = Math.abs(axis.y) < 0.9F ? AXIS_Y : AXIS_X;
        Vector3f first = axis.cross(reference, new Vector3f()).normalize();
        Vector3f second = axis.cross(first, new Vector3f());
        float roll = random.nextFloat() * Mth.TWO_PI;
        return first.mul(Mth.cos(roll)).fma(Mth.sin(roll), second);
    }
}
