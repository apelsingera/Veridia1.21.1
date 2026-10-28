package net.gera.veridia.common.entity.ominous;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Наземная навигация для мобов, которым магма не страшна. Ванильный поиск пути помечает клетку на магме как
 * DAMAGE_FIRE, а клетки рядом с ней как DANGER_FIRE - так же, как огонь и лаву, - и мобы её обходят. Здесь такие
 * клетки становятся обычной землёй, если «огонь» вокруг - только магма (ванильная и чернитовая, всё, что
 * наследует {@link MagmaBlock}). Настоящий огонь, костры и лава остаются опасными.
 * <p>
 * Для мобов, которые пригибаются ({@link DuckingMob}), высота прохода считается по росту пригнувшись, но клетки, где
 * пришлось бы пригибаться, стоят дороже: такой моб предпочитает обойти.
 */
public class MagmaWalkingNavigation extends GroundPathNavigation {
    public MagmaWalkingNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new Evaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    private static class Evaluator extends WalkNodeEvaluator {
        // Коробка роста стоя чуть поднята над полом, чтобы сам пол не считался столкновением
        private static final double STANDING_BOX_LIFT = 0.001D;
        private static final float BLOCKED_MALUS = -1.0F;

        // Цена клетки, где пришлось бы пригибаться, на этот поиск (DuckingMob.duckingPathMalus); NaN - моб не пригибается
        private float duckingMalus = Float.NaN;

        // Мобу, который пригибается (DuckingMob), хватает прохода по его росту пригнувшись: ванильный поиск взял бы
        // рост стоя, и высокий воин не полез бы в проход в 2 блока
        @Override
        public void prepare(PathNavigationRegion level, Mob mob) {
            super.prepare(level, mob);
            this.duckingMalus = Float.NaN;
            if (mob instanceof DuckingMob ducking) {
                this.entityHeight = Mth.floor(ducking.duckingHeight() + 1.0F);
                this.duckingMalus = ducking.duckingPathMalus();
            }
        }

        // Клетка, где стоя не поместиться, для пригибающегося моба дороже или вовсе закрыта: низкий проход он выбирает,
        // только если обходить слишком далеко или обхода нет. Max, а не сумма: одна и та же клетка принимается
        // несколько раз (от разных соседей)
        @Override
        protected Node findAcceptedNode(int x, int y, int z, int verticalDeltaLimit, double nodeFloorLevel,
                                        Direction direction, PathType pathType) {
            Node node = super.findAcceptedNode(x, y, z, verticalDeltaLimit, nodeFloorLevel, direction, pathType);
            if (Float.isNaN(duckingMalus) || node == null || node.costMalus < 0.0F || standingFitsAt(node)) {
                return node;
            }
            node.costMalus = duckingMalus < 0.0F ? BLOCKED_MALUS : Math.max(node.costMalus, duckingMalus);
            return node;
        }

        // Помещается ли моб стоя (во весь рост) на этой клетке, от уровня пола до макушки
        private boolean standingFitsAt(Node node) {
            double floor = this.getFloorLevel(new BlockPos(node.x, node.y, node.z));
            double halfWidth = this.mob.getBbWidth() / 2.0D;
            double height = this.mob.getDimensions(Pose.STANDING).height();
            AABB box = new AABB(node.x + 0.5D - halfWidth, floor + STANDING_BOX_LIFT, node.z + 0.5D - halfWidth,
                    node.x + 0.5D + halfWidth, floor + height, node.z + 0.5D + halfWidth);
            return this.currentContext.level().noCollision(this.mob, box);
        }

        @Override
        public PathType getPathType(PathfindingContext context, int x, int y, int z) {
            PathType type = super.getPathType(context, x, y, z);
            boolean fireRelated = type == PathType.DAMAGE_FIRE || type == PathType.DANGER_FIRE;
            return fireRelated && onlyMagmaAround(context.level(), x, y, z) ? PathType.WALKABLE : type;
        }

        // Тот же куб 3x3x3 вокруг клетки, по которому ванильный поиск ищет опасные соседей
        private static boolean onlyMagmaAround(BlockGetter level, int x, int y, int z) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        pos.set(x + dx, y + dy, z + dz);
                        if (isRealFire(level, pos)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        private static boolean isRealFire(BlockGetter level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof MagmaBlock) {
                return false;
            }
            return state.getFluidState().is(FluidTags.LAVA)
                    || NodeEvaluator.isBurningBlock(state)
                    || state.getBlockPathType(level, pos, null) == PathType.DAMAGE_FIRE;
        }
    }
}
