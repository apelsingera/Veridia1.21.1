package net.gera.veridia.common.aibuff.villager.nailer;

public final class NailerVillagerConfig {
    private NailerVillagerConfig() {
    }

    // База без апгрейдов - с одним разблокированным CLOCK-узлом (NailerUpgrades) уходит на
    // 250-11000 (см. NailerUpgrades.CLOCK_START_SHIFT_PER_NODE/CLOCK_END_SHIFT_PER_NODE).
    public static final long WORK_START_TICK = 1000L;
    public static final long WORK_END_TICK = 10000L;

    /**
     * Строит ли nailer дома (tech_3) сам. Выключено: стену, её улучшение и башни он по-прежнему строит сам, а дома -
     * только по команде игрока из вкладки BUILD окна жителя (VillagerProfessionTabs; пока она пустая, дома не строятся
     * совсем). /calculate building от этого не зависит.
     */
    public static final boolean AUTONOMOUS_HOUSE_BUILDING = false;

    /** В каком порядке nailer ставит блоки стены внутри одной связной массы (см. NailerWallQueue.orderForBuilding). */
    public enum WallBuildOrder {
        /** Как было: заливка по высоте - нижний слой по всему периметру, потом следующий. */
        LAYERED,
        /** Кусок за куском: каждый кусок снизу вверх целиком, потом ближайший к нему ещё не тронутый. */
        PIECE_BY_PIECE
    }

    public static final WallBuildOrder WALL_BUILD_ORDER = WallBuildOrder.PIECE_BY_PIECE;

    /**
     * Второй проход WallPlan.dropStrays после проверки кусков и столбиков по живому миру - как в /calculate result
     * (VillageCalc.buildWall). Он убирает обрывки и мостит щели, которые появились, когда мир забраковал часть
     * стены. Без него очередь nailer'а получалась дырявее, чем /calculate на той же деревне.
     */
    public static final boolean SECOND_DROP_STRAYS = true;

    public static final int BUILD_TICKS_MIN = 20;
    public static final int BUILD_TICKS_MAX = 20 * 2;
    public static final int BUILD_HIT_PERIOD_TICKS = 20;
    /** Частицы veridia:spark на месте будущего блока во время стройки - раз в 3 секунды. */
    public static final int BUILD_SPARK_PERIOD_TICKS = 20 * 3;
    /** Вспышка veridia:spark в момент, когда блок реально встал: сколько частиц, разброс по осям
     * (в блоках) и скорость разлёта. */
    public static final int PLACE_SPARK_COUNT = 8;
    public static final double PLACE_SPARK_SPREAD = 0.3D;
    public static final double PLACE_SPARK_SPEED = 0.02D;

    public static final double BUILD_MOVE_SPEED = 1D;
    // Было 3.5 - на плотной кладке (уже построенные соседние блоки стены) единственный
    // горизонтальный подход к следующей цели мог физически не пройти в этот радиус, а villager
    // не пытается подходить с другой стороны - просто ждёт вплотную. Пометка "недостижимо" в этом
    // случае не помогает: подход не станет свободнее сам по себе, и через UNREACHABLE_RETRY_
    // COOLDOWN_TICKS цель снова проваливается - "бросает быстро и не восстанавливается". Больший
    // запас по горизонтали резко снижает шанс, что рядом вообще не найдётся ни одной клетки для
    // подхода.
    public static final double BUILD_REACH = 6.0D;
    /** Сколько тиков стоит блок при активном NailerDebugMode (nailer wand) - вместо ролла
     * BUILD_TICKS_MIN..MAX. 1 - минимум, быстрее одного тика поставить блок нельзя. Реach уже
     * бесконечный в этом режиме (см. NailerBuildGoal.tickWalkToTarget), так что ходьба и так не
     * тормозит - единственное, что раньше оставалось "медленным", это сама установка блока. */
    public static final int DEBUG_BUILD_TICKS = 1;

    /**
     * Экономика "очков" (см. NailerUpgrades.pointCost/brain.txt) - расход считается в очках, а не
     * в штуках досок, потому что стоимости по тирам (4/3/2/1 и т.п.) не делятся ровно на цену
     * одной доски. Обмен фиксированный и не растёт с деревом ("Всегда по умолчанию" в брифе) -
     * 1 бревно = 16 очков = 4 доски по 4 очка, 1 палка = 1 очко (4 палки = 1 доска, как и раньше).
     * Очки хранятся как абстрактный счётчик у жителя (persistentData), а не как реальные предметы
     * в инвентаре - см. NailerBuildGoal.
     */
    public static final int POINTS_PER_LOG = 16;
    public static final int POINTS_PER_PLANK = 4;
    public static final int POINTS_PER_STICK = 1;
    public static final int POINTS_RESTOCK_TARGET = 128;

    public static final double RETURN_MOVE_SPEED = 0.8D;
    public static final double DEPOSIT_REACH = 3.5D;
    public static final int HOME_SEARCH_RADIUS = 64;
    public static final int HOME_SEARCH_VERTICAL_RADIUS = 8;
    public static final int HOME_RESCAN_TICKS = 20 * 5;
    public static final int EMPTY_STORAGE_WAIT_TICKS = 20 * 20;

    public static final int MAX_VILLAGE_SEARCH_DISTANCE = 160;
    // Было 60 сек - если nailer бросает очередь (giveUpForNow) не потому что стена реально
    // достроена, а из-за временного затора (см. javadoc BUILD_REACH/UNREACHABLE_RETRY_COOLDOWN_
    // TICKS), минута простоя между попытками выглядит как "ничего не делает". Короче интервал -
    // быстрее сам себя лечит, как только затор рассасывается (например сосед-nailer доделал
    // кусок или подход освободился).
    public static final int QUEUE_RECHECK_COOLDOWN_TICKS = 20 * 15;

    // Было 5 - ровно высота ОДНОГО изолированного столбика-заглушки (FILLER_HEIGHT в
    // VillageCalcSettings), из-за чего единственный ни с чем не соединённый столбик (например,
    // заплатка sealLeaks в стороне от остальной стены) как раз набирал порог и строился как
    // висящий в воздухе обрубок. Порог чуть выше высоты одной колонки - теперь такой столбик
    // сам по себе отсеивается, а настоящие сросшиеся куски стены (которых блоков всегда намного
    // больше) не затрагивает.
    public static final int MIN_COMPONENT_SIZE = 6;

    public static final int WALK_TIMEOUT_TICKS = 20 * 90;
    public static final int GIVE_UP_STUCK_TICKS = 20 * 8;
    public static final double STUCK_MOVE_EPSILON_SQR = 0.0025D;
    /** Сколько недавно недостижимых блоков-целей помнит nailer (см. NailerBuildGoal.markUnreachable) -
     * без этого entryCursor++ на "не достучаться" в WALK_TO_TARGET пропускал блок только в текущем
     * проходе: как только кусок дочищался до конца, tickFindTarget видел кусок незавершённым
     * (пропущенный блок так и не поставлен) и claimNextPiece снова находил его же ПЕРВЫМ (скан
     * всегда с индекса 0) - житель зацикливался на одном и том же недостижимом блоке навсегда,
     * так и не добираясь до остальных кусков стены. Верхняя граница на память (см.
     * UNREACHABLE_RETRY_COOLDOWN_TICKS - основной способ "забыть" блок теперь по времени). */
    public static final int UNREACHABLE_ENTRY_MEMORY = 12;
    /** Через сколько тиков nailer пробует ещё раз блок, ранее помеченный недостижимым (см.
     * NailerBuildGoal.isUnreachable) - раньше пометка снималась, только когда накопится 12 НОВЫХ
     * неудач (UNREACHABLE_ENTRY_MEMORY), а на плотной диагональной цепочке столбиков-заглушек
     * таких неудач за один проход легко набирается больше 12: старые так и не забывались бы до
     * конца непрерывной рабочей смены, хотя причина затора (например уже построенный сосед,
     * перекрывший подход) вполне могла к тому моменту исчезнуть - житель просто навсегда бросал
     * эти блоки, оставляя дыры в стене (жалоба "стены разделены, красные частицы там, где должна
     * быть стена, но nailer не построил"). */
    public static final int UNREACHABLE_RETRY_COOLDOWN_TICKS = 20 * 20;
}
