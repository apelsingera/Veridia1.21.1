package net.gera.veridia.common.mana.veridite;

import net.gera.veridia.Veridia;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class VeriditeLaserHandler {

    private static final float DAMAGE_PER_TICK = 20.0F;
    private static final float MAX_DAMAGE_PER_CAST = 120.0F;
    private static final double LENGTH = 12.0D;
    private static final double RADIUS = 0.35D;
    private static final int BURN_TICKS = 20 * 2;
    private static final int GLOW_DURATION_TICKS = 20 * 5;
    private static final DustParticleOptions LASER_PARTICLE =
            new DustParticleOptions(Vec3.fromRGB24(0x36ebab).toVector3f(), 1.6F);

    private static final Map<UUID, Map<UUID, Float>> DAMAGE_DEALT_BY_PLAYER = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        long now = player.level().getGameTime();

        long chargeUntil = player.getPersistentData().getLong(VeriditeLaserAbility.CHARGE_UNTIL_KEY);
        if (chargeUntil != 0L) {
            if (now >= chargeUntil) {
                player.getPersistentData().remove(VeriditeLaserAbility.CHARGE_UNTIL_KEY);
                beginFiring(player, now);
            }
            return;
        }

        long firingUntil = player.getPersistentData().getLong(VeriditeLaserAbility.FIRING_UNTIL_KEY);
        if (firingUntil == 0L) return;

        if (now >= firingUntil) {
            player.getPersistentData().remove(VeriditeLaserAbility.FIRING_UNTIL_KEY);
            DAMAGE_DEALT_BY_PLAYER.remove(player.getUUID());
            return;
        }

        fireBeamTick(player);
    }

    private static void beginFiring(ServerPlayer player, long now) {
        player.getPersistentData().putLong(VeriditeLaserAbility.FIRING_UNTIL_KEY,
                now + VeriditeLaserAbility.FIRE_DURATION_TICKS);
        DAMAGE_DEALT_BY_PLAYER.put(player.getUUID(), new HashMap<>());

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION_TICKS, 0));

        PacketDistributor.sendToPlayer(player, new PlayLaserShakePayload(VeriditeLaserAbility.FIRE_DURATION_TICKS));
    }

    private static void fireBeamTick(ServerPlayer player) {
        Map<UUID, Float> damageDealt = DAMAGE_DEALT_BY_PLAYER.computeIfAbsent(player.getUUID(), key -> new HashMap<>());
        LaserUtil.fireOnceCapped(player, LENGTH, RADIUS, DAMAGE_PER_TICK, BURN_TICKS, LASER_PARTICLE,
                damageDealt, MAX_DAMAGE_PER_CAST);

        if (player.tickCount % 4 == 0) {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.6F, 1.1F);
        }
    }
}