package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class BuffAuraAttackSpeedHandler {

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("veridia", "buff_aura_attack_speed");
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.1F), 0.8F);

    private record Buffed(UUID entityId, ServerLevel level, long expireAtTick) {}

    private static final List<Buffed> ACTIVE = new ArrayList<> ();

    public static void applyAttackSpeedBuff(LivingEntity entity, int durationTicks) {
        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        attackSpeed.removeModifier(MODIFIER_ID);
        attackSpeed.addTransientModifier(new AttributeModifier(MODIFIER_ID,
                AbilityConfig.BUFF_AURA_ATTACK_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        ACTIVE.removeIf(b -> b.entityId().equals(entity.getUUID()));
        ACTIVE.add(new Buffed(entity.getUUID(), serverLevel, serverLevel.getGameTime() + durationTicks));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        long now = level.getGameTime();
        Iterator<Buffed> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Buffed buffed = iterator.next();
            if (buffed.level() != level) continue;

            Entity entity = level.getEntity(buffed.entityId());
            boolean expired = now >= buffed.expireAtTick();
            boolean gone = !(entity instanceof LivingEntity living) || !living.isAlive();

            if (expired || gone) {
                if (entity instanceof LivingEntity living) {
                    AttributeInstance attackSpeed = living.getAttribute(Attributes.ATTACK_SPEED);
                    if (attackSpeed != null) attackSpeed.removeModifier(MODIFIER_ID);
                }
                iterator.remove();
                continue;
            }

            if (entity instanceof LivingEntity living && now % 5 == 0) {
                level.sendParticles(RED_DUST, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                        2, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }
}
