package net.gera.veridia.common.aibuff.villager.lumberjack;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class LumberjackVillagerConfig {
    private LumberjackVillagerConfig() {}

    // База без апгрейдов - разблокированные clock-узлы (LumberjackUpgrades) расширяют этот
    // диапазон до 1000-10000.
    public static final long WORK_START_TICK = 2000L;
    public static final long WORK_END_TICK = 9000L;
    public static final int TREE_SEARCH_RADIUS = 128;
    /**
     * Вертикальный диапазон поиска дерева - асимметричный относительно текущего Y жителя: деревья
     * обычно выше него (холмы, сами стволы), поэтому вверх смотрим дальше, чем вниз. TREE_SEARCH_MIN_Y -
     * абсолютный пол по миру (не относительно жителя) - ниже него не сканируем вообще, даже если
     * житель сам почему-то оказался глубоко под землёй: пещеры/руды нам не интересны, а лишний
     * объём куба на радиусе 128 не бесплатный.
     */
    public static final int TREE_SEARCH_UP = 20;
    public static final int TREE_SEARCH_DOWN = 9;
    public static final int TREE_SEARCH_MIN_Y = 63;
    /**
     * TREE_SEARCH_RADIUS только решает, какие блоки СКАНИРОВАТЬ на предмет дерева - сам поиск
     * пути к найденному дереву ограничен ванильным атрибутом FOLLOW_RANGE (у жителя по умолчанию
     * заметно меньше 128), и PathNavigation просто не строит путь дальше него, даже если дерево
     * уже "найдено" сканом. Поднимаем FOLLOW_RANGE лесорубу отдельно (см.
     * LumberjackVillagerHandler.ensureFollowRange), с запасом над TREE_SEARCH_RADIUS - реальный
     * путь с обходом препятствий обычно длиннее прямой видимости.
     */
    public static final double FOLLOW_RANGE = 144.0D;
    public static final int TREE_RESCAN_TICKS = 20 * 2;
    public static final double CHOP_REACH = 2.2D;
    public static final double CHOP_MOVE_SPEED = 0.6D;
    public static final float LEAF_PATH_MALUS = 12.0F;
    public static final int CHOP_TICKS_PER_LOG = 20 * 25;
    public static final int CHOP_HIT_PERIOD_TICKS = 20;
    public static final int TREE_SCAN_MAX_DISTANCE = 10;
    public static final int MAX_TREE_SCAN_BLOCKS = 128;
    public static final double TREE_CLAIM_RADIUS = 3.0D;
    public static final int LEAF_SEARCH_RADIUS = 2;
    public static final int TREE_LEAF_CLEAR_TICKS = 20 * 3;
    public static final double COLLECT_RADIUS = 12.0D;
    public static final double COLLECT_REACH = 1.2D;
    public static final double COLLECT_MOVE_SPEED = 0.6D;
    public static final int COLLECT_TIMEOUT_TICKS = 20 * 40;
    public static final int WALK_TIMEOUT_TICKS = 20 * 180;
    public static final int GIVE_UP_STUCK_TICKS = 20 * 8;
    // "Стоит на месте" (GIVE_UP_STUCK_TICKS) не ловит житель, который ходит кругами у цели: он же
    // двигается. Поэтому ещё и прогресс: если за GIVE_UP_NO_PROGRESS_TICKS расстояние до цели не стало
    // меньше лучшего на WALK_PROGRESS_MIN_GAIN блока, цель считается недостижимой. Запас большой, чтобы
    // не бросать цель на честном обходе озера/оврага (там расстояние тоже долго не уменьшается).
    public static final int GIVE_UP_NO_PROGRESS_TICKS = 20 * 15;
    public static final double WALK_PROGRESS_MIN_GAIN = 0.5D;
    // Сколько пробовать подойти к листу, который мешает пройти, прежде чем сдаться (лист может быть
    // окружён так, что рядом с ним встать негде).
    public static final int CLEAR_LEAF_APPROACH_TIMEOUT_TICKS = 20 * 5;
    public static final int CLEAR_LEAF_STUCK_TICKS = 10;
    public static final double STUCK_MOVE_EPSILON_SQR = 0.0025D;
    public static final int CLEAR_LEAF_TICKS = 20 * 2;
    public static final int UNREACHABLE_LOG_MEMORY = 12;
    public static final double RETURN_MOVE_SPEED = 0.35D;
    public static final double DEPOSIT_REACH = 2.5D;
    public static final int HOME_SEARCH_RADIUS = 64;
    public static final int HOME_SEARCH_VERTICAL_RADIUS = 8;
    public static final int HOME_RESCAN_TICKS = 20 * 5;
    public static final int MAX_CARRIED_LOGS = 16;
    public static final double DROP_MAGNET_RADIUS = 1.5D;
    public static final double DROP_MAGNET_SPEED = 0.08D;

    private static final TagKey<Block> DYNAMIC_TREES_BRANCHES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("dynamictrees", "branches"));
    private static final TagKey<Block> DYNAMIC_TREES_LEAVES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("dynamictrees", "leaves"));

    public static boolean isChoppableLog(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(DYNAMIC_TREES_BRANCHES);
    }

    public static boolean isTreeLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(DYNAMIC_TREES_LEAVES);
    }

    public static boolean isDynamicTreesBranch(BlockState state) {
        return state.is(DYNAMIC_TREES_BRANCHES);
    }
    public static final int DYNAMIC_TREE_SLOWDOWN = 12;
    public static final float IRON_AXE_EFFICIENCY = 6.0F;
    public static final int DYNAMIC_TREE_MIN_CHOP_TICKS = 20 * 3;
    public static final int DYNAMIC_TREE_FALL_WAIT_TICKS = 20 * 5;
    public static final int DYNAMIC_TREE_MIN_RADIUS = 3;

    public static boolean isBuildingMaterial(ItemStack stack) {
        return stack.is(ItemTags.LOGS) || stack.is(Items.STICK);
    }

    public static boolean isFoodStorageItem(ItemStack stack) {
        return stack.is(ItemTags.SAPLINGS)
                || stack.is(Items.APPLE)
                || stack.is(Items.CRIMSON_FUNGUS)
                || stack.is(Items.WARPED_FUNGUS);
    }
}