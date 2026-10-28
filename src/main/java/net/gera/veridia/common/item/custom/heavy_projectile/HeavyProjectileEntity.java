package net.gera.veridia.common.item.custom.heavy_projectile;

import net.gera.veridia.common.entity.ModEntities;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;

public class HeavyProjectileEntity extends AbstractArrow {
    private float rotation;
    public Vec2 groundedOffset;
    private float damage = 4F;

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public HeavyProjectileEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public HeavyProjectileEntity(LivingEntity shooter, Level level) {
        super(ModEntities.HEAVY_PROJECTILE.get(), shooter, level, new ItemStack(ModItems.HEAVY_PROJECTILE.get()), null);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.HEAVY_PROJECTILE.get());
    }
    public float getRenderingRotation() {
        rotation += 0.5f;
        if(rotation >= 360) {
            rotation = 0;
        }
        return rotation;
    }

    public boolean isGrounded() {
        return inGround;
    }

    /**
     * Если бросивший - "злой" моб (Enemy, например зомби с heavy_core на уровне 11 - см.
     * levelzombie.HeavyCoreThrowGoal), снаряд не считает попаданием других "злых" мобов на пути -
     * пролетает сквозь союзников насквозь и летит дальше к настоящей цели, вместо того чтобы
     * взрываться о первого же соседнего зомби. На игрока/жителя (владелец villager) не влияет.
     */
    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity)) return false;
        Entity owner = getOwner();
        return !(owner instanceof Enemy) || !(entity instanceof Enemy);
    }

    /**
     * Не вызываем super.onHitEntity(AbstractArrow) - ванильная реализация сама наносит урон
     * стрелой и, что важнее, при успешном hurt() дёргает livingEntity.setArrowCount()+1, из-за
     * чего в игрока визуально втыкается обычная ванильная стрела (даже при кастомном рендере
     * снаряда). Весь урон/эффект/удаление реализованы здесь вручную, без этих побочных эффектов.
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();

        // Creative/spectator игроков всё равно нельзя ранить - раньше эффект замедления всё
        // равно применялся, из-за чего игрока "стан"-ило в творческом режиме без всякого урона.
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            if (!this.level().isClientSide) this.discard();
            return;
        }

        entity.hurt(this.damageSources().thrown(this, this.getOwner()), this.damage);
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    60,
                    255,
                    false,
                    false
            ));
        }

        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if(result.getDirection() == Direction.SOUTH) {
            groundedOffset = new Vec2(215f,180f);
        }
        if(result.getDirection() == Direction.NORTH) {
            groundedOffset = new Vec2(215f, 0f);
        }
        if(result.getDirection() == Direction.EAST) {
            groundedOffset = new Vec2(215f,-90f);
        }
        if(result.getDirection() == Direction.WEST) {
            groundedOffset = new Vec2(215f,90f);
        }

        if(result.getDirection() == Direction.DOWN) {
            groundedOffset = new Vec2(115f,180f);
        }
        if(result.getDirection() == Direction.UP) {
            groundedOffset = new Vec2(285f,180f);
        }
    }
}