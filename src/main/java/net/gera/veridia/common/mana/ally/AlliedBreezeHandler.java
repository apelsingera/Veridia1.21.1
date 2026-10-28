package net.gera.veridia.common.mana.ally;

import net.gera.veridia.Veridia;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class AlliedBreezeHandler {

    public static final String ALLY_TAG = "veridia_ally_breeze";
    private static final String EXPIRE_KEY = "veridia_ally_breeze_expire_at";
    private static final String OWNER_KEY = "veridia_ally_breeze_owner";
    private static final String TARGET_KEY = "veridia_ally_breeze_target";

    private static final int LIFETIME_TICKS = 20 * 30;      // 30 сек, фикс
    private static final double MAX_HEALTH = 20.0D;         // своё здоровье - крутить тут (ваниль 30)
    private static final double RETARGET_RADIUS = 16.0D;
    private static final double ORBIT_RADIUS = 1.2D;
    private static final int ORBIT_PERIOD_TICKS = 50;

    public static void summonAllies(ServerPlayer owner, int count) {
        ServerLevel level = owner.serverLevel();
        for (int i = 0; i < count; i++) {
            Breeze breeze = EntityType.BREEZE.create(level);
            if (breeze == null) {
                continue;
            }
            double x = owner.getX() + (owner.getRandom().nextDouble() - 0.5D) * 2.0D;
            double z = owner.getZ() + (owner.getRandom().nextDouble() - 0.5D) * 2.0D;
            breeze.moveTo(x, owner.getY(), z, owner.getYRot(), 0.0F);
            breeze.addTag(ALLY_TAG);
            breeze.setPersistenceRequired();

            AttributeInstance maxHealth = breeze.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(MAX_HEALTH);
            }
            breeze.setHealth((float) MAX_HEALTH);

            breeze.getPersistentData().putLong(EXPIRE_KEY, level.getGameTime() + LIFETIME_TICKS);
            breeze.getPersistentData().putUUID(OWNER_KEY, owner.getUUID());

            level.addFreshEntity(breeze);
        }
    }

    @SubscribeEvent
    public static void onOwnerHitsSomething(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer owner)) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == owner || !target.isAlive()) {
            return;
        }
        if (target instanceof Breeze breeze && breeze.getTags().contains(ALLY_TAG)) {
            return;
        }

        ServerLevel level = owner.serverLevel();
        AABB area = owner.getBoundingBox().inflate(RETARGET_RADIUS);
        for (Breeze ally : level.getEntitiesOfClass(Breeze.class, area,
                b -> b.getTags().contains(ALLY_TAG) && owner.getUUID().equals(ownerUuid(b)))) {
            ally.getPersistentData().putUUID(TARGET_KEY, target.getUUID());
            forceTarget(ally, target);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Breeze breeze)) {
            return;
        }
        if (breeze.level().isClientSide() || !breeze.getTags().contains(ALLY_TAG)) {
            return;
        }
        ServerLevel level = (ServerLevel) breeze.level();

        long expireAt = breeze.getPersistentData().getLong(EXPIRE_KEY);
        if (expireAt != 0L && level.getGameTime() >= expireAt) {
            breeze.discard();
            return;
        }

        LivingEntity designated = resolveDesignatedTarget(level, breeze);
        forceTarget(breeze, designated);
        spawnOrbitParticles(level, breeze);
    }

    private static void forceTarget(Breeze breeze, LivingEntity target) {
        breeze.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        breeze.getBrain().setMemory(MemoryModuleType.NEAREST_ATTACKABLE, target);
    }

    private static LivingEntity resolveDesignatedTarget(ServerLevel level, Breeze breeze) {
        if (!breeze.getPersistentData().hasUUID(TARGET_KEY)) {
            return null;
        }
        Entity entity = level.getEntity(breeze.getPersistentData().getUUID(TARGET_KEY));
        if (entity instanceof LivingEntity living && living.isAlive() && !(living instanceof Player)) {
            return living;
        }
        breeze.getPersistentData().remove(TARGET_KEY);
        return null;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (event.getSource().getEntity() instanceof Breeze breeze && breeze.getTags().contains(ALLY_TAG)) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getExplosion().getIndirectSourceEntity() instanceof Breeze breeze
                && breeze.getTags().contains(ALLY_TAG)
                && breeze.getPersistentData().hasUUID(OWNER_KEY)) {
            UUID ownerId = breeze.getPersistentData().getUUID(OWNER_KEY);
            event.getAffectedEntities().removeIf(e -> e instanceof Player player && ownerId.equals(player.getUUID()));
        }
    }

    private static UUID ownerUuid(Breeze breeze) {
        return breeze.getPersistentData().hasUUID(OWNER_KEY) ? breeze.getPersistentData().getUUID(OWNER_KEY) : null;
    }

    private static void spawnOrbitParticles(ServerLevel level, Breeze breeze) {
        double angle = (level.getGameTime() % ORBIT_PERIOD_TICKS) * (Math.PI * 2 / ORBIT_PERIOD_TICKS);
        double x = breeze.getX() + Math.cos(angle) * ORBIT_RADIUS;
        double z = breeze.getZ() + Math.sin(angle) * ORBIT_RADIUS;
        double y = breeze.getY() + breeze.getBbHeight() * 0.6D;
        level.sendParticles(ParticleTypes.SMALL_GUST, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}