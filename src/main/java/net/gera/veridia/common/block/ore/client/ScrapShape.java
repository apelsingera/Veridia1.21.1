package net.gera.veridia.common.block.ore.client;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.joml.Vector2f;

/**
 * Форма куска скрапа по его спрайту: где у него остриё и куда направлена его длинная ось. Спрайт - плоский предмет,
 * у которого остриё снизу (самая нижняя непрозрачная строка пикселей), а тело уходит вверх и чуть вбок. Когда кусок торчит
 * из блока, остриё должно уйти внутрь блока, поэтому его положение и направление оси нужны, чтобы правильно повернуть
 * кусок (см. {@link ScrapPlanner}). Всё считается по пикселям, так что при перерисовке спрайта ничего менять не надо.
 * <p>
 * Координаты - в долях спрайта: (0, 0) левый нижний угол, (1, 1) правый верхний.
 *
 * @param tip    остриё: середина нижней кромки самой нижней строки непрозрачных пикселей
 * @param axis   единичный вектор от острия к центру масс спрайта (длинная ось куска)
 * @param length расстояние от острия до самого дальнего пикселя вдоль оси (долей спрайта)
 */
record ScrapShape(Vector2f tip, Vector2f axis, float length) {
    // Запасная форма, если в спрайте не нашлось ни одного непрозрачного пикселя: остриё снизу по центру, ось вверх
    private static final ScrapShape FALLBACK = new ScrapShape(new Vector2f(0.5F, 0.0F), new Vector2f(0.0F, 1.0F), 1.0F);

    static ScrapShape of(SpriteContents sprite) {
        int width = sprite.width();
        int height = sprite.height();
        int lowestRow = -1;
        for (int y = height - 1; y >= 0 && lowestRow < 0; y--) {
            for (int x = 0; x < width; x++) {
                if (!sprite.isTransparent(0, x, y)) {
                    lowestRow = y;
                    break;
                }
            }
        }
        if (lowestRow < 0) {
            return FALLBACK;
        }

        Vector2f tip = new Vector2f(rowCenterX(sprite, lowestRow), (height - lowestRow - 1) / (float) height);
        Vector2f centroid = centroid(sprite);
        Vector2f axis = centroid.sub(tip, new Vector2f());
        if (axis.lengthSquared() < 1.0E-6F) {
            return FALLBACK;
        }
        axis.normalize();
        return new ScrapShape(tip, axis, farthestReach(sprite, tip, axis));
    }

    /** Угол оси относительно оси X спрайта (радианы). */
    float axisAngle() {
        return (float) Math.atan2(axis.y, axis.x);
    }

    // Середина по X непрозрачных пикселей строки row
    private static float rowCenterX(SpriteContents sprite, int row) {
        float sum = 0.0F;
        int count = 0;
        for (int x = 0; x < sprite.width(); x++) {
            if (!sprite.isTransparent(0, x, row)) {
                sum += x + 0.5F;
                count++;
            }
        }
        return sum / count / sprite.width();
    }

    private static Vector2f centroid(SpriteContents sprite) {
        float sumX = 0.0F;
        float sumY = 0.0F;
        int count = 0;
        for (int y = 0; y < sprite.height(); y++) {
            for (int x = 0; x < sprite.width(); x++) {
                if (!sprite.isTransparent(0, x, y)) {
                    sumX += pixelCenterX(sprite, x);
                    sumY += pixelCenterY(sprite, y);
                    count++;
                }
            }
        }
        return new Vector2f(sumX / count, sumY / count);
    }

    // Насколько далеко от острия вдоль оси простирается спрайт (до внешней кромки самого дальнего пикселя)
    private static float farthestReach(SpriteContents sprite, Vector2f tip, Vector2f axis) {
        float reach = 0.0F;
        for (int y = 0; y < sprite.height(); y++) {
            for (int x = 0; x < sprite.width(); x++) {
                if (!sprite.isTransparent(0, x, y)) {
                    float along = (pixelCenterX(sprite, x) - tip.x) * axis.x + (pixelCenterY(sprite, y) - tip.y) * axis.y;
                    reach = Math.max(reach, along);
                }
            }
        }
        return reach + 0.5F / sprite.width();
    }

    private static float pixelCenterX(SpriteContents sprite, int x) {
        return (x + 0.5F) / sprite.width();
    }

    // Строки пикселей идут сверху вниз, а координата в долях спрайта растёт вверх
    private static float pixelCenterY(SpriteContents sprite, int y) {
        return (sprite.height() - y - 0.5F) / sprite.height();
    }
}
