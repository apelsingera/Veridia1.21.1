package net.gera.veridia.common.block.spawner;

import net.gera.veridia.Veridia;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Прочность спавнера с рунными камнями рядом: скорость добычи делится так, будто твёрдость блока выросла на
 * бонус камней (ванильная 5, при бонусе +3 получается 8). Событие срабатывает и на сервере, и на клиенте, а
 * камни видны обеим сторонам, поэтому расчёт одинаковый и анимация разрушения не рассинхронизируется.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public class SpawnerHardnessHandler {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        BlockState state = event.getState();
        if (!state.is(Blocks.SPAWNER)) {
            return;
        }
        BlockPos pos = event.getPosition().orElse(null);
        if (pos == null) {
            return;
        }
        Level level = event.getEntity().level();
        float bonus = RunestoneSpawnerBoost.forSpawner(level, pos).hardness();
        if (bonus <= 0.0F) {
            return;
        }
        float baseHardness = state.getDestroySpeed(level, pos);
        event.setNewSpeed(event.getNewSpeed() * baseHardness / (baseHardness + bonus));
    }
}
