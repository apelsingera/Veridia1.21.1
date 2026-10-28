package net.gera.veridia.common.item.custom.heavy_projectile;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HeavyProjectile extends Item {


    public HeavyProjectile(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);

        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.2F / (pLevel.getRandom().nextFloat() * 0.2F + 0.8F));

        if (!pLevel.isClientSide) {
            HeavyProjectileEntity heavyProjectileE = new HeavyProjectileEntity(pPlayer, pLevel);
            heavyProjectileE.shootFromRotation(pPlayer, pPlayer.getXRot(), pPlayer.getYRot(), 0.0F, 1.5F, 0F);
            pLevel.addFreshEntity(heavyProjectileE);
        }

        pPlayer.awardStat(Stats.ITEM_USED.get(this));

        if (!pPlayer.getAbilities().instabuild) {
            itemstack.shrink(1);
        }

        pPlayer.getCooldowns().addCooldown(this, 100);

        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }
}