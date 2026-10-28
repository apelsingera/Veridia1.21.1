package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * "Это стена жителей?" для кода вне пакета - сейчас им пользуются зомби-ломатели блоков
 * (WitherBlockBreakGoal), чтобы стена деревни держалась дольше обычных построек.
 * <p>
 * Определяется по МЕСТУ: блок стоит на клетке стены из плана деревни (WallPlan, он считается заранее
 * для каждой деревни - см. VillageAutoPregen) и выше земли этой клетки (закопанная часть куска и сама
 * земля под стеной - не стена). Какие именно блоки там лежат, не важно: стена из scrap_wood, досок
 * или спрессованной грязи (апгрейд nailer'а) одинаково стена. Деревья и листва на клетке плана до
 * постройки стены - не стена.
 * <p>
 * План живёт в памяти и после перезапуска считается заново, когда рядом с деревней появляется игрок
 * (см. VillageAutoPregen), поэтому до этого замедления нет - там просто нечего проверять.
 */
public final class VillageWallLookup {

    private VillageWallLookup() {
    }

    // Ключ - сам WallPlan (сравнение по ссылке, без тяжёлого hashCode), слабый: план уходит из
    // VillageAutoPregen при остановке сервера, и кеш уходит вместе с ним.
    private static final Map<WallPlan, Set<Cell>> WALL_CELLS = new WeakHashMap<>();

    public static boolean isVillageWall(ServerLevel level, BlockPos pos, BlockState state) {
        if (VillageBlocks.isTreePart(state) || state.is(BlockTags.LEAVES)) {
            return false;
        }
        for (VillageAutoPregen.VillagePlan village : VillageAutoPregen.plans(level)) {
            VillageScan scan = village.scan();
            int cellX = pos.getX() - scan.minX();
            int cellZ = pos.getZ() - scan.minZ();
            if (scan.inBounds(cellX, cellZ)
                    && pos.getY() > scan.surfaceYAt(cellX, cellZ)
                    && wallCells(scan, village.plan()).contains(new Cell(cellX, cellZ))) {
                return true;
            }
        }
        return false;
    }

    private static synchronized Set<Cell> wallCells(VillageScan scan, WallPlan plan) {
        return WALL_CELLS.computeIfAbsent(plan, key -> collectWallCells(scan, key));
    }

    /**
     * Клетки, где стена стоит ИЛИ может встать: и куски/столбики плана, и то, что вырезано под башни
     * (если башню не построят, стена остаётся на месте выреза - см. WallPlan.segmentsFor), и тела
     * башен, и клетки загородок в воде.
     */
    private static Set<Cell> collectWallCells(VillageScan scan, WallPlan plan) {
        Set<Cell> cells = new HashSet<>();
        addSegments(cells, plan.segments());
        addFillers(cells, plan.fillers());
        addFillers(cells, plan.leakPatches());
        for (WallPlan.WaterCell water : plan.waterCells()) {
            cells.add(new Cell(water.cellX(), water.cellZ()));
        }
        for (WallPlan.TowerCut cut : plan.towerCuts()) {
            addSegments(cells, cut.removedSegments());
            addFillers(cells, cut.removedFillers());
            addFillers(cells, cut.jointFillers());
        }
        for (WallPlan.Tower tower : plan.towers()) {
            cells.addAll(TowerFootprint.bodyCells(scan, tower));
            cells.add(TowerFootprint.ladderCell(scan, tower));
        }
        return cells;
    }

    private static void addSegments(Set<Cell> cells, Collection<WallPlan.Segment> segments) {
        for (WallPlan.Segment segment : segments) {
            cells.addAll(segment.cells());
        }
    }

    private static void addFillers(Set<Cell> cells, Collection<WallPlan.Filler> fillers) {
        for (WallPlan.Filler filler : fillers) {
            cells.add(filler.cell());
        }
    }
}
