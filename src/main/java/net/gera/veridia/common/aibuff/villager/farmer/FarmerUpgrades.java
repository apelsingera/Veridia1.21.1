package net.gera.veridia.common.aibuff.villager.farmer;

import net.gera.veridia.common.villager.upgrade.VillagerUpgradeTree;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Дерево улучшений farmer (см. VillagerProfessionScreen/LumberjackUpgrades - тот же движок
 * VillagerUpgradeTree, тут только СОСТАВ дерева и эффекты конкретно для farmer).
 * <p>
 * BONE_MEAL - поднимает шанс ComposterBlock.COMPOSTABLES для farmer-жителя (см.
 * ComposterBlockMixin) - именно через это фермер сам себе делает костную муку (ванильный
 * WorkAtComposter.compostItems закидывает в компостер wheat_seeds/beetroot_seeds из своего
 * инвентаря; шанс поднять уровень компостера на 1 деление у ванили 30% для семян - тут выше на
 * BONE_MEAL_CHANCE_BONUS_PER_NODE за узел).
 * <p>
 * RICH_SOIL - шанс при сборе урожая (HarvestFarmlandMixin) улучшить грядку под собой из farmland
 * в farmersdelight:rich_soil_farmland - 0% без апгрейдов (фермер вообще не умеет), дальше растёт
 * с каждым узлом.
 * <p>
 * Добавить новый узел - одна строка в статическом блоке: {@code TREE.add("tree_3", NodeType.TREE,
 * "speed", x, y)} + рецепт data/veridia/recipe/villager_upgrade/farmer/&lt;id&gt;.json.
 */
public final class FarmerUpgrades {
    private FarmerUpgrades() {
    }

    public enum NodeType {BONE_MEAL, RICH_SOIL, TREE, CLOCK, SPEED}

    // --- Раскладка дерева на экране - совпадает с lumberjack (то же окно 252x140). ---
    private static final int NODE_START_X = 10;
    private static final int NODE_ROW_Y = 58;
    // Отступ между узлами - ровно NODE_SIZE(24) + 4px, чтобы соединительная линия рисовалась
    // одним тайлом line_h/line_v (они по 4px), а не двумя-тремя внахлёст.
    private static final int NODE_COL_SPACING = 28;
    private static final int NODE_BRANCH_OFFSET_Y = 28;

    private static final VillagerUpgradeTree<NodeType> TREE = new VillagerUpgradeTree<>("farmer");

    /** 7 узлов в главном ряду (bone_meal-rich_soil-tree-clock-bone_meal-tree-speed) + 2 в ветке от
     * последнего (speed) - clock вверх, rich_soil вниз. */
    public static final List<VillagerUpgradeTree.Node<NodeType>> NODES;

    static {
        TREE.add("bone_meal_1", NodeType.BONE_MEAL, null, NODE_START_X, NODE_ROW_Y);
        TREE.add("rich_soil_1", NodeType.RICH_SOIL, "bone_meal_1", NODE_START_X + NODE_COL_SPACING, NODE_ROW_Y);
        TREE.add("tree_1", NodeType.TREE, "rich_soil_1", NODE_START_X + NODE_COL_SPACING * 2, NODE_ROW_Y);
        TREE.add("clock_1", NodeType.CLOCK, "tree_1", NODE_START_X + NODE_COL_SPACING * 3, NODE_ROW_Y);
        TREE.add("bone_meal_2", NodeType.BONE_MEAL, "clock_1", NODE_START_X + NODE_COL_SPACING * 4, NODE_ROW_Y);
        TREE.add("tree_2", NodeType.TREE, "bone_meal_2", NODE_START_X + NODE_COL_SPACING * 5, NODE_ROW_Y);
        TREE.add("speed", NodeType.SPEED, "tree_2", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y);
        TREE.add("clock_2", NodeType.CLOCK, "speed", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y - NODE_BRANCH_OFFSET_Y);
        TREE.add("rich_soil_2", NodeType.RICH_SOIL, "speed", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y + NODE_BRANCH_OFFSET_Y);
        NODES = TREE.build();
    }

    // --- Числа эффектов - стакаются с каждым разблокированным узлом своего типа. ---
    public static final float SPEED_WALK_BONUS = 0.10F;
    // Часы: без апгрейдов - FarmerVillagerConfig.WORK_START/END_TICK (2000-10000). С двумя
    // разблокированными clock-узлами старт линейно уходит к 0 (по 1000 тиков за узел), конец не
    // меняется (всегда 10000) - в отличие от lumberjack, тут расширяется только с одной стороны.
    public static final long CLOCK_START_SHIFT_PER_NODE = 1000L;
    // Посадка саженца: без апгрейдов 30 сек, -7 сек за каждый разблокированный tree-узел (их 2).
    public static final int PLANT_HOLD_TICKS_BASE = 20 * 30;
    public static final int PLANT_HOLD_TICKS_REDUCTION_PER_NODE = 20 * 7;
    // Компостер: ванильный базовый шанс для семян - 0.3 (30%), тут +10 процентных пунктов за
    // каждый разблокированный bone_meal-узел (их 2) - 0.3 -> 0.4 -> 0.5. Не трогаем сам список
    // предметов, которые фермер несёт в компостер (это ванильный WorkAtComposter, только
    // wheat_seeds/beetroot_seeds) - только шанс сработки уже имеющихся бросков.
    public static final float BONE_MEAL_CHANCE_BONUS_PER_NODE = 0.10F;
    // Улучшение грядки в rich_soil_farmland при сборе урожая: 0% без апгрейдов (не умеет вообще),
    // +15 процентных пунктов за каждый разблокированный rich_soil-узел (их 2) - 0% -> 15% -> 30%.
    public static final float RICH_SOIL_UPGRADE_CHANCE_PER_NODE = 0.15F;

    /** Ничего не делает - вызов нужен только чтобы прогрузить класс (и его static-блок с
     * TREE.add(...)) ДО VillagerUpgradeTree.registerAll(), см. вызов в Veridia. Без этого узлы
     * добавляются в общий ATTACHMENT_TYPES лениво, при первом реальном обращении к дереву - если
     * это происходит после регистрации (например, в тике AI), игра крашится
     * (DeferredRegister закрыт: "Cannot register new entries after RegisterEvent has been fired"). */
    public static void ensureLoaded() {
    }

    public static int prerequisiteIndex(int nodeIndex) {
        return TREE.prerequisiteIndex(nodeIndex);
    }

    public static boolean isUnlocked(Villager villager, int nodeIndex) {
        return TREE.isUnlocked(villager, nodeIndex);
    }

    public static boolean isAvailable(Villager villager, int nodeIndex) {
        return TREE.isAvailable(villager, nodeIndex);
    }

    public static boolean tryUnlock(Player player, Villager villager, int nodeIndex) {
        return TREE.tryUnlock(player, villager, nodeIndex);
    }

    public static ItemStack costDisplayStack(Player player, int nodeIndex) {
        return TREE.costDisplayStack(player, nodeIndex);
    }

    public static double walkSpeedMultiplier(Villager villager) {
        return TREE.countUnlockedOfType(villager, NodeType.SPEED) > 0 ? 1.0 + SPEED_WALK_BONUS : 1.0;
    }

    public static long workStartTick(Villager villager) {
        int clockLevel = TREE.countUnlockedOfType(villager, NodeType.CLOCK);
        return FarmerVillagerConfig.WORK_START_TICK - CLOCK_START_SHIFT_PER_NODE * clockLevel;
    }

    public static long workEndTick(Villager villager) {
        return FarmerVillagerConfig.WORK_END_TICK;
    }

    public static int plantHoldTicks(Villager villager) {
        int treeLevel = TREE.countUnlockedOfType(villager, NodeType.TREE);
        return Math.max(20, PLANT_HOLD_TICKS_BASE - PLANT_HOLD_TICKS_REDUCTION_PER_NODE * treeLevel);
    }

    /** Вызывается из ComposterBlockMixin - baseChance берётся напрямую из ComposterBlock.COMPOSTABLES
     * (ванильные 0.3 для семян), тут добавляем бонус за bone_meal-узлы. */
    public static float boostedComposterChance(Villager villager, float baseChance) {
        if (baseChance <= 0.0F) return baseChance;
        int boneMealLevel = TREE.countUnlockedOfType(villager, NodeType.BONE_MEAL);
        return Math.min(1.0F, baseChance + BONE_MEAL_CHANCE_BONUS_PER_NODE * boneMealLevel);
    }

    /** Вызывается из HarvestFarmlandMixin - 0 без единого разблокированного rich_soil-узла. */
    public static float richSoilUpgradeChance(Villager villager) {
        int richSoilLevel = TREE.countUnlockedOfType(villager, NodeType.RICH_SOIL);
        return Math.min(1.0F, RICH_SOIL_UPGRADE_CHANCE_PER_NODE * richSoilLevel);
    }
}
