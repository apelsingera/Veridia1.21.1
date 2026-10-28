package net.gera.veridia.server.discord;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DiscordPopupPreferences extends SavedData {
    private static final String DATA_NAME = "veridia_discord_popup_prefs";

    public static final SavedData.Factory<DiscordPopupPreferences> FACTORY =
            new SavedData.Factory<>(DiscordPopupPreferences::new, DiscordPopupPreferences::load);

    private final Set<UUID> disabled = new HashSet<>();

    public static DiscordPopupPreferences get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public boolean isDisabled(UUID playerId) {
        return disabled.contains(playerId);
    }

    public void setDisabled(UUID playerId, boolean value) {
        if (value) {
            disabled.add(playerId);
        } else {
            disabled.remove(playerId);
        }
        setDirty();
    }

    private static DiscordPopupPreferences load(CompoundTag tag, HolderLookup.Provider registries) {
        DiscordPopupPreferences data = new DiscordPopupPreferences();
        ListTag list = tag.getList("disabled", StringTag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            data.disabled.add(UUID.fromString(list.getString(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (UUID id : disabled) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put("disabled", list);
        return tag;
    }
}
