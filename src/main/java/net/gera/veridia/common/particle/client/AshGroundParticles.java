package net.gera.veridia.common.particle.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Пепел у земли в пепельных биомах. Ванильные частицы биома (ambient_particle в ModBiomes) появляются равномерно в
 * кубе вокруг игрока, то есть в основном высоко в воздухе. Этот эмиттер добавляет пепел именно у поверхности:
 * каждый тик берётся несколько случайных столбцов вокруг игрока, и в тех, что в пепельном биоме, у земли
 * появляется частица. Чисто клиентская картинка.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public class AshGroundParticles {
    // Сколько случайных столбцов проверяется за тик, радиус (блоков) вокруг игрока и шанс частицы в столбце:
    // в среднем COLUMNS_PER_TICK * SPAWN_CHANCE частиц за тик (сейчас 4, то есть 80 в секунду)
    private static final int COLUMNS_PER_TICK = 10;
    private static final int RADIUS = 24;
    private static final float SPAWN_CHANCE = 0.5F;

    // Высота частицы над поверхностью: от и до (блоков); чем меньше максимум, тем плотнее пепел у земли
    private static final double MIN_HEIGHT_ABOVE_GROUND = 0.1D;
    private static final double MAX_HEIGHT_ABOVE_GROUND = 2.0D;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused()) {
            return;
        }
        for (int i = 0; i < COLUMNS_PER_TICK; i++) {
            emitInRandomColumn(level, player.blockPosition(), level.random);
        }
    }

    private static void emitInRandomColumn(ClientLevel level, BlockPos center, RandomSource random) {
        if (random.nextFloat() >= SPAWN_CHANCE) {
            return;
        }
        int x = center.getX() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
        int z = center.getZ() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
        int groundTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (!isAshBiome(level, new BlockPos(x, groundTop - 1, z))) {
            return;
        }
        double height = MIN_HEIGHT_ABOVE_GROUND + random.nextDouble() * (MAX_HEIGHT_ABOVE_GROUND - MIN_HEIGHT_ABOVE_GROUND);
        level.addParticle(ParticleTypes.WHITE_ASH, x + random.nextDouble(), groundTop + height, z + random.nextDouble(),
                0.0D, 0.0D, 0.0D);
    }

    private static boolean isAshBiome(ClientLevel level, BlockPos pos) {
        return ModBiomes.isAshBiome(level.getBiome(pos));
    }
}
