package net.gera.veridia.common.item.custom.gems.lvl1;

import net.gera.veridia.common.compat.curios.IActivatableCurio;
import net.gera.veridia.common.item.custom.GemCooldowns;
import net.gera.veridia.common.mana.ManaProgression;
import net.gera.veridia.common.mana.ManaTooltips;
import net.gera.veridia.common.mana.spell.GemSpell;
import net.gera.veridia.common.mana.spell.MobGemAttack;
import net.gera.veridia.common.mana.spell.SpellCasting;
import net.gera.veridia.common.entity.mutant.MutantEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Optional;

public class InfernalStaffItem extends Item implements ICurioItem, IActivatableCurio, GemSpell, MobGemAttack {
    static final String ACTIVE_UNTIL_KEY = "veridia_infernal_staff_active_until";
    static final int DURATION_TICKS = 20 * 15;
    private static final int MANA_COST = 50;

    private static final double MOB_CAST_BEAM_RADIUS = 0.3D;
    private static final double MOB_CAST_PARTICLE_STEP = 0.12D;

    private static final double MOB_CAST_RADIANS_PER_TICK = (Math.PI * 2.0D) / 100.0D;
    private static final DustParticleOptions MOB_CAST_FLAME_PARTICLE =
            new DustParticleOptions(Vec3.fromRGB24(0xFF6A00).toVector3f(), 1.2F);

    private static final int MOB_CAST_DURATION_TICKS_REGULAR = 20 * 3;
    private static final int MOB_CAST_DURATION_TICKS_ELITE = 20 * 5;

    private static final int MOB_CAST_BEAM_COUNT_REGULAR = 1;
    private static final double MOB_CAST_BEAM_LENGTH_REGULAR = 4.0D;
    private static final float MOB_CAST_BEAM_DAMAGE_REGULAR = 6.0F;

    private static final int MOB_CAST_BEAM_COUNT_ELITE = 2;
    private static final double MOB_CAST_BEAM_LENGTH_ELITE = 5.0D;
    private static final float MOB_CAST_BEAM_DAMAGE_ELITE = 8.0F;

    public InfernalStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onActivate(ServerPlayer player, ItemStack stack) {
        if (player.isUnderWater()) {
            return;
        }
        SpellCasting.tryCast(player, this, this, stack, MANA_COST);
    }

    @Override
    public void castNormal(ServerPlayer player, ItemStack stack) {
        activate(player, DURATION_TICKS);
    }

    @Override
    public void castSuper(ServerPlayer player, ItemStack stack) {
        double power = ManaProgression.getSuperPowerMultiplier(player);
        activate(player, (int) (DURATION_TICKS * power));
    }

    private void activate(ServerPlayer player, int durationTicks) {
        long now = player.level().getGameTime();
        player.getPersistentData().putLong(ACTIVE_UNTIL_KEY, now + durationTicks);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    @Override
    public boolean castByMob(LivingEntity caster, LivingEntity target, ItemStack stack) {
        if (!(caster instanceof MutantEntity mutant)) return false;

        boolean elite = mutant.isElite();
        int durationTicks = elite ? MOB_CAST_DURATION_TICKS_ELITE : MOB_CAST_DURATION_TICKS_REGULAR;
        int beamCount = elite ? MOB_CAST_BEAM_COUNT_ELITE : MOB_CAST_BEAM_COUNT_REGULAR;
        double beamLength = elite ? MOB_CAST_BEAM_LENGTH_ELITE : MOB_CAST_BEAM_LENGTH_REGULAR;
        float beamDamage = elite ? MOB_CAST_BEAM_DAMAGE_ELITE : MOB_CAST_BEAM_DAMAGE_REGULAR;

        if (mutant.level() instanceof ServerLevel level) {
            level.playSound(null, mutant.getX(), mutant.getY(), mutant.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.8F);
        }

        mutant.startGemChannel(durationTicks,
                channelTarget -> tickBeamRing(mutant, beamCount, beamLength, beamDamage),
                mutant::dropStolenItem);
        return true;
    }

    private static void tickBeamRing(MutantEntity mutant, int beamCount, double beamLength, float beamDamage) {
        if (!(mutant.level() instanceof ServerLevel level)) return;

        Vec3 center = mutant.position().add(0.0D, mutant.getBbHeight() / 2.0D, 0.0D);
        double baseAngle = mutant.level().getGameTime() * MOB_CAST_RADIANS_PER_TICK;

        for (int i = 0; i < beamCount; i++) {
            double angle = baseAngle + (2.0D * Math.PI / beamCount) * i;
            Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            Vec3 end = center.add(direction.scale(beamLength));

            AABB searchArea = new AABB(center, end).inflate(MOB_CAST_BEAM_RADIUS + 1.0D);
            List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                    e -> e != mutant && e.isAlive());
            for (LivingEntity candidate : candidates) {
                Optional<Vec3> hit = candidate.getBoundingBox().inflate(MOB_CAST_BEAM_RADIUS).clip(center, end);
                if (hit.isPresent()) {
                    candidate.hurt(mutant.damageSources().mobAttack(mutant), beamDamage);
                }
            }
            spawnMobCastBeamParticles(level, center, end);
        }
    }

    private static void spawnMobCastBeamParticles(ServerLevel level, Vec3 start, Vec3 end) {
        double length = start.distanceTo(end);
        int steps = (int) Math.ceil(length / MOB_CAST_PARTICLE_STEP);
        Vec3 direction = end.subtract(start).normalize();
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(direction.scale(Math.min(i * MOB_CAST_PARTICLE_STEP, length)));
            level.sendParticles(MOB_CAST_FLAME_PARTICLE, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ManaTooltips.appendDurationCooldownAndManaCost(tooltip, DURATION_TICKS, GemCooldowns.COOLDOWN_TICKS, MANA_COST);
    }
}