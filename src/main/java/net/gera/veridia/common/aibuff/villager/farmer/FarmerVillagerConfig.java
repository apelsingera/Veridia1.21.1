package net.gera.veridia.common.aibuff.villager.farmer;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FarmerVillagerConfig {
    private FarmerVillagerConfig() {}

    // База без апгрейдов - разблокированные clock-узлы (FarmerUpgrades) сдвигают старт к 0.
    public static final long WORK_START_TICK = 2000L;
    public static final long WORK_END_TICK = 10000L;

    public static final double FOOD_MOVE_SPEED = 0.5D;
    public static final double FOOD_REACH = 2.5D;
    public static final int HOME_SEARCH_RADIUS = 32;
    public static final int HOME_SEARCH_VERTICAL_RADIUS = 8;
    public static final int HOME_RESCAN_TICKS = 20 * 5;
    /** Пауза перед повторной проверкой food_storage на наличие саженца, когда склад привязан, но
     * пуст - раньше фермер проверял каждые HOME_RESCAN_TICKS (5 сек), из-за чего выглядело как
     * постоянная беготня туда-обратно. Теперь ждёт значительно дольше между проверками. */
    public static final int SEED_CHECK_COOLDOWN_TICKS = 2000;

    public static final double PLANT_MOVE_SPEED = 0.5D;
    public static final double PLANT_REACH = 1.5D;
    public static final int WALL_OFFSET_BLOCKS = 4;
    public static final int PLANT_SPOT_ATTEMPTS = 6;
    public static final int PLANT_RESCAN_TICKS = 20 * 10;
    public static final int MAX_VILLAGE_SEARCH_DISTANCE = 128;
    public static final int WALK_TIMEOUT_TICKS = 20 * 60;
    public static final int GIVE_UP_STUCK_TICKS = 20 * 8;
    public static final double STUCK_MOVE_EPSILON_SQR = 0.0025D;
    public static final int PLANT_HOLD_TICKS = 20 * 2;
    public static final int PLANT_HIT_PERIOD_TICKS = 10;
    public static boolean isPlantable(ItemStack stack) {
        return stack.is(ItemTags.SAPLINGS) || stack.is(Items.CRIMSON_FUNGUS) || stack.is(Items.WARPED_FUNGUS);
    }
}