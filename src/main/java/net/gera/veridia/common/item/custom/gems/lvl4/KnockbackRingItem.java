package net.gera.veridia.common.item.custom.gems.lvl4;

import net.gera.veridia.common.compat.curios.IActivatableCurio;
import net.gera.veridia.common.item.custom.GemCooldowns;
import net.gera.veridia.common.mana.ManaProgression;
import net.gera.veridia.common.mana.ally.AlliedBreezeHandler;
import net.gera.veridia.common.mana.ManaTooltips;
import net.gera.veridia.common.mana.spell.GemSpell;
import net.gera.veridia.common.mana.spell.MobGemAttack;
import net.gera.veridia.common.mana.spell.SpellCasting;
import net.gera.veridia.common.entity.mutant.MutantEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.function.Predicate;

public class KnockbackRingItem extends Item implements ICurioItem, IActivatableCurio, GemSpell, MobGemAttack {

    private static final double RADIUS = 8.0D;
    private static final double KNOCKBACK_STRENGTH = 2.4D;
    private static final int PARTICLE_POINTS = 36;
    private static final int MANA_COST = 50;

    private static final double SUPER_KNOCKBACK_STRENGTH = 2.95D;
    private static final double SUPER_LAUNCH_BONUS = 1.4D;

    private static final int BREEZE_ALLY_COUNT = 1;
    private static final double MOB_CAST_KNOCKBACK_STRENGTH_REGULAR = 1.16D;
    private static final double MOB_CAST_RADIUS_REGULAR = 4.35D;
    private static final double MOB_CAST_KNOCKBACK_STRENGTH_ELITE = 1.74D;
    private static final double MOB_CAST_RADIUS_ELITE = 7.25D;

    public KnockbackRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onActivate(ServerPlayer player, ItemStack stack) {
        SpellCasting.tryCast(player, this, this, stack, MANA_COST);
    }

    @Override
    public void castNormal(ServerPlayer player, ItemStack stack) {
        pulse(player.serverLevel(), player, KNOCKBACK_STRENGTH, 0.0D);
        AlliedBreezeHandler.summonAllies(player, BREEZE_ALLY_COUNT);
    }

    @Override
    public void castSuper(ServerPlayer player, ItemStack stack) {
        double power = ManaProgression.getSuperPowerMultiplier(player);
        pulse(player.serverLevel(), player, SUPER_KNOCKBACK_STRENGTH * power, SUPER_LAUNCH_BONUS * power);
        AlliedBreezeHandler.summonAllies(player, BREEZE_ALLY_COUNT);
    }

    private void pulse(ServerLevel level, Player owner, double knockbackStrength, double extraLaunch) {
        pulse(level, owner, knockbackStrength, extraLaunch, KnockbackRingItem.RADIUS, e -> e instanceof Enemy && e.isAlive());
    }

    private void pulse(ServerLevel level, LivingEntity owner, double knockbackStrength, double extraLaunch,
                        double radius, Predicate<LivingEntity> targetFilter) {
        double ox = owner.getX();
        double oy = owner.getY();
        double oz = owner.getZ();

        level.playSound(null, ox, oy, oz, SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS, 1.0F, 1.0F);

        for (int i = 0; i < PARTICLE_POINTS; i++) {
            double angle = (2 * Math.PI / PARTICLE_POINTS) * i;
            double px = ox + radius * Math.cos(angle);
            double pz = oz + radius * Math.sin(angle);
            level.sendParticles(ParticleTypes.GUST, px, oy + 0.1D, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        AABB area = owner.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != owner && targetFilter.test(e));

        for (LivingEntity target : targets) {
            double dx = ox - target.getX();
            double dz = oz - target.getZ();
            if (dx * dx + dz * dz < 1.0E-4D) {
                dx = 0.1D;
                dz = 0.1D;
            }
            Vec3 dir = new Vec3(dx, 0.0D, dz).normalize();
            Vec3 currentVel = target.getDeltaMovement();
            double newY = target.onGround()
                    ? Math.min(0.4D, currentVel.y / 2.0D + knockbackStrength)
                    : currentVel.y;

            target.setDeltaMovement(
                    currentVel.x / 2.0D - dir.x * knockbackStrength,
                    newY + extraLaunch,
                    currentVel.z / 2.0D - dir.z * knockbackStrength
            );
            target.hurtMarked = true;
        }
    }

    @Override
    public boolean castByMob(LivingEntity caster, LivingEntity target, ItemStack stack) {
        if (!(caster.level() instanceof ServerLevel level)) return false;
        boolean elite = caster instanceof MutantEntity mutant && mutant.isElite();
        double strength = elite ? MOB_CAST_KNOCKBACK_STRENGTH_ELITE : MOB_CAST_KNOCKBACK_STRENGTH_REGULAR;
        double radius = elite ? MOB_CAST_RADIUS_ELITE : MOB_CAST_RADIUS_REGULAR;
        pulse(level, caster, strength, 0.0D, radius, e -> e == target);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ManaTooltips.appendCooldownAndManaCost(tooltip, GemCooldowns.COOLDOWN_TICKS, MANA_COST);
    }
}