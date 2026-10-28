package net.gera.veridia.common.particle.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.particle.ModParticles;
import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Эффекты воды пепельной реки: из глубины поднимаются красные пузыри и лопаются у поверхности, а над водой
 * медленно поднимается дым костра. Чисто клиентский декор: каждый тик проверяется несколько случайных
 * столбцов вокруг игрока, и если в столбце открытая вода биома ashen_river - в нём появляется частица.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public class AshenRiverParticles {
    // Сколько случайных столбцов проверяется за тик и в каком радиусе (блоков) вокруг игрока
    private static final int COLUMNS_PER_TICK = 10;
    private static final int RADIUS = 24;

    // Шанс, что в найденном столбце воды появится частица (за тик): красный пузырь и дым
    private static final float BUBBLE_CHANCE = 0.15F;
    private static final float SMOKE_CHANCE = 0.05F;

    // Пузырь рождается не глубже стольки блоков под поверхностью (и только внутри воды)
    private static final double BUBBLE_MAX_DEPTH = 1.5D;
    // Высота дыма над краем блока воды и скорость подъёма, блоков за тик (у костра 0.07; чем меньше, тем медленнее)
    private static final double SMOKE_HEIGHT_ABOVE_WATER = 1.1D;
    private static final double SMOKE_RISE_SPEED = 0.015D;

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
        int x = center.getX() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
        int z = center.getZ() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
        BlockPos surface = openAshenWater(level, x, z);
        if (surface == null) {
            return;
        }
        if (random.nextFloat() < BUBBLE_CHANCE) {
            spawnBubble(level, surface, random);
        }
        if (random.nextFloat() < SMOKE_CHANCE) {
            spawnSmoke(level, surface, random);
        }
    }

    // Верхний блок столбца, если это источник воды биома ashen_river с воздухом над ним
    @Nullable
    private static BlockPos openAshenWater(ClientLevel level, int x, int z) {
        BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
        FluidState fluid = level.getFluidState(top);
        boolean openWater = fluid.is(FluidTags.WATER) && fluid.isSource() && level.getBlockState(top.above()).isAir();
        return openWater && level.getBiome(top).is(ModBiomes.ASHEN_RIVER) ? top : null;
    }

    private static void spawnBubble(ClientLevel level, BlockPos surface, RandomSource random) {
        double x = surface.getX() + random.nextDouble();
        double z = surface.getZ() + random.nextDouble();
        double y = surface.getY() + 0.5D - random.nextDouble() * BUBBLE_MAX_DEPTH;
        if (!level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)) {
            y = surface.getY() + 0.5D;
        }
        level.addParticle(ModParticles.RED_BUBBLE.get(), x, y, z, 0.0D, 0.0D, 0.0D);
    }

    private static void spawnSmoke(ClientLevel level, BlockPos surface, RandomSource random) {
        double x = surface.getX() + random.nextDouble();
        double z = surface.getZ() + random.nextDouble();
        double y = surface.getY() + 1.0D + SMOKE_HEIGHT_ABOVE_WATER;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0.0D, SMOKE_RISE_SPEED, 0.0D);
    }
}
