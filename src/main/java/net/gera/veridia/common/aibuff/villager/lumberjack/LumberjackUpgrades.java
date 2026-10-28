package net.gera.veridia.common.aibuff.villager.lumberjack;

import net.gera.veridia.common.villager.upgrade.VillagerUpgradeTree;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Дерево улучшений lumberjack (см. VillagerProfessionScreen) - сама механика (attachment,
 * разблокировка, доступность по цепочке, стоимость по рецепту) в общем движке
 * VillagerUpgradeTree, тут только СОСТАВ дерева (NODES) и подсчёт эффектов конкретно для
 * lumberjack (digSpeedMultiplier и т.п.).
 * <p>
 * <b>Чтобы добавить новый узел</b> - одна строка в статическом блоке ниже:
 * {@code TREE.add("axe_4", NodeType.AXE, "speed", x, y)} (id/тип/id родителя/позиция на экране).
 * Плюс сам рецепт - data/veridia/recipe/villager_upgrade/lumberjack/&lt;id&gt;.json. Рендер/
 * ссылки/тултип в VillagerProfessionScreen (через LumberjackTreeView) подхватывают новый узел
 * сами, ЕСЛИ тип узла (axe/log/clock/speed) уже существует.
 * <p>
 * Совсем новый ТИП узла - это уже реальная механика: enum NodeType, LumberjackTreeView (иконка +
 * описание), и сам эффект (по образцу digSpeedMultiplier/bonusResourceChance/walkSpeedMultiplier/
 * workStartTick ниже) в месте применения (LumberjackChopGoal и т.п.).
 */
public final class LumberjackUpgrades {
    private LumberjackUpgrades() {
    }

    public enum NodeType {AXE, LOG, CLOCK, SPEED}

    // --- Раскладка дерева на экране. ---
    private static final int NODE_START_X = 10;
    private static final int NODE_ROW_Y = 58;
    // Отступ между узлами - ровно NODE_SIZE(24) + 4px, чтобы соединительная линия рисовалась
    // одним тайлом line_h/line_v (они по 4px), а не двумя-тремя внахлёст.
    private static final int NODE_COL_SPACING = 28;
    private static final int NODE_BRANCH_OFFSET_Y = 28;

    private static final VillagerUpgradeTree<NodeType> TREE = new VillagerUpgradeTree<>("lumberjack");

    /** 7 узлов в главном ряду (axe-log-axe-clock-log-axe-speed) + 2 в ветке от последнего (speed) -
     * clock вверх, log вниз. Добавить ещё узел = ещё одна строка тут. */
    public static final List<VillagerUpgradeTree.Node<NodeType>> NODES;

    static {
        TREE.add("axe_1", NodeType.AXE, null, NODE_START_X, NODE_ROW_Y);
        TREE.add("log_1", NodeType.LOG, "axe_1", NODE_START_X + NODE_COL_SPACING, NODE_ROW_Y);
        TREE.add("axe_2", NodeType.AXE, "log_1", NODE_START_X + NODE_COL_SPACING * 2, NODE_ROW_Y);
        TREE.add("clock_1", NodeType.CLOCK, "axe_2", NODE_START_X + NODE_COL_SPACING * 3, NODE_ROW_Y);
        TREE.add("log_2", NodeType.LOG, "clock_1", NODE_START_X + NODE_COL_SPACING * 4, NODE_ROW_Y);
        TREE.add("axe_3", NodeType.AXE, "log_2", NODE_START_X + NODE_COL_SPACING * 5, NODE_ROW_Y);
        TREE.add("speed", NodeType.SPEED, "axe_3", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y);
        TREE.add("clock_2", NodeType.CLOCK, "speed", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y - NODE_BRANCH_OFFSET_Y);
        TREE.add("log_3", NodeType.LOG, "speed", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y + NODE_BRANCH_OFFSET_Y);
        NODES = TREE.build();
    }

    // --- Числа эффектов - стакаются с каждым разблокированным узлом своего типа. ---
    public static final float AXE_DIG_SPEED_BONUS_PER_NODE = 0.05F;
    public static final float LOG_BONUS_CHANCE_PER_NODE = 0.15F;
    public static final float LOG_BONUS_AMOUNT_PER_NODE = 0.15F;
    public static final float SPEED_WALK_BONUS = 0.10F;
    // Часы: без апгрейдов - LumberjackVillagerConfig.WORK_START/END_TICK (2000-9000), с двумя
    // разблокированными clock-узлами линейно расширяется до 1000-10000, по 500 тиков за узел.
    public static final long CLOCK_START_SHIFT_PER_NODE = 500L;
    public static final long CLOCK_END_SHIFT_PER_NODE = 500L;

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

    public static float digSpeedMultiplier(Villager villager) {
        return 1.0F + AXE_DIG_SPEED_BONUS_PER_NODE * TREE.countUnlockedOfType(villager, NodeType.AXE);
    }

    public static float bonusResourceChance(Villager villager) {
        return LOG_BONUS_CHANCE_PER_NODE * TREE.countUnlockedOfType(villager, NodeType.LOG);
    }

    public static float bonusResourceMultiplier(Villager villager) {
        return 1.0F + LOG_BONUS_AMOUNT_PER_NODE * TREE.countUnlockedOfType(villager, NodeType.LOG);
    }

    public static double walkSpeedMultiplier(Villager villager) {
        return TREE.countUnlockedOfType(villager, NodeType.SPEED) > 0 ? 1.0 + SPEED_WALK_BONUS : 1.0;
    }

    public static long workStartTick(Villager villager) {
        int clockLevel = TREE.countUnlockedOfType(villager, NodeType.CLOCK);
        return LumberjackVillagerConfig.WORK_START_TICK - CLOCK_START_SHIFT_PER_NODE * clockLevel;
    }

    public static long workEndTick(Villager villager) {
        int clockLevel = TREE.countUnlockedOfType(villager, NodeType.CLOCK);
        return LumberjackVillagerConfig.WORK_END_TICK + CLOCK_END_SHIFT_PER_NODE * clockLevel;
    }
}
