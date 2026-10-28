package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Земляные работы под дом (tech_3): площадка выравнивается до уровня дома, а вокруг неё земля плавно
 * переходит к природной высоте. Одинаково для /calculate building (VillageCalc ставит изменения сразу)
 * и nailer'а (NailerWallQueue строит их поблочно за очки, HOUSE_EARTHWORK_COST за блок).
 * <p>
 * Главное правило откоса (по отзыву игрока - "земляные стены в 2 блока не выглядят натурально"):
 * соседние колонки, включая диагональ, никогда не отличаются больше чем на 1 блок - как у природного
 * рельефа. В том числе на стыке с тем, что трогать нельзя (дома деревни, стена, вода, деревья): откос
 * сходится к ним, а не упирается стенкой. Площадки, где так не получится (например, вплотную к дому
 * деревни, стоящему сильно выше), отсекает slopeFits ещё при выборе места.
 * <p>
 * Уровень дома groundY - Y нижнего слоя шаблона (bbox.minY). Роли колонок - см. Role.
 */
public final class HouseEarthworks {

    private HouseEarthworks() {
    }

    private static final int APRON_WIDTH = VillageCalcSettings.HOUSE_APRON_WIDTH;
    /** Сколько раз прогонять выравнивание соседей в откосе - с запасом на ширину откоса туда-обратно. */
    private static final int RELAX_PASSES = 4 * APRON_WIDTH;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState PATH = Blocks.DIRT_PATH.defaultBlockState();

    /** Один блок, который надо поставить (AIR - срезать). */
    public record Change(BlockPos pos, BlockState state) {}

    private enum Role {
        /** Под домом: земля ровно до groundY-1. */
        HOUSE,
        /** Дорожка вплотную к дому: верх ровно на groundY. */
        WALKWAY,
        /** Откос: высота подбирается (плавный переход, не круче блока до соседа). */
        SLOPE,
        /** Не трогаем, но откос с ней сходится не круче блока: природная земля за откосом, вода, за
         * стеной, ствол дерева, земля у фундамента здания. */
        KEEP,
        /** Здание/стена/башня - не трогаем и не учитываем как высоту (у них в колонке крыша, а не
         * земля); вместо этого соседние колонки становятся KEEP, чтобы не подкопать фундамент. */
        BUILDING
    }

    /**
     * Колонка вокруг дома. top - нынешний верх земли (по живому миру - от него считается, что
     * срезать/насыпать); naturalTop - исходный рельеф из скана, от него считается плавный переход откоса
     * (иначе при каждой пересборке очереди nailer'а откос считался бы от уже срезанной земли и сползал
     * бы всё ниже); walkable - по колонке можно проложить тропинку (не дом, не здание, не вода, не
     * ствол, не за стеной).
     */
    private record Column(Role role, int top, int naturalTop, boolean walkable) {

        Column withRole(Role newRole) {
            return new Column(newRole, top, naturalTop, walkable);
        }
    }

    /** Квадрат колонок вокруг дома: сам дом + откос + ещё кольцо за ним (там KEEP - откос сходится к
     * природной земле). */
    private static final class Area {
        final BoundingBox house;
        final int minX;
        final int minZ;
        final int size;
        final Column[][] columns;

        Area(BoundingBox house) {
            this.house = house;
            int reach = APRON_WIDTH + 1;
            this.minX = house.minX() - reach;
            this.minZ = house.minZ() - reach;
            this.size = Math.max(house.getXSpan(), house.getZSpan()) + 2 * reach;
            this.columns = new Column[size][size];
        }

        int worldX(int col) {
            return minX + col;
        }

        int worldZ(int row) {
            return minZ + row;
        }

        boolean inArea(int row, int col) {
            return row >= 0 && col >= 0 && row < size && col < size && columns[row][col] != null;
        }
    }

    /** Ствол (бревно/ветка) выше земли на этой высоте в колонке откоса мешает сделать ступеньку: ищем на этой высоте над землёй. */
    private static final int TRUNK_PROBE_HEIGHT = 3;

    /**
     * Рубит деревья, чей СТВОЛ стоит на откосе или дорожке вокруг дома (в пределах APRON_WIDTH от дома). Колонка со
     * стволом считается непроходимой и откосом не становится (см. classify), так что ствол оставался бы "вместо
     * ступеньки". Деревья в самом ящике дома рубятся отдельно (VillageCalc.clearTreesInBox); здесь - только те, у которых
     * ствол в откосе: крона над откосом дереву не мешает. Каждое дерево рубится целиком (TreeCutter), recorder
     * запоминает его для отката.
     */
    public static int clearApronTrunks(ServerLevel level, VillageScan scan, WallPlan wallPlan, BoundingBox houseBox, Recorder recorder) {
        int cut = 0;
        // Стена и башни тоже из брёвен: на их клетках (и на зданиях деревни) "ствол" - это постройка, её не рубим.
        Set<Cell> wallCells = HouseSitePlan.wallAndTowerCells(scan, wallPlan);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int z = houseBox.minZ() - APRON_WIDTH; z <= houseBox.maxZ() + APRON_WIDTH; z++) {
            for (int x = houseBox.minX() - APRON_WIDTH; x <= houseBox.maxX() + APRON_WIDTH; x++) {
                int cx = x - scan.minX();
                int cz = z - scan.minZ();
                if (!scan.inBounds(cx, cz) || !scan.isTree(cx, cz) || distanceFromHouse(houseBox, x, z) > APRON_WIDTH
                        || wallCells.contains(new Cell(cx, cz)) || scan.isStructure(cx, cz)) {
                    continue;
                }
                int ground = scan.surfaceYAt(cx, cz);
                for (int dy = 1; dy <= TRUNK_PROBE_HEIGHT; dy++) {
                    cursor.set(x, ground + dy, z);
                    BlockState state = level.getBlockState(cursor);
                    if (VillageBlocks.isTreePartAt(level, cursor, state) && !VillageBlocks.isCanopy(state)) {
                        cut += TreeCutter.cut(level, cursor.immutable(), recorder);
                        break;
                    }
                }
            }
        }
        return cut;
    }

    /**
     * Все изменения под дом с bbox houseBox, в порядке, в котором их безопасно делать: сначала срезка
     * сверху вниз (песок/гравий не осыпается в уже вырытое), потом насыпь снизу вверх. Считается по
     * живому миру: уже сделанное (срезано/насыпано) просто не попадает в список.
     */
    public static List<Change> plan(ServerLevel level, VillageScan scan, WallPlan wallPlan, BoundingBox houseBox) {
        Area area = survey(level, scan, wallPlan, houseBox);
        int[][] heights = solveHeights(area);
        List<Change> digs = new ArrayList<>();
        List<Change> fills = new ArrayList<>();
        for (int row = 0; row < area.size; row++) {
            for (int col = 0; col < area.size; col++) {
                Column column = area.columns[row][col];
                if (column == null || column.role() == Role.KEEP || column.role() == Role.BUILDING) {
                    continue;
                }
                reshapeColumn(level, area.worldX(col), area.worldZ(row), column, heights[row][col], digs, fills);
            }
        }
        digs.sort(Comparator.comparingInt((Change change) -> change.pos().getY()).reversed());
        fills.sort(Comparator.comparingInt(change -> change.pos().getY()));
        List<Change> ordered = new ArrayList<>(digs);
        ordered.addAll(fills);
        return ordered;
    }

    /**
     * Можно ли вокруг дома сделать откос без стенок выше блока - по данным скана, для выбора места
     * (HouseSitePlan.chooseSite). Дорожка вокруг дома стоит на groundY, так что колонка KEEP на
     * расстоянии d от дома может отличаться от groundY не больше чем на d-1 - иначе стенка неизбежна.
     */
    public static boolean slopeFits(VillageScan scan, WallPlan wallPlan, BoundingBox houseBox) {
        Area area = survey(null, scan, wallPlan, houseBox);
        int groundY = houseBox.minY();
        for (int row = 0; row < area.size; row++) {
            for (int col = 0; col < area.size; col++) {
                Column column = area.columns[row][col];
                if (column == null || column.role() != Role.KEEP) {
                    continue;
                }
                int distance = chebyshevFromHouse(area.house, area.worldX(col), area.worldZ(row));
                if (Math.abs(column.top() - groundY) > distance - 1) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Какой будет земля вокруг дома после земляных работ (для дорисовки тропинок - HousePaths):
     * итоговый верх каждой колонки, по которой можно ходить, и какие из них - дорожка вплотную к дому.
     * Колонки в координатах скана.
     */
    public record FinishedGround(Map<Cell, Integer> walkableTops, Set<Cell> walkway) {}

    public static FinishedGround finishedGround(ServerLevel level, VillageScan scan, WallPlan wallPlan, BoundingBox houseBox) {
        Area area = survey(level, scan, wallPlan, houseBox);
        int[][] heights = solveHeights(area);
        Map<Cell, Integer> walkableTops = new HashMap<>();
        Set<Cell> walkway = new HashSet<>();
        for (int row = 0; row < area.size; row++) {
            for (int col = 0; col < area.size; col++) {
                Column column = area.columns[row][col];
                if (column == null || !column.walkable()) {
                    continue;
                }
                Cell cell = new Cell(area.worldX(col) - scan.minX(), area.worldZ(row) - scan.minZ());
                walkableTops.put(cell, heights[row][col]);
                if (column.role() == Role.WALKWAY) {
                    walkway.add(cell);
                }
            }
        }
        return new FinishedGround(walkableTops, walkway);
    }

    // --- разметка колонок ---

    /** Роли и высоты колонок вокруг дома. level == null - только по скану (для выбора места), иначе -
     * высоты и постройки по живому миру (scan посчитан один раз и не знает, что срезали/насыпали/
     * построили после него, в том числе под соседний дом). */
    private static Area survey(@Nullable ServerLevel level, VillageScan scan, WallPlan wallPlan, BoundingBox houseBox) {
        Area area = new Area(houseBox);
        Set<Cell> wallCells = HouseSitePlan.wallAndTowerCells(scan, wallPlan);
        for (int row = 0; row < area.size; row++) {
            for (int col = 0; col < area.size; col++) {
                int x = area.worldX(col);
                int z = area.worldZ(row);
                Cell cell = new Cell(x - scan.minX(), z - scan.minZ());
                if (scan.inBounds(cell.x(), cell.z())) {
                    area.columns[row][col] = classify(level, scan, wallCells, area.house, cell, x, z);
                }
            }
        }
        protectFoundations(area);
        return area;
    }

    private static Column classify(@Nullable ServerLevel level, VillageScan scan, Set<Cell> wallCells,
                                   BoundingBox house, Cell cell, int x, int z) {
        int cx = cell.x();
        int cz = cell.z();
        int scanTop = scan.surfaceYAt(cx, cz);
        if (scan.isStructure(cx, cz) || wallCells.contains(cell)) {
            return new Column(Role.BUILDING, scanTop, scanTop, false);
        }
        if (scan.hasWater(cx, cz)) {
            // У воды откос сходится с её поверхностью (берегом), а не с дном.
            int waterSurface = scanTop + scan.waterDepthAt(cx, cz);
            return new Column(Role.KEEP, waterSurface, waterSurface, false);
        }
        int top = scanTop;
        if (level != null) {
            OptionalInt liveTop = liveGroundTop(level, x, z, scanTop);
            if (liveTop.isEmpty()) {
                return new Column(Role.BUILDING, scanTop, scanTop, false);
            }
            top = liveTop.getAsInt();
        }
        double distance = distanceFromHouse(house, x, z);
        if (distance == 0) {
            return new Column(Role.HOUSE, top, scanTop, false);
        }
        if (distance <= 1) {
            return new Column(Role.WALKWAY, top, scanTop, true);
        }
        boolean trunk = level != null && VillageBlocks.isTreePart(level.getBlockState(new BlockPos(x, top + 1, z)));
        boolean walkable = scan.isTerritory(cx, cz) && !trunk;
        if (distance > APRON_WIDTH || !walkable) {
            return new Column(Role.KEEP, top, scanTop, walkable);
        }
        return new Column(Role.SLOPE, top, scanTop, true);
    }

    /** Колонки откоса рядом со зданием/стеной (включая диагональ) не трогаем - иначе подкопаем
     * фундамент; откос сойдётся к ним, как к любой KEEP. */
    private static void protectFoundations(Area area) {
        List<int[]> toKeep = new ArrayList<>();
        for (int row = 0; row < area.size; row++) {
            for (int col = 0; col < area.size; col++) {
                Column column = area.columns[row][col];
                if (column != null && column.role() == Role.SLOPE && touchesBuilding(area, row, col)) {
                    toKeep.add(new int[]{row, col});
                }
            }
        }
        for (int[] at : toKeep) {
            area.columns[at[0]][at[1]] = area.columns[at[0]][at[1]].withRole(Role.KEEP);
        }
    }

    private static boolean touchesBuilding(Area area, int row, int col) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (area.inArea(row + dr, col + dc) && area.columns[row + dr][col + dc].role() == Role.BUILDING) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- высоты откоса ---

    /**
     * Итоговый верх земли для HOUSE/WALKWAY/SLOPE. Для SLOPE:
     * <ol>
     *     <li>желаемая высота - плавный (линейный) переход от groundY у дорожки к природной высоте на
     *     краю откоса (blendedTop);</li>
     *     <li>границы от всех "опорных" колонок (дом/дорожка на groundY и все KEEP): не дальше чем на
     *     расстояние до опоры - так откос гарантированно сходится к каждой из них не круче блока;</li>
     *     <li>несколько проходов выравнивания соседей - чтобы и внутри откоса не было перепада больше блока
     *     (природный рельеф под откосом сам по себе мог быть неровным).</li>
     * </ol>
     */
    private static int[][] solveHeights(Area area) {
        int groundY = area.house.minY();
        List<int[]> anchors = new ArrayList<>();
        int[][] heights = new int[area.size][area.size];
        for (int row = 0; row < area.size; row++) {
            for (int col = 0; col < area.size; col++) {
                Column column = area.columns[row][col];
                if (column == null) {
                    continue;
                }
                switch (column.role()) {
                    case HOUSE, WALKWAY -> {
                        heights[row][col] = groundY;
                        anchors.add(new int[]{row, col, groundY});
                    }
                    case KEEP -> {
                        heights[row][col] = column.top();
                        anchors.add(new int[]{row, col, column.top()});
                    }
                    default -> {
                    }
                }
            }
        }

        int[][] lowest = new int[area.size][area.size];
        int[][] highest = new int[area.size][area.size];
        for (int row = 0; row < area.size; row++) {
            for (int col = 0; col < area.size; col++) {
                Column column = area.columns[row][col];
                if (column == null || column.role() != Role.SLOPE) {
                    continue;
                }
                int low = Integer.MIN_VALUE;
                int high = Integer.MAX_VALUE;
                for (int[] anchor : anchors) {
                    int distance = Math.max(Math.abs(anchor[0] - row), Math.abs(anchor[1] - col));
                    low = Math.max(low, anchor[2] - distance);
                    high = Math.min(high, anchor[2] + distance);
                }
                lowest[row][col] = low;
                highest[row][col] = high;
                int wanted = blendedTop(groundY, distanceFromHouse(area.house, area.worldX(col), area.worldZ(row)), column.naturalTop());
                heights[row][col] = low <= high ? Math.clamp(wanted, low, high) : (low + high) / 2;
            }
        }

        for (int pass = 0; pass < RELAX_PASSES; pass++) {
            boolean changed = false;
            for (int row = 0; row < area.size; row++) {
                for (int col = 0; col < area.size; col++) {
                    Column column = area.columns[row][col];
                    if (column == null || column.role() != Role.SLOPE) {
                        continue;
                    }
                    int low = lowest[row][col];
                    int high = highest[row][col];
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if ((dr != 0 || dc != 0) && hasHeight(area, row + dr, col + dc)) {
                                low = Math.max(low, heights[row + dr][col + dc] - 1);
                                high = Math.min(high, heights[row + dr][col + dc] + 1);
                            }
                        }
                    }
                    if (low <= high) {
                        int settled = Math.clamp(heights[row][col], low, high);
                        changed |= settled != heights[row][col];
                        heights[row][col] = settled;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        return heights;
    }

    /** У колонки есть высота, с которой откос должен сходиться (всё, кроме зданий и края скана). */
    private static boolean hasHeight(Area area, int row, int col) {
        return area.inArea(row, col) && area.columns[row][col].role() != Role.BUILDING;
    }

    /** Желаемый верх в откосе: от groundY у дорожки линейно к природной высоте на краю откоса. */
    private static int blendedTop(int groundY, double distance, int naturalTop) {
        double blend = Math.min(1.0, (distance - 1) / Math.max(1, APRON_WIDTH - 1));
        return groundY + (int) Math.round((naturalTop - groundY) * blend);
    }

    /** По прямой от колонки до ближайшей колонки дома: 0 - под домом, 1 - вплотную (дорожка). Углы
     * откоса от этого скруглённые, а не квадратные. */
    private static double distanceFromHouse(BoundingBox box, int x, int z) {
        int outX = Math.max(0, Math.max(box.minX() - x, x - box.maxX()));
        int outZ = Math.max(0, Math.max(box.minZ() - z, z - box.maxZ()));
        return Math.sqrt(outX * outX + outZ * outZ);
    }

    /** Сколько шагов (включая диагональ) от колонки до дома - так же меряется "не круче блока на шаг". */
    private static int chebyshevFromHouse(BoundingBox box, int x, int z) {
        int outX = Math.max(0, Math.max(box.minX() - x, x - box.maxX()));
        int outZ = Math.max(0, Math.max(box.minZ() - z, z - box.maxZ()));
        return Math.max(outX, outZ);
    }

    // --- сами блоки ---

    private static void reshapeColumn(ServerLevel level, int x, int z, Column column, int newTop,
                                      List<Change> digs, List<Change> fills) {
        int currentTop = column.top();
        boolean underHouse = column.role() == Role.HOUSE;
        BlockState topBlock = level.getBlockState(new BlockPos(x, currentTop, z)).is(Blocks.DIRT_PATH) ? PATH : GRASS;

        // Срезать всё природное выше нового верха - и ещё один блок над старой землёй (трава/цветы),
        // чтобы после срезки ничего не висело в воздухе.
        for (int y = currentTop + 1; y > newTop; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (isDiggable(level, pos)) {
                digs.add(new Change(pos, AIR));
            }
        }
        // Насыпать от старой земли до нового верха. Под домом сам верх (groundY) кладёт нижний слой
        // шаблона, вне дома сверху - то же, что было (трава или тропинка).
        int fillToY = underHouse ? newTop - 1 : newTop;
        for (int y = currentTop + 1; y <= fillToY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (isFillable(level.getBlockState(pos))) {
                fills.add(new Change(pos, y == newTop ? topBlock : DIRT));
            }
        }
        // Срезанный откос/дорожка - сверху снова трава/тропинка, а не голая земля или камень.
        if (!underHouse && newTop < currentTop) {
            BlockPos top = new BlockPos(x, newTop, z);
            if (isDiggable(level, top) && !level.getBlockState(top).is(topBlock.getBlock())) {
                fills.add(new Change(top, topBlock));
            }
        }
    }

    /**
     * Верх природной земли в колонке по ЖИВОМУ миру. Ищем в пределах HOUSE_MAX_TERRAFORM+1 от высоты из
     * скана: дальше земляные работы её сдвинуть не могли. Растения и листва пропускаются; если сверху
     * вниз первым попалось что-то построенное (стена, дом, забор) - пусто: колонка - здание.
     */
    private static OptionalInt liveGroundTop(ServerLevel level, int x, int z, int scanTop) {
        int reach = VillageCalcSettings.HOUSE_MAX_TERRAFORM + 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = scanTop + reach; y >= scanTop - reach; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (isPlant(level, cursor, state)) {
                continue;
            }
            if (VillageBlocks.isStructureAt(level, cursor, state)) {
                return OptionalInt.empty();
            }
            if (isGround(state)) {
                return OptionalInt.of(y);
            }
        }
        return OptionalInt.of(scanTop);
    }

    /** Природная земля: земля/песок/камень/гравий/снег и т.п. (у VillageBlocks.isNaturalFloor имя
     * наоборот - она true для НЕ-природного, поэтому тут отрицание). */
    private static boolean isGround(BlockState state) {
        return VillageBlocks.vegetationOrAir(state)
                && !VillageBlocks.isTreePart(state)
                && !VillageBlocks.isNaturalFloor(state);
    }

    /** Растение (или что-то такое же проходимое): не воздух, не жидкость, сквозь него можно пройти.
     * Отдельно от VillageBlocks.isStructure: та по тегам, и цветок/куст без тега (из других модов)
     * считает постройкой. */
    private static boolean isPlant(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !VillageBlocks.isTreePart(state)
                && state.getCollisionShape(level, pos).isEmpty();
    }

    /** Что можно срезать: природная земля или растение. Не воздух, не жидкость, не дерево, не постройка. */
    private static boolean isDiggable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() || VillageBlocks.isTreePart(state)) {
            return false;
        }
        return !VillageBlocks.isStructure(state) || isPlant(level, pos, state);
    }

    /** Куда можно насыпать землю: воздух или то, что и так заменяется (трава, цветы, снег). */
    private static boolean isFillable(BlockState state) {
        return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty());
    }
}
