package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Уровень 35 (тег "gianttrigger" на инициаторе - см. levelzombie/GiantTriggerGoal) временно
 * превращает СЛУЧАЙНОГО СОСЕДНЕГО зомби (не самого инициатора) в "великана" на GIANT_DURATION_TICKS.
 * GiantTriggerGoal только выставляет GIANT_UNTIL_KEY на выбранном зомби - сам эффект
 * (масштаб/скорость/урон, ломание листвы и вытаптывание травы) применяет и снимает этот хендлер по
 * persistent-data ключам на самом превращённом зомби.
 */
public class GiantZombieHandler {

    public static final String GIANT_UNTIL_KEY = "veridia_giant_until";
    public static final String GIANT_APPLIED_KEY = "veridia_giant_applied";

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.level().isClientSide()) return;
        if (!zombie.getPersistentData().contains(GIANT_UNTIL_KEY)) return;

        long until = zombie.getPersistentData().getLong(GIANT_UNTIL_KEY);
        boolean active = zombie.level().getGameTime() < until;
        boolean applied = zombie.getPersistentData().getBoolean(GIANT_APPLIED_KEY);

        if (active && !applied) {
            applyGiant(zombie);
            zombie.getPersistentData().putBoolean(GIANT_APPLIED_KEY, true);
        } else if (!active && applied) {
            revertGiant(zombie);
            zombie.getPersistentData().putBoolean(GIANT_APPLIED_KEY, false);
            zombie.getPersistentData().remove(GIANT_UNTIL_KEY);
        }

        if (active) {
            trampleAndBreak(zombie);
        }
    }

    private static void applyGiant(Zombie zombie) {
        setScale(zombie, AbilityConfig.GIANT_SCALE);
        multiplyAttribute(zombie, Attributes.MOVEMENT_SPEED, AbilityConfig.GIANT_MOVE_SPEED_MULTIPLIER);
        multiplyAttribute(zombie, Attributes.ATTACK_DAMAGE, AbilityConfig.GIANT_DAMAGE_MULTIPLIER);
    }

    private static void revertGiant(Zombie zombie) {
        setScale(zombie, 1.0F);
        multiplyAttribute(zombie, Attributes.MOVEMENT_SPEED, 1.0D / AbilityConfig.GIANT_MOVE_SPEED_MULTIPLIER);
        multiplyAttribute(zombie, Attributes.ATTACK_DAMAGE, 1.0D / AbilityConfig.GIANT_DAMAGE_MULTIPLIER);
    }

    private static void setScale(Zombie zombie, float scale) {
        AttributeInstance instance = zombie.getAttribute(Attributes.SCALE);
        if (instance != null) instance.setBaseValue(scale);
    }

    private static void multiplyAttribute(Zombie zombie, Holder<Attribute> attribute, double factor) {
        AttributeInstance instance = zombie.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(instance.getBaseValue() * factor);
    }

    private static void trampleAndBreak(Zombie zombie) {
        BlockPos feet = zombie.blockPosition();
        BlockState below = zombie.level().getBlockState(feet.below());
        if (below.is(Blocks.GRASS_BLOCK)) {
            zombie.level().setBlockAndUpdate(feet.below(), Blocks.DIRT.defaultBlockState());
        }

        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-1, 0, -1), feet.offset(1, 2, 1))) {
            BlockState state = zombie.level().getBlockState(pos);
            if (state.is(BlockTags.LEAVES)) {
                zombie.level().destroyBlock(pos, false);
            }
        }
    }
}
