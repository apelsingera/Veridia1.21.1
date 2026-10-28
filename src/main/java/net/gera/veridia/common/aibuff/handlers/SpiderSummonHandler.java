package net.gera.veridia.common.aibuff.handlers;

import net.gera.veridia.common.aibuff.SpiderSummonGoal;
import net.minecraft.world.entity.monster.Spider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class SpiderSummonHandler {

    private static final String TAG = "summoner";
    //private static final String APPLIED_TAG = "summoner_applied";

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Spider spider)) return;
        if (spider.level().isClientSide()) return;
        if (!spider.getTags().contains(TAG)) return;

        boolean hasGoal = spider.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof SpiderSummonGoal);

        if (!hasGoal) {
            spider.goalSelector.addGoal(2, new SpiderSummonGoal(spider));
        }
    }
}