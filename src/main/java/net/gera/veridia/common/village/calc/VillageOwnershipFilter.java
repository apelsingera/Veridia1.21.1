package net.gera.veridia.common.village.calc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;

import java.util.ArrayList;
import java.util.List;
final class VillageOwnershipFilter {

    private VillageOwnershipFilter() {
    }
    static boolean isInVillageStructure(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE).isValid();
    }
    static List<VillageScan.Marker> keepClusterNearCenter(List<VillageScan.Marker> markers, BlockPos center) {
        int markerCount = markers.size();
        if (markerCount == 0) {
            return markers;
        }
        int[] parent = new int[markerCount];
        for (int index = 0; index < markerCount; index++) {
            parent[index] = index;
        }
        for (int index = 0; index < markerCount; index++) {
            BlockPos a = markers.get(index).pos();
            for (int otherIndex = index + 1; otherIndex < markerCount; otherIndex++) {
                BlockPos b = markers.get(otherIndex).pos();
                int dx = Math.abs(a.getX() - b.getX());
                int dz = Math.abs(a.getZ() - b.getZ());
                if (Math.max(dx, dz) <= VillageCalcSettings.VILLAGE_CLUSTER_GAP) {
                    union(parent, index, otherIndex);
                }
            }
        }

        int nearest = 0;
        long best = Long.MAX_VALUE;
        for (int index = 0; index < markerCount; index++) {
            BlockPos pos = markers.get(index).pos();
            long dx = pos.getX() - center.getX();
            long dz = pos.getZ() - center.getZ();
            long distSq = dx * dx + dz * dz;
            if (distSq < best) {
                best = distSq;
                nearest = index;
            }
        }
        int keepRoot = find(parent, nearest);

        List<VillageScan.Marker> kept = new ArrayList<>();
        for (int index = 0; index < markerCount; index++) {
            if (find(parent, index) == keepRoot) {
                kept.add(markers.get(index));
            }
        }
        return kept;
    }

    private static int find(int[] parent, int index) {
        while (parent[index] != index) {
            parent[index] = parent[parent[index]];
            index = parent[index];
        }
        return index;
    }

    private static void union(int[] parent, int first, int second) {
        int firstRoot = find(parent, first);
        int secondRoot = find(parent, second);
        if (firstRoot != secondRoot) {
            parent[firstRoot] = secondRoot;
        }
    }
}
