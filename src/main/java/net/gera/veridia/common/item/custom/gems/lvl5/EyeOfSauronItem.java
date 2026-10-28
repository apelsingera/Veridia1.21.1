package net.gera.veridia.common.item.custom.gems.lvl5;

import net.gera.veridia.common.compat.curios.IActivatableCurio;
import net.gera.veridia.common.item.custom.GemCooldowns;
import net.gera.veridia.common.mana.ManaProgression;
import net.gera.veridia.common.mana.ManaTooltips;
import net.gera.veridia.common.mana.ally.AlliedBreezeHandler;
import net.gera.veridia.common.mana.ally.AlliedDrownedHandler;
import net.gera.veridia.common.mana.spell.GemSpell;
import net.gera.veridia.common.mana.spell.MobGemAttack;
import net.gera.veridia.common.mana.spell.SpellCasting;
import net.gera.veridia.common.mana.veridite.LaserUtil;
import net.gera.veridia.common.entity.mutant.MutantEntity;
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
import java.util.function.DoubleUnaryOperator;

public class EyeOfSauronItem extends Item implements ICurioItem, IActivatableCurio, GemSpell, MobGemAttack {
    private static final int DURATION_TICKS = 20 * 8;
    private static final double LASER_LENGTH = 11.0D;
    private static final int TIER = 5;
    private static final int SOUND_INTERVAL_TICKS = 7 - TIER; // чем выше уровень, тем чаще firecharge
    private static final double LASER_WIDTH = 4.0D / 16.0D;
    private static final double LASER_RADIUS = LASER_WIDTH / 2.0D;
    private static final float LASER_DAMAGE = 9.0F;
    private static final int BURN_DURATION_TICKS = 20 * 25;
    private static final int MANA_COST = 65;
    private static final float SUPER_DAMAGE_MULTIPLIER = 1.9F;

    private static final double PULL_RADIUS = LASER_LENGTH;
    private static final double PULL_STRENGTH = 0.55D;
    private static final DoubleUnaryOperator PROXIMITY_BONUS =
            dist -> dist <= 2.0D ? 1.5D : (dist <= 4.0D ? 1.0D : (dist <= 7.0D ? 0.5D : 0.0D));

    private static final int MOB_CAST_DURATION_TICKS_REGULAR = 20 * 3;
    private static final double MOB_CAST_LASER_LENGTH_REGULAR = 5.0D;
    private static final float MOB_CAST_LASER_DAMAGE_REGULAR = 4.0F;
    private static final int MOB_CAST_BURN_DURATION_TICKS_REGULAR = 20 * 4;

    private static final int MOB_CAST_DURATION_TICKS_ELITE = 20 * 5;
    private static final double MOB_CAST_LASER_LENGTH_ELITE = 6.5D;
    private static final float MOB_CAST_LASER_DAMAGE_ELITE = 6.0F;
    private static final int MOB_CAST_BURN_DURATION_TICKS_ELITE = 20 * 6;

    public EyeOfSauronItem(Properties properties) {
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
        beginFiring(player, false);
    }

    @Override
    public void castSuper(ServerPlayer player, ItemStack stack) {
        beginFiring(player, true);
    }

    private void beginFiring(ServerPlayer player, boolean superCast) {
        long expireAt = player.level().getGameTime() + DURATION_TICKS;
        player.getPersistentData().putLong(EyeOfSauronHandler.FIRING_UNTIL_KEY, expireAt);
        player.getPersistentData().putBoolean(EyeOfSauronHandler.FIRING_SUPER_KEY, superCast);
    }


    static void fireLaser(ServerPlayer player) {
        if (player.isUnderWater()) {
            return;
        }

        boolean superCast = player.getPersistentData().getBoolean(EyeOfSauronHandler.FIRING_SUPER_KEY);
        float damage = superCast
                ? LASER_DAMAGE * SUPER_DAMAGE_MULTIPLIER * (float) ManaProgression.getSuperPowerMultiplier(player)
                : LASER_DAMAGE;

        if (player.level().isRaining() || player.level().isThundering()) {
            damage *= 0.85F;
        }

        pullNearby(player);
        LaserUtil.fireEyeOfSauronBeam(player, LASER_LENGTH, LASER_RADIUS, damage, BURN_DURATION_TICKS,
                PROXIMITY_BONUS, TIER);

        if (player.tickCount % SOUND_INTERVAL_TICKS == 0) {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5F, 1.2F);
        }
    }

    private static void pullNearby(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = player.position();
        AABB area = player.getBoundingBox().inflate(PULL_RADIUS);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area,
                le -> le != player && le.isAlive()
                        && !le.getTags().contains(AlliedDrownedHandler.ALLY_TAG)
                        && !le.getTags().contains(AlliedBreezeHandler.ALLY_TAG))) {
            Vec3 toPlayer = new Vec3(center.x - e.getX(), 0.0D, center.z - e.getZ());
            double dist = toPlayer.length();
            if (dist < 0.6D) {
                continue;
            }
            Vec3 pull = toPlayer.scale(PULL_STRENGTH / dist);
            e.setDeltaMovement(e.getDeltaMovement().add(pull.x, 0.0D, pull.z));
            e.hurtMarked = true;
        }
    }

    @Override
    public boolean castByMob(LivingEntity caster, LivingEntity target, ItemStack stack) {
        if (!(caster instanceof MutantEntity mutant)) {
            LaserUtil.fireEyeOfSauronBeam(caster, LASER_LENGTH, LASER_RADIUS, LASER_DAMAGE, BURN_DURATION_TICKS, TIER);
            return false;
        }

        boolean elite = mutant.isElite();
        int durationTicks = elite ? MOB_CAST_DURATION_TICKS_ELITE : MOB_CAST_DURATION_TICKS_REGULAR;
        double length = elite ? MOB_CAST_LASER_LENGTH_ELITE : MOB_CAST_LASER_LENGTH_REGULAR;
        float damage = elite ? MOB_CAST_LASER_DAMAGE_ELITE : MOB_CAST_LASER_DAMAGE_REGULAR;
        int burnTicks = elite ? MOB_CAST_BURN_DURATION_TICKS_ELITE : MOB_CAST_BURN_DURATION_TICKS_REGULAR;

        mutant.startGemChannel(durationTicks,
                channelTarget -> LaserUtil.fireEyeOfSauronBeamAtTarget(mutant, channelTarget, length, LASER_RADIUS, damage, burnTicks, TIER),
                mutant::dropStolenItem);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ManaTooltips.appendCooldownAndManaCost(tooltip, GemCooldowns.COOLDOWN_TICKS, MANA_COST);
    }
}