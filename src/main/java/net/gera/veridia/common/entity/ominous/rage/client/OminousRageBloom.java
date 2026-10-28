package net.gera.veridia.common.entity.ominous.rage.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.compat.iris.IrisCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Настоящее свечение (bloom) ярости: всё ярко-пурпурное в кадре - облака, светящийся духовный камень, лучи, частицы -
 * получает мягкий ореол, который прибавляется к картинке.
 * <ol>
 *   <li>extract: из готового кадра берётся только яркий пурпурный, в половинном размере;</li>
 *   <li>blur: размывается по горизонтали и вертикали (ближний ореол), уменьшается ещё вдвое и размывается шире
 *       (дальний ореол);</li>
 *   <li>composite: оба ореола прибавляются к кадру.</li>
 * </ol>
 * Работает после отрисовки мира, до руки и интерфейса (RenderLevelStageEvent AFTER_LEVEL), поэтому всё равно, кто
 * рисовал облака - ванилла или Sodium. С шейдерпаком Iris выключается ({@link IrisCompat}).
 * <p>
 * Включено, пока игрок в ярости ({@link OminousRageSky#amount}) или в кадре светится духовный камень
 * ({@link #requestGlow}). Шейдеры не загрузились - свечение выключается до перезагрузки ресурсов (F3+T).
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public final class OminousRageBloom {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Слабее этого свечение не считаем вовсе
    private static final float MIN_STRENGTH = 0.01F;
    // Радиус размытия в текселях своей картинки: ближний ореол (половинный размер), дальний (четвертной)
    private static final float NEAR_BLUR_RADIUS = 6.0F;
    private static final float DOWNSAMPLE_BLUR_RADIUS = 1.0F;
    private static final float FAR_BLUR_RADIUS = 18.0F;
    // Яркость ореолов при сложении с кадром: ближний подсвечивает сами облака, дальний разливается по небу вокруг
    private static final float NEAR_STRENGTH = 0.45F;
    private static final float FAR_STRENGTH = 0.85F;

    private static final String EXTRACT_SHADER = "veridia:rage_bloom_extract";
    private static final String BLUR_SHADER = "veridia:rage_bloom_blur";
    private static final String COMPOSITE_SHADER = "veridia:rage_bloom_composite";

    @Nullable
    private static Pipeline pipeline;
    private static boolean failed;
    private static float requestedGlow;

    private OminousRageBloom() {
    }

    /** В кадре светится духовный камень с такой силой: свечение нужно и вне ярости. */
    public static void requestGlow(float strength) {
        requestedGlow = Math.max(requestedGlow, strength);
    }

    /** Ресурсы перезагружены: шейдеры соберутся заново в следующем кадре. */
    public static void reset() {
        if (pipeline != null) {
            pipeline.close();
            pipeline = null;
        }
        failed = false;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float strength = Math.max(OminousRageSky.amount(partialTick), requestedGlow);
        requestedGlow = 0.0F;
        if (strength < MIN_STRENGTH || failed || IrisCompat.isShaderPackInUse()) {
            return;
        }
        Pipeline current = getOrCreatePipeline();
        if (current != null) {
            current.render(Minecraft.getInstance().getMainRenderTarget(), strength);
        }
    }

    @Nullable
    private static Pipeline getOrCreatePipeline() {
        if (pipeline == null) {
            Minecraft minecraft = Minecraft.getInstance();
            try {
                pipeline = new Pipeline(minecraft.getResourceManager(), minecraft.getMainRenderTarget());
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Rage bloom shaders failed to load, glow is off until resources are reloaded", e);
                failed = true;
            }
        }
        return pipeline;
    }

    /** Проходы и промежуточные картинки. Живут до перезагрузки ресурсов, картинки следят за размером окна. */
    private static final class Pipeline implements AutoCloseable {
        // Ближний ореол - в половинном размере, дальний - в четвертном; по две картинки, чтобы размывать туда-обратно
        private final TextureTarget nearA;
        private final TextureTarget nearB;
        private final TextureTarget farA;
        private final TextureTarget farB;
        private final List<PostPass> passes = new ArrayList<>();
        @Nullable
        private PostPass extract;
        @Nullable
        private EffectInstance composite;
        private int screenWidth;
        private int screenHeight;

        Pipeline(ResourceProvider resources, RenderTarget screen) throws IOException {
            screenWidth = screen.width;
            screenHeight = screen.height;
            nearA = createTarget(half(screenWidth), half(screenHeight));
            nearB = createTarget(half(screenWidth), half(screenHeight));
            farA = createTarget(quarter(screenWidth), quarter(screenHeight));
            farB = createTarget(quarter(screenWidth), quarter(screenHeight));
            try {
                extract = addPass(resources, EXTRACT_SHADER, screen, nearA);
                addBlur(resources, nearA, nearB, 1.0F, 0.0F, NEAR_BLUR_RADIUS);
                addBlur(resources, nearB, nearA, 0.0F, 1.0F, NEAR_BLUR_RADIUS);
                addBlur(resources, nearA, farA, 1.0F, 0.0F, DOWNSAMPLE_BLUR_RADIUS);
                addBlur(resources, farA, farB, 1.0F, 0.0F, FAR_BLUR_RADIUS);
                addBlur(resources, farB, farA, 0.0F, 1.0F, FAR_BLUR_RADIUS);
                composite = new EffectInstance(resources, COMPOSITE_SHADER);
            } catch (IOException | RuntimeException e) {
                close();
                throw e;
            }
        }

        private PostPass addPass(ResourceProvider resources, String shader, RenderTarget in, RenderTarget out)
                throws IOException {
            // Фильтрацию картинок задаём сами (createTarget), флаг прохода без PostChain ни на что не влияет
            PostPass pass = new PostPass(resources, shader, in, out, false);
            passes.add(pass);
            return pass;
        }

        private void addBlur(ResourceProvider resources, RenderTarget in, RenderTarget out,
                             float directionX, float directionY, float radius) throws IOException {
            EffectInstance effect = addPass(resources, BLUR_SHADER, in, out).getEffect();
            effect.safeGetUniform("BlurDir").set(directionX, directionY);
            effect.safeGetUniform("Radius").set(radius);
        }

        void render(RenderTarget screen, float strength) {
            if (extract == null || composite == null) {
                return;
            }
            followScreenSize(screen);
            extract.getEffect().safeGetUniform("Intensity").set(strength);
            for (PostPass pass : passes) {
                pass.process(0.0F);
            }
            addGlowTo(screen, composite);
        }

        // Ореолы прибавляются к кадру (сложение цветов); глубину и прозрачность кадра не трогаем
        private void addGlowTo(RenderTarget screen, EffectInstance effect) {
            screen.bindWrite(true);
            effect.setSampler("NearSampler", nearA::getColorTextureId);
            effect.setSampler("FarSampler", farA::getColorTextureId);
            effect.safeGetUniform("OutSize").set((float) screen.width, (float) screen.height);
            effect.safeGetUniform("NearStrength").set(NEAR_STRENGTH);
            effect.safeGetUniform("FarStrength").set(FAR_STRENGTH);
            effect.apply();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.colorMask(true, true, true, false);
            drawScreenQuad(screen.width, screen.height);
            effect.clear();
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }

        // Квадрат на весь экран в пикселях (как у ванильных пост-проходов); вершинный шейдер делит на OutSize
        private static void drawScreenQuad(float width, float height) {
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            buffer.addVertex(0.0F, 0.0F, 500.0F);
            buffer.addVertex(width, 0.0F, 500.0F);
            buffer.addVertex(width, height, 500.0F);
            buffer.addVertex(0.0F, height, 500.0F);
            BufferUploader.draw(buffer.buildOrThrow());
        }

        private void followScreenSize(RenderTarget screen) {
            if (screen.width == screenWidth && screen.height == screenHeight) {
                return;
            }
            screenWidth = screen.width;
            screenHeight = screen.height;
            resize(nearA, half(screenWidth), half(screenHeight));
            resize(nearB, half(screenWidth), half(screenHeight));
            resize(farA, quarter(screenWidth), quarter(screenHeight));
            resize(farB, quarter(screenWidth), quarter(screenHeight));
        }

        // Картинки ореола читаются с линейной фильтрацией: так размытие и растягивание на весь экран гладкие
        private static TextureTarget createTarget(int width, int height) {
            TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            target.setFilterMode(GL11.GL_LINEAR);
            return target;
        }

        // resize пересоздаёт текстуру с фильтрацией по умолчанию - возвращаем линейную
        private static void resize(TextureTarget target, int width, int height) {
            target.resize(width, height, Minecraft.ON_OSX);
            target.setFilterMode(GL11.GL_LINEAR);
        }

        private static int half(int size) {
            return Math.max(1, size / 2);
        }

        private static int quarter(int size) {
            return Math.max(1, size / 4);
        }

        @Override
        public void close() {
            for (PostPass pass : passes) {
                pass.close();
            }
            passes.clear();
            if (composite != null) {
                composite.close();
            }
            nearA.destroyBuffers();
            nearB.destroyBuffers();
            farA.destroyBuffers();
            farB.destroyBuffers();
        }
    }
}
