package net.gera.veridia.common.aibuff.levelzombie;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** Полная копия LevelRangeZombieEggItem, но спавнит Husk (Кадавр) вместо Zombie. */
public class LevelRangeHuskEggItem extends Item {
    private final int minLevel;
    private final int maxLevel;

    public LevelRangeHuskEggItem(int minLevel, int maxLevel, Properties properties) {
        super(properties);
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;

        if (SpawnerEggLevels.tryConfigureSpawner(context, EntityType.HUSK, minLevel, maxLevel)) {
            return InteractionResult.CONSUME;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Husk husk = EntityType.HUSK.create(serverLevel);
        if (husk == null) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        float yaw = player != null ? player.getYRot() : 0.0F;
        husk.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
        husk.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);

        int huskLevel = minLevel + serverLevel.getRandom().nextInt(maxLevel - minLevel + 1);
        LevelHuskHandler.applyLevel(husk, huskLevel);

        serverLevel.addFreshEntity(husk);

        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
