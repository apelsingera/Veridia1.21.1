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
import net.minecraft.world.entity.monster.Husk;
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

/**
 * Полная копия LevelZombieHandler, но для Кадавра (Husk) - см. LevelHuskConfig. Husk extends
 * Zombie в ванили, поэтому Goal-классы (WitherBlockBreakGoal, MageZombieCastGoal и т.д.) не
 * дублируются - они уже принимают Zombie и одинаково хорошо работают с Husk. LEVEL_KEY - свой,
 * отдельный от LevelZombieHandler (иначе onTick/onLivingDrops зомби-хендлера начал бы видеть и
 * кадавров, раз Husk instanceof Zombie тоже true).
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public class LevelHuskHandler {

    static final String LEVEL_KEY = "veridia_husk_level";

    private static final double TOTAL_LEVEL_SPAWN_WEIGHT = LevelHuskConfig.LEVEL_SPAWN_WEIGHT
            .values().stream().mapToDouble(Double::doubleValue).sum();

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Husk husk)) return;
        if (husk.level().isClientSide()) return;
        if (husk.getType() != EntityType.HUSK) return;

        // Спавнер, настроенный яйцом уровня, задаёт диапазон уровней сам (см. SpawnerEggLevels)
        applyLevel(husk, SpawnerEggLevels.takeLevel(husk).orElseGet(() -> rollLevel(husk.getRandom())));
    }

    public static void applyLevel(Husk husk, int level) {
        husk.getPersistentData().putInt(LEVEL_KEY, level);

        // "Кадавр <римская цифра>" над головой - см. entity.veridia.level_husk_name в лэнге.
        // Уровень 1 ("обычный" кадавр) - БЕЗ римской цифры (просто "Кадавр", см.
        // entity.veridia.level_husk_name_regular). Видимость на экране ограничена кубом
        // 7x7x7 вокруг игрока - см. LevelHuskNameTagHandler.
        husk.setCustomName(level > 1
                ? Component.translatable("entity.veridia.level_husk_name", toRoman(level))
                : Component.translatable("entity.veridia.level_husk_name_regular"));
        husk.setCustomNameVisible(true);

        int tier = tierOf(level);
        if (tier > 1) {
            husk.setPersistenceRequired();
        }

        LevelHuskConfig.LevelProfile profile = LevelHuskConfig.LEVEL_PROFILES.get(level);

        switch (tier) {
            case 1 -> {}
            case 2 -> applyTier2(husk, profile);
            case 3 -> applyTier3(husk, profile);
            default -> applyTier4(husk);
        }

        if (profile != null) {
            applyLevelProfile(husk, profile);
        }

        rollSpeedTag(husk, level);
    }

    /**
     * "Быстрый" ("speed") / "очень быстрый" ("speed2") level-кадавр. Раскатывается ОДИН раз при
     * спавне, взаимоисключающе: сначала speed2, потом speed. Шансы и границы диапазонов уровней -
     * LevelHuskConfig.speed2Chance / speedChance (уровни 30-35 - всегда 0). Это единственный
     * источник этих тегов у level-кадавра; явный тег через /summon не трогаем.
     */
    private static void rollSpeedTag(Husk husk, int level) {
        float roll = husk.getRandom().nextFloat();
        float speed2 = LevelHuskConfig.speed2Chance(level);
        if (roll < speed2) {
            husk.addTag("speed2");
        } else if (roll < speed2 + LevelHuskConfig.speedChance(level)) {
            husk.addTag("speed");
        }
    }

    private static void applyLevelProfile(Husk husk, LevelHuskConfig.LevelProfile profile) {
        applyProfileSlot(husk, EquipmentSlot.HEAD, profile.head(), profile.equipChance(EquipmentSlot.HEAD));
        applyProfileSlot(husk, EquipmentSlot.CHEST, profile.chest(), profile.equipChance(EquipmentSlot.CHEST));
        applyProfileSlot(husk, EquipmentSlot.LEGS, profile.legs(), profile.equipChance(EquipmentSlot.LEGS));
        applyProfileSlot(husk, EquipmentSlot.FEET, profile.boots(), profile.equipChance(EquipmentSlot.FEET));

        if (profile.offhand() != null) {
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(profile.offhand());
            item.ifPresent(value -> husk.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(value, profile.offhandCount())));
        }

        if (!profile.mainhandRoll().isEmpty()) {
            applyWeightedMainhand(husk, profile.mainhandRoll());
        } else {
            applyProfileSlot(husk, EquipmentSlot.MAINHAND, profile.mainhand(), profile.equipChance(EquipmentSlot.MAINHAND));
        }

        for (LevelHuskConfig.EnchantSpec spec : profile.enchants()) {
            ItemStack stack = husk.getItemBySlot(spec.slot());
            if (!stack.isEmpty()) {
                enchant(husk, stack, spec.enchantment(), spec.level());
            }
        }

        profile.dropChances().forEach(husk::setDropChance);

        for (String tag : profile.tags()) {
            husk.addTag(tag);
        }
    }

    private static void applyProfileSlot(Husk husk, EquipmentSlot slot, ResourceLocation itemId, float equipChance) {
        if (itemId == null) return;
        if (equipChance < 1.0F && husk.getRandom().nextFloat() >= equipChance) return;
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
        if (item.isEmpty()) return;
        husk.setItemSlot(slot, new ItemStack(item.get()));
    }

    private static void applyWeightedMainhand(Husk husk, List<LevelHuskConfig.WeightedMainhand> options) {
        double totalWeight = 0.0;
        for (LevelHuskConfig.WeightedMainhand option : options) {
            totalWeight += option.weight();
        }

        double roll = husk.getRandom().nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (LevelHuskConfig.WeightedMainhand option : options) {
            cumulative += option.weight();
            if (roll >= cumulative) continue;

            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(option.itemId());
            if (item.isEmpty()) return;

            husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item.get()));
            husk.setDropChance(EquipmentSlot.MAINHAND, option.dropChance());
            for (String tag : option.extraTags()) {
                husk.addTag(tag);
            }
            return;
        }
    }

    private static void addGoalIfAbsent(Husk husk, int priority, Class<? extends Goal> goalType, Supplier<Goal> factory) {
        boolean has = husk.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> goalType.isInstance(wrapped.getGoal()));
        if (!has) {
            husk.goalSelector.addGoal(priority, factory.get());
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Husk husk)) return;
        if (husk.level().isClientSide()) return;
        if (!husk.getPersistentData().contains(LEVEL_KEY)) return;

        int tier = tierOf(husk.getPersistentData().getInt(LEVEL_KEY));
        Set<String> tags = husk.getTags();

        if (tier == 3 || tags.contains("breakblocks")) {
            addGoalIfAbsent(husk, 0, WitherBlockBreakGoal.class,
                    () -> new WitherBlockBreakGoal(husk, LevelHuskConfig.TIER3_BREAK_SPEED_MULTIPLIER,
                            AbilityConfig.VILLAGE_WALL_SLOWDOWN_LEVEL_ZOMBIE));
        }

        if (tier == 4) {
            boolean hasOwnAbility = tags.contains("launchcast") || tags.contains("poisoncloud")
                    || tags.contains("laserbeam") || tags.contains("buffaura")
                    || tags.contains("slimesummon") || tags.contains("gianttrigger");
            if (!hasOwnAbility) {
                addGoalIfAbsent(husk, 1, MageZombieCastGoal.class, () -> new MageZombieCastGoal(husk));
            }
        }

        if (tags.contains("hit")) {
            addGoalIfAbsent(husk, 1, PullAttackGoal.class, () -> new PullAttackGoal(husk));
        }

        if (tags.contains("heavycore")) {
            addGoalIfAbsent(husk, 0, HeavyCoreRetreatGoal.class, () -> new HeavyCoreRetreatGoal(husk));
            addGoalIfAbsent(husk, 1, HeavyCoreThrowGoal.class, () -> new HeavyCoreThrowGoal(husk));
        }

        if (tags.contains("fangs")) {
            addGoalIfAbsent(husk, 1, FangsSummonGoal.class, () -> new FangsSummonGoal(husk));
        }

        if (tags.contains("launchcast")) {
            addGoalIfAbsent(husk, 0, HideBehindAlliesGoal.class, () -> new HideBehindAlliesGoal(husk));
            addGoalIfAbsent(husk, 1, LaunchCastGoal.class, () -> new LaunchCastGoal(husk));
        }

        if (tags.contains("poisoncloud")) {
            addGoalIfAbsent(husk, 0, HideBehindAlliesGoal.class, () -> new HideBehindAlliesGoal(husk));
            addGoalIfAbsent(husk, 1, PoisonCloudGoal.class, () -> new PoisonCloudGoal(husk));
        }

        if (tags.contains("laserbeam")) {
            addGoalIfAbsent(husk, 1, LaserBeamGoal.class, () -> new LaserBeamGoal(husk));
        }

        if (tags.contains("buffaura")) {
            addGoalIfAbsent(husk, 1, BuffAuraGoal.class, () -> new BuffAuraGoal(husk));
        }

        if (tags.contains("slimesummon")) {
            addGoalIfAbsent(husk, 1, SlimeSummonGoal.class, () -> new SlimeSummonGoal(husk));
        }

        if (tags.contains("gianttrigger")) {
            addGoalIfAbsent(husk, 1, GiantTriggerGoal.class, () -> new GiantTriggerGoal(husk));
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Husk husk)) return;
        if (!husk.getPersistentData().contains(LEVEL_KEY)) return;

        int level = husk.getPersistentData().getInt(LEVEL_KEY);
        List<LevelHuskConfig.LootEntry> tierTable = switch (tierOf(level)) {
            case 1 -> LevelHuskConfig.TIER1_EXTRA_DROPS;
            case 2 -> LevelHuskConfig.TIER2_EXTRA_DROPS;
            case 3 -> LevelHuskConfig.TIER3_EXTRA_DROPS;
            default -> LevelHuskConfig.TIER4_EXTRA_DROPS;
        };
        List<LevelHuskConfig.LootEntry> levelTable = LevelHuskConfig.LEVEL_EXTRA_DROPS.getOrDefault(level, List.of());
        if (tierTable.isEmpty() && levelTable.isEmpty()) return;

        RandomSource random = husk.getRandom();
        rollDrops(husk, event, tierTable, random);
        rollDrops(husk, event, levelTable, random);
    }

    private static void rollDrops(Husk husk, LivingDropsEvent event, List<LevelHuskConfig.LootEntry> table, RandomSource random) {
        for (LevelHuskConfig.LootEntry entry : table) {
            if (random.nextFloat() >= entry.chance()) continue;

            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(entry.itemId());
            if (item.isEmpty()) continue;

            int count = entry.minCount() + random.nextInt(entry.maxCount() - entry.minCount() + 1);
            ItemStack stack = new ItemStack(item.get(), count);
            event.getDrops().add(new ItemEntity(husk.level(), husk.getX(), husk.getY(), husk.getZ(), stack));
        }
    }

    private static int tierOf(int level) {
        if (level <= LevelHuskConfig.TIER1_MAX_LEVEL) return 1;
        if (level <= LevelHuskConfig.TIER2_MAX_LEVEL) return 2;
        if (level <= LevelHuskConfig.TIER3_MAX_LEVEL) return 3;
        return 4;
    }

    private static int rollLevel(RandomSource random) {
        double roll = random.nextDouble() * TOTAL_LEVEL_SPAWN_WEIGHT;
        double cumulative = 0.0;
        for (int level = LevelHuskConfig.MIN_LEVEL; level <= LevelHuskConfig.MAX_LEVEL; level++) {
            cumulative += LevelHuskConfig.LEVEL_SPAWN_WEIGHT.getOrDefault(level, 0.0);
            if (roll < cumulative) {
                return level;
            }
        }
        return LevelHuskConfig.MAX_LEVEL;
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

    private static void applyTier2(Husk husk, LevelHuskConfig.LevelProfile profile) {
        equipDiamondArmor(husk);
        if (!hasOwnMainhand(profile)) {
            applyWeaponAndReaction(husk, false);
        }
    }

    private static void applyTier3(Husk husk, LevelHuskConfig.LevelProfile profile) {
        equipDiamondArmor(husk);
        if (!hasOwnMainhand(profile)) {
            applyWeaponAndReaction(husk, true);
        }
    }

    private static boolean hasOwnMainhand(LevelHuskConfig.LevelProfile profile) {
        return profile != null && (profile.mainhand() != null || !profile.mainhandRoll().isEmpty());
    }

    private static void applyTier4(Husk husk) {
        equipVeriditeArmor(husk);
    }

    /**
     * Только шлем + нагрудник (без поножей/ботинок) - зеркалит LevelZombieHandler.equipVeriditeArmor.
     * Поножи/ботинки явно очищаем на случай случайной ванильной брони при спавне.
     */
    private static void equipVeriditeArmor(Husk husk) {
        equipEnchanted(husk, EquipmentSlot.HEAD, ModItems.VERIDITE_HELMET.get(),
                LevelHuskConfig.IMBA_ARMOR_PROTECTION_LEVEL, true);
        equipEnchanted(husk, EquipmentSlot.CHEST, ModItems.VERIDITE_CHESTPLATE.get(),
                LevelHuskConfig.IMBA_ARMOR_PROTECTION_LEVEL, true);
        husk.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        husk.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
    }

    private static void equipDiamondArmor(Husk husk) {
        equipArmorSet(husk,
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
                LevelHuskConfig.TIER23_ARMOR_PROTECTION_LEVEL, false);
    }

    private static void equipArmorSet(Husk husk, Item helmet, Item chest, Item legs, Item boots,
                                       int protectionLevel, boolean preventDrop) {
        equipEnchanted(husk, EquipmentSlot.HEAD, helmet, protectionLevel, preventDrop);
        equipEnchanted(husk, EquipmentSlot.CHEST, chest, protectionLevel, preventDrop);
        equipEnchanted(husk, EquipmentSlot.LEGS, legs, protectionLevel, preventDrop);
        equipEnchanted(husk, EquipmentSlot.FEET, boots, protectionLevel, preventDrop);
    }

    private static void equipEnchanted(Husk husk, EquipmentSlot slot, Item item, int protectionLevel, boolean preventDrop) {
        ItemStack stack = new ItemStack(item);
        enchant(husk, stack, Enchantments.PROTECTION, protectionLevel);
        husk.setItemSlot(slot, stack);
        if (preventDrop) {
            husk.setDropChance(slot, 0.0F);
        }
    }

    private static void applyWeaponAndReaction(Husk husk, boolean strong) {
        ResourceLocation[] ids = {
                LevelHuskConfig.PIKE_ITEM_ID,
                LevelHuskConfig.BATTLE_HAMMER_ITEM_ID,
                LevelHuskConfig.WARHAMMER_ITEM_ID
        };
        int choice = husk.getRandom().nextInt(ids.length);

        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ids[choice]);
        if (item.isEmpty()) return;

        husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item.get()));

        switch (choice) {
            case 0 -> husk.addTag("longreach");
            case 1 -> {
                double bonus = strong ? LevelHuskConfig.TIER3_HAMMER_REACH_BONUS : LevelHuskConfig.TIER2_HAMMER_REACH_BONUS;
                AttributeInstance reach = husk.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
                if (reach != null) {
                    reach.setBaseValue(Math.max(reach.getBaseValue(), bonus));
                }
            }
            default -> husk.addTag("hit"); // молот (скорость теперь только через rollSpeedTag)
        }
    }

    private static void enchant(LivingEntity contextEntity, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        Holder<Enchantment> holder = contextEntity.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
        stack.enchant(holder, level);
    }
}
