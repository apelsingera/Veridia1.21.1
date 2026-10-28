package net.gera.veridia.common.entity.cinderroamer;

import net.gera.veridia.common.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.gera.veridia.common.entity.ominous.MagmaWalkingNavigation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Пеплоход: большой ящер с костяными шипами на спине.
 * <ul>
 *   <li>Дикий агрессивен: нападает на игроков, которых видит (кроме мирной сложности), и на всех, кто его ударил. Ходит
 *       медленно, за целью бежит (та же анимация walk, только быстрее).</li>
 *   <li>Удары: спереди укус (attack_bite - близко, очень больно) и удар шипастой головой (attackspikehit - чуть дальше,
 *       чуть слабее); хвостом - удар по земле (attack_tail_smash - сильно, замедляет, пыль от земли) и взмах в стороны
 *       (attack_tail_sides - дуга позади: у кончика хвоста сильнее, ближе к телу слабее). Для удара хвостом
 *       разворачивается к жертве задом; если жертва и так сзади - бьёт сразу.</li>
 *   <li>Ночью спит (sleepstart, sleeping, sleepend); просыпается утром или от удара.</li>
 *   <li>Приручается сырым мясом (шанс, как у волка). Прирученный ходит за хозяином и защищает его.</li>
 *   <li>Хозяин спиливает шипы на спине молибденовой ножовкой: клик - один шип (sspike1..5, спереди назад). Без шипов
 *       можно надеть сёдла: первое спереди, второе сзади (ПКМ седлом или в окне). Хозяин садится в переднее (ПКМ
 *       пустой рукой) и управляет, Ctrl - бег; на заднее можно посадить второго игрока. Верхом таранит рогами мобов
 *       перед мордой: урон и отброс, на бегу сильнее.</li>
 *   <li>С двумя сёдлами ПКМ сундуком вешает рюкзаки backpack1..3 по одному, в каждом по 15 ячеек. Окно (как у
 *       лошади): Shift+ПКМ пустой рукой или E верхом; рюкзаки переключаются вкладками 1-3.</li>
 *   <li>Shift+ПКМ ножницами снимает последний рюкзак: выпадают сундук и всё, что в нём было.</li>
 *   <li>Лечь/встать по команде: кнопкой в окне или ПКМ пустой рукой, пока седла нет.</li>
 *   <li>Магмы не боится: ходит по ней (MagmaWalkingNavigation) и не получает от неё урона.</li>
 * </ul>
 */
public class CinderRoamerEntity extends TamableAnimal implements HasCustomInventoryScreen {
    public static final int BACK_SPIKES = 5;

    // Инвентарь: два слота сёдел, потом рюкзаки по BACKPACK_SLOTS ячеек
    public static final int SADDLE_SLOTS = 2;
    public static final int MAX_BACKPACKS = 3;
    public static final int BACKPACK_SLOTS = 15;
    public static final int INVENTORY_SIZE = SADDLE_SLOTS + MAX_BACKPACKS * BACKPACK_SLOTS;

    public final AnimationState biteAnimationState = new AnimationState();
    public final AnimationState spikeHitAnimationState = new AnimationState();
    public final AnimationState tailSmashAnimationState = new AnimationState();
    public final AnimationState tailSidesAnimationState = new AnimationState();
    public final AnimationState sleepStartAnimationState = new AnimationState();
    public final AnimationState sleepingAnimationState = new AnimationState();
    public final AnimationState sleepEndAnimationState = new AnimationState();

    private static final EntityDataAccessor<Integer> DATA_SAWN_SPIKES =
            SynchedEntityData.defineId(CinderRoamerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SADDLES =
            SynchedEntityData.defineId(CinderRoamerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BACKPACKS =
            SynchedEntityData.defineId(CinderRoamerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(CinderRoamerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RUNNING =
            SynchedEntityData.defineId(CinderRoamerEntity.class, EntityDataSerializers.BOOLEAN);

    // Характеристики
    private static final double MAX_HEALTH = 80.0D;
    private static final double WALK_SPEED = 0.16D;
    private static final double ARMOR = 6.0D;
    private static final double KNOCKBACK_RESISTANCE = 0.7D;
    private static final double FOLLOW_RANGE = 24.0D;
    // Залезает на блок без прыжка (как лошадь), в том числе под седоком
    private static final double STEP_HEIGHT = 1.0D;

    // Скорость относительно шага: прогулка, за хозяином, за целью шагом и бегом. Начинает бежать, если цель дальше
    // RUN_START_DISTANCE, и переходит на шаг ближе RUN_STOP_DISTANCE - запас, чтобы на границе не дёргался бег-шаг-бег
    private static final double STROLL_SPEED_MODIFIER = 0.8D;
    private static final double FOLLOW_OWNER_SPEED_MODIFIER = 1.2D;
    private static final double CHASE_WALK_SPEED_MODIFIER = 1.0D;
    private static final double CHASE_RUN_SPEED_MODIFIER = 2.4D;
    private static final double RUN_START_DISTANCE = 7.0D;
    private static final double RUN_STOP_DISTANCE = 4.0D;

    // Анимация walk: шаг - с её родной скоростью, бег - быстрее; между ними скорость проигрывания меняется плавно,
    // время анимации не прыгает (переход без рывка)
    private static final float WALK_ANIMATION_RATE = 1.0F;
    private static final float RUN_ANIMATION_RATE = 1.8F;
    private static final float ANIMATION_RATE_CHANGE_PER_TICK = 0.08F;
    private static final float WALK_ANIMATION_WRAP_TICKS = 20.0F * 100.0F;

    // Верхом: скорость шага и бега (Ctrl) по ровной земле, в блоках в секунду
    private static final double RIDDEN_WALK_BLOCKS_PER_SECOND = 5.0D;
    private static final double RIDDEN_RUN_BLOCKS_PER_SECOND = 10.0D;
    // Под седоком сущность разгоняется до (скорость x 2.16) блоков за тик на обычном блоке: ввод седока 0.98, трение
    // 0.6 * 0.91 - установившийся шаг = ускорение / (1 - 0.546)
    private static final double RIDDEN_BLOCKS_PER_TICK_PER_SPEED = 0.98D / (1.0D - 0.6D * 0.91D);

    // Таран рогами под седоком: с какой скорости (блоков за тик) бьёт и с какой - в полную силу (на бегу); как далеко
    // от морды достаёт; урон и отброс на шаге и на бегу; как часто один и тот же моб может получить
    private static final double RAM_MIN_SPEED = 0.08D;
    private static final double RAM_FULL_SPEED = 0.45D;
    private static final double RAM_REACH = 0.9D;
    private static final float RAM_WALK_DAMAGE = 4.0F;
    private static final float RAM_RUN_DAMAGE = 9.0F;
    private static final double RAM_WALK_KNOCKBACK = 0.8D;
    private static final double RAM_RUN_KNOCKBACK = 1.6D;
    private static final double RAM_TOSS = 0.3D;
    private static final int RAM_COOLDOWN_TICKS = 10;

    // Дикий нападает на игроков, которых видит (на мирной сложности - нет)
    private static final int PLAYER_TARGET_INTERVAL = 10;

    // Где сидят седоки (по текстурам сёдел): вперёд (+) или назад (-) от центра и высота спины, в блоках
    private record Seat(double forward, double height) {
    }

    private static final Seat FRONT_SEAT = new Seat(0.36D, 1.5D);
    private static final Seat REAR_SEAT = new Seat(-0.99D, 1.45D);

    // Приручение сырым мясом и лечение им прирученного
    private static final float TAME_CHANCE = 1.0F / 3.0F;
    private static final float MEAT_HEAL = 6.0F;

    // Где что у модели (в блоках от центра по оси тела): перед головы, корень и кончик хвоста
    private static final double HEAD_FRONT = 1.9D;
    private static final double TAIL_BASE_BACK = 1.4D;
    private static final double TAIL_TIP_BACK = 2.9D;

    // Укус: близко, очень больно. Удар шипастой головой: чуть дальше и чуть слабее, подбрасывает
    private static final double BITE_REACH = 1.0D;
    private static final float BITE_DAMAGE = 14.0F;
    private static final double BITE_KNOCKBACK = 0.4D;
    private static final double SPIKE_HIT_REACH = 2.0D;
    private static final float SPIKE_HIT_DAMAGE = 10.0F;
    private static final double SPIKE_HIT_KNOCKBACK = 1.0D;
    private static final double SPIKE_HIT_TOSS = 0.35D;
    // Удар хвостом по земле: круг у кончика хвоста, сильно и замедление
    private static final double TAIL_SMASH_RADIUS = 1.8D;
    private static final float TAIL_SMASH_DAMAGE = 16.0F;
    private static final int TAIL_SMASH_SLOW_TICKS = 100;
    private static final int TAIL_SMASH_SLOW_LEVEL = 1;
    private static final double TAIL_SMASH_KNOCKBACK = 0.6D;
    // Взмах хвостом: дуга позади от корня хвоста. Дальняя полоса (у кончика) - сильнее, всё внутри - слабее.
    // Дуга заходит на бока до бёдер: TAIL_SIDES_MIN_BACK_COS - косинус наибольшего угла от направления назад
    private static final double TAIL_SIDES_OUTER = 2.8D;
    private static final double TAIL_SIDES_STRONG_FROM = 1.6D;
    private static final double TAIL_SIDES_MIN_BACK_COS = -0.34D;
    private static final float TAIL_SIDES_STRONG_DAMAGE = 12.0F;
    private static final float TAIL_SIDES_WEAK_DAMAGE = 7.0F;
    private static final double TAIL_SIDES_STRONG_KNOCKBACK = 1.2D;
    private static final double TAIL_SIDES_WEAK_KNOCKBACK = 0.7D;
    // Кто выше или ниже этого (от ног пеплохода), до того не достаёт
    private static final double HIT_BELOW = 0.5D;
    private static final double HIT_ABOVE = 2.2D;

    // Выбор удара: веса укуса, удара головой, удара хвостом по земле и взмаха хвостом
    private static final int BITE_WEIGHT = 35;
    private static final int SPIKE_HIT_WEIGHT = 25;
    private static final int TAIL_SMASH_WEIGHT = 20;
    private static final int TAIL_SIDES_WEIGHT = 20;
    private static final int ATTACK_COOLDOWN_TICKS = 15;
    // Разворот: передом к цели и задом к цели (большое тело - медленнее), насколько точно надо встать
    private static final float FACE_TURN_DEGREES_PER_TICK = 15.0F;
    private static final float TAIL_TURN_DEGREES_PER_TICK = 12.0F;
    private static final double FACING_COS = 0.85D;
    // Для удара хвостом подходит на это расстояние (центр - цель), потом разворачивается; не вышло за столько тиков - кусает
    private static final double TAIL_SETUP_DISTANCE = 3.4D;
    private static final int TAIL_SETUP_TIMEOUT_TICKS = 80;

    // Сон: ночью, без цели, с шансом 1 к SLEEP_CHANCE за проверку; прирученный - только рядом с хозяином
    private static final int SLEEP_CHANCE = 100;
    private static final double SLEEP_OWNER_DISTANCE = 12.0D;
    // Длины анимаций sleepstart и sleepend: лёг - потом поза sleeping; встаёт - стоит на месте
    private static final int LIE_DOWN_TICKS = 10;
    private static final int STAND_UP_TICKS = 12;
    // Лёжа (сон или по команде) восстанавливает LYING_HEAL_AMOUNT здоровья раз в LYING_HEAL_INTERVAL_TICKS (15 с)
    private static final float LYING_HEAL_AMOUNT = 1.0F;
    private static final int LYING_HEAL_INTERVAL_TICKS = 15 * 20;

    private static final double SPIKE_HEIGHT = 1.75D;
    // Где шипы sspike1..5 (в блоках вперёд от центра) - для крошки при спиливании
    private static final double[] SPIKE_FORWARD = {0.94D, 0.44D, -0.06D, -0.56D, -1.06D};
    private static final int SAW_PARTICLES = 12;
    private static final int SLAM_DUST_BURST = 28;
    private static final int SLAM_DUST_RING_POINTS = 16;

    private static final String SAWN_SPIKES_KEY = "SawnSpikes";
    private static final String BACKPACKS_KEY = "Backpacks";
    // Сёдла и рюкзаки лежат в инвентаре; LEGACY_SADDLED - флаг из версии с одним седлом
    private static final String LEGACY_SADDLED_KEY = "Saddled";

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);

    // Сервер
    @Nullable
    private Attack attack;
    private Attack plannedAttack = Attack.BITE;
    private int attackTicks;
    private int attackCooldownTicks;
    private int tailSetupTicks;
    // Сам повернулся к цели задом (для удара хвостом); сбрасывается, когда снова бьёт мордой
    private boolean backTurnedOnPurpose;
    private int standUpTicks;
    // Лежал по команде, но его ударили: встал драться, после боя ляжет обратно
    private boolean liesDownAfterFight;
    private int lyingHealTicks;
    private final Set<Integer> hitThisAttack = new HashSet<>();
    // Таран: где был тик назад (для скорости под седоком), кого и до какого тика больше не задевать
    @Nullable
    private Vec3 lastTickPosition;
    private final Map<Integer, Long> ramCooldownUntil = new HashMap<>();
    private int ramAnimationCooldown;

    // Клиент
    private int clientAttackTicksLeft;
    // Время анимации walk (в тиках) и скорость её проигрывания; копится непрерывно
    private float walkAnimationTicks;
    private float walkAnimationTicksO;
    private float walkAnimationRate = WALK_ANIMATION_RATE;
    private int clientLieDownTicksLeft;
    private int clientStandUpTicksLeft;
    private boolean clientWasLying;

    public CinderRoamerEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        planNextAttack();
        // Число сёдел (для текстуры и мест) - из слотов сёдел; клиенту приходит через данные сущности
        this.inventory.addListener(container -> {
            if (!this.level().isClientSide()) {
                this.entityData.set(DATA_SADDLES, countSaddles());
            }
        });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.STEP_HEIGHT, STEP_HEIGHT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SAWN_SPIKES, 0);
        builder.define(DATA_SADDLES, 0);
        builder.define(DATA_BACKPACKS, 0);
        builder.define(DATA_SLEEPING, false);
        builder.define(DATA_RUNNING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        SitWhenOrderedToGoal lieDown = new SitWhenOrderedToGoal(this);
        // Лёжа и голову не крутит: иначе тело разворачивалось бы за взглядом
        lieDown.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        this.goalSelector.addGoal(1, lieDown);
        this.goalSelector.addGoal(2, new CombatGoal());
        this.goalSelector.addGoal(3, new SleepGoal());
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, FOLLOW_OWNER_SPEED_MODIFIER, 10.0F, 3.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, STROLL_SPEED_MODIFIER));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, PLAYER_TARGET_INTERVAL,
                true, false, player -> !this.isTame() && this.level().getDifficulty() != Difficulty.PEACEFUL));
    }

    // ---------------------------------------------------------------- состояние

    /** Сколько шипов на спине уже спилено (0..BACK_SPIKES), спереди назад. */
    public int getSawnSpikes() {
        return this.entityData.get(DATA_SAWN_SPIKES);
    }

    private void setSawnSpikes(int sawn) {
        this.entityData.set(DATA_SAWN_SPIKES, Mth.clamp(sawn, 0, BACK_SPIKES));
    }

    public boolean hasBackSpikes() {
        return getSawnSpikes() < BACK_SPIKES;
    }

    /** Сколько сёдел надето (0..SADDLE_SLOTS): первое спереди, второе сзади. */
    public int getSaddles() {
        return this.entityData.get(DATA_SADDLES);
    }

    public boolean isSaddled() {
        return getSaddles() > 0;
    }

    // Сёдла надеваются по порядку (заднее - только после переднего), поэтому считаем подряд с первого слота
    private int countSaddles() {
        int saddles = 0;
        while (saddles < SADDLE_SLOTS && this.inventory.getItem(saddles).is(Items.SADDLE)) {
            saddles++;
        }
        return saddles;
    }

    /** Сколько рюкзаков висит (0..MAX_BACKPACKS): backpack1, backpack2, backpack3 по порядку. */
    public int getBackpacks() {
        return this.entityData.get(DATA_BACKPACKS);
    }

    private void setBackpacks(int backpacks) {
        this.entityData.set(DATA_BACKPACKS, Mth.clamp(backpacks, 0, MAX_BACKPACKS));
    }

    /** Ячейка инвентаря: index-я в рюкзаке backpack (оба с нуля). */
    public static int backpackSlot(int backpack, int index) {
        return SADDLE_SLOTS + backpack * BACKPACK_SLOTS + index;
    }

    /** Инвентарь (сёдла и рюкзаки) - для окна. */
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    private boolean isSleepingNow() {
        return this.entityData.get(DATA_SLEEPING);
    }

    private void setSleepingNow(boolean sleeping) {
        if (sleeping == isSleepingNow()) {
            return;
        }
        this.entityData.set(DATA_SLEEPING, sleeping);
        if (!sleeping) {
            this.standUpTicks = STAND_UP_TICKS;
        }
    }

    /** Лежит: спит или прирученный лёг по команде хозяина (одни и те же анимации). */
    public boolean isLying() {
        return isSleepingNow() || this.isInSittingPose();
    }

    /** Бежит (клиенту: шаг играется быстрее). */
    public boolean isRunning() {
        return this.entityData.get(DATA_RUNNING);
    }

    private void setRunning(boolean running) {
        if (running != isRunning()) {
            this.entityData.set(DATA_RUNNING, running);
        }
    }

    // Не размножается
    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    // ---------------------------------------------------------------- магма

    // Магма (ванильная и чернитовая) для него обычная земля: ходит по ней и не получает урона hot_floor
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new MagmaWalkingNavigation(this, level);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.HOT_FLOOR) || super.isInvulnerableTo(source);
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
        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
        }
        if (standUpTicks > 0) {
            standUpTicks--;
            this.getNavigation().stop();
        }
        // Сняли седло в окне - лишний седок слезает
        if (this.getPassengers().size() > getSaddles()) {
            this.getPassengers().get(this.getPassengers().size() - 1).stopRiding();
        }
        if (this.getControllingPassenger() instanceof Player rider) {
            setRunning(rider.isSprinting() && rider.zza > 0.0F);
            ramWithHorns();
        } else if (this.getTarget() == null) {
            setRunning(false);
        }
        if (ramAnimationCooldown > 0) {
            ramAnimationCooldown--;
        }
        tickLyingRecovery();
        lastTickPosition = this.position();
    }

    // ---------------------------------------------------------------- таран рогами (верхом)

    // Под седоком идёт вперёд - мобы перед мордой получают урон и отлетают в стороны. Скорость считаем по сдвигу за тик:
    // под седоком пеплохода двигает клиент седока, своя скорость на сервере - ноль
    private void ramWithHorns() {
        if (lastTickPosition == null) {
            return;
        }
        Vec3 step = this.position().subtract(lastTickPosition);
        double speed = Math.sqrt(step.x * step.x + step.z * step.z);
        if (speed < RAM_MIN_SPEED || step.x * forward().x + step.z * forward().z <= 0.0D) {
            return;
        }
        float power = (float) Mth.clamp((speed - RAM_MIN_SPEED) / (RAM_FULL_SPEED - RAM_MIN_SPEED), 0.0D, 1.0D);
        Vec3 horns = pointAlongBody(HEAD_FRONT);
        long now = this.level().getGameTime();
        boolean hitSomething = false;
        for (Mob victim : this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(HEAD_FRONT + RAM_REACH),
                this::canRam)) {
            if (gapTo(horns, victim) > RAM_REACH || now < ramCooldownUntil.getOrDefault(victim.getId(), 0L)) {
                continue;
            }
            ramCooldownUntil.put(victim.getId(), now + RAM_COOLDOWN_TICKS);
            if (victim.hurt(this.damageSources().mobAttack(this), Mth.lerp(power, RAM_WALK_DAMAGE, RAM_RUN_DAMAGE))) {
                push(victim, this.position(), Mth.lerp(power, RAM_WALK_KNOCKBACK, RAM_RUN_KNOCKBACK), RAM_TOSS);
                hitSomething = true;
            }
        }
        ramCooldownUntil.values().removeIf(until -> until < now);
        if (hitSomething) {
            this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 1.2F);
            if (ramAnimationCooldown <= 0) {
                // Взмах головой - та же анимация, что у удара шипастой головой
                this.level().broadcastEntityEvent(this, Attack.SPIKE_HIT.event);
                ramAnimationCooldown = Attack.SPIKE_HIT.durationTicks;
            }
        }
    }

    // Таран задевает мобов, кроме седоков, других пеплоходов и питомцев хозяина; игроков не трогает
    private boolean canRam(Mob mob) {
        if (!mob.isAlive() || mob.getVehicle() == this || mob instanceof CinderRoamerEntity) {
            return false;
        }
        LivingEntity owner = this.getOwner();
        return owner == null || !mob.isAlliedTo(owner);
    }

    // ---------------------------------------------------------------- бой (сервер)

    private enum Attack {
        BITE(10, (byte) 70),
        SPIKE_HIT(14, (byte) 71),
        TAIL_SMASH(11, (byte) 72),
        TAIL_SIDES(23, (byte) 73);

        final int durationTicks;
        final byte event;

        Attack(int durationTicks, byte event) {
            this.durationTicks = durationTicks;
            this.event = event;
        }

        boolean usesTail() {
            return this == TAIL_SMASH || this == TAIL_SIDES;
        }
    }

    private void planNextAttack() {
        int roll = this.random.nextInt(BITE_WEIGHT + SPIKE_HIT_WEIGHT + TAIL_SMASH_WEIGHT + TAIL_SIDES_WEIGHT);
        if ((roll -= BITE_WEIGHT) < 0) {
            plannedAttack = Attack.BITE;
        } else if ((roll -= SPIKE_HIT_WEIGHT) < 0) {
            plannedAttack = Attack.SPIKE_HIT;
        } else if (roll - TAIL_SMASH_WEIGHT < 0) {
            plannedAttack = Attack.TAIL_SMASH;
        } else {
            plannedAttack = Attack.TAIL_SIDES;
        }
        tailSetupTicks = 0;
    }

    private boolean canFight() {
        LivingEntity target = this.getTarget();
        return target != null && target.isAlive() && this.isAlive() && !this.isVehicle() && !this.isLying()
                && standUpTicks <= 0;
    }

    private void combatTick(LivingEntity target) {
        if (attack != null) {
            tickAttack(target);
            return;
        }
        if (!isBehind(target)) {
            backTurnedOnPurpose = false;
        }
        Attack ready = attackCooldownTicks > 0 ? null : readyAttack(target);
        if (ready != null) {
            startAttack(ready);
            return;
        }
        // Цель сама зашла за спину в досягаемости хвоста - не разворачиваться к ней мордой, бить хвостом
        if (isBehindByItself(target) && !plannedAttack.usesTail() && inTailReach(target)) {
            plannedAttack = inTailSmashZone(target) ? Attack.TAIL_SMASH : Attack.TAIL_SIDES;
            tailSetupTicks = 0;
        }
        if (plannedAttack.usesTail()) {
            setUpTailAttack(target);
        } else {
            approachFront(target);
        }
    }

    // Сзади, но не потому, что сам повернулся задом для удара хвостом (тогда следующий удар выбирается заново,
    // и для укуса он развернётся мордой)
    private boolean isBehindByItself(LivingEntity target) {
        return isBehind(target) && !backTurnedOnPurpose;
    }

    private boolean inTailReach(LivingEntity target) {
        return inTailSmashZone(target) || tailSidesDamage(target) > 0.0F;
    }

    // Удар, который достанет цель прямо сейчас. Цель сама зашла сзади - хвостом, даже если собирался кусать
    @Nullable
    private Attack readyAttack(LivingEntity target) {
        if (isBehind(target) && (plannedAttack.usesTail() || !backTurnedOnPurpose)) {
            if (plannedAttack == Attack.TAIL_SMASH && inTailSmashZone(target)) {
                return Attack.TAIL_SMASH;
            }
            if (tailSidesDamage(target) > 0.0F) {
                return Attack.TAIL_SIDES;
            }
            return inTailSmashZone(target) ? Attack.TAIL_SMASH : null;
        }
        if (plannedAttack.usesTail() || !isFacing(target)) {
            return null;
        }
        double reach = plannedAttack == Attack.BITE ? BITE_REACH : SPIKE_HIT_REACH;
        return headGap(target) <= reach ? plannedAttack : null;
    }

    private void approachFront(LivingEntity target) {
        double reach = plannedAttack == Attack.BITE ? BITE_REACH : SPIKE_HIT_REACH;
        if (headGap(target) <= reach) {
            this.getNavigation().stop();
            setRunning(false);
            turnTowards(target.position(), FACE_TURN_DEGREES_PER_TICK);
            return;
        }
        chase(target);
    }

    // Подойти, развернуться задом и ударить хвостом. Не получилось за TAIL_SETUP_TIMEOUT_TICKS - лучше укусить
    private void setUpTailAttack(LivingEntity target) {
        if (++tailSetupTicks > TAIL_SETUP_TIMEOUT_TICKS) {
            plannedAttack = Attack.BITE;
            return;
        }
        if (horizontalDistance(target.position()) > TAIL_SETUP_DISTANCE) {
            chase(target);
            return;
        }
        this.getNavigation().stop();
        setRunning(false);
        backTurnedOnPurpose = true;
        turnAwayFrom(target.position(), TAIL_TURN_DEGREES_PER_TICK);
    }

    private void chase(LivingEntity target) {
        double distance = horizontalDistance(target.position());
        boolean run = isRunning() ? distance > RUN_STOP_DISTANCE : distance > RUN_START_DISTANCE;
        setRunning(run);
        this.getNavigation().moveTo(target, run ? CHASE_RUN_SPEED_MODIFIER : CHASE_WALK_SPEED_MODIFIER);
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void startAttack(Attack chosen) {
        this.attack = chosen;
        this.attackTicks = 0;
        this.backTurnedOnPurpose = chosen.usesTail();
        this.hitThisAttack.clear();
        this.getNavigation().stop();
        setRunning(false);
        this.level().broadcastEntityEvent(this, chosen.event);
    }

    private void tickAttack(LivingEntity target) {
        attackTicks++;
        this.getNavigation().stop();
        if (attack.usesTail()) {
            turnAwayFrom(target.position(), TAIL_TURN_DEGREES_PER_TICK);
        } else {
            turnTowards(target.position(), FACE_TURN_DEGREES_PER_TICK);
        }
        switch (attack) {
            case BITE -> {
                if (attackTicks == 7) {
                    hitFront(BITE_REACH, BITE_DAMAGE, BITE_KNOCKBACK, 0.0D);
                }
            }
            case SPIKE_HIT -> {
                if (attackTicks == 8) {
                    hitFront(SPIKE_HIT_REACH, SPIKE_HIT_DAMAGE, SPIKE_HIT_KNOCKBACK, SPIKE_HIT_TOSS);
                }
            }
            case TAIL_SMASH -> {
                if (attackTicks == 8) {
                    tailSmash();
                }
            }
            case TAIL_SIDES -> {
                // Хвост проходит сначала одну сторону, потом другую
                if (attackTicks == 5) {
                    tailSweep(true);
                } else if (attackTicks == 12) {
                    tailSweep(false);
                }
            }
        }
        if (attackTicks >= attack.durationTicks) {
            attack = null;
            attackCooldownTicks = ATTACK_COOLDOWN_TICKS;
            planNextAttack();
        }
    }

    // ---------------------------------------------------------------- удары

    private void hitFront(double reach, float damage, double knockback, double toss) {
        Vec3 headFront = pointAlongBody(HEAD_FRONT);
        for (LivingEntity victim : victimsAround()) {
            if (!isBehind(victim) && gapTo(headFront, victim) <= reach && hurtOnce(victim, damage)) {
                push(victim, this.position(), knockback, toss);
            }
        }
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 1.1F);
    }

    private void tailSmash() {
        Vec3 tailTip = pointAlongBody(-TAIL_TIP_BACK);
        for (LivingEntity victim : victimsAround()) {
            if (inTailSmashZone(victim) && hurtOnce(victim, TAIL_SMASH_DAMAGE)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TAIL_SMASH_SLOW_TICKS,
                        TAIL_SMASH_SLOW_LEVEL), this);
                push(victim, tailTip, TAIL_SMASH_KNOCKBACK, 0.2D);
            }
        }
        spawnSlamDust(tailTip);
        this.playSound(SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.2F, 0.8F);
    }

    // firstSide - та сторона, по которой хвост проходит первым; цели прямо сзади достаётся с первым взмахом
    private void tailSweep(boolean firstSide) {
        Vec3 tailBase = pointAlongBody(-TAIL_BASE_BACK);
        for (LivingEntity victim : victimsAround()) {
            float damage = tailSidesDamage(victim);
            if (damage <= 0.0F || (sideOf(victim) >= 0.0D) != firstSide) {
                continue;
            }
            if (hurtOnce(victim, damage)) {
                push(victim, tailBase, damage >= TAIL_SIDES_STRONG_DAMAGE
                        ? TAIL_SIDES_STRONG_KNOCKBACK : TAIL_SIDES_WEAK_KNOCKBACK, 0.15D);
            }
        }
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
    }

    // Каждый удар задевает цель один раз
    private boolean hurtOnce(LivingEntity victim, float damage) {
        if (!hitThisAttack.add(victim.getId())) {
            return false;
        }
        return victim.hurt(this.damageSources().mobAttack(this), damage);
    }

    private void push(LivingEntity victim, Vec3 from, double strength, double toss) {
        victim.knockback(strength, from.x - victim.getX(), from.z - victim.getZ());
        if (toss > 0.0D) {
            victim.setDeltaMovement(victim.getDeltaMovement().add(0.0D, toss, 0.0D));
        }
        victim.hurtMarked = true;
    }

    // Кого могут задеть удары: живые рядом, кроме себя, седока, других пеплоходов, хозяина и его питомцев
    private Iterable<LivingEntity> victimsAround() {
        return this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(TAIL_TIP_BACK + 1.0D),
                this::canHit);
    }

    private boolean canHit(LivingEntity entity) {
        if (entity == this || !entity.isAlive() || entity.getVehicle() == this || entity instanceof CinderRoamerEntity) {
            return false;
        }
        if (entity.getY() > this.getY() + HIT_ABOVE || entity.getY() + entity.getBbHeight() < this.getY() - HIT_BELOW) {
            return false;
        }
        LivingEntity owner = this.getOwner();
        return owner == null || (entity != owner && !entity.isAlliedTo(owner));
    }

    // Пыль того блока, на который упал хвост: облако и кольцо
    private void spawnSlamDust(Vec3 point) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos groundPos = BlockPos.containing(point.x, this.getY() - 0.2D, point.z);
        BlockState ground = level.getBlockState(groundPos);
        if (ground.isAir() || ground.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.DUST_PILLAR, ground);
        double y = groundPos.getY() + 1.0D;
        level.sendParticles(dust, point.x, y, point.z, SLAM_DUST_BURST, 0.4D, 0.0D, 0.4D, 0.15D);
        for (int i = 0; i < SLAM_DUST_RING_POINTS; i++) {
            double angle = Math.PI * 2.0D * i / SLAM_DUST_RING_POINTS;
            level.sendParticles(dust, point.x + Math.cos(angle) * TAIL_SMASH_RADIUS, y,
                    point.z + Math.sin(angle) * TAIL_SMASH_RADIUS, 1, 0.1D, 0.0D, 0.1D, 0.02D);
        }
    }

    // ---------------------------------------------------------------- зоны ударов (по горизонтали)

    // Направление тела (куда смотрит морда)
    private Vec3 forward() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
    }

    // Точка на оси тела: вперёд (+) или назад (-) от центра
    private Vec3 pointAlongBody(double distance) {
        return this.position().add(forward().scale(distance));
    }

    private Vec3 flatOffset(Vec3 from, Entity to) {
        return new Vec3(to.getX() - from.x, 0.0D, to.getZ() - from.z);
    }

    // Расстояние от точки до края коробки цели
    private double gapTo(Vec3 point, LivingEntity target) {
        return flatOffset(point, target).length() - target.getBbWidth() / 2.0D;
    }

    private double headGap(LivingEntity target) {
        return gapTo(pointAlongBody(HEAD_FRONT), target);
    }

    private double horizontalDistance(Vec3 point) {
        double dx = point.x - this.getX();
        double dz = point.z - this.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean isBehind(LivingEntity target) {
        return flatOffset(this.position(), target).dot(forward()) < 0.0D;
    }

    private boolean isFacing(LivingEntity target) {
        Vec3 toTarget = flatOffset(this.position(), target);
        return toTarget.lengthSqr() < 1.0E-4D || toTarget.normalize().dot(forward()) >= FACING_COS;
    }

    // С какой стороны цель (знак - только чтобы развести две половины взмаха)
    private double sideOf(LivingEntity target) {
        Vec3 f = forward();
        Vec3 toTarget = flatOffset(this.position(), target);
        return toTarget.x * f.z - toTarget.z * f.x;
    }

    private boolean inTailSmashZone(LivingEntity target) {
        return gapTo(pointAlongBody(-TAIL_TIP_BACK), target) <= TAIL_SMASH_RADIUS;
    }

    // Урон взмаха хвостом по цели (0 - не достаёт): дуга позади от корня хвоста, дальняя полоса сильнее
    private float tailSidesDamage(LivingEntity target) {
        Vec3 fromTailBase = flatOffset(pointAlongBody(-TAIL_BASE_BACK), target);
        double gap = fromTailBase.length() - target.getBbWidth() / 2.0D;
        if (gap > TAIL_SIDES_OUTER) {
            return 0.0F;
        }
        boolean behind = fromTailBase.lengthSqr() < 1.0E-4D
                || fromTailBase.normalize().dot(forward().scale(-1.0D)) >= TAIL_SIDES_MIN_BACK_COS;
        if (!behind) {
            return 0.0F;
        }
        return gap > TAIL_SIDES_STRONG_FROM ? TAIL_SIDES_STRONG_DAMAGE : TAIL_SIDES_WEAK_DAMAGE;
    }

    // ---------------------------------------------------------------- повороты

    private void turnTowards(Vec3 point, float maxDegrees) {
        turnToYaw(yawTo(point), maxDegrees);
    }

    private void turnAwayFrom(Vec3 point, float maxDegrees) {
        turnToYaw(yawTo(point) + 180.0F, maxDegrees);
    }

    private float yawTo(Vec3 point) {
        return (float) (Mth.atan2(point.z - this.getZ(), point.x - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
    }

    // Тело и голова вместе, не быстрее maxDegrees за тик
    private void turnToYaw(float wantedYaw, float maxDegrees) {
        float yaw = Mth.approachDegrees(this.getYRot(), wantedYaw, maxDegrees);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private final class CombatGoal extends Goal {
        CombatGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return canFight();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = CinderRoamerEntity.this.getTarget();
            if (target != null) {
                combatTick(target);
            }
        }

        @Override
        public void stop() {
            attack = null;
            setRunning(false);
            CinderRoamerEntity.this.getNavigation().stop();
        }
    }

    // ---------------------------------------------------------------- сон (сервер)

    private boolean wantsToSleep() {
        if (!this.level().isNight() || this.getTarget() != null || this.isVehicle() || this.isLeashed()
                || this.isInWaterOrBubble() || !this.onGround()) {
            return false;
        }
        if (!this.isTame()) {
            return true;
        }
        LivingEntity owner = this.getOwner();
        return owner != null && this.distanceTo(owner) <= SLEEP_OWNER_DISTANCE;
    }

    private final class SleepGoal extends Goal {
        SleepGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return !isOrderedToSit() && wantsToSleep() && random.nextInt(SLEEP_CHANCE) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return isSleepingNow() && wantsToSleep();
        }

        @Override
        public void start() {
            CinderRoamerEntity.this.getNavigation().stop();
            setSleepingNow(true);
        }

        @Override
        public void stop() {
            setSleepingNow(false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && !this.level().isClientSide()) {
            // Разбудили - встаёт и защищается; лежавший по команде после боя ляжет обратно
            setSleepingNow(false);
            if (this.isOrderedToSit()) {
                this.setOrderedToSit(false);
                liesDownAfterFight = true;
            }
            if (source.getEntity() instanceof LivingEntity attacker && this.getTarget() == null && this.canAttack(attacker)) {
                this.setTarget(attacker);
            }
        }
        return damaged;
    }

    // Бой кончился - снова лечь, если лежал по команде
    private void tickLyingRecovery() {
        if (liesDownAfterFight && this.getTarget() == null && attack == null && !this.isVehicle()) {
            liesDownAfterFight = false;
            this.setOrderedToSit(true);
        }
        // Лёжа очень медленно заживают раны
        if (this.isLying() && this.getHealth() < this.getMaxHealth() && ++lyingHealTicks >= LYING_HEAL_INTERVAL_TICKS) {
            lyingHealTicks = 0;
            this.heal(LYING_HEAL_AMOUNT);
        }
    }

    // ---------------------------------------------------------------- приручение, ножовка, седло, езда

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean client = this.level().isClientSide();
        if (stack.is(ModItems.MOLYBDENUM_HACKSAW.get())) {
            return sawSpike(player, hand, stack);
        }
        if (stack.is(Items.SADDLE)) {
            return putOnSaddle(player, stack);
        }
        if (stack.is(Items.CHEST)) {
            return hangBackpack(player, stack);
        }
        if (stack.is(Items.SHEARS) && player.isSecondaryUseActive() && getBackpacks() > 0) {
            return cutOffBackpack(player, hand, stack);
        }
        if (stack.is(ItemTags.MEAT)) {
            return this.isTame() ? healWithMeat(player, stack) : tryToTame(player, stack);
        }
        if (!stack.isEmpty() || !this.isTame()) {
            return super.mobInteract(player, hand);
        }
        // Второй игрок садится на заднее седло, если впереди уже кто-то едет
        if (!isOwner(player)) {
            if (this.isVehicle() && canAddPassenger(player)) {
                if (!client) {
                    player.startRiding(this);
                }
                return InteractionResult.sidedSuccess(client);
            }
            return super.mobInteract(player, hand);
        }
        if (player.isSecondaryUseActive()) {
            if (!client) {
                openCustomInventoryScreen(player);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (!client) {
            if (this.isSaddled()) {
                mount(player);
            } else {
                // Без седла: лечь / встать
                this.setOrderedToSit(!this.isOrderedToSit());
                this.getNavigation().stop();
                this.setTarget(null);
            }
        }
        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult sawSpike(Player player, InteractionHand hand, ItemStack saw) {
        boolean client = this.level().isClientSide();
        if (!this.isTame() || !isOwner(player)) {
            tellPlayer(player, this.isTame() ? "not_owner" : "tame_first");
            return InteractionResult.sidedSuccess(client);
        }
        if (!hasBackSpikes()) {
            tellPlayer(player, "no_spikes");
            return InteractionResult.sidedSuccess(client);
        }
        if (!client) {
            int spike = getSawnSpikes();
            setSawnSpikes(spike + 1);
            setSleepingNow(false);
            saw.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            spawnSawDust(spike);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GRINDSTONE_USE,
                    SoundSource.NEUTRAL, 1.0F, 0.8F + this.random.nextFloat() * 0.3F);
        }
        return InteractionResult.sidedSuccess(client);
    }

    // Костяная крошка у шипа, который только что спилили
    private void spawnSawDust(int spike) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = pointAlongBody(SPIKE_FORWARD[spike]).add(0.0D, SPIKE_HEIGHT, 0.0D);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()),
                at.x, at.y, at.z, SAW_PARTICLES, 0.15D, 0.15D, 0.15D, 0.05D);
    }

    // Следующее седло: сначала переднее, потом заднее
    private InteractionResult putOnSaddle(Player player, ItemStack saddle) {
        boolean client = this.level().isClientSide();
        if (!this.isTame() || !isOwner(player)) {
            tellPlayer(player, this.isTame() ? "not_owner" : "tame_first");
            return InteractionResult.sidedSuccess(client);
        }
        if (getSaddles() >= SADDLE_SLOTS) {
            return InteractionResult.PASS;
        }
        if (hasBackSpikes()) {
            tellPlayer(player, "spikes_in_way");
            return InteractionResult.sidedSuccess(client);
        }
        if (!client) {
            this.inventory.setItem(getSaddles(), saddle.copyWithCount(1));
            saddle.consume(1, player);
            playEquipSound();
        }
        return InteractionResult.sidedSuccess(client);
    }

    /** Звук седла - и когда надевают ПКМ, и когда кладут в слот окна. */
    public void playEquipSound() {
        this.level().playSound(null, this, SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1.0F, 0.8F);
    }

    // Рюкзак (сундук) - только когда оба седла на месте; вешаются по одному: backpack1, 2, 3
    private InteractionResult hangBackpack(Player player, ItemStack chest) {
        boolean client = this.level().isClientSide();
        if (!this.isTame() || !isOwner(player)) {
            tellPlayer(player, this.isTame() ? "not_owner" : "tame_first");
            return InteractionResult.sidedSuccess(client);
        }
        if (getSaddles() < SADDLE_SLOTS) {
            tellPlayer(player, "need_both_saddles");
            return InteractionResult.sidedSuccess(client);
        }
        if (getBackpacks() >= MAX_BACKPACKS) {
            tellPlayer(player, "no_room_for_backpack");
            return InteractionResult.sidedSuccess(client);
        }
        if (!client) {
            setBackpacks(getBackpacks() + 1);
            chest.consume(1, player);
            this.level().playSound(null, this, SoundEvents.DONKEY_CHEST, SoundSource.NEUTRAL, 1.0F, 0.8F);
        }
        return InteractionResult.sidedSuccess(client);
    }

    // Shift+ПКМ ножницами: срезать последний рюкзак - выпадают сундук и всё, что в нём было
    private InteractionResult cutOffBackpack(Player player, InteractionHand hand, ItemStack shears) {
        boolean client = this.level().isClientSide();
        if (!isOwner(player)) {
            tellPlayer(player, "not_owner");
            return InteractionResult.sidedSuccess(client);
        }
        if (!client) {
            int backpack = getBackpacks() - 1;
            for (int index = 0; index < BACKPACK_SLOTS; index++) {
                ItemStack content = this.inventory.removeItemNoUpdate(backpackSlot(backpack, index));
                if (!content.isEmpty()) {
                    this.spawnAtLocation(content);
                }
            }
            this.inventory.setChanged();
            setBackpacks(backpack);
            this.spawnAtLocation(new ItemStack(Items.CHEST));
            shears.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            this.level().playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 0.8F);
        }
        return InteractionResult.sidedSuccess(client);
    }

    /** Кнопка «Лечь/Встать» в окне: только хозяин и только пока никто не едет. */
    public void toggleLyingByOwner(Player player) {
        if (!this.isTame() || !isOwner(player) || this.isVehicle()) {
            return;
        }
        liesDownAfterFight = false;
        this.setOrderedToSit(!this.isOrderedToSit());
        this.getNavigation().stop();
        this.setTarget(null);
    }

    // Окно с сёдлами и рюкзаками: хозяину (Shift+ПКМ) и любому седоку (E верхом, как у лошади)
    @Override
    public void openCustomInventoryScreen(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !this.isTame()
                || !(isOwner(player) || this.hasPassenger(player))) {
            return;
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, opener) -> new CinderRoamerMenu(containerId, playerInventory, this),
                        this.getDisplayName()),
                buf -> buf.writeVarInt(this.getId()));
    }

    // Приручать можно и разозлённого (он агрессивен, иначе не приручить) - пока кормишь, он бьёт
    private InteractionResult tryToTame(Player player, ItemStack meat) {
        boolean client = this.level().isClientSide();
        if (!client) {
            meat.consume(1, player);
            if (this.random.nextFloat() < TAME_CHANCE) {
                this.tame(player);
                this.getNavigation().stop();
                this.setTarget(null);
                this.setLastHurtByMob(null);
                this.attack = null;
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }
        }
        return InteractionResult.sidedSuccess(client);
    }

    private InteractionResult healWithMeat(Player player, ItemStack meat) {
        if (this.getHealth() >= this.getMaxHealth()) {
            return InteractionResult.PASS;
        }
        boolean client = this.level().isClientSide();
        if (!client) {
            meat.consume(1, player);
            this.heal(MEAT_HEAL);
        }
        return InteractionResult.sidedSuccess(client);
    }

    private void mount(Player player) {
        liesDownAfterFight = false;
        this.setOrderedToSit(false);
        this.setInSittingPose(false);
        setSleepingNow(false);
        this.getNavigation().stop();
        this.setTarget(null);
        player.startRiding(this);
    }

    // Хозяин по UUID: ванильный isOwnedBy ищет игрока в мире и не узнаёт хозяина, которого там нет в списке игроков
    private boolean isOwner(Player player) {
        return player.getUUID().equals(this.getOwnerUUID());
    }

    private void tellPlayer(Player player, String key) {
        if (!this.level().isClientSide()) {
            player.displayClientMessage(Component.translatable("message.veridia.cinder_roamer." + key), true);
        }
    }

    // Управляет тот, кто в переднем седле
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (this.isSaddled() && this.getFirstPassenger() instanceof Player player) {
            return player;
        }
        return super.getControllingPassenger();
    }

    // Сколько сёдел - столько седоков
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < getSaddles();
    }

    // Седок может бежать (Ctrl)
    @Override
    public boolean canSprint() {
        return true;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        this.setRot(player.getYRot(), player.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    // Как у лошади: вбок вполовину, назад медленно
    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float strafe = player.xxa * 0.5F;
        float forward = player.zza;
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }
        return new Vec3(strafe, 0.0D, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        double blocksPerSecond = player.isSprinting() ? RIDDEN_RUN_BLOCKS_PER_SECOND : RIDDEN_WALK_BLOCKS_PER_SECOND;
        double speed = blocksPerSecond / 20.0D / RIDDEN_BLOCKS_PER_TICK_PER_SPEED;
        // Замедление и ускорение (эффекты) действуют и под седоком
        return (float) (speed * this.getAttributeValue(Attributes.MOVEMENT_SPEED) / WALK_SPEED);
    }

    // Первый седок - в переднем седле, второй - в заднем
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        Seat seat = this.getPassengers().indexOf(passenger) <= 0 ? FRONT_SEAT : REAR_SEAT;
        double scale = dimensions.height() / this.getType().getHeight();
        return new Vec3(0.0D, seat.height() * scale, seat.forward() * scale).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
    }

    // Умер - выпадает всё из рюкзаков, сёдла и сами рюкзаки (сундуки)
    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            ItemStack stack = this.inventory.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack);
            }
        }
        if (getBackpacks() > 0) {
            this.spawnAtLocation(new ItemStack(Items.CHEST, getBackpacks()));
            setBackpacks(0);
        }
    }

    // ---------------------------------------------------------------- сохранение

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(SAWN_SPIKES_KEY, getSawnSpikes());
        tag.putInt(BACKPACKS_KEY, getBackpacks());
        ContainerHelper.saveAllItems(tag, this.inventory.getItems(), this.registryAccess());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSawnSpikes(tag.getInt(SAWN_SPIKES_KEY));
        setBackpacks(tag.getInt(BACKPACKS_KEY));
        ContainerHelper.loadAllItems(tag, this.inventory.getItems(), this.registryAccess());
        if (tag.getBoolean(LEGACY_SADDLED_KEY) && this.inventory.getItem(0).isEmpty()) {
            this.inventory.setItem(0, new ItemStack(Items.SADDLE));
        }
        this.entityData.set(DATA_SADDLES, countSaddles());
    }

    // ---------------------------------------------------------------- анимации (клиент)

    @Override
    public void handleEntityEvent(byte id) {
        for (Attack kind : Attack.values()) {
            if (kind.event == id) {
                startAttackAnimation(kind);
                return;
            }
        }
        super.handleEntityEvent(id);
    }

    private AnimationState animationOf(Attack kind) {
        return switch (kind) {
            case BITE -> biteAnimationState;
            case SPIKE_HIT -> spikeHitAnimationState;
            case TAIL_SMASH -> tailSmashAnimationState;
            case TAIL_SIDES -> tailSidesAnimationState;
        };
    }

    private void startAttackAnimation(Attack kind) {
        for (Attack other : Attack.values()) {
            animationOf(other).stop();
        }
        animationOf(kind).start(this.tickCount);
        this.clientAttackTicksLeft = kind.durationTicks;
    }

    /** Время анимации walk в тиках (с долей тика): шаг - родная скорость клипа, бег - быстрее, переход плавный. */
    public float getWalkAnimationTicks(float partialTick) {
        return Mth.lerp(partialTick, walkAnimationTicksO, walkAnimationTicks);
    }

    private void clientTick() {
        float targetRate = isRunning() ? RUN_ANIMATION_RATE : WALK_ANIMATION_RATE;
        walkAnimationRate = Mth.approach(walkAnimationRate, targetRate, ANIMATION_RATE_CHANGE_PER_TICK);
        // Клип walk длится секунду (20 тиков): сдвигаем время на целое число циклов, чтобы не терялась точность
        if (walkAnimationTicks > WALK_ANIMATION_WRAP_TICKS) {
            walkAnimationTicks -= WALK_ANIMATION_WRAP_TICKS;
        }
        walkAnimationTicksO = walkAnimationTicks;
        walkAnimationTicks += walkAnimationRate;

        boolean lying = isLying();
        if (lying != clientWasLying) {
            clientWasLying = lying;
            if (lying) {
                sleepEndAnimationState.stop();
                sleepStartAnimationState.start(this.tickCount);
                clientLieDownTicksLeft = LIE_DOWN_TICKS;
            } else {
                sleepStartAnimationState.stop();
                sleepingAnimationState.stop();
                sleepEndAnimationState.start(this.tickCount);
                clientStandUpTicksLeft = STAND_UP_TICKS;
            }
        }
        if (clientLieDownTicksLeft > 0 && --clientLieDownTicksLeft == 0 && lying) {
            sleepStartAnimationState.stop();
            sleepingAnimationState.start(this.tickCount);
        }
        clientStandUpTicksLeft = tickDown(clientStandUpTicksLeft, sleepEndAnimationState);
        clientAttackTicksLeft = tickDown(clientAttackTicksLeft,
                biteAnimationState, spikeHitAnimationState, tailSmashAnimationState, tailSidesAnimationState);
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

    // ---------------------------------------------------------------- звуки

    @Override
    protected SoundEvent getAmbientSound() {
        return isLying() ? null : SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.RAVAGER_STEP, 0.4F, 1.1F);
    }

    @Override
    protected float getSoundVolume() {
        return 0.7F;
    }

    @Override
    public float getVoicePitch() {
        return 1.25F + (this.random.nextFloat() - 0.5F) * 0.1F;
    }
}
