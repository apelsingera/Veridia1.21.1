package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.PullAttackGoal;
import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.aibuff.WitherBlockBreakGoal;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class LevelZombieHandler {

    static final String LEVEL_KEY = "veridia_zombie_level";

    // Сумма всех весов LevelZombieConfig.LEVEL_SPAWN_WEIGHT - считаем один раз, а не на каждый спавн.
    private static final double TOTAL_LEVEL_SPAWN_WEIGHT = LevelZombieConfig.LEVEL_SPAWN_WEIGHT
            .values().stream().mapToDouble(Double::doubleValue).sum();

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.level().isClientSide()) return;
        if (zombie.getType() != EntityType.ZOMBIE) return;

        // Спавнер, настроенный яйцом уровня, задаёт диапазон уровней сам (см. SpawnerEggLevels)
        applyLevel(zombie, SpawnerEggLevels.takeLevel(zombie).orElseGet(() -> rollLevel(zombie.getRandom())));
    }

    public static void applyLevel(Zombie zombie, int level) {
        zombie.getPersistentData().putInt(LEVEL_KEY, level);

        // "Зомби <римская цифра>" над головой - см. entity.veridia.level_zombie_name в лэнге.
        // Римские цифры считаем сами (не полагаемся на ванильные enchantment.level.N - они
        // определены далеко не до 35), поэтому lang хранит только слово, не сами цифры. Имя есть у
        // ВСЕХ уровней, включая 1 - видимость на экране ограничена только радиусом, см.
        // LevelZombieNameTagHandler (7x7x7 вокруг игрока). Уровень 1 ("обычный" зомби) - БЕЗ
        // римской цифры (просто "Зомби", см. entity.veridia.level_zombie_name_regular) -
        // "Зомби I" для базового уровня выглядело бы странно, цифра нужна только чтобы отличать
        // РЕАЛЬНО разные тиры друг от друга (2 и выше).
        zombie.setCustomName(level > 1
                ? Component.translatable("entity.veridia.level_zombie_name", toRoman(level))
                : Component.translatable("entity.veridia.level_zombie_name_regular"));
        zombie.setCustomNameVisible(true);

        int tier = tierOf(level);
        if (tier > 1) {
            // Ур. 15+ (тиры 2-4) несут ценное снаряжение/способности - обидно, если "имбовая"
            // броня или зомби-маг деспавнятся сами через минуту без урона, стоит игроку отвернуться.
            zombie.setPersistenceRequired();
        }

        LevelZombieConfig.LevelProfile profile = LevelZombieConfig.LEVEL_PROFILES.get(level);

        switch (tier) {
            // Тир 1 (1-14) ничего не делает тут - в отличие от тиров 2-4, у него нет отдельной
            // "базовой" брони: LEVEL_PROFILES САМ полностью описывает броню каждого уровня 1-14
            // (для 1-5 - НАМЕРЕННО неполный набор, см. javadoc LEVEL_PROFILES), applyLevelProfile
            // ниже её и накладывает. Раньше тут ещё стоял applyTier1, который поверх ЛЮБОГО
            // профиля 1-14 насильно надевал полный сет брони (кожа/кольчуга/железо по диапазону
            // уровня) ДО applyLevelProfile - т.к. applyProfileSlot не трогает слот, если профиль
            // его не задаёт, недостающие по профилю куски (например только шлем у 1 уровня)
            // оставались от applyTier1 и никогда не снимались - зомби 1 уровня спавнился в полном
            // кожаном сете вместо одного шлема, и полностью безоружных зомби не бывало вообще.
            case 1 -> {}
            case 2 -> applyTier2(zombie, profile);
            case 3 -> applyTier3(zombie, profile);
            default -> applyTier4(zombie);
        }

        if (profile != null) {
            applyLevelProfile(zombie, profile);
        }

        rollSpeedTag(zombie, level);
    }

    /**
     * "Быстрый" ("speed") / "очень быстрый" ("speed2") level-зомби. Раскатывается ОДИН раз при
     * спавне, взаимоисключающе: сначала speed2, потом speed. Шансы и границы диапазонов уровней -
     * LevelZombieConfig.speed2Chance / speedChance (уровни 30-35 - всегда 0). Это единственный
     * источник этих тегов у level-зомби; явный тег через /summon не трогаем.
     */
    private static void rollSpeedTag(Zombie zombie, int level) {
        float roll = zombie.getRandom().nextFloat();
        float speed2 = LevelZombieConfig.speed2Chance(level);
        if (roll < speed2) {
            zombie.addTag("speed2");
        } else if (roll < speed2 + LevelZombieConfig.speedChance(level)) {
            zombie.addTag("speed");
        }
    }

    /**
     * Кастомный профиль конкретного УРОВНЯ (не тира) - см. LevelZombieConfig.LEVEL_PROFILES.
     * Идёт самым последним, уже после обычной тирной экипировки - каждое заданное в профиле поле
     * перезаписывает соответствующий слот/добавляет тег, не заданные поля (null / пустой tags)
     * просто оставляют то, что уже навесил тир. Каждый слот брони/оружия ещё и проходит через
     * equipChance(slot) (по умолчанию 1.0 = гарантированно) - см. applyProfileSlot.
     */
    private static void applyLevelProfile(Zombie zombie, LevelZombieConfig.LevelProfile profile) {
        applyProfileSlot(zombie, EquipmentSlot.HEAD, profile.head(), profile.equipChance(EquipmentSlot.HEAD));
        applyProfileSlot(zombie, EquipmentSlot.CHEST, profile.chest(), profile.equipChance(EquipmentSlot.CHEST));
        applyProfileSlot(zombie, EquipmentSlot.LEGS, profile.legs(), profile.equipChance(EquipmentSlot.LEGS));
        applyProfileSlot(zombie, EquipmentSlot.FEET, profile.boots(), profile.equipChance(EquipmentSlot.FEET));

        if (profile.offhand() != null) {
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(profile.offhand());
            item.ifPresent(value -> zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(value, profile.offhandCount())));
        }

        if (!profile.mainhandRoll().isEmpty()) {
            applyWeightedMainhand(zombie, profile.mainhandRoll());
        } else {
            applyProfileSlot(zombie, EquipmentSlot.MAINHAND, profile.mainhand(), profile.equipChance(EquipmentSlot.MAINHAND));
        }

        for (LevelZombieConfig.EnchantSpec spec : profile.enchants()) {
            ItemStack stack = zombie.getItemBySlot(spec.slot());
            if (!stack.isEmpty()) {
                enchant(zombie, stack, spec.enchantment(), spec.level());
            }
        }

        profile.dropChances().forEach(zombie::setDropChance);

        for (String tag : profile.tags()) {
            zombie.addTag(tag);
        }
    }

    private static void applyProfileSlot(Zombie zombie, EquipmentSlot slot, ResourceLocation itemId, float equipChance) {
        if (itemId == null) return;
        if (equipChance < 1.0F && zombie.getRandom().nextFloat() >= equipChance) return;
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
        if (item.isEmpty()) return;
        zombie.setItemSlot(slot, new ItemStack(item.get()));
    }

    /** Один случайный вариант из списка (по весу), с его собственным дроп-шансом и тегами - см. LevelZombieConfig.WeightedMainhand. */
    private static void applyWeightedMainhand(Zombie zombie, List<LevelZombieConfig.WeightedMainhand> options) {
        double totalWeight = 0.0;
        for (LevelZombieConfig.WeightedMainhand option : options) {
            totalWeight += option.weight();
        }

        double roll = zombie.getRandom().nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (LevelZombieConfig.WeightedMainhand option : options) {
            cumulative += option.weight();
            if (roll >= cumulative) continue;

            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(option.itemId());
            if (item.isEmpty()) return;

            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item.get()));
            zombie.setDropChance(EquipmentSlot.MAINHAND, option.dropChance());
            for (String tag : option.extraTags()) {
                zombie.addTag(tag);
            }
            return;
        }
    }

    /**
     * Добавляет Goal один раз (идемпотентно - проверяет, что такого класса ещё нет в селекторе),
     * тем же паттерном, что раньше был расписан вручную для каждой Goal по отдельности (см. старые
     * версии этого метода) - вынесено в helper, потому что с новыми тегами уровней 11-35 (см.
     * LevelZombieConfig.LEVEL_PROFILES) таких добавлений стало почти десяток.
     */
    private static void addGoalIfAbsent(Zombie zombie, int priority, Class<? extends Goal> goalType, Supplier<Goal> factory) {
        boolean has = zombie.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> goalType.isInstance(wrapped.getGoal()));
        if (!has) {
            zombie.goalSelector.addGoal(priority, factory.get());
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.level().isClientSide()) return;
        if (!zombie.getPersistentData().contains(LEVEL_KEY)) return;

        int tier = tierOf(zombie.getPersistentData().getInt(LEVEL_KEY));
        Set<String> tags = zombie.getTags();

        // Приоритет 0 - ломание блоков (тир 3 - как раньше, либо тег "breakblocks" у уровней вне
        // тира 3, см. LEVEL_PROFILES 13/19/27/29).
        if (tier == 3 || tags.contains("breakblocks")) {
            addGoalIfAbsent(zombie, 0, WitherBlockBreakGoal.class,
                    () -> new WitherBlockBreakGoal(zombie, LevelZombieConfig.TIER3_BREAK_SPEED_MULTIPLIER,
                            AbilityConfig.VILLAGE_WALL_SLOWDOWN_LEVEL_ZOMBIE));
        }

        // Тир 4 (30-35): у уровня либо своя уникальная способность (см. LEVEL_PROFILES), либо, если
        // ни один из тегов способностей не задан, старый общий "маг" с 3 случайными заклинаниями -
        // как безопасный дефолт на случай будущего тир4-уровня без профиля.
        if (tier == 4) {
            boolean hasOwnAbility = tags.contains("launchcast") || tags.contains("poisoncloud")
                    || tags.contains("laserbeam") || tags.contains("buffaura")
                    || tags.contains("slimesummon") || tags.contains("gianttrigger");
            if (!hasOwnAbility) {
                // Приоритет 1 - строго выше ванильной ZombieAttackGoal (приоритет 2), иначе они
                // дерутся за MOVE/LOOK: у MageZombieCastGoal тоже эти флаги (замах на месте должен
                // явно перехватывать управление, а не спорить за него каждый тик).
                addGoalIfAbsent(zombie, 1, MageZombieCastGoal.class, () -> new MageZombieCastGoal(zombie));
            }
        }

        // "hit" достаётся только через applyWeaponReaction/mainhandRoll (warhammers в руке) - сама
        // Goal и так гейтится по этому тегу (см. PullAttackGoal.canUse), тут только довешиваем её.
        if (tags.contains("hit")) {
            addGoalIfAbsent(zombie, 1, PullAttackGoal.class, () -> new PullAttackGoal(zombie));
        }

        // ==== Уровень 11 ====
        if (tags.contains("heavycore")) {
            addGoalIfAbsent(zombie, 0, HeavyCoreRetreatGoal.class, () -> new HeavyCoreRetreatGoal(zombie));
            addGoalIfAbsent(zombie, 1, HeavyCoreThrowGoal.class, () -> new HeavyCoreThrowGoal(zombie));
        }

        // ==== Уровни 13/19/27/29 - см. блок breakblocks выше (переиспользует WitherBlockBreakGoal) ====

        // ==== Уровень 28 ====
        if (tags.contains("fangs")) {
            addGoalIfAbsent(zombie, 1, FangsSummonGoal.class, () -> new FangsSummonGoal(zombie));
        }

        // ==== Уровень 30 ====
        if (tags.contains("launchcast")) {
            addGoalIfAbsent(zombie, 0, HideBehindAlliesGoal.class, () -> new HideBehindAlliesGoal(zombie));
            addGoalIfAbsent(zombie, 1, LaunchCastGoal.class, () -> new LaunchCastGoal(zombie));
        }

        // ==== Уровень 31 ====
        if (tags.contains("poisoncloud")) {
            addGoalIfAbsent(zombie, 0, HideBehindAlliesGoal.class, () -> new HideBehindAlliesGoal(zombie));
            addGoalIfAbsent(zombie, 1, PoisonCloudGoal.class, () -> new PoisonCloudGoal(zombie));
        }

        // ==== Уровень 32 ====
        if (tags.contains("laserbeam")) {
            addGoalIfAbsent(zombie, 1, LaserBeamGoal.class, () -> new LaserBeamGoal(zombie));
        }

        // ==== Уровень 33 ====
        if (tags.contains("buffaura")) {
            addGoalIfAbsent(zombie, 1, BuffAuraGoal.class, () -> new BuffAuraGoal(zombie));
        }

        // ==== Уровень 34 ====
        if (tags.contains("slimesummon")) {
            addGoalIfAbsent(zombie, 1, SlimeSummonGoal.class, () -> new SlimeSummonGoal(zombie));
        }

        // ==== Уровень 35 ====
        if (tags.contains("gianttrigger")) {
            addGoalIfAbsent(zombie, 1, GiantTriggerGoal.class, () -> new GiantTriggerGoal(zombie));
        }
    }

    /**
     * Дополнительные дропы поверх обычной ванильной добычи зомби (та никак не трогается) - по
     * тиру, см. LevelZombieConfig.TIER1-4_EXTRA_DROPS. Пустой список (по умолчанию тир 1) -
     * ничего дополнительного не падает. Каждый пункт списка - независимый шанс, id ищется мягко
     * (BuiltInRegistries.ITEM.getOptional) - если мод/предмет не установлен, просто пропускаем.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!zombie.getPersistentData().contains(LEVEL_KEY)) return;

        int level = zombie.getPersistentData().getInt(LEVEL_KEY);
        List<LevelZombieConfig.LootEntry> tierTable = switch (tierOf(level)) {
            case 1 -> LevelZombieConfig.TIER1_EXTRA_DROPS;
            case 2 -> LevelZombieConfig.TIER2_EXTRA_DROPS;
            case 3 -> LevelZombieConfig.TIER3_EXTRA_DROPS;
            default -> LevelZombieConfig.TIER4_EXTRA_DROPS;
        };
        // Дроп конкретного уровня СКЛАДЫВАЕТСЯ с тирным, не заменяет его - см. LEVEL_EXTRA_DROPS.
        List<LevelZombieConfig.LootEntry> levelTable = LevelZombieConfig.LEVEL_EXTRA_DROPS.getOrDefault(level, List.of());
        if (tierTable.isEmpty() && levelTable.isEmpty()) return;

        RandomSource random = zombie.getRandom();
        rollDrops(zombie, event, tierTable, random);
        rollDrops(zombie, event, levelTable, random);
    }

    private static void rollDrops(Zombie zombie, LivingDropsEvent event, List<LevelZombieConfig.LootEntry> table, RandomSource random) {
        for (LevelZombieConfig.LootEntry entry : table) {
            if (random.nextFloat() >= entry.chance()) continue;

            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(entry.itemId());
            if (item.isEmpty()) continue;

            int count = entry.minCount() + random.nextInt(entry.maxCount() - entry.minCount() + 1);
            ItemStack stack = new ItemStack(item.get(), count);
            event.getDrops().add(new ItemEntity(zombie.level(), zombie.getX(), zombie.getY(), zombie.getZ(), stack));
        }
    }

    private static int tierOf(int level) {
        if (level <= LevelZombieConfig.TIER1_MAX_LEVEL) return 1;
        if (level <= LevelZombieConfig.TIER2_MAX_LEVEL) return 2;
        if (level <= LevelZombieConfig.TIER3_MAX_LEVEL) return 3;
        return 4;
    }

    /** Прямой взвешенный выбор уровня по LevelZombieConfig.LEVEL_SPAWN_WEIGHT - вес на каждый из 35 уровней отдельно. */
    private static int rollLevel(RandomSource random) {
        double roll = random.nextDouble() * TOTAL_LEVEL_SPAWN_WEIGHT;
        double cumulative = 0.0;
        for (int level = LevelZombieConfig.MIN_LEVEL; level <= LevelZombieConfig.MAX_LEVEL; level++) {
            cumulative += LevelZombieConfig.LEVEL_SPAWN_WEIGHT.getOrDefault(level, 0.0);
            if (roll < cumulative) {
                return level;
            }
        }
        return LevelZombieConfig.MAX_LEVEL;
    }

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private static String toRoman(int number) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (number >= ROMAN_VALUES[i]) {
                number -= ROMAN_VALUES[i];
                result.append(ROMAN_SYMBOLS[i]);
            }
        }
        return result.toString();
    }

    private static void applyTier2(Zombie zombie, LevelZombieConfig.LevelProfile profile) {
        equipDiamondArmor(zombie);
        if (!hasOwnMainhand(profile)) {
            applyWeaponAndReaction(zombie, false);
        }
    }

    private static void applyTier3(Zombie zombie, LevelZombieConfig.LevelProfile profile) {
        equipDiamondArmor(zombie);
        if (!hasOwnMainhand(profile)) {
            applyWeaponAndReaction(zombie, true);
        }
    }

    // Если профиль уровня сам задаёт mainhand (фиксированный или взвешенный ролл) - тирными руками
    // роллить пику/молот/вархаммер незачем: их теги/бонус дальности всё равно относились бы к
    // оружию, которого в руке уже не будет (профиль применяется позже и просто заменит итемстак).
    private static boolean hasOwnMainhand(LevelZombieConfig.LevelProfile profile) {
        return profile != null && (profile.mainhand() != null || !profile.mainhandRoll().isEmpty());
    }

    private static void applyTier4(Zombie zombie) {
        equipVeriditeArmor(zombie);
    }

    /**
     * Веридит - ТОЛЬКО тир 4 (30-35), никогда не дропается (preventDrop=true).
     * Только шлем + нагрудник (без поножей/ботинок): волшебники больше не носят полный веридитовый
     * сет, чтобы не были такими же танковыми, как тир 3 в полном алмазе. Поножи/ботинки явно
     * очищаем - ванильный спавн изредка сам накидывал случайную броню в эти слоты.
     */
    private static void equipVeriditeArmor(Zombie zombie) {
        equipEnchanted(zombie, EquipmentSlot.HEAD, ModItems.VERIDITE_HELMET.get(),
                LevelZombieConfig.IMBA_ARMOR_PROTECTION_LEVEL, true);
        equipEnchanted(zombie, EquipmentSlot.CHEST, ModItems.VERIDITE_CHESTPLATE.get(),
                LevelZombieConfig.IMBA_ARMOR_PROTECTION_LEVEL, true);
        zombie.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        zombie.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
    }

    /** Тиры 2/3 (15-29) - алмаз, а не веридит; дропается как обычная броня (обычный шанс мода). */
    private static void equipDiamondArmor(Zombie zombie) {
        equipArmorSet(zombie,
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
                LevelZombieConfig.TIER23_ARMOR_PROTECTION_LEVEL, false);
    }

    private static void equipArmorSet(Zombie zombie, Item helmet, Item chest, Item legs, Item boots,
                                       int protectionLevel, boolean preventDrop) {
        equipEnchanted(zombie, EquipmentSlot.HEAD, helmet, protectionLevel, preventDrop);
        equipEnchanted(zombie, EquipmentSlot.CHEST, chest, protectionLevel, preventDrop);
        equipEnchanted(zombie, EquipmentSlot.LEGS, legs, protectionLevel, preventDrop);
        equipEnchanted(zombie, EquipmentSlot.FEET, boots, protectionLevel, preventDrop);
    }

    private static void equipEnchanted(Zombie zombie, EquipmentSlot slot, Item item, int protectionLevel, boolean preventDrop) {
        ItemStack stack = new ItemStack(item);
        enchant(zombie, stack, Enchantments.PROTECTION, protectionLevel);
        zombie.setItemSlot(slot, stack);
        if (preventDrop) {
            zombie.setDropChance(slot, 0.0F);
        }
    }

    /**
     * Не тег - три конкретных id (см. LevelZombieConfig.PIKE_ITEM_ID/BATTLE_HAMMER_ITEM_ID/
     * WARHAMMER_ITEM_ID), равновероятно выбираем ОДИН из них и сразу выдаём + применяем
     * соответствующий бонус (мы и так знаем, какую категорию выбрали - незачем потом ещё раз
     * распознавать её через stack.is(tag), как раньше). Если предмета с таким id нет в реестре
     * (мод не установлен или id неверный) - BuiltInRegistries.ITEM.getOptional молча вернёт
     * пусто, ничего не выдаём и не ломаем ничего другого. strong=true (тир 25-29) даёт больший
     * бонус, чем strong=false (тир 15-24) - см. javadoc класса.
     */
    private static void applyWeaponAndReaction(Zombie zombie, boolean strong) {
        ResourceLocation[] ids = {
                LevelZombieConfig.PIKE_ITEM_ID,
                LevelZombieConfig.BATTLE_HAMMER_ITEM_ID,
                LevelZombieConfig.WARHAMMER_ITEM_ID
        };
        int choice = zombie.getRandom().nextInt(ids.length);

        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ids[choice]);
        if (item.isEmpty()) return;

        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item.get()));

        switch (choice) {
            case 0 -> zombie.addTag("longreach"); // пика
            case 1 -> { // боевой молот
                double bonus = strong ? LevelZombieConfig.TIER3_HAMMER_REACH_BONUS : LevelZombieConfig.TIER2_HAMMER_REACH_BONUS;
                AttributeInstance reach = zombie.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
                if (reach != null) {
                    reach.setBaseValue(Math.max(reach.getBaseValue(), bonus));
                }
            }
            default -> zombie.addTag("hit"); // молот (скорость теперь только через rollSpeedTag)
        }
    }

    private static void enchant(LivingEntity contextEntity, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        Holder<Enchantment> holder = contextEntity.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
        stack.enchant(holder, level);
    }
}
