package net.gera.veridia.common.block.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.block.SpiritualRunestoneBlock;
import net.gera.veridia.common.block.entity.types.SpiritualRunestoneBlockEntity;
import net.gera.veridia.common.entity.ominous.OminousWarriorEntity;
import net.gera.veridia.common.entity.ominous.rage.client.OminousRageBloom;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Свечение духовного рунного камня и лучи от него.
 * <ul>
 *   <li>Руны светятся поверх камня: аддитивный слой с маской {@link #GLOW_MASK} (что светлее - то ярче светится),
 *       вокруг мягкий ореол. С каждой молитвой ярче и шире; активный камень (идёт начавшаяся с него ярость) - в полную
 *       силу и пульсирует быстрее.</li>
 *   <li>Активный камень тянет лучи к воинам, которых усиливает ({@link OminousWarriorEntity#isEmpowered}), в радиусе
 *       {@link SpiritualRunestoneBlockEntity#EMPOWER_RADIUS}. Лучи следуют за воинами и мерцают, как горящие.</li>
 *   <li>Всё это ярко-пурпурное, поэтому вокруг ещё ложится настоящее свечение {@link OminousRageBloom}.</li>
 * </ul>
 */
public class SpiritualRunestoneRenderer implements BlockEntityRenderer<SpiritualRunestoneBlockEntity> {
    private static final ResourceLocation GLOW_MASK =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/misc/spiritual_runestone_glow.png");

    // Яркость свечения: при первой молитве, при полном заряде, у активного камня
    private static final float GLOW_FIRST_PRAYER = 0.25F;
    private static final float GLOW_CHARGED = 0.75F;
    private static final float GLOW_ACTIVE = 1.0F;
    // Треснувший камень (SpiritualRunestoneBlock.CRACKS) светится ярче, ореол шире - на столько за каждую трещину
    private static final float CRACK_GLOW_STEP = 0.4F;
    private static final float CRACK_HALO_GROWTH = 0.5F;
    // Пульсация: глубина и скорость (радиан за тик) у заряжающегося и у активного камня
    private static final float PULSE_DEPTH = 0.15F;
    private static final float PULSE_SPEED_CHARGING = 0.08F;
    private static final float PULSE_SPEED_ACTIVE = 0.22F;

    // Слой рун чуть отступает от поверхности камня, чтобы не мерцать вместе с ней
    private static final float RUNE_LAYER_LIFT = 0.01F;

    // Ореол: мягкий круг лицом к камере на середине камня. Радиус при первой молитве / полном заряде / у активного
    private static final float HALO_HEIGHT = 1.1F;
    private static final float HALO_RADIUS_FIRST_PRAYER = 0.7F;
    private static final float HALO_RADIUS_CHARGED = 1.4F;
    private static final float HALO_RADIUS_ACTIVE = 2.2F;
    private static final int HALO_CENTER_ALPHA = 120;
    private static final int[] HALO_COLOR = {170, 60, 255};

    // Лучи: от верхушки камня в грудь воину (доля роста). Ядро и свечение вокруг - полуширина ленты в блоках
    private static final float BEAM_START_HEIGHT = 1.85F;
    private static final double BEAM_TARGET_HEIGHT = 0.6D;
    private static final double BEAM_CORE_HALF_WIDTH = 0.06D;
    private static final double BEAM_GLOW_HALF_WIDTH = 0.25D;
    private static final int[] BEAM_CORE_COLOR = {255, 200, 255};
    private static final int BEAM_CORE_ALPHA = 235;
    private static final int[] BEAM_GLOW_COLOR = {180, 50, 255};
    private static final int BEAM_GLOW_ALPHA = 130;
    // Мерцание лучей (как у посоха): глубина и скорость; у каждого луча своя фаза
    private static final float BEAM_FLICKER_DEPTH = 0.18F;
    private static final float BEAM_FLICKER_SPEED = 0.55F;
    // Вспышки на концах лучей: у камня и на воине
    private static final float BEAM_SOURCE_GLOW_RADIUS = 0.45F;
    private static final float BEAM_IMPACT_GLOW_RADIUS = 0.6F;
    private static final int BEAM_END_GLOW_ALPHA = 200;

    // Мягкий круг: к DISC_MID_RADIUS (доля радиуса) яркость падает до DISC_MID_BRIGHTNESS, к краю - до нуля
    private static final int DISC_SEGMENTS = 20;
    private static final float DISC_MID_RADIUS = 0.35F;
    private static final float DISC_MID_BRIGHTNESS = 0.4F;

    private static final int VIEW_DISTANCE = 96;
    private static final long QUAD_SEED = 42L;

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create();

    public SpiritualRunestoneRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(SpiritualRunestoneBlockEntity stone, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = stone.getLevel();
        float charge = stone.charge();
        int cracks = stone.getBlockState().getValue(SpiritualRunestoneBlock.CRACKS);
        if (level == null || (charge <= 0.0F && cracks == 0)) {
            return;
        }
        boolean active = stone.isActive();
        float time = level.getGameTime() + partialTick;
        float pulse = 1.0F + PULSE_DEPTH * Mth.sin(time * (active ? PULSE_SPEED_ACTIVE : PULSE_SPEED_CHARGING));
        float glow = ((active ? GLOW_ACTIVE : Mth.lerp(charge, GLOW_FIRST_PRAYER, GLOW_CHARGED))
                + cracks * CRACK_GLOW_STEP) * pulse;
        PoseStack.Pose pose = poseStack.last();

        // Ярче единицы один слой не светит - лишнее рисуется вторым слоем поверх (сложение цветов)
        VertexConsumer runes = buffers.getBuffer(RenderType.eyes(GLOW_MASK));
        renderRuneLayer(stone.getBlockState(), pose, runes, Math.min(glow, 1.0F));
        if (glow > 1.0F) {
            renderRuneLayer(stone.getBlockState(), pose, runes, Math.min(glow - 1.0F, 1.0F));
        }

        VertexConsumer rays = buffers.getBuffer(RenderType.dragonRays());
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        float haloRadius = ((active ? HALO_RADIUS_ACTIVE
                : Mth.lerp(charge, HALO_RADIUS_FIRST_PRAYER, HALO_RADIUS_CHARGED)) + cracks * CRACK_HALO_GROWTH) * pulse;
        int haloAlpha = Math.round(HALO_CENTER_ALPHA * Math.min(glow, 1.0F));
        drawGlowDisc(pose, rays, camera, new Vec3(0.5D, HALO_HEIGHT, 0.5D), haloRadius, HALO_COLOR, haloAlpha);
        if (active) {
            renderBeams(stone, partialTick, time, pose, rays, camera);
        }
        OminousRageBloom.requestGlow(glow);
    }

    // ---------------------------------------------------------------- руны

    // Те же квадраты, что у модели камня, но с маской свечения вместо текстуры, без освещения и чуть над поверхностью
    private void renderRuneLayer(BlockState state, PoseStack.Pose pose, VertexConsumer consumer, float brightness) {
        BakedModel model = blockRenderer.getBlockModel(state);
        for (Direction side : Direction.values()) {
            random.setSeed(QUAD_SEED);
            for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, null)) {
                putRuneQuad(pose, consumer, quad, brightness);
            }
        }
        random.setSeed(QUAD_SEED);
        for (BakedQuad quad : model.getQuads(state, null, random, ModelData.EMPTY, null)) {
            putRuneQuad(pose, consumer, quad, brightness);
        }
    }

    private static void putRuneQuad(PoseStack.Pose pose, VertexConsumer consumer, BakedQuad quad, float brightness) {
        int[] vertices = quad.getVertices();
        TextureAtlasSprite sprite = quad.getSprite();
        Vec3i normal = quad.getDirection().getNormal();
        for (int i = 0; i < 4; i++) {
            int offset = i * IQuadTransformer.STRIDE;
            float x = Float.intBitsToFloat(vertices[offset + IQuadTransformer.POSITION]) + normal.getX() * RUNE_LAYER_LIFT;
            float y = Float.intBitsToFloat(vertices[offset + IQuadTransformer.POSITION + 1]) + normal.getY() * RUNE_LAYER_LIFT;
            float z = Float.intBitsToFloat(vertices[offset + IQuadTransformer.POSITION + 2]) + normal.getZ() * RUNE_LAYER_LIFT;
            // UV в атласе блоков -> UV в маске (у неё та же раскладка, что у текстуры камня)
            float u = sprite.getUOffset(Float.intBitsToFloat(vertices[offset + IQuadTransformer.UV0]));
            float v = sprite.getVOffset(Float.intBitsToFloat(vertices[offset + IQuadTransformer.UV0 + 1]));
            consumer.addVertex(pose, x, y, z)
                    .setColor(brightness, brightness, brightness, 1.0F)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(pose, normal.getX(), normal.getY(), normal.getZ());
        }
    }

    // ---------------------------------------------------------------- лучи

    private void renderBeams(SpiritualRunestoneBlockEntity stone, float partialTick, float time,
                             PoseStack.Pose pose, VertexConsumer consumer, Camera camera) {
        BlockPos origin = stone.getBlockPos();
        Vec3 originVec = Vec3.atLowerCornerOf(origin);
        Vec3 cameraPos = camera.getPosition().subtract(originVec);
        Vec3 start = new Vec3(0.5D, BEAM_START_HEIGHT, 0.5D);
        boolean anyBeam = false;
        for (OminousWarriorEntity warrior : stone.ragingWarriorsInRange()) {
            if (!warrior.isEmpowered()) {
                continue;
            }
            Vec3 end = warrior.getPosition(partialTick).subtract(originVec)
                    .add(0.0D, warrior.getBbHeight() * BEAM_TARGET_HEIGHT, 0.0D);
            float flicker = 1.0F - BEAM_FLICKER_DEPTH
                    + BEAM_FLICKER_DEPTH * Mth.sin(time * BEAM_FLICKER_SPEED + warrior.getId());
            drawBeam(pose, consumer, cameraPos, start, end, flicker);
            drawGlowDisc(pose, consumer, camera, end, BEAM_IMPACT_GLOW_RADIUS * flicker, BEAM_GLOW_COLOR,
                    Math.round(BEAM_END_GLOW_ALPHA * flicker));
            anyBeam = true;
        }
        if (anyBeam) {
            drawGlowDisc(pose, consumer, camera, start, BEAM_SOURCE_GLOW_RADIUS, BEAM_CORE_COLOR, BEAM_END_GLOW_ALPHA);
        }
    }

    // Лента лицом к камере: широкое мягкое свечение и узкое яркое ядро
    private static void drawBeam(PoseStack.Pose pose, VertexConsumer consumer, Vec3 cameraPos, Vec3 start, Vec3 end,
                                 float flicker) {
        Vec3 axis = end.subtract(start);
        Vec3 toCamera = cameraPos.subtract(start.add(end).scale(0.5D));
        Vec3 side = axis.cross(toCamera);
        if (axis.lengthSqr() < 1.0E-4D || side.lengthSqr() < 1.0E-8D) {
            return;
        }
        side = side.normalize();
        drawSoftRibbon(pose, consumer, start, end, side.scale(BEAM_GLOW_HALF_WIDTH * flicker),
                BEAM_GLOW_COLOR, Math.round(BEAM_GLOW_ALPHA * flicker));
        drawSoftRibbon(pose, consumer, start, end, side.scale(BEAM_CORE_HALF_WIDTH),
                BEAM_CORE_COLOR, Math.round(BEAM_CORE_ALPHA * flicker));
    }

    // Лента от start до end: по оси яркость alpha, к краям (ось +- halfWidth) гаснет до нуля
    private static void drawSoftRibbon(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end,
                                       Vec3 halfWidth, int[] color, int alpha) {
        Vec3 startLeft = start.add(halfWidth);
        Vec3 startRight = start.subtract(halfWidth);
        Vec3 endLeft = end.add(halfWidth);
        Vec3 endRight = end.subtract(halfWidth);
        triangle(pose, consumer, color, start, alpha, end, alpha, endLeft, 0);
        triangle(pose, consumer, color, start, alpha, endLeft, 0, startLeft, 0);
        triangle(pose, consumer, color, start, alpha, startRight, 0, endRight, 0);
        triangle(pose, consumer, color, start, alpha, endRight, 0, end, alpha);
    }

    // ---------------------------------------------------------------- мягкий круг

    // Круг лицом к камере: ярко в центре, к DISC_MID_RADIUS тускнеет, к краю гаснет
    private static void drawGlowDisc(PoseStack.Pose pose, VertexConsumer consumer, Camera camera, Vec3 center,
                                     float radius, int[] color, int centerAlpha) {
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1.0D);
        Vec3 up = new Vec3(camera.getUpVector());
        int midAlpha = Math.round(centerAlpha * DISC_MID_BRIGHTNESS);
        for (int i = 0; i < DISC_SEGMENTS; i++) {
            float angle0 = Mth.TWO_PI * i / DISC_SEGMENTS;
            float angle1 = Mth.TWO_PI * (i + 1) / DISC_SEGMENTS;
            Vec3 mid0 = pointOnCircle(center, right, up, angle0, radius * DISC_MID_RADIUS);
            Vec3 mid1 = pointOnCircle(center, right, up, angle1, radius * DISC_MID_RADIUS);
            Vec3 edge0 = pointOnCircle(center, right, up, angle0, radius);
            Vec3 edge1 = pointOnCircle(center, right, up, angle1, radius);
            triangle(pose, consumer, color, center, centerAlpha, mid0, midAlpha, mid1, midAlpha);
            triangle(pose, consumer, color, mid0, midAlpha, edge0, 0, edge1, 0);
            triangle(pose, consumer, color, mid0, midAlpha, edge1, 0, mid1, midAlpha);
        }
    }

    private static Vec3 pointOnCircle(Vec3 center, Vec3 right, Vec3 up, float angle, float radius) {
        return center.add(right.scale(Mth.cos(angle) * radius)).add(up.scale(Mth.sin(angle) * radius));
    }

    // Треугольник с обеих сторон: лучи отрисовываются с отсечением задних граней, а смотреть на них можно откуда угодно
    private static void triangle(PoseStack.Pose pose, VertexConsumer consumer, int[] color,
                                 Vec3 a, int alphaA, Vec3 b, int alphaB, Vec3 c, int alphaC) {
        vertex(pose, consumer, color, a, alphaA);
        vertex(pose, consumer, color, b, alphaB);
        vertex(pose, consumer, color, c, alphaC);
        vertex(pose, consumer, color, a, alphaA);
        vertex(pose, consumer, color, c, alphaC);
        vertex(pose, consumer, color, b, alphaB);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int[] color, Vec3 point, int alpha) {
        consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(color[0], color[1], color[2], Mth.clamp(alpha, 0, 255));
    }

    // ---------------------------------------------------------------- видимость

    // Активный камень тянет лучи до воинов в радиусе усиления - рисуем его, пока виден хоть кусок этой области
    @Override
    public AABB getRenderBoundingBox(SpiritualRunestoneBlockEntity stone) {
        BlockPos pos = stone.getBlockPos();
        AABB stoneBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
        return stoneBox.inflate(stone.isActive() ? SpiritualRunestoneBlockEntity.EMPOWER_RADIUS + 2.0D : HALO_RADIUS_ACTIVE);
    }

    // Лучи видны и тогда, когда сам камень за краем экрана или в невидимом участке мира
    @Override
    public boolean shouldRenderOffScreen(SpiritualRunestoneBlockEntity stone) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }
}
