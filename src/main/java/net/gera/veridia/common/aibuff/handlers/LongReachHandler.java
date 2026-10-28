package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.common.aibuff.LongReachAttackGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import static net.gera.veridia.common.aibuff.AbilityConfig.LONG_REACH_DISTANCE;

@EventBusSubscriber(modid = "veridia")
public class LongReachHandler {

    private static final String TAG = "longreach";

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof PathfinderMob mob)) return;

        // Оставляем проверку через ванильные теги, раз тебе так удобнее тестировать
        if (!mob.getTags().contains(TAG)) return;

        // ИСПРАВЛЕНИЕ УРОНА №1: Разрешаем серверу засчитывать урон издалека
        var reachAttr = mob.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reachAttr != null) {
            reachAttr.setBaseValue(LONG_REACH_DISTANCE);
        }

        boolean hasGoal = mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(g -> g.getGoal() instanceof LongReachAttackGoal);

        if (hasGoal) return;

        mob.goalSelector.getAvailableGoals().stream()
                .map(WrappedGoal::getGoal)
                .filter(MeleeAttackGoal.class::isInstance)
                .toList()
                .forEach(mob.goalSelector::removeGoal);

        // ИСПРАВЛЕНИЕ УРОНА №2: Меняем false на true, чтобы ИИ не отменял удар на полпути
        mob.goalSelector.addGoal(2,
                new LongReachAttackGoal(mob, 1.0D, true, LONG_REACH_DISTANCE));
    }
}