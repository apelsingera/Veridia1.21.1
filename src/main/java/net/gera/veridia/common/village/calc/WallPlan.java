package net.gera.veridia.common.village.calc;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WallPlan {

    public record Segment(int cellX, int cellZ, Direction along, Direction outward, boolean entrance) {

        /** Перепад высот земли под куском: самая высокая клетка минус самая низкая (непрогруженные не в счёт). */
        public int heightSpread(VillageScan scan) {
            int lowest = Integer.MAX_VALUE;
            int highest = Integer.MIN_VALUE;
            for (Cell cell : cells()) {
                if (scan.isMeasured(cell.x(), cell.z())) {
                    int height = scan.surfaceYAt(cell.x(), cell.z());
                    lowest = Math.min(lowest, height);
                    highest = Math.max(highest, height);
                }
            }
            return highest < lowest ? 0 : highest - lowest;
        }

        /** Слишком неровно для плоского куска: стена на склоне должна идти по рельефу, а не висеть на подпорках
         * над обрывом. Ворота не режутся - им нужен проход. */
        public boolean tooUneven(VillageScan scan) {
            return !entrance && heightSpread(scan) > VillageCalcSettings.SEGMENT_MAX_SPREAD;
        }

        /** Все PIECE_LENGTH клеток куска подряд вдоль along, начиная с (cellX, cellZ). */
        public List<Cell> cells() {
            List<Cell> cells = new ArrayList<>(VillageCalcSettings.PIECE_LENGTH);
            for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
                cells.add(new Cell(cellX + along.getStepX() * i, cellZ + along.getStepZ() * i));
            }
            return cells;
        }
    }

    public record Filler(int cellX, int cellZ) {

        public Cell cell() {
            return new Cell(cellX, cellZ);
        }
    }

    public record WaterCell(int cellX, int cellZ) {}

    /** Место для башни (вторая технология nailer'а - см. javadoc findTowers). outward - куда
     * повёрнут выступ/лестница башни (см. NailerWallQueue.decomposeTower - та же схема rotationFor,
     * что и у обычного куска стены). */
    public record Tower(int cellX, int cellZ, Direction outward) {}

    private final List<Segment> segments = new ArrayList<>();
    private final List<Filler> fillers = new ArrayList<>();
    private final List<WaterCell> waterCells = new ArrayList<>();
    private final List<Filler> leakPatches = new ArrayList<>();
    private final List<Tower> towers = new ArrayList<>();
    // Полные списки КАНДИДАТОВ (вся угловая/ровная разметка по стене) - отдельно от towers()
    // (итоговый выбор через pickTowersAlongPerimeter), нужны только для дебаг-визуализации
    // (см. VillageCalcDebug), реального отбора не определяют.
    private final List<Tower> greenCandidates = new ArrayList<>();
    private final List<Tower> yellowCandidates = new ArrayList<>();
    /** Что cutWallForTowers поменял в стене ради одной башни: removedSegments/removedFillers - что
     * было на месте её тела, jointFillers - столбики-стыки, которыми заменены куски removedSegments
     * вне тела (от башни до ближайшего целого куска). Нужно потребителю плана, чтобы откатить вырез,
     * если башню он в итоге НЕ строит (tech_2 не открыт, /calculate towers false, towerFits=false) -
     * см. segmentsFor/fillersFor. */
    public record TowerCut(Tower tower, List<Segment> removedSegments, List<Filler> removedFillers,
                           List<Filler> jointFillers) {}

    private final List<TowerCut> towerCuts = new ArrayList<>();
    private final List<Integer> entranceWeights = new ArrayList<>();
    private int candidateCount;
    private int strays;
    private int sealed;
    private int lowestCandidateWeight;
    private int highestCandidateWeight;
    // Диагностика findTowers - чтобы по /calculate start можно было сразу увидеть, ПОЧЕМУ башен
    // нет: мало углов вообще, или углы есть, но все забракованы по неровности (см. isFlatFootprint).
    private int towerBoundaryCells;
    private int towerCorners;
    private int towerCornersRejectedFlatness;

    // TOWER-DEBUG: временная диагностика стыков башня-стена (см. TowerJoinDebug) - на сам план не
    // влияет, только запоминает, какой этап buildPass добавил Filler (f/j/b/g/L - см. легенду в
    // дампе) и какие клетки выкинул dropStrays.
    private final Map<Cell, Character> debugFillerStage = new HashMap<>();
    private final Set<Cell> debugStrayRemoved = new HashSet<>();

    Map<Cell, Character> debugFillerStage() { return debugFillerStage; }
    Set<Cell> debugStrayRemoved() { return debugStrayRemoved; }

    private void debugNoteFillers(char stage) {
        for (Filler filler : fillers) {
            debugFillerStage.putIfAbsent(filler.cell(), stage);
        }
    }

    /** Все клетки, уже занятые куском стены или столбиком. */
    private Set<Cell> occupiedCells() {
        Set<Cell> cells = new HashSet<>();
        for (Segment segment : segments) {
            cells.addAll(segment.cells());
        }
        for (Filler filler : fillers) {
            cells.add(filler.cell());
        }
        return cells;
    }

    @NotNull
    public List<Segment> segments() { return segments; }
    public List<Filler> fillers() { return fillers; }
    public List<WaterCell> waterCells() { return waterCells; }
    public List<Filler> leakPatches() { return leakPatches; }
    public List<Tower> towers() { return towers; }
    public List<Tower> greenCandidates() { return greenCandidates; }
    public List<Tower> yellowCandidates() { return yellowCandidates; }
    public List<Integer> entranceWeights() { return entranceWeights; }
    public int candidateCount() { return candidateCount; }
    public int strays() { return strays; }
    public int sealed() { return sealed; }
    public int lowestCandidateWeight() { return lowestCandidateWeight; }
    public int highestCandidateWeight() { return highestCandidateWeight; }
    public int towerBoundaryCells() { return towerBoundaryCells; }
    public int towerCorners() { return towerCorners; }
    public int towerCornersRejectedFlatness() { return towerCornersRejectedFlatness; }
    public List<TowerCut> towerCuts() { return towerCuts; }

    /** Куски стены к постройке, если из towers() реально встанут только built: segments() уже
     * вырезаны под ВСЕ башни плана (см. cutWallForTowers), тут для каждой НЕ строящейся башни вырез
     * откатывается - иначе на её месте осталась бы дыра на всю ширину тела. */
    public List<Segment> segmentsFor(java.util.Collection<Tower> built) {
        List<Segment> result = new ArrayList<>(segments);
        for (TowerCut cut : cutsOfTowersNotIn(built)) {
            result.addAll(cut.removedSegments());
        }
        return result;
    }

    /** Пара к segmentsFor: для не строящейся башни убирает её стыки (их клетки снова покрывают
     * возвращённые куски) и возвращает столбики, стоявшие на месте тела. */
    public List<Filler> fillersFor(java.util.Collection<Tower> built) {
        List<Filler> result = new ArrayList<>(fillers);
        for (TowerCut cut : cutsOfTowersNotIn(built)) {
            result.removeAll(cut.jointFillers());
            result.addAll(cut.removedFillers());
        }
        return result;
    }

    private List<TowerCut> cutsOfTowersNotIn(java.util.Collection<Tower> built) {
        return towerCuts.stream().filter(cut -> !built.contains(cut.tower())).toList();
    }

    private record Face(int cellX, int cellZ, Direction outward) {}

    /**
     * Два прохода: первый строит план как обычно, второй - только если внутри получившегося
     * забора нашёлся "пустырь" (см. footprint/VillageScan.fillEnclosedByWall) - клетки не-
     * territory, до которых нельзя дойти от края скана, не пересекая саму стену. Забор мостит
     * разрывы territory шире (см. STRAY_JOIN_RANGE), чем закрывает territory-заращивание при
     * первичном сканировании (closeGaps/GAP_FILL_MAX, VillageScan.build) - без второго прохода
     * такой пустырь (визуально уже "внутри" целого забора) остался бы не-territory, и collect()
     * рисовал бы по его границе свою, лишнюю внутреннюю стену.
     */
    public static WallPlan build(VillageScan scan) {
        return build(scan, List.of());
    }

    /**
     * pinnedTowers - уже построенные башни, которые остаются на месте при расширении стены (см.
     * HouseSitePlan.chooseAtWall): те из них, что всё ещё стоят на контуре, считаются башнями плана, и на
     * их дугах новых не ставится; новые башни - только на дугах, где башни ещё нет.
     */
    public static WallPlan build(VillageScan scan, List<Tower> pinnedTowers) {
        WallPlan plan = buildPass(scan, pinnedTowers);
        if (scan.fillEnclosedByWall(footprint(scan, plan))) {
            plan = buildPass(scan, pinnedTowers);
        }
        return plan;
    }

    private static boolean[] footprint(VillageScan scan, WallPlan plan) {
        boolean[] cells = new boolean[scan.sizeX() * scan.sizeZ()];
        for (Segment segment : plan.segments) {
            for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
                mark(scan, cells, segment.cellX() + segment.along().getStepX() * i,
                        segment.cellZ() + segment.along().getStepZ() * i);
            }
        }
        for (Filler filler : plan.fillers) {
            mark(scan, cells, filler.cellX(), filler.cellZ());
        }
        // Под телом башни куски стены вырезаны (cutWallForTowers) - без этого fillEnclosedByWall
        // видел бы на месте каждой башни проход шириной в её тело и считал забор незамкнутым.
        markTowerCells(scan, cells, plan.towers);
        return cells;
    }

    private static WallPlan buildPass(VillageScan scan, List<Tower> pinnedTowers) {
        WallPlan plan = new WallPlan();

        List<Face> north = collect(scan, Direction.NORTH);
        List<Face> south = collect(scan, Direction.SOUTH);
        List<Face> west = collect(scan, Direction.WEST);
        List<Face> east = collect(scan, Direction.EAST);

        plan.tileRuns(scan, north, Direction.EAST, true);
        plan.tileRuns(scan, south, Direction.EAST, true);
        plan.tileRuns(scan, west, Direction.SOUTH, false);
        plan.tileRuns(scan, east, Direction.SOUTH, false);
        plan.debugNoteFillers('f'); // TOWER-DEBUG

        // findTowers - ДО dropStrays (было после): dropStrays решает, какие обрывки стены дропать
        // как "мусор" и куда мостить связность, и должен видеть башни как валидные точки связи -
        // иначе обрывок рядом с башней её попросту не замечает (см. отчёт игрока "игнорирует тот
        // факт что есть башня"). findTowers смотрит только в scan (не в segments/fillers), так что
        // переставить безопасно.
        plan.findTowers(scan, pinnedTowers);
        // Стык башни со стеной решается ЗДЕСЬ, в плане, а не на стройке (см. javadoc
        // cutWallForTowers) - было ensureTowerConnections, который по дампу TOWER-DEBUG ни разу
        // ничего не добавил (у всех башен "заглушек 0"), а дыры делала уже стройка.
        plan.cutWallForTowers(scan);
        plan.debugNoteFillers('j'); // TOWER-DEBUG
        Set<Cell> debugBeforeStrays = plan.occupiedCells(); // TOWER-DEBUG
        plan.dropStrays(scan);
        debugBeforeStrays.removeAll(plan.occupiedCells()); // TOWER-DEBUG
        plan.debugStrayRemoved.addAll(debugBeforeStrays); // TOWER-DEBUG
        plan.debugNoteFillers('b'); // TOWER-DEBUG
        plan.markEntrances(scan);
        plan.collectWaterGaps(scan);
        plan.sealNearWaterGaps(scan);
        plan.debugNoteFillers('g'); // TOWER-DEBUG
        plan.sealLeaks(scan);
        plan.debugNoteFillers('L'); // TOWER-DEBUG
        return plan;
    }

    /** Достраивает сухую "подходную" полосу между концом обычной стены и берегом - см. javadoc
     * VillageScan.facesWaterOutside(). collect() уже исключил эти клетки из обычных Segment'ов
     * (полноразмерный шаблон плохо ложится на неровный берег), но так и оставил их пустыми - тут
     * добираем их простыми столбиками-заглушками (Filler), как для кусков стены, не прошедших
     * fits() на стройке. Реальную воду не трогаем - её берёт на себя collectWaterGaps/WaterCell,
     * isBuildable() ниже её и так исключает (isWet=true). */
    private void sealNearWaterGaps(VillageScan scan) {
        boolean[] occupied = new boolean[scan.sizeX() * scan.sizeZ()];
        for (Segment segment : segments) {
            for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
                mark(scan, occupied, segment.cellX() + segment.along().getStepX() * i,
                        segment.cellZ() + segment.along().getStepZ() * i);
            }
        }
        for (Filler filler : fillers) {
            mark(scan, occupied, filler.cellX(), filler.cellZ());
        }
        markTowerCells(scan, occupied, towers);

        for (int cz = 0; cz < scan.sizeZ(); cz++) {
            for (int cx = 0; cx < scan.sizeX(); cx++) {
                if (occupied[cz * scan.sizeX() + cx]) {
                    continue;
                }
                if (scan.isBuildable(cx, cz) && scan.isBoundary(cx, cz)) {
                    fillers.add(new Filler(cx, cz));
                }
            }
        }
    }

    private void sealLeaks(VillageScan scan) {
        int sizeX = scan.sizeX();
        int sizeZ = scan.sizeZ();
        boolean[] occupied = new boolean[sizeX * sizeZ];
        for (Segment segment : segments) {
            for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
                mark(scan, occupied, segment.cellX() + segment.along().getStepX() * i,
                        segment.cellZ() + segment.along().getStepZ() * i);
            }
        }
        for (Filler filler : fillers) {
            mark(scan, occupied, filler.cellX(), filler.cellZ());
        }
        // Без этого поиск утечки считал место будущей башни проходимой пустотой и обходил/протекал
        // сквозь него зигзагом вместо того чтобы признать её преградой (см. отчёт игрока).
        markTowerCells(scan, occupied, towers);

        boolean[] passable = new boolean[sizeX * sizeZ];
        boolean[] territory = new boolean[sizeX * sizeZ];
        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                int i = cz * sizeX + cx;
                territory[i] = scan.isTerritory(cx, cz);
                passable[i] = !occupied[i] && !scan.isStructure(cx, cz) && !scan.isWater(cx, cz);
            }
        }

        List<Integer> breaches = TerritoryShaper.findBreaches(passable, territory, sizeX, sizeZ);
        for (int index : breaches) {
            Filler patch = new Filler(index % sizeX, index / sizeX);
            fillers.add(patch);
            leakPatches.add(patch);
        }
        sealed = breaches.size();
    }

    private void dropStrays(VillageScan scan) {
        strays = dropStrays(scan, segments, fillers, towers);
    }

    public static int dropStrays(VillageScan scan, List<Segment> segments, List<Filler> fillers,
                                  List<Tower> towers) {
        if (VillageCalcSettings.MIN_WALL_CHAIN <= 1) {
            return 0;
        }
        int sizeX = scan.sizeX();
        int sizeZ = scan.sizeZ();
        boolean[] occupied = new boolean[sizeX * sizeZ];
        for (Segment segment : segments) {
            for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
                mark(scan, occupied, segment.cellX() + segment.along().getStepX() * i,
                        segment.cellZ() + segment.along().getStepZ() * i);
            }
        }
        for (Filler filler : fillers) {
            mark(scan, occupied, filler.cellX(), filler.cellZ());
        }

        // Башня раньше вообще не участвовала тут (findTowers считался ПОСЛЕ dropStrays) - обрывок
        // стены рядом с башней её попросту не видел, и "чинить связность" мог только мостом к
        // другому куску стены, даже если башня была буквально ближе (см. отчёт игрока).
        boolean[] towerMark = new boolean[sizeX * sizeZ];
        markTowerCells(scan, towerMark, towers);
        // Мост (bridgeChain) не должен класть столбик и на клетку лестницы - замуровал бы её.
        for (Tower tower : towers) {
            Cell ladder = TowerFootprint.ladderCell(scan, tower);
            mark(scan, towerMark, ladder.x(), ladder.z());
        }
        for (int i = 0; i < occupied.length; i++) {
            occupied[i] |= towerMark[i];
        }

        int[] chain = new int[occupied.length];
        java.util.Arrays.fill(chain, -1);
        List<Integer> chainSize = new ArrayList<>();
        List<Boolean> chainHasTower = new ArrayList<>();
        Deque<int[]> queue = new ArrayDeque<>();

        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                int start = cz * sizeX + cx;
                if (!occupied[start] || chain[start] >= 0) {
                    continue;
                }
                int id = chainSize.size();
                int count = 0;
                boolean hasTower = towerMark[start];
                chain[start] = id;
                queue.add(new int[]{cx, cz});
                while (!queue.isEmpty()) {
                    int[] cell = queue.poll();
                    count++;
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = cell[0] + dx;
                            int nz = cell[1] + dz;
                            if ((dx == 0 && dz == 0) || !scan.inBounds(nx, nz)) {
                                continue;
                            }
                            int next = nz * sizeX + nx;
                            if (!occupied[next] || chain[next] >= 0) {
                                continue;
                            }
                            chain[next] = id;
                            hasTower |= towerMark[next];
                            queue.add(new int[]{nx, nz});
                        }
                    }
                }
                chainSize.add(count);
                chainHasTower.add(hasTower);
            }
        }

        int chains = chainSize.size();
        List<Set<Integer>> links = new ArrayList<>(chains);
        for (int i = 0; i < chains; i++) {
            links.add(new HashSet<>());
        }
        Map<Long, int[]> closest = new HashMap<>();
        int range = VillageCalcSettings.STRAY_JOIN_RANGE;
        for (int cz = 0; cz < sizeZ; cz++) {
            for (int cx = 0; cx < sizeX; cx++) {
                int here = chain[cz * sizeX + cx];
                if (here < 0) {
                    continue;
                }
                for (int dz = -range; dz <= range; dz++) {
                    for (int dx = -range; dx <= range; dx++) {
                        int nx = cx + dx;
                        int nz = cz + dz;
                        if (!scan.inBounds(nx, nz)) {
                            continue;
                        }
                        int other = chain[nz * sizeX + nx];
                        if (other < 0 || other == here) {
                            continue;
                        }
                        links.get(here).add(other);
                        long key = pairKey(here, other);
                        int distSq = dx * dx + dz * dz;
                        int[] best = closest.get(key);
                        if (best == null || distSq < best[0]) {
                            closest.put(key, new int[]{distSq, cx, cz, nx, nz});
                        }
                    }
                }
            }
        }

        // Мелкий обрывок раньше держался только если видел В ПРЕДЕЛАХ STRAY_JOIN_RANGE ещё 2+
        // других куска (links>=2) - для обычной развилки стены логично, но обрывок рядом ровно с
        // ОДНОЙ башней (и больше ни с чем) под это правило не подходит и удалялся целиком как
        // мусор, даже не доходя до моста (см. отчёт игрока - короткий кусок стены у башни пропадал).
        // Связь с башней сама по себе уже достаточная причина не считать обрывок мусором.
        boolean[] linksTower = new boolean[chains];
        for (int i = 0; i < chains; i++) {
            for (int other : links.get(i)) {
                if (chainHasTower.get(other)) {
                    linksTower[i] = true;
                    break;
                }
            }
        }

        boolean[] keep = new boolean[chains];
        for (int i = 0; i < chains; i++) {
            keep[i] = chainSize.get(i) >= VillageCalcSettings.MIN_WALL_CHAIN
                    || links.get(i).size() >= 2
                    || chainHasTower.get(i)
                    || linksTower[i];
        }

        int before = segments.size() + fillers.size();
        segments.removeIf(segment -> isStray(scan, chain, keep,
                segment.cellX(), segment.cellZ()));
        fillers.removeIf(filler -> isStray(scan, chain, keep,
                filler.cellX(), filler.cellZ()));
        int removed = before - segments.size() - fillers.size();

        if (VillageCalcSettings.BRIDGE_WALL_GAPS) {
            Set<Long> seen = new HashSet<>();
            for (Filler filler : fillers) {
                seen.add(cellKey(filler.cellX(), filler.cellZ()));
            }
            for (Segment segment : segments) {
                for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
                    seen.add(cellKey(segment.cellX() + segment.along().getStepX() * i,
                            segment.cellZ() + segment.along().getStepZ() * i));
                }
            }
            // Раньше мостили КАЖДУЮ пару цепочек, оказавшихся в пределах STRAY_JOIN_RANGE друг от
            // друга - если рядом кучковалось несколько мелких обрывков (частый случай на открытом
            // поле, где мало POI и цепочки короткие), получалось до C(n,2) мостов разом, они
            // пересекались и ветвились в кашу (см. отчёт игрока со скриншотом "конкретный пипец").
            // Тут вместо этого строим минимальное остовное дерево (Kruskal): сортируем пары по
            // расстоянию и мостим только те, что реально соединяют ещё не связанные компоненты -
            // результат тот же (всё соединено, дыр в периметре нет), мостов не больше chains-1.
            List<Map.Entry<Long, int[]>> candidates = new ArrayList<>(closest.entrySet());
            candidates.removeIf(entry -> entry.getValue()[0] <= 1);
            candidates.sort(Comparator.comparingInt(entry -> entry.getValue()[0]));

            int[] bridgeParent = new int[chains];
            for (int i = 0; i < chains; i++) {
                bridgeParent[i] = i;
            }

            for (Map.Entry<Long, int[]> entry : candidates) {
                long pairKey = entry.getKey();
                int a = (int) (pairKey >> 32);
                int b = (int) pairKey;
                if (!keep[a] || !keep[b]) {
                    continue;
                }
                int rootA = findBridgeRoot(bridgeParent, a);
                int rootB = findBridgeRoot(bridgeParent, b);
                if (rootA == rootB) {
                    continue;
                }
                bridgeParent[rootA] = rootB;
                int[] v = entry.getValue();
                // Раньше "continue" тут вообще отменял мост, если он проходил рядом с дорожкой -
                // в итоге между двумя иначе полными петлями стены оставалась постоянная дыра без
                // стены И без ворот (markEntrances видит только настоящие Segment, а не обрубок
                // моста). Стена без прохода в этом месте - меньшее зло, чем дыра в защите; если
                // тут действительно нужны ворота, а не глухая стена - это отдельная доработка
                // (нужно превращать мост в Segment с entrance=true, а не Filler-цепочку).
                bridgeChain(scan, fillers, seen, towerMark, v[1], v[2], v[3], v[4]);
            }
        }

        return removed;
    }

    private static int findBridgeRoot(int[] parent, int index) {
        while (parent[index] != index) {
            parent[index] = parent[parent[index]];
            index = parent[index];
        }
        return index;
    }

    private static long pairKey(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return (((long) lo) << 32) | (hi & 0xffffffffL);
    }

    private static long cellKey(int cx, int cz) {
        return (((long) cx) << 32) ^ (cz & 0xffffffffL);
    }

    private static List<int[]> lPath(int ax, int az, int bx, int bz) {
        List<int[]> path = new ArrayList<>();
        int x = ax;
        int z = az;
        path.add(new int[]{x, z});
        int stepX = Integer.signum(bx - ax);
        while (x != bx) {
            x += stepX;
            path.add(new int[]{x, z});
        }
        int stepZ = Integer.signum(bz - az);
        while (z != bz) {
            z += stepZ;
            path.add(new int[]{x, z});
        }
        return path;
    }

    private static void bridgeChain(VillageScan scan, List<Filler> fillers, Set<Long> seen,
                                    boolean[] towerMark, int ax, int az, int bx, int bz) {
        List<int[]> path = lPath(ax, az, bx, bz);
        for (int i = 1; i < path.size() - 1; i++) {
            int cx = path.get(i)[0];
            int cz = path.get(i)[1];
            if (!scan.inBounds(cx, cz)) {
                continue;
            }
            // Раньше мост клал Filler по прямой L-линии совсем без разбора, что под ней - если
            // между двумя цепями стены (в пределах STRAY_JOIN_RANGE) стоял непризнанный алгоритмом
            // дом/сарай (см. отчёт игрока "случайная заливка у дома"), столбики лепились прямо на
            // него/сквозь него. Тут просто пропускаем такие клетки - мост рвётся ровно на здании,
            // а не игнорирует его. Аналогично не лепим столбик туда, где сама встанет башня (та же
            // отметка, что и в dropStrays) - иначе получалась бы "дырка" ровно на стыке (столбик
            // считается частью моста, но по факту там уже будет структура башни).
            if (scan.isStructure(cx, cz) || towerMark[cz * scan.sizeX() + cx]) {
                continue;
            }
            if (seen.add(cellKey(cx, cz))) {
                fillers.add(new Filler(cx, cz));
            }
        }
    }

    /** Отмечает глухое тело башни (TowerFootprint.bodyCells - 3x3, БЕЗ клетки лестницы: сквозь неё
     * можно пройти) как занятое. Общий хелпер для sealLeaks/sealNearWaterGaps/dropStrays/footprint/
     * cutWallForTowers. Было "квадрат 5x5 вокруг анкера" - по дампу TOWER-DEBUG он не совпадал с
     * реальной башней (та у прямого участка стоит от анкера в +X/+Z, а не по центру): клетку стены
     * вплотную к телу со стороны анкера он считал "уже башней" - туда не ставились ни мост
     * dropStrays, ни заливка sealNearWaterGaps, и там оставалась дыра; а с другой стороны до
     * реального края башни не доставал. */
    private static void markTowerCells(VillageScan scan, boolean[] mark, List<Tower> towers) {
        for (Tower tower : towers) {
            for (Cell cell : TowerFootprint.bodyCells(scan, tower)) {
                mark(scan, mark, cell.x(), cell.z());
            }
        }
    }

    /**
     * Стык башни со стеной - прямо в плане, по реальному телу башни (TowerFootprint), а не на
     * стройке. Каждый кусок стены (Segment), который залезает на тело башни, вырезается целиком, а его
     * клетки ВНЕ тела становятся столбиками-стыками (Filler) - то есть линия стены идёт ровно до
     * грани башни с обеих сторон, без щели. Столбики, стоявшие на месте тела, убираются.
     * <p>
     * Почему не на стройке, как было: там кусок, задетый уже поставленной башней, не проходил fits() и
     * рассыпался на столбики, а ближайший к башне из них выкидывал nearTower (VillageCalc.addColumn) -
     * отсюда щель в 1 блок у каждой второй башни (у угловой со стороны лестницы - в 2). Второй
     * dropStrays эту щель не закрывал: он строит остовное дерево связности, а стена вокруг деревни -
     * кольцо, одна щель в кольце связности не рвёт. И наоборот, кусок, задевший только угловую колонну
     * башни, "влезал": она из брёвен, а isClear брёвна считает деревом, не препятствием - и
     * clearBox/placeInWorld такого куска затирал угол башни стеной.
     * <p>
     * Что вырезано, запоминается в towerCuts - если потребитель башню НЕ строит, он возвращает
     * исходные куски через segmentsFor/fillersFor (иначе на месте башни была бы дыра на всё тело).
     */
    private void cutWallForTowers(VillageScan scan) {
        for (Tower tower : towers) {
            Set<Cell> body = new HashSet<>(TowerFootprint.bodyCells(scan, tower));
            List<Segment> removedSegments = removeSegmentsOverlapping(body);
            List<Filler> removedFillers = removeFillersInside(body);
            List<Filler> joints = addJointFillers(removedSegments, body);
            towerCuts.add(new TowerCut(tower, removedSegments, removedFillers, joints));
        }
    }

    private List<Segment> removeSegmentsOverlapping(Set<Cell> body) {
        List<Segment> removed = segments.stream()
                .filter(segment -> segment.cells().stream().anyMatch(body::contains))
                .toList();
        segments.removeAll(removed);
        return removed;
    }

    private List<Filler> removeFillersInside(Set<Cell> body) {
        List<Filler> removed = fillers.stream()
                .filter(filler -> body.contains(filler.cell()))
                .toList();
        fillers.removeAll(removed);
        return removed;
    }

    /** Клетки вырезанных кусков вне тела башни, которые больше ничем не покрыты, - теперь столбики. */
    private List<Filler> addJointFillers(List<Segment> removedSegments, Set<Cell> body) {
        Set<Cell> covered = occupiedCells();
        covered.addAll(body);
        List<Filler> joints = new ArrayList<>();
        for (Segment segment : removedSegments) {
            for (Cell cell : segment.cells()) {
                if (covered.add(cell)) {
                    Filler joint = new Filler(cell.x(), cell.z());
                    fillers.add(joint);
                    joints.add(joint);
                }
            }
        }
        return joints;
    }

    private static void mark(VillageScan scan, boolean[] occupied, int cx, int cz) {
        if (scan.inBounds(cx, cz)) {
            occupied[cz * scan.sizeX() + cx] = true;
        }
    }

    private static boolean isStray(VillageScan scan, int[] chain, boolean[] keep, int cx, int cz) {
        if (!scan.inBounds(cx, cz)) {
            return true;
        }
        int id = chain[cz * scan.sizeX() + cx];
        return id < 0 || !keep[id];
    }

    /** Клетки САМОЙ воды на границе territory - по ним потом строится барьер (OAK_FENCE,
     * см. VillageCalc.placeWaterBarrier/NailerWallQueue.decomposeWaterCell). Сухой подход к
     * берегу сюда не входит - это отдельно sealNearWaterGaps(), см. её javadoc. */
    private void collectWaterGaps(VillageScan scan) {
        for (int cz = 0; cz < scan.sizeZ(); cz++) {
            for (int cx = 0; cx < scan.sizeX(); cx++) {
                if (scan.isBoundary(cx, cz) && scan.isWater(cx, cz) && !scan.isStructure(cx, cz)) {
                    waterCells.add(new WaterCell(cx, cz));
                }
            }
        }
    }

    private static List<Face> collect(VillageScan scan, Direction outward) {
        List<Face> faces = new ArrayList<>();
        for (int cz = 0; cz < scan.sizeZ(); cz++) {
            for (int cx = 0; cx < scan.sizeX(); cx++) {
                if (!scan.isBuildable(cx, cz)) {
                    continue;
                }
                if (scan.facesWaterOutside(cx, cz)) {
                    continue;
                }
                if (!scan.isTerritory(cx + outward.getStepX(), cz + outward.getStepZ())) {
                    faces.add(new Face(cx, cz, outward));
                }
            }
        }
        return faces;
    }

    private void tileRuns(VillageScan scan, List<Face> faces, Direction along, boolean alongX) {
        if (faces.isEmpty()) {
            return;
        }
        faces.sort(alongX
                ? Comparator.<Face>comparingInt(Face::cellZ).thenComparingInt(Face::cellX)
                : Comparator.<Face>comparingInt(Face::cellX).thenComparingInt(Face::cellZ));

        List<Face> run = new ArrayList<>();
        for (Face face : faces) {
            if (!run.isEmpty() && !continuesRun(run.getLast(), face, alongX)) {
                tileRun(run, along);
                run.clear();
            }
            run.add(face);
        }
        tileRun(run, along);
    }

    private static boolean continuesRun(Face previous, Face next, boolean alongX) {
        if (alongX) {
            return previous.cellZ() == next.cellZ() && next.cellX() == previous.cellX() + 1;
        }
        return previous.cellX() == next.cellX() && next.cellZ() == previous.cellZ() + 1;
    }

    private void tileRun(List<Face> run, Direction along) {
        if (run.isEmpty()) {
            return;
        }
        int length = run.size();
        int piece = VillageCalcSettings.PIECE_LENGTH;
        int whole = length / piece;

        for (int i = 0; i < whole; i++) {
            Face start = run.get(i * piece);
            segments.add(new Segment(start.cellX(), start.cellZ(), along, start.outward(), false));
        }
        for (int i = whole * piece; i < length; i++) {
            Face tail = run.get(i);
            fillers.add(new Filler(tail.cellX(), tail.cellZ()));
        }
    }

    private void markEntrances(VillageScan scan) {
        Map<Integer, Segment> aligned = new HashMap<>();
        List<Integer> candidates = new ArrayList<>();
        Set<Cell> towerBodies = new HashSet<>();
        for (Tower tower : towers) {
            towerBodies.addAll(TowerFootprint.bodyCells(scan, tower));
        }
        for (int i = 0; i < segments.size(); i++) {
            Segment centered = centeredOnPath(scan, segments.get(i));
            if (centered == null) {
                continue;
            }
            // Сдвиг по тропинке не должен заезжать на тело башни (куски под ней уже вырезаны, см.
            // cutWallForTowers) - тогда ворота остаются на исходном месте куска.
            if (centered.cells().stream().anyMatch(towerBodies::contains)) {
                centered = segments.get(i);
            }
            aligned.put(i, centered);
            candidates.add(i);
        }
        candidates.removeIf(i -> weightAtSegment(scan, aligned.get(i))
                < VillageCalcSettings.MIN_GATE_VIRTUAL_WEIGHT);
        candidates.removeIf(i -> facesWater(scan, aligned.get(i)));
        if (candidates.isEmpty()) {
            return;
        }

        candidateCount = candidates.size();
        candidates.sort(Comparator.comparingInt(i -> weightAtSegment(scan, aligned.get(i))));
        lowestCandidateWeight = weightAtSegment(scan, aligned.get(candidates.get(0)));
        highestCandidateWeight = weightAtSegment(scan, aligned.get(candidates.get(candidates.size() - 1)));

        List<Segment> chosen = new ArrayList<>();
        for (int index : candidates) {
            Segment segment = aligned.get(index);
            if (tooCloseToChosen(segment, chosen)) {
                continue;
            }
            segments.set(index, new Segment(segment.cellX(), segment.cellZ(),
                    segment.along(), segment.outward(), true));
            entranceWeights.add(weightAtSegment(scan, segment));
            chosen.add(segment);
        }
    }

    /** Сдвигает кандидата так, чтобы центр 5-блочного куска (а не его случайный край, как
     * изначально нарезал tileRun) совпадал с точкой, где дорожка реально подходит к стене -
     * иначе ворота ставились по центру куска стены, а не по центру дорожки, и визуально "съезжали"
     * в сторону от тропинки. Если сдвинутое место перестаёт быть валидным куском границы
     * (например упирается в угол) - откатываемся на исходное положение куска. */
    @Nullable
    private static Segment centeredOnPath(VillageScan scan, Segment segment) {
        int[] touch = findPathTouch(scan, segment);
        if (touch == null) {
            return null;
        }
        boolean alongX = segment.along().getStepX() != 0;
        int half = VillageCalcSettings.PIECE_LENGTH / 2;
        int centerCoord = (alongX ? touch[0] : touch[1]) - half;
        int newCellX = alongX ? centerCoord : segment.cellX();
        int newCellZ = alongX ? segment.cellZ() : centerCoord;
        Segment recentered = new Segment(newCellX, newCellZ, segment.along(), segment.outward(), false);
        return validSegmentPosition(scan, recentered) ? recentered : segment;
    }

    private static boolean validSegmentPosition(VillageScan scan, Segment segment) {
        Direction outward = segment.outward();
        for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
            int cx = segment.cellX() + segment.along().getStepX() * i;
            int cz = segment.cellZ() + segment.along().getStepZ() * i;
            if (!scan.isBuildable(cx, cz) || scan.facesWaterOutside(cx, cz)) {
                return false;
            }
            if (scan.isTerritory(cx + outward.getStepX(), cz + outward.getStepZ())) {
                return false;
            }
        }
        return true;
    }
    private static boolean tooCloseToChosen(Segment candidate, List<Segment> chosen) {
        for (Segment gate : chosen) {
            if (VillageCalcSettings.GATES_ON_DIFFERENT_SIDES && gate.outward() == candidate.outward()) {
                return true;
            }
            int dx = Math.abs(gate.cellX() - candidate.cellX());
            int dz = Math.abs(gate.cellZ() - candidate.cellZ());
            if (Math.max(dx, dz) < VillageCalcSettings.MIN_GATE_DISTANCE) {
                return true;
            }
        }
        return false;
    }
    private static boolean facesWater(VillageScan scan, Segment segment) {
        Direction outward = segment.outward();
        for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
            int cx = segment.cellX() + segment.along().getStepX() * i;
            int cz = segment.cellZ() + segment.along().getStepZ() * i;
            for (int step = 0; step <= VillageCalcSettings.GATE_WATER_LOOKUP; step++) {
                if (scan.isWater(cx + outward.getStepX() * step, cz + outward.getStepZ() * step)) {
                    return true;
                }
            }
        }
        return false;
    }
    private static int weightAtSegment(VillageScan scan, Segment segment) {
        int half = VillageCalcSettings.PIECE_LENGTH / 2;
        int cx = segment.cellX() + segment.along().getStepX() * half;
        int cz = segment.cellZ() + segment.along().getStepZ() * half;
        return scan.virtualWeightAround(cx, cz, VillageCalcSettings.PATH_AREA);
    }
    @Nullable
    private static int[] findPathTouch(VillageScan scan, Segment segment) {
        Direction inward = segment.outward().getOpposite();
        for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
            int cx = segment.cellX() + segment.along().getStepX() * i;
            int cz = segment.cellZ() + segment.along().getStepZ() * i;
            for (int step = 0; step <= VillageCalcSettings.GATE_PATH_TOUCH_RANGE; step++) {
                int px = cx + inward.getStepX() * step;
                int pz = cz + inward.getStepZ() * step;
                if (scan.isPathBlock(px, pz)) {
                    return new int[]{px, pz};
                }
            }
        }
        return null;
    }

    // --- башни (вторая технология nailer'а - tech_2, см. NailerUpgrades.isTech2Unlocked) ---

    private static final Direction[][] CORNER_PAIRS = {
            {Direction.NORTH, Direction.EAST},
            {Direction.NORTH, Direction.WEST},
            {Direction.SOUTH, Direction.EAST},
            {Direction.SOUTH, Direction.WEST},
    };

    /**
     * Зелёная/жёлтая классификация (ровный угол / ровный прямой участок, см. описание ниже) тут
     * остаётся только ради дебаг-визуализации (/calculate - показывает ВСЕ такие клетки по всей
     * стене, greenCandidates/yellowCandidates). Реальный отбор итоговых towers() - НЕ по этим
     * спискам, а через pickTowersAlongPerimeter (см. её javadoc) - обход периметра, а не перебор
     * всех клеток.
     */
    private void findTowers(VillageScan scan, List<Tower> pinnedTowers) {
        List<Tower> green = new ArrayList<>();
        List<Tower> yellow = new ArrayList<>();

        for (int cz = 0; cz < scan.sizeZ(); cz++) {
            for (int cx = 0; cx < scan.sizeX(); cx++) {
                if (!scan.isBuildable(cx, cz) || !scan.isBoundary(cx, cz)) {
                    continue;
                }
                towerBoundaryCells++;
                Direction[] corner = convexCornerAt(scan, cx, cz);
                boolean flat = isFlatFootprint(scan, cx, cz);
                if (corner != null) {
                    towerCorners++;
                    if (flat) {
                        green.add(new Tower(cx, cz, corner[0]));
                    } else {
                        towerCornersRejectedFlatness++;
                    }
                    continue;
                }
                if (flat) {
                    Direction outward = singleOutwardDirection(scan, cx, cz);
                    if (outward != null) {
                        yellow.add(new Tower(cx, cz, outward));
                    }
                }
            }
        }

        greenCandidates.addAll(green);
        yellowCandidates.addAll(yellow);

        towers.addAll(pickTowersAlongPerimeter(scan, pinnedTowers));
    }

    // --- расстановка башен по периметру (по требованию игрока - см. историю: раньше отбор
    // (greedyPickTowers) мерил расстояние между кандидатами по ПРЯМОЙ, из-за чего на изрезанной
    // стене две башни на разных, но близких по прямой изгибах реально оказывались рядом друг с
    // другом при хождении вдоль забора - см. отчёт игрока со скриншотом. Вместо этого сам периметр
    // стены (обход контура territory, а не перебор кандидатов) делится на MAX_TOWERS равных по
    // длине дуг, и в центре каждой ищется ближайшее валидное место (ровный угол/прямой участок). ---

    /** Соседи по часовой стрелке начиная с севера (N,NE,E,SE,S,SW,W,NW) - см. traceBoundary. */
    private static final int[] TRACE_DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] TRACE_DZ = {-1, -1, 0, 1, 1, 1, 0, -1};

    /**
     * Обход внешнего контура territory (Moore-neighbour tracing - стандартный алгоритм трассировки
     * границы залитой фигуры на сетке): начинаем с самой верхней-левой клетки territory (она
     * гарантированно на границе - выше и левее неё в порядке скана ничего нет), дальше на каждом
     * шаге ищем следующую territory-клетку среди 8 соседей по часовой стрелке, начиная сразу ПОСЛЕ
     * направления, откуда пришли - это и есть правило, которое держит обход строго по контуру, а не
     * ныряет внутрь фигуры. Останавливаемся, когда вернулись в стартовую клетку с тем же направлением
     * прихода, что и в начале (иначе можно остановиться преждевременно, зайдя в старт с другой
     * стороны).
     * <p>
     * Ограничение: если territory состоит из НЕСКОЛЬКИХ несвязанных кусков (редкий случай - обычно
     * VillageScan.TerritoryShaper их либо соединяет, либо отбрасывает все кроме главного), тут
     * обходится только тот, что содержит самую верхнюю-левую клетку - остальные останутся без башен
     * от этого прохода. Не критично для типичной деревни (один сплошной кусок), но если игрок
     * когда-нибудь увидит деревню с несколькими явно разъединёнными участками стены без башен на
     * "дальних" - вот причина.
     */
    @Nullable
    private static List<int[]> traceBoundary(VillageScan scan) {
        int startX = -1;
        int startZ = -1;
        outer:
        for (int cz = 0; cz < scan.sizeZ(); cz++) {
            for (int cx = 0; cx < scan.sizeX(); cx++) {
                if (scan.isTerritory(cx, cz)) {
                    startX = cx;
                    startZ = cz;
                    break outer;
                }
            }
        }
        if (startX < 0) {
            return null;
        }

        List<int[]> boundary = new ArrayList<>();
        int cx = startX;
        int cz = startZ;
        int backtrackDir = 6;
        int firstNextX = Integer.MIN_VALUE;
        int firstNextZ = Integer.MIN_VALUE;
        int maxSteps = scan.sizeX() * scan.sizeZ() * 8 + 8;
        for (int steps = 0; steps < maxSteps; steps++) {
            int nextX = 0;
            int nextZ = 0;
            int nextBacktrack = -1;
            for (int k = 1; k <= 8; k++) {
                int dirIdx = (backtrackDir + k) % 8;
                int nx = cx + TRACE_DX[dirIdx];
                int nz = cz + TRACE_DZ[dirIdx];
                if (scan.isTerritory(nx, nz)) {
                    nextX = nx;
                    nextZ = nz;
                    nextBacktrack = (dirIdx + 4) % 8;
                    break;
                }
            }
            if (nextBacktrack < 0) {
                // Изолированная клетка без соседей - контур из одной клетки.
                boundary.add(new int[]{cx, cz});
                break;
            }
            if (cx == startX && cz == startZ) {
                if (firstNextX == Integer.MIN_VALUE) {
                    firstNextX = nextX;
                    firstNextZ = nextZ;
                } else if (nextX == firstNextX && nextZ == firstNextZ) {
                    break;
                }
            }
            boundary.add(new int[]{cx, cz});
            cx = nextX;
            cz = nextZ;
            backtrackDir = nextBacktrack;
        }

        return boundary;
    }

    /** cumulative[i] - расстояние по контуру от boundary.get(0) до boundary.get(i) (i < size),
     * cumulative[size] - полная длина периметра (включая шаг закрытия от последней клетки обратно
     * к первой). Диагональные шаги (Moore tracing использует 8-связность) считаются как sqrt(2), не
     * как 1 - иначе "равные дуги" были бы чуть смещены на изрезанных углах. */
    private static double[] cumulativeDistances(List<int[]> boundary) {
        int size = boundary.size();
        double[] cumulative = new double[size + 1];
        for (int i = 0; i < size; i++) {
            int[] cur = boundary.get(i);
            int[] next = boundary.get((i + 1) % size);
            double dx = next[0] - cur[0];
            double dz = next[1] - cur[1];
            cumulative[i + 1] = cumulative[i] + Math.sqrt(dx * dx + dz * dz);
        }
        return cumulative;
    }

    /** Индекс в boundary, чья cumulative-дистанция ближе всего к target (двоичный поиск по уже
     * отсортированному по построению массиву cumulative[0..size-1]). */
    private static int nearestIndex(double[] cumulative, int size, double target) {
        int lo = 0;
        int hi = size - 1;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (cumulative[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        if (lo > 0 && Math.abs(cumulative[lo - 1] - target) < Math.abs(cumulative[lo] - target)) {
            return lo - 1;
        }
        return lo;
    }

    /** Индекс клетки башни в обходе контура; -1, если после изменения территории она уже не на контуре. */
    private static int indexOnBoundary(List<int[]> boundary, Tower tower) {
        for (int i = 0; i < boundary.size(); i++) {
            int[] cell = boundary.get(i);
            if (cell[0] == tower.cellX() && cell[1] == tower.cellZ()) {
                return i;
            }
        }
        return -1;
    }

    /** Собирает Tower из клетки периметра по её индексу - угол, если это реально угол
     * (convexCornerAt), иначе прямой участок (singleOutwardDirection). */
    @Nullable
    private static Tower towerAt(VillageScan scan, List<int[]> boundary, int index) {
        int[] cell = boundary.get(index);
        int cx = cell[0];
        int cz = cell[1];
        Direction[] corner = convexCornerAt(scan, cx, cz);
        Direction outward = corner != null ? corner[0] : singleOutwardDirection(scan, cx, cz);
        return outward == null ? null : new Tower(cx, cz, outward);
    }

    /** От baseIndex расширяем поиск по контуру в обе стороны (+1,-1,+2,-2...) до searchRadius шагов,
     * пока не найдём клетку, где реально можно поставить башню (ровный рельеф + угол или прямой
     * участок - та же проверка, что и в findTowers, только тут не по всей стене, а вокруг конкретной
     * точки на дуге). used - уже занятые другими башнями клетки этого же вызова (защита от того,
     * что две соседние по номеру, но короткие дуги случайно схлопнутся в одну и ту же точку).
     * Место рядом с дорожкой пропускается (см. nearPath) - башня глухая и перекрыла бы проход; место у
     * обрыва или ямы - тоже (см. TowerFootprint.nearDrop). */
    private static int findValidTowerIndexNear(VillageScan scan, List<int[]> boundary, int baseIndex,
                                                int searchRadius, Set<Long> used) {
        int size = boundary.size();
        for (int offset = 0; offset <= searchRadius; offset++) {
            int[] signs = offset == 0 ? new int[]{0} : new int[]{offset, -offset};
            for (int sign : signs) {
                int idx = ((baseIndex + sign) % size + size) % size;
                int[] cell = boundary.get(idx);
                int cx = cell[0];
                int cz = cell[1];
                if (used.contains(cellKey(cx, cz)) || !scan.isBuildable(cx, cz) || !isFlatFootprint(scan, cx, cz)) {
                    continue;
                }
                Tower candidate = towerAt(scan, boundary, idx);
                if (candidate != null && TowerFootprint.fitsTerritory(scan, candidate) && !nearPath(scan, candidate)
                        && !TowerFootprint.nearDrop(scan, candidate)) {
                    return idx;
                }
            }
        }
        return -1;
    }

    /** По дампу TOWER-DEBUG две башни из шести (в двух разных деревнях) встали ровно на кусок-вход:
     * тропинка упиралась в стену, markEntrances (он идёт позже) делал там ворота, а стройка затирала
     * их глухим телом башни - деревня оставалась без выхода. Тут башня просто не ставится туда, где
     * в пределах TOWER_PATH_CLEARANCE от её реального bbox есть блок тропинки (тот же isPathBlock, по
     * которому markEntrances ищет место под ворота) - поиск по дуге идёт дальше. */
    private static boolean nearPath(VillageScan scan, Tower tower) {
        int clearance = VillageCalcSettings.TOWER_PATH_CLEARANCE;
        if (clearance < 0) {
            return false;
        }
        TowerFootprint.Box box = TowerFootprint.box(scan, tower);
        for (int cz = box.minZ() - clearance; cz <= box.maxZ() + clearance; cz++) {
            for (int cx = box.minX() - clearance; cx <= box.maxX() + clearance; cx++) {
                if (scan.isPathBlock(cx, cz)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Делит периметр на дуги и ставит башню в центре каждой; на дуге, где уже стоит закреплённая башня
     * (pinnedTowers - построенные раньше), новая не ставится. */
    private List<Tower> pickTowersAlongPerimeter(VillageScan scan, List<Tower> pinnedTowers) {
        List<int[]> boundary = traceBoundary(scan);
        if (boundary == null || boundary.isEmpty()) {
            return List.of();
        }
        double[] cumulative = cumulativeDistances(boundary);
        double perimeterLength = cumulative[boundary.size()];
        int sections = Math.min(VillageCalcSettings.MAX_TOWERS,
                (int) (perimeterLength / VillageCalcSettings.MIN_TOWER_DISTANCE));
        if (sections <= 0) {
            return List.of();
        }

        List<Tower> result = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        boolean[] sectionTaken = new boolean[sections];
        for (Tower pinned : pinnedTowers) {
            int index = indexOnBoundary(boundary, pinned);
            if (index < 0 || !pinned.equals(towerAt(scan, boundary, index))) {
                continue;
            }
            result.add(pinned);
            used.add(cellKey(pinned.cellX(), pinned.cellZ()));
            sectionTaken[Math.min(sections - 1, (int) (cumulative[index] / perimeterLength * sections))] = true;
        }
        int searchRadius = Math.max(4, boundary.size() / (2 * sections));
        for (int i = 0; i < sections; i++) {
            if (sectionTaken[i]) {
                continue;
            }
            double target = (i + 0.5) * perimeterLength / sections;
            int baseIndex = nearestIndex(cumulative, boundary.size(), target);
            int foundIndex = findValidTowerIndexNear(scan, boundary, baseIndex, searchRadius, used);
            Tower tower = foundIndex < 0 ? null : towerAt(scan, boundary, foundIndex);
            if (tower != null) {
                result.add(tower);
                used.add(cellKey(tower.cellX(), tower.cellZ()));
            }
        }
        return result;
    }

    /** Выпуклый угол - клетка, у которой РОВНО два соседних (перпендикулярных) направления смотрят
     * наружу territory, диагональ между ними тоже снаружи (иначе это просто узкий выступ в 1
     * клетку по толщине стены, а не настоящий угол), а противоположные направления - внутри
     * territory (иначе это перешеек/дыра в стене, а не внешний угол забора). */
    @Nullable
    private static Direction[] convexCornerAt(VillageScan scan, int cx, int cz) {
        for (Direction[] pair : CORNER_PAIRS) {
            Direction d1 = pair[0];
            Direction d2 = pair[1];
            boolean open1 = !scan.isTerritory(cx + d1.getStepX(), cz + d1.getStepZ());
            boolean open2 = !scan.isTerritory(cx + d2.getStepX(), cz + d2.getStepZ());
            boolean openDiagonal = !scan.isTerritory(cx + d1.getStepX() + d2.getStepX(), cz + d1.getStepZ() + d2.getStepZ());
            boolean closedOpposite1 = scan.isTerritory(cx - d1.getStepX(), cz - d1.getStepZ());
            boolean closedOpposite2 = scan.isTerritory(cx - d2.getStepX(), cz - d2.getStepZ());
            if (open1 && open2 && openDiagonal && closedOpposite1 && closedOpposite2) {
                return pair;
            }
        }
        return null;
    }

    /** Клетка границы, у которой ровно ОДНО кардинальное направление смотрит наружу - обычный
     * прямой участок стены (не угол и не узкий выступ на 1 клетку). */
    @Nullable
    private static Direction singleOutwardDirection(VillageScan scan, int cx, int cz) {
        Direction found = null;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (!scan.isTerritory(cx + d.getStepX(), cz + d.getStepZ())) {
                if (found != null) {
                    return null;
                }
                found = d;
            }
        }
        return found;
    }

    /** Перепад высоты рельефа под дном башни - было half = TOWER_FOOTPRINT_MAX (радиус 4, то есть
     * требовалась ровная площадка 9x9), хотя сама башня 3x4x4 - на реальном рельефе (грядка на
     * блок ниже травы, тропинка, берег) такой большой пятак почти нигде не набирался даже в
     * честном углу. По требованию - минимум 3x3 (TOWER_FOOTPRINT_MIN). */
    private static boolean isFlatFootprint(VillageScan scan, int cx, int cz) {
        int base = scan.surfaceYAt(cx, cz);
        int half = (VillageCalcSettings.TOWER_FOOTPRINT_MIN - 1) / 2;
        for (int dz = -half; dz <= half; dz++) {
            for (int dx = -half; dx <= half; dx++) {
                if (!scan.inBounds(cx + dx, cz + dz)) {
                    return false;
                }
                if (Math.abs(scan.surfaceYAt(cx + dx, cz + dz) - base) > VillageCalcSettings.TOWER_FLATNESS_TOLERANCE) {
                    return false;
                }
            }
        }
        return true;
    }
}