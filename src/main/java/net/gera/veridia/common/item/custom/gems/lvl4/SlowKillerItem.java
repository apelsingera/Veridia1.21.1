package net.gera.veridia.common.item.custom.gems.lvl4;

import net.gera.veridia.common.compat.curios.IActivatableCurio;
import net.gera.veridia.common.entity.rustynail.RustyNailEntity;
import net.gera.veridia.common.item.custom.GemCooldowns;
import net.gera.veridia.common.mana.ManaProgression;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.function.Predicate;

public class SlowKillerItem extends Item implements ICurioItem, IActivatableCurio, GemSpell, MobGemAttack {

    private static final double RADIUS = 11.0D;
    private static final double INNER_RADIUS = 6.0D;
    private static final int EFFECT_DURATION = 20 * 30;
    private static final int EFFECT_DURATION2 = 20 * 40;
    private static final int EFFECT_AMPLIFIER = 2;
    private static final int EFFECT_AMPLIFIER2 = 2;
    private static final int PARTICLE_POINTS = 48;
    private static final int INNER_PARTICLE_POINTS = 32;
    private static final int MANA_COST = 65;

    private static final int SUPER_AMPLIFIER_BONUS = 3;
    private static final int SUPER_DURATION_BONUS = 20 * 30;

    private static final int NAIL_COUNT_MIN = 50;
    private static final int NAIL_COUNT_MAX = 70;
    private static final double NAIL_RADIUS = 12.0D;
    private static final double NAIL_SPAWN_HEIGHT_MIN = 16.0D;
    private static final double NAIL_SPAWN_HEIGHT_MAX = 24.0D;

    public SlowKillerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onActivate(ServerPlayer player, ItemStack stack) {
        SpellCasting.tryCast(player, this, this, stack, MANA_COST);
    }

    @Override
    public void castNormal(ServerPlayer player, ItemStack stack) {
        pulse(player.serverLevel(), player, EFFECT_AMPLIFIER2, EFFECT_DURATION, EFFECT_AMPLIFIER, EFFECT_DURATION2);
        spawnRustyNailRain(player.serverLevel(), player);
    }

    @Override
    public void castSuper(ServerPlayer player, ItemStack stack) {
        double power = ManaProgression.getSuperPowerMultiplier(player);
        int extraDuration = (int) (SUPER_DURATION_BONUS * power);
        pulse(player.serverLevel(), player,
                EFFECT_AMPLIFIER2 + SUPER_AMPLIFIER_BONUS, EFFECT_DURATION + extraDuration,
                EFFECT_AMPLIFIER + SUPER_AMPLIFIER_BONUS, EFFECT_DURATION2 + extraDuration);
        spawnRustyNailRain(player.serverLevel(), player);
    }

    private void spawnRustyNailRain(ServerLevel level, ServerPlayer owner) {
        int count = NAIL_COUNT_MIN + owner.getRandom().nextInt(NAIL_COUNT_MAX - NAIL_COUNT_MIN + 1);
        for (int i = 0; i < count; i++) {
            double angle = owner.getRandom().nextDouble() * Math.PI * 2.0D;
            double dist = Math.sqrt(owner.getRandom().nextDouble()) * NAIL_RADIUS;
            double x = owner.getX() + Math.cos(angle) * dist;
            double z = owner.getZ() + Math.sin(angle) * dist;
            double y = owner.getY() + NAIL_SPAWN_HEIGHT_MIN
                    + owner.getRandom().nextDouble() * (NAIL_SPAWN_HEIGHT_MAX - NAIL_SPAWN_HEIGHT_MIN);

            RustyNailEntity nail = new RustyNailEntity(level, owner);
            nail.setPos(x, y, z);
            nail.setDeltaMovement(0.0D, -0.6D, 0.0D);
            level.addFreshEntity(nail);
        }
    }

    private void pulse(ServerLevel level, Player owner, int poisonAmplifier, int poisonDuration,
                        int slowAmplifier, int slowDuration) {
        pulse(level, owner, poisonAmplifier, poisonDuration, slowAmplifier, slowDuration,
                e -> e instanceof Enemy && e.isAlive());
    }

    private void pulse(ServerLevel level, LivingEntity owner, int poisonAmplifier, int poisonDuration,
                        int slowAmplifier, int slowDuration, Predicate<LivingEntity> targetFilter) {
        double ox = owner.getX();
        double oy = owner.getY();
        double oz = owner.getZ();

        level.playSound(null, ox, oy, oz, SoundEvents.WARDEN_AGITATED, SoundSource.PLAYERS, 1.0F, 1.1F);
        level.playSound(null, ox, oy, oz, SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 1.0F, 0.9F);

        for (int i = 0; i < PARTICLE_POINTS; i++) {
            double angle = (2 * Math.PI / PARTICLE_POINTS) * i;
            double px = ox + RADIUS * Math.cos(angle);
            double pz = oz + RADIUS * Math.sin(angle);
            level.sendParticles(ParticleTypes.WITCH, px, oy + 0.1D, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        for (int i = 0; i < INNER_PARTICLE_POINTS; i++) {
            double angle = (2 * Math.PI / INNER_PARTICLE_POINTS) * i;
            double px = ox + INNER_RADIUS * Math.cos(angle);
            double pz = oz + INNER_RADIUS * Math.sin(angle);
            level.sendParticles(ParticleTypes.ITEM_SLIME, px, oy + 0.1D, pz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        AABB area = owner.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != owner && targetFilter.test(e));

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, poisonDuration, poisonAmplifier));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowDuration, slowAmplifier));
        }
    }

    @Override
    public boolean castByMob(LivingEntity caster, LivingEntity target, ItemStack stack) {
        if (caster instanceof MutantEntity mutant) {
            mutant.throwStolenItemAway();
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ManaTooltips.appendCooldownAndManaCost(tooltip, GemCooldowns.COOLDOWN_TICKS, MANA_COST);
    }
}