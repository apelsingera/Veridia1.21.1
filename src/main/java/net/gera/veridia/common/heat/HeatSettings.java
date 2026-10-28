package net.gera.veridia.common.heat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Переключатели жара на весь мир (хранятся в сохранении, в данных верхнего мира). Сейчас один: жар от больших
 * лавовых озёр пепельных биомов (тех, где «Слишком жарко!» и молибден). Меняется командой
 * /veridiadebug heat lakes on|off.
 */
public final class HeatSettings extends SavedData {
    private static final String DATA_NAME = "veridia_heat_settings";
    private static final String LAKE_HEAT_KEY = "LakeHeat";

    private static final SavedData.Factory<HeatSettings> FACTORY = new SavedData.Factory<>(HeatSettings::new, HeatSettings::load);

    private boolean lakeHeat = true;

    public static HeatSettings get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public boolean lakeHeat() {
        return lakeHeat;
    }

    public void setLakeHeat(boolean enabled) {
        lakeHeat = enabled;
        setDirty();
    }

    private static HeatSettings load(CompoundTag tag, HolderLookup.Provider registries) {
        HeatSettings settings = new HeatSettings();
        if (tag.contains(LAKE_HEAT_KEY)) {
            settings.lakeHeat = tag.getBoolean(LAKE_HEAT_KEY);
        }
        return settings;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean(LAKE_HEAT_KEY, lakeHeat);
        return tag;
    }
}
