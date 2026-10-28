package net.gera.veridia.common.block.entity.types;

import net.gera.veridia.common.block.SpiritualRunestoneBlock;
import net.gera.veridia.common.block.entity.ModBlockEntities;
import net.gera.veridia.common.entity.ominous.OminousWarriorEntity;
import net.gera.veridia.common.entity.ominous.rage.OminousRage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Блок-сущность нижней половины духовного рунного камня.
 * <ul>
 *   <li>Пока копятся молитвы (они в состоянии блока): на клиенте кольцо фиолетовых частиц у подножия, с каждой
 *       молитвой шире, плотнее и крупнее; с {@link #ENCHANT_FROM_PRAYERS} молитв в камень втягиваются частицы
 *       зачарования. Ванильный animateTick вызывается для блока слишком редко, ровное кольцо из него не собрать.</li>
 *   <li>Камень, с которого началась ярость, активен до её конца ({@link #activeUntil} по часам мира, клиенту приходит
 *       вместе с блоком). Всё это время он заряжен до предела, а воинов в ярости в радиусе {@link #EMPOWER_RADIUS}
 *       усиливает ({@link OminousWarriorEntity#empower}). Ярость кончилась - камень гаснет, молитвы обнуляются, и
 *       {@link #REST_TICKS} он отдыхает: молитв не принимает.</li>
 *   <li>Свечение рун, ореол и лучи к воинам рисует SpiritualRunestoneRenderer.</li>
 * </ul>
 */
public class SpiritualRunestoneBlockEntity extends BlockEntity {
    // Кольцо: при одной молитве и при последней перед яростью; между ними - плавно
    private static final double RING_MIN_RADIUS = 0.9D;
    private static final double RING_MAX_RADIUS = 2.2D;
    private static final int RING_MIN_POINTS = 6;
    private static final int RING_MAX_POINTS = 28;
    private static final float RING_MIN_SIZE = 0.7F;
    private static final float RING_MAX_SIZE = 1.8F;
    private static final double RING_SPIN_RADIANS_PER_TICK = 0.04D;
    private static final int RING_INTERVAL_TICKS = 2;
    private static final double RING_HEIGHT = 0.15D;
    private static final double RING_RISE_SPEED = 0.01D;
    private static final Vector3f RING_COLOR = new Vector3f(0.62F, 0.18F, 0.95F);

    // Частицы зачарования: откуда летят (разброс вокруг камня) и сколько в среднем за тик при последней молитве
    public static final int ENCHANT_FROM_PRAYERS = 10;
    private static final float ENCHANT_MAX_PER_TICK = 3.0F;
    private static final double ENCHANT_SPREAD = 3.0D;
    private static final double ENCHANT_TARGET_HEIGHT = 1.4D;

    // Усиление воинов активным камнем: радиус от центра камня, как часто камень его продлевает и на сколько тиков.
    // Вышел из радиуса - усиление спадает через EMPOWER_DURATION_TICKS
    public static final double EMPOWER_RADIUS = 16.0D;
    private static final int EMPOWER_INTERVAL_TICKS = 10;
    private static final int EMPOWER_DURATION_TICKS = 25;

    // После своей ярости камень отдыхает 7 игровых дней: молитв не принимает, воины к нему не ходят.
    // Кулдаун у самого камня: у новых воинов (ночной спавн, яйца) своего кулдауна ещё нет
    private static final long REST_TICKS = 7L * 24000L;

    private static final String ACTIVE_UNTIL_KEY = "ActiveUntil";
    private static final String REST_UNTIL_KEY = "RestUntil";
    private static final long NOT_ACTIVE = 0L;

    private long activeUntil = NOT_ACTIVE;
    private long restUntil;

    public SpiritualRunestoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPIRITUAL_RUNESTONE_BE.get(), pos, state);
    }

    // ---------------------------------------------------------------- состояние

    /** Идёт ли ярость, начавшаяся с этого камня. */
    public boolean isActive() {
        return level != null && level.getGameTime() < activeUntil;
    }

    /** Отдыхает ли камень после своей ярости (молитв не принимает). */
    public boolean isResting() {
        return level != null && level.getGameTime() < restUntil;
    }

    /** Заряд камня от 0 до 1: доля молитв до ярости. Активный камень заряжен полностью. */
    public float charge() {
        return isActive() ? 1.0F : prayers() / (float) (SpiritualRunestoneBlock.PRAYERS_FOR_RAGE - 1);
    }

    private int prayers() {
        return isActive() ? SpiritualRunestoneBlock.PRAYERS_FOR_RAGE - 1
                : getBlockState().getValue(SpiritualRunestoneBlock.PRAYERS);
    }

    /** Центр камня (середина двух блоков в высоту): отсюда меряется радиус усиления. */
    public Vec3 center() {
        return Vec3.atBottomCenterOf(worldPosition).add(0.0D, 1.0D, 0.0D);
    }

    /** Живые воины в ярости в радиусе действия камня. */
    public List<OminousWarriorEntity> ragingWarriorsInRange() {
        if (level == null) {
            return List.of();
        }
        Vec3 center = center();
        double radiusSqr = EMPOWER_RADIUS * EMPOWER_RADIUS;
        return level.getEntitiesOfClass(OminousWarriorEntity.class, new AABB(center, center).inflate(EMPOWER_RADIUS),
                warrior -> warrior.isAlive() && warrior.isRaging() && warrior.distanceToSqr(center) <= radiusSqr);
    }

    /** С этого камня началась ярость: он активен до её конца. */
    public void activate(long untilGameTime) {
        activeUntil = untilGameTime;
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---------------------------------------------------------------- сервер

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpiritualRunestoneBlockEntity stone) {
        if (stone.activeUntil == NOT_ACTIVE || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // Ярость кончилась по времени, остановлена командой или кончилась, пока чанк был выгружен
        if (!stone.isActive() || !OminousRage.isActive(serverLevel)) {
            stone.calmDown(serverLevel);
            return;
        }
        if (level.getGameTime() % EMPOWER_INTERVAL_TICKS == 0) {
            for (OminousWarriorEntity warrior : stone.ragingWarriorsInRange()) {
                warrior.empower(EMPOWER_DURATION_TICKS);
            }
        }
    }

    // Ярость кончилась: камень гаснет и отдыхает, потом счёт молитв заново
    private void calmDown(ServerLevel level) {
        activeUntil = NOT_ACTIVE;
        restUntil = level.getGameTime() + REST_TICKS;
        setChangedAndSync();
        SpiritualRunestoneBlock.calmDown(level, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (activeUntil != NOT_ACTIVE) {
            tag.putLong(ACTIVE_UNTIL_KEY, activeUntil);
        }
        if (restUntil != 0L) {
            tag.putLong(REST_UNTIL_KEY, restUntil);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        activeUntil = tag.getLong(ACTIVE_UNTIL_KEY);
        restUntil = tag.getLong(REST_UNTIL_KEY);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---------------------------------------------------------------- клиент

    public static void clientTick(Level level, BlockPos pos, BlockState state, SpiritualRunestoneBlockEntity stone) {
        int prayers = stone.prayers();
        if (prayers <= 0) {
            return;
        }
        float strength = prayers / (float) (SpiritualRunestoneBlock.PRAYERS_FOR_RAGE - 1);
        if (level.getGameTime() % RING_INTERVAL_TICKS == 0) {
            spawnRing(level, pos, strength);
        }
        if (prayers >= ENCHANT_FROM_PRAYERS) {
            spawnEnchant(level, pos, prayers, level.getRandom());
        }
    }

    private static void spawnRing(Level level, BlockPos pos, float strength) {
        double radius = Mth.lerp(strength, RING_MIN_RADIUS, RING_MAX_RADIUS);
        int points = Math.round(Mth.lerp(strength, RING_MIN_POINTS, RING_MAX_POINTS));
        DustParticleOptions dust = new DustParticleOptions(RING_COLOR, Mth.lerp(strength, RING_MIN_SIZE, RING_MAX_SIZE));
        double spin = level.getGameTime() * RING_SPIN_RADIANS_PER_TICK;
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        for (int i = 0; i < points; i++) {
            double angle = spin + Math.PI * 2.0D * i / points;
            level.addParticle(dust, centerX + Math.cos(angle) * radius, pos.getY() + RING_HEIGHT,
                    centerZ + Math.sin(angle) * radius, 0.0D, RING_RISE_SPEED, 0.0D);
        }
    }

    // Частица зачарования летит из точки (цель + смещение) в цель, как от книжных полок к столу зачарования
    private static void spawnEnchant(Level level, BlockPos pos, int prayers, RandomSource random) {
        float progress = (prayers - ENCHANT_FROM_PRAYERS + 1)
                / (float) (SpiritualRunestoneBlock.PRAYERS_FOR_RAGE - ENCHANT_FROM_PRAYERS);
        float perTick = ENCHANT_MAX_PER_TICK * progress;
        int count = (int) perTick + (random.nextFloat() < perTick - (int) perTick ? 1 : 0);
        for (int i = 0; i < count; i++) {
            level.addParticle(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5D, pos.getY() + ENCHANT_TARGET_HEIGHT, pos.getZ() + 0.5D,
                    (random.nextDouble() * 2.0D - 1.0D) * ENCHANT_SPREAD,
                    random.nextDouble() * ENCHANT_SPREAD - 1.0D,
                    (random.nextDouble() * 2.0D - 1.0D) * ENCHANT_SPREAD);
        }
    }
}
