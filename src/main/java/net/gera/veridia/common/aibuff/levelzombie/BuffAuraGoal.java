package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.aibuff.handlers.BuffAuraAttackSpeedHandler;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.List;

/**
 * Уровень 33 (тег "buffaura") - замах BUFF_AURA_CAST_TICKS: взлетает вверх, красные частицы,
 * страшный звук. По окончании замаха баффает все Enemy в BUFF_AURA_RADIUS: Скорость I (обычный
 * MobEffect, снимается сам) + временный +25% к скорости атаки (см. BuffAuraAttackSpeedHandler -
 * AttributeModifier с автоснятием по истечении BUFF_AURA_DURATION_TICKS, плюс красный trail частиц
 * на баффнутых мобах, пока действует). Перезарядка BUFF_AURA_COOLDOWN_TICKS. Требует хотя бы
 * одного Enemy поблизости, чтобы вообще сработать.
 */
public class BuffAuraGoal extends Goal {

    private static final String COOLDOWN_KEY = "veridia_buffaura_cooldown";
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.1F), 1.2F);

    private final Zombie zombie;
    private int castTicksLeft;

    public BuffAuraGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return isCooldownReady() && !findNearbyEnemies().isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        return castTicksLeft > 0;
    }

    @Override
    public void start() {
        castTicksLeft = AbilityConfig.BUFF_AURA_CAST_TICKS;
        if (zombie.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, zombie.getX(), zombie.getY(), zombie.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.5F);
        }
    }

    @Override
    public void tick() {
        castTicksLeft--;
        zombie.setDeltaMovement(zombie.getDeltaMovement().x, 0.15D, zombie.getDeltaMovement().z);

        if (zombie.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(RED_DUST, zombie.getX(), zombie.getY() + 1.0, zombie.getZ(),
                    6, 0.4, 0.6, 0.4, 0.0);
        }

        if (castTicksLeft <= 0) {
            resolveBuff();
            setCooldown();
        }
    }

    private void resolveBuff() {
        if (!(zombie.level() instanceof ServerLevel serverLevel)) return;

        for (LivingEntity enemy : findNearbyEnemies()) {
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, AbilityConfig.BUFF_AURA_DURATION_TICKS, 0));
            BuffAuraAttackSpeedHandler.applyAttackSpeedBuff(enemy, AbilityConfig.BUFF_AURA_DURATION_TICKS);
        }

        serverLevel.sendParticles(RED_DUST, zombie.getX(), zombie.getY() + 1.0, zombie.getZ(),
                40, AbilityConfig.BUFF_AURA_RADIUS * 0.5, 1.0, AbilityConfig.BUFF_AURA_RADIUS * 0.5, 0.05);
    }

    private List<LivingEntity> findNearbyEnemies() {
        AABB area = zombie.getBoundingBox().inflate(AbilityConfig.BUFF_AURA_RADIUS);
        return zombie.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e instanceof Enemy && e.isAlive() && e != zombie);
    }

    private boolean isCooldownReady() {
        long last = zombie.getPersistentData().getLong(COOLDOWN_KEY);
        return zombie.level().getGameTime() - last >= AbilityConfig.BUFF_AURA_COOLDOWN_TICKS;
    }

    private void setCooldown() {
        zombie.getPersistentData().putLong(COOLDOWN_KEY, zombie.level().getGameTime());
    }
}
