package net.gera.veridia.common.village.calc;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Реальная геометрия башни (tower_1.nbt) в клетках скана - ОДНО место, где она считается, и для
 * плана стены (WallPlan), и для обеих строек (VillageCalc/NailerWallQueue.towerOriginFor). Раньше
 * план про форму башни не знал вообще (только анкер и "квадрат 5x5 вокруг него"), а стройка ставила
 * её несимметрично - из этого расхождения и росли дыры у башен (см. дампы TOWER-DEBUG).
 * <p>
 * Форма из NBT (size 3x9x4, без поворота лестница смотрит на юг), если смотреть сверху и считать
 * "глубину" от внешней грани (со стороны outward) внутрь деревни:
 * <pre>
 *   глубина 0:  # # #    глухое тело 3x3
 *   глубина 1:  # # #
 *   глубина 2:  # # #
 *   глубина 3:  . H .    H - лестница (не полный блок, сквозь её клетку можно пройти),
 *                        "." - в NBT пусто (structure void)
 * </pre>
 */
public final class TowerFootprint {

    private TowerFootprint() {
    }

    /** Ширина башни вдоль стены (NBT size X). */
    public static final int ALONG_SPAN = VillageCalcSettings.TOWER_FOOTPRINT_MIN;
    /** Глубина башни от внешней грани внутрь (NBT size Z: тело 3 + ряд лестницы). */
    public static final int DEPTH_SPAN = VillageCalcSettings.TOWER_FOOTPRINT_MAX;

    private static final int BODY_DEPTH = DEPTH_SPAN - 1;
    private static final int LADDER_DEPTH = DEPTH_SPAN - 1;
    private static final int LADDER_ALONG = ALONG_SPAN / 2;

    /** bbox башни в клетках скана (min включительно, size - число клеток по оси). */
    public record Box(int minX, int minZ, int sizeX, int sizeZ) {

        public int maxX() {
            return minX + sizeX - 1;
        }

        public int maxZ() {
            return minZ + sizeZ - 1;
        }
    }

    /** Тот же bbox, что стройка получает из шаблона (getBoundingBox + biasedAxisMin), только без шаблона. */
    public static Box box(VillageScan scan, WallPlan.Tower tower) {
        boolean outwardAlongX = tower.outward().getAxis() == Direction.Axis.X;
        int sizeX = outwardAlongX ? DEPTH_SPAN : ALONG_SPAN;
        int sizeZ = outwardAlongX ? ALONG_SPAN : DEPTH_SPAN;
        Direction second = secondaryOutward(scan, tower);
        int minX = biasedAxisMin(tower.cellX(), sizeX, tower.outward(), second, Direction.Axis.X);
        int minZ = biasedAxisMin(tower.cellZ(), sizeZ, tower.outward(), second, Direction.Axis.Z);
        return new Box(minX, minZ, sizeX, sizeZ);
    }

    /** Глухое тело башни (3x3) - то, что реально перекрывает проход. Клетка лестницы сюда НЕ входит. */
    public static List<Cell> bodyCells(VillageScan scan, WallPlan.Tower tower) {
        Box box = box(scan, tower);
        List<Cell> cells = new ArrayList<>(ALONG_SPAN * BODY_DEPTH);
        for (int depth = 0; depth < BODY_DEPTH; depth++) {
            for (int along = 0; along < ALONG_SPAN; along++) {
                cells.add(cellAt(box, tower.outward(), along, depth));
            }
        }
        return cells;
    }

    public static Cell ladderCell(VillageScan scan, WallPlan.Tower tower) {
        return cellAt(box(scan, tower), tower.outward(), LADDER_ALONG, LADDER_DEPTH);
    }

    /**
     * Рядом с башней обрыв или яма: в пределах TOWER_DROP_CLEARANCE клеток от её основания есть земля ниже
     * основания башни больше чем на TOWER_MAX_DROP блоков либо клетка ямы (TerrainHazards). Такая башня стояла
     * бы над пропастью, поэтому в этом месте не ставится вообще - поиск по дуге (WallPlan.findValidTowerIndexNear)
     * идёт дальше. Непрогруженные клетки не в счёт (высота у них не измерена).
     */
    public static boolean nearDrop(VillageScan scan, WallPlan.Tower tower) {
        int clearance = VillageCalcSettings.TOWER_DROP_CLEARANCE;
        if (clearance < 0) {
            return false;
        }
        Box box = box(scan, tower);
        int base = scan.surfaceYAt(tower.cellX(), tower.cellZ());
        for (int cz = box.minZ() - clearance; cz <= box.maxZ() + clearance; cz++) {
            for (int cx = box.minX() - clearance; cx <= box.maxX() + clearance; cx++) {
                if (!scan.isMeasured(cx, cz)) {
                    continue;
                }
                if (scan.isPitZone(cx, cz) || base - scan.surfaceYAt(cx, cz) > VillageCalcSettings.TOWER_MAX_DROP) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Можно ли вообще ставить башню так: всё тело - на застраиваемой территории, а лестница - внутри
     * деревни, НЕ на линии стены. Без этой проверки прямая башня в клетке от угла перевешивалась за
     * угол наружу (вдоль стены она растёт от анкера в +X/+Z, см. biasedAxisMin), а её лестница
     * вставала ровно на линию перпендикулярной стены: стена упиралась в лестницу, и сквозь клетку
     * лестницы был проход (скриншот игрока, дамп: башня (1968,1176)). Такой кандидат отбрасывается,
     * поиск по дуге (WallPlan.findValidTowerIndexNear) берёт следующий - обычно сам угол.
     */
    public static boolean fitsTerritory(VillageScan scan, WallPlan.Tower tower) {
        for (Cell cell : bodyCells(scan, tower)) {
            if (!scan.isBuildable(cell.x(), cell.z())) {
                return false;
            }
        }
        Cell ladder = ladderCell(scan, tower);
        return scan.isBuildable(ladder.x(), ladder.z()) && !scan.isBoundary(ladder.x(), ladder.z());
    }

    /** Второе "наружу" направление угла (перпендикулярное outward, тоже не-territory). Tower хранит
     * только один outward - для угла второй ищется заново по scan'у. У прямого участка обе
     * перпендикулярные клетки territory - null (ожидаемо). */
    @Nullable
    static Direction secondaryOutward(VillageScan scan, WallPlan.Tower spot) {
        for (Direction side : new Direction[]{spot.outward().getClockWise(), spot.outward().getCounterClockWise()}) {
            if (!scan.isTerritory(spot.cellX() + side.getStepX(), spot.cellZ() + side.getStepZ())) {
                return side;
            }
        }
        return null;
    }

    /** По оси, для которой известно направление "наружу" (outward или secondaryOutward), bbox растёт
     * СТРОГО внутрь - к анкеру примыкает внешняя грань. По оси без известного "наружу" (вдоль прямого
     * участка) - от анкера в + (так было исторически; fitsTerritory отсекает случаи, когда из-за
     * этого башня вылезает за угол). */
    static int biasedAxisMin(int anchor, int span, Direction outward, @Nullable Direction second, Direction.Axis axis) {
        Direction onAxis = outward.getAxis() == axis ? outward
                : (second != null && second.getAxis() == axis ? second : null);
        if (onAxis == null) {
            return anchor;
        }
        int step = axis == Direction.Axis.X ? onAxis.getStepX() : onAxis.getStepZ();
        return step > 0 ? anchor - (span - 1) : anchor;
    }

    /** Клетка bbox по двум "башенным" координатам: along - поперёк outward (0..ALONG_SPAN-1),
     * depth - от внешней грани внутрь (0..DEPTH_SPAN-1). */
    private static Cell cellAt(Box box, Direction outward, int along, int depth) {
        return switch (outward) {
            case NORTH -> new Cell(box.minX() + along, box.minZ() + depth);
            case SOUTH -> new Cell(box.minX() + along, box.maxZ() - depth);
            case WEST -> new Cell(box.minX() + depth, box.minZ() + along);
            default -> new Cell(box.maxX() - depth, box.minZ() + along);
        };
    }
}
