package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.aibuff.handlers.ExplosiveSlimeHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;

/**
 * Уровень 34 (тег "slimesummon") - призывает SLIME_SUMMON_COUNT взрывных слизней (см.
 * ExplosiveSlimeHandler - при смерти взрываются, но не вредят другим злым мобам). Разово
 * (canContinueToUse=false), перезарядка SLIME_SUMMON_COOLDOWN_TICKS - тот же паттерн, что и
 * WitherMagmaSummonGoal у визера.
 */
public class SlimeSummonGoal extends Goal {

    private static final String COOLDOWN_KEY = "veridia_slimesummon_cooldown";

    private final Zombie zombie;

    public SlimeSummonGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (zombie.getTarget() == null) return false;
        return isCooldownReady();
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        if (!(zombie.level() instanceof ServerLevelAccessor serverLevel)) return;

        LivingEntity target = zombie.getTarget();

        for (int i = 0; i < AbilityConfig.SLIME_SUMMON_COUNT; i++) {
            double angle = (Math.PI * 2 / AbilityConfig.SLIME_SUMMON_COUNT) * i;
            double x = zombie.getX() + Math.cos(angle) * AbilityConfig.SLIME_SUMMON_RADIUS;
            double z = zombie.getZ() + Math.sin(angle) * AbilityConfig.SLIME_SUMMON_RADIUS;
            double y = zombie.getY();

            Slime slime = new Slime(EntityType.SLIME, zombie.level());
            slime.moveTo(x, y, z, zombie.getYRot(), 0.0F);
            slime.finalizeSpawn(serverLevel, zombie.level().getCurrentDifficultyAt(slime.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            // Slime#finalizeSpawn сам рандомизирует размер - конфиг-размер ставим строго ПОСЛЕ,
            // иначе затирается (см. тот же комментарий в WitherMagmaSummonGoal).
            slime.setSize(1, true);
            slime.addTag(ExplosiveSlimeHandler.TAG);

            if (target != null) {
                slime.setTarget(target);
            }

            zombie.level().addFreshEntity(slime);
        }

        setCooldown();
    }

    private boolean isCooldownReady() {
        long last = zombie.getPersistentData().getLong(COOLDOWN_KEY);
        return zombie.level().getGameTime() - last >= AbilityConfig.SLIME_SUMMON_COOLDOWN_TICKS;
    }

    private void setCooldown() {
        zombie.getPersistentData().putLong(COOLDOWN_KEY, zombie.level().getGameTime());
    }
}
