package net.gera.veridia.common.aibuff;

import net.gera.veridia.common.aibuff.handlers.ExplosiveMagmaCubeHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;

/**
 * Призыв взрывных магма-кубов: пока у визера есть цель, раз в WITHER_MAGMA_SUMMON_COOLDOWN_TICKS он ставит вокруг себя
 * кольцо из WITHER_MAGMA_SUMMON_COUNT кубов, которые сразу идут на цель. Первый призыв - сразу, как появилась цель
 * (время прошлого призыва хранится в NBT моба и не переживает смерть, но переживает перезагрузку чанка).
 */
public class WitherMagmaSummonGoal extends Goal {

    private static final String COOLDOWN_KEY = "veridia_wither_magma_cooldown";

    private final WitherSkeleton skeleton;

    public WitherMagmaSummonGoal(WitherSkeleton skeleton) {
        this.skeleton = skeleton;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (skeleton.getTarget() == null) return false;
        return isCooldownReady();
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        if (!(skeleton.level() instanceof ServerLevelAccessor serverLevel)) return;

        LivingEntity target = skeleton.getTarget();
        int count = AbilityConfig.WITHER_MAGMA_SUMMON_COUNT;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 / count) * i;
            spawnCube(serverLevel,
                    skeleton.getX() + Math.cos(angle) * AbilityConfig.WITHER_MAGMA_SUMMON_RADIUS,
                    skeleton.getZ() + Math.sin(angle) * AbilityConfig.WITHER_MAGMA_SUMMON_RADIUS,
                    target);
        }
        setCooldown();
    }

    private void spawnCube(ServerLevelAccessor serverLevel, double x, double z, LivingEntity target) {
        MagmaCube cube = new MagmaCube(EntityType.MAGMA_CUBE, skeleton.level());
        cube.moveTo(x, skeleton.getY(), z, skeleton.getYRot(), 0.0F);
        cube.finalizeSpawn(serverLevel, skeleton.level().getCurrentDifficultyAt(cube.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        // Размер ставится после finalizeSpawn: иначе он затирается случайным размером из finalizeSpawn.
        cube.setSize(AbilityConfig.WITHER_MAGMA_SUMMON_SIZE, true);

        cube.addTag(ExplosiveMagmaCubeHandler.TAG);
        if (target != null) {
            cube.setTarget(target);
        }
        skeleton.level().addFreshEntity(cube);
    }

    private boolean isCooldownReady() {
        long last = skeleton.getPersistentData().getLong(COOLDOWN_KEY);
        return skeleton.level().getGameTime() - last >= AbilityConfig.WITHER_MAGMA_SUMMON_COOLDOWN_TICKS;
    }

    private void setCooldown() {
        skeleton.getPersistentData().putLong(COOLDOWN_KEY, skeleton.level().getGameTime());
    }
}
