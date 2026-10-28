package net.gera.veridia.common.mana.veridite;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.DoubleUnaryOperator;

public class LaserUtil {
    private static final double PARTICLE_STEP = 0.2D;
    private static final float GLASS_DAMAGE_MULTIPLIER = 1.2F;
    private static final int MAX_GLASS_PIERCES = 8;
    private static final double GLASS_SKIP_EPSILON = 0.05D;

    private static final DoubleUnaryOperator NO_BONUS = d -> 0.0D;

    public static Vec3 fireOnce(ServerPlayer shooter, double length, double radius, float damage,
                                 int burnTicks, ParticleOptions particle) {
        return fireOnce((LivingEntity) shooter, length, radius, damage, burnTicks, particle);
    }

    public static Vec3 fireOnce(LivingEntity shooter, double length, double radius, float damage,
                                 int burnTicks, ParticleOptions particle) {
        return fireAlongDirection(shooter, shooter.getEyePosition(), shooter.getLookAngle().normalize(),
                length, radius, damage, burnTicks, particle, NO_BONUS);
    }

    public static Vec3 fireOnce(ServerPlayer shooter, double length, double radius, float damage,
                                 int burnTicks, ParticleOptions particle, DoubleUnaryOperator bonusByDistanceToShooter) {
        return fireAlongDirection(shooter, shooter.getEyePosition(), shooter.getLookAngle().normalize(),
                length, radius, damage, burnTicks, particle, bonusByDistanceToShooter);
    }

    public static Vec3 fireOnceAtTarget(LivingEntity shooter, LivingEntity target, double length, double radius,
                                         float damage, int burnTicks, ParticleOptions particle) {
        Vec3 start = shooter.getEyePosition();
        Vec3 direction = target.getEyePosition().subtract(start).normalize();
        return fireAlongDirection(shooter, start, direction, length, radius, damage, burnTicks, particle, NO_BONUS);
    }

    public static void sendEyeBeam(LivingEntity shooter, Vec3 end, int tier) {
        if (!(shooter.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.SMALL_FLAME, end.x, end.y, end.z, 2, 0.06D, 0.06D, 0.06D, 0.01D);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(shooter,
                new LaserBeamPayload(shooter.getId(), end.x, end.y, end.z, tier));
    }

    public static void fireEyeOfSauronBeam(LivingEntity shooter, double length, double radius, float damage,
                                           int burnTicks, int tier) {
        Vec3 end = fireOnce(shooter, length, radius, damage, burnTicks, null);
        sendEyeBeam(shooter, end, tier);
    }

    public static void fireEyeOfSauronBeam(ServerPlayer shooter, double length, double radius, float damage,
                                           int burnTicks, DoubleUnaryOperator bonusByDistanceToShooter, int tier) {
        Vec3 end = fireOnce(shooter, length, radius, damage, burnTicks, null, bonusByDistanceToShooter);
        sendEyeBeam(shooter, end, tier);
    }

    public static void fireEyeOfSauronBeamAtTarget(LivingEntity shooter, LivingEntity target, double length,
                                                   double radius, float damage, int burnTicks, int tier) {
        Vec3 end = fireOnceAtTarget(shooter, target, length, radius, damage, burnTicks, null);
        sendEyeBeam(shooter, end, tier);
    }

    private static Vec3 fireAlongDirection(LivingEntity shooter, Vec3 start, Vec3 direction, double length,
                                            double radius, float damage, int burnTicks, ParticleOptions particle,
                                            DoubleUnaryOperator bonusByDistanceToShooter) {
        Vec3 expectedEnd = start.add(direction.scale(length));
        if (!(shooter.level() instanceof ServerLevel level)) {
            return expectedEnd;
        }

        BlockHitResult blockHit = level.clip(new ClipContext(
                start, expectedEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        Vec3 actualEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : expectedEnd;

        applyEffects(level, shooter, start, actualEnd, radius, damage, burnTicks, bonusByDistanceToShooter);
        spawnParticles(level, start, actualEnd, direction, radius, particle);
        return actualEnd;
    }

    private static void applyEffects(ServerLevel level, LivingEntity shooter, Vec3 start, Vec3 end,
                                       double radius, float damage, int burnTicks,
                                       DoubleUnaryOperator bonusByDistanceToShooter) {
        AABB searchArea = new AABB(start, end).inflate(radius + 1.0D);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != shooter && entity.isAlive());

        DamageSource damageSource = shooter instanceof Player player
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(shooter);

        for (LivingEntity target : candidates) {
            Optional<Vec3> hit = target.getBoundingBox().inflate(radius).clip(start, end);
            if (hit.isPresent()) {
                if (target.getRemainingFireTicks() <= 0) {
                    float bonus = (float) Math.max(0.0D, bonusByDistanceToShooter.applyAsDouble(target.distanceTo(shooter)));
                    target.invulnerableTime = 0;
                    target.hurt(damageSource, damage + bonus);
                }
                if (burnTicks > 0) {
                    target.setRemainingFireTicks(burnTicks);
                }
            }
        }
    }

    public static void fireOnceCapped(ServerPlayer shooter, double length, double radius, float damagePerTick,
                                       int burnTicks, ParticleOptions particle,
                                       Map<UUID, Float> damageDealt, float maxTotalDamage) {
        ServerLevel level = shooter.serverLevel();
        Vec3 start = shooter.getEyePosition();
        Vec3 direction = shooter.getLookAngle().normalize();

        GlassPierceResult pierce = traceThroughGlass(level, shooter, start, direction, length);
        float effectiveDamage = pierce.passedGlass() ? damagePerTick * GLASS_DAMAGE_MULTIPLIER : damagePerTick;

        applyCappedEffects(level, shooter, start, pierce.end(), radius, effectiveDamage, burnTicks, damageDealt, maxTotalDamage);
        spawnParticles(level, start, pierce.end(), direction, radius, particle);
    }

    private record GlassPierceResult(Vec3 end, boolean passedGlass) {
    }

    private static GlassPierceResult traceThroughGlass(ServerLevel level, ServerPlayer shooter,
                                                         Vec3 start, Vec3 direction, double length) {
        Vec3 segmentStart = start;
        double remaining = length;
        boolean passedGlass = false;

        for (int i = 0; i < MAX_GLASS_PIERCES && remaining > 0; i++) {
            Vec3 segmentEnd = segmentStart.add(direction.scale(remaining));
            BlockHitResult hit = level.clip(new ClipContext(
                    segmentStart, segmentEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));

            if (hit.getType() == HitResult.Type.MISS) {
                return new GlassPierceResult(segmentEnd, passedGlass);
            }
            if (!isAmplifyingGlass(level.getBlockState(hit.getBlockPos()))) {
                return new GlassPierceResult(hit.getLocation(), passedGlass);
            }

            passedGlass = true;
            double traveled = segmentStart.distanceTo(hit.getLocation()) + GLASS_SKIP_EPSILON;
            remaining -= traveled;
            segmentStart = hit.getLocation().add(direction.scale(GLASS_SKIP_EPSILON));
        }
        return new GlassPierceResult(segmentStart, passedGlass);
    }

    private static boolean isAmplifyingGlass(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.TINTED_GLASS) return false;
        return block == Blocks.GLASS
                || block instanceof StainedGlassBlock
                || block == Blocks.GLASS_PANE
                || block instanceof StainedGlassPaneBlock;
    }

    private static void applyCappedEffects(ServerLevel level, ServerPlayer shooter, Vec3 start, Vec3 end,
                                            double radius, float damagePerTick, int burnTicks,
                                            Map<UUID, Float> damageDealt, float maxTotalDamage) {
        AABB searchArea = new AABB(start, end).inflate(radius + 1.0D);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != shooter && entity.isAlive());

        for (LivingEntity target : candidates) {
            Optional<Vec3> hit = target.getBoundingBox().inflate(radius).clip(start, end);
            if (hit.isEmpty()) continue;

            float dealtSoFar = damageDealt.getOrDefault(target.getUUID(), 0.0F);
            if (dealtSoFar < maxTotalDamage) {
                float toApply = Math.min(damagePerTick, maxTotalDamage - dealtSoFar);
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().playerAttack(shooter), toApply);
                damageDealt.put(target.getUUID(), dealtSoFar + toApply);
            }
            if (burnTicks > 0) {
                target.setRemainingFireTicks(burnTicks);
            }
        }
    }

    private static void spawnParticles(ServerLevel level, Vec3 start, Vec3 end, Vec3 direction,
                                        double radius, ParticleOptions particle) {
        // null - вызывающий сам рисует луч (см. LaserUtil.sendEyeBeam/EyeOfSauronBeamRenderer -
        // свой пакет+рендер вместо ванильных партиклов). level.sendParticles(null, ...) не
        // падает сразу - ClientboundLevelParticlesPacket долетает до кодирования и роняет
        // соединение с "Failed to Encode packet 'clientbound/minecraft:level_particles'"
        // (и роняет ЗАНОВО при каждом переподключении, пока держится FIRING_UNTIL_KEY).
        if (particle == null) return;

        double length = start.distanceTo(end);
        int steps = (int) Math.ceil(length / PARTICLE_STEP);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(direction.scale(Math.min(i * PARTICLE_STEP, length)));
            level.sendParticles(particle,
                    point.x, point.y, point.z,
                    1, radius / 2.0, radius / 2.0, radius / 2.0, 0.0D);
        }
    }
}
