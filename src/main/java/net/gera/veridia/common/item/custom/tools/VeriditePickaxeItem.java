package net.gera.veridia.common.item.custom.tools;

import net.gera.veridia.common.mana.ManaHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VeriditePickaxeItem extends PickaxeItem {
    private static final int MANA_THRESHOLD_3X3 = 100;
    private static final int MANA_THRESHOLD_2X2 = 50;
    private static final int MANA_COST_PER_BLOCK = 1;
    private static final double REACH = 6.0D;

    public VeriditePickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entityLiving) {
        super.mineBlock(stack, level, state, pos, entityLiving);

        if (!(level instanceof ServerLevel serverLevel) || !(entityLiving instanceof ServerPlayer player)) {
            return true;
        }

        int mana = ManaHandler.getMana(player);
        ManaHandler.consumeMana(player, MANA_COST_PER_BLOCK);

        if (mana <= MANA_THRESHOLD_2X2) {
            return true;
        }

        Direction hitFace = resolveHitFace(player);
        if (hitFace == null) {
            return true;
        }

        List<BlockPos> extraPositions = mana > MANA_THRESHOLD_3X3
                ? squareOffsets(pos, hitFace)
                : leftDownOffsets(pos, hitFace, player);

        for (BlockPos extra : extraPositions) {
            if (extra.equals(pos)) continue;
            breakExtraBlock(serverLevel, extra, player, stack);
        }
        return true;
    }

    @Nullable
    private static Direction resolveHitFace(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * REACH, look.y * REACH, look.z * REACH);
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getDirection() : null;
    }

    private static Direction.Axis[] perpendicularAxes(Direction.Axis depthAxis) {
        return switch (depthAxis) {
            case X -> new Direction.Axis[]{Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private static BlockPos offsetAxis(BlockPos pos, Direction.Axis axis, int amount) {
        return switch (axis) {
            case X -> pos.offset(amount, 0, 0);
            case Y -> pos.offset(0, amount, 0);
            case Z -> pos.offset(0, 0, amount);
        };
    }

    private static BlockPos offset(BlockPos origin, Direction.Axis axisA, int a, Direction.Axis axisB, int b) {
        return offsetAxis(offsetAxis(origin, axisA, a), axisB, b);
    }

    private static List<BlockPos> squareOffsets(BlockPos origin, Direction hitFace) {
        Direction.Axis[] plane = perpendicularAxes(hitFace.getAxis());
        List<BlockPos> result = new ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                result.add(offset(origin, plane[0], a, plane[1], b));
            }
        }
        return result;
    }

    private static List<BlockPos> leftDownOffsets(BlockPos origin, Direction hitFace, ServerPlayer player) {
        Direction.Axis[] plane = perpendicularAxes(hitFace.getAxis());
        Direction.Axis axisA = plane[0];
        Direction.Axis axisB = plane[1];

        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));

        int signA = axisA == Direction.Axis.Y ? -1 : leftSign(right, axisA);
        int signB = axisB == Direction.Axis.Y ? -1 : leftSign(right, axisB);

        List<BlockPos> result = new ArrayList<>();
        for (int a : new int[]{0, signA}) {
            for (int b : new int[]{0, signB}) {
                result.add(offset(origin, axisA, a, axisB, b));
            }
        }
        return result;
    }

    private static int leftSign(Vec3 right, Direction.Axis axis) {
        double component = axis == Direction.Axis.X ? right.x : right.z;
        double left = -component;
        if (left > 1.0E-4D) return 1;
        return -1;
    }

    private static void breakExtraBlock(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        if (state.getDestroySpeed(level, pos) < 0) return;
        if (!level.mayInteract(player, pos)) return;
        if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) return;

        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        boolean destroyed = level.destroyBlock(pos, false, player);
        if (destroyed && !player.isCreative()) {
            Block.dropResources(state, level, pos, blockEntity, player, tool);
        }
    }
}