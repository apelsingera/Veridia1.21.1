package net.gera.veridia.common.aibuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;

public class PullAttackGoal extends Goal {

    private static final String NBT_KEY = "zombieabilities_pull_cooldown";

    private final Mob mob;
    private int warmupTicksLeft;

    public PullAttackGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!mob.getTags().contains("hit")) return false;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (mob.distanceToSqr(target) > AbilityConfig.PULL_RADIUS * AbilityConfig.PULL_RADIUS) return false;
        return isCooldownReady();
    }

    @Override
    public boolean canContinueToUse() {
        return warmupTicksLeft > 0 && mob.isAlive();
    }

    @Override
    public void start() {
        warmupTicksLeft = AbilityConfig.PULL_WARMUP_TICKS;
    }

    @Override
    public void stop() {
        warmupTicksLeft = 0;
    }

    @Override
    public void tick() {
        if (warmupTicksLeft <= 0) return;

        spawnWarningParticles();
        warmupTicksLeft--;

        if (warmupTicksLeft == 0) {
            performPullAttack();
            setCooldown();
        }
    }

    private boolean isCooldownReady() {
        long last = mob.getPersistentData().getLong(NBT_KEY);
        return mob.level().getGameTime() - last >= AbilityConfig.PULL_COOLDOWN_TICKS;
    }

    private void setCooldown() {
        mob.getPersistentData().putLong(NBT_KEY, mob.level().getGameTime());
    }

    /**
     * Кольцо частиц по границе радиуса атаки (показывает игрокам зону поражения)
     * + частицы над головой моба как явный сигнал "готовится удар".
     */
    private void spawnWarningParticles() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        double radius = AbilityConfig.PULL_RADIUS;
        Vec3 center = mob.position();

        int points = (int) (radius * 6);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    x, center.y + 0.1, z,
                    1, 0, 0, 0, 0);
        }

        serverLevel.sendParticles(ParticleTypes.CRIT,
                center.x, mob.getEyeY() + 0.3, center.z,
                3, 0.2, 0.1, 0.2, 0.01);
    }

    private void performPullAttack() {
        Level level = mob.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        mob.swing(InteractionHand.MAIN_HAND);

        AABB area = mob.getBoundingBox().inflate(AbilityConfig.PULL_RADIUS);
        List<Player> players = level.getEntitiesOfClass(Player.class, area,
                p -> p.isAlive() && !p.isSpectator()
                        && p.distanceToSqr(mob) <= AbilityConfig.PULL_RADIUS * AbilityConfig.PULL_RADIUS);

        double centerY = mob.getY() + mob.getBbHeight() * 0.5;

        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                mob.getX(), centerY, mob.getZ(),
                1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.POOF,
                mob.getX(), centerY, mob.getZ(),
                12, AbilityConfig.PULL_RADIUS * 0.4, 0.3, AbilityConfig.PULL_RADIUS * 0.4, 0.02);

        for (Player player : players) {
            player.hurt(mob.damageSources().mobAttack(mob), AbilityConfig.PULL_DAMAGE);

            Vec3 pull = mob.position().subtract(player.position());
            if (pull.lengthSqr() > 1.0E-4) {
                Vec3 scaled = pull.normalize().scale(AbilityConfig.PULL_STRENGTH);
                player.setDeltaMovement(player.getDeltaMovement().add(scaled.x, Math.max(scaled.y, 0.25), scaled.z));
                player.hurtMarked = true;

                spawnPullTrailParticles(serverLevel,
                        player.position().add(0, player.getBbHeight() * 0.5, 0),
                        new Vec3(mob.getX(), centerY, mob.getZ()));
            }
        }

        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            breakNearbyBlocks(level, mob.blockPosition());
        }
    }

    /**
     * Частицы вдоль линии от игрока к мобу - визуализирует сам эффект притяжения.
     */
    private void spawnPullTrailParticles(ServerLevel level, Vec3 from, Vec3 to) {
        int steps = 8;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = from.x + (to.x - from.x) * t;
            double y = from.y + (to.y - from.y) * t;
            double z = from.z + (to.z - from.z) * t;
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    private void breakNearbyBlocks(Level level, @NotNull BlockPos center) {
        int r = (int) Math.ceil(AbilityConfig.PULL_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
            if (pos.distSqr(center) > AbilityConfig.PULL_RADIUS * AbilityConfig.PULL_RADIUS) continue;
            var state = level.getBlockState(pos);
            if (AbilityConfig.isPullBreakable(state)) {
                level.destroyBlock(pos, true, mob);
            }
        }
    }
}