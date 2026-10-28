package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.checkerframework.checker.units.qual.UnitsBottom;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
public final class VillageScan {

    public enum Category {
        HOUSE(VillageCalcSettings.HOUSE_VIRTUAL_WEIGHT, VillageCalcSettings.HOUSE_AREA),
        MISC(VillageCalcSettings.MISC_VIRTUAL_WEIGHT, VillageCalcSettings.MISC_AREA),
        STORAGE(VillageCalcSettings.STORAGE_VIRTUAL_WEIGHT, VillageCalcSettings.STORAGE_AREA),
        PATH(VillageCalcSettings.PATH_VIRTUAL_WEIGHT, VillageCalcSettings.PATH_AREA);

        public final int virtualWeight;
        public final int area;

        Category(int virtualWeight, int area) {
            this.virtualWeight = virtualWeight;
            this.area = area;
        }
    }

    public record Marker(BlockPos pos, Category category) {}
    public record VillageObject(Category category, List<Marker> blocks) {}

    private final int minX;
    private final int minZ;
    private final int sizeX;
    private final int sizeZ;

    private final int[] virtualWeight;
    private final boolean[] territory;
    // Клетки, для которых чанк реально был прогружен на момент скана (см. loop в scan() ниже) -
    // territory ставится не только по этому циклу, но и отдельным проходом stampSquare (радиус
    // вокруг каждого найденного POI-маркера, БЕЗ проверки прогружен ли чанк у конкретной клетки в
    // этом радиусе). Раньше из-за этого клетка могла стать territory, даже если её surfaceY ни разу
    // не измерялся (оставался дефолтным нулём) - в плоском мире, где настоящая земля на Y≈-60, это
    // давало стену на ~60 блоков выше настоящей земли (см. отчёт игрока). В конце scan() territory
    // обрезается этой маской - см. её применение там же.
    private final boolean[] loaded;
    private final boolean[] pathTerritory;
    private final boolean[] pathBlock;
    private final boolean[] structure;
    // Клетки structure, лежащие внутри куска деревни из генерации мира (дом, церковь, колодец, загон): такая постройка
    // - часть деревни при любом размере, в отличие от безымянной постройки рядом (см. absorbStructures).
    private final boolean[] villageBuilding;
    // Верх колонки - обычный лёд (VillageBlocks.isIceSheet). iceLand - из них те, что рядом с деревней и считаются землёй
    // (settleIce): по ним строится стена, под ней лёд меняется на плотный. Остальной лёд - постройка, как раньше.
    private final boolean[] iceGround;
    private final boolean[] iceLand;
    private int iceLandCells;
    private int iceFarCells;
    private final boolean[] water;
    private final int[] waterDepth;
    private final boolean[] tree;
    private final int[] surfaceY;
    private final int[] stampedBy;
    // Зона ям: клетки ям и ущелий (TerrainHazards) с отступом PIT_MARGIN, кроме занятых постройками, тропинками
    // и якорями домов - они остаются территорией. Из территории зона вырезается в carvePits.
    private final boolean[] pitZone;
    private int pitCells;
    private int pitCarved;

    private final List<Marker> markers = new ArrayList<>();
    private final List<VillageObject> objects = new ArrayList<>();
    private int poiFound;
    private int treeCells;
    private int structuresAbsorbed;
    private int structuresRejectedSize;
    private int structuresRejectedDistance;
    private int largestRejectedStructureBlob;

    private VillageScan(int minX, int minZ, int size) {
        this.minX = minX;
        this.minZ = minZ;
        this.sizeX = size;
        this.sizeZ = size;
        this.virtualWeight = new int[size * size];
        this.territory = new boolean[size * size];
        this.loaded = new boolean[size * size];
        this.pathTerritory = new boolean[size * size];
        this.pathBlock = new boolean[size * size];
        this.structure = new boolean[size * size];
        this.villageBuilding = new boolean[size * size];
        this.iceGround = new boolean[size * size];
        this.iceLand = new boolean[size * size];
        this.water = new boolean[size * size];
        this.waterDepth = new int[size * size];
        this.tree = new boolean[size * size];
        this.surfaceY = new int[size * size];
        this.stampedBy = new int[size * size];
        this.pitZone = new boolean[size * size];
        Arrays.fill(this.stampedBy, -1);
    }

    public int minX() { return minX; }
    public int minZ() { return minZ; }
    public int sizeX() { return sizeX; }
    public int sizeZ() { return sizeZ; }
    public List<Marker> markers() { return markers; }
    public List<VillageObject> objects() { return objects; }
    public int poiFound() { return poiFound; }
    public int treeCells() { return treeCells; }
    public int structuresAbsorbed() { return structuresAbsorbed; }
    public int structuresRejectedSize() { return structuresRejectedSize; }
    public int structuresRejectedDistance() { return structuresRejectedDistance; }
    public int largestRejectedStructureBlob() { return largestRejectedStructureBlob; }
    /** Клеток-ям (без отступа) и сколько клеток территории вырезано вокруг них - для отчёта /calculate. */
    public int pitCells() { return pitCells; }
    public int pitCarved() { return pitCarved; }

    /** Клетка в зоне ямы (сама яма или отступ от её края). */
    public boolean isPitZone(int cx, int cz) {
        return inBounds(cx, cz) && pitZone[index(cx, cz)];
    }

    /** Высота этой клетки реально измерена (чанк был прогружен); у непрогруженных surfaceYAt - просто 0. */
    public boolean isMeasured(int cx, int cz) {
        return inBounds(cx, cz) && loaded[index(cx, cz)];
    }

    public boolean inBounds(int cx, int cz) {
        return cx >= 0 && cz >= 0 && cx < sizeX && cz < sizeZ;
    }

    private int index(int cx, int cz) {
        return cz * sizeX + cx;
    }

    public boolean isTerritory(int cx, int cz) {
        return inBounds(cx, cz) && territory[index(cx, cz)];
    }

    public boolean isPathTerritory(int cx, int cz) {
        return inBounds(cx, cz) && pathTerritory[index(cx, cz)];
    }

    public boolean isPathBlock(int cx, int cz) {
        return inBounds(cx, cz) && pathBlock[index(cx, cz)];
    }

    public boolean isStructure(int cx, int cz) {
        return inBounds(cx, cz) && structure[index(cx, cz)];
    }

    /** Верх колонки - лёд рядом с деревней, который считается землёй (см. iceLand). */
    public boolean isIceLand(int cx, int cz) {
        return inBounds(cx, cz) && iceLand[index(cx, cz)];
    }

    /** Клеток льда-земли и дальнего льда (остался постройкой) - для отчёта /calculate. */
    public int iceLandCells() { return iceLandCells; }
    public int iceFarCells() { return iceFarCells; }

    public boolean isWater(int cx, int cz) {
        return inBounds(cx, cz) && water[index(cx, cz)];
    }

    public int waterDepthAt(int cx, int cz) {
        return inBounds(cx, cz) ? waterDepth[index(cx, cz)] : 0;
    }

    /** Глубокая вода, на которой стену не ставим. Мелкая (<= SHALLOW_WATER_MAX_DEPTH: ручей, лужа) - нет: стена
     * встаёт на дно. Для домов, которым нужна полностью сухая площадка, есть hasWater. */
    public boolean isWet(int cx, int cz) {
        return waterDepthAt(cx, cz) > VillageCalcSettings.SHALLOW_WATER_MAX_DEPTH;
    }

    /** Есть хоть какая-то жидкость (даже один блок): дому на такой клетке не место. */
    public boolean hasWater(int cx, int cz) {
        return waterDepthAt(cx, cz) > 0;
    }

    /** Есть ли вода в пределах WATER_BARRIER_RANGE клеток от (cx,cz) в направлении outward. */
    public boolean waterOutward(int cx, int cz, Direction outward) {
        for (int step = 1; step <= VillageCalcSettings.WATER_BARRIER_RANGE; step++) {
            if (isWater(cx + outward.getStepX() * step, cz + outward.getStepZ() * step)) {
                return true;
            }
        }
        return false;
    }

    /** true, если (cx,cz) - клетка суши на границе, за которой (наружу) в пределах
     * WATER_BARRIER_RANGE есть вода. WallPlan.collect() намеренно НЕ превращает такие клетки в
     * обычный кусок стены (Segment) - неровный берег плохо годится под посадку полноразмерного
     * 5-блочного шаблона. ВАЖНО: сама по себе эта проверка ничего не строит взамен - сухую
     * "подходную" полосу до самой воды достраивает WallPlan.sealNearWaterGaps() простыми
     * столбиками-заглушками, а воду - WallPlan.collectWaterGaps()/WaterCell. Без первого шага
     * между концом обычной стены и берегом оставался бы пустой разрыв. */
    public boolean facesWaterOutside(int cx, int cz) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (isTerritory(cx + side.getStepX(), cz + side.getStepZ())) {
                continue;
            }
            if (waterOutward(cx, cz, side)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTree(int cx, int cz) {
        return inBounds(cx, cz) && tree[index(cx, cz)];
    }

    public boolean isBuildable(int cx, int cz) {
        return isTerritory(cx, cz) && !isStructure(cx, cz) && !isWet(cx, cz);
    }

    /** См. javadoc TerritoryShaper.fillEnclosedBy - закрашивает "пустырь" (не-territory клетки),
     * со всех сторон окружённый уже построенной стеной (wallCells), в territory - без этого
     * collect() рисовал бы по границе такого пустыря свою, лишнюю внутреннюю стену, даже когда
     * забор снаружи уже выглядит цельным (WallPlan.build). */
    public boolean fillEnclosedByWall(boolean[] wallCells) {
        return TerritoryShaper.fillEnclosedBy(territory, wallCells, sizeX, sizeZ);
    }

    /** Копия карты территории: расширение под дом примеряется на скане и откатывается, если место не подошло. */
    public boolean[] territoryCopy() {
        return territory.clone();
    }

    public void restoreTerritory(boolean[] saved) {
        System.arraycopy(saved, 0, territory, 0, territory.length);
    }

    /**
     * Расширяет территорию на клетки extra (маска в индексации скана) и сглаживает контур теми же проходами, что при
     * обычном скане: стена огибает новый кусок гладко, а не рваным прямоугольником. Клетки extra остаются
     * территорией в любом случае (сглаживание не должно срезать дом), но только измеренные: непрогруженное не
     * застраивается.
     */
    public void expandTerritory(boolean[] extra) {
        for (int i = 0; i < territory.length; i++) {
            territory[i] |= extra[i] && loaded[i];
        }
        TerritoryShaper.closeGaps(territory, sizeX, sizeZ, VillageCalcSettings.GAP_FILL_MAX);
        TerritoryShaper.fillEnclosed(territory, sizeX, sizeZ);
        TerritoryShaper.fillBays(territory, neverFill(), sizeX, sizeZ, VillageCalcSettings.BAY_FILL_RADIUS);
        TerritoryShaper.open(territory, sizeX, sizeZ, VillageCalcSettings.CAPE_TRIM_RADIUS);
        TerritoryShaper.smooth(territory, sizeX, sizeZ, VillageCalcSettings.SMOOTH_PASSES);
        TerritoryShaper.fillEnclosed(territory, sizeX, sizeZ);
        for (int i = 0; i < territory.length; i++) {
            territory[i] = loaded[i] && (territory[i] || extra[i]);
        }
    }

    public int virtualWeightAt(int cx, int cz) {
        return inBounds(cx, cz) ? virtualWeight[index(cx, cz)] : 0;
    }

    public int surfaceYAt(int cx, int cz) {
        return inBounds(cx, cz) ? surfaceY[index(cx, cz)] : 0;
    }

    public int worldX(int cx) { return minX + cx; }
    public int worldZ(int cz) { return minZ + cz; }

    public boolean isBoundary(int cx, int cz) {
        if (!isTerritory(cx, cz)) {
            return false;
        }
        return !isTerritory(cx - 1, cz) || !isTerritory(cx + 1, cz)
                || !isTerritory(cx, cz - 1) || !isTerritory(cx, cz + 1);
    }

    public int virtualWeightAround(int cx, int cz, int r) {
        int sum = 0;
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                sum += virtualWeightAt(cx + dx, cz + dz);
            }
        }
        return sum;
    }
    @SuppressWarnings("deprecation")
    public static VillageScan scan(ServerLevel level, BlockPos center,
                                   VillageCalcSettings.ScanShape shape, int radius,
                                   VillageCalcSettings.MarkerSource source) {
        int size = radius * 2 + 1;
        VillageScan scan = new VillageScan(center.getX() - radius, center.getZ() - radius, size);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int cz = 0; cz < size; cz++) {
            for (int cx = 0; cx < size; cx++) {
                if (shape == VillageCalcSettings.ScanShape.CIRCLE && !insideCircle(cx, cz, radius)) {
                    continue;
                }
                int x = scan.worldX(cx);
                int z = scan.worldZ(cz);
                if (!level.hasChunkAt(x, z)) {
                    continue;
                }
                int i = scan.index(cx, cz);
                scan.loaded[i] = true;
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
                int descended = 0;
                boolean underTree = false;
                while (ground > level.getMinBuildHeight()
                        && descended < VillageCalcSettings.MAX_TREE_DESCENT) {
                    cursor.set(x, ground, z);
                    BlockState state = level.getBlockState(cursor);
                    boolean treePart = VillageBlocks.isTreePartAt(level, cursor, state);
                    if (!treePart && VillageBlocks.vegetationOrAir(state)) {
                        break;
                    }
                    underTree |= treePart;
                    ground--;
                    descended++;
                }
                if (underTree) {
                    scan.tree[i] = true;
                    scan.treeCells++;
                }
                scan.surfaceY[i] = ground;

                int depth = 0;
                while (depth < VillageCalcSettings.MAX_WATER_PROBE) {
                    cursor.set(x, ground + 1 + depth, z);
                    if (level.getFluidState(cursor).isEmpty()) {
                        break;
                    }
                    depth++;
                }
                scan.waterDepth[i] = depth;
                if (depth >= VillageCalcSettings.WATER_MIN_DEPTH) {
                    scan.water[i] = true;
                }

                boolean marked = false;
                for (int d = 0; d <= VillageCalcSettings.SURFACE_PROBE_DEPTH; d++) {
                    cursor.set(x, ground - d, z);
                    Category category = classify(level.getBlockState(cursor).getBlock(), source);
                    if (category != null) {
                        if (source != VillageCalcSettings.MarkerSource.VILLAGE
                                || category == Category.STORAGE
                                || VillageOwnershipFilter.isInVillageStructure(level, cursor)) {
                            scan.addMarker(cursor.immutable(), category);
                            marked = true;
                        }
                        break;
                    }
                }
                if (!marked && isLampPost(level, cursor, x, ground, z) && VillageOwnershipFilter.isInVillageStructure(level, cursor)) {
                    scan.addMarker(new BlockPos(x, ground, z), Category.MISC);
                    marked = true;
                }
                cursor.set(x, ground, z);
                scan.iceGround[i] = VillageBlocks.isIceSheet(level.getBlockState(cursor));
                if (!marked) {
                    if (VillageBlocks.isStructureAt(level, cursor, level.getBlockState(cursor))) {
                        scan.structure[i] = true;
                        scan.villageBuilding[i] = VillageOwnershipFilter.isInVillageStructure(level, cursor);
                    }
                }
            }
        }

        scan.levelMarkerColumns();

        if (source == VillageCalcSettings.MarkerSource.VILLAGE) {
            scan.collectVillagePoi(level, center, radius);
        }
        scan.markPits();

        scan.groupObjects();
        scan.stampObjects();
        scan.settleIce();
        scan.absorbStructures();
        TerritoryShaper.bridgeWater(scan.territory, scan.water, size, size,
                VillageCalcSettings.WATER_BRIDGE_MAX);
        TerritoryShaper.closeGaps(scan.territory, size, size, VillageCalcSettings.GAP_FILL_MAX);
        TerritoryShaper.fillEnclosed(scan.territory, size, size);
        TerritoryShaper.fillBays(scan.territory, scan.neverFill(), size, size, VillageCalcSettings.BAY_FILL_RADIUS);
        TerritoryShaper.open(scan.territory, size, size, VillageCalcSettings.CAPE_TRIM_RADIUS);
        TerritoryShaper.smooth(scan.territory, size, size, VillageCalcSettings.SMOOTH_PASSES);
        TerritoryShaper.fillEnclosed(scan.territory, size, size);
        scan.carvePits();
        scan.dropHouselessBlobs(radius);
        // Обрезаем territory маской loaded - см. её javadoc у поля. Специально ПОСЛЕ всех шейпинг-
        // проходов и dropHouselessBlobs (не до них) - они по-прежнему видят "щедрую" неотрезанную
        // territory для своей же связности/заращивания дыр (иначе один непрогруженный на момент
        // скана чанк посреди деревни мог бы разорвать её на несвязанные куски и уронить половину как
        // "бездомный" остров). Обрезаем только в самом конце, финальным шагом.
        for (int idx = 0; idx < scan.territory.length; idx++) {
            if (!scan.loaded[idx]) {
                scan.territory[idx] = false;
            }
        }
        // Загон (или любая другая площадка, полностью замкнутая кольцом structure-блоков, напр.
        // деревянный забор без разрыва) - до этой строки structure=true стоит только на самих
        // блоках забора по периметру, трава внутри загона ничем не отмечена и для isBuildable/
        // isStructure выглядит как обычная проходимая территория. Из-за этого sealLeaks/dropStrays-
        // мосты в WallPlan закапывали столбик-заглушку прямо посреди загона (см. отчёт игрока со
        // скриншотом: столбик появился внутри загона с свиньёй, хотя рандома в связности нет).
        // Тут находим все такие непомеченные "карманы" (недостижимые от края скана без пересечения
        // structure) и тоже помечаем их structure - после absorbStructures/territory-шейпинга
        // специально, чтобы не трогать уже отлаженную форму territory, а повлиять только на то, что
        // WallPlan.isBuildable/isStructure считает препятствием для стены/заливки/мостов.
        TerritoryShaper.fillEnclosed(scan.structure, size, size);
        return scan;
    }

    /** Яма и отступ вокруг неё, кроме клеток, которые нельзя отдавать: постройки, тропинки, якоря домов. */
    private void markPits() {
        if (VillageCalcSettings.PIT_MIN_DEPTH <= 0) {
            return;
        }
        boolean[] wet = new boolean[sizeX * sizeZ];
        for (int i = 0; i < wet.length; i++) {
            wet[i] = waterDepth[i] > 0;
        }
        boolean[] pits = TerrainHazards.find(surfaceY, loaded, wet, sizeX, sizeZ);
        boolean[] anchored = new boolean[sizeX * sizeZ];
        for (Marker marker : markers) {
            int cx = marker.pos().getX() - minX;
            int cz = marker.pos().getZ() - minZ;
            if (inBounds(cx, cz)) {
                anchored[index(cx, cz)] = true;
            }
        }
        boolean[] zone = pitZoneAroundBuildings(pits, anchored);
        for (int i = 0; i < zone.length; i++) {
            pitCells += pits[i] ? 1 : 0;
            pitZone[i] = zone[i] && !structure[i] && !pathBlock[i] && !anchored[i];
        }
    }

    /**
     * Яма с отступом, но у построек отступ короче. Обычный отступ PIT_MARGIN отъедал всё место между домом и обрывом:
     * дом оставался единственным, что держит границу, а на месте дома стену не поставить (isStructure), и у дома
     * получалась дыра прямо над обрывом. Вблизи построек (PIT_BUILDING_REACH) зона ямы - только PIT_MARGIN_NEAR_BUILDINGS,
     * так что стена встаёт на самом дальнем от дома месте, которое ещё на твёрдой земле, а не на доме и не над пропастью.
     */
    private boolean[] pitZoneAroundBuildings(boolean[] pits, boolean[] anchored) {
        boolean[] farZone = TerritoryShaper.dilate(pits, sizeX, sizeZ, VillageCalcSettings.PIT_MARGIN);
        boolean[] nearZone = TerritoryShaper.dilate(pits, sizeX, sizeZ, VillageCalcSettings.PIT_MARGIN_NEAR_BUILDINGS);
        boolean[] buildings = new boolean[pits.length];
        for (int i = 0; i < buildings.length; i++) {
            buildings[i] = structure[i] || anchored[i];
        }
        boolean[] nearBuilding = TerritoryShaper.dilate(buildings, sizeX, sizeZ, VillageCalcSettings.PIT_BUILDING_REACH);
        boolean[] zone = new boolean[pits.length];
        for (int i = 0; i < zone.length; i++) {
            zone[i] = nearBuilding[i] ? nearZone[i] : farZone[i];
        }
        return zone;
    }

    /** Что не должно заращиваться при закрытии заливов: глубокая вода и зона ям - стена не пойдёт поперёк. */
    private boolean[] neverFill() {
        boolean[] blocked = water.clone();
        for (int i = 0; i < blocked.length; i++) {
            blocked[i] |= pitZone[i];
        }
        return blocked;
    }

    /**
     * Вырезает зону ям из территории: граница, а за ней и стена, огибает яму с отступом, а не спускается в
     * неё. После вырезания узкие перемычки и обрывки срезаются так же, как при обычном формировании, а
     * яма, которая оказалась внутри территории целиком (вокруг неё земля со всех сторон), заращивается
     * обратно: стену вокруг внутренней ямы строить незачем.
     */
    private void carvePits() {
        int before = 0;
        int after = 0;
        for (int i = 0; i < territory.length; i++) {
            before += territory[i] ? 1 : 0;
            if (pitZone[i]) {
                territory[i] = false;
            }
        }
        TerritoryShaper.open(territory, sizeX, sizeZ, VillageCalcSettings.CAPE_TRIM_RADIUS);
        TerritoryShaper.smooth(territory, sizeX, sizeZ, VillageCalcSettings.SMOOTH_PASSES);
        TerritoryShaper.fillEnclosed(territory, sizeX, sizeZ);
        for (boolean cell : territory) {
            after += cell ? 1 : 0;
        }
        pitCarved = before - after;
    }

    private void dropHouselessBlobs(int radius) {
        boolean[] seeds = new boolean[sizeX * sizeZ];
        boolean anyHouse = false;
        for (Marker marker : markers) {
            // STORAGE (building_materials/food_storage) тоже валидный якорь территории, не только
            // HOUSE - иначе остров территории вокруг склада, стоящего в стороне от деревни и не
            // соединённого сплошной дорожкой, всегда отбрасывался бы здесь как "бездомный",
            // несмотря на то что сам маркер уже был правильно распознан (см. скриншот игрока).
            if (marker.category() != Category.HOUSE && marker.category() != Category.STORAGE) {
                continue;
            }
            int cx = marker.pos().getX() - minX;
            int cz = marker.pos().getZ() - minZ;
            if (inBounds(cx, cz)) {
                seeds[index(cx, cz)] = true;
                anyHouse = true;
            }
        }
        if (!anyHouse) {
            TerritoryShaper.keepMainComponent(territory, sizeX, sizeZ, radius, radius);
            return;
        }

        boolean[] before = territory.clone();
        TerritoryShaper.keepSeeded(territory, sizeX, sizeZ, seeds);
        if (!TerritoryShaper.anyTrue(territory)) {
            System.arraycopy(before, 0, territory, 0, before.length);
            TerritoryShaper.keepMainComponent(territory, sizeX, sizeZ, radius, radius);
        }
    }

    private static boolean isLampPost(ServerLevel level, BlockPos.MutableBlockPos cursor,
                                      int x, int ground, int z) {
        cursor.set(x, ground, z);
        if (!VillageBlocks.isLampPostBlock(level.getBlockState(cursor))) {
            return false;
        }
        for (int dy = 0; dy <= VillageCalcSettings.LAMP_PROBE_HEIGHT; dy++) {
            int y = ground + dy;
            cursor.set(x, y, z);
            if (VillageBlocks.isLampLight(level.getBlockState(cursor))) {
                return true;
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                cursor.set(x + side.getStepX(), y, z + side.getStepZ());
                if (VillageBlocks.isLampLight(level.getBlockState(cursor))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void levelMarkerColumns() {
        for (Marker marker : markers) {
            if (marker.category() != Category.MISC) {
                continue;
            }
            int cx = marker.pos().getX() - minX;
            int cz = marker.pos().getZ() - minZ;
            if (!inBounds(cx, cz)) {
                continue;
            }
            int i = index(cx, cz);
            int lowest = surfaceY[i];
            int floor = surfaceY[i] - VillageCalcSettings.MARKER_POST_HEIGHT;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (!inBounds(cx + dx, cz + dz)) {
                        continue;
                    }
                    lowest = Math.min(lowest, surfaceY[index(cx + dx, cz + dz)]);
                }
            }
            surfaceY[i] = Math.max(lowest, floor);
        }
    }

    private static boolean insideCircle(int cx, int cz, int radius) {
        int dx = cx - radius;
        int dz = cz - radius;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static Category classify(Block block, VillageCalcSettings.MarkerSource source) {
        // building_materials/food_storage - наш собственный блок, не привязанный к режиму
        // источника (маркеры для теста /calculate или реальная деревня) - распознаём в обоих.
        if (VillageCalcSettings.isVillageStorageBlock(block)) {
            return Category.STORAGE;
        }
        if (source == VillageCalcSettings.MarkerSource.VILLAGE) {
            if (VillageCalcSettings.VILLAGE_PATH_BLOCKS.contains(block)) {
                return Category.PATH;
            }
            if (VillageCalcSettings.isVillageMiscBlock(block)) {
                return Category.MISC;
            }
            return null;
        }
        if (VillageCalcSettings.HOUSE_BLOCKS.contains(block)) {
            return Category.HOUSE;
        }
        if (VillageCalcSettings.MISC_BLOCKS.contains(block)) {
            return Category.MISC;
        }
        if (VillageCalcSettings.PATH_BLOCKS.contains(block)) {
            return Category.PATH;
        }
        return null;
    }

    private void addMarker(BlockPos pos, Category category) {
        int cx = pos.getX() - minX;
        int cz = pos.getZ() - minZ;
        if (!inBounds(cx, cz)) {
            return;
        }
        markers.add(new Marker(pos, category));
        if (category == Category.PATH) {
            pathBlock[index(cx, cz)] = true;
        }
    }

    private void collectVillagePoi(ServerLevel level, BlockPos center, int radius) {
        level.getPoiManager()
                .getInSquare(holder -> holder.is(PoiTypeTags.VILLAGE), center, radius, PoiManager.Occupancy.ANY)
                .forEach(record -> {
                    BlockPos pos = record.getPos();
                    if (!VillageOwnershipFilter.isInVillageStructure(level, pos)) {
                        return;
                    }
                    poiFound++;
                    Category category = record.getPoiType().is(PoiTypes.HOME) ? Category.HOUSE : Category.MISC;
                    addMarker(pos, category);
                });
        // building_materials/food_storage - собственный POI-тип (не тегирован minecraft:village),
        // поэтому ищем отдельным запросом. Без isInVillageStructure - склад может стоять под
        // крышей сарая (колоночный скан высоты туда просто не достаёт) и вне ванильной jigsaw-
        // структуры, раз игрок построил его сам рядом с деревней.
        level.getPoiManager()
                .getInSquare(holder -> holder.value() == net.gera.veridia.common.villager.ModVillagers.WORKER_STORAGE_POI.value(),
                        center, radius, PoiManager.Occupancy.ANY)
                .forEach(record -> addMarker(record.getPos(), Category.STORAGE));
        // lumberjack_poi (рабочее место lumberjack'а на LUMBERJACK_LOG) не тегирован minecraft:village
        // и не PoiTypes.HOME, поэтому первый запрос его не находит - домик лесоруба без кровати
        // рядом иначе не даёт вообще никакого якоря территории (см. отчёт игрока).
        level.getPoiManager()
                .getInSquare(holder -> holder.value() == net.gera.veridia.common.villager.ModVillagers.LUMBERJACK_POI.value(),
                        center, radius, PoiManager.Occupancy.ANY)
                .forEach(record -> addMarker(record.getPos(), Category.HOUSE));
        List<Marker> kept = VillageOwnershipFilter.keepClusterNearCenter(
                markers, center);
        markers.clear();
        markers.addAll(kept);
    }

    private void groupObjects() {
        Marker[] byCell = new Marker[sizeX * sizeZ];
        for (Marker marker : markers) {
            int cx = marker.pos().getX() - minX;
            int cz = marker.pos().getZ() - minZ;
            if (inBounds(cx, cz)) {
                byCell[index(cx, cz)] = marker;
            }
        }

        boolean[] visited = new boolean[sizeX * sizeZ];
        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                int start = index(cx, cz);
                if (visited[start] || byCell[start] == null) {
                    continue;
                }
                Category category = byCell[start].category();
                List<Marker> blocks = new ArrayList<>();
                Deque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{cx, cz});
                visited[start] = true;

                while (!queue.isEmpty()) {
                    int[] cell = queue.poll();
                    blocks.add(byCell[index(cell[0], cell[1])]);
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = cell[0] + dx;
                            int nz = cell[1] + dz;
                            if ((dx == 0 && dz == 0) || !inBounds(nx, nz)) {
                                continue;
                            }
                            int next = index(nx, nz);
                            if (visited[next] || byCell[next] == null
                                    || byCell[next].category() != category) {
                                continue;
                            }
                            visited[next] = true;
                            queue.add(new int[]{nx, nz});
                        }
                    }
                }
                objects.add(new VillageObject(category, blocks));
            }
        }
    }
    private void stampObjects() {
        int id = 0;
        for (VillageObject object : objects) {
            if (object.category() == Category.HOUSE) {
                stamp(object, id++, null);
            }
        }
        boolean[] houseTerritory = territory.clone();
        for (VillageObject object : objects) {
            if (object.category() != Category.MISC) {
                continue;
            }
            stamp(object, id++, VillageCalcSettings.MISC_NEEDS_ANCHOR ? houseTerritory : null);
        }
        for (VillageObject object : objects) {
            if (object.category() != Category.STORAGE) {
                continue;
            }
            stamp(object, id++, VillageCalcSettings.STORAGE_NEEDS_ANCHOR ? houseTerritory : null);
        }
        boolean[] anchor = territory.clone();
        for (VillageObject object : objects) {
            if (object.category() != Category.PATH) {
                continue;
            }
            stamp(object, id++, VillageCalcSettings.PATH_NEEDS_ANCHOR ? anchor : null);
        }
    }

    private void stamp(VillageObject object, int id, boolean[] anchor) {
        int area = object.category().area;
        int weight = object.category().virtualWeight;
        boolean path = object.category() == Category.PATH;

        for (Marker block : object.blocks()) {
            int centerX = block.pos().getX() - minX;
            int centerZ = block.pos().getZ() - minZ;
            if (anchor != null
                    && (!inBounds(centerX, centerZ) || !anchor[index(centerX, centerZ)])) {
                continue;
            }
            stampSquare(block, area, weight, path, id);
        }
    }

    private void stampSquare(Marker block, int area, int weight, boolean path, int id) {
        int centerX = block.pos().getX() - minX;
        int centerZ = block.pos().getZ() - minZ;
        for (int dz = -area; dz <= area; dz++) {
            for (int dx = -area; dx <= area; dx++) {
                int cx = centerX + dx;
                int cz = centerZ + dz;
                if (!inBounds(cx, cz)) {
                    continue;
                }
                int i = index(cx, cz);
                territory[i] = true;
                if (path) {
                    pathTerritory[i] = true;
                }
                if (stampedBy[i] != id) {
                    stampedBy[i] = id;
                    virtualWeight[i] += weight;
                }
            }
        }
    }
    /**
     * Лёд замёрзших озёр и рек: по нему ходят мобы, поэтому рядом с деревней (ICE_LAND_RANGE клеток от территории,
     * поставленной по POI) это земля, и стена идёт по нему, как по любой земле. Дальше лёд остаётся "постройкой"
     * (VillageBlocks.isIceSheet - природная земля для кода, но скан без этого помечал бы далёкие озёра землёй
     * деревни): большие куски absorbStructures не поглощает, как и раньше.
     */
    private void settleIce() {
        boolean[] near = TerritoryShaper.dilate(territory, sizeX, sizeZ, VillageCalcSettings.ICE_LAND_RANGE);
        for (int i = 0; i < iceGround.length; i++) {
            if (!iceGround[i]) {
                continue;
            }
            if (near[i] && VillageCalcSettings.ICE_LAND_RANGE > 0) {
                iceLand[i] = true;
                iceLandCells++;
            } else {
                structure[i] = true;
                iceFarCells++;
            }
        }
    }

    private void absorbStructures() {
        boolean[] near = TerritoryShaper.dilate(territory, sizeX, sizeZ,
                VillageCalcSettings.STRUCTURE_ABSORB_RANGE);
        boolean[] visited = new boolean[sizeX * sizeZ];

        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                int start = index(cx, cz);
                if (visited[start] || !structure[start]) {
                    continue;
                }
                List<Integer> blob = new ArrayList<>();
                boolean closeToTerritory = false;
                boolean partOfVillage = false;
                Deque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{cx, cz});
                visited[start] = true;

                while (!queue.isEmpty()) {
                    int[] cell = queue.poll();
                    int i = index(cell[0], cell[1]);
                    blob.add(i);
                    if (near[i]) {
                        closeToTerritory = true;
                    }
                    if (villageBuilding[i]) {
                        partOfVillage = true;
                    }
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = cell[0] + dx;
                            int nz = cell[1] + dz;
                            if ((dx == 0 && dz == 0) || !inBounds(nx, nz)) {
                                continue;
                            }
                            int next = index(nx, nz);
                            if (visited[next] || !structure[next]) {
                                continue;
                            }
                            visited[next] = true;
                            queue.add(new int[]{nx, nz});
                        }
                    }
                }

                // Предел размера отсекает чужие большие постройки, но не дома деревни: церковь с пристройками
                // это ~94 клетки, и стена обходила её снаружи.
                if (closeToTerritory && (partOfVillage || blob.size() <= VillageCalcSettings.MAX_STRUCTURE_BLOB)) {
                    for (int i : blob) {
                        territory[i] = true;
                    }
                    structuresAbsorbed++;
                } else if (closeToTerritory) {
                    structuresRejectedSize++;
                    largestRejectedStructureBlob = Math.max(largestRejectedStructureBlob, blob.size());
                } else {
                    structuresRejectedDistance++;
                }
            }
        }
    }
}