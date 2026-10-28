package net.gera.veridia.common.villager.storage;

import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.world.entity.npc.VillagerProfession;

public enum StorageKind {
    MATERIALS("veridia_materials_storage"),
    FOOD("veridia_food_storage");

    private final String prefix;

    StorageKind(String prefix) {
        this.prefix = prefix;
    }
    public String posKey() {
        return prefix + "_pos";
    }
    public String dimKey() {
        return prefix + "_dim";
    }
    public String idKey() {
        return prefix + "_id";
    }

    /**
     * Профессии, которые пользуются этим хранилищем: плотник и лесоруб берут и кладут материалы, фермер и лесоруб -
     * еду. Остальных жителей к хранилищу привязать нельзя.
     */
    public boolean accepts(VillagerProfession profession) {
        return switch (this) {
            case MATERIALS -> profession == ModVillagers.NAILER.value() || profession == ModVillagers.LUMBERJACK.value();
            case FOOD -> profession == VillagerProfession.FARMER || profession == ModVillagers.LUMBERJACK.value();
        };
    }

    /** Работает ли профессия хоть с каким-то хранилищем. */
    public static boolean anyAccepts(VillagerProfession profession) {
        for (StorageKind kind : values()) {
            if (kind.accepts(profession)) {
                return true;
            }
        }
        return false;
    }
}
