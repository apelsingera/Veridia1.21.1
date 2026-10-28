package net.gera.veridia.common.particle.client;

import net.gera.veridia.common.particle.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

/**
 * Красный пузырь: поднимается в воде и лопается (RED_BUBBLE_POP), дойдя до поверхности. Как ванильный
 * BubbleParticle, только поднимается ровно (ванильный живёт слишком мало и до поверхности не доплывает)
 * и лопается сам, а не просто исчезает.
 */
public class RedBubbleParticle extends TextureSheetParticle {
    // Размер пузыря в блоках (у ванильного 0.02-0.16)
    private static final float MIN_SIZE = 0.06F;
    private static final float MAX_SIZE = 0.12F;
    // Скорость подъёма, блоков за тик (разброс между пузырями), и боковое качание
    private static final double MIN_RISE_SPEED = 0.025D;
    private static final double MAX_RISE_SPEED = 0.045D;
    private static final double SWAY = 0.004D;
    // Шанс лопнуть в любой тик, пока пузырь ещё под водой
    private static final float POP_CHANCE_PER_TICK = 0.006F;
    // Предохранитель: пузырь, не дошедший до поверхности за это время, просто лопается
    private static final int MAX_AGE_TICKS = 20 * 8;

    protected RedBubbleParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.setSize(0.02F, 0.02F);
        this.quadSize = MIN_SIZE + this.random.nextFloat() * (MAX_SIZE - MIN_SIZE);
        this.xd = 0.0D;
        this.yd = MIN_RISE_SPEED + this.random.nextDouble() * (MAX_RISE_SPEED - MIN_RISE_SPEED);
        this.zd = 0.0D;
        this.lifetime = MAX_AGE_TICKS;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime || this.random.nextFloat() < POP_CHANCE_PER_TICK) {
            pop();
            return;
        }
        this.xd += (this.random.nextDouble() * 2.0D - 1.0D) * SWAY;
        this.zd += (this.random.nextDouble() * 2.0D - 1.0D) * SWAY;
        this.xd *= 0.9D;
        this.zd *= 0.9D;
        this.move(this.xd, this.yd, this.zd);
        if (!isUnderWaterSurface()) {
            pop();
        }
    }

    // Пузырь под водой, пока его точка в воде и ниже её поверхности (у источника вода ниже края блока)
    private boolean isUnderWaterSurface() {
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        FluidState fluid = this.level.getFluidState(pos);
        return fluid.is(FluidTags.WATER) && this.y < pos.getY() + fluid.getHeight(this.level, pos);
    }

    private void pop() {
        this.remove();
        this.level.addParticle(ModParticles.RED_BUBBLE_POP.get(), this.x, this.y, this.z, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            RedBubbleParticle particle = new RedBubbleParticle(level, x, y, z);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
