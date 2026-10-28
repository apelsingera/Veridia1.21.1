package net.gera.veridia.common.aibuff.villager.warrior;

import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.compat.spartanweaponry.SpartanWeaponryCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WarriorVillagerConfig {
    private WarriorVillagerConfig() {}

    public static final double MAX_HEALTH = 25.0D;
    /** Ванильное здоровье жителя - к нему возвращаем воина, когда снимаем профессию. */
    public static final double DEFAULT_MAX_HEALTH = 20.0D;
    /**
     * Возвращать воину рабочее место, если ванильный PoiCompetitorScan отобрал его в пользу
     * другого жителя. Без этого один строп держит ровно одного воина, каким бы ни был
     * ModVillagers.STROP_MAX_WORKERS.
     */
    public static final boolean SHARE_JOB_SITE = true;
    /**
     * Временная диагностика: раз в DEBUG_JOB_SITE_PERIOD_TICKS пишет в лог состояние каждого
     * жителя (профессия, POTENTIAL_JOB_SITE, JOB_SITE) и свободные билеты ближайших стропов.
     * Выключить, когда разберёмся, почему станок берёт только один.
     */
    public static final boolean DEBUG_JOB_SITE = false;
    public static final int DEBUG_JOB_SITE_PERIOD_TICKS = 100;

    /**
     * Как часто чинить счётчик билетов у стропов. Проверка разнесена по жителям, не пиковая.
     * <p>
     * AcquirePoi у каждого жителя срабатывает раз в 20-40 тиков, поэтому окно, в котором билет
     * должен быть свободен, узкое. При периоде 100 житель почти всегда приходил к пустому стропу -
     * отсюда и "работает, но медленно".
     */
    public static final int TICKET_REPAIR_PERIOD_TICKS = 20;
    /** В каком радиусе от жителя искать стропы для починки. */
    public static final int TICKET_REPAIR_RADIUS = 16;
    /** В каком радиусе считать воинов, реально закреплённых за стропом. */
    public static final int TICKET_HOLDER_RADIUS = 24;
    public static final double MOVE_SPEED = 0.7D;
    public static final double TARGET_RADIUS = 16.0D;
    public static final double TARGET_GIVE_UP_RADIUS = 24.0D;
    public static final int TARGET_RESCAN_TICKS = 10;
    public static final boolean ATTACK_CREEPERS = false;
    public static final double MELEE_REACH = 3.0D;
    public static final double PIKE_REACH = AbilityConfig.LONG_REACH_DISTANCE;

    public static final double BOW_ATTACK_RADIUS = 15.0D;
    public static final double BOW_RETREAT_RADIUS = 5.0D;
    public static final double BOW_RETREAT_DISTANCE = 8.0D;
    public static final int BOW_DRAW_TICKS = 20;
    public static final int BOW_COOLDOWN_TICKS = 20;
    public static final float BOW_VELOCITY = 1.6F;
    public static final float BOW_INACCURACY = 10.0F;

    /**
     * Сколько ударов нужно получить, чтобы воин перешёл в атаку на обидчика.
     * Один случайный удар прощается - иначе воина нельзя было бы задеть по неосторожности.
     */
    public static final int GRUDGE_HITS_TO_ANGER = 2;
    /** Если нового удара не было столько тиков, счётчик обнуляется. 1200 = минута. */
    public static final int GRUDGE_FORGET_TICKS = 1200;
    /** Сколько воин помнит обиду после того, как счётчик сработал. */
    public static final int GRUDGE_ANGER_TICKS = 1200;
    /** Отвечать ли на удары железного голема так же, как на удары игрока. */
    public static final boolean GRUDGE_INCLUDE_GOLEMS = true;

    public static final boolean SUPPRESS_PANIC = true;
    public static final boolean FRIENDLY_FIRE_IMMUNE = true;
    public static final float EQUIPMENT_DROP_CHANCE = 0.0F;

    public record GearOption(int weight, @Nullable ResourceLocation moddedId, Item fallback) {
        public static GearOption vanilla(int weight, Item item) {
            return new GearOption(weight, null, item);
        }

        public static GearOption spartan(int weight, ResourceLocation moddedId, Item fallback) {
            return new GearOption(weight, moddedId, fallback);
        }

        public Item resolve() {
            if (moddedId != null) {
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(moddedId);
                if (item.isPresent()) {
                    return item.get();
                }
            }
            return fallback;
        }
    }
    public record WeaponCategory(int weight, boolean ranged, boolean longReach, List<GearOption> tiers) {
        public static WeaponCategory melee(int weight, GearOption... tiers) {
            return new WeaponCategory(weight, false, false, List.of(tiers));
        }
    }

    public static final List<WeaponCategory> WEAPONS = List.of(
            // --20%-- рапира
            WeaponCategory.melee(20,
                    GearOption.spartan(40, SpartanWeaponryCompat.IRON_RAPIER, Items.IRON_SWORD),
                    GearOption.spartan(60, SpartanWeaponryCompat.COPPER_RAPIER, Items.STONE_SWORD)),
            // --10%-- боевой молот
            WeaponCategory.melee(10,
                    GearOption.spartan(30, SpartanWeaponryCompat.IRON_WARHAMMER, Items.IRON_AXE),
                    GearOption.spartan(70, SpartanWeaponryCompat.COPPER_WARHAMMER, Items.STONE_AXE)),
            // --10%-- пика: длинная дистанция удара (тег longreach)
            new WeaponCategory(10, false, true, List.of(
                    GearOption.spartan(40, SpartanWeaponryCompat.IRON_PIKE, Items.IRON_SWORD),
                    GearOption.spartan(60, SpartanWeaponryCompat.COPPER_PIKE, Items.STONE_SWORD))),
            // --20%-- меч
            WeaponCategory.melee(20,
                    GearOption.vanilla(40, Items.IRON_SWORD),
                    GearOption.vanilla(60, Items.STONE_SWORD)),
            // --10%-- топор
            WeaponCategory.melee(10,
                    GearOption.vanilla(100, Items.IRON_AXE)),
            // --30%-- лук
            new WeaponCategory(30, true, false, List.of(
                    GearOption.vanilla(100, Items.BOW)))
    );

    /** Начальный розыгрыш шлема/нагрудника/поножей при появлении воина - не зависит от дерева
     * улучшений (тем и апгрейд - шлем ПОСЛЕ него на ступень выше, а не переигран заново). */
    public static final List<GearOption> HELMETS = List.of(
            GearOption.vanilla(10, Items.IRON_HELMET),
            GearOption.vanilla(35, Items.CHAINMAIL_HELMET),
            GearOption.vanilla(55, Items.LEATHER_HELMET)
    );

    public static final List<GearOption> CHESTPLATES = List.of(
            GearOption.vanilla(10, Items.IRON_CHESTPLATE),
            GearOption.vanilla(35, Items.CHAINMAIL_CHESTPLATE),
            GearOption.vanilla(55, Items.LEATHER_CHESTPLATE)
    );

    public static final List<GearOption> LEGGINGS = List.of(
            GearOption.vanilla(30, Items.CHAINMAIL_LEGGINGS),
            GearOption.vanilla(70, Items.LEATHER_LEGGINGS)
    );

    /**
     * Лесенка материалов для узла ARMOR (WarriorVillager.syncArmorGear) - кожа -> кольчуга ->
     * железо. Апгрейд поднимает УЖЕ надетый предмет на одну ступень вверх по этому списку; если
     * предмет уже максимального тира (железо) или не опознан (не из этого списка) - вместо смены
     * материала добавляется/увеличивается Protection. Поножи включают IRON_LEGGINGS, хотя в
     * начальном розыгрыше (LEGGINGS выше) железных поножей нет - "максимум" у апгрейда всё равно
     * железо, как у шлема/нагрудника. Сапоги в лесенку не входят (в мокапе/armor.txt их нет).
     */
    public static final List<Item> HELMET_MATERIAL_LADDER =
            List.of(Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET);
    public static final List<Item> CHESTPLATE_MATERIAL_LADDER =
            List.of(Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE);
    public static final List<Item> LEGGINGS_MATERIAL_LADDER =
            List.of(Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS);
    /**
     * Сдвиг задаётся в пикселях модели (1px = 1/16 блока) относительно жителя, а не относительно
     * осей кадра рендера - handRight/handForward/handUp означают ровно то, что написано.
     * Углы - в градусах.
     */
    public record HeldItemTransform(float handRight, float handForward, float handUp,
                                    float armsPitch,
                                    float pitch, float yaw, float roll,
                                    float scale) {
        public static HeldItemTransform base() {
            return new HeldItemTransform(0.0F, 0.0F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        }
        /** Right - вбок, forward - от груди наружу, up - вверх (минус вниз, -4 = куб сложенных кистей). */
        public HeldItemTransform hand(float right, float forward, float up) {
            return new HeldItemTransform(right, forward, up, armsPitch, pitch, yaw, roll, scale);
        }
        public HeldItemTransform armsPitch(float degrees) {
            return new HeldItemTransform(handRight, handForward, handUp, degrees, pitch, yaw, roll, scale);
        }
        public HeldItemTransform rotate(float pitchDegrees, float yawDegrees, float rollDegrees) {
            return new HeldItemTransform(handRight, handForward, handUp, armsPitch, pitchDegrees, yawDegrees, rollDegrees, scale);
        }
        public HeldItemTransform scale(float factor) {
            return new HeldItemTransform(handRight, handForward, handUp, armsPitch, pitch, yaw, roll, factor);
        }
    }
    public static final HeldItemTransform DEFAULT_HELD_ITEM_TRANSFORM = HeldItemTransform.base();
    public static final Map<String, HeldItemTransform> HELD_ITEM_TRANSFORMS = Map.ofEntries(
            Map.entry(SpartanWeaponryCompat.IRON_RAPIER.toString(), HeldItemTransform.base()),
            Map.entry(SpartanWeaponryCompat.COPPER_RAPIER.toString(), HeldItemTransform.base()),
            Map.entry(SpartanWeaponryCompat.IRON_WARHAMMER.toString(), HeldItemTransform.base()
                    .hand(0.0F, 0.0F, -6.0F)),
            Map.entry(SpartanWeaponryCompat.COPPER_WARHAMMER.toString(), HeldItemTransform.base()
                    .hand(0.0F, 0.0F, -6.0F)),
            Map.entry(SpartanWeaponryCompat.IRON_PIKE.toString(), HeldItemTransform.base()
                    .armsPitch(43.0F)
                    .hand(0.0F, 6.0F, -4.0F)
                    .rotate(45F,0,0).scale(0.5F)),
            Map.entry(SpartanWeaponryCompat.COPPER_PIKE.toString(), HeldItemTransform.base()
                    .armsPitch(43.0F)
                    .hand(0.0F, 6.0F, -4.0F)
                    .rotate(45F,0,0).scale(0.5F)),
            Map.entry("minecraft:iron_sword", HeldItemTransform.base()),
            Map.entry("minecraft:stone_sword", HeldItemTransform.base()),
            Map.entry("minecraft:iron_axe", HeldItemTransform.base()),
            Map.entry("minecraft:stone_axe", HeldItemTransform.base()),
            Map.entry("minecraft:bow", HeldItemTransform.base()
                    .hand(0.0F, 0.0F, -8.0F)
                    .rotate(-135.0F, 0.0F, 180.0F)
                    .scale(1F))
    );

    public static HeldItemTransform heldItemTransform(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return HELD_ITEM_TRANSFORMS.getOrDefault(id.toString(), DEFAULT_HELD_ITEM_TRANSFORM);
    }

    public static final List<GearOption> BOOTS = List.of(
            GearOption.vanilla(30, Items.CHAINMAIL_BOOTS),
            GearOption.vanilla(70, Items.LEATHER_BOOTS)
    );
}