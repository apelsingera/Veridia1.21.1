package net.gera.veridia.common.mana.veridite;

import net.gera.veridia.common.compat.curios.IActivatableCurio;
import net.gera.veridia.common.mana.ManaTooltips;
import net.gera.veridia.common.mana.spell.MobGemAttack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class BlindingPrismItem extends Item implements ICurioItem, IActivatableCurio, MobGemAttack {

    private static final double MOB_CAST_LASER_LENGTH = 10.0D;
    private static final double MOB_CAST_LASER_RADIUS = 0.3D;
    private static final float MOB_CAST_LASER_DAMAGE = 7.0F;

    public BlindingPrismItem(Properties properties) {
        super(properties);
    }

    @Override
    public void onActivate(ServerPlayer player, ItemStack stack) {
        VeriditeLaserAbility.tryActivate(player, stack);
    }

    @Override
    public boolean castByMob(LivingEntity caster, LivingEntity target, ItemStack stack) {
        LaserUtil.fireOnceAtTarget(caster, target, MOB_CAST_LASER_LENGTH, MOB_CAST_LASER_RADIUS, MOB_CAST_LASER_DAMAGE,
                0, ParticleTypes.END_ROD);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ManaTooltips.appendCooldownAndManaCost(tooltip, VeriditeLaserAbility.COOLDOWN_TICKS, VeriditeLaserAbility.MANA_COST);
    }
}
