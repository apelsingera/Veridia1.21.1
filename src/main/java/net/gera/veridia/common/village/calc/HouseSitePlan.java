package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Поиск площадки под дом (третья технология nailer'а, tech_3). Сам ничего не строит - только читает
 * VillageScan/WallPlan (и живой мир в chooseSite) и возвращает место.
 * <p>
 * Правила площадки (по договорённости с игроком):
 * <ul>
 *     <li>застраиваемая территория, которую можно выровнять: ни одна клетка не выше/ниже уровня дома
 *     (медиана высот площадки) больше чем на HOUSE_MAX_TERRAFORM - само выравнивание в HouseEarthworks;</li>
 *     <li>не на тропинках - ни одного блока тропинки под домом, иначе дом перегораживает улицу;</li>
 *     <li>не касается зданий - ни существующих домов/заборов деревни, ни стены, ни башен (включая
 *     клетку лестницы): вокруг площадки HOUSE_BUILDING_CLEARANCE клеток пустоты.</li>
 * </ul>
 */
public final class HouseSitePlan {

    private HouseSitePlan() {
    }

    /**
     * (cellX, cellZ) - угол площадки с минимальными X/Z в координатах скана. groundY - уровень дома
     * (мировой Y, на него ложится нижний слой шаблона): медиана высот площадки - так копать и
     * насыпать меньше всего. unevenness - насколько самая выбивающаяся клетка площадки выше/ниже
     * groundY (0 - идеально ровно).
     */
    public record HouseSite(int cellX, int cellZ, int groundY, int unevenness) {

        public boolean isVeryFlat() {
            return unevenness <= VillageCalcSettings.HOUSE_VERY_FLAT_TOLERANCE;
        }
    }

    /**
     * Лучшее свободное место под дом sizeX x sizeY x sizeZ для реальной постройки (по просьбе игрока:
     * "ближе к центру, но если есть очень ровная территория - ставить там"): сначала очень ровные
     * площадки (isVeryFlat), среди них - ближайшая к центру деревни; если очень ровных нет - просто
     * ближайшая к центру. Каждая ещё проверяется: что вокруг получится откос без земляных стенок выше
     * блока (HouseEarthworks.slopeFits - например, не вплотную к дому деревни, стоящему сильно выше), и
     * по ЖИВОМУ миру (isAreaFree) - scan посчитан один раз и не знает, что построено после него
     * (например, дома, которые nailer уже поставил). null - места нет.
     */
    @Nullable
    public static HouseSite chooseSite(ServerLevel level, VillageScan scan, WallPlan plan,
                                       int sizeX, int sizeY, int sizeZ) {
        return chooseSite(level, scan, plan, sizeX, sizeY, sizeZ, Flatness.ANY);
    }

    /** Какие площадки внутри территории подходят: любые (очень ровные раньше) или только очень ровные. */
    public enum Flatness { ANY, VERY_FLAT_ONLY }

    @Nullable
    public static HouseSite chooseSite(ServerLevel level, VillageScan scan, WallPlan plan,
                                       int sizeX, int sizeY, int sizeZ, Flatness flatness) {
        List<HouseSite> candidates = new ArrayList<>(findCandidates(scan, plan, sizeX, sizeZ));
        if (flatness == Flatness.VERY_FLAT_ONLY) {
            candidates.removeIf(site -> !site.isVeryFlat());
        }
        int centerX = scan.sizeX() / 2;
        int centerZ = scan.sizeZ() / 2;
        Comparator<HouseSite> veryFlatFirst = Comparator.comparing(site -> !site.isVeryFlat());
        Comparator<HouseSite> closerToCenter = Comparator.comparingLong(site -> {
            long dx = site.cellX() + sizeX / 2 - centerX;
            long dz = site.cellZ() + sizeZ / 2 - centerZ;
            return dx * dx + dz * dz;
        });
        candidates.sort(veryFlatFirst.thenComparing(closerToCenter));
        for (HouseSite site : candidates) {
            if (HouseEarthworks.slopeFits(scan, plan, houseBox(scan, site, sizeX, sizeY, sizeZ))
                    && isAreaFree(level, scan, site, sizeX, sizeY, sizeZ)) {
                return site;
            }
        }
        return null;
    }

    /** Место у стены, под которое территорию пришлось расширить, и план стены после расширения. Территория скана уже
     * расширена: её копия на случай отката - у вызывающего (VillageScan.territoryCopy). */
    public record WallExpansion(HouseSite site, WallPlan plan) {}

    /**
     * Второй ярус выбора площадки (решения игрока от 29.09: "очень ровное у стены с расширением стены"): когда внутри
     * территории очень ровного места нет, ищется очень ровное место, которому разрешено заходить за стену или лежать
     * вплотную к ней, лишь бы оно касалось территории. Стена под него перестраивается: территория расширяется на
     * дом плюс HOUSE_EXPANSION_MARGIN, план считается заново.
     * <p>
     * Что остаётся как есть: здания деревни, воду, тропинки, ямы, другие дома и башни (otherHouses - их ящики,
     * builtTowers - уже построенные башни: они закрепляются в новом плане, см. WallPlan.build). Стену под домом
     * можно: она старая, и её уберёт перестройка. Поэтому живой мир проверяется, не считая блоков, которые уже менял
     * расчёт (WallSnapshot).
     * <p>
     * Ближайшие к центру места перебираются до HOUSE_EXPANSION_ATTEMPTS раз: на каждое считается новый план.
     * Место не подходит, если вокруг дома после расширения не получается откос (HouseEarthworks.slopeFits).
     */
    @Nullable
    public static WallExpansion chooseAtWall(ServerLevel level, VillageScan scan, WallPlan plan,
                                             int sizeX, int sizeY, int sizeZ,
                                             List<BoundingBox> otherHouses, List<WallPlan.Tower> builtTowers) {
        CellMask forbidden = forbiddenForExpansion(scan, plan, otherHouses);
        List<HouseSite> candidates = new ArrayList<>();
        for (int cz = 0; cz <= scan.sizeZ() - sizeZ; cz++) {
            for (int cx = 0; cx <= scan.sizeX() - sizeX; cx++) {
                if (!touchesTerritory(scan, cx, cz, sizeX, sizeZ)) {
                    continue;
                }
                HouseSite site = evaluate(scan, forbidden, cx, cz, sizeX, sizeZ);
                if (site != null && site.isVeryFlat()) {
                    candidates.add(site);
                }
            }
        }
        candidates.sort(Comparator.comparingLong(site -> distanceToCenterSq(scan, site, sizeX, sizeZ)));

        WallSnapshot snapshot = WallSnapshot.get(level);
        int planned = 0;
        for (HouseSite site : candidates) {
            if (planned >= VillageCalcSettings.HOUSE_EXPANSION_ATTEMPTS) {
                break;
            }
            if (!isAreaFree(level, scan, site, sizeX, sizeY, sizeZ, snapshot::isRecorded)) {
                continue;
            }
            planned++;
            WallPlan expanded = expandFor(scan, houseBox(scan, site, sizeX, sizeY, sizeZ), builtTowers);
            if (expanded != null) {
                return new WallExpansion(site, expanded);
            }
        }
        return null;
    }

    /** Расширяет территорию скана на дом с запасом и считает план заново; null (территория возвращена) - если вокруг
     * такого дома откос не получается. */
    @Nullable
    private static WallPlan expandFor(VillageScan scan, BoundingBox houseBox, List<WallPlan.Tower> builtTowers) {
        boolean[] before = scan.territoryCopy();
        scan.expandTerritory(footprintMask(scan, houseBox, VillageCalcSettings.HOUSE_EXPANSION_MARGIN));
        WallPlan expanded = WallPlan.build(scan, builtTowers);
        if (HouseEarthworks.slopeFits(scan, expanded, houseBox)) {
            return expanded;
        }
        scan.restoreTerritory(before);
        return null;
    }

    /** Клетки, которые дому нельзя накрывать при расширении: здания деревни, башни (с отступом), тропинки, вода, ямы,
     * непрогруженное и другие дома. Стена и территория не мешают. */
    private static CellMask forbiddenForExpansion(VillageScan scan, WallPlan plan, List<BoundingBox> otherHouses) {
        CellMask forbidden = expansionBuildings(scan, plan, otherHouses).grow(VillageCalcSettings.HOUSE_BUILDING_CLEARANCE);
        for (int cz = 0; cz < scan.sizeZ(); cz++) {
            for (int cx = 0; cx < scan.sizeX(); cx++) {
                if (!scan.isMeasured(cx, cz) || scan.isPathBlock(cx, cz) || scan.hasWater(cx, cz) || scan.isPitZone(cx, cz)) {
                    forbidden.mark(cx, cz);
                }
            }
        }
        return forbidden;
    }

    /** То, к чему дом нельзя вплотную при расширении (без отступа): здания деревни, башни и уже стоящие дома. */
    private static CellMask expansionBuildings(VillageScan scan, WallPlan plan, List<BoundingBox> otherHouses) {
        CellMask buildings = new CellMask(scan);
        for (int cz = 0; cz < scan.sizeZ(); cz++) {
            for (int cx = 0; cx < scan.sizeX(); cx++) {
                if (scan.isStructure(cx, cz)) {
                    buildings.mark(cx, cz);
                }
            }
        }
        for (WallPlan.Tower tower : plan.towers()) {
            for (Cell cell : TowerFootprint.bodyCells(scan, tower)) {
                buildings.mark(cell.x(), cell.z());
            }
            Cell ladder = TowerFootprint.ladderCell(scan, tower);
            buildings.mark(ladder.x(), ladder.z());
        }
        for (BoundingBox house : otherHouses) {
            for (int z = house.minZ(); z <= house.maxZ(); z++) {
                for (int x = house.minX(); x <= house.maxX(); x++) {
                    buildings.mark(x - scan.minX(), z - scan.minZ());
                }
            }
        }
        return buildings;
    }

    // --- почему места нет: счётчики по этапам отбора, для сообщения /calculate building ---

    private enum Reject {
        NOT_TOUCHING_TERRITORY("не касается территории"),
        UNMEASURED("не прогружено"),
        NEAR_BUILDING("вплотную к зданию/башне/дому"),
        PATH("на тропинке"),
        WATER("вода"),
        PIT("яма"),
        TOO_STEEP("перепад больше " + 2 * VillageCalcSettings.HOUSE_MAX_TERRAFORM + " или неровнее " + VillageCalcSettings.HOUSE_MAX_TERRAFORM),
        NOT_VERY_FLAT("не очень ровно (неровность больше " + VillageCalcSettings.HOUSE_VERY_FLAT_TOLERANCE + ")"),
        FITS("подходит по плану");

        final String text;

        Reject(String text) {
            this.text = text;
        }
    }

    /**
     * Разбор неудачи buildHouse: сколько мест отсеяно на каждом этапе у стены (какая причина первая), и сколько из
     * подходящих по плану не прошли проверку живого мира и откоса. Территория скана после разбора прежняя.
     */
    public static List<String> explainNoSite(ServerLevel level, VillageScan scan, WallPlan plan,
                                             int sizeX, int sizeY, int sizeZ, List<BoundingBox> otherHouses,
                                             List<WallPlan.Tower> builtTowers) {
        List<String> lines = new ArrayList<>();
        lines.add("разбор: площадка " + sizeX + "x" + sizeZ + ", очень ровной считается с неровностью до "
                + VillageCalcSettings.HOUSE_VERY_FLAT_TOLERANCE + ", с терраформом до " + VillageCalcSettings.HOUSE_MAX_TERRAFORM);
        List<HouseSite> inside = findCandidates(scan, plan, sizeX, sizeZ);
        long insideFlat = inside.stream().filter(HouseSite::isVeryFlat).count();
        lines.add("  внутри стены: подходит по плану " + inside.size() + " мест, из них очень ровных " + insideFlat);

        Map<Reject, Integer> histogram = new EnumMap<>(Reject.class);
        List<HouseSite> atWall = new ArrayList<>();
        classifyAtWall(scan, plan, sizeX, sizeZ, otherHouses, histogram, atWall);
        StringBuilder reasons = new StringBuilder();
        for (Reject reject : Reject.values()) {
            reasons.append("\n    ").append(reject.text).append(": ").append(histogram.getOrDefault(reject, 0));
        }
        lines.add("  у стены: на каждом из мест первая причина отказа (счёт мест, а не клеток):" + reasons);

        WallSnapshot snapshot = WallSnapshot.get(level);
        int liveBlocked = 0;
        int slopeBlocked = 0;
        int checked = 0;
        boolean[] territoryBefore = scan.territoryCopy();
        for (HouseSite site : atWall) {
            if (checked >= VillageCalcSettings.HOUSE_EXPANSION_ATTEMPTS) {
                break;
            }
            if (!isAreaFree(level, scan, site, sizeX, sizeY, sizeZ, snapshot::isRecorded)) {
                liveBlocked++;
                continue;
            }
            checked++;
            if (expandFor(scan, houseBox(scan, site, sizeX, sizeY, sizeZ), builtTowers) == null) {
                slopeBlocked++;
            }
            scan.restoreTerritory(territoryBefore);
        }
        lines.add("  из подходящих у стены " + atWall.size() + ": занято в живом мире (до первых проверок) " + liveBlocked
                + ", не получился откос после расширения " + slopeBlocked + " из " + checked + " проверенных");
        return lines;
    }

    private static void classifyAtWall(VillageScan scan, WallPlan plan, int sizeX, int sizeZ, List<BoundingBox> otherHouses,
                                       Map<Reject, Integer> histogram, List<HouseSite> fitting) {
        CellMask nearBuildings = expansionBuildings(scan, plan, otherHouses).grow(VillageCalcSettings.HOUSE_BUILDING_CLEARANCE);
        CellMask noMask = new CellMask(scan);
        for (int cz = 0; cz <= scan.sizeZ() - sizeZ; cz++) {
            for (int cx = 0; cx <= scan.sizeX() - sizeX; cx++) {
                Reject reject = rejectionAtWall(scan, nearBuildings, noMask, cx, cz, sizeX, sizeZ);
                histogram.merge(reject, 1, Integer::sum);
                if (reject == Reject.FITS) {
                    fitting.add(evaluate(scan, noMask, cx, cz, sizeX, sizeZ));
                }
            }
        }
        fitting.sort(Comparator.comparingLong(site -> distanceToCenterSq(scan, site, sizeX, sizeZ)));
    }

    private static Reject rejectionAtWall(VillageScan scan, CellMask nearBuildings, CellMask noMask,
                                          int cx, int cz, int sizeX, int sizeZ) {
        if (!touchesTerritory(scan, cx, cz, sizeX, sizeZ)) {
            return Reject.NOT_TOUCHING_TERRITORY;
        }
        Reject blocking = firstBlockingCell(scan, nearBuildings, cx, cz, sizeX, sizeZ);
        if (blocking != null) {
            return blocking;
        }
        HouseSite site = evaluate(scan, noMask, cx, cz, sizeX, sizeZ);
        if (site == null) {
            return Reject.TOO_STEEP;
        }
        return site.isVeryFlat() ? Reject.FITS : Reject.NOT_VERY_FLAT;
    }

    /** Первая по порядку причина, по которой площадка накрывает запретную клетку, или null. */
    @Nullable
    private static Reject firstBlockingCell(VillageScan scan, CellMask nearBuildings, int cx, int cz, int sizeX, int sizeZ) {
        Reject[] order = {Reject.UNMEASURED, Reject.NEAR_BUILDING, Reject.PATH, Reject.WATER, Reject.PIT};
        for (Reject reject : order) {
            for (int dz = 0; dz < sizeZ; dz++) {
                for (int dx = 0; dx < sizeX; dx++) {
                    if (cellBlocks(scan, nearBuildings, reject, cx + dx, cz + dz)) {
                        return reject;
                    }
                }
            }
        }
        return null;
    }

    private static boolean cellBlocks(VillageScan scan, CellMask nearBuildings, Reject reject, int cx, int cz) {
        return switch (reject) {
            case UNMEASURED -> !scan.isMeasured(cx, cz);
            case NEAR_BUILDING -> nearBuildings.contains(cx, cz);
            case PATH -> scan.isPathBlock(cx, cz);
            case WATER -> scan.hasWater(cx, cz);
            case PIT -> scan.isPitZone(cx, cz);
            default -> false;
        };
    }

    /** Площадка или кольцо в одну клетку вокруг неё касается территории. */
    private static boolean touchesTerritory(VillageScan scan, int cx, int cz, int sizeX, int sizeZ) {
        for (int z = cz - 1; z <= cz + sizeZ; z++) {
            for (int x = cx - 1; x <= cx + sizeX; x++) {
                if (scan.isTerritory(x, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long distanceToCenterSq(VillageScan scan, HouseSite site, int sizeX, int sizeZ) {
        long dx = site.cellX() + sizeX / 2 - scan.sizeX() / 2;
        long dz = site.cellZ() + sizeZ / 2 - scan.sizeZ() / 2;
        return dx * dx + dz * dz;
    }

    /** Маска клеток скана: ящик дома, расширенный на margin клеток. */
    private static boolean[] footprintMask(VillageScan scan, BoundingBox houseBox, int margin) {
        boolean[] mask = new boolean[scan.sizeX() * scan.sizeZ()];
        for (int z = houseBox.minZ() - margin; z <= houseBox.maxZ() + margin; z++) {
            for (int x = houseBox.minX() - margin; x <= houseBox.maxX() + margin; x++) {
                int cx = x - scan.minX();
                int cz = z - scan.minZ();
                if (scan.inBounds(cx, cz)) {
                    mask[cz * scan.sizeX() + cx] = true;
                }
            }
        }
        return mask;
    }

    /** Мировой bbox дома на площадке site (нижний слой шаблона - на уровне дома groundY). */
    public static net.minecraft.world.level.levelgen.structure.BoundingBox houseBox(
            VillageScan scan, HouseSite site, int sizeX, int sizeY, int sizeZ) {
        int minX = scan.worldX(site.cellX());
        int minZ = scan.worldZ(site.cellZ());
        return new net.minecraft.world.level.levelgen.structure.BoundingBox(
                minX, site.groundY(), minZ, minX + sizeX - 1, site.groundY() + sizeY - 1, minZ + sizeZ - 1);
    }

    /** Все места по scan/плану стены, где дом sizeX x sizeZ проходит правила из javadoc класса. */
    public static List<HouseSite> findCandidates(VillageScan scan, WallPlan plan, int sizeX, int sizeZ) {
        CellMask forbidden = forbiddenCells(scan, plan);
        List<HouseSite> candidates = new ArrayList<>();
        for (int cz = 0; cz <= scan.sizeZ() - sizeZ; cz++) {
            for (int cx = 0; cx <= scan.sizeX() - sizeX; cx++) {
                HouseSite site = evaluate(scan, forbidden, cx, cz, sizeX, sizeZ);
                if (site != null) {
                    candidates.add(site);
                }
            }
        }
        return candidates;
    }

    /** Клетки, которые площадка накрывать не может: не застраиваемые, тропинки и всё, что ближе
     * HOUSE_BUILDING_CLEARANCE к зданиям деревни, стене или башням. */
    private static CellMask forbiddenCells(VillageScan scan, WallPlan plan) {
        CellMask buildings = new CellMask(scan);
        for (int cz = 0; cz < scan.sizeZ(); cz++) {
            for (int cx = 0; cx < scan.sizeX(); cx++) {
                if (scan.isStructure(cx, cz)) {
                    buildings.mark(cx, cz);
                }
            }
        }
        buildings.markAll(wallAndTowerCells(scan, plan));

        CellMask forbidden = buildings.grow(VillageCalcSettings.HOUSE_BUILDING_CLEARANCE);
        for (int cz = 0; cz < scan.sizeZ(); cz++) {
            for (int cx = 0; cx < scan.sizeX(); cx++) {
                if (!scan.isBuildable(cx, cz) || scan.isPathBlock(cx, cz) || scan.hasWater(cx, cz)) {
                    forbidden.mark(cx, cz);
                }
            }
        }
        return forbidden;
    }

    /** Все клетки, где может встать стена или башня - в любой версии плана: и с вырезами под
     * башни, и без них (какие башни реально построятся, заранее неизвестно). */
    static Set<Cell> wallAndTowerCells(VillageScan scan, WallPlan plan) {
        Set<Cell> cells = new java.util.HashSet<>();
        for (WallPlan.Segment segment : plan.segmentsFor(Set.of())) {
            cells.addAll(segment.cells());
        }
        for (WallPlan.Filler filler : plan.fillersFor(Set.of())) {
            cells.add(filler.cell());
        }
        for (WallPlan.Filler filler : plan.fillers()) {
            cells.add(filler.cell());
        }
        for (WallPlan.Tower tower : plan.towers()) {
            cells.addAll(TowerFootprint.bodyCells(scan, tower));
            cells.add(TowerFootprint.ladderCell(scan, tower));
        }
        return cells;
    }

    /** Площадка с углом (cx, cz), если на ней можно строить (с уровнем дома и неровностью), иначе null. */
    @Nullable
    private static HouseSite evaluate(VillageScan scan, CellMask forbidden, int cx, int cz, int sizeX, int sizeZ) {
        int maxTerraform = VillageCalcSettings.HOUSE_MAX_TERRAFORM;
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int dz = 0; dz < sizeZ; dz++) {
            for (int dx = 0; dx < sizeX; dx++) {
                if (forbidden.contains(cx + dx, cz + dz)) {
                    return null;
                }
                int height = scan.surfaceYAt(cx + dx, cz + dz);
                lowest = Math.min(lowest, height);
                highest = Math.max(highest, height);
                // Раз уровень дома - где-то между lowest и highest, при размахе больше 2*max ни
                // один уровень не уложится в ±max - дальше считать незачем.
                if (highest - lowest > 2 * maxTerraform) {
                    return null;
                }
            }
        }
        int groundY = medianHeight(scan, cx, cz, sizeX, sizeZ, lowest, highest);
        int unevenness = Math.max(highest - groundY, groundY - lowest);
        if (unevenness > maxTerraform) {
            return null;
        }
        return new HouseSite(cx, cz, groundY, unevenness);
    }

    /** Медиана высот площадки - через подсчёт по высотам: размах тут маленький (не больше
     * 2*HOUSE_MAX_TERRAFORM), сортировать все sizeX*sizeZ клеток для каждой площадки незачем. */
    private static int medianHeight(VillageScan scan, int cx, int cz, int sizeX, int sizeZ, int lowest, int highest) {
        int[] countAtHeight = new int[highest - lowest + 1];
        for (int dz = 0; dz < sizeZ; dz++) {
            for (int dx = 0; dx < sizeX; dx++) {
                countAtHeight[scan.surfaceYAt(cx + dx, cz + dz) - lowest]++;
            }
        }
        int half = (sizeX * sizeZ + 1) / 2;
        int seen = 0;
        for (int i = 0; i < countAtHeight.length; i++) {
            seen += countAtHeight[i];
            if (seen >= half) {
                return lowest + i;
            }
        }
        return highest;
    }

    /**
     * Проверка по живому миру: над площадкой и в HOUSE_BUILDING_CLEARANCE вокруг неё, на высоту дома,
     * нет ничего построенного (VillageBlocks.isStructure - стена/столбики тоже считаются). Деревья и
     * трава не мешают - деревья на площадке срубаются перед стройкой.
     */
    public static boolean isAreaFree(ServerLevel level, VillageScan scan, HouseSite site, int sizeX, int sizeY, int sizeZ) {
        return isAreaFree(level, scan, site, sizeX, sizeY, sizeZ, pos -> false);
    }

    /** То же, но блоки на позициях, для которых ignored истинно, не считаются помехой (расширение стены: старая стена
     * на месте дома будет убрана перестройкой). */
    public static boolean isAreaFree(ServerLevel level, VillageScan scan, HouseSite site, int sizeX, int sizeY, int sizeZ,
                                     Predicate<BlockPos> ignored) {
        return firstBlocker(level, scan, site, sizeX, sizeY, sizeZ, ignored) == null;
    }

    /** Первое, что мешает дому встать на площадку (см. isAreaFree); null - свободно. */
    @Nullable
    private static BlockPos firstBlocker(ServerLevel level, VillageScan scan, HouseSite site, int sizeX, int sizeY, int sizeZ,
                                         Predicate<BlockPos> ignored) {
        int clearance = VillageCalcSettings.HOUSE_BUILDING_CLEARANCE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int cz = site.cellZ() - clearance; cz < site.cellZ() + sizeZ + clearance; cz++) {
            for (int cx = site.cellX() - clearance; cx < site.cellX() + sizeX + clearance; cx++) {
                if (!scan.inBounds(cx, cz)) {
                    return new BlockPos(scan.worldX(cx), site.groundY(), scan.worldZ(cz));
                }
                // И от земли этой клетки, и от уровня дома: на насыпанной клетке дом встаёт выше
                // её земли, на срезанной - ниже.
                int ground = scan.surfaceYAt(cx, cz);
                int fromY = Math.min(ground, site.groundY()) + 1;
                int toY = Math.max(ground, site.groundY()) + sizeY;
                for (int y = fromY; y <= toY; y++) {
                    cursor.set(scan.worldX(cx), y, scan.worldZ(cz));
                    if (!level.isLoaded(cursor)) {
                        return cursor.immutable();
                    }
                    if (!ignored.test(cursor) && VillageBlocks.isStructureAt(level, cursor, level.getBlockState(cursor))) {
                        return cursor.immutable();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Отбор площадок ТОЛЬКО для дебаг-частиц /calculate start (реальная постройка берёт одну площадку
     * через chooseSite): жадный farthest-point-sampling - каждый раз кандидат, максимально далёкий от
     * уже выбранных, пока не наберётся MAX_HOUSE_SITES или не останется никого дальше
     * MIN_HOUSE_DISTANCE. Без отбора соседние по сетке кандидаты слились бы в сплошное пятно.
     */
    public static List<HouseSite> pickSites(List<HouseSite> candidates) {
        List<HouseSite> remaining = new ArrayList<>(candidates);
        List<HouseSite> chosen = new ArrayList<>();
        long minDistSq = (long) VillageCalcSettings.MIN_HOUSE_DISTANCE * VillageCalcSettings.MIN_HOUSE_DISTANCE;
        while (!remaining.isEmpty() && chosen.size() < VillageCalcSettings.MAX_HOUSE_SITES) {
            HouseSite best = null;
            long bestDistSq = -1;
            for (HouseSite candidate : remaining) {
                long dist = chosen.isEmpty() ? Long.MAX_VALUE : minDistSqTo(candidate, chosen);
                if (dist > bestDistSq) {
                    bestDistSq = dist;
                    best = candidate;
                }
            }
            if (!chosen.isEmpty() && bestDistSq < minDistSq) {
                break;
            }
            chosen.add(best);
            remaining.remove(best);
        }
        return chosen;
    }

    private static long minDistSqTo(HouseSite candidate, List<HouseSite> chosen) {
        long min = Long.MAX_VALUE;
        for (HouseSite other : chosen) {
            long distX = candidate.cellX() - other.cellX();
            long distZ = candidate.cellZ() - other.cellZ();
            min = Math.min(min, distX * distX + distZ * distZ);
        }
        return min;
    }

    /** Отметки по клеткам скана - быстрее Set&lt;Cell&gt;, когда проверок миллионы (перебор всех площадок). */
    private static final class CellMask {
        private final VillageScan scan;
        private final boolean[] marked;

        CellMask(VillageScan scan) {
            this.scan = scan;
            this.marked = new boolean[scan.sizeX() * scan.sizeZ()];
        }

        void mark(int cx, int cz) {
            if (scan.inBounds(cx, cz)) {
                marked[cz * scan.sizeX() + cx] = true;
            }
        }

        void markAll(Set<Cell> cells) {
            for (Cell cell : cells) {
                mark(cell.x(), cell.z());
            }
        }

        boolean contains(int cx, int cz) {
            return scan.inBounds(cx, cz) && marked[cz * scan.sizeX() + cx];
        }

        /** Новая маска: всё отмеченное плюс всё в пределах radius клеток (квадратом) от него. */
        CellMask grow(int radius) {
            CellMask grown = new CellMask(scan);
            for (int cz = 0; cz < scan.sizeZ(); cz++) {
                for (int cx = 0; cx < scan.sizeX(); cx++) {
                    if (!contains(cx, cz)) {
                        continue;
                    }
                    for (int dz = -radius; dz <= radius; dz++) {
                        for (int dx = -radius; dx <= radius; dx++) {
                            grown.mark(cx + dx, cz + dz);
                        }
                    }
                }
            }
            return grown;
        }
    }
}
