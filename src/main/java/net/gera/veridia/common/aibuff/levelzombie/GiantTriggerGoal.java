package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.aibuff.handlers.GiantZombieHandler;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Уровень 35 (тег "gianttrigger") - раз в GIANT_TRIGGER_COOLDOWN_TICKS превращает случайного
 * ДРУГОГО зомби в радиусе GIANT_TRIGGER_RADIUS в "великана" на GIANT_DURATION_TICKS. Сам эффект
 * (масштаб/скорость/урон, ломание листвы, вытаптывание травы) применяет GiantZombieHandler по
 * persistent-data ключу, который тут только выставляется.
 */
public class GiantTriggerGoal extends Goal {

    private static final String COOLDOWN_KEY = "veridia_gianttrigger_cooldown";

    private final Zombie zombie;

    public GiantTriggerGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (zombie.getTarget() == null) return false;
        if (!isCooldownReady()) return false;
        return !findCandidates().isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        List<Zombie> candidates = findCandidates();
        if (candidates.isEmpty()) return;

        Zombie chosen = candidates.get(zombie.getRandom().nextInt(candidates.size()));
        chosen.getPersistentData().putLong(GiantZombieHandler.GIANT_UNTIL_KEY,
                zombie.level().getGameTime() + AbilityConfig.GIANT_DURATION_TICKS);

        setCooldown();
    }

    private List<Zombie> findCandidates() {
        AABB area = zombie.getBoundingBox().inflate(AbilityConfig.GIANT_TRIGGER_RADIUS);
        return zombie.level().getEntitiesOfClass(Zombie.class, area,
                other -> other != zombie && other.isAlive()
                        && !other.getPersistentData().getBoolean(GiantZombieHandler.GIANT_APPLIED_KEY));
    }

    private boolean isCooldownReady() {
        long last = zombie.getPersistentData().getLong(COOLDOWN_KEY);
        return zombie.level().getGameTime() - last >= AbilityConfig.GIANT_TRIGGER_COOLDOWN_TICKS;
    }

    private void setCooldown() {
        zombie.getPersistentData().putLong(COOLDOWN_KEY, zombie.level().getGameTime());
    }
}
