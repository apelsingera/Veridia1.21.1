package net.gera.veridia.common.entity.rustynail;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.gera.veridia.common.effect.ModEffects;
import net.gera.veridia.common.entity.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class RustyNailEntity extends AbstractArrow {
    private static final float HIT_DAMAGE = 6.0F;
    private static final float PLAGUE_CHANCE = 0.10F;
    private static final int PLAGUE_DURATION_TICKS = 20 * 60 * 3;
    private static final int GROUND_LIFETIME_TICKS = 20 * 30;
    private static final int MAX_FLIGHT_TICKS = 20 * 12;
    private static final EntityDataAccessor<Boolean> DATA_LANDED =
            SynchedEntityData.defineId(RustyNailEntity.class, EntityDataSerializers.BOOLEAN);
    private final IntOpenHashSet hitEntities = new IntOpenHashSet();
    private int groundTicks = -1;
    public RustyNailEntity(EntityType<? extends RustyNailEntity> type, Level level) {
        super(type, level);
    }
    public RustyNailEntity(Level level, LivingEntity owner) {
        super(ModEntities.RUSTY_NAIL.get(), owner, level, new ItemStack(Items.IRON_NUGGET), null);
        this.setNoGravity(false);
        this.setSilent(true);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LANDED, false);
    }
    public boolean isLanded() {
        return this.entityData.get(DATA_LANDED);
    }
    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.IRON_NUGGET);
    }
    @Override
    protected double getDefaultGravity() {
        return 0.08D;
    }
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.inGround) {
            groundTicks = groundTicks < 0 ? 0 : groundTicks + 1;
            if (groundTicks >= GROUND_LIFETIME_TICKS) {
                this.discard();
            }
        } else {
            if (this.entityData.get(DATA_LANDED)) {
                this.entityData.set(DATA_LANDED, false);
            }
            if (this.tickCount > MAX_FLIGHT_TICKS) {
                this.discard();
            }
        }
    }
    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity)
                && entity != this.getOwner()
                && !this.hitEntities.contains(entity.getId());
    }
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        if (!(entity instanceof LivingEntity living) || entity == this.getOwner()) {
            return;
        }
        this.hitEntities.add(entity.getId());
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        living.hurt(this.damageSources().thrown(this, this.getOwner()), HIT_DAMAGE);
        if (this.random.nextFloat() < PLAGUE_CHANCE) {
            living.addEffect(new MobEffectInstance(ModEffects.PLAGUE, PLAGUE_DURATION_TICKS, 0));
        }
    }
    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.groundTicks = 0;
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.entityData.set(DATA_LANDED, true);
    }
}