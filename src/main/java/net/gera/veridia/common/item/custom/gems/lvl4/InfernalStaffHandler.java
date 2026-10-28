package net.gera.veridia.common.item.custom.gems.lvl4;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.item.custom.gems.InfernalStaffFx;
import net.gera.veridia.common.mana.veridite.InfernalStaffBeamPayload;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class InfernalStaffHandler {

    private static final int BEAM_COUNT = 4;
    private static final int TIER = 4;
    private static final double BEAM_LENGTH = 5.0D;
    private static final double BEAM_RADIUS = 0.3D;
    private static final float BASE_DAMAGE = 3.5F;
    private static final double PARTICLE_STEP = 0.4D;
    private static final double HUB_SKIP = 1.0D;
    // RPS 1 -> оборот за 20 тиков.
    private static final double RADIANS_PER_TICK = (Math.PI * 2.0D) / 20.0D;

    private static final int HITS_PER_RAMP = 8;
    private static final float RAMP_STEP = 0.5F;
    private static final float MAX_RAMP_BONUS = 2.0F;
    private static final int HIT_COUNT_CAP = HITS_PER_RAMP * (int) (MAX_RAMP_BONUS / RAMP_STEP);

    private static final DustParticleOptions FLAME_PARTICLE =
            new DustParticleOptions(Vec3.fromRGB24(0x8B0000).toVector3f(), 1.2F);

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        long now = player.level().getGameTime();
        long activeUntil = player.getPersistentData().getLong(InfernalStaffItem.ACTIVE_UNTIL_KEY);
        if (now >= activeUntil) return;

        if (player.isUnderWater()) return;

        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0.0D, 0.12D, 0.0D); // из-под ног
        double baseAngle = now * RADIANS_PER_TICK;

        int hitCount = player.getPersistentData().getInt(InfernalStaffItem.HIT_COUNT_KEY);
        float damage = BASE_DAMAGE + Math.min(MAX_RAMP_BONUS, ((float) hitCount / HITS_PER_RAMP) * RAMP_STEP);

        int hitsThisTick = 0;
        for (int i = 0; i < BEAM_COUNT; i++) {
            double angle = baseAngle + (2.0D * Math.PI / BEAM_COUNT) * i;
            Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            Vec3 end = center.add(direction.scale(BEAM_LENGTH));

            hitsThisTick += damageAlongBeam(level, player, center, end, damage);
        }

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new InfernalStaffBeamPayload(
                player.getId(), BEAM_COUNT, (float) BEAM_LENGTH, (float) RADIANS_PER_TICK, TIER));
        InfernalStaffFx.spawnHubParticles(level, center);

        if (hitsThisTick > 0) {
            player.getPersistentData().putInt(InfernalStaffItem.HIT_COUNT_KEY,
                    Math.min(HIT_COUNT_CAP, hitCount + hitsThisTick));
        }
    }

    private static int damageAlongBeam(ServerLevel level, ServerPlayer shooter, Vec3 start, Vec3 end, float damage) {
        AABB searchArea = new AABB(start, end).inflate(BEAM_RADIUS + 1.0D);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != shooter && entity.isAlive());

        int hits = 0;
        for (LivingEntity target : candidates) {
            Optional<Vec3> hit = target.getBoundingBox().inflate(BEAM_RADIUS).clip(start, end);
            if (hit.isPresent() && target.hurt(level.damageSources().playerAttack(shooter), damage)) {
                hits++;
            }
        }
        return hits;
    }

    private static void spawnBeamParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start).normalize();
        double length = start.distanceTo(end);
        // Разреженная линия от HUB_SKIP до кончика: плотный "хаб" у игрока не рисуем, иначе за
        // оборот RPS след частиц сливается в сплошной красный диск и вращение не видно.
        for (double d = HUB_SKIP; d <= length; d += PARTICLE_STEP) {
            Vec3 point = start.add(direction.scale(d));
            level.sendParticles(FLAME_PARTICLE, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}