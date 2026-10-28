package net.gera.veridia.common.village.calc;

import net.gera.veridia.common.aibuff.villager.nailer.NailerUpgrades;
import net.gera.veridia.common.aibuff.villager.nailer.NailerUpgrades.MaterialKind;
import net.gera.veridia.common.aibuff.villager.nailer.NailerVillagerConfig;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.wood.ScrapWoodBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

public final class NailerWallQueue {

    private NailerWallQueue() {
    }

    public record Entry(BlockPos pos, BlockState state, int pointCost, boolean support) {}

    public record Piece(BlockPos anchor, List<Entry> entries) {}

    private static final Block[] KNOWN_BLOCKS = {
            ModBlocks.OAK_SCRAP_WOOD.get(),
            ModBlocks.SPRUCE_SCRAP_WOOD.get(),
            Blocks.OAK_PLANKS,
            Blocks.OAK_SLAB,
            Blocks.OAK_STAIRS,
            Blocks.OAK_FENCE,
            Blocks.OAK_FENCE_GATE,
            Blocks.OAK_DOOR,
            Blocks.STRIPPED_OAK_LOG,
            Blocks.PACKED_MUD,
            Blocks.MUD,
            Blocks.MUD_BRICKS,
            // Снежная деревня: снег и лёд вместо грязи (VillageStyle.MudSubstitutes)
            Blocks.SNOW_BLOCK,
            Blocks.PACKED_ICE,
            Blocks.BLUE_ICE,
            Blocks.POLISHED_DIORITE,
            ModBlocks.SNOW_BRICKS.get(),
            ModBlocks.PACKED_ICE_BRICKS.get(),
            FillerDecoration.ROPE_FENCE,
            // Башня (tower_1.nbt, вторая технология - tech_2) добавляет ещё 2 новых блока сверху.
            Blocks.OAK_LOG,
            Blocks.LADDER,
            Blocks.SPRUCE_PLANKS,
            Blocks.SPRUCE_SLAB,
            Blocks.SPRUCE_STAIRS,
            Blocks.SPRUCE_FENCE,
            Blocks.SPRUCE_FENCE_GATE,
            Blocks.SPRUCE_DOOR,
            Blocks.STRIPPED_SPRUCE_LOG,
            Blocks.SPRUCE_LOG,
    };

    private static final int MUD_DISCOUNT_FROM_PACKED_MUD = 2;

    public static int pointCost(BlockState state, Villager villager) {
        Block block = state.getBlock();
        if (block instanceof ScrapWoodBlock || block == FillerDecoration.ROPE_FENCE) return NailerUpgrades.pointCost(villager, MaterialKind.SCRAP_WOOD);
        // Снег и лёд стоят как грязь, которую заменяют: снежный блок - как плотная грязь, плотный лёд - как рыхлая,
        // оба вида кирпичей, синий лёд и полированный диорит (они есть в собранных вручную снежных шаблонах) - как
        // кирпичи из грязи.
        if (block == Blocks.PACKED_MUD || block == Blocks.SNOW_BLOCK) return NailerUpgrades.pointCost(villager, MaterialKind.PACKED_MUD);
        if (block == Blocks.MUD || block == Blocks.PACKED_ICE) {
            return NailerUpgrades.pointCost(villager, MaterialKind.PACKED_MUD) - MUD_DISCOUNT_FROM_PACKED_MUD;
        }
        if (block == Blocks.MUD_BRICKS || block == Blocks.BLUE_ICE || block == Blocks.POLISHED_DIORITE
                || block == ModBlocks.SNOW_BRICKS.get() || block == ModBlocks.PACKED_ICE_BRICKS.get()) {
            return NailerUpgrades.pointCost(villager, MaterialKind.PACKED_MUD_BRICKS);
        }
        if (state.is(BlockTags.LOGS)) return NailerUpgrades.pointCost(villager, MaterialKind.LOG);
        if (state.is(BlockTags.WOODEN_DOORS)) {
            return state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    ? NailerUpgrades.pointCost(villager, MaterialKind.DOOR) : 0;
        }
        if (state.is(BlockTags.WOODEN_STAIRS)) return NailerUpgrades.pointCost(villager, MaterialKind.STAIRS);
        if (state.is(BlockTags.WOODEN_SLABS)) return NailerUpgrades.pointCost(villager, MaterialKind.SLAB);
        if (state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.FENCE_GATES)) return NailerUpgrades.pointCost(villager, MaterialKind.FENCE_GATE);
        return NailerUpgrades.pointCost(villager, MaterialKind.PLANKS);
    }

    private record WeightedPiece(StructureTemplate template, int weight) {}

    public static List<Piece> build(ServerLevel level, VillageAutoPregen.VillagePlan villagePlan, Villager villager) {
        return build(level, villagePlan, villager, QueueTrace.NONE);
    }

    /** То же, но с журналом решений (что отброшено и почему) - см. QueueTrace/NailerQueueAudit. */
    public static List<Piece> build(ServerLevel level, VillageAutoPregen.VillagePlan villagePlan, Villager villager,
                                    QueueTrace trace) {
        VillageScan scan = villagePlan.scan();
        WallPlan plan = villagePlan.plan();
        VillageStyle style = villagePlan.style();

        boolean upgraded = NailerUpgrades.isTechUnlocked(villager);
        StructureTemplate wall = template(level, style.wall(variant(upgraded, VillageCalcSettings.WALL_PIECE, VillageCalcSettings.WALL_PIECE_UPGRADED)));
        StructureTemplate entrance = template(level, style.wall(variant(upgraded, VillageCalcSettings.ENTRANCE_PIECE, VillageCalcSettings.ENTRANCE_PIECE_UPGRADED)));
        if (wall == null || entrance == null) {
            return List.of();
        }
        List<WeightedPiece> wallVariants = new ArrayList<>();
        wallVariants.add(new WeightedPiece(wall, VillageCalcSettings.WALL_VARIANT_WEIGHT_1));
        addIfPresent(wallVariants, template(level, style.wall(variant(upgraded, VillageCalcSettings.WALL_PIECE_2, VillageCalcSettings.WALL_PIECE_2_UPGRADED))), VillageCalcSettings.WALL_VARIANT_WEIGHT_2);
        addIfPresent(wallVariants, template(level, style.wall(variant(upgraded, VillageCalcSettings.WALL_PIECE_3, VillageCalcSettings.WALL_PIECE_3_UPGRADED))), VillageCalcSettings.WALL_VARIANT_WEIGHT_3);

        List<StructureTemplate> windowVariants = new ArrayList<>();
        addIfPresent(windowVariants, template(level, style.wall(variant(upgraded, VillageCalcSettings.WALL_WINDOW_1, VillageCalcSettings.WALL_WINDOW_1_UPGRADED))));
        addIfPresent(windowVariants, template(level, style.wall(variant(upgraded, VillageCalcSettings.WALL_WINDOW_2, VillageCalcSettings.WALL_WINDOW_2_UPGRADED))));
        addIfPresent(windowVariants, template(level, style.wall(variant(upgraded, VillageCalcSettings.WALL_WINDOW_3, VillageCalcSettings.WALL_WINDOW_3_UPGRADED))));

        // Какие башни реально встанут - решается ДО стены: план уже вырезал под каждую башню кусок
        // стены и поставил до её граней столбики-стыки (WallPlan.cutWallForTowers). Для башни, которую
        // этот nailer строить не будет (tech_2 не открыт или место занято), вырез откатывается
        // (segmentsFor/fillersFor) - иначе на её месте осталась бы дыра на всё тело.
        StructureTemplate towerTemplate = NailerUpgrades.isTech2Unlocked(villager)
                ? template(level, style.tower()) : null;
        List<WallPlan.Tower> builtTowers = new ArrayList<>();
        if (towerTemplate != null) {
            for (WallPlan.Tower spot : plan.towers()) {
                QueueTrace.Obstacle obstacle = towerObstacle(level, scan, towerTemplate, spot);
                trace.tower(spot, obstacle);
                if (obstacle == null) {
                    builtTowers.add(spot);
                }
            }
        }

        List<WallPlan.Segment> ready = new ArrayList<>();
        List<WallPlan.Filler> columns = new ArrayList<>();
        Set<Long> columnSeen = new HashSet<>();
        Map<WallPlan.Segment, StructureTemplate> chosenPiece = new HashMap<>();

        for (WallPlan.Segment segment : plan.segmentsFor(builtTowers)) {
            StructureTemplate piece = segment.entrance() ? entrance
                    : pickPieceFor(segment, wallVariants, windowVariants);
            boolean uneven = segment.tooUneven(scan);
            QueueTrace.Obstacle obstacle = uneven ? null : segmentObstacle(level, scan, piece, segment);
            if (!uneven && obstacle == null) {
                trace.segment(segment, QueueTrace.SegmentVerdict.WHOLE, null);
                ready.add(segment);
                chosenPiece.put(segment, piece);
                continue;
            }
            trace.segment(segment, uneven ? QueueTrace.SegmentVerdict.TOO_UNEVEN : QueueTrace.SegmentVerdict.BLOCKED, obstacle);
            for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
                addColumn(level, scan, columns, columnSeen,
                        segment.cellX() + segment.along().getStepX() * i,
                        segment.cellZ() + segment.along().getStepZ() * i, trace);
            }
        }
        for (WallPlan.Filler filler : plan.fillersFor(builtTowers)) {
            addColumn(level, scan, columns, columnSeen, filler.cellX(), filler.cellZ(), trace);
        }
        if (NailerVillagerConfig.SECOND_DROP_STRAYS) {
            dropStraysAfterFit(level, scan, ready, columns, builtTowers, trace);
        }

        // Группа - то, что ставится вместе: один кусок стены, один столбик, одна клетка водной решётки, одна
        // башня. Группы идут в порядке плана; от порядка групп зависит, в какой последовательности nailer
        // обходит стену (см. orderForBuilding).
        List<List<Entry>> wallGroups = new ArrayList<>();
        for (WallPlan.Segment segment : ready) {
            wallGroups.add(decomposePiece(scan, chosenPiece.get(segment), segment, villager, style));
        }
        // Башни (tech_2, см. NailerUpgrades.isTech2Unlocked) - ВСЕГДА добавляются В КОНЕЦ allEntries.
        // groupIntoGrowableComponents ниже обнаруживает компоненты по возрастанию индекса в
        // allEntries - башня физически не связана со стеной, так что её Piece гарантированно
        // окажется в конце итогового списка pieces, а claimNextPiece (сканирует от индекса 0)
        // естественным образом берётся за неё, только когда всё остальное уже готово/занято.
        List<List<Entry>> towerGroups = new ArrayList<>();
        for (WallPlan.Tower spot : builtTowers) {
            towerGroups.add(decomposeTower(scan, towerTemplate, spot, villager));
        }

        List<List<Entry>> groups = new ArrayList<>(wallGroups);
        groups.addAll(fillerColumnsClearOf(positionsOf(flat(wallGroups), flat(towerGroups)), columns, scan, villager, upgraded, style, trace));
        if (VillageCalcSettings.SEAL_WATER_GAPS) {
            for (WallPlan.WaterCell cell : plan.waterCells()) {
                groups.add(decomposeWaterCell(scan, cell, villager));
            }
        }
        groups.addAll(towerGroups);

        Map<Entry, Integer> groupOf = new IdentityHashMap<>();
        List<Entry> allEntries = flatNumbered(groups, groupOf);
        List<Piece> pieces = new ArrayList<>(groupIntoGrowableComponents(onePerPosition(allEntries, trace), groupOf, trace));
        // Дома (tech_3, см. NailerUpgrades.isTech3Unlocked) - ВСЕГДА в конце, после стены и башен
        // (claimNextPiece сканирует с начала и возьмётся за дом, только когда всё остальное уже
        // готово/занято). Не через groupIntoGrowableComponents: тот упорядочил бы всё снизу вверх и
        // склеил бы земляные работы с самим домом - копка шла бы вперемешку со стройкой, а срезка
        // холма снизу вверх оставляла бы висящую землю. У дома свой порядок, см. currentHousePieces.
        // Сам nailer дома не строит (AUTONOMOUS_HOUSE_BUILDING выключен) - ими будет управлять вкладка BUILD окна жителя.
        if (NailerVillagerConfig.AUTONOMOUS_HOUSE_BUILDING && NailerUpgrades.isTech3Unlocked(villager)) {
            pieces.addAll(currentHousePieces(level, scan, plan, villager, style));
        }
        return pieces;
    }

    /**
     * Столбики-заглушки, кроме тех, что задевают уже спланированный кусок стены или башню - такой
     * столбик пропускается ЦЕЛИКОМ, как в /calculate (VillageCalc.fillerFits: клетка не свободна -
     * столбика нет). Типичный случай - угол стены: клетка угла входит в два ряда стены, в одном кусок
     * встал, а у другого не влез и развалился на столбики - и столбик хотел встать ровно туда, где уже
     * доски куска.
     */
    private static List<List<Entry>> fillerColumnsClearOf(Set<BlockPos> taken, List<WallPlan.Filler> columns,
                                                          VillageScan scan, Villager villager, boolean upgraded,
                                                          VillageStyle style, QueueTrace trace) {
        List<List<Entry>> kept = new ArrayList<>();
        for (WallPlan.Filler filler : columns) {
            List<Entry> column = decomposeFiller(scan, filler, villager, upgraded, style);
            Entry overlap = column.stream().filter(entry -> taken.contains(entry.pos())).findFirst().orElse(null);
            if (overlap == null) {
                kept.add(column);
            } else {
                trace.columnOverlapsPiece(filler, overlap.pos());
            }
        }
        return kept;
    }

    @SafeVarargs
    private static Set<BlockPos> positionsOf(List<Entry>... lists) {
        Set<BlockPos> positions = new HashSet<>();
        for (List<Entry> list : lists) {
            for (Entry entry : list) {
                positions.add(entry.pos());
            }
        }
        return positions;
    }

    private static List<Entry> flat(List<List<Entry>> groups) {
        List<Entry> entries = new ArrayList<>();
        groups.forEach(entries::addAll);
        return entries;
    }

    /** Склеивает группы в один список и запоминает номер группы каждой записи (по ссылке: запись - record,
     * две равные по значению записи не должны делить номер). */
    private static List<Entry> flatNumbered(List<List<Entry>> groups, Map<Entry, Integer> groupOf) {
        List<Entry> entries = new ArrayList<>();
        for (int group = 0; group < groups.size(); group++) {
            for (Entry entry : groups.get(group)) {
                groupOf.put(entry, group);
                entries.add(entry);
            }
        }
        return entries;
    }

    /**
     * Второй проход WallPlan.dropStrays - ровно как в /calculate (VillageCalc.buildWall): после того как живой
     * мир забраковал часть кусков и столбиков, в стене могли остаться обрывки и щели. dropStrays убирает мелкие
     * обрывки и достраивает мосты (остовное дерево связности) между цепочками. Первый проход делает сам план
     * (WallPlan.buildPass) - до проверки живого мира; без второго nailer оставлял щели, которые /calculate
     * закрывал. Мосты проверяются тем же fillerObstacle, что обычные столбики (в /calculate их проверяет
     * placeFiller).
     */
    private static void dropStraysAfterFit(ServerLevel level, VillageScan scan, List<WallPlan.Segment> ready,
                                           List<WallPlan.Filler> columns, List<WallPlan.Tower> builtTowers,
                                           QueueTrace trace) {
        Set<WallPlan.Filler> before = new HashSet<>(columns);
        int removed = WallPlan.dropStrays(scan, ready, columns, new ArrayList<>(builtTowers));
        List<WallPlan.Filler> bridges = columns.stream().filter(filler -> !before.contains(filler)).toList();
        List<WallPlan.Filler> unusable = bridges.stream()
                .filter(filler -> fillerObstacle(level, scan, filler.cellX(), filler.cellZ()) != null)
                .toList();
        columns.removeAll(unusable);
        trace.strays(removed, bridges.size(), unusable.size());
    }

    /**
     * Одна клетка - одна запись. Если на одну позицию претендуют две записи с разными блоками (угол
     * стены, где сходятся куски двух рядов; подпорка одного куска под блоком соседнего), nailer
     * зацикливался: ставил доски по одной записи, потом scrap_wood по другой, первая снова "не
     * совпадала" - и так без конца (скриншоты игрока). groupIntoGrowableComponents от этого не спасал:
     * лишняя запись обычно отпадала одиночным куском, но если она оказывалась первой в своём куске
     * стены - в кусок попадали обе.
     * <p>
     * Кто остаётся - как в /calculate, где такие клетки просто перезаписываются по порядку постройки:
     * запись позже по списку побеждает, но подпорка никогда не перебивает блок самой стены/башни.
     */
    private static List<Entry> onePerPosition(List<Entry> entries, QueueTrace trace) {
        Map<BlockPos, Entry> winner = new HashMap<>();
        for (Entry entry : entries) {
            Entry current = winner.get(entry.pos());
            boolean supportUnderRealBlock = entry.support() && current != null && !current.support();
            if (!supportUnderRealBlock) {
                winner.put(entry.pos(), entry);
            }
        }
        List<Entry> result = new ArrayList<>(winner.size());
        for (Entry entry : entries) {
            Entry chosen = winner.get(entry.pos());
            if (chosen == entry) {
                result.add(entry);
            } else if (!statesMatch(entry.state(), chosen.state())) {
                // Запись с тем же блоком на той же клетке - не потеря, в журнал идут только настоящие споры.
                trace.shadowed(entry, chosen);
            }
        }
        return result;
    }

    private static String variant(boolean upgraded, String tier1, String tier2) {
        return upgraded ? tier2 : tier1;
    }

    private static void addIfPresent(List<WeightedPiece> variants, @Nullable StructureTemplate piece, int weight) {
        if (piece != null) variants.add(new WeightedPiece(piece, weight));
    }

    private static void addIfPresent(List<StructureTemplate> variants, @Nullable StructureTemplate piece) {
        if (piece != null) variants.add(piece);
    }

    private static StructureTemplate pickPieceFor(WallPlan.Segment segment,
                                                    List<WeightedPiece> wallVariants, List<StructureTemplate> windowVariants) {
        long seed = (((long) segment.cellX()) << 32) ^ (segment.cellZ() & 0xffffffffL);
        java.util.Random rng = new java.util.Random(seed);

        if (!windowVariants.isEmpty() && rng.nextFloat() < VillageCalcSettings.WALL_WINDOW_CHANCE) {
            return windowVariants.get(rng.nextInt(windowVariants.size()));
        }
        int total = 0;
        for (WeightedPiece variant : wallVariants) total += variant.weight();
        int roll = rng.nextInt(total);
        for (WeightedPiece variant : wallVariants) {
            roll -= variant.weight();
            if (roll < 0) return variant.template();
        }
        return wallVariants.get(0).template();
    }


    private static final int[][] DIAGONAL_XZ_OFFSETS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private static List<Piece> groupIntoGrowableComponents(List<Entry> allEntries, Map<Entry, Integer> groupOf,
                                                           QueueTrace trace) {
        Map<BlockPos, Integer> indexOf = new HashMap<>();
        for (int i = 0; i < allEntries.size(); i++) {
            indexOf.put(allEntries.get(i).pos(), i);
        }

        boolean[] visited = new boolean[allEntries.size()];
        List<Piece> pieces = new ArrayList<>();

        for (int start = 0; start < allEntries.size(); start++) {
            if (visited[start]) continue;

            List<Entry> component = new ArrayList<>();
            PriorityQueue<Integer> frontier = new PriorityQueue<>(Comparator
                    .comparingInt((Integer idx) -> allEntries.get(idx).pos().getY())
                    .thenComparingInt(idx -> idx));
            visited[start] = true;
            frontier.add(start);

            while (!frontier.isEmpty()) {
                int idx = frontier.poll();
                Entry entry = allEntries.get(idx);
                component.add(entry);
                for (Direction dir : Direction.values()) {
                    Integer neighborIdx = indexOf.get(entry.pos().relative(dir));
                    if (neighborIdx != null && !visited[neighborIdx]) {
                        visited[neighborIdx] = true;
                        frontier.add(neighborIdx);
                    }
                }
                for (int[] offset : DIAGONAL_XZ_OFFSETS) {
                    Integer neighborIdx = indexOf.get(entry.pos().offset(offset[0], 0, offset[1]));
                    if (neighborIdx != null && !visited[neighborIdx]) {
                        visited[neighborIdx] = true;
                        frontier.add(neighborIdx);
                    }
                }
            }

            boolean kept = component.size() >= NailerVillagerConfig.MIN_COMPONENT_SIZE;
            if (kept) {
                List<Entry> ordered = orderForBuilding(component, groupOf);
                trace.component(ordered, true);
                pieces.add(new Piece(component.get(0).pos(), ordered));
            } else {
                trace.component(component, false);
            }
        }
        return pieces;
    }

    // --- порядок стройки внутри одной связной массы (Piece) ---

    /**
     * Компонент собирается заливкой по высоте (PriorityQueue по Y выше), и если оставить записи в порядке
     * заливки, nailer строит ВСЮ стену слоями: сначала нижний ряд по всему периметру, потом второй и так
     * далее. На рельефе это выглядит как дыры и недострой: низины уже под крышу, а на холме стоит один нижний
     * ряд (в мире игрока 55 из 78 недостроенных колонок обрывались ровно на одной высоте y=72), и nailer
     * обегает весь периметр на каждый слой. Поэтому по умолчанию (WALL_BUILD_ORDER = PIECE_BY_PIECE) куски
     * ставятся по одному целиком: снизу вверх внутри куска, и дальше к ближайшему ещё не тронутому куску.
     */
    private static List<Entry> orderForBuilding(List<Entry> component, Map<Entry, Integer> groupOf) {
        return switch (NailerVillagerConfig.WALL_BUILD_ORDER) {
            case LAYERED -> component;
            case PIECE_BY_PIECE -> pieceByPiece(component, groupOf);
        };
    }

    /** Группа записей (кусок стены / столбик / башня) целиком, center - середина её габарита. */
    private record BuildGroup(int id, List<Entry> entries, BlockPos center) {}

    private static List<Entry> pieceByPiece(List<Entry> component, Map<Entry, Integer> groupOf) {
        Map<Integer, List<Entry>> byGroup = new LinkedHashMap<>();
        for (Entry entry : component) {
            byGroup.computeIfAbsent(groupOf.get(entry), id -> new ArrayList<>()).add(entry);
        }
        List<BuildGroup> remaining = new ArrayList<>();
        byGroup.forEach((id, entries) -> remaining.add(buildGroupOf(id, entries)));

        List<Entry> ordered = new ArrayList<>(component.size());
        BuildGroup current = takeById(remaining, groupOf.get(component.get(0)));
        while (current != null) {
            ordered.addAll(current.entries());
            current = takeNearest(remaining, current.center());
        }
        return ordered;
    }

    private static BuildGroup buildGroupOf(int id, List<Entry> entries) {
        List<Entry> bottomUp = new ArrayList<>(entries);
        bottomUp.sort(Comparator.comparingInt(entry -> entry.pos().getY()));
        BoundingBox box = BoundingBox.encapsulatingPositions(bottomUp.stream().map(Entry::pos).toList()).orElseThrow();
        return new BuildGroup(id, bottomUp, box.getCenter());
    }

    @Nullable
    private static BuildGroup takeById(List<BuildGroup> groups, int id) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).id() == id) {
                return groups.remove(i);
            }
        }
        return null;
    }

    @Nullable
    private static BuildGroup takeNearest(List<BuildGroup> groups, BlockPos from) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < groups.size(); i++) {
            double distance = groups.get(i).center().distSqr(from);
            if (distance < bestDistance) {
                best = i;
                bestDistance = distance;
            }
        }
        return best < 0 ? null : groups.remove(best);
    }

    private static void addColumn(ServerLevel level, VillageScan scan, List<WallPlan.Filler> columns,
                                   Set<Long> seen, int cx, int cz, QueueTrace trace) {
        QueueTrace.Obstacle obstacle = fillerObstacle(level, scan, cx, cz);
        if (obstacle != null) {
            trace.column(cx, cz, QueueTrace.ColumnVerdict.REJECTED, obstacle);
            return;
        }
        long key = (((long) cx) << 32) ^ (cz & 0xffffffffL);
        if (seen.add(key)) {
            columns.add(new WallPlan.Filler(cx, cz));
            trace.column(cx, cz, QueueTrace.ColumnVerdict.ADDED, null);
        } else {
            trace.column(cx, cz, QueueTrace.ColumnVerdict.DUPLICATE, null);
        }
    }

    private static List<Entry> decomposePiece(VillageScan scan, StructureTemplate piece, WallPlan.Segment segment,
                                                Villager villager, VillageStyle style) {
        StructurePlaceSettings settings = settingsFor(piece, segment);
        BlockPos origin = originFor(scan, piece, segment, settings);

        List<StructureTemplate.StructureBlockInfo> infos = new ArrayList<>();
        for (Block block : KNOWN_BLOCKS) {
            infos.addAll(piece.filterBlocks(origin, settings, block));
        }
        infos.sort(Comparator.comparingInt(info -> info.pos().getY()));

        List<Entry> entries = new ArrayList<>(infos.size());
        Map<Long, Integer> columnBottom = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo info : infos) {
            entries.add(new Entry(info.pos(), info.state(), pointCost(info.state(), villager), false));
            columnBottom.merge(columnKey(info.pos().getX(), info.pos().getZ()), info.pos().getY(), Math::min);
        }
        for (Map.Entry<Long, Integer> column : columnBottom.entrySet()) {
            int x = (int) (column.getKey() >> 32);
            int z = (int) (long) column.getKey();
            entries.addAll(supportColumnsFor(scan, x, column.getValue(), z, villager, style));
        }
        entries.addAll(iceFoundationFor(scan, entries, villager));
        return entries;
    }

    private static long columnKey(int worldX, int worldZ) {
        return (((long) worldX) << 32) | (worldZ & 0xffffffffL);
    }

    /** Лёд прямо под нижним блоком каждой колонки записей меняется на плотный (IceFoundation: обычный лёд тает от света). */
    private static List<Entry> iceFoundationFor(VillageScan scan, List<Entry> entries, Villager villager) {
        Map<Long, Integer> lowestByColumn = new HashMap<>();
        for (Entry entry : entries) {
            lowestByColumn.merge(columnKey(entry.pos().getX(), entry.pos().getZ()), entry.pos().getY(), Math::min);
        }
        BlockState foundation = IceFoundation.state();
        List<Entry> ice = new ArrayList<>();
        for (Map.Entry<Long, Integer> column : lowestByColumn.entrySet()) {
            int x = (int) (column.getKey() >> 32);
            int z = (int) (long) column.getKey();
            BlockPos under = IceFoundation.iceUnder(scan, x, column.getValue(), z);
            if (under != null) {
                ice.add(new Entry(under, foundation, pointCost(foundation, villager), true));
            }
        }
        return ice;
    }

    private static List<Entry> supportColumnsFor(VillageScan scan, int worldX, int bottomY, int worldZ, Villager villager,
                                                   VillageStyle style) {
        int cellX = worldX - scan.minX();
        int cellZ = worldZ - scan.minZ();
        if (!scan.inBounds(cellX, cellZ)) {
            return List.of();
        }
        int groundTop = scan.surfaceYAt(cellX, cellZ);
        int gap = (bottomY - 1) - groundTop;
        if (gap <= 0) {
            return List.of();
        }
        int depth = Math.min(gap, VillageCalcSettings.SUPPORT_MAX_DEPTH);
        List<Entry> entries = new ArrayList<>(depth);
        for (int d = 0; d < depth; d++) {
            int y = bottomY - 1 - d;
            BlockState state = pickSupportBlock(style, worldX, y, worldZ);
            entries.add(new Entry(new BlockPos(worldX, y, worldZ), state, pointCost(state, villager), true));
        }
        return entries;
    }

    private static BlockState pickSupportBlock(VillageStyle style, int x, int y, int z) {
        long seed = (((long) x) * 341873128712L) ^ (((long) y) * 132897987541L) ^ (long) z;
        java.util.Random rng = new java.util.Random(seed);
        float roll = rng.nextFloat();
        if (roll < VillageCalcSettings.SUPPORT_TERTIARY_CHANCE) {
            return style.packedMud().defaultBlockState();
        }
        if (roll < VillageCalcSettings.SUPPORT_TERTIARY_CHANCE + VillageCalcSettings.SUPPORT_SECONDARY_CHANCE) {
            return style.planks().defaultBlockState();
        }
        return style.scrapWood().defaultBlockState();
    }

    /** Подбор материала по слоям высоты (низ тяжелее/плотнее, верх легче) + окошко(tier1)/редкий
     * бревенчатый столб(tier2) - вынесено в FillerDecoration, см. её javadoc. */
    private static List<Entry> decomposeFiller(VillageScan scan, WallPlan.Filler filler, Villager villager, boolean upgraded,
                                                 VillageStyle style) {
        int worldX = scan.worldX(filler.cellX());
        int worldZ = scan.worldZ(filler.cellZ());
        int y = fillerBaseY(scan, filler.cellX(), filler.cellZ());
        List<Block> blocks = FillerDecoration.columnBlocks(filler.cellX(), filler.cellZ(), upgraded, style);

        List<Entry> entries = new ArrayList<>(blocks.size());
        for (int dy = 0; dy < blocks.size(); dy++) {
            BlockState state = blocks.get(dy).defaultBlockState();
            entries.add(new Entry(new BlockPos(worldX, y + dy, worldZ), state, pointCost(state, villager), false));
        }
        return entries;
    }

    private static List<Entry> decomposeWaterCell(VillageScan scan, WallPlan.WaterCell cell, Villager villager) {
        int cx = cell.cellX();
        int cz = cell.cellZ();
        int depth = scan.waterDepthAt(cx, cz);
        if (depth < VillageCalcSettings.WATER_MIN_DEPTH || depth > VillageCalcSettings.MAX_WATER_BARRIER_HEIGHT) {
            return List.of();
        }
        int height = depth - VillageCalcSettings.WATER_BARRIER_SUBMERGE;

        int worldX = scan.worldX(cx);
        int worldZ = scan.worldZ(cz);
        int base = scan.surfaceYAt(cx, cz) + 1;
        BlockState state = waterBarrierState();
        int cost = pointCost(state, villager);

        List<Entry> entries = new ArrayList<>(height);
        for (int dy = 0; dy < height; dy++) {
            entries.add(new Entry(new BlockPos(worldX, base + dy, worldZ), state, cost, false));
        }
        return entries;
    }

    private static BlockState waterBarrierState() {
        BlockState state = VillageCalcSettings.WATER_BARRIER_BLOCK.defaultBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true);
        }
        return state;
    }


    @Nullable
    private static StructureTemplate template(ServerLevel level, ResourceLocation id) {
        return level.getStructureManager().get(id).orElse(null);
    }

    private static StructurePlaceSettings settingsFor(StructureTemplate piece, WallPlan.Segment segment) {
        return new StructurePlaceSettings()
                .setRotation(WallPieceRotation.of(piece, segment.outward()))
                .setIgnoreEntities(true);
    }

    /** <b><i>Y</b></i> level*/
    private static int surfaceUnder(VillageScan scan, WallPlan.Segment segment) {
        if (segment.entrance()) {
            int half = VillageCalcSettings.PIECE_LENGTH / 2;
            int cx = segment.cellX() + segment.along().getStepX() * half;
            int cz = segment.cellZ() + segment.along().getStepZ() * half;
            Direction out = segment.outward();
            int center = scan.surfaceYAt(cx, cz);
            int before = scan.surfaceYAt(cx - out.getStepX(), cz - out.getStepZ());
            int after = scan.surfaceYAt(cx + out.getStepX(), cz + out.getStepZ());
            int guard = VillageCalcSettings.GATE_POTHOLE_GUARD;
            int low = Math.min(before, after) - guard;
            int high = Math.max(before, after) + guard;
            return Math.max(low, Math.min(high, center)) + VillageCalcSettings.GATE_HEIGHT_OFFSET;
        }

        int[] heights = new int[VillageCalcSettings.PIECE_LENGTH];
        for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
            int cx = segment.cellX() + segment.along().getStepX() * i;
            int cz = segment.cellZ() + segment.along().getStepZ() * i;
            heights[i] = scan.surfaceYAt(cx, cz);
        }
        int highest = heights[0];
        for (int height : heights) {
            highest = Math.max(highest, height);
        }
        int[] sorted = heights.clone();
        java.util.Arrays.sort(sorted);
        int median = sorted[sorted.length / 2];
        return Math.min(highest, median + VillageCalcSettings.SURFACE_OUTLIER_CAP)
                + VillageCalcSettings.WALL_HEIGHT_OFFSET;
    }

    private static int baseY(VillageScan scan, WallPlan.Segment segment) {
        return surfaceUnder(scan, segment) + 1 - VillageCalcSettings.WALL_EMBED_DEPTH;
    }

    private static BlockPos originFor(VillageScan scan, StructureTemplate piece, WallPlan.Segment segment,
                                       StructurePlaceSettings settings) {
        int y = baseY(scan, segment);
        BlockPos wanted = new BlockPos(scan.worldX(segment.cellX()), y, scan.worldZ(segment.cellZ()));
        BoundingBox probe = piece.getBoundingBox(settings, wanted);
        return wanted.offset(wanted.getX() - probe.minX(), 0, wanted.getZ() - probe.minZ());
    }

    private static final BlockState NO_BLOCK = Blocks.AIR.defaultBlockState();

    /** Что мешает поставить кусок целиком: вода/чужая постройка на земле под ним или в его габарите. null - ничто. */
    @Nullable
    private static QueueTrace.Obstacle segmentObstacle(ServerLevel level, VillageScan scan, StructureTemplate piece,
                                                       WallPlan.Segment segment) {
        QueueTrace.Obstacle ground = groundObstacle(level, scan, segment);
        if (ground != null) return ground;
        StructurePlaceSettings settings = settingsFor(piece, segment);
        return firstObstacle(level, piece.getBoundingBox(settings, originFor(scan, piece, segment, settings)));
    }

    @Nullable
    private static QueueTrace.Obstacle groundObstacle(ServerLevel level, VillageScan scan, WallPlan.Segment segment) {
        for (int i = 0; i < VillageCalcSettings.PIECE_LENGTH; i++) {
            int cx = segment.cellX() + segment.along().getStepX() * i;
            int cz = segment.cellZ() + segment.along().getStepZ() * i;
            if (!scan.inBounds(cx, cz)) continue;
            BlockPos surface = new BlockPos(scan.worldX(cx), scan.surfaceYAt(cx, cz), scan.worldZ(cz));
            if (scan.isWet(cx, cz)) {
                return new QueueTrace.Obstacle(surface, NO_BLOCK, "глубокая вода под куском");
            }
            QueueTrace.Obstacle blocker = obstacleAt(level, surface);
            if (blocker != null) {
                return new QueueTrace.Obstacle(blocker.pos(), blocker.state(), "земля под куском: " + blocker.why());
            }
        }
        return null;
    }

    /** Блок на месте будущего куска стены, который мешает его поставить: чужая постройка (не наши материалы -
     * KNOWN_BLOCKS) или бревно здания деревни; непрогруженный чанк тоже помеха (скан там вслепую). Брёвна здесь
     * особые: они есть и в KNOWN_BLOCKS (бревно башни), и по самому блоку выглядят деревом, поэтому крышу таёжного
     * дома различает только место (VillageBuildings). Деревья (в т.ч. Dynamic Trees) помехой не считаются - как и
     * в /calculate, только там их перед стройкой рубят, а настоящий nailer пока нет. */
    @Nullable
    private static QueueTrace.Obstacle obstacleAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return new QueueTrace.Obstacle(pos.immutable(), NO_BLOCK, "чанк не прогружен");
        }
        BlockState state = level.getBlockState(pos);
        if (VillageBuildings.isLogOfBuilding(level, pos, state)) {
            return new QueueTrace.Obstacle(pos.immutable(), state, "бревно здания деревни");
        }
        if (VillageBlocks.isStructure(state) && isForeign(state)) {
            return new QueueTrace.Obstacle(pos.immutable(), state, "чужая постройка");
        }
        return null;
    }

    /** Материал, которым nailer строит стену (без брёвен: они есть и у обычных деревьев) - для диагностики. */
    static boolean isWallMaterial(BlockState state) {
        return !isForeign(state) && !state.is(BlockTags.LOGS);
    }

    /** Блок не из материалов стены (KNOWN_BLOCKS): свои блоки стене не помеха - их можно перестроить поверх. */
    private static boolean isForeign(BlockState state) {
        Block block = state.getBlock();
        for (Block known : KNOWN_BLOCKS) {
            if (block == known) return false;
        }
        return true;
    }

    @Nullable
    private static QueueTrace.Obstacle firstObstacle(ServerLevel level, BoundingBox box) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    cursor.set(x, y, z);
                    QueueTrace.Obstacle blocker = obstacleAt(level, cursor);
                    if (blocker != null) return blocker;
                }
            }
        }
        return null;
    }

    // --- башни (tech_2) - позиции/углы уже посчитаны в WallPlan.findTowers, тут только разбор
    // на блоки, поворот и live-проверка препятствий, тем же паттерном, что и decomposePiece/fits. ---

    /** Башня как сохранена в NBT (см. tower_1.nbt) "смотрит" (выступ+лестница) на юг без поворота -
     * не переиспользуем rotationFor/WALL_ROTATION_FIX сегментов стены, у них своя, отдельная
     * поправка под ИХ файлы. outward у Tower - "наружу" (прочь от деревни), а лестница должна
     * смотреть ВНУТРЬ, то есть на outward.getOpposite() - маппинг SOUTH/NORTH и WEST/EAST
     * поменян местами (было SOUTH -> NONE, лестница смотрела наружу вместе со стеной). */
    private static Rotation towerRotationFor(Direction outward) {
        return switch (outward) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static StructurePlaceSettings towerSettingsFor(WallPlan.Tower spot) {
        return new StructurePlaceSettings()
                .setRotation(towerRotationFor(spot.outward()))
                .setIgnoreEntities(true);
    }

    /** См. VillageCalc.towerBaseY - WALL_EMBED_DEPTH тут не подходит (тот запас на закапывание
     * заложен в NBT кусков стены, у башни нижний ряд уже видимая часть). */
    private static int towerBaseY(VillageScan scan, WallPlan.Tower spot) {
        return scan.surfaceYAt(spot.cellX(), spot.cellZ()) + 1;
    }

    private static BlockPos towerOriginFor(VillageScan scan, StructureTemplate tower, WallPlan.Tower spot,
                                            StructurePlaceSettings settings) {
        int y = towerBaseY(scan, spot);
        int anchorX = scan.worldX(spot.cellX());
        int anchorZ = scan.worldZ(spot.cellZ());
        BlockPos probeOrigin = new BlockPos(anchorX, y, anchorZ);
        BoundingBox probe = tower.getBoundingBox(settings, probeOrigin);
        // Та же геометрия, что у плана (TowerFootprint) - единственный источник, см. её javadoc.
        Direction second = TowerFootprint.secondaryOutward(scan, spot);
        int minX = TowerFootprint.biasedAxisMin(anchorX, probe.getXSpan(), spot.outward(), second, Direction.Axis.X);
        int minZ = TowerFootprint.biasedAxisMin(anchorZ, probe.getZSpan(), spot.outward(), second, Direction.Axis.Z);
        return probeOrigin.offset(minX - probe.minX(), 0, minZ - probe.minZ());
    }

    /** Что мешает башне встать: null - ничто (та же схема, что у segmentObstacle). */
    @Nullable
    private static QueueTrace.Obstacle towerObstacle(ServerLevel level, VillageScan scan, StructureTemplate tower,
                                                     WallPlan.Tower spot) {
        BlockPos anchor = new BlockPos(scan.worldX(spot.cellX()),
                scan.surfaceYAt(spot.cellX(), spot.cellZ()), scan.worldZ(spot.cellZ()));
        if (!scan.inBounds(spot.cellX(), spot.cellZ())) {
            return new QueueTrace.Obstacle(anchor, NO_BLOCK, "якорь башни вне скана");
        }
        if (scan.isWet(spot.cellX(), spot.cellZ())) {
            return new QueueTrace.Obstacle(anchor, NO_BLOCK, "якорь башни в глубокой воде");
        }
        QueueTrace.Obstacle ground = obstacleAt(level, anchor);
        if (ground != null) return ground;
        StructurePlaceSettings settings = towerSettingsFor(spot);
        return firstObstacle(level, tower.getBoundingBox(settings, towerOriginFor(scan, tower, spot, settings)));
    }

    private static List<Entry> decomposeTower(VillageScan scan, StructureTemplate tower, WallPlan.Tower spot, Villager villager) {
        StructurePlaceSettings settings = towerSettingsFor(spot);
        BlockPos origin = towerOriginFor(scan, tower, spot, settings);

        List<StructureTemplate.StructureBlockInfo> infos = new ArrayList<>();
        for (Block block : KNOWN_BLOCKS) {
            infos.addAll(tower.filterBlocks(origin, settings, block));
        }
        infos.sort(Comparator.comparingInt(info -> info.pos().getY()));

        List<Entry> entries = new ArrayList<>(infos.size());
        for (StructureTemplate.StructureBlockInfo info : infos) {
            entries.add(new Entry(info.pos(), info.state(), pointCost(info.state(), villager), false));
        }
        entries.addAll(iceFoundationFor(scan, entries, villager));
        return entries;
    }

    // --- дома (tech_3, см. NailerUpgrades.isTech3Unlocked/HouseSitePlan) - в отличие от стены дом
    // строится поблочно по ФИКСИРОВАННОЙ цене за весь дом (см. javadoc decomposeHouse), и строго по
    // одному: пока заложенный дом не достроен, новый не начинается (см. currentHouseEntries). ---

    private static StructurePlaceSettings houseSettings() {
        return new StructurePlaceSettings().setIgnoreEntities(true);
    }

    /**
     * Куски очереди для ОДНОГО дома, который сейчас строится в этой деревне: сначала - первый
     * заложенный, но не законченный (NailerHouses помнит закладки между перезапусками), и только если
     * все закончены и лимит MAX_NAILER_HOUSES не выбран - новая площадка (HouseSitePlan.chooseSite),
     * которая сразу записывается в NailerHouses. Раньше площадки искались заново при каждой пересборке
     * очереди, и начатый дом из неё выпадал (на площадке уже лежит его же булыжник - "занято"), а
     * nailer начинал следующий - см. javadoc NailerHouses.
     */
    private static List<Piece> currentHousePieces(ServerLevel level, VillageScan scan, WallPlan plan, Villager villager,
                                                  VillageStyle style) {
        NailerHouses registry = NailerHouses.get(level);
        List<NailerHouses.House> villageHouses = registry.inVillage(scan);
        for (NailerHouses.House house : villageHouses) {
            List<Piece> remaining = remainingWork(level, scan, plan, house, villager);
            if (!remaining.isEmpty()) {
                return remaining;
            }
        }
        if (villageHouses.size() >= VillageCalcSettings.MAX_NAILER_HOUSES) {
            return List.of();
        }
        NailerHouses.House newHouse = chooseNewHouse(level, scan, plan, style);
        if (newHouse == null) {
            return List.of();
        }
        registry.add(newHouse);
        return remainingWork(level, scan, plan, newHouse, villager);
    }

    @Nullable
    private static NailerHouses.House chooseNewHouse(ServerLevel level, VillageScan scan, WallPlan plan, VillageStyle style) {
        // Нет дома стиля (файл не добавили) - берём равнинный, как и для стены (VillageStyle.of).
        ResourceLocation id = style.house(VillageCalcSettings.DEFAULT_HOUSE_TEMPLATE);
        StructureTemplate house = template(level, id);
        if (house == null) {
            id = VillageStyle.PLAINS.house(VillageCalcSettings.DEFAULT_HOUSE_TEMPLATE);
            house = template(level, id);
        }
        if (house == null) {
            return null;
        }
        Vec3i size = house.getSize();
        HouseSitePlan.HouseSite site = HouseSitePlan.chooseSite(level, scan, plan, size.getX(), size.getY(), size.getZ());
        if (site == null) {
            return null;
        }
        // Y - уровень дома (медиана высот площадки, см. HouseSitePlan.HouseSite), а не высота угла:
        // площадка выравнивается под него земляными работами (HouseEarthworks).
        BlockPos origin = new BlockPos(scan.worldX(site.cellX()), site.groundY(), scan.worldZ(site.cellZ()));
        return new NailerHouses.House(origin, id);
    }

    /**
     * Что ещё осталось сделать для дома, куски очереди по порядку:
     * <ol>
     *     <li>земляные работы (HouseEarthworks) - срезка сверху вниз, потом насыпь снизу вверх, по
     *     HOUSE_EARTHWORK_COST очков за блок;</li>
     *     <li>сам дом - снизу вверх, фиксированная цена HOUSE_BUILD_COST (см. decomposeHouse);</li>
     *     <li>тропинки к дому и между кусками, которые он разорвал (HousePaths) - по той же цене за
     *     блок, что земляные работы. Считаются по земле ПОСЛЕ работ, так что их можно спланировать
     *     сразу, пока площадка ещё не готова.</li>
     * </ol>
     * Когда сам дом уже стоит, земляные работы больше не планируются: откос вокруг готового дома,
     * пересчитанный заново (особенно после перезапуска, когда скан видит уже срезанный рельеф), мог бы
     * найти мелкие "доделки", и nailer вечно ковырялся бы вокруг готового дома. Остаются только
     * недорисованные тропинки. Пусто - дом закончен целиком.
     * <p>
     * Деревья на площадке срубаются сразу (как VillageCalc.clearTrees: isStructure их не считает
     * препятствием, и без расчистки дом влез бы прямо в крону). Якоря кусков (для NailerWallClaims) -
     * от origin дома, чтобы не менялись между пересборками очереди.
     */
    private static List<Piece> remainingWork(ServerLevel level, VillageScan scan, WallPlan plan,
                                             NailerHouses.House house, Villager villager) {
        StructureTemplate template = template(level, house.template());
        if (template == null) {
            return List.of();
        }
        BoundingBox box = template.getBoundingBox(houseSettings(), house.origin());
        Piece housePiece = new Piece(house.origin().above(), houseEntries(level, house, villager));
        Piece pathsPiece = new Piece(house.origin().above(2),
                groundEntries(HousePaths.plan(level, scan, HouseEarthworks.finishedGround(level, scan, plan, box))));

        List<Piece> remaining = new ArrayList<>();
        if (!isBuilt(level, housePiece.entries())) {
            clearTreesInFootprint(level, scan, box);
            HouseEarthworks.clearApronTrunks(level, scan, plan, box, (pos, state) -> { });
            // Земляные работы не трогают клеток, где потом встанет дом или тропинка: иначе, например,
            // трава сверху откоса и тропинка на той же клетке по очереди перебивали бы друг друга
            // (claimNextPiece всегда возвращается к первому незаконченному куску - к земляным работам).
            Set<BlockPos> laterWork = positionsOf(housePiece.entries(), pathsPiece.entries());
            List<Entry> earthworks = groundEntries(HouseEarthworks.plan(level, scan, plan, box)).stream()
                    .filter(entry -> !laterWork.contains(entry.pos()))
                    .toList();
            remaining.add(new Piece(house.origin(), earthworks));
            remaining.add(housePiece);
        }
        remaining.add(pathsPiece);
        remaining.removeIf(piece -> isBuilt(level, piece.entries()));
        return remaining;
    }

    /** Земляные работы и тропинки - HOUSE_EARTHWORK_COST очков за каждый блок. */
    private static List<Entry> groundEntries(List<HouseEarthworks.Change> changes) {
        List<Entry> entries = new ArrayList<>(changes.size());
        for (HouseEarthworks.Change change : changes) {
            entries.add(new Entry(change.pos(), change.state(), VillageCalcSettings.HOUSE_EARTHWORK_COST, false));
        }
        return entries;
    }

    private static List<Entry> houseEntries(ServerLevel level, NailerHouses.House house, Villager villager) {
        StructureTemplate template = template(level, house.template());
        if (template == null) {
            return List.of();
        }
        return decomposeHouse(level, template, house.origin(), houseSettings(), villager);
    }

    private static boolean isBuilt(ServerLevel level, List<Entry> entries) {
        for (Entry entry : entries) {
            if (!statesMatch(level.getBlockState(entry.pos()), entry.state())) {
                return false;
            }
        }
        return true;
    }

    /** Забор/стенка/решётка (ROPE_FENCE, OAK_FENCE, COBBLESTONE_WALL, GLASS_PANE...) сами
     * пересчитывают свои north/south/east/west/up при любом соседнем block-update - у
     * decomposeFiller они сохранены как .defaultBlockState() (все стороны разъединены "в вакууме"), а
     * у decomposePiece/decomposeHouse - как записано в изолированном NBT-шаблоне (без соседей).
     * NailerBuildGoal ставит блок с Block.UPDATE_ALL, это тут же меняет фактические стороны под
     * реальных соседей - и живое состояние после этого НИКОГДА не совпадает с сохранённым по
     * .equals(). Строгое сравнение считало бы такую клетку вечно "недостроенной" (nailer без конца
     * переставлял бы одни и те же блоки, см. отчёт игрока). Поэтому сравниваем блок и все свойства,
     * КРОМЕ этих самопересчитываемых. Общее для NailerBuildGoal (что строить дальше) и
     * currentHouseEntries (достроен ли дом). */
    // Тем же образом сами пересчитываются: shape у ступенек (угловые/прямые - по соседям), in_wall у
    // калитки (рядом со стенкой), snowy у травы (снег сверху) - иначе такие клетки тоже "никогда не
    // совпадали" бы, и nailer переставлял бы их по кругу.
    private static final Set<String> SHAPE_PROPERTY_NAMES = Set.of("north", "south", "east", "west", "up",
            "shape", "in_wall", "snowy");

    public static boolean statesMatch(BlockState actual, BlockState expected) {
        // Срезка земли (HouseEarthworks) - цель "тут пусто": натекла вода, выросла трава - всё равно
        // пусто. Иначе такая клетка никогда не совпала бы, и nailer навсегда застрял бы на куске
        // земляных работ (claimNextPiece всегда берёт первый незавершённый).
        if (expected.isAir()) {
            return actual.isAir() || actual.canBeReplaced();
        }
        if (actual.getBlock() != expected.getBlock()) {
            return false;
        }
        for (Property<?> property : expected.getProperties()) {
            if (SHAPE_PROPERTY_NAMES.contains(property.getName())) {
                continue;
            }
            if (!Objects.equals(actual.getValue(property), expected.getValue(property))) {
                return false;
            }
        }
        return true;
    }

    private static void clearTreesInFootprint(ServerLevel level, VillageScan scan, BoundingBox box) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int cx = x - scan.minX();
                int cz = z - scan.minZ();
                if (!scan.inBounds(cx, cz) || !scan.isTree(cx, cz)) {
                    continue;
                }
                int base = scan.surfaceYAt(cx, cz);
                for (int dy = 1; dy <= box.getYSpan() + 1; dy++) {
                    cursor.set(x, base + dy, z);
                    if (VillageBlocks.isTreePart(level.getBlockState(cursor))) {
                        // Нет надобности в снапшоте для отката (как у /calculate) - настоящая
                        // постройка ничего не откатывает, пустой Recorder.
                        TreeCutter.cut(level, cursor.immutable(), (pos, state) -> { });
                    }
                }
            }
        }
    }

    /**
     * Все блоки, которые есть в палитре шаблона, кроме воздуха, structure_void (placeInWorld их не ставит) и
     * jigsaw (их decomposeHouse разбирает отдельно). Имена берутся из NBT самого шаблона (save), так что
     * дом любого стиля строится целиком без ручного списка материалов. Блок неустановленного мода (его нет
     * в реестре) пропускается.
     */
    private static Set<Block> templateBlocks(StructureTemplate template) {
        CompoundTag tag = template.save(new CompoundTag());
        List<ListTag> palettes = new ArrayList<>();
        if (tag.contains("palette", Tag.TAG_LIST)) {
            palettes.add(tag.getList("palette", Tag.TAG_COMPOUND));
        }
        ListTag several = tag.getList("palettes", Tag.TAG_LIST);
        for (int i = 0; i < several.size(); i++) {
            palettes.add(several.getList(i));
        }
        Set<Block> blocks = new HashSet<>();
        for (ListTag palette : palettes) {
            for (int i = 0; i < palette.size(); i++) {
                String name = palette.getCompound(i).getString("Name");
                BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(name))
                        .filter(block -> !block.defaultBlockState().isAir())
                        .filter(block -> block != Blocks.STRUCTURE_VOID && block != Blocks.JIGSAW)
                        .ifPresent(blocks::add);
            }
        }
        return blocks;
    }

    /** Разбирает placeInWorld-план дома на Entry, как decomposePiece/decomposeTower, но: 1) блоки - ВСЕ из
     * палитры самого шаблона (templateBlocks), а не KNOWN_BLOCKS (дом не из материалов стены) и не
     * заранее составленный список: у каждого стиля свои дома со своими материалами; 2)
     * minecraft:jigsaw блоки (шаблон изначально под ванильную генерацию, см. ModVillageHouses)
     * разбираются отдельно через JigsawReplacementProcessor.INSTANCE - filterBlocks() сам по себе
     * НИКАКИЕ процессоры не применяет (в отличие от placeInWorld), поэтому тут нужно явно прогнать
     * каждый найденный jigsaw-блок через него и заменить на final_state (или отбросить, если тот
     * оказался structure_void - см. javadoc самого процессора); 3) цена НЕ по материалу за блок, а
     * ФИКСИРОВАННАЯ за весь дом (HOUSE_BUILD_COST) - висит целиком на первом НЕ-земляном блоке
     * (после сортировки по Y), остальные бесплатны. Игрок явно попросил именно так - "на будущее
     * появятся альтернативные способы получить points для строительства", то есть цена дома не
     * обязана быть суммой материалов. Раньше цена висела просто на первой записи - а это трава из
     * нижнего слоя шаблона (там записана захваченная земля), которая на месте дома обычно уже
     * есть: nailer её пропускал как "уже стоит", и весь дом выходил бесплатным. */
    private static List<Entry> decomposeHouse(ServerLevel level, StructureTemplate house, BlockPos origin,
                                                StructurePlaceSettings settings, Villager villager) {
        List<StructureTemplate.StructureBlockInfo> infos = new ArrayList<>();
        for (Block block : templateBlocks(house)) {
            infos.addAll(house.filterBlocks(origin, settings, block));
        }
        for (StructureTemplate.StructureBlockInfo jigsawInfo : house.filterBlocks(origin, settings, Blocks.JIGSAW)) {
            StructureTemplate.StructureBlockInfo resolved = JigsawReplacementProcessor.INSTANCE
                    .processBlock(level, origin, origin, jigsawInfo, jigsawInfo, settings);
            if (resolved != null) {
                infos.add(resolved);
            }
        }
        infos.sort(Comparator.comparingInt(info -> info.pos().getY()));

        List<Entry> entries = new ArrayList<>(infos.size());
        boolean charged = false;
        for (StructureTemplate.StructureBlockInfo info : infos) {
            boolean ground = info.state().is(BlockTags.DIRT);
            int cost = !charged && !ground ? VillageCalcSettings.HOUSE_BUILD_COST : 0;
            charged |= cost > 0;
            entries.add(new Entry(info.pos(), info.state(), cost, false));
        }
        return entries;
    }

    private static int fillerBaseY(VillageScan scan, int cx, int cz) {
        return scan.surfaceYAt(cx, cz) + 1 - VillageCalcSettings.WALL_EMBED_DEPTH;
    }

    /** Что мешает столбику-заглушке: вне скана / глубокая вода / под ним постройка (нет опоры) / помеха в самой
     * колонке. null - столбик встанет. */
    @Nullable
    private static QueueTrace.Obstacle fillerObstacle(ServerLevel level, VillageScan scan, int cx, int cz) {
        int worldX = scan.worldX(cx);
        int worldZ = scan.worldZ(cz);
        if (!scan.inBounds(cx, cz)) {
            return new QueueTrace.Obstacle(new BlockPos(worldX, 0, worldZ), NO_BLOCK, "клетка вне скана");
        }
        int y = fillerBaseY(scan, cx, cz);
        if (scan.isWet(cx, cz)) {
            return new QueueTrace.Obstacle(new BlockPos(worldX, y, worldZ), NO_BLOCK, "глубокая вода");
        }
        BlockPos below = new BlockPos(worldX, y - 1, worldZ);
        if (!level.isLoaded(below)) {
            return new QueueTrace.Obstacle(below, NO_BLOCK, "чанк не прогружен");
        }
        BlockState belowState = level.getBlockState(below);
        if (VillageBlocks.isStructureAt(level, below, belowState)) {
            String why = isForeign(belowState)
                    ? "под столбиком чужая постройка, а не земля"
                    : "под столбиком наш же блок стены (план считался поверх уже построенной стены)";
            return new QueueTrace.Obstacle(below, belowState, why);
        }
        return firstObstacle(level, new BoundingBox(worldX, y, worldZ,
                worldX, y + VillageCalcSettings.FILLER_HEIGHT - 1, worldZ));
    }
}
