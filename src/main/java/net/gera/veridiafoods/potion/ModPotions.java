package net.gera.veridiafoods.potion;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(BuiltInRegistries.POTION, VeridiaFoods.MOD_ID);

    public static final Holder<Potion> PLAGUE_POTION = POTIONS.register("plague_potion",
            () -> new Potion(new MobEffectInstance(ModEffects.PLAGUE, 600,0)));
    private static IEventBus eventBus;

    public static void register(IEventBus eventBus) {
        ModPotions.eventBus = eventBus;
        POTIONS.register(eventBus);
    }
}