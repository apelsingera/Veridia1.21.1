package net.gera.veridia.common.loot;

import com.mojang.serialization.MapCodec;
import net.gera.veridia.Veridia;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModLootModifier {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Veridia.MOD_ID);

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> ADD_ITEM =
            LOOT_MODIFIER_SERIALIZERS.register("add_item", () -> AddItemModifier.CODEC);

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> REPLACE_ITEM =
            LOOT_MODIFIER_SERIALIZERS.register("replace_item", () -> ReplaceItemModifier.CODEC);

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> REPLACE_LOOT_IN_BIOMES =
            LOOT_MODIFIER_SERIALIZERS.register("replace_loot_in_biomes", () -> ReplaceLootInBiomesModifier.CODEC);

    public static void register (IEventBus eventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(eventBus);
    }
}
