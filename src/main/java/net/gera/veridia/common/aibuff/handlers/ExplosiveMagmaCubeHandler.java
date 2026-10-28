package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Взрывной магма-куб: активируется ТОЛЬКО тегом EXPLOSIVE_MAGMA_CUBE - его назначает
 * WitherMagmaSummonGoal при призыве визером, но с тем же успехом его может выставить и обычная
 * команда (например /summon minecraft:magma_cube ~ ~ ~ {Tags:["explosive_magma_cube"]}) - весь
 * функционал (имя, частицы, отложенный взрыв) завязан только на тег, а не на то, кто заспавнил.
 * Пока жив - испускает красные частицы. Умирает как обычный магма-куб, но через
 * EXPLOSIVE_MAGMA_FUSE_TICKS тиков после смерти на месте гибели происходит взрыв.
 */
public class ExplosiveMagmaCubeHandler {

    public static final String TAG = "explosive_magma_cube";
    private static final String NAME = "взрывной магма-куб";

    private static final DustParticleOptions RED_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.05F), 1.0F);

    private record PendingExplosion(ServerLevel level, Vec3 pos, long explodeAtTick) {}

    private static final List<PendingExplosion> PENDING = new ArrayList<>();

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof MagmaCube cube)) return;
        if (cube.level().isClientSide()) return;
        if (!cube.getTags().contains(TAG)) return;

        if (!cube.hasCustomName()) {
            cube.setCustomName(Component.literal(NAME).withStyle(ChatFormatting.RED));
            cube.setCustomNameVisible(true);
        }

        if (cube.tickCount % AbilityConfig.EXPLOSIVE_MAGMA_PARTICLE_PERIOD_TICKS != 0) return;

        ServerLevel level = (ServerLevel) cube.level();
        level.sendParticles(RED_DUST,
                cube.getX(), cube.getY() + cube.getBbHeight() * 0.5, cube.getZ(),
                AbilityConfig.EXPLOSIVE_MAGMA_PARTICLE_COUNT,
                cube.getBbWidth() * 0.3, cube.getBbHeight() * 0.3, cube.getBbWidth() * 0.3, 0.0);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof MagmaCube cube)) return;
        if (!cube.getTags().contains(TAG)) return;
        if (!(cube.level() instanceof ServerLevel level)) return;

        PENDING.add(new PendingExplosion(level, cube.position(),
                level.getGameTime() + AbilityConfig.EXPLOSIVE_MAGMA_FUSE_TICKS));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        long now = level.getGameTime();

        List<PendingExplosion> due = new ArrayList<>();
        Iterator<PendingExplosion> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingExplosion pending = iterator.next();
            if (pending.level() == level && now >= pending.explodeAtTick()) {
                due.add(pending);
                iterator.remove();
            }
        }
        for (PendingExplosion pending : due) {
            level.explode(null, pending.pos().x, pending.pos().y, pending.pos().z,
                    AbilityConfig.EXPLOSIVE_MAGMA_EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
        }
    }
}
