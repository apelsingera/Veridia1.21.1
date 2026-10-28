package net.gera.veridia.common.aibuff;

import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.util.ModTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.List;
import java.util.Set;

public final class AbilityConfig {
    // "hit" (PullAttackGoal): замах-телеграф перед рывком. 25 тиков (1.25с) вместо 15 - на границе
    // радиуса весь замах рисуется кольцо частиц, игроку должно хватать времени разорвать линию
    // видимости или уйти за угол.
    public static final int PULL_WARMUP_TICKS = 25;
    public static final List<TagKey<Block>> PICKAXE_WHITELIST_TAGS = List.of(
            BlockTags.MINEABLE_WITH_PICKAXE,
            BlockTags.LEAVES,
            BlockTags.PLANKS,
            BlockTags.FENCES,
            BlockTags.TRAPDOORS,
            BlockTags.FENCE_GATES,
            BlockTags.STAIRS,
            BlockTags.SLABS,
            BlockTags.DIRT,
            BlockTags.SAND
    );
    private static final List<TagKey<Block>> PICKAXE_BLACKLIST_TAGS = List.of(
            Tags.Blocks.ORES,
            ModTags.Blocks.COPPER_BLOCKS
    );
    private static final Set<Block> PICKAXE_BLACKLIST = Set.of(
            Blocks.OBSIDIAN,
            Blocks.CRYING_OBSIDIAN,
            Blocks.ANCIENT_DEBRIS,
            Blocks.NETHERITE_BLOCK,
            Blocks.RESPAWN_ANCHOR,
            Blocks.IRON_DOOR,
            Blocks.DIAMOND_BLOCK,
            Blocks.REINFORCED_DEEPSLATE
    );
    public static boolean isPickaxeWhitelisted(BlockState state) {
        if (PICKAXE_BLACKLIST.contains(state.getBlock())) return false;
        for (TagKey<Block> tag : PICKAXE_BLACKLIST_TAGS) {
            if (state.is(tag)) return false;
        }
        for (TagKey<Block> tag : PICKAXE_WHITELIST_TAGS) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    // "hit" (PullAttackGoal) - подрезано, чтобы способность не была имбовой:
    //  - COOLDOWN 15с -> 20с: реже затягивает игрока в ближний бой посреди толпы.
    //  - RADIUS 6 -> 4: рывок срабатывает только когда моб уже почти вплотную (и меньше диск,
    //    в котором заодно ломается листва).
    //  - DAMAGE 6 -> 3: смысл способности в смещении цели, а не в бонусном уроне.
    //  - STRENGTH 1.8 -> 1.2: рывок короче, проще снова разорвать дистанцию.
    public static final int PULL_COOLDOWN_TICKS = 20 * 20;
    public static final double PULL_RADIUS = 4.0;
    public static final float PULL_DAMAGE = 3.0F;
    public static final double PULL_STRENGTH = 1.2;
    // ==== Яд ====
    public static final int POISON_DURATION_TICKS = 20 * 5;
    public static final int POISON_AMPLIFIER = 1;
    // ==== Стрельба очередью (скелет) ====
    public static final int BURST_ARROW_COUNT = 7;
    public static final int BURST_ARROW_DELAY_TICKS = 4;
    public static final int BURST_RELOAD_TICKS = 20 * 3;
    public static final float BURST_ATTACK_RADIUS = 15.0F;
    public static final double BURST_MOVE_SPEED = 1.0D;
    // ==== Призыв пауков (паук) ====
    public static final int SPIDER_SUMMON_COUNT = 2;
    public static final int SPIDER_SUMMON_COOLDOWN_TICKS = 20 * 10;
    public static final double SPIDER_SUMMON_RADIUS = 0.25;
    // ==== Радиус атаки ====
    public static final double LONG_REACH_DISTANCE = 4.0D;

    public static final List<TagKey<Block>> PULL_BREAKABLE_TAGS = List.of(
            BlockTags.LEAVES

    );

    public static final Set<Block> PULL_BREAKABLE_EXTRA = Set.of(
            Blocks.SWEET_BERRY_BUSH,
            ModBlocks.BLUE_BERRY_BUSH.get(),
            ModBlocks.GOOSE_BERRY_BUSH.get(),
            ModBlocks.CRANBERRY_BUSH.get()
    );

    public static boolean isPullBreakable(BlockState state) {
        for (TagKey<Block> tag : PULL_BREAKABLE_TAGS) {
            if (state.is(tag)) return true;
        }
        return PULL_BREAKABLE_EXTRA.contains(state.getBlock());
    }
    public static final TagKey<EntityType<?>> EXTENDED_ATTACK_REACH_MOBS = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("veridia", "extended_attack_reach")
    );

    // ==== Зомби-шахтёр (тег "pickaxezombie") ====
    // Шанс того, что появившийся зомби (любым способом спавна) станет зомби-шахтёром.
    public static final double PICKAXE_ZOMBIE_SPAWN_CHANCE = 0.025D;

    // ==== Усиленный зомби-шахтёр (тег "strongpickaxezombie") ====
    // Отдельная, более частая доля розыгрыша при спавне (см. ZombieBuffHandler.onFinalizeSpawn) -
    // тоже с киркой и тем же WitherBlockBreakGoal, но крепче, не отбрасывается и ломает быстрее
    // обычного зомби-шахтёра.
    public static final double STRONG_PICKAXE_ZOMBIE_SPAWN_CHANCE = 1.0D / 25.0D;
    public static final double STRONG_PICKAXE_ZOMBIE_MAX_HEALTH = 60.0D;
    public static final double STRONG_PICKAXE_ZOMBIE_KNOCKBACK_RESISTANCE = 1.0D;
    // Множитель в WitherBlockBreakGoal.calculateProgressPerTick - 1.0 значит без искусственного
    // замедления (полная ванильная скорость кирки), тогда как обычный зомби-шахтёр/визер ломают
    // вдвое медленнее (см. WITHER_BREAK_SLOWNESS_MULTIPLIER ниже).
    public static final double STRONG_PICKAXE_ZOMBIE_BREAK_SLOWNESS_MULTIPLIER = 1.0D;

    // ==== Визер-скелет — авто-баф при спавне (по аналогии с PICKAXE_ZOMBIE_SPAWN_CHANCE у зомби
    // выше) ====
    // Срабатывает на ЛЮБОМ заспавнившемся визере-скелете (natural spawn в крепости, яйцо спавна,
    // спаунер) - см. WitherBuffHandler.onFinalizeSpawn. Явный тег через /summon по-прежнему
    // работает как раньше (он читается на каждом тике независимо от того, откуда взялся), это
    // просто ДОПОЛНИТЕЛЬНЫЙ шанс получить один из двух бафов автоматически, без команды.
    // Полноценный "witherbuff" (статы+ломание+магма-кубы+частицы) - редкий (1/30).
    public static final double WITHER_FULL_BUFF_SPAWN_CHANCE = 1.0D / 30.0D;
    // "witherbreaker" - только ломание блоков (то же WitherBlockBreakGoal, включая пиллар-джамп),
    // без статов/меча/КБ-иммунитета, без магма-кубов, без огненной ауры - чаще (1/10).
    public static final double WITHER_BREAKER_SPAWN_CHANCE = 1.0D / 10.0D;

    // ==== Визер-скелет (тег "witherbuff") — статы ====
    public static final double WITHER_MAX_HEALTH = 100.0D;
    // 1.0 = полный иммунитет к отбрасыванию (в т.ч. от зачарования "Отбрасывание") - см.
    // WitherBuffHandler.applyStats, Attributes.KNOCKBACK_RESISTANCE.
    public static final double WITHER_KNOCKBACK_RESISTANCE = 1.0D;
    // Сколько тиков ванильный TargetGoal (mustSee=true) держит цель без прямой видимости, прежде
    // чем сам обнулит mob.setTarget(null) (ванильный дефолт - всего 60 = 3 сек). Обход стены или
    // лесенка на несколько блоков часто занимают дольше 3 секунд без прямой видимости игрока,
    // поэтому дефолт нужно ощутимо увеличить - иначе цель обнуляется раньше, чем моб успевает
    // дойти (см. WitherBuffHandler.extendTargetMemory).
    public static final int WITHER_TARGET_UNSEEN_MEMORY_TICKS = 20 * 60 * 5;
    // Доля входящего урона, которая срезается, пока моб реально ломает блок (WitherBlockBreakGoal.BREAKING_KEY).
    public static final double WITHER_BREAK_DAMAGE_RESIST = 0.8D;

    // ==== Визер-скелет (тег "witherbuff") — огненная аура ====
    public static final double WITHER_AURA_RADIUS = 0.8D;
    public static final double WITHER_AURA_HEIGHT_FACTOR = 0.6D;
    public static final int WITHER_AURA_POINTS = 4;
    public static final int WITHER_AURA_PERIOD_TICKS = 40;
    // ==== Визер-скелет — продвинутый path finding с ломанием блоков на пути к игроку ====
    public static final double WITHER_BREAK_MOVE_SPEED = 1.0D;
    public static final int WITHER_BREAK_STUCK_TICKS = 10;
    public static final double WITHER_BREAK_SLOWNESS_MULTIPLIER = 2.0D;

    // ==== Стены жителей: во сколько раз медленнее ломаются блоки на клетках плана стены деревни ====
    // (WitherBlockBreakGoal + VillageWallLookup; делится итоговая скорость ломания, поверх обычных
    // множителей выше). Уровневые зомби/хаски с бонусом "ломает блоки" слабее, поэтому стена держит их
    // меньше; зомби-шахтёры (обычные и усиленные) и визер-скелеты - сильнее.
    public static final double VILLAGE_WALL_SLOWDOWN_LEVEL_ZOMBIE = 4.0D;
    public static final double VILLAGE_WALL_SLOWDOWN_BREAKER = 6.0D;
    // A*-поиск маршрута (WitherPathfinder): стоимость прохода через ломаемый блок (в единицах
    // "шагов ходьбы") - чем выше, тем длиннее обход вокруг стены должен предпочитаться ломанию.
    // Сравнение идёт по СУММЕ (шаги ходьбы + сломанные блоки * эта стоимость), а не по одной
    // ходьбе или одному ломанию - поэтому длинный путь пешком (например, извилистый коридор)
    // проиграет короткому пути с парой сломанных блоков, если разница в шагах больше этой цены.
    public static final double WITHER_BREAK_PATH_BREAK_COST = 12.0D;
    // Насколько за пределы прямоугольника (моб-цель) может уходить поиск маршрута, в блоках.
    public static final int WITHER_BREAK_SEARCH_MARGIN = 10;
    // Максимум узлов A*, чтобы поиск не мог зависнуть на сложном/большом уровне.
    // Без диагоналей обходные маршруты длиннее, поэтому бюджет чуть выше.
    public static final int WITHER_BREAK_SEARCH_NODE_BUDGET = 800;
    // После того как очередь на ломание опустела, сколько тиков моб сам идёт по уже открытому
    // пути, не отдавая MOVE/LOOK флаги обратно атакующей Goal (иначе мгновенная передача флагов
    // дёргает голову между игроком и последним ломаемым блоком, если моб ещё не успел дойти).
    // Важно: пока это время идёт, A* НЕ перезапускается - иначе на грани "маршрут почти открыт"
    // поиск от тика к тику может по-разному решать, нужен ли ещё один блок на ломание, и это
    // само по себе дёргает взгляд между игроком и случайным блоком где-то позади.
    public static final int WITHER_BREAK_FINAL_APPROACH_TICKS = 100;
    // Как часто на этом самостоятельном подходе заново подтверждать moveTo к живой цели
    // (игрок мог отойти) - НЕ пересчитывает план, только освежает направление движения.
    public static final int WITHER_BREAK_IDLE_REPLAN_TICKS = 20;
    // На каком расстоянии (в блоках) моб уже может начать ломать текущий блок шага, а не просто
    // идти к нему - не даёт прогрессу ломания тикать, пока моб физически ещё далеко.
    public static final double WITHER_BREAK_REACH = 3.0D;
    // Отдельная от A* проверка потолка (WitherBlockBreakGoal.findCeilingObstruction): насколько
    // выше моба должна быть цель, прежде чем вообще имеет смысл проверять блок прямо над головой -
    // ниже этого порога обычный 1-блочный шаг вверх и так прекрасно решается самим A* (лесенка),
    // отдельная проверка тут не нужна.
    public static final double WITHER_BREAK_CEILING_MIN_TARGET_HEIGHT_DIFF = 2.0D;
    // ==== Визер-скелет — пиллар-джамп (яма/ловушка без прохода вбок) ====
    // Включается ТОЛЬКО когда WitherPathfinder вообще не находит маршрута (ни пешего, ни с
    // ломанием) - см. WitherBlockBreakGoal.refreshPlan/isWorthPillaring. Сначала моб подходит к ближайшей к цели
    // клетке (без ломания блоков), потом, если цель выше и рядом, лезет вверх, подставляя блоки под себя - как игрок
    // вылезает из ямы без прохода вбок. Насколько выше моба должна быть цель, чтобы это имело смысл:
    public static final double WITHER_PILLAR_MIN_HEIGHT_DIFF = 2.0D;
    // Насколько далеко от основания столба цель может стоять по горизонтали (в клетках, по большей из осей): дальше моб
    // на высоте цели всё равно её не достанет, столб в стороне ничего не даёт - он не строится, моб только подходит ближе.
    public static final int WITHER_PILLAR_MAX_GAP = 2;
    // Предохранитель: максимум блоков за один заход пиллара - не даёт мобу лезть в небо
    // бесконечно, если цель всё равно недостижима (например, игрок в лодке высоко в воздухе).
    // Было 12: на высокой башне моб не добирался до верха и бросал стройку на полпути.
    public static final int WITHER_PILLAR_MAX_BLOCKS = 24;
    // Копать или строиться (WitherBlockBreakGoal.shouldBuildInsteadOfDig): если цель выше, а ломание всех блоков маршрута
    // займёт дольше этого (тиков; по инструменту, твёрдости и стене деревни), моб вместо прорубания "лестницы" в толще
    // блоков идёт к основанию и строит столб. Один блок камня моб ломает около 45 тиков, столб - около 12 тиков на блок.
    public static final int WITHER_DIG_TOO_LONG_TICKS = 20 * 12;
    // Если моб уже копал в этом заходе дольше этого (тиков), а цель всё ещё выше - тоже переходит к стройке (оценка плана
    // могла ошибиться или цель ушла выше).
    public static final int WITHER_DIG_GIVE_UP_TICKS = 20 * 20;
    // Сколько тиков подряд подъём может обходиться без нового блока, прежде чем считается застрявшим (прыжки на месте
    // под потолком, в воде, в узкой щели) и заход заканчивается. Один прыжок с блоком занимает около 12 тиков.
    public static final int WITHER_PILLAR_STALL_TICKS = 20 * 4;
    public static final Block WITHER_PILLAR_BLOCK = Blocks.COBBLESTONE;
    // Принудительный пиллар (WitherBlockBreakGoal.FORCE_PILLAR_KEY): сколько ударов игрока подряд
    // (см. WitherBuffHandler.onIncomingDamage) заставляют моба полезть к игроку, не дожидаясь маршрута - но только если
    // игрок бьёт сверху и рядом (WITHER_PILLAR_MIN_HEIGHT_DIFF, WITHER_PILLAR_MAX_GAP), иначе запрос сбрасывается.
    // Поднимается моб до высоты игрока, а не на фиксированное число блоков.
    public static final int WITHER_PILLAR_TRIGGER_HITS = 5;
    // Окно "подряд": если с прошлого удара игрока прошло больше тиков, счётчик начинается заново. Раньше счётчик
    // не сбрасывался никогда, и пятый удар за всю жизнь моба (хоть через час) включал принудительный пиллар.
    // 0 - не сбрасывать (как было).
    public static final int WITHER_PILLAR_HIT_WINDOW_TICKS = 20 * 10;
    // ==== Визер-скелет — призыв взрывных магма-кубов ====
    public static final int WITHER_MAGMA_SUMMON_COUNT = 2;
    public static final int WITHER_MAGMA_SUMMON_COOLDOWN_TICKS = 20 * 30;
    public static final double WITHER_MAGMA_SUMMON_RADIUS = 1.0D;
    public static final int WITHER_MAGMA_SUMMON_SIZE = 1;
    // ==== Взрывной магма-куб (спавнится визер-скелетом) ====
    public static final int EXPLOSIVE_MAGMA_FUSE_TICKS = 20 * 2;
    public static final float EXPLOSIVE_MAGMA_EXPLOSION_POWER = 1.5F;
    public static final int EXPLOSIVE_MAGMA_PARTICLE_PERIOD_TICKS = 5;
    public static final int EXPLOSIVE_MAGMA_PARTICLE_COUNT = 3;

    // ==== Злой житель (тег "angryvillager") ====
    public static final int VILLAGER_ANGER_DURATION_TICKS = 20 * 60 * 3; // 3 минуты, потом прощает
    public static final double VILLAGER_MOVE_SPEED = 1.0D;
    // Ближний бой (железная мотыга)
    public static final double VILLAGER_MELEE_ENGAGE_RADIUS = 4.0D; // ближе этого - переключается на MeleeAttackGoal
    public static final double VILLAGER_ATTACK_DAMAGE = 6.0D; // база ATTACK_DAMAGE (было 2.0 по умолчанию) + мотыга
    // Отступление после удара (цикл "подошёл-ударил-отбежал-кинул")
    public static final int VILLAGER_RETREAT_DURATION_TICKS = 20 * 2;
    public static final double VILLAGER_RETREAT_DISTANCE = 8.0D;
    // Дальний бой (heavy_projectile)
    public static final double VILLAGER_PROJECTILE_ATTACK_RADIUS = 12.0D;
    public static final int VILLAGER_PROJECTILE_COOLDOWN_TICKS = 20 * 40; // перезарядка - 1 ядро раз в 40 секунд
    public static final float VILLAGER_PROJECTILE_DAMAGE = 7.0F; // было 4 (общее значение снаряда игрока)
    // Охрана сундуков: житель злится на игрока, открывшего сундук в этом радиусе от жителя.
    public static final double VILLAGER_CHEST_GUARD_RADIUS = 8.0D;

    // ==== Житель-оружейник — лечит железного голема слитком (по профессии weaponsmith) ====
    // Вешается на ЛЮБОГО жителя-оружейника (VillagerProfession.WEAPONSMITH) - см. HealerVillagerHandler.
    // Пока рядом есть раненый железный голем, житель достаёт железный слиток, бежит к нему и "чинит"
    // на HEALER_VILLAGER_HEAL_AMOUNT HP раз в HEALER_VILLAGER_HEAL_PERIOD_TICKS - как игрок слитком в
    // IronGolem.mobInteract, только слиток при этом не тратится.
    public static final double HEALER_VILLAGER_SEARCH_RADIUS = 16.0D;   // радиус поиска раненого голема
    public static final double HEALER_VILLAGER_GIVE_UP_RADIUS = 32.0D;  // дальше этого житель бросает голема
    public static final double HEALER_VILLAGER_HEAL_REACH = 2.5D;       // с этой дистанции уже чинит, ближе не подходит
    public static final double HEALER_VILLAGER_MOVE_SPEED = 1.0D;       // как у злого жителя (VILLAGER_MOVE_SPEED)
    public static final int HEALER_VILLAGER_HEAL_PERIOD_TICKS = 20 * 3; // один "ремонт" раз в 3 секунды
    public static final float HEALER_VILLAGER_HEAL_AMOUNT = 10.0F;      // HP за ремонт (ванильный игрок даёт 25 и тратит слиток)

    // ==== Level-зомби, уровень 11 (тег "heavycore") - бросок heavy_projectile + отступление ====
    // Переиспользует HeavyProjectileEntity (тот же снаряд, что и у злого жителя) и ту же схему
    // "подошёл-кинул-отбежал", см. VillagerProjectileThrowGoal/VillagerRetreatGoal.
    public static final double HEAVY_CORE_THROW_RADIUS = 10.0D;
    public static final int HEAVY_CORE_COOLDOWN_TICKS = 20 * 6;
    public static final float HEAVY_CORE_DAMAGE = 5.0F;
    public static final int HEAVY_CORE_RETREAT_TICKS = 20 * 2;
    public static final double HEAVY_CORE_RETREAT_DISTANCE = 6.0D;
    public static final double HEAVY_CORE_MOVE_SPEED = 1.0D;

    // ==== Level-зомби, уровень 28 (тег "fangs") - призыв "крокодилов" (EvokerFangs) кольцом ====
    public static final int FANGS_SUMMON_COOLDOWN_TICKS = 20 * 20;
    public static final int FANGS_SUMMON_COUNT = 6;
    public static final double FANGS_SUMMON_RADIUS = 3.0D;

    // ==== Level-зомби, уровни 30-35 (тир 4) - у каждого своя уникальная способность ====
    // Общий для 30/31 "спрятаться за союзником" - см. HideBehindAlliesGoal.
    public static final double HIDE_SEARCH_RADIUS = 12.0D;
    public static final double HIDE_BEHIND_DISTANCE = 1.5D;
    public static final int HIDE_DURATION_TICKS = 20 * 4;

    // Уровень 30 (тег "launchcast") - отдача+подкидывание.
    public static final int LAUNCH_CAST_COOLDOWN_TICKS = 20 * 10;
    public static final int LAUNCH_CAST_WINDUP_TICKS = 20;
    public static final double LAUNCH_CAST_RANGE = 8.0D;
    public static final double LAUNCH_CAST_KNOCKBACK = 2.2D;
    public static final double LAUNCH_CAST_UP_VELOCITY = 1.1D;

    // Уровень 31 (тег "poisoncloud") - ядовитое облако.
    public static final int POISON_CLOUD_COOLDOWN_TICKS = 20 * 30;
    public static final int POISON_CLOUD_WINDUP_TICKS = 30;
    public static final double POISON_CLOUD_MAX_RADIUS = 15.0D;
    // Было 0.25 (5 блоков/сек) - примерно скорость бега игрока, плюс фора в WINDUP_TICKS на замах,
    // так что игрок реально мог просто убежать от расширяющегося облака и ни разу не отравиться.
    public static final double POISON_CLOUD_EXPAND_PER_TICK = 1.0D;
    public static final int POISON_CLOUD_EFFECT_DURATION_TICKS = 20 * 6;
    public static final int POISON_CLOUD_LIFETIME_TICKS = 20 * 8;

    // Уровень 32 (тег "laserbeam") - самонаводящийся луч.
    public static final int LASER_COOLDOWN_TICKS = 20 * 30;
    public static final double LASER_RANGE = 9.0D;
    public static final float LASER_DAMAGE_PER_TICK = 0.75F; // 15 урона/сек
    public static final double LASER_TURN_RATE_DEGREES = 6.0D; // "медленнее чем игрок бегает"
    public static final int LASER_ACTIVE_TICKS = 20 * 4;

    // Уровень 33 (тег "buffaura") - аура баффа на соседних злых мобов.
    public static final int BUFF_AURA_COOLDOWN_TICKS = 20 * 60;
    public static final int BUFF_AURA_CAST_TICKS = 20 * 2;
    public static final double BUFF_AURA_RADIUS = 5.0D;
    public static final int BUFF_AURA_DURATION_TICKS = 20 * 30;
    public static final double BUFF_AURA_ATTACK_SPEED_BONUS = 0.25D; // +25%

    // Уровень 34 (тег "slimesummon") - призыв взрывных слизней, не вредящих злым мобам.
    public static final int SLIME_SUMMON_COOLDOWN_TICKS = 20 * 30;
    public static final int SLIME_SUMMON_COUNT = 2;
    public static final double SLIME_SUMMON_RADIUS = 1.0D;
    public static final int SLIME_FUSE_TICKS = 20 * 2;
    public static final double SLIME_EXPLOSION_RADIUS = 3.0D;
    public static final float SLIME_EXPLOSION_DAMAGE = 6.0F;

    // Уровень 35 (тег "gianttrigger") - временно увеличивает случайного соседнего зомби.
    public static final int GIANT_TRIGGER_COOLDOWN_TICKS = 20 * 30;
    public static final double GIANT_TRIGGER_RADIUS = 10.0D;
    public static final int GIANT_DURATION_TICKS = 20 * 30;
    public static final float GIANT_SCALE = 3.0F;
    public static final double GIANT_MOVE_SPEED_MULTIPLIER = 0.6D;
    public static final double GIANT_DAMAGE_MULTIPLIER = 1.5D;

    private AbilityConfig(){}
}