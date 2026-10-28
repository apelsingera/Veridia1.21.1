package net.gera.veridia.common.aibuff.villager.warrior;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Лучник: "стрелять из лука и отбегать если враг близко".
 * <p>
 * Ванильную RangedBowAttackGoal взять нельзя - она требует Monster & RangedAttackMob, а житель
 * ни тем, ни другим не является, поэтому цикл "натянул - выстрелил - перезарядка" собран здесь
 * руками. Дистанции три: дальше BOW_ATTACK_RADIUS - подходим, ближе BOW_RETREAT_RADIUS - отходим,
 * между ними стоим и стреляем. Натяжение (startUsingItem) идёт параллельно движению, как у
 * скелета, - отступление не сбрасывает прицел.
 * <p>
 * Стрелы бесконечные: Mob.getProjectile на пустой инвентарь возвращает обычную стрелу (та же
 * механика, что у скелетов), так что воину не нужен колчан.
 */
public class WarriorBowAttackGoal extends Goal {

    private final Villager villager;

    private int drawTicks;
    private int cooldownTicks;

    public WarriorBowAttackGoal(Villager villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!WarriorVillager.usesBow(villager)) return false;
        LivingEntity target = villager.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        villager.setAggressive(true);
    }

    @Override
    public void stop() {
        villager.setAggressive(false);
        villager.stopUsingItem();
        villager.getNavigation().stop();
        drawTicks = 0;
        cooldownTicks = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = villager.getTarget();
        if (target == null) return;

        villager.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distSqr = villager.distanceToSqr(target);
        boolean canSee = villager.getSensing().hasLineOfSight(target);
        double attackRadius = WarriorVillagerConfig.BOW_ATTACK_RADIUS;
        double retreatRadius = WarriorVillagerConfig.BOW_RETREAT_RADIUS;

        if (!canSee || distSqr > attackRadius * attackRadius) {
            villager.getNavigation().moveTo(target, WarriorVillagerConfig.MOVE_SPEED);
        } else if (distSqr < retreatRadius * retreatRadius) {
            retreatFrom(target);
        } else {
            villager.getNavigation().stop();
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        // Натягиваем только когда цель видно - иначе воин "залипал" бы с полным натяжением за стеной.
        if (!canSee) {
            resetDraw();
            return;
        }

        if (!villager.isUsingItem()) {
            villager.startUsingItem(InteractionHand.MAIN_HAND);
        }
        drawTicks++;

        if (drawTicks >= WarriorVillagerConfig.BOW_DRAW_TICKS && distSqr <= attackRadius * attackRadius) {
            float distanceFactor = Mth.clamp(Mth.sqrt((float) distSqr) / (float) attackRadius, 0.1F, 1.0F);
            shoot(target, distanceFactor);
            resetDraw();
            cooldownTicks = WarriorVillagerConfig.BOW_COOLDOWN_TICKS;
        }
    }

    private void resetDraw() {
        villager.stopUsingItem();
        drawTicks = 0;
    }

    private void retreatFrom(LivingEntity target) {
        Vec3 away = villager.position().subtract(target.position());
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(villager.getRandom().nextDouble() - 0.5, 0.0, villager.getRandom().nextDouble() - 0.5);
        }
        Vec3 destination = villager.position().add(away.normalize().scale(WarriorVillagerConfig.BOW_RETREAT_DISTANCE));
        villager.getNavigation().moveTo(destination.x, destination.y, destination.z, WarriorVillagerConfig.MOVE_SPEED);
    }

    private void shoot(LivingEntity target, float distanceFactor) {
        ItemStack bow = villager.getMainHandItem();
        if (!bow.is(Items.BOW)) return;

        // Житель не Monster: getProjectile на пустой инвентарь даёт пустой стек, и стрела с ним не сохраняется
        // в мир ("Cannot encode empty ItemStack" при каждом сохранении). Подобрать её всё равно нельзя -
        // стрелок не игрок
        ItemStack ammo = villager.getProjectile(bow);
        if (ammo.isEmpty()) {
            ammo = new ItemStack(Items.ARROW);
        }
        AbstractArrow arrow = ProjectileUtil.getMobArrow(villager, ammo, distanceFactor, bow);

        double dx = target.getX() - villager.getX();
        double dy = target.getY(0.3333D) - arrow.getY();
        double dz = target.getZ() - villager.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + horizontalDist * 0.2D, dz,
                WarriorVillagerConfig.BOW_VELOCITY, WarriorVillagerConfig.BOW_INACCURACY);
        villager.playSound(SoundEvents.ARROW_SHOOT, 1.0F,
                1.0F / (villager.getRandom().nextFloat() * 0.4F + 0.8F));
        villager.level().addFreshEntity(arrow);
        villager.swing(InteractionHand.MAIN_HAND);
    }
}