package net.gera.veridia.common.particle.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

/** Лопание красного пузыря: пять кадров red_bubble_pop_0..4 растягиваются на время жизни частицы. */
public class RedBubblePopParticle extends TextureSheetParticle {
    // Сколько тиков идёт лопание (у ванильного 4) и размер вспышки в блоках
    private static final int LIFETIME_TICKS = 6;
    private static final float MIN_SIZE = 0.14F;
    private static final float MAX_SIZE = 0.2F;

    private final SpriteSet sprites;

    protected RedBubblePopParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = LIFETIME_TICKS;
        this.gravity = 0.0F;
        this.quadSize = MIN_SIZE + this.random.nextFloat() * (MAX_SIZE - MIN_SIZE);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new RedBubblePopParticle(level, x, y, z, sprites);
        }
    }
}
