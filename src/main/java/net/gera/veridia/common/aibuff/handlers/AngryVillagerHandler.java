package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.aibuff.villager.angry.AngryVillager;
import net.gera.veridia.common.aibuff.villager.angry.AngryVillagerTargetGoal;
import net.gera.veridia.common.aibuff.villager.angry.VillagerMeleeAttackGoal;
import net.gera.veridia.common.aibuff.villager.angry.VillagerProjectileThrowGoal;
import net.gera.veridia.common.aibuff.villager.angry.VillagerRetreatGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Житель с тегом AngryVillager.TAG злится на игрока или враждебного моба (см. Enemy), который его
 * ударил, или на игрока, который открыл сундук поблизости (см. AngryVillager), гоняется за
 * обидчиком, дерётся мотыгой вплотную (VillagerMeleeAttackGoal) или кидает heavy_projectile
 * издалека (VillagerProjectileThrowGoal), пока не простит через AbilityConfig.VILLAGER_ANGER_DURATION_TICKS.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public class AngryVillagerHandler {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;
        if (!AngryVillager.hasTag(villager)) return;

        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;
        if (!(attacker instanceof Player) && !(attacker instanceof Enemy)) return;

        AngryVillager.makeAngry(villager, attacker);
    }

    /** Открыл сундук рядом со злым жителем (охрана добра) - тот же makeAngry, что и от удара. */
    @SubscribeEvent
    public static void onRightClickChest(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (!(level.getBlockState(event.getPos()).getBlock() instanceof ChestBlock)) return;

        Player player = event.getEntity();
        AABB area = new AABB(event.getPos()).inflate(AbilityConfig.VILLAGER_CHEST_GUARD_RADIUS);
        for (Villager villager : level.getEntitiesOfClass(Villager.class, area, AngryVillager::hasTag)) {
            AngryVillager.makeAngry(villager, player);
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;
        if (!AngryVillager.hasTag(villager)) return;

        if (!AngryVillager.hasEquippedHoe(villager)) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_HOE));
            villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            AngryVillager.markHoeEquipped(villager);
        }

        boolean hasTargetGoal = villager.targetSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof AngryVillagerTargetGoal);
        if (!hasTargetGoal) {
            villager.targetSelector.addGoal(1, new AngryVillagerTargetGoal(villager));
        }

        boolean hasRetreatGoal = villager.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof VillagerRetreatGoal);
        if (!hasRetreatGoal) {
            villager.goalSelector.addGoal(1, new VillagerRetreatGoal(villager));
        }

        boolean hasMeleeGoal = villager.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof MeleeAttackGoal);
        if (!hasMeleeGoal) {
            villager.goalSelector.addGoal(2, new VillagerMeleeAttackGoal(villager));
        }

        boolean hasProjectileGoal = villager.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof VillagerProjectileThrowGoal);
        if (!hasProjectileGoal) {
            villager.goalSelector.addGoal(3, new VillagerProjectileThrowGoal(villager));
        }
    }
}
