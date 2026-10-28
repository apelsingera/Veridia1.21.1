
package net.gera.veridia.common.aibuff;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.EnumSet;

public class SpiderSummonGoal extends Goal {

    private final Spider spider;
    private int cooldown;

    public SpiderSummonGoal(Spider spider) {
        this.spider = spider;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        return this.spider.getTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        if (!(this.spider.level() instanceof ServerLevelAccessor serverLevel)) return;

        LivingEntity target = this.spider.getTarget();

        for (int i = 0; i < AbilityConfig.SPIDER_SUMMON_COUNT; i++) {
            double angle = (Math.PI * 2 / AbilityConfig.SPIDER_SUMMON_COUNT) * i;
            double x = this.spider.getX() + Math.cos(angle) * AbilityConfig.SPIDER_SUMMON_RADIUS;
            double z = this.spider.getZ() + Math.sin(angle) * AbilityConfig.SPIDER_SUMMON_RADIUS;
            double y = this.spider.getY();

            CaveSpider minion = new CaveSpider(EntityType.CAVE_SPIDER, this.spider.level());
            minion.moveTo(x, y, z, this.spider.getYRot(), 0.0F);
            minion.finalizeSpawn(serverLevel, this.spider.level().getCurrentDifficultyAt(minion.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);

            if (target != null) {
                minion.setTarget(target);
            }

            this.spider.level().addFreshEntity(minion);
        }

        this.cooldown = AbilityConfig.SPIDER_SUMMON_COOLDOWN_TICKS;
    }
}