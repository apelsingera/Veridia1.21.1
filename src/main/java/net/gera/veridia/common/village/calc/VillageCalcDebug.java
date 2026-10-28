package net.gera.veridia.common.village.calc;

import net.gera.veridia.Veridia;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.List;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class VillageCalcDebug {

    private VillageCalcDebug() {
    }

    public static final int SHOW_TICKS = 20 * 20;
    public static final int REDRAW_EVERY = 10;
    public static final int MAX_PARTICLES_PER_PASS = 5000;
    public static final double PARTICLE_Y_OFFSET = 1.2;

    private static final Vector3f HOUSE_COLOR = new Vector3f(0.1F, 1.0F, 0.1F);
    private static final Vector3f MISC_COLOR = new Vector3f(0.2F, 0.4F, 1.0F);
    private static final Vector3f PATH_COLOR = new Vector3f(1.0F, 0.6F, 0.0F);
    private static final Vector3f TERRITORY_COLOR = new Vector3f(0.55F, 0.55F, 0.6F);
    private static final Vector3f BOUNDARY_COLOR = new Vector3f(1.0F, 0.05F, 0.05F);
    private static final Vector3f WATER_COLOR = new Vector3f(0.0F, 0.9F, 1.0F);
    private static final Vector3f LEAK_COLOR = new Vector3f(1.0F, 0.95F, 0.1F);
    // Башни (tech_2, см. WallPlan.findTowers) - зелёный/жёлтый показывают ВСЕХ кандидатов (для
    // проверки угловой/ровной разметки), белый акцент сверху - какие из них реально прошли отбор
    // (greedyPickTowers). Выше обычных частиц (см. TOWER_PARTICLE_Y_OFFSET) - видно издалека даже
    // из-за самой стены.
    private static final Vector3f TOWER_GREEN_COLOR = new Vector3f(0.05F, 1.0F, 0.05F);
    private static final Vector3f TOWER_YELLOW_COLOR = new Vector3f(1.0F, 0.85F, 0.0F);
    private static final Vector3f TOWER_CHOSEN_COLOR = new Vector3f(1.0F, 1.0F, 1.0F);
    private static final double TOWER_PARTICLE_Y_OFFSET = 3.0;
    // Дома (tech_3, см. HouseSitePlan) - пока только показ отобранных площадок, самой постройки нет.
    // Отдельный цвет (розовый), чтобы не путать с чем-то из уже существующей визуализации.
    private static final Vector3f HOUSE_SITE_COLOR = new Vector3f(1.0F, 0.1F, 1.0F);

    private static ServerLevel level;
    private static VillageScan scan;
    private static WallPlan plan;
    private static List<HouseSitePlan.HouseSite> houseSites;
    private static int ticksLeft;

    public static void show(ServerLevel serverLevel, VillageScan villageScan, WallPlan wallPlan,
                             List<HouseSitePlan.HouseSite> houseSitePlan) {
        level = serverLevel;
        scan = villageScan;
        plan = wallPlan;
        houseSites = houseSitePlan;
        ticksLeft = SHOW_TICKS;
    }

    public static void stop() {
        level = null;
        scan = null;
        plan = null;
        houseSites = null;
        ticksLeft = 0;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (scan == null || level == null || ticksLeft <= 0) {
            return;
        }
        ticksLeft--;
        if (ticksLeft % REDRAW_EVERY != 0) {
            return;
        }
        draw();
    }

    private static void draw() {
        int budget = MAX_PARTICLES_PER_PASS;

        for (VillageScan.Marker marker : scan.markers()) {
            if (budget <= 0) {
                return;
            }
            Vector3f color = switch (marker.category()) {
                case HOUSE -> HOUSE_COLOR;
                case MISC, STORAGE -> MISC_COLOR;
                case PATH -> PATH_COLOR;
            };
            spawn(new DustParticleOptions(color, 1.6F),
                    marker.pos().getX(), marker.pos().getY(), marker.pos().getZ());
            budget--;
        }

        if (plan != null) {
            DustParticleOptions waterDust = new DustParticleOptions(WATER_COLOR, 1.4F);
            for (WallPlan.WaterCell cell : plan.waterCells()) {
                if (budget <= 0) {
                    return;
                }
                spawn(waterDust, scan.worldX(cell.cellX()),
                        scan.surfaceYAt(cell.cellX(), cell.cellZ()), scan.worldZ(cell.cellZ()));
                budget--;
            }
        }

        if (plan != null) {
            DustParticleOptions leakDust = new DustParticleOptions(LEAK_COLOR, 1.6F);
            for (WallPlan.Filler cell : plan.leakPatches()) {
                if (budget <= 0) {
                    return;
                }
                spawn(leakDust, scan.worldX(cell.cellX()),
                        scan.surfaceYAt(cell.cellX(), cell.cellZ()), scan.worldZ(cell.cellZ()));
                budget--;
            }
        }

        DustParticleOptions boundaryDust = new DustParticleOptions(BOUNDARY_COLOR, 1.5F);
        for (int cz = 0; cz < scan.sizeZ() && budget > 0; cz++) {
            for (int cx = 0; cx < scan.sizeX() && budget > 0; cx++) {
                if (!scan.isBoundary(cx, cz)) {
                    continue;
                }
                spawn(boundaryDust, scan.worldX(cx), scan.surfaceYAt(cx, cz), scan.worldZ(cz));
                budget--;
            }
        }
        if (plan != null) {
            DustParticleOptions greenDust = new DustParticleOptions(TOWER_GREEN_COLOR, 1.2F);
            for (WallPlan.Tower spot : plan.greenCandidates()) {
                if (budget <= 0) {
                    return;
                }
                spawnTower(greenDust, spot);
                budget--;
            }
            DustParticleOptions yellowDust = new DustParticleOptions(TOWER_YELLOW_COLOR, 1.0F);
            for (WallPlan.Tower spot : plan.yellowCandidates()) {
                if (budget <= 0) {
                    return;
                }
                spawnTower(yellowDust, spot);
                budget--;
            }
            DustParticleOptions chosenDust = new DustParticleOptions(TOWER_CHOSEN_COLOR, 2.2F);
            for (WallPlan.Tower spot : plan.towers()) {
                if (budget <= 0) {
                    return;
                }
                spawnTower(chosenDust, spot);
                budget--;
            }
        }

        if (houseSites != null) {
            DustParticleOptions houseDust = new DustParticleOptions(HOUSE_SITE_COLOR, 1.3F);
            int size = VillageCalcSettings.HOUSE_FOOTPRINT_SIZE;
            for (HouseSitePlan.HouseSite site : houseSites) {
                for (int dz = 0; dz < size && budget > 0; dz++) {
                    for (int dx = 0; dx < size && budget > 0; dx++) {
                        int cx = site.cellX() + dx;
                        int cz = site.cellZ() + dz;
                        // Только контур площадки, не сплошная заливка 7x7 - тот же смысл, что и у
                        // TERRITORY_COLOR/BOUNDARY_COLOR: проще увидеть форму и размер на глаз, и
                        // бюджет частиц расходуется куда экономнее на несколько площадок сразу.
                        boolean edge = dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1;
                        if (!edge) {
                            continue;
                        }
                        spawn(houseDust, scan.worldX(cx), scan.surfaceYAt(cx, cz), scan.worldZ(cz));
                        budget--;
                    }
                }
            }
        }

        int cells = countTerritory();
        if (cells == 0 || budget <= 0) {
            return;
        }
        int stride = Math.max(1, (int) Math.ceil(Math.sqrt((double) cells / budget)));
        DustParticleOptions territoryDust = new DustParticleOptions(TERRITORY_COLOR, 0.8F);
        for (int cz = 0; cz < scan.sizeZ() && budget > 0; cz += stride) {
            for (int cx = 0; cx < scan.sizeX() && budget > 0; cx += stride) {
                if (!scan.isTerritory(cx, cz) || scan.isBoundary(cx, cz)) {
                    continue;
                }
                spawn(territoryDust, scan.worldX(cx), scan.surfaceYAt(cx, cz), scan.worldZ(cz));
                budget--;
            }
        }
    }

    private static int countTerritory() {
        int count = 0;
        for (int cz = 0; cz < scan.sizeZ(); cz++) {
            for (int cx = 0; cx < scan.sizeX(); cx++) {
                if (scan.isTerritory(cx, cz)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void spawn(DustParticleOptions dust, int x, int y, int z) {
        level.sendParticles(dust, x + 0.5, y + PARTICLE_Y_OFFSET, z + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static void spawnTower(DustParticleOptions dust, WallPlan.Tower spot) {
        level.sendParticles(dust, scan.worldX(spot.cellX()) + 0.5,
                scan.surfaceYAt(spot.cellX(), spot.cellZ()) + TOWER_PARTICLE_Y_OFFSET,
                scan.worldZ(spot.cellZ()) + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
    }
}