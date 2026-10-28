package net.gera.veridia.common.aibuff.villager.golemhealer;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class HealerVillagerGoal extends Goal {

    private final Villager villager;
    private IronGolem golem;
    private int healDelay;

    public HealerVillagerGoal(Villager villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private static boolean needsHealing(IronGolem golem) {
        return golem.isAlive() && golem.getHealth() < golem.getMaxHealth();
    }

    @Override
    public boolean canUse() {
        if (villager.isBaby()) return false;
        if ((villager.getId() + villager.tickCount) % 10 != 0) return false;

        IronGolem nearest = findWoundedGolem();
        if (nearest == null) return false;
        this.golem = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return golem != null
                && needsHealing(golem)
                && villager.distanceToSqr(golem)
                        <= AbilityConfig.HEALER_VILLAGER_GIVE_UP_RADIUS * AbilityConfig.HEALER_VILLAGER_GIVE_UP_RADIUS;
    }

    @Override
    public void start() {
        villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
        villager.setDropChance(EquipmentSlot.MAINHAND, 0.8F);
        this.healDelay = AbilityConfig.HEALER_VILLAGER_HEAL_PERIOD_TICKS;
    }

    @Override
    public void stop() {
        villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        villager.getNavigation().stop();
        this.golem = null;
    }

    @Override
    public void tick() {
        if (golem == null) return;

        villager.getLookControl().setLookAt(golem, 30.0F, 30.0F);

        double reachSqr = AbilityConfig.HEALER_VILLAGER_HEAL_REACH * AbilityConfig.HEALER_VILLAGER_HEAL_REACH;
        if (villager.distanceToSqr(golem) > reachSqr) {
            villager.getNavigation().moveTo(golem, AbilityConfig.HEALER_VILLAGER_MOVE_SPEED);
            return;
        }

        villager.getNavigation().stop();
        if (--healDelay > 0)
            return;
        healDelay = AbilityConfig.HEALER_VILLAGER_HEAL_PERIOD_TICKS;

        float before = golem.getHealth();
        golem.heal(AbilityConfig.HEALER_VILLAGER_HEAL_AMOUNT);
        if (golem.getHealth() == before)
            return;

        villager.swing(InteractionHand.MAIN_HAND);
        float pitch = 1.0F + (villager.getRandom().nextFloat() - villager.getRandom().nextFloat()) * 0.2F;
        golem.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, pitch);
        if (golem.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    golem.getX(), golem.getY() + golem.getBbHeight() * 0.8, golem.getZ(),
                    5, 0.4, 0.5, 0.4, 0.0);
        }
    }

    private IronGolem findWoundedGolem() {
        double r = AbilityConfig.HEALER_VILLAGER_SEARCH_RADIUS;
        List<IronGolem> golems = villager.level().getEntitiesOfClass(
                IronGolem.class, villager.getBoundingBox().inflate(r), HealerVillagerGoal::needsHealing);
        return golems.stream().min(Comparator.comparingDouble(villager::distanceToSqr)).orElse(null);
    }
}