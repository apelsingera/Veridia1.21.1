package net.gera.veridiafoods.recipe.cookingtable;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ToolRequirement implements StringRepresentable {
    NONE("none"),
    HAMMER("hammer"),
    KNIFE("knife");

    public static final Codec<ToolRequirement> CODEC = StringRepresentable.fromEnum(ToolRequirement::values);

    private final String name;

    ToolRequirement(String name) {
        this.name = name;
    }

    public static ToolRequirement byId(int id) {
        return switch (id) {
            case 1 -> HAMMER;
            case 2 -> KNIFE;
            default -> NONE;
        };
    }

    public int getId() {
        return ordinal();
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}