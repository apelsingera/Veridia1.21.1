package net.gera.veridia.common.entity.mutant;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class MutantSpawnHandler {
    private MutantSpawnHandler() {}

    public static final int SPAWN_ATTEMPT_INTERVAL_TICKS = 20 * 30;
    public static final float SPAWN_CHANCE_PER_ATTEMPT = 0.02F;
    public static final double SPAWN_SUPPRESS_RADIUS = 64.0D;
    public static final int SPAWN_MIN_RADIUS = 24;
    public static final int SPAWN_MAX_RADIUS = 44;
    public static final int SPAWN_MAX_LIGHT = 7;

    private static final int PLACEMENT_ATTEMPTS = 12;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.getGameTime() % SPAWN_ATTEMPT_INTERVAL_TICKS != 0L) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (!level.isNight()) return;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) continue;
            if (level.random.nextFloat() >= SPAWN_CHANCE_PER_ATTEMPT) continue;
            trySpawnNear(level, player);
        }
    }
    @SuppressWarnings("deprecation")
    private static void trySpawnNear(ServerLevel level, ServerPlayer player) {
        boolean mutantNearby = !level.getEntitiesOfClass(MutantEntity.class,
                player.getBoundingBox().inflate(SPAWN_SUPPRESS_RADIUS)).isEmpty();
        if (mutantNearby) return;

        RandomSource random = level.random;
        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * (Math.PI * 2.0D);
            double dist = SPAWN_MIN_RADIUS + random.nextDouble() * (SPAWN_MAX_RADIUS - SPAWN_MIN_RADIUS);
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            if (!level.hasChunkAt(x, z)) continue;

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= level.getMinBuildHeight() + 1) continue;
            BlockPos pos = new BlockPos(x, y, z);

            if (level.getMaxLocalRawBrightness(pos) > SPAWN_MAX_LIGHT) continue;

            BlockPos floor = pos.below();
            if (!level.getBlockState(floor).isValidSpawn(level, floor, ModEntities.MUTANT.get())) continue;
            if (!level.noCollision(ModEntities.MUTANT.get().getSpawnAABB(x + 0.5D, y, z + 0.5D))) continue;

            MutantEntity mutant = ModEntities.MUTANT.get().create(level);
            if (mutant == null) return;
            mutant.moveTo(x + 0.5D, y, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            mutant.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
            level.addFreshEntity(mutant);
            return;
        }
    }
}