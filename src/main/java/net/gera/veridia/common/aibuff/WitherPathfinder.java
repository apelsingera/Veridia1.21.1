package net.gera.veridia.common.aibuff;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Поиск маршрута A* для мобов, которые умеют ломать блоки (WitherBlockBreakGoal): визер-скелет, зомби-шахтёры,
 * уровневые зомби и хаски. Подробный разбор формул - в witherpathfinder.md рядом.
 * <p>
 * Узел - клетка, в которой моб может стоять ногами. Шаг: на 1 блок в одну из 4 сторон с перепадом высоты -1/0/+1
 * или падение с края на 2-3 блока. Цена шага: 1 плюс {@link Settings#breakCost()} за каждый блок, который на нём
 * надо сломать (блок в клетках тела моба, если его можно сломать, см. {@link Terrain#isMineable}). Поэтому путь
 * в обход стены побеждает ломание, пока обход короче ломания на цену блока.
 * <p>
 * Сам поиск не знает про Minecraft-мир: он работает через {@link Terrain}, поэтому проверяется на искусственных картах
 * без запуска игры. Мир подставляет {@link LevelWitherTerrain}.
 */
public final class WitherPathfinder {

    /** Падение на столько блоков и меньше не наносит урона - дальше планировщик с обрыва не спускается. */
    public static final int MAX_FALL_DISTANCE = 3;

    private static final int[][] HORIZONTAL_DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[] STEP_DELTAS = {0, 1, -1};
    /** Допуск при сравнении цен: цены - суммы целых, но double сравниваем с запасом. */
    private static final double COST_EPSILON = 1.0e-6;

    private WitherPathfinder() {
    }

    /** Что планировщик знает о мире. Клетка в непрогруженном чанке не проходима, не ломаема и не держит ногу. */
    public interface Terrain {
        /** Через клетку можно пройти, ничего не ломая (воздух или блок без коллизии). */
        boolean isPassable(BlockPos pos);

        /** Верхняя грань блока в этой клетке выдерживает моба (на ней можно стоять). */
        boolean isSolidFloor(BlockPos pos);

        /** Блок в клетке можно сломать (не воздух и разрешён списком AbilityConfig). */
        boolean isMineable(BlockPos pos);
    }

    /** Числа поиска. В игре берутся из AbilityConfig (fromConfig), в проверках задаются прямо. */
    public record Settings(double breakCost, int nodeBudget, int margin, boolean allowBreaking) {
        public Settings(double breakCost, int nodeBudget, int margin) {
            this(breakCost, nodeBudget, margin, true);
        }

        public static Settings fromConfig() {
            return new Settings(AbilityConfig.WITHER_BREAK_PATH_BREAK_COST,
                    AbilityConfig.WITHER_BREAK_SEARCH_NODE_BUDGET,
                    AbilityConfig.WITHER_BREAK_SEARCH_MARGIN);
        }

        /** Те же числа, но ломать блоки нельзя: поиск видит только то, куда моб дойдёт пешком. */
        public Settings withoutBreaking() {
            return new Settings(breakCost, nodeBudget, margin, false);
        }
    }

    public record PlannedStep(BlockPos pos, List<BlockPos> breakTargets) {}

    /** Клеток тела моба по высоте: рост, округлённый вверх (визер-скелет 2.4 - это 3 клетки, зомби 1.95 - 2). */
    public static int clearanceHeight(Mob mob) {
        return Math.max(1, (int) Math.ceil(mob.getBbHeight()));
    }

    /**
     * План от моба к цели: шаги по порядку, у каждого - блоки, которые надо сломать, чтобы встать в клетку.
     * null - маршрута нет (в окне поиска или в бюджете узлов); пустой список - моб уже рядом с целью.
     */
    @Nullable
    public static List<PlannedStep> findPlan(Mob mob, LivingEntity target) {
        return findPlan(new LevelWitherTerrain(mob.level()), mob.blockPosition(), target.blockPosition(),
                clearanceHeight(mob), Settings.fromConfig());
    }

    @Nullable
    public static List<PlannedStep> findPlan(Terrain terrain, BlockPos start, BlockPos goal, int clearanceHeight,
                                             Settings settings) {
        Approach approach = findApproach(terrain, start, goal, clearanceHeight, settings);
        return approach.reachesGoal() ? approach.steps() : null;
    }

    /**
     * Путь к цели, а если цель недостижима - к ближайшей к ней клетке, куда можно дойти НЕ ломая блоков (сначала ближе по
     * горизонтали, потом по высоте). Нужен, чтобы моб, которому не достать игрока на столбе или крыше, подошёл к его
     * основанию и там поднялся башней, а не строил столб там, где остановился.
     */
    public record Approach(List<PlannedStep> steps, boolean reachesGoal, BlockPos end) {}

    public static Approach findApproach(Mob mob, LivingEntity target) {
        return findApproach(new LevelWitherTerrain(mob.level()), mob.blockPosition(), target.blockPosition(),
                clearanceHeight(mob), Settings.fromConfig());
    }

    /** То же, но не ломая блоков вовсе: куда моб дойдёт пешком (основание башни, у которой стоит игрок). */
    public static Approach findWalkingApproach(Mob mob, LivingEntity target) {
        return findApproach(new LevelWitherTerrain(mob.level()), mob.blockPosition(), target.blockPosition(),
                clearanceHeight(mob), Settings.fromConfig().withoutBreaking());
    }

    public static Approach findApproach(Terrain terrain, BlockPos start, BlockPos goal, int clearanceHeight,
                                        Settings settings) {
        Search search = new Search(terrain, goal, clearanceHeight, settings, Bounds.around(start, goal, settings.margin()));
        Node reached = search.run(start);
        if (reached != null) {
            return new Approach(search.stepsTo(reached), true, reached.pos());
        }
        Node closest = search.closest();
        return new Approach(closest == null ? List.of() : search.stepsTo(closest), false,
                closest == null ? start : closest.pos());
    }

    /** Прямоугольный ящик, за пределы которого поиск не выходит: моб и цель плюс margin во все стороны. */
    private record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        static Bounds around(BlockPos a, BlockPos b, int margin) {
            return new Bounds(
                    Math.min(a.getX(), b.getX()) - margin, Math.max(a.getX(), b.getX()) + margin,
                    Math.min(a.getY(), b.getY()) - margin, Math.max(a.getY(), b.getY()) + margin,
                    Math.min(a.getZ(), b.getZ()) - margin, Math.max(a.getZ(), b.getZ()) + margin);
        }

        boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX
                    && pos.getY() >= minY && pos.getY() <= maxY
                    && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }
    }

    /** cost - цена пути от старта (g), priority - cost плюс оценка остатка (f), breaks - сколько блоков ломать на всём пути. */
    private record Node(BlockPos pos, double cost, double priority, int breaks, Node parent) {
        /** По приоритету; при равных раньше тот, что дальше от старта (ближе к цели): так поиск идёт к цели, а не по всему равному фронту. */
        static final Comparator<Node> ORDER = Comparator.comparingDouble(Node::priority)
                .thenComparing(Comparator.comparingDouble(Node::cost).reversed());
    }

    /** Что нужно для стоянки в клетке: какие клетки тела (снизу вверх, бит = номер) надо сломать. */
    private record Step(int breakMask) {
        static final Step FREE = new Step(0);
        static final Step BLOCKED = new Step(-1);

        boolean isBlocked() {
            return breakMask < 0;
        }

        int breakCount() {
            return Integer.bitCount(breakMask);
        }
    }

    private static final class Search {

        private final Terrain terrain;
        private final BlockPos goal;
        private final int clearance;
        private final Settings settings;
        private final Bounds bounds;

        private final PriorityQueue<Node> open = new PriorityQueue<>(Node.ORDER);
        private final Long2DoubleOpenHashMap bestCost = new Long2DoubleOpenHashMap();
        /** Что нужно для стоянки в клетке - считается один раз на клетку: в неё ведёт до 12 соседей, а чтение мира дорогое. */
        private final Long2ObjectOpenHashMap<Step> steps = new Long2ObjectOpenHashMap<>();

        Search(Terrain terrain, BlockPos goal, int clearance, Settings settings, Bounds bounds) {
            this.terrain = terrain;
            this.goal = goal;
            this.clearance = clearance;
            this.settings = settings;
            this.bounds = bounds;
            this.bestCost.defaultReturnValue(Double.MAX_VALUE);
        }

        /** Ближайший к цели из разобранных узлов, к которому ведёт путь без ломания (rank - см. approachRank). */
        private Node closest;
        private long closestRank = Long.MAX_VALUE;

        @Nullable
        Node run(BlockPos start) {
            open.add(new Node(start, 0.0, heuristic(start), 0, null));
            bestCost.put(start.asLong(), 0.0);

            int expansions = 0;
            while (!open.isEmpty() && expansions < settings.nodeBudget()) {
                Node current = open.poll();
                if (current.cost() > bestCost.get(current.pos().asLong()) + COST_EPSILON) {
                    continue; // устаревшая запись в очереди: до этой клетки уже нашли путь дешевле
                }
                if (isNearGoal(current.pos())) {
                    return current;
                }
                considerClosest(current);
                expansions++;
                expand(current);
            }
            return null;
        }
        /** Куда дошёл бы поиск, не найдя цели; null - не разобрано ни одного узла. */

        @Nullable
        Node closest() {
            return closest;
        }

        private void considerClosest(Node node) {
            if (node.breaks() != 0) {
                return;
            }
            long rank = approachRank(node.pos());
            if (rank < closestRank || (rank == closestRank && node.cost() < closest.cost())) {
                closest = node;
                closestRank = rank;
            }
        }

        /** Чем меньше, тем ближе к цели: сначала по горизонтали (до 1 блока по каждой оси не считается), потом по высоте. */
        private long approachRank(BlockPos pos) {
            return horizontalRemaining(pos) * 1000L + Math.abs(goal.getY() - pos.getY());
        }

        private void expand(Node current) {
            for (int[] direction : HORIZONTAL_DIRECTIONS) {
                for (int deltaY : STEP_DELTAS) {
                    offer(current, current.pos().offset(direction[0], deltaY, direction[1]));
                }
                BlockPos landing = fallLanding(current.pos(), direction[0], direction[1]);
                if (landing != null) {
                    offer(current, landing);
                }
            }
        }

        private void offer(Node from, BlockPos candidate) {
            if (!bounds.contains(candidate)) {
                return;
            }
            Step step = stepAt(candidate);
            if (step.isBlocked()) {
                return;
            }
            double cost = from.cost() + 1.0 + step.breakCount() * settings.breakCost();
            long key = candidate.asLong();
            if (bestCost.get(key) <= cost + COST_EPSILON) {
                return;
            }
            bestCost.put(key, cost);
            open.add(new Node(candidate, cost, cost + heuristic(candidate), from.breaks() + step.breakCount(), from));
        }

        private Step stepAt(BlockPos foot) {
            long key = foot.asLong();
            Step step = steps.get(key);
            if (step == null) {
                step = evaluate(foot);
                steps.put(key, step);
            }
            return step;
        }

        /** Можно ли встать в клетку foot: пол держит, а клетки тела свободны или ломаемы. */
        private Step evaluate(BlockPos foot) {
            if (!terrain.isSolidFloor(foot.below())) {
                return Step.BLOCKED;
            }
            int breakMask = 0;
            for (int level = 0; level < clearance; level++) {
                BlockPos cell = foot.above(level);
                if (terrain.isPassable(cell)) {
                    continue;
                }
                if (!settings.allowBreaking() || !terrain.isMineable(cell)) {
                    return Step.BLOCKED;
                }
                breakMask |= 1 << level;
            }
            return breakMask == 0 ? Step.FREE : new Step(breakMask);
        }

        /**
         * Падение с края на 2..MAX_FALL_DISTANCE блоков: клетка за краем и вся шахта под ней до места приземления должны
         * быть свободны (иначе моб "провалился" бы сквозь пол), а внизу - пол. Падение на 1 блок - обычный шаг вниз.
         */
        @Nullable
        private BlockPos fallLanding(BlockPos from, int dx, int dz) {
            BlockPos edge = from.offset(dx, 0, dz);
            for (int level = 0; level < clearance; level++) {
                if (!terrain.isPassable(edge.above(level))) {
                    return null;
                }
            }
            for (int fall = 1; fall <= MAX_FALL_DISTANCE; fall++) {
                BlockPos foot = edge.below(fall);
                if (!terrain.isPassable(foot)) {
                    return null;
                }
                if (fall >= 2 && terrain.isSolidFloor(foot.below())) {
                    return foot;
                }
            }
            return null;
        }

        /**
         * Нижняя оценка цены остатка пути. Каждый шаг сдвигает ровно на 1 по X или Z, поднимает не больше чем на 1,
         * опускает не больше чем на MAX_FALL_DISTANCE и стоит не меньше 1 - значит, число шагов не меньше оценки
         * по каждой из осей, и цена пути не меньше их максимума. Оценка не завышает цену, поэтому A* возвращает самый
         * дешёвый путь. Доходить до цели вплотную не нужно (isNearGoal допускает 1 блок по каждой оси) - отсюда "- 1".
         */
        private double heuristic(BlockPos pos) {
            int rise = goal.getY() - pos.getY();
            int vertical = rise > 0
                    ? Math.max(0, rise - 1)
                    : Math.ceilDiv(Math.max(0, -rise - 1), MAX_FALL_DISTANCE);
            return Math.max(horizontalRemaining(pos), vertical);
        }

        /** Сколько шагов по горизонтали осталось до цели (до 1 блока по каждой оси не доходим). */
        private int horizontalRemaining(BlockPos pos) {
            return Math.max(0, Math.abs(pos.getX() - goal.getX()) - 1)
                    + Math.max(0, Math.abs(pos.getZ() - goal.getZ()) - 1);
        }

        private boolean isNearGoal(BlockPos pos) {
            return Math.abs(pos.getX() - goal.getX()) <= 1
                    && Math.abs(pos.getY() - goal.getY()) <= 1
                    && Math.abs(pos.getZ() - goal.getZ()) <= 1;
        }

        /** Цепочка от старта (без него) до reached и блоки на слом у каждого шага. */
        List<PlannedStep> stepsTo(Node reached) {
            Deque<BlockPos> waypoints = new ArrayDeque<>();
            for (Node node = reached; node.parent() != null; node = node.parent()) {
                waypoints.addFirst(node.pos());
            }
            List<PlannedStep> plan = new ArrayList<>(waypoints.size());
            for (BlockPos pos : waypoints) {
                plan.add(new PlannedStep(pos, breakTargets(pos, stepAt(pos))));
            }
            return plan;
        }

        private List<BlockPos> breakTargets(BlockPos foot, Step step) {
            if (step.breakMask() <= 0) {
                return List.of();
            }
            List<BlockPos> targets = new ArrayList<>(step.breakCount());
            for (int level = 0; level < clearance; level++) {
                if ((step.breakMask() & (1 << level)) != 0) {
                    targets.add(foot.above(level));
                }
            }
            return targets;
        }
    }
}
