package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.common.compat.spartanweaponry.SpartanWeaponryCompat;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class LevelZombieConfig {
    private LevelZombieConfig() {}
    public record LootEntry(ResourceLocation itemId, float chance, int minCount, int maxCount) {
        public LootEntry(ResourceLocation itemId, float chance, int count) {
            this(itemId, chance, count, count);
        }
    }

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 35;

    public static final int TIER1_MAX_LEVEL = 14;
    public static final int TIER2_MAX_LEVEL = 24;
    public static final int TIER3_MAX_LEVEL = 29;
    public static final Map<Integer, Double> LEVEL_SPAWN_WEIGHT = Map.ofEntries(
            Map.entry(1, 80.0),
            Map.entry(2, 5.07),
            Map.entry(3, 2.25),
            Map.entry(4, 2.10),
            Map.entry(5, 1.95),
            Map.entry(6, 1.85),
            Map.entry(7, 1.75),
            Map.entry(8, 1.68),
            Map.entry(9, 1.62),
            Map.entry(10, 0.48),
            Map.entry(11, 0.40),
            Map.entry(12, 0.33),
            Map.entry(13, 0.28),
            Map.entry(14, 0.24),
            Map.entry(15, 0.20),
            Map.entry(16, 0.17),
            Map.entry(17, 0.15),
            Map.entry(18, 0.13),
            Map.entry(19, 0.11),
            Map.entry(20, 0.10),
            Map.entry(21, 0.09),
            Map.entry(22, 0.08),
            Map.entry(23, 0.07),
            Map.entry(24, 0.07),
            Map.entry(25, 0.06),
            Map.entry(26, 0.05),
            Map.entry(27, 0.05),
            Map.entry(28, 0.05),
            Map.entry(29, 0.04)
    );

    // ==== Шанс на "быстрого" ("speed") / "очень быстрого" ("speed2") level-зомби ====
    // Раскатывается ОДИН раз при спавне (LevelZombieHandler.rollSpeedTag), взаимоисключающе:
    // сначала проверяется speed2, потом speed. Это ЕДИНСТВЕННЫЙ источник этих тегов у level-зомби -
    // профили уровней и тирная экипировка их больше не выдают. Явный тег через /summon по-прежнему
    // работает (теги только добавляются, не снимаются).
    public static final int SPEED_ROLL_LOW_MAX_LEVEL = 15;   // диапазон "1-15"
    public static final int SPEED_ROLL_MID_MAX_LEVEL = 29;   // диапазон "16-29"; выше (30-35) - всегда 0
    public static final float SPEED_CHANCE_LOW = 0.05F;      // "speed",  уровни 1-15
    public static final float SPEED_CHANCE_MID = 0.10F;      // "speed",  уровни 16-29
    public static final float SPEED2_CHANCE_LOW = 0.025F;    // "speed2", уровни 1-15
    public static final float SPEED2_CHANCE_MID = 0.05F;     // "speed2", уровни 16-29

    public static float speedChance(int level) {
        if (level <= SPEED_ROLL_LOW_MAX_LEVEL) return SPEED_CHANCE_LOW;
        if (level <= SPEED_ROLL_MID_MAX_LEVEL) return SPEED_CHANCE_MID;
        return 0.0F;
    }

    public static float speed2Chance(int level) {
        if (level <= SPEED_ROLL_LOW_MAX_LEVEL) return SPEED2_CHANCE_LOW;
        if (level <= SPEED_ROLL_MID_MAX_LEVEL) return SPEED2_CHANCE_MID;
        return 0.0F;
    }

    public static final int IMBA_ARMOR_PROTECTION_LEVEL = 3;
    public static final int TIER23_ARMOR_PROTECTION_LEVEL = 2;
    public static final double TIER3_BREAK_SPEED_MULTIPLIER = 1.0D;
    public static final ResourceLocation PIKE_ITEM_ID = SpartanWeaponryCompat.IRON_PIKE;
    public static final ResourceLocation BATTLE_HAMMER_ITEM_ID = SpartanWeaponryCompat.IRON_BATTLE_HAMMER;
    public static final ResourceLocation WARHAMMER_ITEM_ID = SpartanWeaponryCompat.IRON_WARHAMMER;

    public static final double TIER2_HAMMER_REACH_BONUS = 1.5D;
    public static final double TIER3_HAMMER_REACH_BONUS = 3.0D;

    public static final int MAGE_MIN_COOLDOWN_TICKS = 20 * 4;
    public static final int MAGE_MAX_COOLDOWN_TICKS = 20 * 8;

    public static final int MAGE_POISON_MIN_DURATION_TICKS = 20 * 5;
    public static final int MAGE_POISON_MAX_DURATION_TICKS = 20 * 10;
    public static final int MAGE_SLOW_DURATION_TICKS = 20 * 6;

    public static final double MAGE_KNOCKBACK_MIN_STRENGTH = 1.2D;
    public static final double MAGE_KNOCKBACK_MAX_STRENGTH = 2.2D;

    public static final float MAGE_LIFESTEAL_MIN_DAMAGE = 3.0F;
    public static final float MAGE_LIFESTEAL_MAX_DAMAGE = 6.0F;
    public static final int MAGE_WITHER_DURATION_TICKS = 20 * 4;

    public static final int MAGE_WINDUP_MIN_TICKS = 10;
    public static final int MAGE_WINDUP_MAX_TICKS = 25;
    public static final double MAGE_CAST_RANGE = 12.0D;

    private static final String GRAVEYARD_MODID = "graveyard";

    private static ResourceLocation graveyardItemId(String path) {
        return ResourceLocation.fromNamespaceAndPath(GRAVEYARD_MODID, path);
    }
    public static final List<LootEntry> TIER1_EXTRA_DROPS = List.of();
    public static final List<LootEntry> TIER2_EXTRA_DROPS = List.of(
            new LootEntry(graveyardItemId("corruption"), 1.0F, 1));
    public static final List<LootEntry> TIER3_EXTRA_DROPS = List.of(
            new LootEntry(graveyardItemId("corruption"), 1.0F, 1));
    public static final List<LootEntry> TIER4_EXTRA_DROPS = List.of(
            new LootEntry(graveyardItemId("corruption"), 1.0F, 1));
    public record EnchantSpec(EquipmentSlot slot, ResourceKey<Enchantment> enchantment, int level) {}
    public record WeightedMainhand(ResourceLocation itemId, double weight, float dropChance, List<String> extraTags) {
        public static WeightedMainhand of(ResourceLocation itemId, double weight, float dropChance, String... extraTags) {
            return new WeightedMainhand(itemId, weight, dropChance, List.of(extraTags));
        }
    }

    public static final class LevelProfile {
        private ResourceLocation head;
        private ResourceLocation chest;
        private ResourceLocation legs;
        private ResourceLocation boots;
        private ResourceLocation mainhand;
        private ResourceLocation offhand;
        private int offhandCount = 1;
        private List<String> tags = List.of();
        private List<WeightedMainhand> mainhandRoll = List.of();
        private final List<EnchantSpec> enchants = new ArrayList<>();
        private final Map<EquipmentSlot, Float> dropChances = new EnumMap<>(EquipmentSlot.class);
        private final Map<EquipmentSlot, Float> equipChances = new EnumMap<>(EquipmentSlot.class);

        public LevelProfile head(ResourceLocation id) { this.head = id; return this; }
        public LevelProfile chest(ResourceLocation id) { this.chest = id; return this; }
        public LevelProfile legs(ResourceLocation id) { this.legs = id; return this; }
        public LevelProfile boots(ResourceLocation id) { this.boots = id; return this; }
        public LevelProfile mainhand(ResourceLocation id) { this.mainhand = id; return this; }
        public LevelProfile offhand(ResourceLocation id) { this.offhand = id; return this; }
        public LevelProfile offhand(ResourceLocation id, int count) { this.offhand = id; this.offhandCount = count; return this; }
        public LevelProfile tags(String... tags) { this.tags = List.of(tags); return this; }

        public LevelProfile mainhandRoll(WeightedMainhand... options) { this.mainhandRoll = List.of(options); return this; }

        public LevelProfile enchant(EquipmentSlot slot, ResourceKey<Enchantment> enchantment, int level) {
            this.enchants.add(new EnchantSpec(slot, enchantment, level));
            return this;
        }

        public LevelProfile dropChance(EquipmentSlot slot, float chance) {
            this.dropChances.put(slot, chance);
            return this;
        }

        public LevelProfile equipChance(EquipmentSlot slot, float chance) {
            this.equipChances.put(slot, chance);
            return this;
        }

        public ResourceLocation head() { return head; }
        public ResourceLocation chest() { return chest; }
        public ResourceLocation legs() { return legs; }
        public ResourceLocation boots() { return boots; }
        public ResourceLocation mainhand() { return mainhand; }
        public ResourceLocation offhand() { return offhand; }
        public int offhandCount() { return offhandCount; }
        public List<String> tags() { return tags; }
        public List<WeightedMainhand> mainhandRoll() { return mainhandRoll; }
        public List<EnchantSpec> enchants() { return enchants; }
        public Map<EquipmentSlot, Float> dropChances() { return dropChances; }
        public float equipChance(EquipmentSlot slot) { return equipChances.getOrDefault(slot, 1.0F); }
    }
    public static final Map<Integer, LevelProfile> LEVEL_PROFILES = Map.ofEntries(
            Map.entry(1, new LevelProfile()
                    .head(vanillaId("leather_helmet")).equipChance(EquipmentSlot.HEAD, 0.3F)
                    .dropChance(EquipmentSlot.HEAD, 0.02F)),
            Map.entry(2, new LevelProfile()
                    .head(vanillaId("leather_helmet")).dropChance(EquipmentSlot.HEAD, 0.02F)
                    .chest(vanillaId("leather_chestplate")).dropChance(EquipmentSlot.CHEST, 0.01F)),
            Map.entry(3, new LevelProfile()
                    .head(vanillaId("leather_helmet")).dropChance(EquipmentSlot.HEAD, 0.02F)
                    .chest(vanillaId("chainmail_chestplate")).dropChance(EquipmentSlot.CHEST, 0.05F)
                    .mainhand(vanillaId("wooden_sword")).dropChance(EquipmentSlot.MAINHAND, 0.10F)),
            Map.entry(4, new LevelProfile()
                    .head(vanillaId("leather_helmet")).dropChance(EquipmentSlot.HEAD, 0.02F)
                    .chest(vanillaId("chainmail_chestplate")).dropChance(EquipmentSlot.CHEST, 0.05F)
                    .boots(vanillaId("leather_boots")).dropChance(EquipmentSlot.FEET, 0.02F)
                    .mainhand(vanillaId("wooden_sword")).dropChance(EquipmentSlot.MAINHAND, 0.10F)),
            Map.entry(5, new LevelProfile()
                    .head(vanillaId("leather_helmet")).dropChance(EquipmentSlot.HEAD, 0.02F)
                    .chest(vanillaId("chainmail_chestplate")).dropChance(EquipmentSlot.CHEST, 0.05F)
                    .legs(vanillaId("chainmail_leggings")).dropChance(EquipmentSlot.LEGS, 0.055F)
                    .boots(vanillaId("leather_boots")).dropChance(EquipmentSlot.FEET, 0.02F)
                    .mainhand(vanillaId("stone_sword")).dropChance(EquipmentSlot.MAINHAND, 0.065F)),
            Map.entry(6, new LevelProfile()
                    .head(vanillaId("chainmail_helmet")).dropChance(EquipmentSlot.HEAD, 0.06F)
                    .chest(vanillaId("chainmail_chestplate")).dropChance(EquipmentSlot.CHEST, 0.05F)
                    .legs(vanillaId("chainmail_leggings")).dropChance(EquipmentSlot.LEGS, 0.055F)
                    .boots(vanillaId("chainmail_boots")).dropChance(EquipmentSlot.FEET, 0.06F)
                    .mainhandRoll(
                            WeightedMainhand.of(vanillaId("stone_sword"), 75, 0.065F),
                            WeightedMainhand.of(vanillaId("stone_axe"), 20, 0.05F),
                            WeightedMainhand.of(SpartanWeaponryCompat.IRON_PIKE, 5, 0.065F, "longreach"))
                    .tags("noknockback")),
            Map.entry(7, new LevelProfile()
                    .head(vanillaId("chainmail_helmet")).dropChance(EquipmentSlot.HEAD, 0.06F)
                    .chest(vanillaId("iron_chestplate")).dropChance(EquipmentSlot.CHEST, 0.03F)
                    .legs(vanillaId("iron_leggings")).dropChance(EquipmentSlot.LEGS, 0.035F)
                    .boots(vanillaId("chainmail_boots")).dropChance(EquipmentSlot.FEET, 0.06F)
                    .mainhandRoll(
                            WeightedMainhand.of(vanillaId("iron_sword"), 75, 0.05F),
                            WeightedMainhand.of(vanillaId("iron_axe"), 10, 0.05F),
                            WeightedMainhand.of(SpartanWeaponryCompat.IRON_PIKE, 15, 0.05F, "longreach"))
                    .tags("noknockback")),
            Map.entry(8, new LevelProfile()
                    .head(vanillaId("iron_helmet")).dropChance(EquipmentSlot.HEAD, 0.04F)
                    .chest(vanillaId("diamond_chestplate"))
                    .legs(vanillaId("iron_leggings")).dropChance(EquipmentSlot.LEGS, 0.035F)
                    .boots(vanillaId("iron_boots")).dropChance(EquipmentSlot.FEET, 0.04F)
                    .mainhandRoll(
                            WeightedMainhand.of(vanillaId("iron_sword"), 75, 0.02F),
                            WeightedMainhand.of(vanillaId("iron_axe"), 10, 0.05F),
                            WeightedMainhand.of(SpartanWeaponryCompat.IRON_PIKE, 15, 0.05F, "longreach"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.15F)
                    .tags("noknockback")),
            Map.entry(9, new LevelProfile()
                    .head(vanillaId("iron_helmet")).dropChance(EquipmentSlot.HEAD, 0.04F)
                    .chest(vanillaId("iron_chestplate")).dropChance(EquipmentSlot.CHEST, 0.03F)
                    .legs(vanillaId("golden_leggings")).dropChance(EquipmentSlot.LEGS, 0.05F)
                    .boots(vanillaId("golden_boots")).dropChance(EquipmentSlot.FEET, 0.06F)
                    .mainhandRoll(
                            WeightedMainhand.of(vanillaId("diamond_sword"), 75, 0.02F),
                            WeightedMainhand.of(vanillaId("iron_axe"), 10, 0.05F),
                            WeightedMainhand.of(SpartanWeaponryCompat.IRON_PIKE, 15, 0.05F, "longreach"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.15F)
                    .tags("noknockback")),
            Map.entry(10, new LevelProfile()
                    .head(vanillaId("iron_helmet")).dropChance(EquipmentSlot.HEAD, 0.04F)
                    .chest(vanillaId("iron_chestplate")).dropChance(EquipmentSlot.CHEST, 0.03F)
                    .legs(vanillaId("iron_leggings")).dropChance(EquipmentSlot.LEGS, 0.035F)
                    .boots(vanillaId("iron_boots")).dropChance(EquipmentSlot.FEET, 0.04F)
                    // sharpness1 привязан именно к мечу - взвешенный ролл тут не по вариантам оружия, а фиксированный меч + чары.
                    .mainhand(vanillaId("diamond_sword")).dropChance(EquipmentSlot.MAINHAND, 0.02F)
                    .enchant(EquipmentSlot.MAINHAND, Enchantments.SHARPNESS, 1)
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.175F)
                    .tags("noknockback")),
            Map.entry(11, new LevelProfile()
                    .head(vanillaId("golden_helmet")).dropChance(EquipmentSlot.HEAD, 0.06F)
                    .chest(vanillaId("golden_chestplate")).dropChance(EquipmentSlot.CHEST, 0.05F)
                    .legs(vanillaId("golden_leggings")).dropChance(EquipmentSlot.LEGS, 0.055F)
                    .boots(vanillaId("golden_boots")).dropChance(EquipmentSlot.FEET, 0.06F)
                    .mainhandRoll(
                            WeightedMainhand.of(vanillaId("iron_sword"), 45, 0.02F),
                            WeightedMainhand.of(vanillaId("iron_axe"), 30, 0.05F),
                            WeightedMainhand.of(SpartanWeaponryCompat.IRON_PIKE, 15, 0.05F, "longreach"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.70F)
                    // Бонус: кидает heavy_core, хитует и убегает, чтобы кинуть снова - см. HeavyCoreThrowGoal.
                    .tags("noknockback", "heavycore")),
            Map.entry(12, new LevelProfile()
                    .head(vanillaId("iron_helmet")).dropChance(EquipmentSlot.HEAD, 0.04F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .legs(vanillaId("iron_leggings")).dropChance(EquipmentSlot.LEGS, 0.035F)
                    .boots(vanillaId("iron_boots")).dropChance(EquipmentSlot.FEET, 0.04F)
                    .mainhandRoll(
                            WeightedMainhand.of(vanillaId("diamond_sword"), 75, 0.02F),
                            WeightedMainhand.of(vanillaId("iron_axe"), 10, 0.05F),
                            WeightedMainhand.of(SpartanWeaponryCompat.DIAMOND_PIKE, 15, 0.05F, "longreach"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.25F)
                    .tags("noknockback")),
            Map.entry(13, new LevelProfile()
                    .head(vanillaId("iron_helmet")).dropChance(EquipmentSlot.HEAD, 0.04F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .boots(vanillaId("iron_boots")).dropChance(EquipmentSlot.FEET, 0.04F)
                    .mainhand(vanillaId("golden_pickaxe"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.60F)
                    // Бонус: может ломать блоки (переиспользует WitherBlockBreakGoal - см. onTick).
                    .tags("noknockback", "breakblocks")),
            Map.entry(14, new LevelProfile()
                    .head(vanillaId("iron_helmet")).dropChance(EquipmentSlot.HEAD, 0.04F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .boots(vanillaId("iron_boots")).dropChance(EquipmentSlot.FEET, 0.04F)
                    .mainhandRoll(
                            WeightedMainhand.of(vanillaId("diamond_sword"), 75, 0.02F),
                            WeightedMainhand.of(SpartanWeaponryCompat.DIAMOND_PIKE, 25, 0.05F, "longreach"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.25F)
                    .tags("noknockback")),
            Map.entry(15, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhandRoll(
                            WeightedMainhand.of(SpartanWeaponryCompat.NETHERITE_RAPIER, 75, 0.0045F),
                            WeightedMainhand.of(SpartanWeaponryCompat.IRON_WARHAMMER, 25, 0.05F, "hit"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.40F)
                    // Бонус: пассивный эффект Силы I на себя - см. SelfEffectHandler.
                    .tags("noknockback", "strength1")),
            Map.entry(16, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .enchant(EquipmentSlot.CHEST, Enchantments.PROJECTILE_PROTECTION, 2)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .enchant(EquipmentSlot.LEGS, Enchantments.PROJECTILE_PROTECTION, 2)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhand(SpartanWeaponryCompat.NETHERITE_RAPIER).dropChance(EquipmentSlot.MAINHAND, 0.03F)
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.40F)
                    .tags("noknockback", "strength1")),
            Map.entry(17, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .enchant(EquipmentSlot.CHEST, Enchantments.PROJECTILE_PROTECTION, 2)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .enchant(EquipmentSlot.LEGS, Enchantments.PROJECTILE_PROTECTION, 2)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhandRoll(
                            WeightedMainhand.of(vanillaId("diamond_sword"), 75, 0.02F),
                            WeightedMainhand.of(SpartanWeaponryCompat.DIAMOND_PIKE, 25, 0.015F, "longreach"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.45F)
                    .tags("noknockback")),
            Map.entry(18, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .enchant(EquipmentSlot.HEAD, Enchantments.PROJECTILE_PROTECTION, 2)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .enchant(EquipmentSlot.CHEST, Enchantments.PROJECTILE_PROTECTION, 2)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .enchant(EquipmentSlot.LEGS, Enchantments.PROJECTILE_PROTECTION, 2)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .enchant(EquipmentSlot.FEET, Enchantments.PROJECTILE_PROTECTION, 2)
                    .mainhandRoll(
                            WeightedMainhand.of(vanillaId("diamond_sword"), 75, 0.015F),
                            WeightedMainhand.of(SpartanWeaponryCompat.IRON_BATTLE_HAMMER, 25, 0.025F, "hit", "slow0"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.45F)
                    .tags("noknockback")),
            Map.entry(19, new LevelProfile()
                    .head(vanillaId("golden_helmet")).dropChance(EquipmentSlot.HEAD, 0.06F)
                    .chest(vanillaId("golden_chestplate")).dropChance(EquipmentSlot.CHEST, 0.05F)
                    .legs(vanillaId("golden_leggings")).dropChance(EquipmentSlot.LEGS, 0.055F)
                    .boots(vanillaId("golden_boots")).dropChance(EquipmentSlot.FEET, 0.06F)
                    .mainhand(vanillaId("diamond_pickaxe")).dropChance(EquipmentSlot.MAINHAND, 0.02F)
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.70F)
                    // Бонус: может ломать блоки (WitherBlockBreakGoal).
                    .tags("noknockback", "breakblocks")),
            Map.entry(20, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhand(SpartanWeaponryCompat.DIAMOND_GREATSWORD)
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.40F)
                    // Бонус: эффект Медлительности 0 при попадании (вместе с "hit"/PullAttackGoal) - см. OnHitEffectHandler.
                    .tags("noknockback", "hit", "slow0")),
            Map.entry(21, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhand(vanillaId("trident")).dropChance(EquipmentSlot.MAINHAND, 0.04F)
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.45F)
                    .tags("noknockback")),
            Map.entry(22, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .enchant(EquipmentSlot.CHEST, Enchantments.PROJECTILE_PROTECTION, 2)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .enchant(EquipmentSlot.LEGS, Enchantments.PROJECTILE_PROTECTION, 2)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhand(vanillaId("trident")).dropChance(EquipmentSlot.MAINHAND, 0.04F)
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.50F)
                    // Бонус: пассивный эффект Силы I на себя.
                    .tags("noknockback", "hit", "strength1")),
            Map.entry(23, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .enchant(EquipmentSlot.CHEST, Enchantments.PROJECTILE_PROTECTION, 2)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .enchant(EquipmentSlot.LEGS, Enchantments.PROJECTILE_PROTECTION, 2)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhand(SpartanWeaponryCompat.DIAMOND_SCYTHE)
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.50F)
                    .tags("noknockback")),
            Map.entry(24, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    // Было ошибочно vanillaId("diamond_leggings") в слоте ботинок - поправлено на diamond_boots.
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhand(SpartanWeaponryCompat.DIAMOND_WARHAMMER).dropChance(EquipmentSlot.MAINHAND, 0.01F)
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.55F)
                    .tags("noknockback", "hit")),
            Map.entry(25, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .enchant(EquipmentSlot.CHEST, Enchantments.PROJECTILE_PROTECTION, 2)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .enchant(EquipmentSlot.LEGS, Enchantments.PROJECTILE_PROTECTION, 2)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhand(SpartanWeaponryCompat.DIAMOND_PIKE).dropChance(EquipmentSlot.MAINHAND, 0.02F)
                    .offhand(modId(GRAVEYARD_MODID, "corruption")).dropChance(EquipmentSlot.OFFHAND, 0.55F)
                    .tags("longreach", "noknockback")),
            Map.entry(26, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .enchant(EquipmentSlot.CHEST, Enchantments.PROJECTILE_PROTECTION, 2)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .enchant(EquipmentSlot.LEGS, Enchantments.PROJECTILE_PROTECTION, 2)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhand(vanillaId("netherite_sword")).dropChance(EquipmentSlot.MAINHAND, 0.01F)
                    .offhand(modId(GRAVEYARD_MODID, "corruption"), 2).dropChance(EquipmentSlot.OFFHAND, 0.55F)
                    .tags("noknockback")),
            Map.entry(27, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .enchant(EquipmentSlot.CHEST, Enchantments.PROJECTILE_PROTECTION, 2)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhand(vanillaId("netherite_sword")).dropChance(EquipmentSlot.MAINHAND, 0.01F)
                    .offhand(modId(GRAVEYARD_MODID, "corruption"), 2).dropChance(EquipmentSlot.OFFHAND, 0.60F)
                    // Бонус: может ломать блоки (WitherBlockBreakGoal).
                    .tags("hit", "noknockback", "breakblocks")),
            Map.entry(28, new LevelProfile()
                    .head(vanillaId("diamond_helmet")).dropChance(EquipmentSlot.HEAD, 0.03F)
                    .chest(vanillaId("diamond_chestplate")).dropChance(EquipmentSlot.CHEST, 0.02F)
                    .enchant(EquipmentSlot.CHEST, Enchantments.PROJECTILE_PROTECTION, 4)
                    .legs(vanillaId("diamond_leggings")).dropChance(EquipmentSlot.LEGS, 0.025F)
                    .enchant(EquipmentSlot.LEGS, Enchantments.PROJECTILE_PROTECTION, 4)
                    .boots(vanillaId("diamond_boots")).dropChance(EquipmentSlot.FEET, 0.03F)
                    .mainhand(modId(GRAVEYARD_MODID, "netherite_greatsword")).dropChance(EquipmentSlot.MAINHAND, 0.01F)
                    .offhand(modId(GRAVEYARD_MODID, "corruption"), 2).dropChance(EquipmentSlot.OFFHAND, 0.65F)
                    // Бонус: эффект Медлительности I при попадании + призыв "крокодилов" (EvokerFangs) раз в 20 сек.
                    .tags("hit", "noknockback", "slow1", "fangs")),
            Map.entry(29, new LevelProfile()
                    .head(vanillaId("iron_helmet"))
                    .chest(vanillaId("iron_chestplate"))
                    .legs(vanillaId("iron_leggings"))
                    .boots(vanillaId("iron_boots"))
                    .mainhand(vanillaId("iron_pickaxe"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption"), 3).dropChance(EquipmentSlot.OFFHAND, 1.0F)
                    // Бонус: ломает блоки как WitherBlockBreakGoal, но быстрее (максимальный множитель уже и так предельный).
                    .tags("noknockback", "breakblocks")),
            Map.entry(30, new LevelProfile()
                    .mainhand(vanillaId("netherite_sword"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption"), 3).dropChance(EquipmentSlot.OFFHAND, 1.0F)
                    // Бонус: кастует отдачу+подкидывание, убегает и прячется за другими зомби - см. LaunchCastGoal.
                    .tags("noknockback", "launchcast")),
            Map.entry(31, new LevelProfile()
                    .mainhand(vanillaId("netherite_sword"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption"), 3).dropChance(EquipmentSlot.OFFHAND, 1.0F)
                    // Бонус: ядовитое облако, убегает и прячется за другими зомби, пассивная Медлительность 0 на себя.
                    .tags("noknockback", "poisoncloud", "selfslow0")),
            Map.entry(32, new LevelProfile()
                    .mainhand(vanillaId("netherite_sword"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption"), 4).dropChance(EquipmentSlot.OFFHAND, 1.0F)
                    // Бонус: самонаводящийся лазер, не задевает злых мобов - см. LaserBeamGoal.
                    .tags("noknockback", "laserbeam")),
            Map.entry(33, new LevelProfile()
                    .mainhand(vanillaId("netherite_sword"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption"), 3).dropChance(EquipmentSlot.OFFHAND, 1.0F)
                    // Бонус: аура баффа на соседних злых мобов - см. BuffAuraGoal.
                    .tags("noknockback", "buffaura")),
            Map.entry(34, new LevelProfile()
                    .mainhand(vanillaId("netherite_sword"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption"), 3).dropChance(EquipmentSlot.OFFHAND, 1.0F)
                    // Бонус: призыв 2 взрывных слизней, не вредящих злым мобам - см. SlimeSummonGoal.
                    .tags("noknockback", "slimesummon")),
            Map.entry(35, new LevelProfile()
                    .mainhand(vanillaId("netherite_sword"))
                    .offhand(modId(GRAVEYARD_MODID, "corruption"), 3).dropChance(EquipmentSlot.OFFHAND, 1.0F)
                    // Бонус: превращает случайного соседнего зомби в великана на 30 сек - см. GiantTransformGoal.
                    .tags("noknockback", "gianttrigger"))
    );

    public static final Map<Integer, List<LootEntry>> LEVEL_EXTRA_DROPS = Map.of();

    private static ResourceLocation vanillaId(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static ResourceLocation modId(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
//this shit is fucking crazy