package net.gera.veridia.common.aibuff.villager.angry;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.item.custom.heavy_projectile.HeavyProjectileEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;

import java.util.EnumSet;

/**
 * Пока житель злой (AngryVillager.isAngry), постоянно идёт к обидчику (игрок или враждебный моб,
 * не останавливается на VILLAGER_PROJECTILE_ATTACK_RADIUS - иначе он бы вечно стоял на месте и
 * никогда не доходил до дистанции ближнего боя) и кидает heavy_projectile с перезарядкой
 * VILLAGER_PROJECTILE_COOLDOWN_TICKS, но только пока видит цель (иначе бросок пропускается, а не
 * копится). На ближней дистанции уступает VillagerMeleeAttackGoal - см. гейт по дистанции там же.
 */
public class VillagerProjectileThrowGoal extends Goal {

    private final Villager villager;

    public VillagerProjectileThrowGoal(Villager villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return AngryVillager.isAngry(villager) && villager.getTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = villager.getTarget();
        if (target == null) return;

        villager.getLookControl().setLookAt(target, 30.0F, 30.0F);
        villager.getNavigation().moveTo(target, AbilityConfig.VILLAGER_MOVE_SPEED);

        double distSqr = villager.distanceToSqr(target);
        boolean inRange = distSqr <= AbilityConfig.VILLAGER_PROJECTILE_ATTACK_RADIUS * AbilityConfig.VILLAGER_PROJECTILE_ATTACK_RADIUS;

        if (!inRange || !villager.getSensing().hasLineOfSight(target)) return;
        if (!AngryVillager.isProjectileCooldownReady(villager)) return;

        throwProjectile(target);
        AngryVillager.setProjectileCooldown(villager);
    }

    private void throwProjectile(LivingEntity target) {
        HeavyProjectileEntity projectile = new HeavyProjectileEntity(villager, villager.level());
        projectile.setDamage(AbilityConfig.VILLAGER_PROJECTILE_DAMAGE);

        double dx = target.getX() - villager.getX();
        double dy = target.getEyeY() - 1.1F - projectile.getY();
        double dz = target.getZ() - villager.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz) * 0.2;

        projectile.shoot(dx, dy + horizontalDist, dz, 1.5F, 12.0F);
        villager.level().addFreshEntity(projectile);
        villager.swing(InteractionHand.MAIN_HAND);
    }
}
