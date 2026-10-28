package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Взрывной слизень (уровень 34, тег "slimesummon" - см. SlimeSummonGoal). Тег вместо жёсткой
 * привязки к мобу, как и ExplosiveMagmaCubeHandler у визера. НЕ настоящий level.explode() (тот
 * бьёт вообще всех подряд и ломает блоки) - урон наносится вручную, пропуская Enemy (другие злые
 * мобы не задеваются), блоки не трогаются.
 */
public class ExplosiveSlimeHandler {

    public static final String TAG = "explosive_slime";

    private record PendingBlast(ServerLevel level, Vec3 pos, long explodeAtTick) {}

    private static final List<PendingBlast> PENDING = new ArrayList<>();

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Slime slime)) return;
        if (slime.level().isClientSide()) return;
        if (!slime.getTags().contains(TAG)) return;
        if (slime.tickCount % 5 != 0) return;

        if (slime.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, slime.getX(), slime.getY() + slime.getBbHeight() * 0.5, slime.getZ(),
                    2, 0.2, 0.2, 0.2, 0.0);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Slime slime)) return;
        if (!slime.getTags().contains(TAG)) return;
        if (!(slime.level() instanceof ServerLevel level)) return;

        PENDING.add(new PendingBlast(level, slime.position(), level.getGameTime() + AbilityConfig.SLIME_FUSE_TICKS));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        long now = level.getGameTime();
        List<PendingBlast> due = new ArrayList<>();
        Iterator<PendingBlast> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingBlast pending = iterator.next();
            if (pending.level() == level && now >= pending.explodeAtTick()) {
                due.add(pending);
                iterator.remove();
            }
        }

        for (PendingBlast pending : due) {
            blast(pending);
        }
    }

    private static void blast(PendingBlast pending) {
        ServerLevel level = pending.level();
        Vec3 pos = pending.pos();

        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.0F);

        AABB area = new AABB(
                pos.x - AbilityConfig.SLIME_EXPLOSION_RADIUS, pos.y - AbilityConfig.SLIME_EXPLOSION_RADIUS, pos.z - AbilityConfig.SLIME_EXPLOSION_RADIUS,
                pos.x + AbilityConfig.SLIME_EXPLOSION_RADIUS, pos.y + AbilityConfig.SLIME_EXPLOSION_RADIUS, pos.z + AbilityConfig.SLIME_EXPLOSION_RADIUS);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && !(e instanceof Enemy));
        for (LivingEntity victim : victims) {
            if (victim.position().distanceTo(pos) > AbilityConfig.SLIME_EXPLOSION_RADIUS) continue;
            victim.hurt(victim.damageSources().magic(), AbilityConfig.SLIME_EXPLOSION_DAMAGE);
        }
    }
}
