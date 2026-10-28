package net.gera.veridia.common.mana.spell;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface MobGemAttack {
        boolean castByMob(LivingEntity caster, LivingEntity target, ItemStack stack);
    }