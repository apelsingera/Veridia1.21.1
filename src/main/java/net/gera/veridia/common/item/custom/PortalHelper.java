package net.gera.veridia.common.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.PortalShape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class PortalHelper {

    public static boolean IS_CUSTOM_IGNITION = false;

    public static List<BlockPos> tryCreatePortal(Level level, BlockPos targetPos) {
        Optional<PortalShape> shape = PortalShape.findEmptyPortalShape(level, targetPos, Direction.Axis.X);

        if (shape.isEmpty()) {
            shape = PortalShape.findEmptyPortalShape(level, targetPos, Direction.Axis.Z);
        }

        if (shape.isPresent()) {
            IS_CUSTOM_IGNITION = true;
            shape.get().createPortalBlocks();
            IS_CUSTOM_IGNITION = false;

            return getConnectedPortalBlocks(level, targetPos);
        }

        return null;
    }

    public static List<BlockPos> getConnectedPortalBlocks(Level level, BlockPos startPos) {
        List<BlockPos> portalBlocks = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (level.getBlockState(current).is(Blocks.NETHER_PORTAL)) {
                portalBlocks.add(current);
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return portalBlocks;
    }
}