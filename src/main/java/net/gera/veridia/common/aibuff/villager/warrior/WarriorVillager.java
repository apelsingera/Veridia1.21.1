package net.gera.veridia.common.aibuff.villager.warrior;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorVillagerConfig.GearOption;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorVillagerConfig.WeaponCategory;
import net.gera.veridia.common.effect.ModEffects;
import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Состояние жителя-воина: разовый розыгрыш экипировки по таблицам WarriorVillagerConfig и флаги,
 * которые из него следуют (лук / длинная дистанция удара). Хранится в persistentData - по той же
 * конвенции aibuff, что и AngryVillager, без отдельного Attachment.
 * <p>
 * Розыгрыш делается один раз (GEAR_KEY) и переживает перезаход в мир, поэтому один и тот же воин
 * не перекатывает себе оружие каждый тик.
 */
public final class WarriorVillager {

    /** Ванильный тег, который читает LongReachHandler - его же вешаем пикинёру. */
    public static final String LONG_REACH_TAG = "longreach";

    private static final String GEAR_KEY = "veridia_warrior_gear";
    private static final String RANGED_KEY = "veridia_warrior_ranged";
    private static final String LONG_REACH_KEY = "veridia_warrior_longreach";
    private static final String STATS_KEY = "veridia_warrior_stats";
    private static final String WEAPON_KEY = "veridia_warrior_weapon";

    /** Сколько ступеней апгрейда ARMOR уже применено к надетой броне - см. syncArmorGear.
     * Отсутствие тега (CompoundTag.getInt по умолчанию) значит 0 - апгрейды ещё не применялись. */
    private static final String ARMOR_TIER_KEY = "veridia_warrior_armor_tier";
    /** Игровой тик последнего удара от монстра (Enemy) - на нём завязана пассивная регенерация
     * узла BRAIN, см. tickBrainHeal. */
    private static final String LAST_MONSTER_HIT_KEY = "veridia_warrior_last_monster_hit";
    /** Следующий тик, на котором можно снова сработать пассивному лечению - отдельно от
     * LAST_MONSTER_HIT_KEY, иначе лечение срабатывало бы каждый тик, пока порог не сбит ударом. */
    private static final String NEXT_BRAIN_HEAL_KEY = "veridia_warrior_next_brain_heal";

    private static final ResourceLocation SPEED_UPGRADE_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "warrior_speed_upgrade");

    /** Обида: кто бил, сколько раз, когда в последний раз и до какого тика воин зол. */
    private static final String GRUDGE_TARGET_KEY = "veridia_warrior_grudge_target";
    private static final String GRUDGE_HITS_KEY = "veridia_warrior_grudge_hits";
    private static final String GRUDGE_LAST_HIT_KEY = "veridia_warrior_grudge_last_hit";
    private static final String GRUDGE_ANGRY_UNTIL_KEY = "veridia_warrior_grudge_angry_until";

    private WarriorVillager() {}

    public static boolean isWarrior(Villager villager) {
        return villager.getVillagerData().getProfession() == ModVillagers.WARRIOR.value();
    }

    /** Лук выпал - значит воюет на дистанции (WarriorBowAttackGoal), а не мечом. */
    public static boolean usesBow(Villager villager) {
        return villager.getPersistentData().getBoolean(RANGED_KEY);
    }

    /** Пика: бьёт с WarriorVillagerConfig.PIKE_REACH вместо MELEE_REACH. */
    public static boolean hasLongReach(Villager villager) {
        return villager.getPersistentData().getBoolean(LONG_REACH_KEY);
    }

    public static double meleeReach(Villager villager) {
        return hasLongReach(villager) ? WarriorVillagerConfig.PIKE_REACH : WarriorVillagerConfig.MELEE_REACH;
    }

    /**
     * 25 HP + бонус HEART-узлов по умолчанию. Через setBaseValue (как WitherBuffHandler.applyStats),
     * а не через модификатор - базовое значение сохраняется в NBT, так что после перезахода воин не
     * "худеет" обратно. Полный хил до максимума - только в самый первый раз (когда житель ТОЛЬКО
     * стал воином), иначе разблокировка heart-узла в бою давала бы бесплатный фулл-хил каждый раз -
     * дальше maxHealth растёт, а текущее здоровье остаётся как было (как и должно расти "место").
     */
    public static void ensureStats(Villager villager) {
        AttributeInstance maxHealth = villager.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null || villager.level().isClientSide()) return;

        boolean firstTime = !villager.getPersistentData().getBoolean(STATS_KEY);
        double expected = WarriorVillagerConfig.MAX_HEALTH + WarriorUpgrades.maxHealthBonus(villager);
        if (maxHealth.getBaseValue() != expected) {
            maxHealth.setBaseValue(expected);
            if (firstTime) {
                villager.setHealth((float) expected);
            }
        }
        if (firstTime) {
            villager.getPersistentData().putBoolean(STATS_KEY, true);
        }
    }

    /**
     * Зерно розыгрыша снаряжения, привязанное к UUID жителя.
     * <p>
     * Раньше брался villager.getRandom() - общий поток мира, поэтому один и тот же житель после
     * снятия и повторного получения профессии получал новый комплект. Это открывало реролл:
     * сломал строп, поставил обратно, повторил до алмазного набора. С зерном от UUID комплект у
     * конкретного жителя один и тот же навсегда, сколько раз ни снимай профессию, - поэтому
     * снаряжение стало безопасно снимать вместе с профессией (см. stripGear).
     */
    private static RandomSource gearRandom(Villager villager) {
        UUID uuid = villager.getUUID();
        return RandomSource.create(uuid.getMostSignificantBits() * 31L ^ uuid.getLeastSignificantBits());
    }

    /**
     * Розыгрыш оружия, сапог и начального шлема/нагрудника/поножей (WarriorVillagerConfig.HELMETS
     * и т.п.) - дерево улучшений тут не участвует, апгрейд ARMOR (syncArmorGear) поднимает уже
     * выданный тут предмет на ступень выше, а не переигрывает розыгрыш заново.
     */
    public static void ensureGear(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        // Флаг - только оптимизация: розыгрыш детерминирован по UUID, повторный вызов дал бы
        // тот же результат. Воинам без WEAPON_KEY комплект перекатываем (оружие мог стереть
        // ShowTradesToPlayer, см. keepWeaponEquipped).
        if (data.getBoolean(GEAR_KEY) && data.contains(WEAPON_KEY)) return;

        RandomSource random = gearRandom(villager);
        WeaponCategory category = rollCategory(random);
        ItemStack weapon = stackOf(roll(random, category.tiers()));
        data.putString(WEAPON_KEY, BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString());

        equip(villager, EquipmentSlot.MAINHAND, weapon);
        equip(villager, EquipmentSlot.HEAD, stackOf(roll(random, WarriorVillagerConfig.HELMETS)));
        equip(villager, EquipmentSlot.CHEST, stackOf(roll(random, WarriorVillagerConfig.CHESTPLATES)));
        equip(villager, EquipmentSlot.LEGS, stackOf(roll(random, WarriorVillagerConfig.LEGGINGS)));
        equip(villager, EquipmentSlot.FEET, stackOf(roll(random, WarriorVillagerConfig.BOOTS)));

        data.putBoolean(RANGED_KEY, category.ranged());
        data.putBoolean(LONG_REACH_KEY, category.longReach());
        data.putBoolean(GEAR_KEY, true);

        applyLongReach(villager);
    }

    /**
     * Апгрейд брони узлом ARMOR - "улучшает броню на один": кожа -> кольчуга -> железо
     * (WarriorVillagerConfig.HELMET_MATERIAL_LADDER и т.п.), а если предмет уже железный - вместо
     * смены материала +1 уровень Protection. Действует на уже существующих воинов "живьём": как
     * только число разблокированных ARMOR-узлов (ARMOR_TIER_KEY -> targetSteps) выросло, каждый
     * недостающий шаг применяется по очереди к ТЕКУЩЕМУ надетому предмету (а не переигрывается
     * розыгрышем заново) - поэтому 2 апгрейда подряд от кожи дают железо, а от железа - Protection II.
     */
    public static void syncArmorGear(Villager villager) {
        if (villager.level().isClientSide()) return;

        int targetSteps = WarriorUpgrades.armorTier(villager);
        CompoundTag data = villager.getPersistentData();
        int appliedSteps = data.getInt(ARMOR_TIER_KEY);
        if (targetSteps <= appliedSteps) return;

        for (int step = appliedSteps; step < targetSteps; step++) {
            upgradeArmorPiece(villager, EquipmentSlot.HEAD, WarriorVillagerConfig.HELMET_MATERIAL_LADDER);
            upgradeArmorPiece(villager, EquipmentSlot.CHEST, WarriorVillagerConfig.CHESTPLATE_MATERIAL_LADDER);
            upgradeArmorPiece(villager, EquipmentSlot.LEGS, WarriorVillagerConfig.LEGGINGS_MATERIAL_LADDER);
        }
        data.putInt(ARMOR_TIER_KEY, targetSteps);
    }

    /** Один предмет на одну ступень выше по лесенке материалов - см. syncArmorGear. Материал не
     * из лесенки (например, воин уже снял/потерял броню) считается "как максимум" - тоже +Protection. */
    private static void upgradeArmorPiece(Villager villager, EquipmentSlot slot, List<Item> materialLadder) {
        ItemStack current = villager.getItemBySlot(slot);
        int index = materialLadder.indexOf(current.getItem());

        if (index >= 0 && index < materialLadder.size() - 1) {
            equip(villager, slot, new ItemStack(materialLadder.get(index + 1)));
            return;
        }

        ItemStack stack = current.isEmpty() ? new ItemStack(materialLadder.get(materialLadder.size() - 1)) : current;
        Holder<Enchantment> protection = villager.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.PROTECTION);
        stack.enchant(protection, stack.getEnchantmentLevel(protection) + 1);
        equip(villager, slot, stack);
    }

    /** Скорость воина - постоянный модификатор MOVEMENT_SPEED, а не масштабирование скорости в
     * каждом Goal (как у lumberjack/farmer): у воина слишком много путей движения (сближение в
     * ближнем бою, отход лучника, поиск цели), чтобы прокидывать множитель в каждый из них. */
    public static void syncSpeedBonus(Villager villager) {
        if (villager.level().isClientSide()) return;
        AttributeInstance speed = villager.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        double bonus = WarriorUpgrades.walkSpeedMultiplier(villager) - 1.0;
        AttributeModifier existing = speed.getModifier(SPEED_UPGRADE_ID);
        double existingAmount = existing != null ? existing.amount() : 0.0;
        if (existingAmount == bonus) return;

        if (existing != null) {
            speed.removeModifier(SPEED_UPGRADE_ID);
        }
        if (bonus != 0.0) {
            speed.addPermanentModifier(new AttributeModifier(SPEED_UPGRADE_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Кровотечение (если разблокирован хоть один BLEED-узел) и шанс поджечь (FIRE_ASPECT) - при
     * попадании воина по цели. Не завязано на конкретное оружие (рапира/молот/меч/пика/лук) -
     * бонус за узел дерева, а не за случайно выпавший предмет, как и остальные warrior-апгрейды. */
    public static void applyCombatUpgrades(Villager villager, LivingEntity target) {
        if (villager.level().isClientSide()) return;

        int bleedDuration = WarriorUpgrades.bleedDurationTicks(villager);
        if (bleedDuration > 0) {
            target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, bleedDuration, 0, false, true, true));
        }

        float fireChance = WarriorUpgrades.fireAspectChance(villager);
        if (fireChance > 0.0F && villager.getRandom().nextFloat() < fireChance) {
            target.setRemainingFireTicks(WarriorUpgrades.FIRE_ASPECT_IGNITE_SECONDS * 20);
        }
    }

    /** Обновляет тик последнего удара от монстра - от него отсчитывается "тишина"
     * BRAIN_HEAL_NO_DAMAGE_TICKS для пассивной регенерации, см. tickBrainHeal. */
    public static void registerMonsterHit(Villager villager) {
        villager.getPersistentData().putLong(LAST_MONSTER_HIT_KEY, villager.level().getGameTime());
    }

    /**
     * Пассивная регенерация узла BRAIN - замена старой WarriorHealAtJobSiteGoal ("бежит к стропу
     * лечиться"). Если по воину не били монстры BRAIN_HEAL_NO_DAMAGE_TICKS - лечит на
     * BRAIN_HEAL_AMOUNT и сдвигает NEXT_BRAIN_HEAL_KEY ещё на BRAIN_HEAL_NO_DAMAGE_TICKS вперёд,
     * то есть лечится раз в 10 сек мирной жизни, а не только один раз.
     */
    public static void tickBrainHeal(Villager villager) {
        if (villager.level().isClientSide()) return;
        if (!WarriorUpgrades.hasBrainHeal(villager)) return;
        if (villager.getHealth() >= villager.getMaxHealth()) return;

        CompoundTag data = villager.getPersistentData();
        long now = villager.level().getGameTime();
        if (now - data.getLong(LAST_MONSTER_HIT_KEY) < WarriorUpgrades.BRAIN_HEAL_NO_DAMAGE_TICKS) return;
        if (now < data.getLong(NEXT_BRAIN_HEAL_KEY)) return;

        villager.heal(WarriorUpgrades.BRAIN_HEAL_AMOUNT);
        data.putLong(NEXT_BRAIN_HEAL_KEY, now + WarriorUpgrades.BRAIN_HEAL_NO_DAMAGE_TICKS);
    }

    /**
     * Пикинёру нужен и ванильный тег (по нему тестируется руками и работает LongReachHandler),
     * и увеличенный ENTITY_INTERACTION_RANGE - без него сервер просто не засчитает удар с
     * дистанции, каким бы длинным ни был radius в Goal (то же "исправление урона", что в
     * LongReachHandler).
     */
    public static void applyLongReach(Villager villager) {
        if (!hasLongReach(villager)) return;

        if (!villager.getTags().contains(LONG_REACH_TAG)) {
            villager.addTag(LONG_REACH_TAG);
        }
        AttributeInstance reach = villager.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach != null && reach.getBaseValue() < WarriorVillagerConfig.PIKE_REACH) {
            reach.setBaseValue(WarriorVillagerConfig.PIKE_REACH);
        }
    }

    /**
     * Возвращает оружие в руку, если его оттуда убрали.
     * <p>
     * Ванильное поведение мозга ShowTradesToPlayer срабатывает, как только игрок подходит к жителю
     * ближе ~4 блоков, и в своём tick() вызывает clearHeldItem -> setItemSlot(MAINHAND, EMPTY):
     * если показывать нечего (а у воина торгов нет), оно просто ОЧИЩАЕТ руку. Отсюда "оружие
     * иногда видно, иногда прячут без причины" - и заодно потеря урона от оружия. Поэтому оружие
     * запоминается в persistentData при розыгрыше и переставляется обратно каждый тик.
     * <p>
     * Вызывать нужно ПОСЛЕ мозга (EntityTickEvent.Post) - иначе очистка произойдёт уже после нас,
     * в этом же тике.
     */
    public static void keepWeaponEquipped(Villager villager) {
        String id = villager.getPersistentData().getString(WEAPON_KEY);
        if (id.isEmpty()) return;

        ResourceLocation itemId = ResourceLocation.tryParse(id);
        if (itemId == null) return;

        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
        if (item.isEmpty()) return;

        if (villager.getMainHandItem().is(item.get())) {
            // Оружие на месте, но шанс дропа мог подняться: ShowTradesToPlayer.clearHeldItem
            // выставляет MAINHAND ванильные 0.085 каждый раз, когда чистит руку. Подтверждаем ноль,
            // иначе воина можно было бы фармить на железо.
            villager.setDropChance(EquipmentSlot.MAINHAND, WarriorVillagerConfig.EQUIPMENT_DROP_CHANCE);
            return;
        }

        equip(villager, EquipmentSlot.MAINHAND, new ItemStack(item.get()));
    }

    /**
     * Засчитывает удар от игрока или голема и говорит, пора ли бить в ответ.
     * <p>
     * Обида привязана к конкретному обидчику: ударил другой - счётчик начинается заново, иначе
     * воин копил бы удары от разных источников и кидался на случайного. Если с прошлого удара
     * прошло больше GRUDGE_FORGET_TICKS, счётчик тоже обнуляется - один задевший удар прощается.
     *
     * @return true, если порог GRUDGE_HITS_TO_ANGER достигнут и воину пора атаковать
     */
    public static boolean registerGrudgeHit(Villager villager, LivingEntity attacker) {
        CompoundTag data = villager.getPersistentData();
        long now = villager.level().getGameTime();

        boolean sameAttacker = data.hasUUID(GRUDGE_TARGET_KEY)
                && data.getUUID(GRUDGE_TARGET_KEY).equals(attacker.getUUID());
        boolean stillFresh = now - data.getLong(GRUDGE_LAST_HIT_KEY) <= WarriorVillagerConfig.GRUDGE_FORGET_TICKS;

        int hits = sameAttacker && stillFresh ? data.getInt(GRUDGE_HITS_KEY) + 1 : 1;

        data.putUUID(GRUDGE_TARGET_KEY, attacker.getUUID());
        data.putInt(GRUDGE_HITS_KEY, hits);
        data.putLong(GRUDGE_LAST_HIT_KEY, now);

        if (hits < WarriorVillagerConfig.GRUDGE_HITS_TO_ANGER) return false;

        data.putLong(GRUDGE_ANGRY_UNTIL_KEY, now + WarriorVillagerConfig.GRUDGE_ANGER_TICKS);
        return true;
    }

    public static boolean isHoldingGrudge(Villager villager) {
        return villager.getPersistentData().getLong(GRUDGE_ANGRY_UNTIL_KEY) > villager.level().getGameTime();
    }

    /** Обидчик, если он ещё жив и загружен в мире, иначе null. */
    public static LivingEntity resolveGrudgeTarget(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        if (!data.hasUUID(GRUDGE_TARGET_KEY)) return null;
        if (!(villager.level() instanceof ServerLevel serverLevel)) return null;

        Entity entity = serverLevel.getEntity(data.getUUID(GRUDGE_TARGET_KEY));
        if (entity instanceof LivingEntity living && living.isAlive()
                && !(living instanceof Player player && (player.isSpectator() || player.isCreative()))) {
            return living;
        }
        return null;
    }

    public static void forgetGrudge(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        data.remove(GRUDGE_TARGET_KEY);
        data.remove(GRUDGE_HITS_KEY);
        data.remove(GRUDGE_LAST_HIT_KEY);
        data.remove(GRUDGE_ANGRY_UNTIL_KEY);
    }

    /** Житель когда-то был воином и таскает на себе наше снаряжение. */
    public static boolean hasWarriorGear(Villager villager) {
        return villager.getPersistentData().getBoolean(GEAR_KEY);
    }

    /**
     * Снимает всё, что навесил воину мод: снаряжение, 25 HP, дистанцию пики и наши флаги.
     * Вызывается, когда житель перестал быть воином (сломали строп, сменилась профессия).
     * <p>
     * Реролла тут нет: розыгрыш детерминирован по UUID, так что вернув профессию, житель получит
     * ровно тот же комплект. Именно поэтому снимать снаряжение безопасно.
     */
    public static void stripGear(Villager villager) {
        CompoundTag data = villager.getPersistentData();

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.MAINHAND, EquipmentSlot.HEAD,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            villager.setItemSlot(slot, ItemStack.EMPTY);
            villager.setDropChance(slot, WarriorVillagerConfig.EQUIPMENT_DROP_CHANCE);
        }

        AttributeInstance maxHealth = villager.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(WarriorVillagerConfig.DEFAULT_MAX_HEALTH);
            // Здоровье могло быть выше нового максимума - иначе останется 25/20.
            villager.setHealth(Math.min(villager.getHealth(), villager.getMaxHealth()));
        }

        villager.removeTag(LONG_REACH_TAG);
        AttributeInstance reach = villager.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach != null) {
            reach.setBaseValue(reach.getAttribute().value().getDefaultValue());
        }

        AttributeInstance speed = villager.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(SPEED_UPGRADE_ID) != null) {
            speed.removeModifier(SPEED_UPGRADE_ID);
        }

        data.remove(GEAR_KEY);
        data.remove(WEAPON_KEY);
        data.remove(RANGED_KEY);
        data.remove(LONG_REACH_KEY);
        data.remove(STATS_KEY);
        data.remove(ARMOR_TIER_KEY);
        data.remove(LAST_MONSTER_HIT_KEY);
        data.remove(NEXT_BRAIN_HEAL_KEY);
        forgetGrudge(villager);
    }

    private static void equip(Villager villager, EquipmentSlot slot, ItemStack stack) {
        villager.setItemSlot(slot, stack);
        villager.setDropChance(slot, WarriorVillagerConfig.EQUIPMENT_DROP_CHANCE);
    }

    private static WeaponCategory rollCategory(RandomSource random) {
        List<WeaponCategory> categories = WarriorVillagerConfig.WEAPONS;
        int total = 0;
        for (WeaponCategory category : categories) {
            total += category.weight();
        }
        int pick = total > 0 ? random.nextInt(total) : 0;
        for (WeaponCategory category : categories) {
            pick -= category.weight();
            if (pick < 0) return category;
        }
        return categories.getLast();
    }

    private static GearOption roll(RandomSource random, List<GearOption> options) {
        int total = 0;
        for (GearOption option : options) {
            total += option.weight();
        }
        int pick = total > 0 ? random.nextInt(total) : 0;
        for (GearOption option : options) {
            pick -= option.weight();
            if (pick < 0) return option;
        }
        return options.getLast();
    }

    private static ItemStack stackOf(GearOption option) {
        return new ItemStack(option.resolve());
    }

}