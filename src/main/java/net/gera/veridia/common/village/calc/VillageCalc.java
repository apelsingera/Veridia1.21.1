package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class VillageCalc {

    private VillageCalc() {
    }

    private static VillageScan lastScan;
    // Стиль деревни последнего /calculate start (шаблоны и материал заливки) - см. VillageStyle.
    private static VillageStyle lastStyle = VillageStyle.PLAINS;
    private static WallPlan lastPlan;
    private static ServerLevel lastLevel;
    @Nullable
    private static BoundingBox lastHouseBox;
    // "/calculate building" требует, чтобы стена/башни уже были реально построены (result), а не
    // только посчитаны (start) - см. javadoc buildHouse: HouseSitePlan ищет место по абстрактному
    // scan'у, который вообще не знает про WallPlan.segments/fillers/towers. Если ставить дом ДО
    // result, стена может позже спланировать кусок/столбик/башню прямо на месте дома - fits()/
    // towerFits() тогда просто молча пропустят его (skipped++), и в стене останется дыра ровно там.
    // После result те же куски уже настоящие блоки в мире, и isClear() в buildHouse их увидит.
    private static boolean wallBuilt;

    /** Дом, поставленный /calculate building: шаблон и площадка. Нужен, чтобы при расширении стены (перестройка с нуля)
     * поставить уже стоявшие дома обратно на те же места. */
    private record PlacedHouse(ResourceLocation template, HouseSitePlan.HouseSite site, Rotation rotation) {}

    private static final List<PlacedHouse> placedHouses = new ArrayList<>();
    private static Rotation lastHouseRotation = Rotation.NONE;
    // Башни, которые реально встали при последней постройке стены: при расширении стены они закрепляются в новом плане
    // (WallPlan.build) и остаются на месте.
    private static Set<WallPlan.Tower> lastBuiltTowers = Set.of();

    private static VillageCalcSettings.ScanShape scanShape = VillageCalcSettings.DEFAULT_SCAN_SHAPE;
    private static VillageCalcSettings.MarkerSource markerSource = VillageCalcSettings.DEFAULT_MARKER_SOURCE;

    public static void setMarkerSource(VillageCalcSettings.MarkerSource source) {
        markerSource = source;
    }

    public static VillageCalcSettings.MarkerSource markerSource() {
        return markerSource;
    }

    /** Источник, по которому реально считался последний скан: AUTO к этому моменту уже стал MARKERS или VILLAGE. */
    private static VillageCalcSettings.MarkerSource usedSource = VillageCalcSettings.MarkerSource.MARKERS;

    public static VillageCalcSettings.MarkerSource usedSource() {
        return usedSource;
    }

    private static VillageCalcSettings.MarkerSource resolveSource(ServerLevel level, BlockPos center) {
        if (markerSource != VillageCalcSettings.MarkerSource.AUTO) {
            return markerSource;
        }
        return VillageStyle.isVillageNear(level, center)
                ? VillageCalcSettings.MarkerSource.VILLAGE
                : VillageCalcSettings.MarkerSource.MARKERS;
    }
    private static int scanRadius = VillageCalcSettings.DEFAULT_SCAN_RADIUS;

    public static void setScanArea(VillageCalcSettings.ScanShape shape, int radius) {
        scanShape = shape;
        scanRadius = Math.max(1, Math.min(radius, VillageCalcSettings.MAX_SCAN_RADIUS));
    }

    public static VillageCalcSettings.ScanShape scanShape() {
        return scanShape;
    }

    public static int scanRadius() {
        return scanRadius;
    }

    // Для тестов через /calculate - предпросмотр того, что реально построил бы nailer с
    // разблокированными tech_1/tech_2 (villager у /calculate нет, так что своих флагов разблокировки
    // тоже нет - просто два независимых переключателя).
    private static boolean upgraded = false;
    private static boolean buildTowers = true;

    public static void setUpgraded(boolean value) {
        upgraded = value;
    }

    public static boolean isUpgraded() {
        return upgraded;
    }

    public static void setBuildTowers(boolean value) {
        buildTowers = value;
    }

    public static boolean isBuildTowers() {
        return buildTowers;
    }

    private static int skipped;
    private static int treeBlocksCut;
    private static int waterBarriers;
    private static int strayColumns;
    private static int towersPlaced;
    private static int towersSkipped;

    public static int towersPlaced() {
        return towersPlaced;
    }

    public static int towersSkipped() {
        return towersSkipped;
    }

    public static VillageStyle style() {
        return lastStyle;
    }

    /** Строка для отчёта: сколько клеток ям/ущелий нашли и сколько территории вырезано вокруг них. */
    public static String pitSummary() {
        if (lastScan == null) {
            return "";
        }
        return "ямы и ущелья: найдено клеток " + lastScan.pitCells() + ", вырезано из территории " + lastScan.pitCarved()
                + " (глубина от " + VillageCalcSettings.PIT_MIN_DEPTH + " блоков, отступ " + VillageCalcSettings.PIT_MARGIN + ")";
    }

    public static boolean hasScan() {
        return lastScan != null;
    }
    public static boolean hasBuild(ServerLevel level) {
        return !WallSnapshot.get(level).isEmpty();
    }

    public static boolean isInVillageStructure(ServerLevel level, BlockPos pos) {
        return VillageOwnershipFilter.isInVillageStructure(level, pos);
    }

    public record ScanReport(int houses, int misc, int storage, int paths, int territoryCells,
                             int segments, int entrances, int fillers,
                             int gateCandidates, java.util.List<Integer> gateWeights,
                             int gateWeightMin, int gateWeightMax, int poiFound, int treeCells,
                             int waterGaps, int strays, int sealed,
                             int structuresAbsorbed, int structuresRejectedSize,
                             int structuresRejectedDistance, int largestRejectedStructureBlob,
                             int towerGreenCandidates, int towerYellowCandidates, int towersChosen,
                             int towerBoundaryCells, int towerCorners, int towerCornersRejectedFlatness,
                             int houseCandidates, int houseSitesChosen, int iceLandCells, int iceFarCells) {}

    public static ScanReport calculate(ServerLevel level, BlockPos center) {
        usedSource = resolveSource(level, center);
        VillageScan scan = VillageScan.scan(level, center, scanShape, scanRadius, usedSource);
        WallPlan plan = WallPlan.build(scan);
        // tech_3 (см. NailerUpgrades/HouseSitePlan) - ТОЛЬКО поиск места под дом, ничего не строит.
        // Чисто для дебаг-визуализации - фиксированный квадрат HOUSE_FOOTPRINT_SIZE, а не размер
        // конкретного шаблона (для этого есть отдельный buildHouse/HOUSE_TEMPLATES, см. /calculate
        // building <name>). Правила площадки те же, что у настоящей стройки (тропинки, отступ от
        // зданий/стены/башен) - см. javadoc HouseSitePlan.
        List<HouseSitePlan.HouseSite> houseCandidates = HouseSitePlan.findCandidates(scan, plan,
                VillageCalcSettings.HOUSE_FOOTPRINT_SIZE, VillageCalcSettings.HOUSE_FOOTPRINT_SIZE);
        List<HouseSitePlan.HouseSite> houseSites = HouseSitePlan.pickSites(houseCandidates);

        lastScan = scan;
        lastPlan = plan;
        lastLevel = level;
        lastStyle = VillageStyle.of(level, center);
        // Новый скан - старый wallBuilt/lastHouseBox больше не про эту территорию (см. javadoc
        // wallBuilt/buildHouse).
        wallBuilt = false;
        lastHouseBox = null;
        placedHouses.clear();
        lastBuiltTowers = Set.of();
        VillageCalcDebug.show(level, scan, plan, houseSites);

        int houses = 0;
        int misc = 0;
        int storage = 0;
        int paths = 0;
        for (VillageScan.Marker marker : scan.markers()) {
            switch (marker.category()) {
                case PATH -> paths++;
                case HOUSE -> houses++;
                case MISC -> misc++;
                case STORAGE -> storage++;
            }
        }
        int territory = 0;
        for (int cz = 0; cz < scan.sizeZ(); cz++) {
            for (int cx = 0; cx < scan.sizeX(); cx++) {
                if (scan.isTerritory(cx, cz)) {
                    territory++;
                }
            }
        }
        int entrances = (int) plan.segments().stream().filter(WallPlan.Segment::entrance).count();
        return new ScanReport(houses, misc, storage, paths, territory,
                plan.segments().size(), entrances, plan.fillers().size(),
                plan.candidateCount(), plan.entranceWeights(),
                plan.lowestCandidateWeight(), plan.highestCandidateWeight(),
                scan.poiFound(), scan.treeCells(), plan.waterCells().size(), plan.strays(), plan.sealed(),
                scan.structuresAbsorbed(), scan.structuresRejectedSize(),
                scan.structuresRejectedDistance(), scan.largestRejectedStructureBlob(),
                plan.greenCandidates().size(), plan.yellowCandidates().size(), plan.towers().size(),
                plan.towerBoundaryCells(), plan.towerCorners(), plan.towerCornersRejectedFlatness(),
                houseCandidates.size(), houseSites.size(), scan.iceLandCells(), scan.iceFarCells());
    }

    public static int skipped() {
        return skipped;
    }

    public static int strayColumns() {
        return strayColumns;
    }

    public static int waterBarriers() {
        return waterBarriers;
    }

    public static int treeBlocksCut() {
        return treeBlocksCut;
    }

    /** /calculate result: стена с нуля. Дома, поставленные до этого, откатываются вместе с прежней стеной. */
    public static int build(ServerLevel level) {
        if (!hasScan() || lastLevel != level) {
            return 0;
        }
        placedHouses.clear();
        return buildWall(level);
    }

    /** Откатывает всё, что поставил расчёт, и строит стену и башни по lastPlan (дома ставит вызывающий). */
    private static int buildWall(ServerLevel level) {
        undo(level);

        StructureTemplate wall = template(level, lastStyle.wall(tierPath(VillageCalcSettings.WALL_PIECE, VillageCalcSettings.WALL_PIECE_UPGRADED)));
        StructureTemplate entrance = template(level, lastStyle.wall(tierPath(VillageCalcSettings.ENTRANCE_PIECE, VillageCalcSettings.ENTRANCE_PIECE_UPGRADED)));
        if (wall == null || entrance == null) {
            return -1;
        }
        List<WeightedPiece> wallVariants = new ArrayList<>();
        wallVariants.add(new WeightedPiece(wall, VillageCalcSettings.WALL_VARIANT_WEIGHT_1));
        addIfPresent(wallVariants, template(level, lastStyle.wall(tierPath(VillageCalcSettings.WALL_PIECE_2, VillageCalcSettings.WALL_PIECE_2_UPGRADED))),
                VillageCalcSettings.WALL_VARIANT_WEIGHT_2);
        addIfPresent(wallVariants, template(level, lastStyle.wall(tierPath(VillageCalcSettings.WALL_PIECE_3, VillageCalcSettings.WALL_PIECE_3_UPGRADED))),
                VillageCalcSettings.WALL_VARIANT_WEIGHT_3);

        List<StructureTemplate> windowVariants = new ArrayList<>();
        addIfPresent(windowVariants, template(level, lastStyle.wall(tierPath(VillageCalcSettings.WALL_WINDOW_1, VillageCalcSettings.WALL_WINDOW_1_UPGRADED))));
        addIfPresent(windowVariants, template(level, lastStyle.wall(tierPath(VillageCalcSettings.WALL_WINDOW_2, VillageCalcSettings.WALL_WINDOW_2_UPGRADED))));
        addIfPresent(windowVariants, template(level, lastStyle.wall(tierPath(VillageCalcSettings.WALL_WINDOW_3, VillageCalcSettings.WALL_WINDOW_3_UPGRADED))));

        skipped = 0;
        treeBlocksCut = 0;
        waterBarriers = 0;
        strayColumns = 0;
        towersPlaced = 0;
        towersSkipped = 0;
        int placed = 0;

        if (VillageCalcSettings.CUT_TREES) {
            clearTrees(level);
        }
        TowerJoinDebug.beginBuild(); // TOWER-DEBUG

        // Башни ставятся ДО кусков стены/столбиков, а не после (было). towerFits/fits обе смотрят
        // на isClear/isStructure по живому миру - если бы башню проверяли последней (как раньше),
        // она каждый раз натыкалась бы на уже построенный на той же клетке кусок стены/столбик и
        // получала "не влезла" стабильно для всех кандидатов (см. отчёт игрока: выбрано 8, построено
        // 0). Теперь наоборот: кусок стены/столбик, чья клетка перекрывается с уже поставленной
        // башней, сам увидит её как препятствие и корректно уступит (тем же fits()/fillerFits(),
        // никакой отдельной проверки не потребовалось).
        // Стык со стеной при этом решён уже в плане (WallPlan.cutWallForTowers): куски стены на месте
        // тела башни вырезаны, до её граней идут столбики-стыки. Тут остаётся только выбрать версию
        // плана: для башни, которая реально встала, - с вырезом, для не вставшей - исходные куски
        // (segmentsFor/fillersFor), иначе на её месте была бы дыра.
        Set<WallPlan.Tower> builtTowers = new HashSet<>();
        if (buildTowers) {
            StructureTemplate tower = template(level, lastStyle.tower());
            if (tower != null) {
                warnIfTowerShapeChanged(tower);
                for (WallPlan.Tower spot : lastPlan.towers()) {
                    BoundingBox box = placeTower(level, tower, spot);
                    TowerJoinDebug.tower(spot, box); // TOWER-DEBUG
                    if (box != null) {
                        towersPlaced++;
                        builtTowers.add(spot);
                    } else {
                        towersSkipped++;
                    }
                }
            }
        }
        lastBuiltTowers = builtTowers;
        Set<Cell> towerCells = towerCells(builtTowers);

        List<WallPlan.Segment> ready = new ArrayList<>();
        List<WallPlan.Filler> columns = new ArrayList<>();
        Set<Long> columnSeen = new HashSet<>();
        // Один и тот же ДЕТЕРМИНИРОВАННЫЙ (по позиции сегмента, см. pickWallPiece) выбор варианта
        // используется и для проверки fits(), и для самой постройки - раньше тут был level.random
        // (см. историю), из-за чего /calculate result каждый раз показывал СЛУЧАЙНЫЙ вариант,
        // не совпадающий с тем, что детерминированно построил бы nailer на этом же месте
        // (NailerWallQueue.pickPieceFor) - плохо для теста "что реально построится".
        Map<WallPlan.Segment, StructureTemplate> chosenPiece = new HashMap<>();

        for (WallPlan.Segment segment : lastPlan.segmentsFor(builtTowers)) {
            StructureTemplate piece = segment.entrance() ? entrance : pickWallPiece(segment, wallVariants, windowVariants);
            boolean uneven = segment.tooUneven(lastScan);
            if (!uneven && fits(level, piece, segment)) {
                ready.add(segment);
                chosenPiece.put(segment, piece);
                continue;
            }
            if (!uneven) {
                skipped++;
            }
            TowerJoinDebug.segmentDegraded(segment); // TOWER-DEBUG
            for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
                addColumn(columns, columnSeen,
                        segment.cellX() + segment.along().getStepX() * i,
                        segment.cellZ() + segment.along().getStepZ() * i, level, towerCells);
            }
        }
        for (WallPlan.Filler filler : lastPlan.fillersFor(builtTowers)) {
            if (!addColumn(columns, columnSeen, filler.cellX(), filler.cellZ(), level, towerCells)) {
                skipped++;
            }
        }

        TowerJoinDebug.beforeStrays(ready, columns); // TOWER-DEBUG
        strayColumns = WallPlan.dropStrays(lastScan, ready, columns, new ArrayList<>(builtTowers));
        TowerJoinDebug.afterStrays(ready, columns); // TOWER-DEBUG

        for (WallPlan.Segment segment : ready) {
            if (placePiece(level, chosenPiece.get(segment), segment)) {
                placed++;
            }
        }
        for (WallPlan.Filler filler : columns) {
            if (placeFiller(level, filler)) {
                placed++;
            } else {
                TowerJoinDebug.placeFailed(filler); // TOWER-DEBUG
            }
        }
        if (VillageCalcSettings.SEAL_WATER_GAPS) {
            for (WallPlan.WaterCell cell : lastPlan.waterCells()) {
                if (placeWaterBarrier(level, cell)) {
                    waterBarriers++;
                }
            }
        }
        // См. javadoc wallBuilt - дальше можно вызывать buildHouse на ЭТОТ level (стена реально
        // стоит в мире, isClear там её увидит). Тут, а не раньше - выше в методе есть ранний return -1
        // (не нашли шаблон стены), тогда объявлять стену "построенной" неверно.
        wallBuilt = true;
        TowerJoinDebug.dump(level, lastScan, lastPlan); // TOWER-DEBUG
        return placed;
    }

    private static boolean placeWaterBarrier(ServerLevel level, WallPlan.WaterCell cell) {
        int cx = cell.cellX();
        int cz = cell.cellZ();
        int depth = lastScan.waterDepthAt(cx, cz);
        if (depth < VillageCalcSettings.WATER_MIN_DEPTH
                || depth > VillageCalcSettings.MAX_WATER_BARRIER_HEIGHT) {
            return false;
        }
        int height = depth - VillageCalcSettings.WATER_BARRIER_SUBMERGE;

        int worldX = lastScan.worldX(cx);
        int worldZ = lastScan.worldZ(cz);
        int base = lastScan.surfaceYAt(cx, cz) + 1;
        BlockState state = waterBarrierState();
        WallSnapshot snapshot = WallSnapshot.get(level);
        boolean any = false;
        for (int dy = 0; dy < height; dy++) {
            BlockPos pos = new BlockPos(worldX, base + dy, worldZ);
            if (level.getFluidState(pos).isEmpty()) {
                break;
            }
            snapshot.record(pos, level.getBlockState(pos));
            level.setBlock(pos, state, Block.UPDATE_ALL);
            any = true;
        }
        return any;
    }

    private static BlockState waterBarrierState() {
        BlockState state = VillageCalcSettings.WATER_BARRIER_BLOCK.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        return state;
    }

    public static boolean isWrongLevel(ServerLevel level) {
        return lastLevel != level && hasBuild(lastLevel);
    }

    public static int reset(ServerLevel level) {
        ServerLevel target = lastLevel;
        int restored = undo(target);
        lastScan = null;
        lastPlan = null;
        lastLevel = null;
        lastHouseBox = null;
        wallBuilt = false;
        placedHouses.clear();
        lastBuiltTowers = Set.of();
        VillageCalcDebug.stop();
        return restored;
    }

    private static int undo(ServerLevel level) {
        return WallSnapshot.get(level).restoreAll(level);
    }

    // --- дома (tech_3, см. NailerUpgrades/HouseSitePlan) - /calculate building <name>. Отдельно от
    // build()/result: не часть очереди стены/башен, вызывается только явно, по имени шаблона. Один
    // вызов - одна попытка поставить один дом (см. javadoc buildHouse). ---

    public enum HouseBuildResult { PLACED, PLACED_WITH_WALL_EXPANSION, NO_SCAN, WALL_NOT_BUILT, UNKNOWN_NAME, TEMPLATE_MISSING, NO_SITE }

    /**
     * Ставит ОДИН дом заданного шаблона (см. VillageCalcSettings.HOUSE_TEMPLATES) - тестовый путь
     * того, что построил бы nailer: площадка выбирается тем же HouseSitePlan.chooseSite (не на
     * тропинках, не вплотную к зданиям/стене/башням, ближайшая к центру деревни, свободная в живом
     * мире), деревья на ней срубаются так же мгновенно. Размер - НАСТОЯЩИЙ размер NBT-структуры
     * (у warrior_house 14x13), а не дебажный квадрат HOUSE_FOOTPRINT_SIZE из calculate().
     * <p>
     * Требует, чтобы result() уже построил стену/башни (см. wallBuilt) - тот же порядок, что у
     * nailer'а (дома в его очереди всегда после стены), и заодно проверка isAreaFree видит настоящие
     * блоки стены рядом с площадкой. Повторный вызов ставит следующий дом: первый уже стоит в мире,
     * и его площадка (плюс отступ вокруг) больше не свободна.
     * <p>
     * Три яруса площадки (решения игрока от 29.09): очень ровное место внутри территории; иначе очень ровное
     * у стены с расширением стены (HouseSitePlan.chooseAtWall - стена перестраивается вокруг дома, уже стоявшие
     * дома и башни остаются); иначе любое место внутри территории с выравниванием.
     */
    public static HouseBuildResult buildHouse(ServerLevel level, String name, @Nullable Rotation forcedRotation) {
        if (!hasScan() || lastLevel != level) {
            return HouseBuildResult.NO_SCAN;
        }
        if (!wallBuilt) {
            return HouseBuildResult.WALL_NOT_BUILT;
        }
        if (!VillageCalcSettings.HOUSE_NAMES.contains(name)) {
            return HouseBuildResult.UNKNOWN_NAME;
        }
        ResourceLocation id = lastStyle.house(name);
        StructureTemplate house = template(level, id);
        if (house == null) {
            return HouseBuildResult.TEMPLATE_MISSING;
        }
        Vec3i size = house.getSize();
        HouseSitePlan.HouseSite flat = HouseSitePlan.chooseSite(level, lastScan, lastPlan,
                size.getX(), size.getY(), size.getZ(), HouseSitePlan.Flatness.VERY_FLAT_ONLY);
        if (flat != null) {
            return placeNewHouse(level, orient(level, id, house, flat, lastPlan, true, true, forcedRotation), house);
        }
        HouseSitePlan.WallExpansion expansion = HouseSitePlan.chooseAtWall(level, lastScan, lastPlan,
                size.getX(), size.getY(), size.getZ(), placedHouseBoxes(level), List.copyOf(lastBuiltTowers));
        if (expansion != null) {
            // Стена уже перестроена под этот габарит: поворот только на 180 (габарит и место те же).
            return placeWithWallExpansion(level, orient(level, id, house, expansion.site(), expansion.plan(), false, true, forcedRotation), expansion.plan());
        }
        HouseSitePlan.HouseSite terraformed = HouseSitePlan.chooseSite(level, lastScan, lastPlan,
                size.getX(), size.getY(), size.getZ());
        if (terraformed == null) {
            return HouseBuildResult.NO_SITE;
        }
        return placeNewHouse(level, orient(level, id, house, terraformed, lastPlan, true, false, forcedRotation), house);
    }

    /** Почему buildHouse не нашёл место (счётчики по этапам, см. HouseSitePlan.explainNoSite); пусто, если разбирать нечего. */
    public static List<String> explainNoSite(ServerLevel level, String name) {
        StructureTemplate house = hasScan() && lastLevel == level ? template(level, lastStyle.house(name)) : null;
        if (house == null) {
            return List.of();
        }
        Vec3i size = house.getSize();
        return HouseSitePlan.explainNoSite(level, lastScan, lastPlan, size.getX(), size.getY(), size.getZ(),
                placedHouseBoxes(level), List.copyOf(lastBuiltTowers));
    }

    /** Дом лицом к дорожкам (HouseOrientation): запоминает выбранный поворот, чтобы /calculate building мог о нём сказать. */
    private static PlacedHouse orient(ServerLevel level, ResourceLocation id, StructureTemplate house, HouseSitePlan.HouseSite site,
                                      WallPlan plan, boolean quarterTurns, boolean veryFlatOnly,
                                      @Nullable Rotation forcedRotation) {
        HouseOrientation.Oriented oriented = HouseOrientation.choose(level, lastScan, plan, house, site, quarterTurns, veryFlatOnly, forcedRotation);
        lastHouseRotation = oriented.rotation();
        return new PlacedHouse(id, oriented.site(), oriented.rotation());
    }

    private static HouseBuildResult placeNewHouse(ServerLevel level, PlacedHouse placed, StructureTemplate house) {
        lastHouseBox = placeHouse(level, placed, house);
        placedHouses.add(placed);
        return HouseBuildResult.PLACED;
    }

    /**
     * Стена расширена под дом: lastScan уже содержит расширенную территорию, plan - стена вокруг неё. Всё, что стояло,
     * откатывается, стена и башни ставятся заново по новому плану (уже построенные башни закреплены в нём, см.
     * HouseSitePlan.chooseAtWall), и все дома, включая новый, ставятся обратно. Старый кусок стены, оказавшийся внутри,
     * исчезает сам: в новом плане его нет.
     */
    private static HouseBuildResult placeWithWallExpansion(ServerLevel level, PlacedHouse added, WallPlan plan) {
        lastPlan = plan;
        List<PlacedHouse> houses = new ArrayList<>(placedHouses);
        houses.add(added);
        placedHouses.clear();
        buildWall(level);
        for (PlacedHouse house : houses) {
            StructureTemplate template = template(level, house.template());
            if (template == null) {
                continue;
            }
            lastHouseBox = placeHouse(level, house, template);
            placedHouses.add(house);
        }
        return HouseBuildResult.PLACED_WITH_WALL_EXPANSION;
    }

    /** Ящики уже поставленных домов (по живому шаблону): новый дом не может заходить на них. */
    private static List<BoundingBox> placedHouseBoxes(ServerLevel level) {
        List<BoundingBox> boxes = new ArrayList<>();
        for (PlacedHouse placed : placedHouses) {
            StructureTemplate template = template(level, placed.template());
            if (template != null) {
                boxes.add(template.getBoundingBox(houseSettings(placed.rotation()), houseOrigin(template, placed)));
            }
        }
        return boxes;
    }

    private static StructurePlaceSettings houseSettings(Rotation rotation) {
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setIgnoreEntities(true)
                .addProcessor(BlockIgnoreProcessor.AIR)
                // warrior_house.nbt изначально сделан под ванильную генерацию деревни (см.
                // ModVillageHouses) - в палитре реально есть minecraft:jigsaw (точки, куда обычная
                // jigsaw-сборка присоединяла бы соседние куски). При прямом placeInWorld (без сборки)
                // они так и остаются обычными jigsaw-блоками в мире. JigsawReplacementProcessor -
                // готовый ванильный процессор именно под этот случай: читает final_state из NBT
                // каждого jigsaw-блока (то, во что он должен превратиться, если ничего не
                // присоединилось - см. JigsawBlockEntity.FINAL_STATE) и подставляет его вместо блока.
                .addProcessor(JigsawReplacementProcessor.INSTANCE);
    }

    /** Y - уровень дома (медиана высот площадки, см. HouseSitePlan.HouseSite): у шаблона в самом низу (local y=0)
     * записана исходная захваченная земля (grass_block/dirt - см. NBT-дамп warrior_house.nbt), реальные стены
     * начинаются с local y=1 - значит y=0 ложится РОВНО на уровень земли выровненной площадки (без +1, как у
     * башни). */
    private static BlockPos houseOrigin(StructureTemplate template, PlacedHouse placed) {
        HouseSitePlan.HouseSite site = placed.site();
        BlockPos corner = new BlockPos(lastScan.worldX(site.cellX()), site.groundY(), lastScan.worldZ(site.cellZ()));
        return HouseOrientation.originFor(template, placed.rotation(), corner);
    }

    /** Ставит дом на площадку: деревья срубаются, земля выравнивается, тропинки дорисовываются. Возвращает ящик дома. */
    private static BoundingBox placeHouse(ServerLevel level, PlacedHouse placed, StructureTemplate house) {
        StructurePlaceSettings settings = houseSettings(placed.rotation());
        BlockPos origin = houseOrigin(house, placed);
        BoundingBox box = house.getBoundingBox(settings, origin);
        clearTreesInBox(level, box);
        // Стволы на откосе вокруг дома тоже рубятся: иначе колонка со стволом не становится ступенькой, и бревно остаётся на её месте.
        treeBlocksCut += HouseEarthworks.clearApronTrunks(level, lastScan, lastPlan, box, WallSnapshot.get(level)::record);
        // Выравнивание площадки и откоса вокруг - сразу, тем же планом, что nailer делает поблочно
        // (HouseEarthworks). Было fillTowerFoundation - подпорка из scrap_wood под домом; теперь яму
        // под домом засыпает земля из этого же плана.
        WallSnapshot snapshot = WallSnapshot.get(level);
        for (HouseEarthworks.Change change : HouseEarthworks.plan(level, lastScan, lastPlan, box)) {
            snapshot.record(change.pos(), level.getBlockState(change.pos()));
            level.setBlock(change.pos(), change.state(), Block.UPDATE_CLIENTS);
        }
        snapshotBox(level, box);
        house.placeInWorld(level, origin, origin, settings, level.random, Block.UPDATE_CLIENTS);
        // Дорисовка тропинок к дому и между кусками, которые он разорвал (HousePaths) - по уже
        // готовой земле.
        HouseEarthworks.FinishedGround ground = HouseEarthworks.finishedGround(level, lastScan, lastPlan, box);
        for (HouseEarthworks.Change change : HousePaths.plan(level, lastScan, ground)) {
            snapshot.record(change.pos(), level.getBlockState(change.pos()));
            level.setBlock(change.pos(), change.state(), Block.UPDATE_CLIENTS);
        }
        return box;
    }

    /** Как NailerWallQueue.clearTreesInFootprint, но с записью в WallSnapshot - /calculate reset
     * вернёт и срубленные деревья. */
    private static void clearTreesInBox(ServerLevel level, BoundingBox box) {
        WallSnapshot snapshot = WallSnapshot.get(level);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int cx = x - lastScan.minX();
                int cz = z - lastScan.minZ();
                if (!lastScan.inBounds(cx, cz) || !lastScan.isTree(cx, cz)) {
                    continue;
                }
                int ground = lastScan.surfaceYAt(cx, cz);
                for (int y = ground + 1; y <= ground + box.getYSpan() + 1; y++) {
                    cursor.set(x, y, z);
                    if (VillageBlocks.isTreePart(level.getBlockState(cursor))) {
                        treeBlocksCut += TreeCutter.cut(level, cursor.immutable(), snapshot::record);
                    }
                }
            }
        }
    }

    /** Поворот дома, поставленного последним (Rotation.NONE - вход на юг, как в шаблоне). */
    public static Rotation lastHouseRotation() {
        return lastHouseRotation;
    }

    @Nullable
    public static BoundingBox lastHouseBox() {
        return lastHouseBox;
    }

    private static void clearTrees(ServerLevel level) {
        // Вариант плана БЕЗ вырезов под башни (segmentsFor(пусто)) - деревья рубятся ДО постановки
        // башен, ещё не известно, какая из них реально встанет; так покрыта вся линия стены в любом
        // случае, включая место под телом башни.
        for (WallPlan.Segment segment : lastPlan.segmentsFor(Set.of())) {
            for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
                cutColumn(level, segment.cellX() + segment.along().getStepX() * i,
                        segment.cellZ() + segment.along().getStepZ() * i);
            }
        }
        for (WallPlan.Filler filler : lastPlan.fillersFor(Set.of())) {
            cutColumn(level, filler.cellX(), filler.cellZ());
        }
    }

    private static void cutColumn(ServerLevel level, int cx, int cz) {
        if (!lastScan.inBounds(cx, cz) || !lastScan.isTree(cx, cz)) {
            return;
        }
        int worldX = lastScan.worldX(cx);
        int worldZ = lastScan.worldZ(cz);
        int base = lastScan.surfaceYAt(cx, cz);
        WallSnapshot snapshot = WallSnapshot.get(level);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 1; dy <= VillageCalcSettings.WALL_CLEAR_HEIGHT; dy++) {
            cursor.set(worldX, base + dy, worldZ);
            if (VillageBlocks.isTreePart(level.getBlockState(cursor))) {
                treeBlocksCut += TreeCutter.cut(level, cursor.immutable(), snapshot::record);
            }
        }
    }

    private static StructureTemplate template(ServerLevel level, ResourceLocation id) {
        Optional<StructureTemplate> found = level.getStructureManager().get(id);
        assert found.orElse(null) != null;
        return found.orElse(null);
    }

    /** См. setUpgraded/isUpgraded - какой набор из семи шаблонов брать в build(). */
    private static String tierPath(String tier1, String tier2) {
        return upgraded ? tier2 : tier1;
    }

    private record WeightedPiece(StructureTemplate template, int weight) {}

    private static void addIfPresent(List<WeightedPiece> variants, StructureTemplate piece, int weight) {
        variants.add(new WeightedPiece(piece, weight));
    }

    private static void addIfPresent(List<StructureTemplate> variants, StructureTemplate piece) {
        variants.add(piece);
    }

    /** Детерминированный выбор по позиции сегмента - тот же сид, что и NailerWallQueue.pickPieceFor,
     * чтобы /calculate result показывал ИМЕННО тот вариант, что детерминированно построил бы nailer
     * на этом же месте (раньше был level.random - каждый пересчёт давал случайный, не совпадающий
     * с реальной стройкой вариант). */
    private static StructureTemplate pickWallPiece(WallPlan.Segment segment,
                                                    List<WeightedPiece> wallVariants,
                                                    List<StructureTemplate> windowVariants) {
        long seed = (((long) segment.cellX()) << 32) ^ (segment.cellZ() & 0xffffffffL);
        java.util.Random random = new java.util.Random(seed);
        if (!windowVariants.isEmpty() && random.nextFloat() < VillageCalcSettings.WALL_WINDOW_CHANCE) {
            return windowVariants.get(random.nextInt(windowVariants.size()));
        }
        int total = 0;
        for (WeightedPiece variant : wallVariants) {
            total += variant.weight();
        }
        int roll = random.nextInt(total);
        for (WeightedPiece variant : wallVariants) {
            roll -= variant.weight();
            if (roll < 0) {
                return variant.template();
            }
        }
        return wallVariants.getFirst().template();
    }

    private static StructurePlaceSettings settingsFor(StructureTemplate piece, WallPlan.Segment segment) {
        return new StructurePlaceSettings()
                .setRotation(WallPieceRotation.of(piece, segment.outward()))
                .setIgnoreEntities(true);
    }

    private static int baseY(WallPlan.Segment segment) {
        return surfaceUnder(segment) + 1 - VillageCalcSettings.WALL_EMBED_DEPTH;
    }

    private static BlockPos originFor(StructureTemplate piece, WallPlan.Segment segment,
                                      StructurePlaceSettings settings) {
        int y = baseY(segment);
        BlockPos wanted = new BlockPos(
                lastScan.worldX(segment.cellX()), y, lastScan.worldZ(segment.cellZ()));
        BoundingBox probe = piece.getBoundingBox(settings, wanted);
        return wanted.offset(wanted.getX() - probe.minX(), 0, wanted.getZ() - probe.minZ());
    }

    private static boolean fits(ServerLevel level, StructureTemplate piece, WallPlan.Segment segment) {
        if (!standsOnGround(level, segment)) {
            return false;
        }
        StructurePlaceSettings settings = settingsFor(piece, segment);
        return isClear(level, piece.getBoundingBox(settings, originFor(piece, segment, settings)));
    }

    private static boolean placePiece(ServerLevel level, StructureTemplate piece, WallPlan.Segment segment) {
        StructurePlaceSettings settings = settingsFor(piece, segment);
        BlockPos origin = originFor(piece, segment, settings);
        BoundingBox box = piece.getBoundingBox(settings, origin);

        snapshotBox(level, box);
        clearBox(level, box);
        boolean placed = piece.placeInWorld(level, origin, origin, settings, level.random, Block.UPDATE_CLIENTS);
        fillFoundation(level, segment, baseY(segment));
        replaceIceUnderWall(level, box);
        return placed;
    }

    /**
     * Лёд прямо под нижним блоком колонки уже поставленного куска или башни меняется на плотный (IceFoundation): колонки
     * берутся по ящику, нижний блок - первый непустой над верхом земли скана. Куски закопаны в землю (нижний ряд стоит
     * на месте льда) - у них менять нечего.
     */
    private static void replaceIceUnderWall(ServerLevel level, BoundingBox box) {
        WallSnapshot snapshot = WallSnapshot.get(level);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int surface = lastScan.surfaceYAt(x - lastScan.minX(), z - lastScan.minZ());
                BlockPos ice = IceFoundation.iceUnder(lastScan, x, surface + 1, z);
                if (ice == null || level.getBlockState(cursor.set(x, surface + 1, z)).isAir()
                        || !VillageBlocks.isIceSheet(level.getBlockState(ice))) {
                    continue;
                }
                snapshot.record(ice, level.getBlockState(ice));
                level.setBlock(ice, IceFoundation.state(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean addColumn(List<WallPlan.Filler> columns, Set<Long> seen,
                                     int cx, int cz, ServerLevel level, Set<Cell> towerCells) {
        // Было ещё nearTower - кольцо в 1 клетку ВОКРУГ башни (по просьбе игрока: не лепить рваный
        // "квадрат" столбиков вокруг неё, когда кусок стены, задетый башней, рассыпался). По дампу
        // TOWER-DEBUG именно это кольцо и выкидывало клетку стены вплотную к башне - щель в 1 блок (у
        // угловой со стороны лестницы - в 2). Теперь куски, задевающие башню, вырезаются ещё в плане
        // (WallPlan.cutWallForTowers), рассыпаться у башни нечему, и кольцо не нужно. Осталась только
        // защита самой башни (towerCells): её угловые колонки из брёвен, а isClear брёвна за
        // препятствие не считает - без этого столбик мог встать прямо в угол башни и затереть его.
        if (fillerFits(level, cx, cz) || towerCells.contains(new Cell(cx, cz))) {
            TowerJoinDebug.columnRejected(cx, cz); // TOWER-DEBUG
            return false;
        }
        long key = (((long) cx) << 32) ^ (cz & 0xffffffffL);
        if (seen.add(key)) {
            columns.add(new WallPlan.Filler(cx, cz));
        }
        return true;
    }

    /** Клетки построенных башен, куда нельзя ставить столбик (см. addColumn): тело 3x3 и клетка
     * лестницы - столбик на ней замуровал бы подъём. */
    private static Set<Cell> towerCells(Set<WallPlan.Tower> built) {
        Set<Cell> cells = new HashSet<>();
        for (WallPlan.Tower spot : built) {
            cells.addAll(TowerFootprint.bodyCells(lastScan, spot));
            cells.add(TowerFootprint.ladderCell(lastScan, spot));
        }
        return cells;
    }

    /** TowerFootprint (план) считает форму башни без шаблона, по константам TOWER_FOOTPRINT_MIN/MAX -
     * если tower_1.nbt когда-нибудь поменяют (в т.ч. сохранят поверх из игры в generated/), план и
     * стройка разъедутся молча. Тут хотя бы громко. */
    private static void warnIfTowerShapeChanged(StructureTemplate tower) {
        Vec3i size = tower.getSize();
        if (size.getX() != TowerFootprint.ALONG_SPAN || size.getZ() != TowerFootprint.DEPTH_SPAN) {
            com.mojang.logging.LogUtils.getLogger().warn("tower_1 size {}x{} (XZ) != TowerFootprint {}x{} - "
                            + "план стены будет вырезать не то место под башню, поправь TOWER_FOOTPRINT_MIN/MAX",
                    size.getX(), size.getZ(), TowerFootprint.ALONG_SPAN, TowerFootprint.DEPTH_SPAN);
        }
    }

    private static int surfaceUnder(WallPlan.Segment segment) {
        if (segment.entrance()) {
            int half = VillageCalcSettings.PIECE_LENGTH / 2;
            int cx = segment.cellX() + segment.along().getStepX() * half;
            int cz = segment.cellZ() + segment.along().getStepZ() * half;
            Direction out = segment.outward();
            int center = lastScan.surfaceYAt(cx, cz);
            int before = lastScan.surfaceYAt(cx - out.getStepX(), cz - out.getStepZ());
            int after = lastScan.surfaceYAt(cx + out.getStepX(), cz + out.getStepZ());
            int guard = VillageCalcSettings.GATE_POTHOLE_GUARD;
            int low = Math.min(before, after) - guard;
            int high = Math.max(before, after) + guard;
            return Math.max(low, Math.min(high, center)) + VillageCalcSettings.GATE_HEIGHT_OFFSET;
        }

        int[] heights = new int[VillageCalcSettings.PIECE_LENGTH];
        for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
            int cx = segment.cellX() + segment.along().getStepX() * i;
            int cz = segment.cellZ() + segment.along().getStepZ() * i;
            heights[i] = lastScan.surfaceYAt(cx, cz);
        }
        int highest = heights[0];
        for (int height : heights) {
            highest = Math.max(highest, height);
        }
        int[] sorted = heights.clone();
        Arrays.sort(sorted);
        int median = sorted[sorted.length / 2];
        return Math.min(highest, median + VillageCalcSettings.SURFACE_OUTLIER_CAP)
                + VillageCalcSettings.WALL_HEIGHT_OFFSET;
    }

    private static void fillFoundation(ServerLevel level, WallPlan.Segment segment, int bottomY) {
        BlockState state = lastStyle.scrapWood().defaultBlockState();
        WallSnapshot snapshot = WallSnapshot.get(level);
        for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
            int cx = segment.cellX() + segment.along().getStepX() * i;
            int cz = segment.cellZ() + segment.along().getStepZ() * i;
            if (!lastScan.inBounds(cx, cz)) {
                continue;
            }
            int surface = lastScan.surfaceYAt(cx, cz);
            int worldX = lastScan.worldX(cx);
            int worldZ = lastScan.worldZ(cz);
            for (int y = bottomY - 1; y > surface; y--) {
                if (bottomY - y > VillageCalcSettings.MAX_FOUNDATION_DEPTH) {
                    break;
                }
                BlockPos pos = new BlockPos(worldX, y, worldZ);
                snapshot.record(pos, level.getBlockState(pos));
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean isClear(ServerLevel level, BoundingBox box) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    cursor.set(x, y, z);
                    if (VillageBlocks.isStructureAt(level, cursor, level.getBlockState(cursor))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    private static boolean standsOnGround(ServerLevel level, WallPlan.Segment segment) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
            int cx = segment.cellX() + segment.along().getStepX() * i;
            int cz = segment.cellZ() + segment.along().getStepZ() * i;
            if (!lastScan.inBounds(cx, cz)) {
                continue;
            }
            if (lastScan.isWet(cx, cz)) {
                return false;
            }
            cursor.set(lastScan.worldX(cx), lastScan.surfaceYAt(cx, cz), lastScan.worldZ(cz));
            if (VillageBlocks.isStructureAt(level, cursor, level.getBlockState(cursor))) {
                return false;
            }
        }
        return true;
    }

    private static int fillerBaseY(int cx, int cz) {
        return lastScan.surfaceYAt(cx, cz) + 1 - VillageCalcSettings.WALL_EMBED_DEPTH;
    }

    private static boolean fillerFits(ServerLevel level, int cx, int cz) {
        if (!lastScan.inBounds(cx, cz) || lastScan.isWet(cx, cz)) {
            return true;
        }
        int worldX = lastScan.worldX(cx);
        int worldZ = lastScan.worldZ(cz);
        int y = fillerBaseY(cx, cz);
        BlockPos below = new BlockPos(worldX, y - 1, worldZ);
        if (VillageBlocks.isStructureAt(level, below, level.getBlockState(below))) {
            return true;
        }
        return !isClear(level, new BoundingBox(worldX, y, worldZ,
                worldX, y + VillageCalcSettings.FILLER_HEIGHT - 1, worldZ));
    }

    private static boolean placeFiller(ServerLevel level, WallPlan.Filler filler) {
        if (fillerFits(level, filler.cellX(), filler.cellZ())) {
            return false;
        }
        int worldX = lastScan.worldX(filler.cellX());
        int worldZ = lastScan.worldZ(filler.cellZ());
        int y = fillerBaseY(filler.cellX(), filler.cellZ());
        List<Block> blocks = FillerDecoration.columnBlocks(filler.cellX(), filler.cellZ(), upgraded, lastStyle);
        WallSnapshot snapshot = WallSnapshot.get(level);
        for (int dy = 0; dy < blocks.size(); dy++) {
            BlockPos pos = new BlockPos(worldX, y + dy, worldZ);
            snapshot.record(pos, level.getBlockState(pos));
            level.setBlock(pos, blocks.get(dy).defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }
    // --- башни (tech_2, см. WallPlan.findTowers) - тот же паттерн, что и у сегментов стены выше,
    // только без fillFoundation (башня сама по себе компактная - 3x4x4, ей отдельный фундамент
    // не нужен, TOWER_FLATNESS_TOLERANCE и так не даёт ставить её на заметно неровный рельеф). ---

    private static Rotation towerRotationFor(Direction outward) {
        // В tower_1.nbt лестница смотрит на SOUTH. outward у Tower - "наружу" (как и у Segment.outward,
        // прочь от деревни), а лестница должна смотреть ВНУТРЬ - то есть на outward.getOpposite().
        // Раньше тут стояло SOUTH -> NONE (без поворота), и лестница разворачивалась вместе со
        // стеной наружу вместо внутрь (см. отчёт игрока со скриншотом). Маппинг SOUTH/NORTH и
        // WEST/EAST просто поменян местами.
        return switch (outward) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static StructurePlaceSettings towerSettingsFor(WallPlan.Tower spot) {
        // Башня не сплошной параллелепипед - "один face выпирает изза лестницы" (см. исходное
        // описание) означает, что в самом NBT часть клеток bounding box'а честно хранит AIR (вырез
        // под лестницу/проём). Без AIR-процессора placeInWorld кладёт этот air поверх травы/земли
        // как обычный блок - получается настоящая яма в грунте на месте выреза (см. отчёт игрока
        // "башня в земле"). С процессором такие клетки просто пропускаются, и там остаётся то, что
        // было в мире (трава/земля), как и задумано.
        return new StructurePlaceSettings()
                .setRotation(towerRotationFor(spot.outward()))
                .setIgnoreEntities(true)
                .addProcessor(BlockIgnoreProcessor.AIR);
    }

    private static int towerBaseY(WallPlan.Tower spot) {
        // WALL_EMBED_DEPTH тут не подходит - у обычных кусков стены в NBT заложен лишний "подземный"
        // ряд специально под эту подрезку (видимая часть остаётся вровень с землёй), а у башни
        // нижний ряд - это уже видимая часть, не запас на закапывание. С вычитанием башня вставала
        // на блок ниже травы (игрок откопал и проверил - см. отчёт). Тут просто "на землю", без
        // встраивания.
        return lastScan.surfaceYAt(spot.cellX(), spot.cellZ()) + 1;
    }

    private static BlockPos towerOriginFor(StructureTemplate tower, WallPlan.Tower spot, StructurePlaceSettings settings) {
        int y = towerBaseY(spot);
        int anchorX = lastScan.worldX(spot.cellX());
        int anchorZ = lastScan.worldZ(spot.cellZ());
        BlockPos probeOrigin = new BlockPos(anchorX, y, anchorZ);
        BoundingBox probe = tower.getBoundingBox(settings, probeOrigin);
        // Та же геометрия, что у плана (TowerFootprint) - единственный источник, см. её javadoc.
        Direction second = TowerFootprint.secondaryOutward(lastScan, spot);
        int minX = TowerFootprint.biasedAxisMin(anchorX, probe.getXSpan(), spot.outward(), second, Direction.Axis.X);
        int minZ = TowerFootprint.biasedAxisMin(anchorZ, probe.getZSpan(), spot.outward(), second, Direction.Axis.Z);
        return probeOrigin.offset(minX - probe.minX(), 0, minZ - probe.minZ());
    }

    private static boolean towerFits(ServerLevel level, StructureTemplate tower, WallPlan.Tower spot) {
        if (!lastScan.inBounds(spot.cellX(), spot.cellZ()) || lastScan.isWet(spot.cellX(), spot.cellZ())) {
            return false;
        }
        StructurePlaceSettings settings = towerSettingsFor(spot);
        return isClear(level, tower.getBoundingBox(settings, towerOriginFor(tower, spot, settings)));
    }

    @Nullable
    private static BoundingBox placeTower(ServerLevel level, StructureTemplate tower, WallPlan.Tower spot) {
        if (!towerFits(level, tower, spot)) {
            return null;
        }
        StructurePlaceSettings settings = towerSettingsFor(spot);
        BlockPos origin = towerOriginFor(tower, spot, settings);
        BoundingBox box = tower.getBoundingBox(settings, origin);

        // clearBox тут не нужен и вреден: он сносит в воздух ВЕСЬ bounding box без разбора, включая
        // клетки под вырезом (air в NBT) - как раз то, что должен сохранять BlockIgnoreProcessor.AIR
        // из towerSettingsFor. snapshotBox сам по себе уже бэкапит все клетки для /calculate reset -
        // отдельная очистка не требуется, placeInWorld и так перезапишет каждую непрозрачную клетку.
        snapshotBox(level, box);
        tower.placeInWorld(level, origin, origin, settings, level.random, Block.UPDATE_CLIENTS);
        fillTowerFoundation(level, box);
        replaceIceUnderWall(level, box);
        return box;
    }

    /** TOWER_FLATNESS_TOLERANCE (см. WallPlan.isFlatFootprint) допускает перепад рельефа до 1 блока
     * под всем footprint'ом башни - её же собственное дно ровное (один Y на весь 3x4/4x3), так что
     * на стороне footprint'а, где земля на блок ниже anchor-клетки, без подпорки оставался бы видимый
     * зазор ("башня летает", см. отчёт игрока). Тот же приём, что fillFoundation у сегментов стены,
     * только по всей 2D-площади footprint'а, а не вдоль одной линии. */
    private static void fillTowerFoundation(ServerLevel level, BoundingBox box) {
        BlockState state = lastStyle.scrapWood().defaultBlockState();
        WallSnapshot snapshot = WallSnapshot.get(level);
        int bottomY = box.minY();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int cx = x - lastScan.minX();
                int cz = z - lastScan.minZ();
                if (!lastScan.inBounds(cx, cz)) {
                    continue;
                }
                int surface = lastScan.surfaceYAt(cx, cz);
                for (int y = bottomY - 1; y > surface; y--) {
                    if (bottomY - y > VillageCalcSettings.MAX_FOUNDATION_DEPTH) {
                        break;
                    }
                    BlockPos pos = new BlockPos(x, y, z);
                    snapshot.record(pos, level.getBlockState(pos));
                    level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void clearBox(ServerLevel level, BoundingBox box) {
        BlockState air = Blocks.AIR.defaultBlockState();
        WallSnapshot snapshot = WallSnapshot.get(level);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    cursor.set(x, y, z);
                    if (level.getBlockState(cursor).isAir()) {
                        continue;
                    }
                    BlockPos pos = cursor.immutable();
                    snapshot.record(pos, level.getBlockState(pos));
                    level.setBlock(pos, air, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void snapshotBox(ServerLevel level, BoundingBox box) {
        WallSnapshot snapshot = WallSnapshot.get(level);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    cursor.set(x, y, z);
                    BlockPos pos = cursor.immutable();
                    snapshot.record(pos, level.getBlockState(pos));
                }
            }
        }
    }
}