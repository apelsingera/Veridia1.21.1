package net.gera.veridia.common.entity.ominous;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Высокий зловещий воин (бывший обычный): всё как у {@link OminousWarriorEntity} - бой, щит, молитва, ярость,
 * усиление камнем, - только ростом 2.2 блока, сильнее (больше здоровья и урона) и умеет пригибаться.
 * <p>
 * Приседание. Стоя он не пролезает в проход высотой 2 блока, пригнувшись (поза CROUCHING, рост
 * {@link #DUCKING_HEIGHT}) - пролезает. Поиск пути поэтому считает такие проходы проходимыми, но дорогими
 * ({@link DuckingMob}): если есть обход не слишком длиннее, воин идёт в обход. И то лишь по делу (бой, молитва):
 * без дела низкие места для него закрыты, и на прогулке он не выбирает мест, где стоя не поместиться.
 * Пригибается, когда стоя упёрся бы в потолок, а пригнувшись - нет: там, где стоит, на ближайших клетках пути или,
 * если идёт без пути, чуть впереди (LOOK_AHEAD_DISTANCES; если там ступенька - на ней). Выпрямляется, когда
 * низких мест вокруг нет уже STAND_UP_DELAY_TICKS тиков и стоя он помещается. Клиенту поза приходит сама, анимации:
 * duckdown, ducking, duckdownend (см. {@link BrutalOminousWarrior}).
 */
public class BrutalOminousWarriorEntity extends OminousWarriorEntity implements DuckingMob {
    public final AnimationState duckDownAnimationState = new AnimationState();
    public final AnimationState duckUpAnimationState = new AnimationState();

    // Сильнее обычного (у того 60 здоровья, урон 3 без оружия, молот добавляет +6): здоровье, урон без оружия, опыт
    private static final double MAX_HEALTH = 100.0D;
    private static final double ATTACK_DAMAGE = 6.0D;
    private static final int EXPERIENCE_REWARD = 35;

    // Рост и уровень глаз пригнувшись: модель в позе ducking ~1.95 блока, коробка чуть ниже, чтобы влезать в 2 блока
    private static final float DUCKING_HEIGHT = 1.9F;
    private static final float DUCKING_EYE_HEIGHT = 1.6F;
    // Поиск пути: клетка, где пришлось бы пригибаться, стоит как 1 + столько обычных шагов. Когда важно (есть цель или
    // идёт молиться) - умеренно: за целью в дверь полезет, если обход длиннее примерно на 8 блоков на каждую низкую
    // клетку. Уже стоит пригнувшись в низком месте - дорого, но можно: выбирается наружу. Иначе низкие места для
    // него закрыты. Обход ищется в пределах дальности преследования (около 32 блоков пути)
    private static final float IMPORTANT_DUCKING_PATH_MALUS = 8.0F;
    private static final float ESCAPE_DUCKING_PATH_MALUS = 30.0F;
    private static final float NO_DUCKING_PATH_MALUS = -1.0F;
    // Пригибается заранее, если низкая одна из стольких ближайших клеток пути
    private static final int PATH_LOOK_AHEAD_NODES = 2;
    // Прогулка: сколько раз выбрать другую точку, если выпала такая, где стоя не поместиться
    private static final int STROLL_POSITION_TRIES = 5;

    // Куда смотреть вперёд по ходу (блоков). Встанет он там на том же уровне или, если мешает ступенька, на полблока
    // или блок выше: берётся первый уровень, где он помещается хотя бы пригнувшись
    private static final double[] LOOK_AHEAD_DISTANCES = {0.5D, 1.0D};
    private static final double[] STEP_RISES = {0.0D, 0.5D, 1.0D};
    // Сколько тиков без низких мест рядом, прежде чем выпрямиться (чтобы не дёргался между двумя низкими местами)
    private static final int STAND_UP_DELAY_TICKS = 6;
    // Коробки чуть сжимаются, чтобы касание потолка вплотную не считалось столкновением
    private static final double COLLISION_EPSILON = 1.0E-4D;

    // Длина duckdown и duckdownend (0.83 с) в тиках
    private static final int DUCK_DOWN_TICKS = 17;
    private static final int DUCK_UP_TICKS = 17;

    private static final String DUCKING_KEY = "Ducking";

    // Сервер
    private int standUpDelay;

    // Клиент
    private boolean clientWasDucking;
    private int clientDuckDownTicksLeft;
    private int clientDuckUpTicksLeft;

    public BrutalOminousWarriorEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = EXPERIENCE_REWARD;
    }

    // Всё как у обычного, только здоровье и урон свои (повторный add заменяет значение)
    public static AttributeSupplier.Builder createAttributes() {
        return OminousWarriorEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);
    }

    /** Пригнулся ли (клиенту: анимации приседания). */
    public boolean isDucking() {
        return this.getPose() == Pose.CROUCHING;
    }

    @Override
    public float duckingHeight() {
        return DUCKING_HEIGHT;
    }

    // Пригибаться по пути можно, только когда это важно: бой или молитва. Без дела - нет, разве что уже залез в низкое
    // место и выбирается из него
    @Override
    public float duckingPathMalus() {
        if (this.isAggressive() || this.isGoingToPray()) {
            return IMPORTANT_DUCKING_PATH_MALUS;
        }
        return isDucking() ? ESCAPE_DUCKING_PATH_MALUS : NO_DUCKING_PATH_MALUS;
    }

    @Override
    protected Goal createStrollGoal() {
        return new StandingRoomStrollGoal(this);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions standing = super.getDefaultDimensions(pose);
        if (pose != Pose.CROUCHING) {
            return standing;
        }
        return EntityDimensions.scalable(standing.width(), DUCKING_HEIGHT).withEyeHeight(DUCKING_EYE_HEIGHT);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            clientDuckTick();
        } else if (this.isAlive()) {
            updateDucking();
        }
    }

    // ---------------------------------------------------------------- приседание (сервер)

    private void updateDucking() {
        if (isLowSpot(this.position()) || isLowAhead()) {
            standUpDelay = STAND_UP_DELAY_TICKS;
            if (!isDucking()) {
                setDucking(true);
            }
            return;
        }
        if (!isDucking()) {
            return;
        }
        if (standUpDelay > 0) {
            standUpDelay--;
        } else if (fits(Pose.STANDING, this.position())) {
            setDucking(false);
        }
    }

    private void setDucking(boolean ducking) {
        Pose pose = this.getPose();
        if (pose == Pose.STANDING || pose == Pose.CROUCHING) {
            this.setPose(ducking ? Pose.CROUCHING : Pose.STANDING);
        }
    }

    // Низкое место впереди. Идёт по пути - смотрим на ближайшие клетки пути: путь, который кончается у низкого места,
    // пригибаться не заставит. Идёт к точке без пути (погоня в ярости напрямик) - вперёд по направлению взгляда
    private boolean isLowAhead() {
        Path path = this.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            return isLowOnPath(path);
        }
        return this.getMoveControl().hasWanted() && isLowInFront();
    }

    private boolean isLowOnPath(Path path) {
        int last = Math.min(path.getNodeCount(), path.getNextNodeIndex() + PATH_LOOK_AHEAD_NODES);
        for (int i = path.getNextNodeIndex(); i < last; i++) {
            Vec3 foothold = footholdAhead(Vec3.atBottomCenterOf(path.getNodePos(i)));
            if (foothold != null && isLowSpot(foothold)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLowInFront() {
        Vec3 heading = Vec3.directionFromRotation(0.0F, this.getYRot());
        for (double distance : LOOK_AHEAD_DISTANCES) {
            Vec3 foothold = footholdAhead(this.position().add(heading.scale(distance)));
            if (foothold != null && isLowSpot(foothold)) {
                return true;
            }
        }
        return false;
    }

    // Где он встанет в этой точке: на том же уровне, а если там мешает ступенька - на ней. Раньше проверялся всегда и
    // уровень на блок выше, и под потолком в 3 блока он пригибался на ровном полу. Нигде не помещается - стена, null
    @Nullable
    private Vec3 footholdAhead(Vec3 ahead) {
        for (double rise : STEP_RISES) {
            Vec3 feet = ahead.add(0.0D, rise, 0.0D);
            if (fits(Pose.CROUCHING, feet)) {
                return feet;
            }
        }
        return null;
    }

    // Стоя здесь упёрся бы в потолок, а пригнувшись помещается (стена или ступенька мешают обоим - это не низкое место)
    private boolean isLowSpot(Vec3 feet) {
        return !fits(Pose.STANDING, feet) && fits(Pose.CROUCHING, feet);
    }

    private boolean fits(Pose pose, Vec3 feet) {
        AABB box = this.getDimensions(pose).makeBoundingBox(feet).deflate(COLLISION_EPSILON);
        return this.level().noCollision(this, box);
    }

    // Поза в сохранение не попадает: без этого загруженный в проходе воин первый тик стоял бы головой в потолке
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (isDucking()) {
            tag.putBoolean(DUCKING_KEY, true);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean(DUCKING_KEY)) {
            setDucking(true);
        }
    }

    // ---------------------------------------------------------------- анимации (клиент)

    // Пригнулся - duckdown, после него поза ducking (её держит модель); выпрямился - duckdownend
    private void clientDuckTick() {
        boolean ducking = isDucking();
        if (ducking != clientWasDucking) {
            clientWasDucking = ducking;
            if (ducking) {
                duckUpAnimationState.stop();
                clientDuckUpTicksLeft = 0;
                duckDownAnimationState.start(this.tickCount);
                clientDuckDownTicksLeft = DUCK_DOWN_TICKS;
            } else {
                duckDownAnimationState.stop();
                clientDuckDownTicksLeft = 0;
                duckUpAnimationState.start(this.tickCount);
                clientDuckUpTicksLeft = DUCK_UP_TICKS;
            }
        }
        clientDuckDownTicksLeft = tickDown(clientDuckDownTicksLeft, duckDownAnimationState);
        clientDuckUpTicksLeft = tickDown(clientDuckUpTicksLeft, duckUpAnimationState);
    }

    private static int tickDown(int ticksLeft, AnimationState state) {
        if (ticksLeft <= 0) {
            return 0;
        }
        if (ticksLeft == 1) {
            state.stop();
        }
        return ticksLeft - 1;
    }

    // ---------------------------------------------------------------- прогулка

    // Обычная прогулка, только точка выбирается там, где он помещается стоя: под низкими крышами не гуляет
    private static class StandingRoomStrollGoal extends WaterAvoidingRandomStrollGoal {
        private final BrutalOminousWarriorEntity brutal;

        StandingRoomStrollGoal(BrutalOminousWarriorEntity brutal) {
            super(brutal, 1.0D);
            this.brutal = brutal;
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            for (int i = 0; i < STROLL_POSITION_TRIES; i++) {
                Vec3 position = super.getPosition();
                if (position == null || brutal.fits(Pose.STANDING, position)) {
                    return position;
                }
            }
            return null;
        }
    }
}
