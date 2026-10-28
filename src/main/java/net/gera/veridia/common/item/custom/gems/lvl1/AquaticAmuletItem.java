package net.gera.veridia.common.item.custom.gems.lvl1;

import net.gera.veridia.common.compat.curios.IActivatableCurio;
import net.gera.veridia.common.item.custom.GemCooldowns;
import net.gera.veridia.common.mana.ManaProgression;
import net.gera.veridia.common.mana.ManaTooltips;
import net.gera.veridia.common.mana.ally.AlliedDrownedHandler;
import net.gera.veridia.common.mana.spell.GemSpell;
import net.gera.veridia.common.mana.spell.MobGemAttack;
import net.gera.veridia.common.mana.spell.SpellCasting;
import net.gera.veridia.common.entity.mutant.MutantEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class AquaticAmuletItem extends Item implements ICurioItem, IActivatableCurio, GemSpell, MobGemAttack {

    private static final int MANA_COST = 20;

    private static final int EFFECTS_DURATION_TICKS = 20 * 15;
    private static final int WATER_BREATHING_AMPLIFIER = 1;
    private static final int DOLPHINS_GRACE_AMPLIFIER = 1;
    private static final int MOVEMENT_SPEED_AMPLIFIER = 1;

    private static final double ATTACK_SPEED_BONUS = 0.2D;

    private static final int MOB_CAST_SPEED_DURATION_TICKS_REGULAR = 10;
    private static final int MOB_CAST_SPEED_AMPLIFIER_REGULAR = 0;
    private static final int MOB_CAST_SPEED_DURATION_TICKS_ELITE = 20 * 15;
    private static final int MOB_CAST_SPEED_AMPLIFIER_ELITE = 1;

    public static final ResourceLocation ATTACK_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("veridia", "aquatic_amulet_attack_speed");

    public AquaticAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onActivate(ServerPlayer player, ItemStack stack) {
        SpellCasting.tryCast(player, this, this, stack, MANA_COST);
    }

    @Override
    public void castNormal(ServerPlayer player, ItemStack stack) {
        applyPlayerBuffs(player, WATER_BREATHING_AMPLIFIER, DOLPHINS_GRACE_AMPLIFIER, MOVEMENT_SPEED_AMPLIFIER,
                EFFECTS_DURATION_TICKS, ATTACK_SPEED_BONUS);
    }

    @Override
    public void castSuper(ServerPlayer player, ItemStack stack) {
        applyPlayerBuffs(player, WATER_BREATHING_AMPLIFIER, DOLPHINS_GRACE_AMPLIFIER, MOVEMENT_SPEED_AMPLIFIER,
                EFFECTS_DURATION_TICKS, ATTACK_SPEED_BONUS);
        AlliedDrownedHandler.summonAlly(player, ManaProgression.getSuperPowerMultiplier(player));
    }

    public static void applyPlayerBuffs(ServerPlayer player, int waterBreathingAmp, int dolphinsGraceAmp,
                                        int movementSpeedAmp, int effectsDuration, double attackSpeedBonus) {
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, effectsDuration, waterBreathingAmp));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, effectsDuration, dolphinsGraceAmp));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, effectsDuration, movementSpeedAmp));

        applyAttackSpeedBonus(player, attackSpeedBonus, effectsDuration);

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY(0.5), player.getZ(), 20, 0.4, 0.5, 0.4, 0.01);
        level.sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getY(0.5), player.getZ(), 30, 0.3, 0.4, 0.3, 0.01);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DOLPHIN_SPLASH, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    public static void applyAttackSpeedBonus(ServerPlayer player, double bonus, int durationTicks) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.addOrUpdateTransientModifier(new AttributeModifier(
                    ATTACK_SPEED_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        player.getPersistentData().putLong(AquaticAmuletHandler.PENDING_KEY,
                player.level().getGameTime() + durationTicks);
    }

    private void grantWaterBuffs(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,
                AquaticAmuletItem.EFFECTS_DURATION_TICKS, AquaticAmuletItem.WATER_BREATHING_AMPLIFIER));
        entity.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,
                AquaticAmuletItem.EFFECTS_DURATION_TICKS, AquaticAmuletItem.DOLPHINS_GRACE_AMPLIFIER));
    }
    @Override
    public boolean castByMob(LivingEntity caster, LivingEntity target, ItemStack stack) {
        grantWaterBuffs(caster);

        boolean elite = caster instanceof MutantEntity mutant && mutant.isElite();
        if (elite) {
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                    MOB_CAST_SPEED_DURATION_TICKS_ELITE, MOB_CAST_SPEED_AMPLIFIER_ELITE));
        } else {
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                    MOB_CAST_SPEED_DURATION_TICKS_REGULAR, MOB_CAST_SPEED_AMPLIFIER_REGULAR));
        }
        return false;
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,  TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ManaTooltips.appendCooldownAndManaCost(tooltip, GemCooldowns.COOLDOWN_TICKS, MANA_COST);
    }
}