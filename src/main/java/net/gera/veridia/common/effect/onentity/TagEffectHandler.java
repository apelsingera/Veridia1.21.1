package net.gera.veridia.common.effect.onentity;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
@EventBusSubscriber(modid = "veridia")
public final class TagEffectHandler {
    private static final int CHECK_INTERVAL_TICKS = 20;
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide()) return;
        if (entity.tickCount % CHECK_INTERVAL_TICKS != 0) return;
        Map<Holder<MobEffect>, Integer> targetEffects = new HashMap<>();
        for (TagEffects.Tag tag : TagEffects.TAGS) {
            if (entity.getTags().contains(tag.name())) {
                for (TagEffects.Effect e : tag.effects()) {
                    targetEffects.merge(e.effect(), e.amplifier(), Math::max);
                }}}
        for (Map.Entry<Holder<MobEffect>, Integer> entry : targetEffects.entrySet()) {
            Holder<MobEffect> holder = entry.getKey();
            int targetAmplifier = entry.getValue();
            MobEffectInstance current = entity.getEffect(holder);
            boolean isCorrect = current != null
                    && current.isInfiniteDuration()
                    && current.getAmplifier() == targetAmplifier;
            if (!isCorrect) {
                entity.addEffect(new MobEffectInstance(
                        holder,
                        MobEffectInstance.INFINITE_DURATION,
                        targetAmplifier,
                        false,
                        false,
                        true
                ));}}
        for (TagEffects.Tag tag : TagEffects.TAGS) {
            for (TagEffects.Effect e : tag.effects()) {
                Holder<MobEffect> holder = e.effect();
                if (!targetEffects.containsKey(holder)) {
                    MobEffectInstance current = entity.getEffect(holder);
                    if (current != null && current.isInfiniteDuration()) {
                        entity.removeEffect(holder);
                    }}}}
    }
}