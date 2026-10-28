package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class HideBehindAlliesGoal extends Goal {

    private static final String HIDE_UNTIL_KEY = "veridia_hide_until";

    private final Zombie zombie;
    private Zombie ally;

    public HideBehindAlliesGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (zombie.level().getGameTime() >= zombie.getPersistentData().getLong(HIDE_UNTIL_KEY)) return false;
        if (zombie.getTarget() == null) return false;
        return findAlly() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        ally = findAlly();
    }

    @Override
    public void stop() {
        ally = null;
        zombie.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = zombie.getTarget();
        if (target == null || ally == null || !ally.isAlive()) return;

        Vec3 away = ally.position().subtract(target.position());
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(zombie.getRandom().nextDouble() - 0.5, 0.0, zombie.getRandom().nextDouble() - 0.5);
        }
        Vec3 destination = ally.position().add(away.normalize().scale(AbilityConfig.HIDE_BEHIND_DISTANCE));

        zombie.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0D);
        zombie.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private Zombie findAlly() {
        AABB area = zombie.getBoundingBox().inflate(AbilityConfig.HIDE_SEARCH_RADIUS);
        List<Zombie> nearby = zombie.level().getEntitiesOfClass(Zombie.class, area,
                other -> other != zombie && other.isAlive());
        return nearby.stream()
                .min((a, b) -> Double.compare(zombie.distanceToSqr(a), zombie.distanceToSqr(b)))
                .orElse(null);
    }

    static void startHiding(Zombie zombie) {
        zombie.getPersistentData().putLong(HIDE_UNTIL_KEY,
                zombie.level().getGameTime() + AbilityConfig.HIDE_DURATION_TICKS);
    }
}
