package net.gera.veridia.common.mana.ally;

import net.gera.veridia.Veridia;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class AlliedDrownedHandler {

    public static final String ALLY_TAG = "veridia_ally_drowned";
    private static final String EXPIRE_KEY = "veridia_ally_expire_at";
    private static final String OWNER_KEY = "veridia_ally_owner";
    private static final String TARGET_KEY = "veridia_ally_target";
    private static final double RETARGET_RADIUS = 12.0D;
    private static final int BASE_LIFETIME_TICKS = 20 * 30;
    private static final double ORBIT_RADIUS = 0.9D;
    private static final int ORBIT_PERIOD_TICKS = 40;
    private static final double MAX_HEALTH = 25.0D;
    private static final double LAUNCH_UP_VELOCITY = 0.75D;

    public static void summonAlly(ServerPlayer owner, double lifetimeMultiplier) {
        summonAlly(owner, lifetimeMultiplier, false, false);
    }

    public static void summonAlly(ServerPlayer owner, double lifetimeMultiplier,
                                  boolean withTrident, boolean withDiamondChestplate) {
        ServerLevel level = owner.serverLevel();
        Drowned drowned = EntityType.DROWNED.create(level);
        if (drowned == null) return;

        drowned.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
        drowned.addTag(ALLY_TAG);
        drowned.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 2, false, false));

        AttributeInstance maxHealthAttribute = drowned.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(MAX_HEALTH);
        }
        drowned.setHealth((float) MAX_HEALTH);

        if (withTrident) {
            drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
            drowned.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
        if (withDiamondChestplate) {
            drowned.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            drowned.setDropChance(EquipmentSlot.CHEST, 0.0F);
        }

        long expireAt = level.getGameTime() + (long) (BASE_LIFETIME_TICKS * lifetimeMultiplier);
        drowned.getPersistentData().putLong(EXPIRE_KEY, expireAt);
        drowned.getPersistentData().putUUID(OWNER_KEY, owner.getUUID());

        level.addFreshEntity(drowned);
    }

    @SubscribeEvent
    public static void onOwnerHitsSomething(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer owner)) return;

        LivingEntity target = event.getEntity();
        if (target == owner || !target.isAlive()) return;
        if (target instanceof Drowned targetDrowned && targetDrowned.getTags().contains(ALLY_TAG)) return;

        ServerLevel level = owner.serverLevel();
        AABB area = owner.getBoundingBox().inflate(RETARGET_RADIUS);
        List<Drowned> allies = level.getEntitiesOfClass(Drowned.class, area,
                mob -> mob.getTags().contains(ALLY_TAG)
                        && owner.getUUID().equals(mob.getPersistentData().getUUID(OWNER_KEY)));

        for (Drowned ally : allies) {
            ally.getPersistentData().putUUID(TARGET_KEY, target.getUUID());
            ally.setTarget(target);
        }
    }

    @SubscribeEvent
    public static void onAllyHitsSomething(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof Drowned drowned)) return;
        if (!drowned.getTags().contains(ALLY_TAG)) return;

        LivingEntity victim = event.getEntity();
        var velocity = victim.getDeltaMovement();
        victim.setDeltaMovement(velocity.x, Math.max(velocity.y, 0.0D) + LAUNCH_UP_VELOCITY, velocity.z);
        victim.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Drowned drowned)) return;
        if (drowned.level().isClientSide()) return;
        if (!drowned.getTags().contains(ALLY_TAG)) return;

        ServerLevel level = (ServerLevel) drowned.level();

        long expireAt = drowned.getPersistentData().getLong(EXPIRE_KEY);
        if (expireAt != 0L && level.getGameTime() >= expireAt) {
            drowned.discard();
            return;
        }

        LivingEntity designated = resolveDesignatedTarget(level, drowned);
        drowned.setTarget(designated);
        spawnOrbitParticles(level, drowned);
    }

    private static LivingEntity resolveDesignatedTarget(ServerLevel level, Drowned drowned) {
        if (!drowned.getPersistentData().hasUUID(TARGET_KEY)) return null;

        UUID targetId = drowned.getPersistentData().getUUID(TARGET_KEY);
        Entity entity = level.getEntity(targetId);
        if (entity instanceof LivingEntity living && living.isAlive()) {
            return living;
        }

        drowned.getPersistentData().remove(TARGET_KEY);
        return null;
    }

    private static void spawnOrbitParticles(ServerLevel level, Drowned drowned) {
        double angle = (level.getGameTime() % ORBIT_PERIOD_TICKS) * (Math.PI * 2 / ORBIT_PERIOD_TICKS);
        double bobbing = drowned.getBbHeight() * 0.5 + 0.2 * Math.sin(angle * 2);

        for (double offset = 0; offset < Math.PI * 2; offset += Math.PI) {
            double a = angle + offset;
            double x = drowned.getX() + Math.cos(a) * ORBIT_RADIUS;
            double z = drowned.getZ() + Math.sin(a) * ORBIT_RADIUS;
            double y = drowned.getY() + bobbing;
            level.sendParticles(ParticleTypes.NAUTILUS, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getSource().getEntity() instanceof Drowned drowned && drowned.getTags().contains(ALLY_TAG)) {
            event.setCanceled(true);
        }
    }
}
