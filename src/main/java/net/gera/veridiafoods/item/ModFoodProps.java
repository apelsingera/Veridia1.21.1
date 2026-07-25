package net.gera.veridiafoods.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class ModFoodProps {
    public static final FoodProperties DANDELION_ROOTS = new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f)
            .effect(() -> new MobEffectInstance(MobEffects.POISON, 200, 2), 0.15f).build();
    public static final FoodProperties RAW_EGG = new FoodProperties.Builder().nutrition(1).saturationModifier(0.2f)
            .effect(() -> new MobEffectInstance(MobEffects.POISON, 100, 1), 0.5f).build();
    public static final FoodProperties PIE = new FoodProperties.Builder().nutrition(5).saturationModifier(0.7f)
            .effect(() -> new MobEffectInstance(MobEffects.JUMP, 600, 1), 0.65f).build();

    public static final FoodProperties EGG = new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).build();
    public static final FoodProperties CHEESE = new FoodProperties.Builder().nutrition(3).saturationModifier(0.6f).build();
    public static final FoodProperties GENERIC_VEGETABLE = new FoodProperties.Builder().nutrition(2).saturationModifier(0.5f).build();
    public static final FoodProperties RED_BELL_PEPPER = new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).build();
    public static final FoodProperties RED_BELL_PEPPER_SLICE = new FoodProperties.Builder().nutrition(1).saturationModifier(0.2f).fast().build();
    public static final FoodProperties GENERIC_BERRY = new FoodProperties.Builder().nutrition(2).saturationModifier(0.15f).build();
    public static final FoodProperties RAW_MEAT = new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build();
    public static final FoodProperties PATTY = new FoodProperties.Builder().nutrition(4).saturationModifier(0.4f).build();

    public static final FoodProperties STUFFED_PEPPER = new FoodProperties.Builder().nutrition(10).saturationModifier(0.9f).build();
    public static final FoodProperties BURGER = new FoodProperties.Builder().nutrition(12).saturationModifier(1.1f).build();
    ///
    public static final FoodProperties NICE_BOWL = new FoodProperties.Builder().nutrition(8).saturationModifier(1f).build();
    ///
    public static final FoodProperties NICE_BOWL_REGEN = new FoodProperties.Builder().nutrition(8).saturationModifier(1f)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 600, 1), 1f).build();
    ///
    public static final FoodProperties NICE_BOWL_HASTE = new FoodProperties.Builder().nutrition(8).saturationModifier(1f)
            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 2400, 0), 1f).build();


    ///
    public static final FoodProperties AWESOME_BOWL = new FoodProperties.Builder().nutrition(12).saturationModifier(1.2f).build();
    ///
    public static final FoodProperties AWESOME_BOWL_REGEN = new FoodProperties.Builder().nutrition(12).saturationModifier(1.2f)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 600, 1), 1f).build();
    ///
    public static final FoodProperties AWESOME_BOWL_HASTE = new FoodProperties.Builder().nutrition(12).saturationModifier(1.2f)
            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 2400, 0), 1f).build();



    public static final FoodProperties FABULOUS_BOWL = new FoodProperties.Builder().nutrition(14).saturationModifier(1.6f).build();
    ///
    public static final FoodProperties FABULOUS_BOWL_REGEN = new FoodProperties.Builder().nutrition(14).saturationModifier(1.6f)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 600, 1), 1f).build();
    ///
    public static final FoodProperties FABULOUS_BOWL_HASTE = new FoodProperties.Builder().nutrition(14).saturationModifier(1.6f)
            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 2400, 0), 1f).build();
    /*
      DRINKS
     */
    public static final FoodProperties JUICE = new FoodProperties.Builder().nutrition(2).saturationModifier(0.4f).build();
    public static final FoodProperties MILK = new FoodProperties.Builder().nutrition(0).saturationModifier(0f).build();
    ///
    public static final FoodProperties BLUE_BERRY_JUICE_REGEN = new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200, 0), 1f).build();
    ///
    public static final FoodProperties GOOSE_BERRY_JUICE_STRENGTH = new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f)
            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0), 1f).build();
    ///
    public static final FoodProperties CRANBERRY_JUICE_SPEED = new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f)
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0), 1f).build();
}
