package net.gera.veridia.item.custom;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoodProps {
    public static final FoodProperties DANDELION_ROOTS = new FoodProperties.Builder().nutrition(2).saturationModifier(0.75f)
            .effect(() -> new MobEffectInstance(MobEffects.POISON, 100, 1), 0.2f).build();

    public static final FoodProperties GARLIC = new FoodProperties.Builder().nutrition(3).saturationModifier(0.75f).build();
    public static final FoodProperties BLUE_BERRY = new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).build();


}
