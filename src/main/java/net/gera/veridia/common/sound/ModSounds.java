package net.gera.veridia.common.sound;

import net.gera.veridia.Veridia;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Veridia.MOD_ID);

    public static final Supplier<SoundEvent> OMEN_REPRISE = registerSoundEvent("omen_reprise");
    public static final ResourceKey<JukeboxSong> OMEN_REPRISE_KEY = createSong();

    public static final Supplier<SoundEvent> RUNIC_PEDESTAL_SFX_1 = registerSoundEvent("runic_pedestal_sfx1");
    public static final Supplier<SoundEvent> RUNIC_PEDESTAL_SFX_2 = registerSoundEvent("runic_pedestal_sfx2");
    public static final Supplier<SoundEvent> RUNIC_PEDESTAL_SFX_3 = registerSoundEvent("runic_pedestal_sfx3");

    public static final DeferredHolder<SoundEvent, SoundEvent> THE_BIG_GUNDOWN = SOUND_EVENTS.register("the_big_gundown",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "the_big_gundown")));

    private static ResourceKey<JukeboxSong> createSong() {
        return ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "omen_reprise"));
    }

    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}