package net.gera.veridia.common.block.crop;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * В Пепельных землях все культуры, кроме конопли, погибают: через WITHER_DELAY_TICKS после посадки они ломаются без
 * выпадения предметов. Посаженное игроком (и другим существом, ставящим блок событием) ловится при посадке; всё остальное
 * (посажено жителем, осталось в мире) гибнет при первой же попытке вырасти, так что засеять пепельную землю
 * чем-то, кроме конопли, не получится.
 * <p>
 * Отсчёт хранится в памяти: если сервер перезапустили за эти секунды, культура погибнет при попытке вырасти.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class AshCropWither {
    // ---------- Настройки ----------
    // Через сколько тиков после посадки культура ломается (20 тиков = 1 секунда, 300 = 15 секунд)
    private static final int WITHER_DELAY_TICKS = 300;

    private record Pending(BlockPos pos, long dueTick) {
    }

    // Очередь погибающих культур по измерениям; срок у всех на одно время позже посадки, поэтому очередь уже по порядку
    private static final Map<ResourceKey<Level>, ArrayDeque<Pending>> QUEUES = new HashMap<>();

    private AshCropWither() {
    }

    @SubscribeEvent
    public static void onPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && withers(level, event.getPos(), event.getPlacedBlock())) {
            QUEUES.computeIfAbsent(level.dimension(), key -> new ArrayDeque<>())
                    .add(new Pending(event.getPos().immutable(), level.getGameTime() + WITHER_DELAY_TICKS));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ArrayDeque<Pending> queue = QUEUES.get(level.dimension());
        while (queue != null && !queue.isEmpty() && queue.peek().dueTick() <= level.getGameTime()) {
            wither(level, queue.poll().pos());
        }
    }

    // Не посаженное при нас, а выросшее: гибнет, не успев вырасти
    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level && withers(level, event.getPos(), event.getState())) {
            wither(level, event.getPos());
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        QUEUES.clear();
    }

    private static void wither(ServerLevel level, BlockPos pos) {
        if (withers(level, pos, level.getBlockState(pos))) {
            level.destroyBlock(pos, false);
        }
    }

    // Культура ли это, которая гибнет здесь: любая, кроме конопли, в пепельном биоме
    private static boolean withers(ServerLevel level, BlockPos pos, BlockState state) {
        return isCrop(state) && !state.is(ModBlocks.HEMP_CROP.get()) && ModBiomes.isAshBiome(level.getBiome(pos));
    }

    private static boolean isCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock || state.getBlock() instanceof StemBlock || state.is(BlockTags.CROPS);
    }
}
