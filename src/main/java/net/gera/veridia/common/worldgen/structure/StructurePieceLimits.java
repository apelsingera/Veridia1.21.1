package net.gera.veridia.common.worldgen.structure;

import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class StructurePieceLimits {

    private StructurePieceLimits() {
    }

    private static final Map<StructurePoolElement, Integer> LIMITS = new IdentityHashMap<>();

    private static final ThreadLocal<Map<StructurePoolElement, Integer>> PLACED =
            ThreadLocal.withInitial(IdentityHashMap::new);

    public static synchronized void limit(StructurePoolElement element, int maxPerStructure) {
        LIMITS.put(element, maxPerStructure);
    }

    public static synchronized void clearLimits() {
        LIMITS.clear();
    }

    private static synchronized Integer limitOf(StructurePoolElement element) {
        return LIMITS.isEmpty() ? null : LIMITS.get(element);
    }

    public static void beginStructure() {
        PLACED.get().clear();
    }

    public static void onPiecePlaced(StructurePoolElement element) {
        if (limitOf(element) == null) {
            return;
        }
        PLACED.get().merge(element, 1, Integer::sum);
    }

    public static boolean isExhausted(StructurePoolElement element) {
        Integer limit = limitOf(element);
        return limit != null && PLACED.get().getOrDefault(element, 0) >= limit;
    }

    public static List<StructurePoolElement> filterExhausted(List<StructurePoolElement> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        List<StructurePoolElement> allowed = null;
        for (int i = 0; i < candidates.size(); i++) {
            StructurePoolElement element = candidates.get(i);
            if (isExhausted(element)) {
                if (allowed == null) {
                    allowed = new ArrayList<>(candidates.subList(0, i));
                }
            } else if (allowed != null) {
                allowed.add(element);
            }
        }
        return allowed == null ? candidates : allowed;
    }
}