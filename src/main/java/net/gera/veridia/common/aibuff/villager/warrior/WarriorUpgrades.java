package net.gera.veridia.common.aibuff.villager.warrior;

import net.gera.veridia.common.effect.Bleeding;
import net.gera.veridia.common.villager.upgrade.VillagerUpgradeTree;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Дерево улучшений warrior (см. VillagerProfessionScreen/LumberjackUpgrades - тот же движок
 * VillagerUpgradeTree, тут только СОСТАВ дерева и эффекты конкретно для warrior).
 * <p>
 * HEART - плоская прибавка к MAX_HEALTH (WarriorVillager.ensureStats). ARMOR - поднимает уже
 * надетый шлем/нагрудник/поножи на одну ступень по лесенке материалов кожа -> кольчуга -> железо
 * (WarriorVillagerConfig.HELMET_MATERIAL_LADDER и т.п., WarriorVillager.syncArmorGear), а если
 * материал уже железный - вместо смены добавляет/увеличивает Protection - воин переодевается
 * "живьём", как только апгрейд разблокирован, а не только при новом розыгрыше снаряжения. BLEED -
 * при попадании воина по цели
 * вешает ModEffects.BLEEDING (см. Bleeding) - длительность растёт с каждым узлом. FIRE_ASPECT -
 * шанс поджечь цель при попадании. BRAIN - даёт пассивную регенерацию, если по воину не попадали
 * монстры BRAIN_HEAL_NO_DAMAGE_TICKS (см. WarriorVillager.tickBrainHeal) - замена старой механики
 * "лечится у стропа" (WarriorHealAtJobSiteGoal, удалена).
 * <p>
 * Добавить новый узел - одна строка в статическом блоке: {@code TREE.add("armor_3", NodeType.ARMOR,
 * "brain", x, y)} + рецепт data/veridia/recipe/villager_upgrade/warrior/&lt;id&gt;.json.
 */
public final class WarriorUpgrades {
    private WarriorUpgrades() {
    }

    public enum NodeType {SPEED, HEART, ARMOR, BLEED, FIRE_ASPECT, BRAIN}

    // --- Раскладка дерева на экране - совпадает с lumberjack/farmer (то же окно 252x140). ---
    private static final int NODE_START_X = 10;
    private static final int NODE_ROW_Y = 58;
    // Отступ между узлами - ровно NODE_SIZE(24) + 4px, чтобы соединительная линия рисовалась
    // одним тайлом line_h/line_v (они по 4px), а не двумя-тремя внахлёст.
    private static final int NODE_COL_SPACING = 28;
    private static final int NODE_BRANCH_OFFSET_Y = 28;

    private static final VillagerUpgradeTree<NodeType> TREE = new VillagerUpgradeTree<>("warrior");

    /** 8 узлов в главном ряду (speed-heart-armor-bleed-heart-armor-fire_aspect-brain) + 2 в ветке
     * от fire_aspect - speed вверх, bleed вниз (см. мокап). */
    public static final List<VillagerUpgradeTree.Node<NodeType>> NODES;

    static {
        TREE.add("speed_1", NodeType.SPEED, null, NODE_START_X, NODE_ROW_Y);
        TREE.add("heart_1", NodeType.HEART, "speed_1", NODE_START_X + NODE_COL_SPACING, NODE_ROW_Y);
        TREE.add("armor_1", NodeType.ARMOR, "heart_1", NODE_START_X + NODE_COL_SPACING * 2, NODE_ROW_Y);
        TREE.add("bleed_1", NodeType.BLEED, "armor_1", NODE_START_X + NODE_COL_SPACING * 3, NODE_ROW_Y);
        TREE.add("heart_2", NodeType.HEART, "bleed_1", NODE_START_X + NODE_COL_SPACING * 4, NODE_ROW_Y);
        TREE.add("armor_2", NodeType.ARMOR, "heart_2", NODE_START_X + NODE_COL_SPACING * 5, NODE_ROW_Y);
        TREE.add("fire_aspect", NodeType.FIRE_ASPECT, "armor_2", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y);
        TREE.add("speed_2", NodeType.SPEED, "fire_aspect", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y - NODE_BRANCH_OFFSET_Y);
        TREE.add("bleed_2", NodeType.BLEED, "fire_aspect", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y + NODE_BRANCH_OFFSET_Y);
        TREE.add("brain", NodeType.BRAIN, "fire_aspect", NODE_START_X + NODE_COL_SPACING * 7, NODE_ROW_Y);
        NODES = TREE.build();
    }

    // --- Числа эффектов - стакаются с каждым разблокированным узлом своего типа. ---
    public static final float SPEED_WALK_BONUS_PER_NODE = 0.10F;
    // "+1 полное хп к базовому значению" за каждый heart-узел (их 2) - MAX_HEALTH: 25 -> 26 -> 27.
    public static final float HEART_MAX_HEALTH_BONUS_PER_NODE = 1.0F;
    // Кровотечение: за узел - ещё BLEED_TICK_APPLICATIONS_PER_NODE срабатываний по
    // Bleeding.DAMAGE_PER_TICK раз в Bleeding.DAMAGE_INTERVAL_TICKS. С одним узлом - 3 раза по
    // 0.5хп каждые 3 сек (9 сек всего), как попросили; со вторым - 6 раз (18 сек).
    public static final int BLEED_TICK_APPLICATIONS_PER_NODE = 3;
    // Шанс поджечь цель при попадании - один узел, без стаканья.
    public static final float FIRE_ASPECT_CHANCE = 0.075F;
    public static final int FIRE_ASPECT_IGNITE_SECONDS = 4;
    // Пассивное лечение "мозгом" - один узел, без стаканья. Если по воину не попадали монстры
    // BRAIN_HEAL_NO_DAMAGE_TICKS - лечится на BRAIN_HEAL_AMOUNT, затем таймер запускается заново
    // (то есть лечится раз в 10 сек мирной жизни, а не один раз).
    public static final float BRAIN_HEAL_AMOUNT = 3.0F;
    public static final int BRAIN_HEAL_NO_DAMAGE_TICKS = 20 * 10;

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
        return 1.0 + SPEED_WALK_BONUS_PER_NODE * TREE.countUnlockedOfType(villager, NodeType.SPEED);
    }

    public static float maxHealthBonus(Villager villager) {
        return HEART_MAX_HEALTH_BONUS_PER_NODE * TREE.countUnlockedOfType(villager, NodeType.HEART);
    }

    /** Сколько ступеней апгрейда брони положено воину - см. WarriorVillager.syncArmorGear
     * (каждая ступень поднимает надетый предмет на материал выше или, на железе, +1 Protection). */
    public static int armorTier(Villager villager) {
        return TREE.countUnlockedOfType(villager, NodeType.ARMOR);
    }

    /** 0, если ни одного bleed-узла не разблокировано - тогда Bleeding вообще не вешаем. */
    public static int bleedDurationTicks(Villager villager) {
        int level = TREE.countUnlockedOfType(villager, NodeType.BLEED);
        return BLEED_TICK_APPLICATIONS_PER_NODE * level * Bleeding.DAMAGE_INTERVAL_TICKS;
    }

    public static float fireAspectChance(Villager villager) {
        return TREE.countUnlockedOfType(villager, NodeType.FIRE_ASPECT) > 0 ? FIRE_ASPECT_CHANCE : 0.0F;
    }

    public static boolean hasBrainHeal(Villager villager) {
        return TREE.countUnlockedOfType(villager, NodeType.BRAIN) > 0;
    }
}
