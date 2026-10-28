package net.gera.veridia.common.particle.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public class SparkParticle extends TextureSheetParticle {
    private static final int TICKS_PER_FRAME = 1;
    private static final int FRAME_COUNT = 6;

    private final SpriteSet sprites;

    protected SparkParticle(ClientLevel level, double x, double y, double z,
                             double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.friction = 0.96F;
        this.gravity = 0.5F;
        this.quadSize = 0.15F + this.random.nextFloat() * 0.1F;
        this.lifetime = 10 + this.random.nextInt(8);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = true;
        this.updateSprite();
    }

    @Override
    public void tick() {
        super.tick();
        this.updateSprite();
    }

    private void updateSprite() {
        int frame = (this.age / TICKS_PER_FRAME) % FRAME_COUNT;
        this.setSprite(sprites.get(frame, FRAME_COUNT - 1));
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {

        @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                           double xd, double yd, double zd) {
                return new SparkParticle(level, x, y, z, xd, yd, zd, sprites);
            }
        }
}
