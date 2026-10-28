package net.gera.veridia.common.block;

import com.mojang.serialization.MapCodec;
import net.gera.veridia.common.block.entity.ModBlockEntities;
import net.gera.veridia.common.block.entity.types.SpiritualRunestoneBlockEntity;
import net.gera.veridia.common.entity.ominous.rage.OminousRage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Духовный рунный камень: та же форма, что у кровавого (два блока в высоту, 8 поворотов), но спавнеры он не усиливает.
 * Зловещие воины приходят к нему молиться. Число молитв хранится в самом блоке ({@link #PRAYERS}, у обеих половин):
 * чем их больше, тем ярче камень светится и тем гуще кольцо частиц вокруг (частицы рисует
 * {@link SpiritualRunestoneBlockEntity}). На {@link #PRAYERS_FOR_RAGE}-й молитве начинается ярость воинов
 * ({@link OminousRage}): камень остаётся заряженным и активным, пока она идёт, а в конце гаснет ({@link #calmDown})
 * и 7 игровых дней отдыхает - молитв не принимает. Сломали активный камень - ярость кончается сразу.
 * <p>
 * Очень прочный. Пока с него идёт ярость, ломается в три приёма ({@link #CRACKS}): первые два раза только трескается
 * и светится всё ярче, на третий разлетается со взрывом (блоки не ломает), дымом и фиолетовыми частицами, выпадает
 * мрачное ядро. Без ярости ломается сразу и как обычно. Кончилась ярость - трещины зарастают.
 */
public class SpiritualRunestoneBlock extends BloodyRunestoneBlock implements EntityBlock {
    public static final MapCodec<SpiritualRunestoneBlock> CODEC = simpleCodec(SpiritualRunestoneBlock::new);

    public static final int PRAYERS_FOR_RAGE = 20;
    public static final IntegerProperty PRAYERS = IntegerProperty.create("prayers", 0, PRAYERS_FOR_RAGE - 1);
    private static final int MAX_LIGHT = 15;

    // Ломается в три приёма: первые два раза только трескается (CRACKS) и светится ярче, на третий - взрыв.
    // В творческом режиме ломается сразу
    public static final int CRACKS_BEFORE_BREAK = 2;
    public static final IntegerProperty CRACKS = IntegerProperty.create("cracks", 0, CRACKS_BEFORE_BREAK);
    private static final int[] CRACK_LIGHT = {0, 11, MAX_LIGHT};
    private static final int CRACK_DUST_COUNT = 30;
    private static final int CRACK_PORTAL_COUNT = 40;

    // Взрыв на третий раз: блоки не ломает, но бьёт и отбрасывает всех рядом; потом дым и фиолетовые частицы
    private static final float SHATTER_EXPLOSION_POWER = 1.5F;
    private static final int SHATTER_SMOKE_COUNT = 90;
    private static final int SHATTER_RISING_SMOKE_COUNT = 30;
    private static final int SHATTER_DUST_COUNT = 140;
    private static final int SHATTER_PORTAL_COUNT = 120;
    private static final DustParticleOptions SHATTER_DUST = new DustParticleOptions(new Vector3f(0.62F, 0.18F, 0.95F), 2.0F);

    // Отклик на молитву: облачко фиолетовой пыли и звон
    private static final DustParticleOptions PRAYER_DUST = new DustParticleOptions(new Vector3f(0.62F, 0.18F, 0.95F), 1.5F);
    private static final int PRAYER_DUST_COUNT = 16;
    private static final int RAGE_BURST_COUNT = 160;

    public SpiritualRunestoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PRAYERS, CRACKS);
    }

    /** Свет от камня: растёт с числом молитв (к последней перед яростью - полный) и с каждой трещиной. */
    public static int lightLevel(BlockState state) {
        int fromPrayers = state.getValue(PRAYERS) * MAX_LIGHT / (PRAYERS_FOR_RAGE - 1);
        return Math.max(fromPrayers, CRACK_LIGHT[state.getValue(CRACKS)]);
    }

    // ---------------------------------------------------------------- ломание в три приёма

    // Ломание в три приёма - только у камня, с которого сейчас идёт ярость (он же светится и тянет лучи к воинам).
    // Это знают и сервер, и клиент (время конца ярости приходит с блок-сущностью), поэтому решают одинаково
    private static boolean isRageStone(Level level, BlockPos lowerPos) {
        return level.getBlockEntity(lowerPos) instanceof SpiritualRunestoneBlockEntity stone && stone.isActive();
    }

    // Ярость идёт с этого камня, ломают в выживании, а он ещё не треснул дважды - это только трещина
    private static boolean onlyCracks(Level level, BlockPos pos, BlockState state, Player player) {
        return !player.isCreative() && state.getValue(CRACKS) < CRACKS_BEFORE_BREAK
                && isRageStone(level, lowerPos(pos, state));
    }

    private static BlockPos lowerPos(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    // Пока это трещина, вторую половину не трогаем (кровавый камень убирает её, когда ломают верхнюю)
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (onlyCracks(level, pos, state, player)) {
            return state;
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
                                       FluidState fluid) {
        if (onlyCracks(level, pos, state, player)) {
            // Блок остаётся; новую трещину ставит сервер и сам сообщает клиентам
            if (level instanceof ServerLevel serverLevel) {
                crack(serverLevel, lowerPos(pos, state), state.getValue(CRACKS) + 1);
            }
            return false;
        }
        BlockPos lower = lowerPos(pos, state);
        // Узнаём до удаления: потом блок-сущности уже нет
        boolean rageStone = isRageStone(level, lower);
        boolean removed = super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        if (removed && rageStone && !player.isCreative() && level instanceof ServerLevel serverLevel) {
            shatter(serverLevel, lower, willHarvest);
        }
        return removed;
    }

    private static void crack(ServerLevel level, BlockPos lowerPos, int cracks) {
        setOnBothHalves(level, lowerPos, CRACKS, cracks);
        Vec3 center = Vec3.atBottomCenterOf(lowerPos).add(0.0D, 1.0D, 0.0D);
        float progress = cracks / (float) CRACKS_BEFORE_BREAK;
        level.playSound(null, lowerPos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F + progress * 0.2F);
        level.playSound(null, lowerPos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.6F + progress * 0.3F);
        level.sendParticles(SHATTER_DUST, center.x, center.y, center.z, CRACK_DUST_COUNT * cracks, 0.5D, 0.8D, 0.5D, 0.05D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, CRACK_PORTAL_COUNT * cracks,
                0.4D, 0.8D, 0.4D, 0.15D);
    }

    // Третий раз: взрыв без разрушения блоков, клубы дыма и фиолетовые частицы. Мрачное ядро выпадает, если ломали
    // подходящим инструментом (как и остальная добыча камня)
    private static void shatter(ServerLevel level, BlockPos lowerPos, boolean harvested) {
        Vec3 center = Vec3.atBottomCenterOf(lowerPos).add(0.0D, 1.0D, 0.0D);
        if (harvested) {
            Block.popResource(level, lowerPos, new ItemStack(ModItems.DISMAL_CORE.get()));
        }
        level.explode(null, center.x, center.y, center.z, SHATTER_EXPLOSION_POWER, false, Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, SHATTER_SMOKE_COUNT,
                1.0D, 1.0D, 1.0D, 0.08D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y, center.z, SHATTER_RISING_SMOKE_COUNT,
                0.7D, 0.5D, 0.7D, 0.02D);
        level.sendParticles(SHATTER_DUST, center.x, center.y, center.z, SHATTER_DUST_COUNT, 1.2D, 1.2D, 1.2D, 0.1D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, SHATTER_PORTAL_COUNT,
                0.6D, 1.0D, 0.6D, 0.6D);
        level.playSound(null, lowerPos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.BLOCKS, 1.2F, 0.7F);
    }

    /**
     * Можно ли сейчас молиться у камня (lowerPos - нижняя половина): камень на месте, ярость не идёт (во время неё
     * молитвы не копятся) и камень не отдыхает после своей ярости.
     */
    public static boolean acceptsPrayers(ServerLevel level, BlockPos lowerPos) {
        BlockState state = level.getBlockState(lowerPos);
        return state.getBlock() instanceof SpiritualRunestoneBlock && state.getValue(HALF) == DoubleBlockHalf.LOWER
                && !OminousRage.isActive(level)
                && !(level.getBlockEntity(lowerPos) instanceof SpiritualRunestoneBlockEntity stone && stone.isResting());
    }

    /**
     * Воин помолился у камня (lowerPos - нижняя половина). На {@link #PRAYERS_FOR_RAGE}-й молитве начинается ярость,
     * камень становится активным до её конца. Возвращает true, если молитва засчитана ({@link #acceptsPrayers}).
     */
    public static boolean addPrayer(ServerLevel level, BlockPos lowerPos) {
        if (!acceptsPrayers(level, lowerPos)) {
            return false;
        }
        int prayers = level.getBlockState(lowerPos).getValue(PRAYERS) + 1;
        if (prayers >= PRAYERS_FOR_RAGE) {
            // Молитв уже предел (PRAYERS_FOR_RAGE - 1): камень так и остаётся заряженным, пока идёт ярость
            long rageEnd = OminousRage.start(level);
            if (level.getBlockEntity(lowerPos) instanceof SpiritualRunestoneBlockEntity stone) {
                stone.activate(rageEnd);
            }
            playRageBurst(level, lowerPos);
        } else {
            setPrayers(level, lowerPos, prayers);
            playPrayerEcho(level, lowerPos, prayers);
        }
        return true;
    }

    // Сломали камень, с которого началась ярость (любую половину - вторая убирается следом): ярость кончается сразу.
    // Смена числа молитв - тот же блок, это не удаление
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof SpiritualRunestoneBlockEntity stone && stone.isActive()) {
            OminousRage.stop(serverLevel);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Ярость, начавшаяся с этого камня, кончилась: молитвы обнуляются, свет и частицы гаснут. */
    public static void calmDown(ServerLevel level, BlockPos lowerPos) {
        if (!(level.getBlockState(lowerPos).getBlock() instanceof SpiritualRunestoneBlock)) {
            return;
        }
        setPrayers(level, lowerPos, 0);
        // Трещины от ярости зарастают
        setOnBothHalves(level, lowerPos, CRACKS, 0);
        level.playSound(null, lowerPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5F, 0.7F);
    }

    private static void setPrayers(ServerLevel level, BlockPos lowerPos, int prayers) {
        setOnBothHalves(level, lowerPos, PRAYERS, prayers);
    }

    // Молитвы и трещины хранятся в обеих половинах одинаково
    private static void setOnBothHalves(ServerLevel level, BlockPos lowerPos, IntegerProperty property, int value) {
        BlockPos upperPos = lowerPos.above();
        BlockState lower = level.getBlockState(lowerPos);
        if (lower.getBlock() instanceof SpiritualRunestoneBlock) {
            level.setBlock(lowerPos, lower.setValue(property, value), Block.UPDATE_ALL);
        }
        BlockState upper = level.getBlockState(upperPos);
        if (upper.getBlock() instanceof SpiritualRunestoneBlock) {
            level.setBlock(upperPos, upper.setValue(property, value), Block.UPDATE_ALL);
        }
    }

    private static void playPrayerEcho(ServerLevel level, BlockPos lowerPos, int prayers) {
        float progress = prayers / (float) (PRAYERS_FOR_RAGE - 1);
        level.playSound(null, lowerPos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS,
                1.0F + progress, 0.6F + progress * 0.6F);
        level.sendParticles(PRAYER_DUST, lowerPos.getX() + 0.5D, lowerPos.getY() + 1.0D, lowerPos.getZ() + 0.5D,
                PRAYER_DUST_COUNT, 0.4D, 0.8D, 0.4D, 0.02D);
    }

    private static void playRageBurst(ServerLevel level, BlockPos lowerPos) {
        level.playSound(null, lowerPos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 6.0F, 0.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, lowerPos.getX() + 0.5D, lowerPos.getY() + 1.0D,
                lowerPos.getZ() + 0.5D, RAGE_BURST_COUNT, 0.6D, 1.2D, 0.6D, 0.4D);
    }

    // Кровавые частицы родителя не нужны: свои рисует блок-сущность нижней половины
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new SpiritualRunestoneBlockEntity(pos, state) : null;
    }

    // Клиент - частицы; сервер - усиление воинов, пока камень активен, и угасание в конце ярости
    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.SPIRITUAL_RUNESTONE_BE.get()) {
            return null;
        }
        BlockEntityTicker<SpiritualRunestoneBlockEntity> ticker = level.isClientSide()
                ? SpiritualRunestoneBlockEntity::clientTick
                : SpiritualRunestoneBlockEntity::serverTick;
        return (BlockEntityTicker<T>) ticker;
    }
}
