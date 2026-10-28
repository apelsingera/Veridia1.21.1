package net.gera.veridia.common.entity.ominous;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.compat.spartanweaponry.SpartanWeaponryCompat;
import net.gera.veridia.common.entity.ominous.rage.OminousRage;
import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Зловещий воин: тяжёлый ближний боец с боевым молотом и щитом.
 * <ul>
 *   <li>Нападает на игроков и на всех остальных мобов, кроме других зловещих воинов. Жители ему почти ничего не
 *       могут сделать даже толпой ({@link OminousWarriorVillagerBalance}).</li>
 *   <li>Появляется в пепельных биомах группами по 3-5. Небольшая часть воинов появляется быстрыми
 *       (постоянная прибавка к скорости, см. SWIFT_CHANCE).</li>
 *   <li>Магмы не боится: ходит по ней ({@link MagmaWalkingNavigation}) и не получает от неё урона.</li>
 *   <li>Увидев цель, сначала бежит к ней (та же анимация walk, только быстрее), потом, сблизившись или через
 *       несколько секунд, идёт обычным шагом. Пока не прошла перезарядка, второй рывок не начинается.</li>
 *   <li>Бьёт анимацией attack_1 (удар правой рукой сверху), иногда attack_2 (удар двумя руками: достаёт дальше,
 *       сильнее отбрасывает и поднимает пыль, если попал). Какой удар будет следующим, решается заранее, и моб
 *       подходит на дальность именно этого удара. Урон наносится в момент, когда рука доходит до цели.</li>
 *   <li>Урон берётся из {@link #doHurtTarget}: оружие в руке (молот) добавляет свой урон и зачарования.</li>
 *   <li>Со щитом во второй руке может закрыться (анимация block), если в него летит снаряд, после сильного
 *       удара или серии ударов; успевает не всегда, шанс зависит от сложности (15/25/35%).
 *       Пока щит поднят, урон спереди блокируется ванильной логикой щита
 *       ({@link #isBlocking}): стрелы отскакивают, атакующего отбрасывает.</li>
 *   <li>Без цели раз в 7 игровых дней молится у духовного рунного камня ({@link OminousWarriorPrayGoal}).</li>
 *   <li>Ярость ({@link OminousRage}): пока она идёт, воины в пепельных биомах разъярены. Красная текстура, столб
 *       частиц, правая рука с оружием поднимается (ragehandup), дальше бег за игроком с поднятой рукой (rage) и удары
 *       прямо на бегу (rageattack). Нападают на всех, кроме своих; игроков ищут дальше и без прямой видимости, они
 *       важнее остальных целей. Ломают блоки на пути ({@link RageBlockBreaker}).
 *       Без оружия рука не поднимается, а качается, и удары обычные. Ярость тратит молитву: следующая не раньше,
 *       чем через 7 дней, иначе после ярости все, кто не успел помолиться, сразу заряжали бы камень заново.</li>
 *   <li>Рядом с камнем, с которого началась ярость, воин усилен ({@link #empower}): к нему тянется луч от камня,
 *       урон и броня выше.</li>
 *   <li>Смерть: анимация death, моб остаётся на месте до её конца.</li>
 * </ul>
 */
public class OminousWarriorEntity extends Monster {
    public final AnimationState attack1AnimationState = new AnimationState();
    public final AnimationState attack2AnimationState = new AnimationState();
    public final AnimationState blockAnimationState = new AnimationState();
    public final AnimationState prayAnimationState = new AnimationState();
    public final AnimationState rageHandUpAnimationState = new AnimationState();
    public final AnimationState rageAttackAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();

    private static final EntityDataAccessor<Boolean> DATA_RUNNING =
            SynchedEntityData.defineId(OminousWarriorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RAGE =
            SynchedEntityData.defineId(OminousWarriorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_EMPOWERED =
            SynchedEntityData.defineId(OminousWarriorEntity.class, EntityDataSerializers.BOOLEAN);

    private static final byte EVENT_ATTACK_1 = 70;
    private static final byte EVENT_ATTACK_2 = 71;
    private static final byte EVENT_BLOCK = 72;
    private static final byte EVENT_PRAY = 73;
    private static final byte EVENT_RAGE_ATTACK = 74;

    // Характеристики. Урон без оружия; молот добавляет свой (у железного боевого молота +6)
    private static final double MAX_HEALTH = 60.0D;
    private static final double WALK_SPEED = 0.32D;
    private static final double ATTACK_DAMAGE = 3.0D;
    private static final double ARMOR = 6.0D;
    private static final double KNOCKBACK_RESISTANCE = 0.5D;
    private static final double FOLLOW_RANGE = 32.0D;
    private static final int EXPERIENCE_REWARD = 20;
    // Естественный спавн группами до 5 (ванильный предел - 4)
    private static final int MAX_SPAWN_GROUP = 5;

    // Снаряжение при появлении (яйцо, спавнер, естественный спавн)
    private static final float WARHAMMER_CHANCE = 1.0F;
    private static final float SHIELD_CHANCE = 1.0F;
    // Часть воинов появляется быстрыми: постоянная прибавка к скорости (сохраняется вместе с мобом)
    private static final float SWIFT_CHANCE = 0.05F;
    private static final double SWIFT_SPEED_BONUS = 0.25D;
    private static final ResourceLocation SWIFT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "ominous_warrior_swift");

    // Бег в начале боя: множитель скорости относительно шага, откуда стартует и когда заканчивается
    private static final double WALK_SPEED_MODIFIER = 1.0D;
    // Бег примерно той же скорости, что раньше (0.26 * 1.5): ускорили только обычный шаг
    private static final double RUN_SPEED_MODIFIER = 1.3D;
    private static final double RUN_MIN_START_DISTANCE = 6.0D;
    private static final double RUN_STOP_DISTANCE = 4.0D;
    private static final int RUN_MAX_TICKS = 80;
    private static final int RUN_COOLDOWN_TICKS = 200;

    // Удары. Тики по анимациям, попадание засчитывается чуть раньше, чем рука доходит до низа, чтобы удар не
    // ощущался запоздалым: attack_1 - рука вверху до 0.375 с, опускается до 0.625 с, урон на 0.35 с;
    // attack_2 - руки вверху на 0.25 с, внизу на 0.46 с, урон на 0.35 с; движение заканчивается на 0.67 с
    private static final float ATTACK_2_CHANCE = 0.3F;
    private static final int ATTACK_1_DURATION_TICKS = 13;
    private static final int ATTACK_1_HIT_TICK = 7;
    private static final int ATTACK_2_DURATION_TICKS = 14;
    private static final int ATTACK_2_HIT_TICK = 7;
    private static final int ATTACK_COOLDOWN_TICKS = 10;
    // Дальность считается между краями коробок моба и цели; в момент удара цель может отойти чуть дальше
    private static final double ATTACK_1_REACH = 1.7D;
    private static final double ATTACK_2_REACH = 2.6D;
    private static final double HIT_EXTRA_REACH = 0.6D;
    private static final float ATTACK_TURN_DEGREES_PER_TICK = 30.0F;
    private static final double ATTACK_2_EXTRA_KNOCKBACK = 0.7D;

    // Пыль от attack_2 при попадании: облако в точке удара и кольцо вокруг
    private static final int SLAM_DUST_BURST_COUNT = 24;
    private static final double SLAM_DUST_BURST_SPREAD = 0.35D;
    private static final double SLAM_DUST_BURST_SPEED = 0.12D;
    private static final int SLAM_DUST_RING_POINTS = 18;
    private static final double SLAM_DUST_RING_RADIUS = 1.3D;

    // Щит. Длина анимации block 0.625 с; пока она идёт, щит поднят
    private static final int BLOCK_DURATION_TICKS = 13;
    private static final int BLOCK_COOLDOWN_TICKS = 10;
    private static final float BLOCK_TURN_DEGREES_PER_TICK = 60.0F;
    // Снаряды: ищем в этом радиусе каждые несколько тиков; закрываемся, если снаряд долетит за столько тиков
    // и пройдёт ближе этого расстояния от центра моба
    private static final double PROJECTILE_SCAN_RADIUS = 16.0D;
    // Каждый тик: стрела с 4-5 блоков долетает за 1-2 тика и иначе проскакивала между проверками
    private static final int PROJECTILE_SCAN_INTERVAL_TICKS = 1;
    private static final int PROJECTILE_REACTION_TICKS = 10;
    private static final double PROJECTILE_HIT_RADIUS = 1.5D;
    private static final double PROJECTILE_MIN_SPEED = 0.2D;
    // Каждый снаряд решается один раз; помним решённые столько тиков (снаряд к этому времени уже долетел)
    private static final int JUDGED_PROJECTILE_MEMORY_TICKS = 40;
    // Удары: сильный удар (урон до брони) или серия ударов подряд
    private static final float HEAVY_HIT_DAMAGE = 6.0F;
    private static final int HIT_SERIES_LENGTH = 3;
    private static final int HIT_SERIES_GAP_TICKS = 30;
    // Шанс успеть поднять щит, когда есть повод (снаряд, сильный удар, серия ударов), по сложности мира
    private static final float BLOCK_CHANCE_EASY = 0.15F;
    private static final float BLOCK_CHANCE_NORMAL = 0.25F;
    private static final float BLOCK_CHANCE_HARD = 0.35F;

    // Молитва: не чаще раза в 7 игровых дней
    private static final long PRAY_COOLDOWN_TICKS = 7L * 24000L;
    private static final long NEVER_PRAYED = Long.MIN_VALUE;

    // Ярость. Бег быстрее обычного шага; поднять руку (ragehandup, 0.46 с) - стоя; удар на бегу (rageattack 0.83 с),
    // урон, когда рука проходит перед собой (0.42 с). Цели ищутся дальше и без прямой видимости
    private static final double RAGE_SPEED_MODIFIER = 1.4D;
    private static final int RAGE_CHECK_INTERVAL_TICKS = 10;
    private static final int RAGE_HAND_UP_TICKS = 10;
    private static final int RAGE_ATTACK_DURATION_TICKS = 17;
    private static final int RAGE_ATTACK_HIT_TICK = 8;
    private static final double RAGE_FOLLOW_RANGE_BONUS = 32.0D;
    private static final ResourceLocation RAGE_FOLLOW_RANGE_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "ominous_warrior_rage_range");
    // Столб частиц над разъярённым воином: сколько за тик, как быстро поднимаются (в единицах частицы пыли)
    private static final int RAGE_PARTICLES_PER_TICK = 2;
    private static final double RAGE_PARTICLE_RISE_SPEED = 2.5D;
    private static final DustParticleOptions RAGE_DUST = new DustParticleOptions(new Vector3f(0.7F, 0.1F, 0.9F), 1.3F);
    private static final int RAGE_START_BURST_COUNT = 30;

    // Усиление от активного духовного камня (пока воин в ярости и в радиусе камня): урон +50%, броня и её прочность
    private static final double EMPOWER_DAMAGE_BONUS = 0.5D;
    private static final double EMPOWER_ARMOR_BONUS = 6.0D;
    private static final double EMPOWER_TOUGHNESS_BONUS = 4.0D;
    private static final ResourceLocation EMPOWER_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "ominous_warrior_empower_damage");
    private static final ResourceLocation EMPOWER_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "ominous_warrior_empower_armor");
    private static final ResourceLocation EMPOWER_TOUGHNESS_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "ominous_warrior_empower_toughness");
    // Усиленный воин дымит гуще
    private static final int EMPOWERED_RAGE_PARTICLES_PER_TICK = 4;

    // Раны от жителей и железных големов затягиваются по столько здоровья в секунду (см. OminousWarriorVillagerBalance)
    private static final float VILLAGER_WOUND_HEAL_PER_SECOND = 2.0F;
    private static final int VILLAGER_WOUND_HEAL_INTERVAL_TICKS = 20;

    private static final int REPATH_MIN_TICKS = 4;
    private static final int REPATH_RANDOM_TICKS = 7;
    // Длина анимации death (1.04 с) в тиках: столько моб лежит, пока не исчезнет
    private static final int DEATH_DURATION_TICKS = 22;

    private static final int NO_ATTACK = 0;
    private static final int ATTACK_1 = 1;
    private static final int ATTACK_2 = 2;
    private static final int RAGE_ATTACK = 3;

    private static final String LAST_PRAYER_KEY = "LastPrayerTime";

    // Сервер
    private int runTicksLeft;
    private int runCooldownTicks;
    private int plannedAttack = ATTACK_1;
    private int attackKind = NO_ATTACK;
    private int attackTicks;
    private int attackCooldownTicks;
    private int repathDelay;
    private int blockTicks;
    private int blockCooldownTicks;
    @Nullable
    private Vec3 blockThreat;
    private int hitStreak;
    private int lastHitTick;
    // id снаряда -> тик, когда решили, закрываться от него или нет
    private final Map<Integer, Integer> judgedProjectiles = new HashMap<>();
    private long lastPrayerTime = NEVER_PRAYED;
    private int rageHandUpTicks;
    // Путь до цели в ярости не найден: идёт к ней напрямую и ломает, что мешает
    private boolean rageDirectMove;
    private final RageBlockBreaker rageBreaker = new RageBlockBreaker(this);
    private int empoweredTicks;
    // Сколько здоровья сняли жители и ещё не затянулось
    private float villagerWounds;

    // Клиент: сколько ещё играют анимации
    private int clientAttackTicksLeft;
    private int clientBlockTicksLeft;
    private int clientPrayTicksLeft;
    private int clientRageHandUpTicksLeft;
    private int clientRageAttackTicksLeft;

    public OminousWarriorEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = EXPERIENCE_REWARD;
        planNextAttack();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RUNNING, false);
        builder.define(DATA_RAGE, false);
        builder.define(DATA_EMPOWERED, false);
    }

    /** Бежит ли моб сейчас (клиенту: шаг играется быстрее). */
    public boolean isRunning() {
        return this.entityData.get(DATA_RUNNING);
    }

    private void setRunning(boolean running) {
        this.entityData.set(DATA_RUNNING, running);
    }

    /** В ярости ли воин (клиенту: текстура, частицы и анимации ярости). */
    public boolean isRaging() {
        return this.entityData.get(DATA_RAGE);
    }

    /** Усилен ли воин активным духовным камнем (клиенту: к нему тянется луч от камня). */
    public boolean isEmpowered() {
        return this.entityData.get(DATA_EMPOWERED);
    }

    /** Есть ли в правой руке оружие (предмет, прибавляющий урон): только тогда в ярости поднимается рука. */
    public boolean isArmed() {
        ItemStack weapon = this.getMainHandItem();
        if (weapon.isEmpty()) {
            return false;
        }
        MutableBoolean addsDamage = new MutableBoolean(false);
        weapon.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE) && modifier.amount() > 0.0D) {
                addsDamage.setTrue();
            }
        });
        return addsDamage.booleanValue();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Открывает двери на пути и закрывает за собой: любые, что открываются рукой - ванильные деревянные и
        // раздвижные/складные двери Create и TFMG (они тоже DoorBlock); железные (только от редстоуна) - нет
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new CombatGoal(this));
        this.goalSelector.addGoal(4, new OminousWarriorPrayGoal(this));
        this.goalSelector.addGoal(5, createStrollGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // В ярости - ближайший игрок даже без прямой видимости, он важнее остальных целей
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                player -> this.isRaging()));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                this::canTargetMob));
    }

    /** Прогулка без цели (Brutal выбирает только места, где помещается стоя). */
    protected Goal createStrollGoal() {
        return new WaterAvoidingRandomStrollGoal(this, 1.0D);
    }

    // Нападает на всех мобов, кроме своих и детей жителей (и в ярости тоже)
    private boolean canTargetMob(LivingEntity entity) {
        return !(entity instanceof OminousWarriorEntity) && !(entity instanceof Villager && entity.isBaby());
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return MAX_SPAWN_GROUP;
    }

    // ---------------------------------------------------------------- снаряжение

    @SuppressWarnings("deprecation")
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.populateDefaultEquipmentSlots(this.random, difficulty);
        if (this.random.nextFloat() < SWIFT_CHANCE) {
            makeSwift();
        }
        return data;
    }

    private void makeSwift() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(SWIFT_MODIFIER_ID)) {
            speed.addPermanentModifier(new AttributeModifier(SWIFT_MODIFIER_ID, SWIFT_SPEED_BONUS,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (random.nextFloat() < WARHAMMER_CHANCE) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(SpartanWeaponryCompat.ironWarhammer()));
        }
        if (random.nextFloat() < SHIELD_CHANCE) {
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
    }

    // ---------------------------------------------------------------- магма

    // Свой поиск пути: магма (ванильная и чернитовая) для него обычная земля, огонь и лава - нет
    @Override
    protected PathNavigation createNavigation(Level level) {
        MagmaWalkingNavigation navigation = new MagmaWalkingNavigation(this, level);
        // Путь строится и через закрытые двери (их откроет OpenDoorGoal)
        navigation.setCanOpenDoors(true);
        return navigation;
    }

    // Магма (ванильная и чернитовая) бьёт уроном hot_floor, его воин не получает
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.HOT_FLOOR) || super.isInvulnerableTo(source);
    }

    private boolean hasShield() {
        return this.getOffhandItem().canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    // ---------------------------------------------------------------- тик

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            clientTick();
        } else {
            serverTick();
        }
    }

    private void serverTick() {
        if (runCooldownTicks > 0) {
            runCooldownTicks--;
        }
        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
        }
        if (blockCooldownTicks > 0) {
            blockCooldownTicks--;
        }
        if (rageHandUpTicks > 0) {
            rageHandUpTicks--;
        }
        if (this.tickCount % RAGE_CHECK_INTERVAL_TICKS == 0 && this.isAlive()) {
            updateRage();
        }
        if (empoweredTicks > 0 && --empoweredTicks == 0) {
            setEmpowered(false);
        }
        if (villagerWounds > 0.0F && this.isAlive() && this.tickCount % VILLAGER_WOUND_HEAL_INTERVAL_TICKS == 0) {
            healVillagerWounds();
        }
        if (blockTicks > 0) {
            tickBlock();
        } else if (this.isAlive() && this.tickCount % PROJECTILE_SCAN_INTERVAL_TICKS == 0) {
            blockIncomingProjectile();
        }
    }

    // ---------------------------------------------------------------- ярость (сервер)

    // Ярость идёт в измерении, а воин стоит в пепельном биоме - разъярён; иначе нет
    private void updateRage() {
        boolean shouldRage = this.level() instanceof ServerLevel level && OminousRage.isActive(level)
                && ModBiomes.isAshBiome(level.getBiome(this.blockPosition()));
        if (shouldRage != this.isRaging()) {
            setRaging(shouldRage);
        }
    }

    private void setRaging(boolean raging) {
        this.entityData.set(DATA_RAGE, raging);
        AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (raging) {
            // Ритуал состоялся - молитва потрачена, следующая через 7 дней
            markPrayed();
            this.rageHandUpTicks = RAGE_HAND_UP_TICKS;
            if (this.isRunning()) {
                stopRun();
            }
            this.attackKind = NO_ATTACK;
            this.attackTicks = 0;
            this.getNavigation().stop();
            if (followRange != null && !followRange.hasModifier(RAGE_FOLLOW_RANGE_ID)) {
                followRange.addTransientModifier(new AttributeModifier(RAGE_FOLLOW_RANGE_ID, RAGE_FOLLOW_RANGE_BONUS,
                        AttributeModifier.Operation.ADD_VALUE));
            }
            this.playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 1.3F);
        } else {
            if (followRange != null) {
                followRange.removeModifier(RAGE_FOLLOW_RANGE_ID);
            }
            this.rageDirectMove = false;
            this.rageBreaker.stop();
            if (empoweredTicks > 0) {
                empoweredTicks = 0;
                setEmpowered(false);
            }
        }
    }

    /**
     * Активный духовный камень рядом: усиление на ticks тиков. Камень продлевает его, пока воин в радиусе;
     * действует только в ярости.
     */
    public void empower(int ticks) {
        if (!this.isRaging() || !this.isAlive()) {
            return;
        }
        if (empoweredTicks <= 0) {
            setEmpowered(true);
        }
        empoweredTicks = Math.max(empoweredTicks, ticks);
    }

    private void setEmpowered(boolean empowered) {
        this.entityData.set(DATA_EMPOWERED, empowered);
        toggleModifier(Attributes.ATTACK_DAMAGE, EMPOWER_DAMAGE_ID, EMPOWER_DAMAGE_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, empowered);
        toggleModifier(Attributes.ARMOR, EMPOWER_ARMOR_ID, EMPOWER_ARMOR_BONUS,
                AttributeModifier.Operation.ADD_VALUE, empowered);
        toggleModifier(Attributes.ARMOR_TOUGHNESS, EMPOWER_TOUGHNESS_ID, EMPOWER_TOUGHNESS_BONUS,
                AttributeModifier.Operation.ADD_VALUE, empowered);
    }

    // Временный модификатор (в сохранение не попадает): снять и, если нужно, повесить заново
    private void toggleModifier(Holder<Attribute> attribute, ResourceLocation id, double amount,
                                AttributeModifier.Operation operation, boolean enabled) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (enabled) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    // ---------------------------------------------------------------- бой (сервер)

    private boolean hasLiveTarget() {
        LivingEntity target = this.getTarget();
        return target != null && target.isAlive() && this.isAlive();
    }

    // Цель только что появилась: если она достаточно далеко и рывок не на перезарядке, начинаем бежать.
    // В ярости рывка нет: воин и так всё время бежит
    private void beginEncounter(LivingEntity target) {
        this.setAggressive(true);
        this.repathDelay = 0;
        if (!this.isRaging() && runCooldownTicks <= 0 && this.distanceTo(target) >= RUN_MIN_START_DISTANCE) {
            this.setRunning(true);
            this.runTicksLeft = RUN_MAX_TICKS;
        }
    }

    private void endEncounter() {
        this.setAggressive(false);
        if (this.isRunning()) {
            stopRun();
        }
        this.attackKind = NO_ATTACK;
        this.attackTicks = 0;
        this.rageDirectMove = false;
        this.rageBreaker.stop();
        this.getNavigation().stop();
    }

    private void combatTick(LivingEntity target) {
        if (blockTicks > 0) {
            return;
        }
        if (rageHandUpTicks > 0) {
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            return;
        }
        if (this.isRaging() && this.isArmed()) {
            rageCombatTick(target);
            return;
        }
        if (attackKind != NO_ATTACK) {
            tickAttack(target);
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        tickRun(target);

        boolean inReach = isWithinReach(target, reachOf(plannedAttack));
        if (inReach) {
            this.getNavigation().stop();
            this.rageBreaker.stop();
        } else if (this.isRaging()) {
            rageChase(target);
        } else {
            chase(target);
        }
        if (inReach && attackCooldownTicks <= 0 && this.getSensing().hasLineOfSight(target)) {
            startAttack(target);
        }
    }

    private void chase(LivingEntity target) {
        if (--repathDelay > 0) {
            return;
        }
        repathDelay = REPATH_MIN_TICKS + this.random.nextInt(REPATH_RANDOM_TICKS);
        this.getNavigation().moveTo(target, isRunning() ? RUN_SPEED_MODIFIER : WALK_SPEED_MODIFIER);
    }

    private void tickRun(LivingEntity target) {
        if (!isRunning()) {
            return;
        }
        runTicksLeft--;
        if (runTicksLeft <= 0 || this.distanceTo(target) <= RUN_STOP_DISTANCE) {
            stopRun();
        }
    }

    private void stopRun() {
        this.setRunning(false);
        this.runCooldownTicks = RUN_COOLDOWN_TICKS;
        this.getNavigation().setSpeedModifier(WALK_SPEED_MODIFIER);
    }

    // Следующий удар выбирается заранее: моб подходит на дальность именно этого удара
    private void planNextAttack() {
        this.plannedAttack = this.random.nextFloat() < ATTACK_2_CHANCE ? ATTACK_2 : ATTACK_1;
    }

    private static double reachOf(int attack) {
        return attack == ATTACK_2 ? ATTACK_2_REACH : ATTACK_1_REACH;
    }

    private void startAttack(LivingEntity target) {
        this.attackKind = plannedAttack;
        this.attackTicks = 0;
        this.getNavigation().stop();
        this.level().broadcastEntityEvent(this, attackKind == ATTACK_2 ? EVENT_ATTACK_2 : EVENT_ATTACK_1);
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
        turnTowards(target.position(), ATTACK_TURN_DEGREES_PER_TICK);
    }

    private void tickAttack(LivingEntity target) {
        turnTowards(target.position(), ATTACK_TURN_DEGREES_PER_TICK);
        attackTicks++;
        if (attackTicks == currentHitTick()) {
            strike(target);
        }
        if (attackTicks >= currentDurationTicks()) {
            finishAttack();
        }
    }

    private void finishAttack() {
        this.attackKind = NO_ATTACK;
        this.attackTicks = 0;
        this.attackCooldownTicks = ATTACK_COOLDOWN_TICKS;
        planNextAttack();
    }

    private int currentHitTick() {
        return switch (attackKind) {
            case ATTACK_2 -> ATTACK_2_HIT_TICK;
            case RAGE_ATTACK -> RAGE_ATTACK_HIT_TICK;
            default -> ATTACK_1_HIT_TICK;
        };
    }

    private int currentDurationTicks() {
        return switch (attackKind) {
            case ATTACK_2 -> ATTACK_2_DURATION_TICKS;
            case RAGE_ATTACK -> RAGE_ATTACK_DURATION_TICKS;
            default -> ATTACK_1_DURATION_TICKS;
        };
    }

    // ---------------------------------------------------------------- бой в ярости (сервер)

    // С оружием в ярости: бежит без остановки и бьёт rageattack прямо на бегу
    private void rageCombatTick(LivingEntity target) {
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        boolean inReach = isWithinReach(target, ATTACK_1_REACH);
        if (inReach) {
            this.rageBreaker.stop();
        } else {
            rageChase(target);
        }
        if (attackKind == RAGE_ATTACK) {
            attackTicks++;
            if (attackTicks == RAGE_ATTACK_HIT_TICK) {
                strike(target);
            }
            if (attackTicks >= RAGE_ATTACK_DURATION_TICKS) {
                finishAttack();
            }
        } else if (inReach && attackCooldownTicks <= 0 && this.getSensing().hasLineOfSight(target)) {
            this.attackKind = RAGE_ATTACK;
            this.attackTicks = 0;
            this.level().broadcastEntityEvent(this, EVENT_RAGE_ATTACK);
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
        }
    }

    // Погоня в ярости: по пути, а если пути до цели нет - напрямик, ломая то, во что упёрся. Напрямик и тогда,
    // когда путь кончился, а цель ещё не достать (навигация останавливается за клетку до цели)
    private void rageChase(LivingEntity target) {
        if (--repathDelay <= 0) {
            repathDelay = REPATH_MIN_TICKS + this.random.nextInt(REPATH_RANDOM_TICKS);
            boolean pathFound = this.getNavigation().moveTo(target, RAGE_SPEED_MODIFIER);
            Path path = this.getNavigation().getPath();
            this.rageDirectMove = !pathFound || path == null || !path.canReach();
            if (rageDirectMove) {
                this.getNavigation().stop();
            }
        }
        if (rageDirectMove || this.getNavigation().isDone()) {
            this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), RAGE_SPEED_MODIFIER);
        }
        if (this.horizontalCollision) {
            this.rageBreaker.tick(target.position());
        } else {
            this.rageBreaker.stop();
        }
    }

    // ---------------------------------------------------------------- попадание

    // Рука дошла до цели: бьём, если цель всё ещё рядом (отступивший удара не получает)
    private void strike(LivingEntity target) {
        if (!target.isAlive() || !isWithinReach(target, reachOf(attackKind) + HIT_EXTRA_REACH)) {
            return;
        }
        boolean hit = this.doHurtTarget(target);
        if (hit && attackKind == ATTACK_2) {
            target.knockback(ATTACK_2_EXTRA_KNOCKBACK, this.getX() - target.getX(), this.getZ() - target.getZ());
            target.hurtMarked = true;
            spawnSlamDust(target);
        }
    }

    // Пыль от удара молотом о землю под целью (частицы того блока, на котором она стоит)
    private void spawnSlamDust(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos groundPos = BlockPos.containing(target.getX(), target.getY() - 0.2D, target.getZ());
        BlockState ground = level.getBlockState(groundPos);
        if (ground.isAir() || ground.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.DUST_PILLAR, ground);
        double x = target.getX();
        double y = groundPos.getY() + 1.0D;
        double z = target.getZ();
        level.sendParticles(dust, x, y, z, SLAM_DUST_BURST_COUNT,
                SLAM_DUST_BURST_SPREAD, 0.0D, SLAM_DUST_BURST_SPREAD, SLAM_DUST_BURST_SPEED);
        for (int i = 0; i < SLAM_DUST_RING_POINTS; i++) {
            double angle = Math.PI * 2.0D * i / SLAM_DUST_RING_POINTS;
            level.sendParticles(dust, x + Math.cos(angle) * SLAM_DUST_RING_RADIUS, y,
                    z + Math.sin(angle) * SLAM_DUST_RING_RADIUS, 1, 0.1D, 0.0D, 0.1D, 0.02D);
        }
    }

    // Расстояние между краями коробок по горизонтали не больше reach, и по высоте коробки пересекаются
    private boolean isWithinReach(LivingEntity target, double reach) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double gap = Math.sqrt(dx * dx + dz * dz) - (this.getBbWidth() + target.getBbWidth()) / 2.0D;
        boolean sameHeight = target.getY() < this.getY() + this.getBbHeight()
                && target.getY() + target.getBbHeight() > this.getY();
        return gap <= reach && sameHeight;
    }

    // Поворачивает моба лицом к точке (тело и голова вместе), не быстрее maxDegrees за тик
    void turnTowards(Vec3 point, float maxDegrees) {
        double dx = point.x - this.getX();
        double dz = point.z - this.getZ();
        if (dx * dx + dz * dz < 1.0E-4D) {
            return;
        }
        float wantedYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float yaw = Mth.approachDegrees(this.getYRot(), wantedYaw, maxDegrees);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    @Override
    protected void playAttackSound() {
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
    }

    // ---------------------------------------------------------------- молитва (сервер)

    /** Можно ли помолиться: ещё не молился или прошло 7 игровых дней. */
    boolean canPrayNow() {
        return lastPrayerTime == NEVER_PRAYED || this.level().getGameTime() - lastPrayerTime >= PRAY_COOLDOWN_TICKS;
    }

    /** Идёт ли сейчас к камню молиться или молится (цель молитвы запущена). */
    boolean isGoingToPray() {
        return this.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.isRunning() && goal.getGoal() instanceof OminousWarriorPrayGoal);
    }

    void markPrayed() {
        this.lastPrayerTime = this.level().getGameTime();
    }

    void playPrayAnimation() {
        this.level().broadcastEntityEvent(this, EVENT_PRAY);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (lastPrayerTime != NEVER_PRAYED) {
            tag.putLong(LAST_PRAYER_KEY, lastPrayerTime);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lastPrayerTime = tag.contains(LAST_PRAYER_KEY) ? tag.getLong(LAST_PRAYER_KEY) : NEVER_PRAYED;
    }

    // ---------------------------------------------------------------- жители (сервер)

    /** Житель или железный голем снял столько здоровья: оно затянется со временем. Урон от игроков сюда не идёт. */
    void woundedByVillageDefender(float damage) {
        villagerWounds += damage;
    }

    private void healVillagerWounds() {
        float healed = Math.min(villagerWounds, VILLAGER_WOUND_HEAL_PER_SECOND);
        villagerWounds -= healed;
        this.heal(healed);
    }

    // ---------------------------------------------------------------- щит (сервер)

    /** Пока поднят щит, ванильный {@code hurt} блокирует урон спереди (как у игрока со щитом). */
    @Override
    public boolean isBlocking() {
        return blockTicks > 0 && hasShield();
    }

    // Поднимает щит, повернувшись к угрозе. Прерывает удар, если он шёл
    private void startBlock(Vec3 threat) {
        if (!hasShield() || blockTicks > 0 || blockCooldownTicks > 0 || !this.isAlive()) {
            return;
        }
        this.blockTicks = BLOCK_DURATION_TICKS;
        this.blockThreat = threat;
        if (attackKind != NO_ATTACK) {
            finishAttack();
        }
        this.getNavigation().stop();
        turnTowards(threat, 180.0F);
        this.level().broadcastEntityEvent(this, EVENT_BLOCK);
    }

    private void tickBlock() {
        this.getNavigation().stop();
        if (blockThreat != null) {
            turnTowards(blockThreat, BLOCK_TURN_DEGREES_PER_TICK);
        }
        blockTicks--;
        if (blockTicks <= 0) {
            this.blockThreat = null;
            this.blockCooldownTicks = BLOCK_COOLDOWN_TICKS;
        }
    }

    // Снаряд летит в моба и долетит скоро: с шансом по сложности закрываемся щитом в сторону, откуда он летит.
    // Каждый снаряд решается один раз, иначе повторные проверки каждые пару тиков подняли бы шанс
    private void blockIncomingProjectile() {
        forgetOldProjectiles();
        if (!hasShield() || blockCooldownTicks > 0) {
            return;
        }
        for (Projectile projectile : this.level().getEntitiesOfClass(Projectile.class,
                this.getBoundingBox().inflate(PROJECTILE_SCAN_RADIUS), this::isIncoming)) {
            if (judgedProjectiles.putIfAbsent(projectile.getId(), this.tickCount) != null) {
                continue;
            }
            if (this.random.nextFloat() < blockChance()) {
                startBlock(projectile.position());
                return;
            }
        }
    }

    private void forgetOldProjectiles() {
        judgedProjectiles.values().removeIf(judgedAt -> this.tickCount - judgedAt > JUDGED_PROJECTILE_MEMORY_TICKS);
    }

    private float blockChance() {
        return switch (this.level().getDifficulty()) {
            case PEACEFUL, EASY -> BLOCK_CHANCE_EASY;
            case NORMAL -> BLOCK_CHANCE_NORMAL;
            case HARD -> BLOCK_CHANCE_HARD;
        };
    }

    private boolean isIncoming(Projectile projectile) {
        if (projectile.getOwner() == this || projectile.isRemoved()) {
            return false;
        }
        Vec3 velocity = projectile.getDeltaMovement();
        double speedSqr = velocity.lengthSqr();
        if (speedSqr < PROJECTILE_MIN_SPEED * PROJECTILE_MIN_SPEED) {
            return false;
        }
        Vec3 toCenter = this.getBoundingBox().getCenter().subtract(projectile.position());
        double ticksToClosest = toCenter.dot(velocity) / speedSqr;
        if (ticksToClosest <= 0.0D || ticksToClosest > PROJECTILE_REACTION_TICKS) {
            return false;
        }
        double missDistance = toCenter.subtract(velocity.scale(ticksToClosest)).length();
        return missDistance < PROJECTILE_HIT_RADIUS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !this.level().isClientSide() && this.isAlive()) {
            maybeBlockAfterHit(source, amount);
        }
        return damaged;
    }

    // После сильного удара или серии ударов подряд (если понятно, откуда бьют) с шансом по сложности закрывается.
    // Не успел - серия продолжается, и следующий удар даёт ещё одну попытку
    private void maybeBlockAfterHit(DamageSource source, float amount) {
        Vec3 from = source.getSourcePosition();
        if (from == null || source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            return;
        }
        hitStreak = this.tickCount - lastHitTick <= HIT_SERIES_GAP_TICKS ? hitStreak + 1 : 1;
        lastHitTick = this.tickCount;
        boolean heavyHit = amount >= HEAVY_HIT_DAMAGE;
        boolean hitSeries = hitStreak >= HIT_SERIES_LENGTH;
        if ((heavyHit || hitSeries) && this.random.nextFloat() < blockChance()) {
            hitStreak = 0;
            startBlock(from);
        }
    }

    // ---------------------------------------------------------------- анимации (клиент)

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_ATTACK_1 -> startAttackAnimation(attack1AnimationState, attack2AnimationState, ATTACK_1_DURATION_TICKS);
            case EVENT_ATTACK_2 -> startAttackAnimation(attack2AnimationState, attack1AnimationState, ATTACK_2_DURATION_TICKS);
            case EVENT_BLOCK -> startBlockAnimation();
            case EVENT_PRAY -> {
                prayAnimationState.start(this.tickCount);
                this.clientPrayTicksLeft = OminousWarriorPrayGoal.PRAY_DURATION_TICKS;
            }
            case EVENT_RAGE_ATTACK -> {
                rageAttackAnimationState.start(this.tickCount);
                this.clientRageAttackTicksLeft = RAGE_ATTACK_DURATION_TICKS;
            }
            default -> super.handleEntityEvent(id);
        }
    }

    // Ярость началась: если есть оружие - поднимает руку, вокруг вспышка частиц
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!DATA_RAGE.equals(key) || !this.level().isClientSide()) {
            return;
        }
        prayAnimationState.stop();
        rageAttackAnimationState.stop();
        rageHandUpAnimationState.stop();
        this.clientRageHandUpTicksLeft = 0;
        if (this.isRaging()) {
            if (this.isArmed()) {
                rageHandUpAnimationState.start(this.tickCount);
                this.clientRageHandUpTicksLeft = RAGE_HAND_UP_TICKS;
            }
            for (int i = 0; i < RAGE_START_BURST_COUNT; i++) {
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(0.8D), this.getRandomY(),
                        this.getRandomZ(0.8D), 0.0D, 0.1D, 0.0D);
            }
        }
    }

    private void startAttackAnimation(AnimationState started, AnimationState other, int durationTicks) {
        other.stop();
        started.start(this.tickCount);
        this.clientAttackTicksLeft = durationTicks;
    }

    private void startBlockAnimation() {
        attack1AnimationState.stop();
        attack2AnimationState.stop();
        this.clientAttackTicksLeft = 0;
        blockAnimationState.start(this.tickCount);
        this.clientBlockTicksLeft = BLOCK_DURATION_TICKS;
    }

    private void clientTick() {
        if (clientAttackTicksLeft > 0 || clientBlockTicksLeft > 0) {
            // Тело разворачивается за головой (сервер поворачивает моба к цели), иначе замах и щит шли бы вбок
            this.yBodyRot = Mth.approachDegrees(this.yBodyRot, this.yHeadRot, BLOCK_TURN_DEGREES_PER_TICK);
        }
        clientAttackTicksLeft = tickDown(clientAttackTicksLeft, attack1AnimationState, attack2AnimationState);
        clientBlockTicksLeft = tickDown(clientBlockTicksLeft, blockAnimationState);
        clientPrayTicksLeft = tickDown(clientPrayTicksLeft, prayAnimationState);
        clientRageHandUpTicksLeft = tickDown(clientRageHandUpTicksLeft, rageHandUpAnimationState);
        clientRageAttackTicksLeft = tickDown(clientRageAttackTicksLeft, rageAttackAnimationState);
        if (this.isRaging() && this.isAlive()) {
            spawnRageParticles();
        }
    }

    // Отсчёт тиков анимации: на последнем тике анимации останавливаются
    private static int tickDown(int ticksLeft, AnimationState... states) {
        if (ticksLeft <= 0) {
            return 0;
        }
        if (ticksLeft == 1) {
            for (AnimationState state : states) {
                state.stop();
            }
        }
        return ticksLeft - 1;
    }

    // Столб фиолетовых частиц, поднимающихся от разъярённого воина (у усиленного камнем - гуще)
    private void spawnRageParticles() {
        int count = this.isEmpowered() ? EMPOWERED_RAGE_PARTICLES_PER_TICK : RAGE_PARTICLES_PER_TICK;
        for (int i = 0; i < count; i++) {
            this.level().addParticle(RAGE_DUST, this.getRandomX(0.6D), this.getY() + this.random.nextDouble() * 0.5D,
                    this.getRandomZ(0.6D), 0.0D, RAGE_PARTICLE_RISE_SPEED, 0.0D);
        }
    }

    // ---------------------------------------------------------------- смерть

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide()) {
            this.setRunning(false);
            this.attackKind = NO_ATTACK;
            this.blockTicks = 0;
            this.rageBreaker.stop();
        }
        super.die(damageSource);
    }

    // Как ванильный tickDeath, но моб исчезает только после конца анимации death
    @Override
    protected void tickDeath() {
        if (this.deathTime == 0 && this.level().isClientSide()) {
            attack1AnimationState.stop();
            attack2AnimationState.stop();
            blockAnimationState.stop();
            prayAnimationState.stop();
            rageHandUpAnimationState.stop();
            rageAttackAnimationState.stop();
            deathAnimationState.start(this.tickCount);
        }
        this.deathTime++;
        if (this.deathTime >= DEATH_DURATION_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    // ---------------------------------------------------------------- звуки

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VINDICATOR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VINDICATOR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VINDICATOR_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }

    // ---------------------------------------------------------------- цель

    private static class CombatGoal extends Goal {
        private final OminousWarriorEntity warrior;

        CombatGoal(OminousWarriorEntity warrior) {
            this.warrior = warrior;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return warrior.hasLiveTarget();
        }

        @Override
        public boolean canContinueToUse() {
            return warrior.hasLiveTarget();
        }

        @Override
        public void start() {
            LivingEntity target = warrior.getTarget();
            if (target != null) {
                warrior.beginEncounter(target);
            }
        }

        @Override
        public void stop() {
            warrior.endEncounter();
        }

        @Override
        public void tick() {
            LivingEntity target = warrior.getTarget();
            if (target != null) {
                warrior.combatTick(target);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }
}
