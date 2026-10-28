package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Поворот дома лицом к дорожкам. Вход дома размечен в самом шаблоне: jigsaw "minecraft:building_entrance" смотрит в
 * сторону улицы (у warrior_house - на юг, там же кусок дорожки). Для каждого из четырёх поворотов считается клетка
 * перед входом, и выбирается тот, у которого она ближе всего к дорожке деревни; дом, у которого поблизости нет
 * дорожек, или шаблон без входа остаются без поворота.
 * <p>
 * Поворот на 180 градусов не меняет габарит, и место остаётся тем же. Повороты на 90 меняют длину и ширину местами:
 * для них место пересчитывается вокруг того же центра и проходит все те же проверки (подходящая ровная площадка,
 * откос, свободный мир), иначе такой поворот не рассматривается.
 */
public final class HouseOrientation {

    private HouseOrientation() {
    }

    /** Выбранный поворот и место (угол площадки в клетках скана) под повёрнутый габарит. */
    public record Oriented(Rotation rotation, HouseSitePlan.HouseSite site) {}

    /** Вход шаблона: локальная позиция jigsaw'а и сторона, в которую он смотрит без поворота. */
    private record Entrance(BlockPos local, Direction front) {}

    private static final String ENTRANCE_JIGSAW = "minecraft:building_entrance";

    /** Насколько клетка "перед входом" вынесена от двери. */
    private static final int FRONT_REACH = 3;
    /** Дальше этого от входа дорожки не ищутся (клеток). */
    private static final int PATH_SEARCH_RADIUS = 24;
    /** Поворот выбирается, только если выигрывает у текущего хотя бы столько клеток до дорожки. */
    private static final double MIN_GAIN = 1.5;
    private static final List<Rotation> OTHER_ROTATIONS =
            List.of(Rotation.CLOCKWISE_180, Rotation.CLOCKWISE_90, Rotation.COUNTERCLOCKWISE_90);

    /**
     * Лучший поворот для дома template на площадке site. quarterTurns = false - только 0 и 180 (место не меняется,
     * например когда стена уже перестроена под этот габарит); veryFlatOnly - для повёрнутого на 90 места нужна та же
     * очень ровная площадка, что и для первого.
     */
    public static Oriented choose(ServerLevel level, VillageScan scan, WallPlan plan, StructureTemplate template,
                                  HouseSitePlan.HouseSite site, boolean quarterTurns, boolean veryFlatOnly,
                                  @Nullable Rotation forced) {
        Oriented current = new Oriented(Rotation.NONE, site);
        if (forced != null) {
            return forcedOrientation(level, scan, plan, template, site, quarterTurns, veryFlatOnly, forced);
        }
        Entrance entrance = entranceOf(template);
        if (entrance == null) {
            return current;
        }
        double bestDistance = distanceToPath(scan, template, entrance, current);
        StringBuilder trace = new StringBuilder("0:").append(Math.round(bestDistance * 10) / 10.0);
        if (bestDistance == Double.MAX_VALUE) {
            NailerQueueAudit.log("поворот дома у {},{}: дорожек у входа в {} клетках нет - без поворота", scan.worldX(site.cellX()), scan.worldZ(site.cellZ()), PATH_SEARCH_RADIUS);
            return current;
        }
        for (Rotation rotation : OTHER_ROTATIONS) {
            boolean quarter = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
            if (quarter && !quarterTurns) {
                continue;
            }
            HouseSitePlan.HouseSite rotatedSite = quarter ? refit(level, scan, plan, template, site, veryFlatOnly) : site;
            if (rotatedSite == null) {
                continue;
            }
            Oriented candidate = new Oriented(rotation, rotatedSite);
            double distance = distanceToPath(scan, template, entrance, candidate);
            trace.append(" ").append(rotation).append(':').append(Math.round(distance * 10) / 10.0);
            if (distance + MIN_GAIN < bestDistance) {
                current = candidate;
                bestDistance = distance;
            }
        }
        NailerQueueAudit.log("поворот дома у {},{}: расстояние от клетки перед входом до дорожки по поворотам [{}] -> {}",
                scan.worldX(site.cellX()), scan.worldZ(site.cellZ()), trace, current.rotation());
        return current;
    }

    /** Поворот, заданный вручную: если для него нет места (или он невозможен при расширенной стене) - без поворота. */
    private static Oriented forcedOrientation(ServerLevel level, VillageScan scan, WallPlan plan, StructureTemplate template,
                                              HouseSitePlan.HouseSite site, boolean quarterTurns, boolean veryFlatOnly, Rotation forced) {
        boolean quarter = forced == Rotation.CLOCKWISE_90 || forced == Rotation.COUNTERCLOCKWISE_90;
        if (!quarter) {
            return new Oriented(forced, site);
        }
        HouseSitePlan.HouseSite rotatedSite = quarterTurns ? refit(level, scan, plan, template, site, veryFlatOnly) : null;
        return rotatedSite == null ? new Oriented(Rotation.NONE, site) : new Oriented(forced, rotatedSite);
    }

    /** Начало координат шаблона в мире так, чтобы угол повёрнутого ящика лёг на угол площадки (мировые corner X/Z, Y - groundY). */
    public static BlockPos originFor(StructureTemplate template, Rotation rotation, BlockPos corner) {
        BoundingBox probe = template.getBoundingBox(new StructurePlaceSettings().setRotation(rotation), BlockPos.ZERO);
        return new BlockPos(corner.getX() - probe.minX(), corner.getY(), corner.getZ() - probe.minZ());
    }

    @Nullable
    private static Entrance entranceOf(StructureTemplate template) {
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW)) {
            CompoundTag nbt = info.nbt();
            if (nbt != null && ENTRANCE_JIGSAW.equals(nbt.getString("name"))) {
                return new Entrance(info.pos(), JigsawBlock.getFrontFacing(info.state()));
            }
        }
        return null;
    }

    /** Место для дома, повёрнутого на 90: тот же центр, длина и ширина местами. null - там дому не стоять. */
    @Nullable
    private static HouseSitePlan.HouseSite refit(ServerLevel level, VillageScan scan, WallPlan plan, StructureTemplate template,
                                                 HouseSitePlan.HouseSite site, boolean veryFlatOnly) {
        Vec3i size = template.getSize();
        int width = size.getZ();
        int depth = size.getX();
        int cellX = site.cellX() + (size.getX() - width) / 2;
        int cellZ = site.cellZ() + (size.getZ() - depth) / 2;
        for (HouseSitePlan.HouseSite candidate : HouseSitePlan.findCandidates(scan, plan, width, depth)) {
            if (candidate.cellX() != cellX || candidate.cellZ() != cellZ) {
                continue;
            }
            return checkRefit(level, scan, plan, candidate, width, size.getY(), depth, veryFlatOnly);
        }
        NailerQueueAudit.log("поворот дома на 90 у {},{}: на повёрнутом габарите {}x{} место по правилам площадки не подходит",
                scan.worldX(cellX), scan.worldZ(cellZ), width, depth);
        return null;
    }

    @Nullable
    private static HouseSitePlan.HouseSite checkRefit(ServerLevel level, VillageScan scan, WallPlan plan, HouseSitePlan.HouseSite candidate,
                                                      int width, int height, int depth, boolean veryFlatOnly) {
        String problem = null;
        if (veryFlatOnly && !candidate.isVeryFlat()) {
            problem = "неровно";
        } else if (!HouseEarthworks.slopeFits(scan, plan, HouseSitePlan.houseBox(scan, candidate, width, height, depth))) {
            problem = "откос не получается";
        } else if (!HouseSitePlan.isAreaFree(level, scan, candidate, width, height, depth)) {
            problem = "занято в мире";
        }
        if (problem != null) {
            NailerQueueAudit.log("поворот дома на 90 у {},{}: {}", scan.worldX(candidate.cellX()), scan.worldZ(candidate.cellZ()), problem);
            return null;
        }
        return candidate;
    }

    /** Расстояние от клетки перед входом до ближайшей дорожки; Double.MAX_VALUE - дорожек поблизости нет. */
    private static double distanceToPath(VillageScan scan, StructureTemplate template, Entrance entrance, Oriented oriented) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(oriented.rotation());
        BlockPos corner = new BlockPos(scan.worldX(oriented.site().cellX()), oriented.site().groundY(),
                scan.worldZ(oriented.site().cellZ()));
        BlockPos door = originFor(template, oriented.rotation(), corner)
                .offset(StructureTemplate.calculateRelativePosition(settings, entrance.local()));
        Direction front = oriented.rotation().rotate(entrance.front());
        int frontX = door.getX() + front.getStepX() * FRONT_REACH - scan.minX();
        int frontZ = door.getZ() + front.getStepZ() * FRONT_REACH - scan.minZ();

        double best = Double.MAX_VALUE;
        for (int cz = frontZ - PATH_SEARCH_RADIUS; cz <= frontZ + PATH_SEARCH_RADIUS; cz++) {
            for (int cx = frontX - PATH_SEARCH_RADIUS; cx <= frontX + PATH_SEARCH_RADIUS; cx++) {
                if (scan.isPathBlock(cx, cz)) {
                    best = Math.min(best, Math.hypot(cx - frontX, cz - frontZ));
                }
            }
        }
        return best;
    }
}
