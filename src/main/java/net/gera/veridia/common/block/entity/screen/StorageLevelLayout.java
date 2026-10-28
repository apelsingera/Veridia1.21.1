package net.gera.veridia.common.block.entity.screen;

import net.gera.veridia.Veridia;
import net.minecraft.resources.ResourceLocation;

public enum StorageLevelLayout {
    LEVEL_1(1, "generic_level1_gui", new int[][]{{12, 27}, {30, 27}, {48, 27}}),
    LEVEL_2(2, "generic_level2_gui", new int[][]{{12, 27}, {30, 27}, {48, 27}, {30, 45}}),
    LEVEL_3(3, "generic_level3_gui", new int[][]{{12, 27}, {30, 27}, {48, 27}, {21, 45}, {39, 45}}),
    LEVEL_4(4, "generic_level4_gui", new int[][]{{12, 27}, {30, 27}, {48, 27}, {12, 45}, {30, 45}, {48, 45}});

    private final int level;
    private final ResourceLocation texture;
    private final int[][] slotPositions;

    StorageLevelLayout(int level, String textureName, int[][] slotPositions) {
        this.level = level;
        this.texture = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID,
                "textures/gui/levels/village/" + textureName + ".png");
        this.slotPositions = slotPositions;
    }

    public int level() {
        return level;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public int slotCount() {
        return slotPositions.length;
    }

    public int slotX(int index) {
        return slotPositions[index][0];
    }

    public int slotY(int index) {
        return slotPositions[index][1];
    }

    public static StorageLevelLayout forLevel(int level) {
        return switch (level) {
            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            case 3 -> LEVEL_3;
            default -> LEVEL_4;
        };
    }
}