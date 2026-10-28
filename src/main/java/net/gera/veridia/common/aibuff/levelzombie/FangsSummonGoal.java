package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;

/**
 * Уровень 28 (тег "fangs") - "крокодилы, как у вызывателя" реализованы через ванильные EvokerFangs
 * (клыки заклинателя) кольцом вокруг себя - отдельного моба "крокодил" в модпаке нет (ни в ванили,
 * ни в уже используемых Spartan Weaponry/Graveyard), поэтому по договорённости используем готовый
 * ванильный эффект с тем же "укус из-под земли". Разово (canContinueToUse=false), перезарядка
 * FANGS_SUMMON_COOLDOWN_TICKS - тот же паттерн, что и WitherMagmaSummonGoal у визера.
 */
public class FangsSummonGoal extends Goal {

    private static final String COOLDOWN_KEY = "veridia_fangs_cooldown";

    private final Zombie zombie;

    public FangsSummonGoal(Zombie zombie) {
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
        if (!(zombie.level() instanceof ServerLevelAccessor)) return;

        for (int i = 0; i < AbilityConfig.FANGS_SUMMON_COUNT; i++) {
            double angle = (Math.PI * 2 / AbilityConfig.FANGS_SUMMON_COUNT) * i;
            double x = zombie.getX() + Math.cos(angle) * AbilityConfig.FANGS_SUMMON_RADIUS;
            double z = zombie.getZ() + Math.sin(angle) * AbilityConfig.FANGS_SUMMON_RADIUS;
            double y = zombie.getY();

            EvokerFangs fangs = new EvokerFangs(zombie.level(), x, y, z, zombie.getYRot(), i * 2, zombie);
            zombie.level().addFreshEntity(fangs);
        }

        setCooldown();
    }

    private boolean isCooldownReady() {
        long last = zombie.getPersistentData().getLong(COOLDOWN_KEY);
        return zombie.level().getGameTime() - last >= AbilityConfig.FANGS_SUMMON_COOLDOWN_TICKS;
    }

    private void setCooldown() {
        zombie.getPersistentData().putLong(COOLDOWN_KEY, zombie.level().getGameTime());
    }
}
