package net.gera.veridia.common.block.ore.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Перенос готовых граней модели (скрап, куб) в другое место и поворот: торчащие из блока предметы - это грани заготовок,
 * пересчитанные матрицей. Матрица состоит из переноса, поворота и равномерного масштаба, поэтому обход граней
 * не меняется.
 */
final class DecorQuads {
    private DecorQuads(){}

    static List<BakedQuad> transformAll(List<BakedQuad> quads, Matrix4f matrix, int light) {
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            result.add(transform(quad, matrix, light));
        }
        return result;
    }

    // Грань после поворота: вершины пересчитаны, а в данных вершин записан свет места, куда она попала. Ближайшая к
    // новой нормали ось задаёт направление грани для затенения; сглаженное освещение для таких граней выключено, оно рассчитано
    // на грани вровень с блоком и для торчащих давало бы тёмные пятна.
    private static BakedQuad transform(BakedQuad quad, Matrix4f matrix, int light) {
        int[] vertices = quad.getVertices().clone();
        Vector3f position = new Vector3f();
        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * IQuadTransformer.STRIDE;
            position.set(Float.intBitsToFloat(vertices[base + IQuadTransformer.POSITION]),
                    Float.intBitsToFloat(vertices[base + IQuadTransformer.POSITION + 1]),
                    Float.intBitsToFloat(vertices[base + IQuadTransformer.POSITION + 2]));
            matrix.transformPosition(position);
            vertices[base + IQuadTransformer.POSITION] = Float.floatToRawIntBits(position.x);
            vertices[base + IQuadTransformer.POSITION + 1] = Float.floatToRawIntBits(position.y);
            vertices[base + IQuadTransformer.POSITION + 2] = Float.floatToRawIntBits(position.z);
            // Свет в данных вершины объединяется с рассчитанным по максимуму (см. IVertexConsumerExtension.applyBakedLighting)
            vertices[base + IQuadTransformer.UV2] = light;
            // Нормаль в данных вершины после поворота устарела: ноль заставляет взять нормаль по направлению грани
            vertices[base + IQuadTransformer.NORMAL] = 0;
        }
        Vector3f normal = matrix.transformDirection(quad.getDirection().step());
        Direction direction = Direction.getNearest(normal.x, normal.y, normal.z);
        return new BakedQuad(vertices, quad.getTintIndex(), direction, quad.getSprite(), quad.isShade(), false);
    }
}
