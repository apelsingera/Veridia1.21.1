package net.gera.veridia.common.block.entity.types;

public final class StorageLevelConfig {
    private StorageLevelConfig() {}

    public static final int MAX_LEVEL = 4;
    public static final int[] LEVEL_UP_THRESHOLDS = {128, 256, 512};

    public static int levelFor(int totalReceived) {
        int level = 1;
        for (int threshold : LEVEL_UP_THRESHOLDS) {
            if (totalReceived < threshold) break;
            level++;
        }
        return Math.min(level, MAX_LEVEL);
    }

    public static int previousThreshold(int level) {
        return level <= 1 ? 0 : LEVEL_UP_THRESHOLDS[level - 2];
    }

    public static int nextThreshold(int level) {
        return level >= MAX_LEVEL ? previousThreshold(level) : LEVEL_UP_THRESHOLDS[level - 1];
    }
}