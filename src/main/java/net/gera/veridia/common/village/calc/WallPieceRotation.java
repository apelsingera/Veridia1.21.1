package net.gera.veridia.common.village.calc;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Поворот куска стены по направлению "наружу" его участка (WallPlan.Segment.outward). Общий для настоящей стройки
 * (NailerWallQueue) и /calculate (VillageCalc): раньше у каждого была своя копия.
 * <p>
 * Код рассчитан на куски, лежащие вдоль оси Z (размер 1x5x5, как у plains/taiga), см. VillageCalcSettings.WALL_ROTATION_FIX.
 * Если кусок сохранён вдоль оси X (5x5x1 - например, собран в игре и сохранён структурным блоком лицом на другую
 * сторону), он добавочно поворачивается на 90 градусов, чтобы лечь вдоль стены, а не поперёк: так стены можно
 * строить в любую сторону, не пересохраняя файл.
 */
public final class WallPieceRotation {

    private WallPieceRotation() {
    }

    /** Добавочный поворот куска, сохранённого вдоль X (передняя сторона - у такого куска дверь/ступени смотрят на -Z). */
    private static final Rotation ALONG_X_FIX = Rotation.CLOCKWISE_90;

    public static Rotation of(StructureTemplate piece, Direction outward) {
        Rotation base = switch (outward) {
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        return base.getRotated(VillageCalcSettings.WALL_ROTATION_FIX).getRotated(alongAxisFix(piece));
    }

    private static Rotation alongAxisFix(StructureTemplate piece) {
        Vec3i size = piece.getSize();
        return size.getX() > size.getZ() ? ALONG_X_FIX : Rotation.NONE;
    }
}
