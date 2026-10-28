package net.gera.veridia.common.block.spawner.mixin;

import net.gera.veridia.common.block.spawner.RunestoneSpawnerBoost;
import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Что кровавые рунные камни рядом меняют в работе спавнера (см. {@link RunestoneSpawnerBoost}):
 * <ul>
 *   <li>максимум мобов рядом: ванильный serverTick читает поле maxNearbyEntities ровно один раз - когда решает,
 *       не слишком ли много таких мобов вокруг; там вместо поля отдаётся оно плюс бонус. Само поле и NBT
 *       спавнера не меняются;</li>
 *   <li>свет камней (9) не мешает спавну: ванильная проверка места требует темноты, а камни рядом освещают
 *       всю округу спавнера. Если камни рядом, место, не прошедшее проверку из-за света, перепроверяется как для
 *       пробного спавнера (TRIAL_SPAWNER), которому свет безразличен;</li>
 *   <li>в пепельных биомах спавнеры работают в любое время суток: там тоже не требуется темнота, так что днём они
 *       спавнят так же, как ночью (естественный спавн мобов не меняется);</li>
 *   <li>скорость спавна: пока игрок рядом, пауза между спавнами убывает быстрее (подробности у
 *       {@link #veridia$nearPlayerAndSpeedUp});</li>
 *   <li>мобов за одну попытку больше (spawnCount плюс бонус), но лимит мобов вокруг не даёт им накопиться.</li>
 * </ul>
 * Камни считаются раз в RECOUNT_INTERVAL_TICKS тиков, а не каждый тик: подсчёт обходит шар радиуса 9.
 */
@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin {
    @Unique
    private static final int RECOUNT_INTERVAL_TICKS = 20;

    @Shadow
    private int maxNearbyEntities;
    @Shadow
    private int spawnDelay;
    @Shadow
    private int spawnCount;

    @Shadow
    protected abstract boolean isNearPlayer(Level level, BlockPos pos);

    @Unique
    private int veridia$bonusMobs;
    @Unique
    private int veridia$bonusSpawnCount;
    @Unique
    private boolean veridia$runestonesNearby;
    @Unique
    private float veridia$spawnSpeedBonus;
    @Unique
    private float veridia$speedRemainder;
    @Unique
    private long veridia$recountAtTick;

    @Inject(method = "serverTick", at = @At("HEAD"), remap = false)
    private void veridia$refreshBonus(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        long now = level.getGameTime();
        if (now >= veridia$recountAtTick) {
            RunestoneSpawnerBoost.Boost boost = RunestoneSpawnerBoost.forSpawner(level, pos);
            veridia$bonusMobs = boost.maxMobs();
            veridia$runestonesNearby = boost.isActive();
            veridia$spawnSpeedBonus = boost.spawnSpeed();
            veridia$bonusSpawnCount = boost.spawnCount();
            veridia$recountAtTick = now + RECOUNT_INTERVAL_TICKS;
        }
    }

    // Сколько мобов пробуем создать за одну попытку: ванильное поле spawnCount читается только в условии цикла
    // в serverTick, поэтому там отдаём его плюс бонус; поле и NBT не меняются
    @Redirect(
            method = "serverTick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/BaseSpawner;spawnCount:I",
                    opcode = Opcodes.GETFIELD),
            remap = false
    )
    private int veridia$spawnCountWithBonus(BaseSpawner spawner) {
        return this.spawnCount + veridia$bonusSpawnCount;
    }

    // Ванильный serverTick спрашивает isNearPlayer один раз за тик и только если спавнер работает (игрок рядом).
    // Здесь же укорачиваем паузу до следующего спавна: за каждый тик отсчёта списываются ещё veridia$spawnSpeedBonus
    // тиков (дробные копятся). Меньше единицы пауза не становится: последний тик отсчитает сам ванильный код.
    @Redirect(
            method = "serverTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BaseSpawner;isNearPlayer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"),
            remap = false
    )
    private boolean veridia$nearPlayerAndSpeedUp(BaseSpawner spawner, Level level, BlockPos pos) {
        boolean nearPlayer = this.isNearPlayer(level, pos);
        if (nearPlayer) {
            veridia$shortenSpawnDelay();
        }
        return nearPlayer;
    }

    @Unique
    private void veridia$shortenSpawnDelay() {
        veridia$speedRemainder += veridia$spawnSpeedBonus;
        int extraTicks = (int) veridia$speedRemainder;
        veridia$speedRemainder -= extraTicks;
        if (extraTicks > 0 && spawnDelay > 1) {
            spawnDelay = Math.max(1, spawnDelay - extraTicks);
        }
    }

    @Redirect(
            method = "serverTick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/BaseSpawner;maxNearbyEntities:I",
                    opcode = Opcodes.GETFIELD),
            remap = false
    )
    private int veridia$maxNearbyEntitiesWithBonus(BaseSpawner spawner) {
        return this.maxNearbyEntities + veridia$bonusMobs;
    }

    @Redirect(
            method = "serverTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/SpawnPlacements;checkSpawnRules"
                    + "(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/ServerLevelAccessor;"
                    + "Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/util/RandomSource;)Z"),
            remap = false
    )
    private boolean veridia$spawnPlaceIgnoringRunestoneLight(EntityType<?> type, ServerLevelAccessor level,
                                                             MobSpawnType spawnType, BlockPos spawnPos, RandomSource random) {
        if (SpawnPlacements.checkSpawnRules(type, level, spawnType, spawnPos, random)) {
            return true;
        }
        boolean lightIgnored = veridia$runestonesNearby || ModBiomes.isAshBiome(level.getBiome(spawnPos));
        return lightIgnored && SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.TRIAL_SPAWNER, spawnPos, random);
    }
}
