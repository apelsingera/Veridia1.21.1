package net.gera.veridia.common.compat.graveyard;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Одно вылезание моба из гроба на сервере: крышка открывается, моб плавно поднимается изнутри гроба наружу за
 * AppearTime тиков, крышка закрывается. Пока идёт, гроб занят (клики по нему ничего не открывают). Тикает из
 * {@link CoffinEmergences}; в мире не сохраняется, а моб, застрявший на середине (сервер остановили), приводится в
 * порядок при загрузке, см. {@link #restoreIfStuck}.
 */
final class CoffinEmergence {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Событие блока, которым Graveyard открывает/закрывает крышку (параметр 1 - открыть, 0 - закрыть)
    private static final int LID_EVENT = 1;
    // Крышка Graveyard раскрывается на 0.1 за тик, то есть за 10 тиков
    private static final int LID_TICKS = 10;

    private static final int PARTICLES_PER_TICK = 3;
    private static final int BURST_PARTICLES = 24;
    private static final double PARTICLE_SPREAD_ALONG = 0.5;
    private static final double PARTICLE_SPREAD_ACROSS = 0.2;
    private static final double PARTICLE_SPREAD_UP = 0.1;
    private static final double PARTICLE_SPEED = 0.02;

    private static final String GRAVEYARD_NAMESPACE = "graveyard";
    private static final float SOUND_VOLUME = 0.75F;

    // Метки на мобе, пока он вылезает (теги переживают сохранение, в отличие от наших полей)
    private static final String TAG_EMERGING = "veridia_coffin_emerging";
    private static final String TAG_WAS_NO_AI = "veridia_coffin_was_no_ai";
    private static final String TAG_WAS_INVULNERABLE = "veridia_coffin_was_invulnerable";

    private enum Phase { OPENING_LID, RISING, CLOSING_LID }

    private final ServerLevel level;
    private final CoffinSite site;
    private final Block block;
    private final CoffinTrapSpec spec;
    private final UUID openerId;

    private Phase phase = Phase.OPENING_LID;
    private int phaseTicks;
    private Entity mob;
    // Дно гроба и его верх (по коллизии блока), абсолютные высоты
    private double floorY;
    private double topY;
    // Высота ног моба в начале (целиком под дном) и в конце (стоит на гробе)
    private double startY;

    CoffinEmergence(ServerLevel level, CoffinSite site, Block block, CoffinTrapSpec spec, UUID openerId) {
        this.level = level;
        this.site = site;
        this.block = block;
        this.spec = spec;
        this.openerId = openerId;
    }

    boolean isIn(ServerLevel other) {
        return level == other;
    }

    boolean occupies(ServerLevel other, CoffinSite otherSite) {
        return level == other && site.foot().equals(otherSite.foot());
    }

    boolean isMob(Entity entity) {
        return mob != null && mob.getUUID().equals(entity.getUUID());
    }

    void begin() {
        measureCoffin();
        setLidOpen(true);
        playLidSound(true);
        emitParticles(BURST_PARTICLES);
    }

    private void measureCoffin() {
        VoxelShape shape = level.getBlockState(site.foot()).getCollisionShape(level, site.foot());
        floorY = site.foot().getY() + (shape.isEmpty() ? 0.0 : shape.min(Direction.Axis.Y));
        topY = site.foot().getY() + (shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y));
    }

    /** Один тик; true - всё закончилось, задачу можно убирать. */
    boolean tick() {
        if (!level.isLoaded(site.foot()) || level.getBlockState(site.foot()).getBlock() != block) {
            unfreezeMob();
            return true;
        }
        phaseTicks++;
        switch (phase) {
            case OPENING_LID -> {
                if (phaseTicks >= LID_TICKS) {
                    startRising();
                }
            }
            case RISING -> tickRising();
            case CLOSING_LID -> {
                return phaseTicks >= LID_TICKS;
            }
        }
        return false;
    }

    private void startRising() {
        Vec3 center = site.center();
        Entity created = spec.mob().create(level, new Vec3(center.x, topY, center.z));
        if (created == null) {
            LOGGER.warn("Ловушка гроба в {}: моб не создался", site.foot());
            beginClosing();
            return;
        }
        mob = created;
        startY = floorY - created.getBbHeight();
        freezeMob();
        faceOpener(center);
        created.setPos(center.x, startY, center.z);
        if (!level.tryAddFreshEntityWithPassengers(created)) {
            LOGGER.warn("Ловушка гроба в {}: моб {} не добавился в мир", site.foot(), created.getUUID());
            unfreezeMob();
            mob = null;
            beginClosing();
            return;
        }
        CoffinTrap.disarm(level, site);
        phase = Phase.RISING;
        phaseTicks = 0;
    }

    private void tickRising() {
        if (mob.isRemoved()) {
            beginClosing();
            return;
        }
        double progress = Math.min(1.0, phaseTicks / (double) spec.appearTicks());
        double eased = progress * progress * (3.0 - 2.0 * progress);
        Vec3 center = site.center();
        mob.setPos(center.x, Mth.lerp(eased, startY, topY), center.z);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.fallDistance = 0.0F;
        emitParticles(PARTICLES_PER_TICK);
        if (progress >= 1.0) {
            unfreezeMob();
            emitParticles(BURST_PARTICLES);
            beginClosing();
        }
    }

    private void beginClosing() {
        setLidOpen(false);
        playLidSound(false);
        phase = Phase.CLOSING_LID;
        phaseTicks = 0;
    }

    // Пока моб вылезает: без ИИ и неуязвим, без столкновений (он стартует внутри блоков)
    private void freezeMob() {
        if (mob instanceof Mob living) {
            if (living.isNoAi()) {
                mob.addTag(TAG_WAS_NO_AI);
            }
            living.setNoAi(true);
        }
        if (mob.isInvulnerable()) {
            mob.addTag(TAG_WAS_INVULNERABLE);
        }
        mob.setInvulnerable(true);
        mob.noPhysics = true;
        mob.addTag(TAG_EMERGING);
    }

    private void unfreezeMob() {
        if (mob != null) {
            restore(mob);
        }
    }

    /** Возвращает мобу ИИ и уязвимость, отнятые на время вылезания, и снимает метки. */
    private static void restore(Entity entity) {
        if (entity instanceof Mob living) {
            living.setNoAi(entity.getTags().contains(TAG_WAS_NO_AI));
        }
        entity.setInvulnerable(entity.getTags().contains(TAG_WAS_INVULNERABLE));
        entity.noPhysics = false;
        entity.removeTag(TAG_EMERGING);
        entity.removeTag(TAG_WAS_NO_AI);
        entity.removeTag(TAG_WAS_INVULNERABLE);
    }

    /**
     * Моб, загрузившийся с меткой "вылезает", но без живой задачи (сервер остановили на середине): возвращает ему
     * ИИ и уязвимость, чтобы он не остался замороженным навсегда.
     */
    static void restoreIfStuck(Entity entity) {
        if (entity.getTags().contains(TAG_EMERGING)) {
            restore(entity);
        }
    }

    // Моб смотрит на открывшего гроб игрока, если он ещё здесь, иначе вдоль гроба
    private void faceOpener(Vec3 center) {
        Player opener = level.getPlayerByUUID(openerId);
        float yaw = opener == null
                ? site.facing().toYRot()
                : (float) (Mth.atan2(opener.getZ() - center.z, opener.getX() - center.x) * Mth.RAD_TO_DEG) - 90.0F;
        mob.setYRot(yaw);
        mob.setXRot(0.0F);
        if (mob instanceof LivingEntity living) {
            living.setYBodyRot(yaw);
            living.setYHeadRot(yaw);
        }
    }

    private void setLidOpen(boolean open) {
        level.blockEvent(site.foot(), block, LID_EVENT, open ? 1 : 0);
    }

    // Звуки самого Graveyard: у гроба (LIT) свои открытие и закрытие, у саркофага один звук на оба
    private void playLidSound(boolean open) {
        BlockState state = level.getBlockState(site.foot());
        boolean coffin = state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
        String name = coffin ? (open ? "block.coffin.open" : "block.coffin.close") : "block.sarcophagus.use";
        BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.fromNamespaceAndPath(GRAVEYARD_NAMESPACE, name))
                .ifPresent(this::playAtCoffin);
    }

    private void playAtCoffin(SoundEvent sound) {
        Vec3 center = site.center();
        level.playSound(null, BlockPos.containing(center.x, topY, center.z), sound, SoundSource.BLOCKS,
                SOUND_VOLUME, 0.9F + level.random.nextFloat() * 0.1F);
    }

    // Частицы выбранного типа над гробом, всегда одним и тем же узором: облако вдоль гроба
    private void emitParticles(int count) {
        SimpleParticleType particle = spec.particle();
        Vec3 center = site.center();
        boolean alongX = site.axis() == Direction.Axis.X;
        level.sendParticles(particle, center.x, topY, center.z, count,
                alongX ? PARTICLE_SPREAD_ALONG : PARTICLE_SPREAD_ACROSS, PARTICLE_SPREAD_UP,
                alongX ? PARTICLE_SPREAD_ACROSS : PARTICLE_SPREAD_ALONG, PARTICLE_SPEED);
    }
}
