package net.gera.veridia.common.item.custom;

import net.gera.veridia.Veridia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.UUID;

/**
 * Пока игрок держит в руке job_application с привязанным жителем (клик по нему -
 * JobApplicationItem.interactLivingEntity/linkedVillager), непрерывно рисует линию партиклов от
 * жителя к складу(ам), на которые его можно назначить, и подсвечивает сами склады - всё время,
 * пока привязка жива, а не фиксированные несколько секунд, как раньше (жалоба "частицы быстро
 * пропадают"). Привязка живёт в памяти (JobApplicationItem.LINKS), а не в NBT предмета, поэтому
 * "убрать предмет из руки" по-настоящему ОТМЕНЯЕТ job_application (жалоба "чтобы отменить
 * job_application надо убрать предмет с руки") - как только игрок больше не держит его в руке,
 * снимаем привязку тут же, а не просто перестаём рисовать.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public class JobApplicationEvents {

    private static final DustParticleOptions LINE_DUST =
            new DustParticleOptions(Vec3.fromRGB24(0x33CC33).toVector3f(), 1.0F);
    /** Крупнее и ярче LINE_DUST - облако прямо над складом, чтобы было видно, какой конкретно
     * блок сейчас выбран целью, а не только линию к нему. */
    private static final DustParticleOptions STORAGE_GLOW =
            new DustParticleOptions(Vec3.fromRGB24(0x33CC33).toVector3f(), 1.8F);
    private static final double LINE_STEP = 0.5D;
    private static final int REDRAW_PERIOD_TICKS = 5;
    private static final int GLOW_PARTICLES_PER_STORAGE = 8;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % REDRAW_PERIOD_TICKS != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID villagerId = JobApplicationItem.linkedVillager(player.getUUID());
            if (villagerId == null) continue;

            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(mainHand.getItem() instanceof JobApplicationItem)) {
                // Предмет убрали из руки - это и есть отмена, снимаем привязку насовсем.
                JobApplicationItem.cancelLink(player.getUUID());
                continue;
            }

            ServerLevel level = player.serverLevel();
            if (!(level.getEntity(villagerId) instanceof Villager villager)) continue;

            List<BlockPos> targets = JobApplicationItem.computeTargets(villager, level);
            Vec3 from = villager.position().add(0.0D, villager.getBbHeight() * 0.5D, 0.0D);
            for (BlockPos target : targets) {
                spawnLine(level, from, Vec3.atCenterOf(target));
                spawnStorageGlow(level, target);
            }
        }
    }

    /** Чистим привязку при выходе игрока, чтобы карта не копила записи для тех, кто уже отключился. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        JobApplicationItem.cancelLink(event.getEntity().getUUID());
    }

    private static void spawnLine(ServerLevel level, Vec3 from, Vec3 to) {
        double distance = from.distanceTo(to);
        int steps = Math.max(1, (int) (distance / LINE_STEP));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.lerp(to, (double) i / steps);
            level.sendParticles(LINE_DUST, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void spawnStorageGlow(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.1D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(STORAGE_GLOW, x, y, z, GLOW_PARTICLES_PER_STORAGE, 0.35D, 0.2D, 0.35D, 0.0D);
    }
}
