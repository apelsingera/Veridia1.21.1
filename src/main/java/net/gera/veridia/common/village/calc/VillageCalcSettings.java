package net.gera.veridia.common.village.calc;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.Set;

public final class VillageCalcSettings {

    private VillageCalcSettings() {
    }
    /**
     * Откуда брать дома, рабочие места и склады. MARKERS - цветные блоки тестовой карты, которые видны сверху; VILLAGE -
     * настоящая деревня, её POI (кровати, котлы, склады под крышей: сверху их не видно, поэтому в MARKERS дом с котлом
     * и складом выпадал из территории, и стена шла вокруг него). AUTO - VILLAGE, если рядом деревня из генерации
     * мира, иначе MARKERS; /calculate source markers|village включает режим принудительно.
     */
    public enum MarkerSource { AUTO, MARKERS, VILLAGE }

    public static final MarkerSource DEFAULT_MARKER_SOURCE = MarkerSource.AUTO;

    public static boolean isVillageMiscBlock(Block block) {
        if (block == Blocks.HAY_BLOCK) return true;
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null || !"farmersdelight".equals(key.getNamespace())) return false;
        String path = key.getPath();
        return path.equals("organic_compost") || path.equals("rich_soil");
    }

    public static boolean isVillageStorageBlock(Block block) {
        return block == net.gera.veridia.common.block.ModBlocks.BUILDING_MATERIALS.get()
                || block == net.gera.veridia.common.block.ModBlocks.FOOD_STORAGE.get();
    }

    public static final int LAMP_PROBE_HEIGHT = 3;

    public static final Set<Block> VILLAGE_PATH_BLOCKS = Set.of(Blocks.DIRT_PATH);


    public static final Set<Block> HOUSE_BLOCKS = Set.of(
            Blocks.LIME_CONCRETE, Blocks.LIME_WOOL, Blocks.GREEN_WOOL, Blocks.GREEN_CONCRETE);
    public static final Set<Block> MISC_BLOCKS = Set.of(
            Blocks.BLUE_WOOL, Blocks.BLUE_CONCRETE);
    public static final Set<Block> PATH_BLOCKS = Set.of(
            Blocks.YELLOW_TERRACOTTA, Blocks.DIRT_PATH);

    public static final int HOUSE_VIRTUAL_WEIGHT = 3;
    public static final int MISC_VIRTUAL_WEIGHT = 2;
    public static final int PATH_VIRTUAL_WEIGHT = 1;


    public static final int HOUSE_AREA = 11;
    public static final int MISC_AREA = 11;
    public static final int STORAGE_VIRTUAL_WEIGHT = 2;
    public static final int STORAGE_AREA = 9;
    public static final boolean STORAGE_NEEDS_ANCHOR = false;
    public static final int PATH_AREA = 2;

    public static final boolean PATH_NEEDS_ANCHOR = true;

    public static final boolean MISC_NEEDS_ANCHOR = false;

    public static final int GAP_FILL_MAX = 4;
    // Заливы территории (вогнутости контура) уже 2*BAY_FILL_RADIUS+1 клеток засыпаются целиком (см.
    // TerritoryShaper.fillBays) - иначе стена огибала каждый такой залив "карманом" (скриншот
    // игрока: некрасивый П-образный изгиб, съедающий место). GAP_FILL_MAX выше закрывает только щели
    // до ~8 блоков по прямой. Больше - стена прямее и деревня просторнее, но стена длиннее уходит в
    // поле; 0 - выключить.
    public static final int BAY_FILL_RADIUS = 12;

    public static final int MAX_STRUCTURE_BLOB = 80;
    // Было 6 - загон/сарай без POI, стоящий чуть в стороне от территории (забор сам по себе
    // считается "структурой", absorbStructures притягивает такие блобы, только если они в этом
    // радиусе от territory), не дотягивался и оставался вне стены.
    public static final int STRUCTURE_ABSORB_RANGE = 10;
    // Обычный лёд (замёрзшие озёра и реки) в этом радиусе от территории деревни (клеток) - земля: по нему ходят
    // мобы, поэтому стена идёт и по нему, а под ней лёд меняется на плотный (IceFoundation). Лёд дальше остаётся
    // "постройкой", как и раньше (большие куски стена обходит, см. absorbStructures). 0 - лёд везде как раньше.
    public static final int ICE_LAND_RANGE = 24;
    // Что кладётся под стену вместо обычного льда, который тает от света (плотный лёд не тает).
    public static final Block ICE_FOUNDATION_BLOCK = Blocks.PACKED_ICE;

    public static final int VILLAGE_CLUSTER_GAP = 48;

    public static final int PREGEN_DETECT_RADIUS = 16;
    // Как далеко от точки /calculate искать деревню, чтобы определить её стиль (см. VillageStyle.of): точка
    // может быть над деревней или сбоку от неё, а не внутри структуры.
    public static final int VILLAGE_STYLE_SEARCH_RADIUS = 96;
    public static final int PREGEN_TICK_INTERVAL = 100;
    // План стены для nailer'а считается, только когда прогружена ВСЯ деревня (её структура из генерации
    // мира) плюс этот запас в блоках вокруг: territory выходит за крайние дома на HOUSE_AREA и
    // дотягивается до загонов на STRUCTURE_ABSORB_RANGE. См. VillageAutoPregen.pollReadyVillage.
    public static final int PREGEN_LOAD_MARGIN = 16;
    // Сколько проверок (по одной раз в PREGEN_TICK_INTERVAL) ждать, пока деревня прогрузится целиком,
    // пока игрок рядом. Потом план считается по тому, что есть: при совсем маленькой прорисовке деревня
    // не влезает целиком никогда, и без этого nailer не строил бы ничего. 24 * 100 тиков = 2 минуты.
    public static final int PREGEN_MAX_WAIT_CHECKS = 24;

    public enum ScanShape { SQUARE, CIRCLE }

    public static final ScanShape DEFAULT_SCAN_SHAPE = ScanShape.SQUARE;
    public static final int DEFAULT_SCAN_RADIUS = 128;
    public static final int MAX_SCAN_RADIUS = 256;

    public static final int SURFACE_PROBE_DEPTH = 2;

    // Ямы, воронки и ущелья (см. TerrainHazards): стена в них не спускается - яма вместе с отступом вырезается
    // из территории, и граница (а с ней стена) идёт вокруг. PIT_MIN_DEPTH - глубина в блоках, с которой
    // котловина/ущелье считается ямой (меньше - обычный склон); PIT_ENCLOSE_RADIUS - как далеко от клетки
    // ищутся края ущелья по каждой оси (шире этого овраг уже не "узкий"); PIT_MARGIN - отступ границы от
    // края ямы в клетках. Глубже порог = меньше срабатываний на холмистой местности. 0 глубины - не вырезать.
    public static final int PIT_MIN_DEPTH = 10;
    // Яма меньше стольких клеток (связная группа по 8 соседям) игнорируется: пещерные входы и воронки в 2-4 клетки
    // стене не мешают, а вырезать вокруг каждой изрезало бы деревню. На реальной карте: одна система провалов в
    // ~1000 клеток и сотни мелких дырок.
    public static final int PIT_MIN_AREA = 25;
    public static final int PIT_ENCLOSE_RADIUS = 8;
    public static final int PIT_MARGIN = 3;
    // У построек отступ от ямы короче: иначе между домом и обрывом не оставалось места для стены (см. VillageScan.pitZoneAroundBuildings).
    // PIT_BUILDING_REACH - на каком расстоянии от постройки (в клетках) отступ уже PIT_MARGIN_NEAR_BUILDINGS, а не PIT_MARGIN.
    public static final int PIT_MARGIN_NEAR_BUILDINGS = 1;
    public static final int PIT_BUILDING_REACH = 6;

    public static final int MAX_TREE_DESCENT = 40;

    public static final int SURFACE_OUTLIER_CAP = 2;
    // Кусок стены (5 клеток) ставится плоским только на ровном месте: если земля под ним перепадает больше чем на
    // столько блоков (самая высокая клетка минус самая низкая), он не ставится, а стена идёт столбиками, каждый на
    // своей земле, - по склону ступенями, без висящих над обрывом подпорок. Ворота не режутся. 2 - как
    // SURFACE_OUTLIER_CAP: то, что кусок ещё сам догоняет подпорками. Больше - реже столбики, но чаще подпорки.
    public static final int SEGMENT_MAX_SPREAD = 2;

    public static final int GATE_POTHOLE_GUARD = 2;

    public static final int GATE_HEIGHT_OFFSET = 1;

    public static final int WALL_HEIGHT_OFFSET = 1;

    public static final int MARKER_POST_HEIGHT = 4;

    public static final int CAPE_TRIM_RADIUS = 4;

    // Было 2 - мало, чтобы стесать "лестницу" на границе territory (естественный результат
    // круглых радиусов-штампов HOUSE/MISC/STORAGE на квадратной сетке). Зубчатая граница рассыпает
    // WallPlan.tileRuns на кучу коротких (<PIECE_LENGTH) кусков, которые становятся одиночными
    // столбиками-заглушками вместо длинных ровных пролётов стены - визуально смотрится как
    // "разбитая" на отдельные обрубки стена. Больше проходов - более плавная (менее ступенчатая)
    // граница и, соответственно, более длинные, ровные куски стены.
    public static final int SMOOTH_PASSES = 5;
    public static final int WATER_BARRIER_RANGE = 8;

    public static final int WATER_MIN_DEPTH = 2;
    // Вода мельче WATER_MIN_DEPTH (ручей, текущая вода на склоне, лужа в 1 блок) водой для стены не считается: она
    // стоит на дне, блоки стены просто заменяют воду. Раньше любая вода, даже один блок, считалась занятой клеткой,
    // и в стене оставалась дыра ровно там, где течёт ручей. Дома это не касается (VillageScan.hasWater).
    public static final int SHALLOW_WATER_MAX_DEPTH = WATER_MIN_DEPTH - 1;
    public static final int MAX_WATER_PROBE = 24;

    public static final int WATER_BRIDGE_MAX = 32;

    // И /calculate result, и постройка nailer'ом (NailerWallQueue.decomposeWaterCell) проверяют
    // этот единственный флаг перед тем, как ставить барьер (WATER_BARRIER_BLOCK) на воде - подводные
    // заборчики по просьбе пользователя выключены, но вся логика (collectWaterGaps/WaterCell)
    // остаётся на месте и включится обратно одной правкой этой константы.
    public static final boolean SEAL_WATER_GAPS = false;
    public static final Block WATER_BARRIER_BLOCK = Blocks.OAK_FENCE;
    public static final int WATER_BARRIER_SUBMERGE = 1;
    public static final int MAX_WATER_BARRIER_HEIGHT = 8;


    // Имена файлов шаблонов стены без папки: папка зависит от деревни (data/veridia/structure/wall/<стиль>/,
    // см. VillageStyle.wall) - plains или taiga, у каждого стиля одинаковый набор файлов.
    public static final String WALL_PIECE = "wallpiece_1";
    public static final String WALL_PIECE_2 = "wallpiece_2";
    public static final String WALL_PIECE_3 = "wallpiece_3";
    public static final String WALL_WINDOW_1 = "wall_window_1";
    public static final String WALL_WINDOW_2 = "wall_window_2";
    public static final String WALL_WINDOW_3 = "wall_window_3";
    public static final String ENTRANCE_PIECE = "entrance_1";

    // "Апгрейд стены" (первая технология nailer'а, см. NailerUpgrades.isTechUnlocked) - 1:1 замена
    // тех же 7 шаблонов на "2"-версии, тот же footprint/вращение/pickPieceFor (та же схема весов и
    // тот же детерминированный сид по позиции сегмента - значит апгрейд предсказуемо заменяет
    // ИМЕННО тот вариант, что уже стоит на месте, а не перевыбирает случайно). Файл
    // wall/plains/2wallpiece_2w пока никуда не подключён - нет однозначной пары среди tier1-шаблонов
    // (не путать с обычным wallpiece_2w).
    public static final String WALL_PIECE_UPGRADED = "2wallpiece_1";
    public static final String WALL_PIECE_2_UPGRADED = "2wallpiece_2";
    public static final String WALL_PIECE_3_UPGRADED = "2wallpiece_3";
    public static final String WALL_WINDOW_1_UPGRADED = "2wall_window_1";
    public static final String WALL_WINDOW_2_UPGRADED = "2wall_window_2";
    public static final String WALL_WINDOW_3_UPGRADED = "2wall_window_3";
    public static final String ENTRANCE_PIECE_UPGRADED = "2entrance_1";

    // Башни (вторая технология nailer'а, tech_2 - см. NailerUpgrades.isTech2Unlocked) - файл в той же
    // папке стиля, что и стены (VillageStyle.tower; см. WallPlan.findTowers для геометрии выбора места).
    public static final String TOWER_1 = "tower_1";
    // Реальный footprint модели (по NBT size) - 3 по одной горизонтали, 4 по другой (см. javadoc
    // WallPlan.findTowers). Проверка ровности рельефа берёт чуть с запасом на все стороны поворота.
    public static final int TOWER_FOOTPRINT_MIN = 3;
    public static final int TOWER_FOOTPRINT_MAX = 4;
    // Допустимый перепад высоты рельефа под башней (как SURFACE_OUTLIER_CAP у сегментов стены, но
    // строже - башня не гнётся под неровности, как обычный 5-блочный шаблон стены). Больше этого -
    // КРАСНАЯ позиция (нельзя ставить вообще, даже в углу).
    public static final int TOWER_FLATNESS_TOLERANCE = 1;
    // Башня не ставится, если рядом обрыв: в пределах TOWER_DROP_CLEARANCE клеток от её основания есть земля
    // ниже основания башни больше чем на TOWER_MAX_DROP блоков (край пропасти, воронки, ущелья) или клетка ямы.
    // Башня у пропасти висела бы над ней (скриншот игрока). Пологий склон 1:1 под башней (3 клетки - 3 блока)
    // проходит, обрыв - нет.
    public static final int TOWER_DROP_CLEARANCE = 3;
    public static final int TOWER_MAX_DROP = 4;
    // Расстановка - НЕ "жадно максимально далеко друг от друга по прямой" (см. историю
    // WallPlan.greedyPickTowers, удалено по отчёту игрока: на изрезанной стене две точки, близкие
    // по прямой на соседних изгибах, всё равно выбирались, хотя по факту обхода вдоль забора были
    // рядом). Вместо этого весь периметр стены (WallPlan.traceBoundary) делится на MAX_TOWERS
    // одинаковых по длине дуг, башня - в центре каждой (WallPlan.pickTowersAlongPerimeter).
    // MIN_TOWER_DISTANCE тут используется только чтобы не наплодить башен на коротком периметре -
    // реальное число дуг = min(MAX_TOWERS, периметр / MIN_TOWER_DISTANCE).
    public static final int MIN_TOWER_DISTANCE = 8;
    public static final int MAX_TOWERS = 6;
    // Башня не встаёт туда, где в пределах стольких клеток от её реального bbox есть блок тропинки
    // (см. WallPlan.nearPath) - иначе глухое тело башни перекрывало вход в деревню (по дампу -
    // 2 башни из 6 встали прямо на ворота). -1 - проверку выключить. Больше - башни дальше от ворот,
    // но на изрезанном тропинками краю деревни место под башню может не найтись вовсе.
    public static final int TOWER_PATH_CLEARANCE = 2;
    public static final int MIN_TOWERS = 1;

    // Дома (третья технология nailer'а, tech_3 - узел уже зарезервирован в NailerUpgrades как
    // заглушка). Пока ТОЛЬКО поиск площадки под дом (см. HouseSitePlan) - сама постройка, расчистка
    // и расширение territory/забора под новый дом - отдельные, более поздние этапы плана.
    // 7x7 - совпадает с NBT size nailer_house.nbt/lumberjack_house.nbt (самые компактные из готовых
    // домиков, см. data/veridia/structure/village/plains/) - какой шаблон реально ставить, ещё не
    // решено, размер площадки для поиска места можно уточнить позже под конкретный выбор.
    public static final int HOUSE_FOOTPRINT_SIZE = 7;
    // Выравнивание площадки под дом (см. HouseEarthworks): уровень дома - медиана высот площадки, и
    // ни одна клетка не должна отличаться от него больше чем на столько блоков (копать вниз / насыпать
    // вверх). Было HOUSE_FLATNESS_TOLERANCE=1 без выравнивания - в обычном (не плоском) мире такого
    // ровного пятна под 14x13 почти не находилось. Больше - дома встают почти везде, но работ больше
    // и откосы вокруг дома шире (ширина откоса = 1 + это значение).
    public static final int HOUSE_MAX_TERRAFORM = 3;
    // "Очень ровная" площадка - все клетки в пределах стольких блоков от уровня дома. Такие места
    // выбираются в первую очередь, даже если они дальше от центра деревни (по просьбе игрока: "ближе
    // к центру, но если есть очень ровная территория - ставить там").
    public static final int HOUSE_VERY_FLAT_TOLERANCE = 1;
    // Очков за каждый срезанный/насыпанный блок земли (nailer делает земляные работы поблочно, за
    // очки - по просьбе игрока). Дом на холме выходит дороже дома на ровном месте.
    public static final int HOUSE_EARTHWORK_COST = 1;
    // Ширина откоса вокруг дома (клеток): на ней земля плавно переходит от уровня дома к природной
    // высоте (см. HouseEarthworks). Было 1 + HOUSE_MAX_TERRAFORM = 4 кольца ступеньками по блоку -
    // по скриншоту игрока выглядело как яма с отвесными стенками. Больше - плавнее, но больше
    // земляных работ (и очков); при 6 уклон даже на перепаде 3 блока - не круче блока на две клетки.
    public static final int HOUSE_APRON_WIDTH = 6;
    // Дорисовка тропинок у нового дома (см. HousePaths): куски тропинок рядом с домом и сам дом
    // соединяются новыми тропинками не длиннее стольких блоков (дальние куски не тянем через всё поле).
    public static final int HOUSE_PATH_MAX_LENGTH = 16;
    // Сколько клеток пустоты должно быть между домом и любым зданием - домами/заборами деревни,
    // стеной, башнями (по просьбе игрока "не касался зданий", см. HouseSitePlan). 1 - вплотную
    // нельзя, но через клетку можно; больше - дома реже, зато вокруг них свободнее проход.
    public static final int HOUSE_BUILDING_CLEARANCE = 1;
    // Расширение стены под дом (третий ярус выбора площадки: очень ровное место у стены, см. HouseSitePlan.chooseAtWall).
    // Сколько клеток территории добавляется вокруг дома: стена встанет не ближе стольких блоков к его стенам. Меньше -
    // стена прижимается к дому, больше - деревня разрастается сильнее.
    public static final int HOUSE_EXPANSION_MARGIN = 3;
    // Сколько лучших мест у стены перебирается (на каждое - пересчёт плана стены), прежде чем сдаться.
    public static final int HOUSE_EXPANSION_ATTEMPTS = 8;
    // Сколько домов максимум строят nailer'ы в одной деревне (строго по одному: следующий -
    // только когда предыдущий достроен, см. NailerWallQueue.currentHouseEntries).
    public static final int MAX_NAILER_HOUSES = 3;
    // Как MIN_TOWER_DISTANCE/MAX_TOWERS - тот же жадный farthest-point-sampling отбор
    // (HouseSitePlan.pickSites), нужен только чтобы дебаг-частицы /calculate не сливались в одно
    // пятно из соседних по сетке кандидатов, реального выбора "где строить" тут ещё нет.
    public static final int MIN_HOUSE_DISTANCE = 12;
    public static final int MAX_HOUSE_SITES = 5;

    // Реальные шаблоны домов, доступные по имени в /calculate building <name> (см. VillageCalc.
    // buildHouse) - НЕ то же самое, что показ площадок выше (тот всегда квадрат HOUSE_FOOTPRINT_SIZE
    // для дебага, этот использует настоящий размер NBT-структуры под конкретное имя). Только имена:
    // файл лежит в папке стиля деревни - data/veridia/structure/village/<стиль>/<имя>.nbt (см.
    // VillageStyle.house; ModVillageHouses - там те же домики подключены к ванильной генерации, тут
    // отдельно к рантайм-постройке nailer'ом).
    public static final Set<String> HOUSE_NAMES = new java.util.TreeSet<>(
            Set.of("warrior_house", "nailer_house", "lumberjack_house"));

    // Какой из HOUSE_NAMES реально строит настоящий nailer (см. NailerWallQueue.
    // buildHouseEntries) - пока единственный вариант, /calculate building <name> отдельно может
    // проверять и другие имена вручную.
    public static final String DEFAULT_HOUSE_TEMPLATE = "warrior_house";
    // Фиксированная цена за ВЕСЬ дом, списывается на первом не-земляном блоке (не по материалу за блок, как у стены - см. javadoc
    // NailerWallQueue.decomposeHouse и обсуждение с игроком: блоков в доме ~30 разных типов, тариф
    // по каждому не нужен, "на будущее появятся альтернативные способы получить points"). ВАЖНО:
    // должна быть МЕНЬШЕ NailerVillagerConfig.POINTS_RESTOCK_TARGET (128) - restock() за один поход
    // на склад пополняет очки ровно до этого потолка и на этом останавливается, так что цена выше
    // него в принципе никогда не набралась бы (nailer вечно ходил бы туда-обратно). 120 - прикидка
    // для старта (с запасом под потолок), подкрутить по тому, как это ощущается в игре.
    public static final int HOUSE_BUILD_COST = 120;

    public static final int WALL_VARIANT_WEIGHT_1 = 6;
    public static final int WALL_VARIANT_WEIGHT_2 = 2;
    public static final int WALL_VARIANT_WEIGHT_3 = 2;

    public static final float WALL_WINDOW_CHANCE = 0.10F;

    public static final Rotation WALL_ROTATION_FIX = Rotation.CLOCKWISE_90;

    public static final int PIECE_LENGTH = 5;
    // Материал заливки (столбики-заглушки, подпорки, фундамент) - хлам-доски породы стиля деревни, см.
    // VillageStyle.scrapWood: дуб на равнинах, ель в тайге.
    public static final int FILLER_HEIGHT = 5;

    // Подпорки под цельным куском стены (Segment) - вся высота Y у него одна на весь PIECE_LENGTH
    // (см. NailerWallQueue.surfaceUnder), а рельеф под конкретной клеткой куска может провалиться
    // ниже неё - без подпорок дно куска повисало бы в воздухе на неровном рельефе. См. javadoc
    // NailerWallQueue.supportColumnsFor - это НЕ часть стены (Entry.support=true), просто чтобы
    // ничего не висело в воздухе, когда мир не плоский.
    public static final int SUPPORT_MAX_DEPTH = 4;
    // Материал подпорки - псевдослучайно по позиции блока (см. NailerWallQueue.pickSupportBlock):
    // 88% хлам-доски стиля (тот же материал, что у столбиков-заглушек), 10% доски стиля, 2% PACKED_MUD.
    public static final float SUPPORT_SECONDARY_CHANCE = 0.10F;
    public static final float SUPPORT_TERTIARY_CHANCE = 0.02F;

    public static final int MAX_FOUNDATION_DEPTH = 6;

    public static final int WALL_EMBED_DEPTH = 1;

    public static final boolean CUT_TREES = true;
    // Предел размера "скелета" одного дерева (брёвна/ветки/корни, связанные между собой) - см. TreeCutter.
    public static final int MAX_TREE_BLOCKS = 4096;
    // Как далеко (в блоках по любой оси) от ствола листва ещё считается кроной ЭТОГО дерева. Листва, которая
    // ближе к стволу соседнего дерева, остаётся ему. Ванильная листва исчезает дальше 6 блоков от бревна,
    // у Dynamic Trees крона не больше - 8 с запасом.
    public static final int TREE_LEAF_REACH = 8;
    // Dynamic Trees: вокруг срубленного дерева на столько блоков дальше TREE_LEAF_REACH блоки соседних деревьев
    // записываются в снимок отката (не удаляясь) - см. TreeCutter.recordNeighbours.
    public static final int TREE_BYSTANDER_MARGIN = 4;
    public static final int WALL_CLEAR_HEIGHT = 6;

    public static final int MIN_WALL_CHAIN = 12;

    // Было 6 - реальные разрывы (крупное дерево/скала между двумя иначе целыми петлями стены,
    // как на скриншоте игрока) регулярно оказывались шире и мост даже не пытался их закрыть -
    // кандидатов на bridgeChain искали только в этом радиусе (WallPlan.dropStrays).
    public static final int STRAY_JOIN_RANGE = 12;

    public static final boolean BRIDGE_WALL_GAPS = true;

    public static final int GATE_WATER_LOOKUP = 10;

    public static final int GATE_PATH_TOUCH_RANGE = 3;

    public static final int MIN_GATE_VIRTUAL_WEIGHT = 1;
    public static final boolean GATES_ON_DIFFERENT_SIDES = false;
    public static final int MIN_GATE_DISTANCE = 10;
}