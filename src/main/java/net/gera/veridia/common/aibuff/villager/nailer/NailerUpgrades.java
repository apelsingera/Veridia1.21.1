package net.gera.veridia.common.aibuff.villager.nailer;

import net.gera.veridia.common.villager.upgrade.VillagerUpgradeTree;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Дерево улучшений nailer (см. VillagerProfessionScreen/LumberjackUpgrades - тот же движок
 * VillagerUpgradeTree, тут только СОСТАВ дерева и эффекты конкретно для nailer).
 * <p>
 * SPEED - скорость ходьбы, как у остальных деревьев (NailerBuildGoal.effectiveSpeed). HAMMER -
 * скорость стройки (NailerBuildGoal.tickWalkToTarget делит рулон тиков на buildSpeedMultiplier).
 * CLOCK - сдвигает рабочее окно (NailerVillagerConfig.WORK_START/END_TICK), как у lumberjack/
 * farmer. BRAIN - снижает стоимость постройки блоков в очках (см. pointCost/brain.txt) - 4 узла
 * дерева (brain_1..brain_4, из них brain_4 - в level3) = 4 тира ("Уровень 1-4" из брифа), считается
 * напрямую по числу разблокированных BRAIN-узлов (без отдельного "стакания на узел", как у
 * остальных типов - тут сразу целая таблица на тир, см. POINT_COST_BY_TIER).
 * <p>
 * Добавить новый узел - одна строка в статическом блоке: {@code TREE.add("hammer_5", NodeType.HAMMER,
 * "gold_block", x, y)} + рецепт data/veridia/recipe/villager_upgrade/nailer/&lt;id&gt;.json.
 * TRADE_DISCOUNT - скидка на сделки с этим nailer'ом (ModEvents.addCustomTrades умножает стоимость
 * на tradeCostMultiplier). GOLD_BLOCK/TECHNOLOGY (кроме tech_1) - пока пустые узлы без эффекта,
 * задел на будущее (как PACKED_MUD_BRICKS в pointCost). Level2 (tech_1..gold_block, "второй
 * уровень" - имя из background_level2.png) полностью заменяет показ первого экрана, КАК ТОЛЬКО
 * nailer реально достроит всю текущую стену (см. markWallComplete/isLevel2Unlocked,
 * NailerBuildGoal.giveUpForNow) - copper_block у него больше не покупается за ресурсы вручную
 * (см. javadoc markWallComplete), только внутренний ключ разблокировки level2.
 */
public final class NailerUpgrades {
    private NailerUpgrades() {
    }

    public enum NodeType {SPEED, HAMMER, BRAIN, CLOCK, TRADE_DISCOUNT, COPPER_BLOCK, TECHNOLOGY, GOLD_BLOCK}

    /** Категория материала для NailerWallQueue.pointCost - соответствует строкам таблицы в
     * brain.txt. PACKED_MUD_BRICKS пока не встречается в реальной стройке (нет в KNOWN_BLOCKS
     * NailerWallQueue), стоимость на будущее уже заведена, как попросили. */
    public enum MaterialKind {SCRAP_WOOD, PLANKS, SLAB, STAIRS, FENCE_GATE, DOOR, LOG, PACKED_MUD, PACKED_MUD_BRICKS}

    // --- Раскладка дерева на экране - совпадает с остальными профессиями (то же окно 252x140). ---
    private static final int NODE_START_X = 10;
    private static final int NODE_ROW_Y = 58;
    private static final int NODE_COL_SPACING = 28;
    private static final int NODE_BRANCH_OFFSET_Y = 28;

    private static final VillagerUpgradeTree<NodeType> TREE = new VillagerUpgradeTree<>("nailer");

    /** 8 узлов в главном ряду (speed-hammer-brain-hammer-brain-speed-clock-speed) + brain вверх от
     * последнего (speed_3) + "level2" веткой вниз от него же: speed_3 -> trade_discount_1 ->
     * trade_discount_2 -> copper_block (см. мокап - идёт вниз и затем влево, а не вправо, потому
     * что дереву справа уже некуда расти в рамке 252px).
     * <p>
     * "Level2" - НОВЫЙ ряд узлов (tech_1..gold_block ниже), который полностью ЗАМЕНЯЕТ показ
     * первого экрана, как только nailer реально достроит всю стену (см. markWallComplete/
     * isLevel2Unlocked, NailerTreeView.isVisible) - "просто убрать старое и заменить новым", как
     * попросили. copper_block остаётся в дереве как внутренний ключ разблокировки level2 (его
     * unlock-флаг), но больше не покупается вручную за ресурсы - рецепт copper_block.json удалён,
     * villager.unlock() на него вызывает сам код при достройке стены. Координаты level2 намеренно
     * те же самые (тот же ряд NODE_ROW_Y и та же ветка снизу у последнего узла) - старые узлы всё
     * равно скрыты, когда видны новые, так что позиции можно переиспользовать. TECHNOLOGY (кроме
     * tech_1) - пока пустой узел-заглушка (эффект сложный, не готов), GOLD_BLOCK - тоже пока
     * пустой (задел на будущее, как раньше COPPER_BLOCK). */
    public static final List<VillagerUpgradeTree.Node<NodeType>> NODES;

    private static final int LEVEL2_START_INDEX;
    private static final int COPPER_BLOCK_INDEX;
    private static final int TECH2_INDEX;
    private static final int TECH3_INDEX;

    static {
        TREE.add("speed_1", NodeType.SPEED, null, NODE_START_X, NODE_ROW_Y);
        TREE.add("hammer_1", NodeType.HAMMER, "speed_1", NODE_START_X + NODE_COL_SPACING, NODE_ROW_Y);
        TREE.add("brain_1", NodeType.BRAIN, "hammer_1", NODE_START_X + NODE_COL_SPACING * 2, NODE_ROW_Y);
        TREE.add("hammer_2", NodeType.HAMMER, "brain_1", NODE_START_X + NODE_COL_SPACING * 3, NODE_ROW_Y);
        TREE.add("brain_2", NodeType.BRAIN, "hammer_2", NODE_START_X + NODE_COL_SPACING * 4, NODE_ROW_Y);
        TREE.add("speed_2", NodeType.SPEED, "brain_2", NODE_START_X + NODE_COL_SPACING * 5, NODE_ROW_Y);
        TREE.add("clock", NodeType.CLOCK, "speed_2", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y);
        TREE.add("speed_3", NodeType.SPEED, "clock", NODE_START_X + NODE_COL_SPACING * 7, NODE_ROW_Y);
        TREE.add("brain_3", NodeType.BRAIN, "speed_3", NODE_START_X + NODE_COL_SPACING * 7, NODE_ROW_Y - NODE_BRANCH_OFFSET_Y);
        // "Level 2" - см. мокап "Nailer level2": скидка на торговлю (2 узла, копится) + пустой
        // пока узел copper_block (задел на будущее, как PACKED_MUD_BRICKS у pointCost).
        TREE.add("trade_discount_1", NodeType.TRADE_DISCOUNT, "speed_3", NODE_START_X + NODE_COL_SPACING * 7, NODE_ROW_Y + NODE_BRANCH_OFFSET_Y);
        TREE.add("trade_discount_2", NodeType.TRADE_DISCOUNT, "trade_discount_1", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y + NODE_BRANCH_OFFSET_Y);
        TREE.add("copper_block", NodeType.COPPER_BLOCK, "trade_discount_2", NODE_START_X + NODE_COL_SPACING * 5, NODE_ROW_Y + NODE_BRANCH_OFFSET_Y);

        // "Level2" - см. javadoc выше. Порядок ряда по просьбе: tech-hammer-speed-hammer-tech-
        // brain-tech-speed, ветка снизу - gold_block. brain_4 - обычный NodeType.BRAIN (НЕ отдельный
        // тип), просто четвёртый по счёту - поднимает тир pointCost() до 4 (см. POINT_COST_BY_TIER).
        TREE.add("tech_1", NodeType.TECHNOLOGY, "copper_block", NODE_START_X, NODE_ROW_Y);
        TREE.add("hammer_3", NodeType.HAMMER, "tech_1", NODE_START_X + NODE_COL_SPACING, NODE_ROW_Y);
        TREE.add("speed_4", NodeType.SPEED, "hammer_3", NODE_START_X + NODE_COL_SPACING * 2, NODE_ROW_Y);
        TREE.add("hammer_4", NodeType.HAMMER, "speed_4", NODE_START_X + NODE_COL_SPACING * 3, NODE_ROW_Y);
        TREE.add("tech_2", NodeType.TECHNOLOGY, "hammer_4", NODE_START_X + NODE_COL_SPACING * 4, NODE_ROW_Y);
        TREE.add("brain_4", NodeType.BRAIN, "tech_2", NODE_START_X + NODE_COL_SPACING * 5, NODE_ROW_Y);
        TREE.add("tech_3", NodeType.TECHNOLOGY, "brain_4", NODE_START_X + NODE_COL_SPACING * 6, NODE_ROW_Y);
        TREE.add("speed_5", NodeType.SPEED, "tech_3", NODE_START_X + NODE_COL_SPACING * 7, NODE_ROW_Y);
        TREE.add("gold_block", NodeType.GOLD_BLOCK, "speed_5", NODE_START_X + NODE_COL_SPACING * 7, NODE_ROW_Y + NODE_BRANCH_OFFSET_Y);

        NODES = TREE.build();
        LEVEL2_START_INDEX = TREE.indexOf("tech_1");
        COPPER_BLOCK_INDEX = TREE.indexOf("copper_block");
        TECH2_INDEX = TREE.indexOf("tech_2");
        TECH3_INDEX = TREE.indexOf("tech_3");
    }

    /** См. javadoc NODES - level2 полностью заменяет первый экран. */
    public static boolean isLevel2Node(int nodeIndex) {
        return nodeIndex >= LEVEL2_START_INDEX;
    }

    public static boolean isLevel2Unlocked(Villager villager) {
        return TREE.isUnlocked(villager, COPPER_BLOCK_INDEX);
    }

    /** Открывает "второй уровень" (level2) жителю - вызывается ТОЛЬКО из
     * NailerBuildGoal.giveUpForNow, когда nailer реально достроил всю свою текущую очередь стены
     * (не за ресурсы игрока - рецепт copper_block.json удалён, обычный tryUnlock на этот узел
     * больше не пройдёт: hasCost() вернёт false). copper_block тут используется просто как готовый
     * unlock-флаг в дереве (на него уже завязаны isLevel2Unlocked/isLevel2Node), а не как
     * покупаемый узел.
     * <p>
     * isAvailable() (= разблокирован trade_discount_2, а через цепочку prerequisiteId - вообще
     * ВСЯ ветка level1 до него) - обязательное условие: "стены построены" не должно само по себе
     * перепрыгивать весь первый уровень, если игрок ещё не купил остальные узлы как обычно.
     * Вызывается регулярно (при каждом опустошении очереди, раз в QUEUE_RECHECK_COOLDOWN_TICKS) -
     * если стена уже готова, а дерево ещё не докуплено, просто ничего не делает и попробует
     * снова в следующий раз, когда игрок докупит остальное. */
    public static void markWallComplete(Villager villager) {
        if (!TREE.isUnlocked(villager, COPPER_BLOCK_INDEX) && TREE.isAvailable(villager, COPPER_BLOCK_INDEX)) {
            TREE.unlock(villager, COPPER_BLOCK_INDEX);
        }
    }

    /** Первая технология (tech_1, шестерёнка в самом начале level2) - разблокирует апгрейд стены
     * (см. NailerWallQueue.build): нaйлер заменяет уже построенные tier1-шаблоны на их "2"-версии
     * (VillageCalcSettings.WALL_PIECE_UPGRADED и т.п.), вместо того чтобы просто достраивать
     * недостающее - тем же самым основным циклом постройки (target state меняется, а не отдельная
     * "фаза апгрейда" в NailerBuildGoal). */
    public static boolean isTechUnlocked(Villager villager) {
        return TREE.isUnlocked(villager, LEVEL2_START_INDEX);
    }

    /** Вторая технология (tech_2, 5-й узел ряда level2) - разблокирует башни (см.
     * WallPlan.findTowers/NailerWallQueue.build): позиции считаются вместе со стеной, строит их
     * тот же nailer, но только когда больше нечего строить/апгрейдить (см. javadoc build() - башни
     * всегда добавляются в конец очереди). */
    public static boolean isTech2Unlocked(Villager villager) {
        return TREE.isUnlocked(villager, TECH2_INDEX);
    }

    /** Третья технология (tech_3, 7-й узел ряда level2) - разблокирует постройку домов (см.
     * NailerWallQueue.buildHouseEntries/HouseSitePlan): позиции ищутся по тому же scan, что и
     * стена/башни, строит их тот же nailer, поблочно, но ВСЕГДА в конец очереди (после стены и
     * башен) - тем же приёмом на groupIntoGrowableComponents/claimNextPiece, что и у tech_2. */
    public static boolean isTech3Unlocked(Villager villager) {
        return TREE.isUnlocked(villager, TECH3_INDEX);
    }

    // --- Числа эффектов - стакаются с каждым разблокированным узлом своего типа. ---
    public static final float SPEED_WALK_BONUS_PER_NODE = 0.10F;
    public static final float HAMMER_BUILD_SPEED_BONUS_PER_NODE = 0.15F;
    // Часы: без апгрейдов - NailerVillagerConfig.WORK_START/END_TICK (1000-10000), с одним
    // разблокированным clock-узлом - 250-11000, как попросили.
    public static final long CLOCK_START_SHIFT_PER_NODE = 750L;
    public static final long CLOCK_END_SHIFT_PER_NODE = 1000L;
    // Скидка на сделки с этим nailer'ом (см. ModEvents.addCustomTrades) - 2 trade_discount-узла,
    // по 12.5% за узел = 25% суммарно при обоих разблокированных, как попросили ("изумруд делает
    // цены на 25% дешевле"). COPPER_BLOCK пока нигде не считается - пустой узел на будущее.
    public static final float TRADE_DISCOUNT_PER_NODE = 0.125F;

    /** Стоимость постройки в очках по тирам [база, уровень1, уровень2, уровень3, уровень4] - из
     * brain.txt. Тир = число разблокированных BRAIN-узлов (их теперь 4, ровно по числу уровней -
     * см. brain_4 в NODES). */
    private static final Map<MaterialKind, int[]> POINT_COST_BY_TIER = Map.of(
            MaterialKind.SCRAP_WOOD, new int[]{4, 3, 2, 1, 1},
            MaterialKind.PLANKS, new int[]{8, 6, 5, 4, 3},
            MaterialKind.SLAB, new int[]{4, 3, 2, 2, 2},
            MaterialKind.STAIRS, new int[]{6, 5, 4, 4, 3},
            MaterialKind.FENCE_GATE, new int[]{6, 5, 5, 4, 3},
            MaterialKind.DOOR, new int[]{6, 5, 5, 4, 3},
            MaterialKind.LOG, new int[]{16, 14, 12, 10, 8},
            MaterialKind.PACKED_MUD, new int[]{12, 10, 8, 6, 5},
            MaterialKind.PACKED_MUD_BRICKS, new int[]{18, 15, 12, 10, 9}
    );

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

    public static double buildSpeedMultiplier(Villager villager) {
        return 1.0 + HAMMER_BUILD_SPEED_BONUS_PER_NODE * TREE.countUnlockedOfType(villager, NodeType.HAMMER);
    }

    public static long workStartTick(Villager villager) {
        int clockLevel = TREE.countUnlockedOfType(villager, NodeType.CLOCK);
        return NailerVillagerConfig.WORK_START_TICK - CLOCK_START_SHIFT_PER_NODE * clockLevel;
    }

    public static long workEndTick(Villager villager) {
        int clockLevel = TREE.countUnlockedOfType(villager, NodeType.CLOCK);
        return NailerVillagerConfig.WORK_END_TICK + CLOCK_END_SHIFT_PER_NODE * clockLevel;
    }

    /** Вызывается из NailerWallQueue.pointCost - baseTier (0) без апгрейдов, дальше на тир за
     * каждый разблокированный BRAIN-узел, максимум 4 (столько тиров есть в таблице). */
    public static int pointCost(Villager villager, MaterialKind kind) {
        int tier = Math.min(4, TREE.countUnlockedOfType(villager, NodeType.BRAIN));
        return POINT_COST_BY_TIER.get(kind)[tier];
    }

    /** Множитель цены сделок с этим nailer'ом (см. ModEvents.addCustomTrades) - 1.0 без апгрейдов,
     * -12.5% за каждый разблокированный trade_discount-узел (их 2, максимум -25%). */
    public static float tradeCostMultiplier(Villager villager) {
        int level = TREE.countUnlockedOfType(villager, NodeType.TRADE_DISCOUNT);
        return 1.0F - TRADE_DISCOUNT_PER_NODE * level;
    }
}
