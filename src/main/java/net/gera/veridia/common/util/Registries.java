package net.gera.veridia.common.util;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.checkerframework.framework.qual.Unused;

import java.util.function.Supplier;

public class Registries {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "veridia");

    public static final Supplier<MapCodec<RandomCountLootModifier>> RANDOM_COUNT_MEAT =
            LOOT_MODIFIER_SERIALIZERS.register("random_count_meat", () -> RandomCountLootModifier.CODEC);

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(eventBus);
    }
}