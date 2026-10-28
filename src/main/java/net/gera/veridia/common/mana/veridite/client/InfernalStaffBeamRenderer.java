package net.gera.veridia.common.mana.veridite.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.gera.veridia.Veridia;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public final class InfernalStaffBeamRenderer {

    private static final long TTL_TICKS = 4L;
    private static final double FOOT_Y = 0.12D;
    private static final double HUB_SKIP = 0.35D;
    private static final float TIP_FRAC = 0.12F;
    private static final double TIP_RISE = 0.22D;

    private static final float CORE_RADIUS = 0.05F;
    private static final float GLOW_RADIUS = 0.14F;
    private static final int CORE_R = 190, CORE_G = 18, CORE_B = 12, CORE_A = 235;
    private static final int GLOW_R = 100, GLOW_G = 4, GLOW_B = 3, GLOW_A = 90;

    private static final Map<Integer, Staff> STAVES = new HashMap<>();

    private InfernalStaffBeamRenderer() {
    }

    private record Staff(int beamCount, float beamLength, float radiansPerTick, int tier, long lastTick) {
    }

    public static void onBeamPayload(int playerId, int beamCount, float beamLength, float radiansPerTick, int tier) {
        Level level = Minecraft.getInstance().level;
        long now = level != null ? level.getGameTime() : 0L;
        STAVES.put(playerId, new Staff(Math.max(1, beamCount), beamLength, radiansPerTick,
                Math.max(1, Math.min(5, tier)), now));
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || STAVES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            STAVES.clear();
            return;
        }

        long now = level.getGameTime();
        STAVES.values().removeIf(staff -> now - staff.lastTick() > TTL_TICKS);
        if (STAVES.isEmpty()) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType renderType = RenderType.dragonRays();
        VertexConsumer vc = buffers.getBuffer(renderType);

        double baseTime = (double) now + partialTick;

        for (Map.Entry<Integer, Staff> entry : STAVES.entrySet()) {
            Entity player = level.getEntity(entry.getKey());
            if (player == null) {
                continue;
            }
            Staff staff = entry.getValue();
            Vec3 feet = player.getPosition(partialTick).add(0.0D, FOOT_Y, 0.0D);
            double baseAngle = baseTime * staff.radiansPerTick();

            float pulse = 0.82F + 0.18F * (float) Math.sin(baseTime * 0.55); // горение
            float aMul = (0.40F + 0.16F * staff.tier()) * pulse;             // ярче с уровнем: t1 ~0.56 .. t5 ~1.2
            float rMul = 0.78F + 0.06F * staff.tier();
            int ga = clamp255(GLOW_A * aMul);
            int ca = clamp255(CORE_A * aMul);
            float gr = GLOW_RADIUS * rMul;
            float cr = CORE_RADIUS * rMul;

            int count = staff.beamCount();
            for (int i = 0; i < count; i++) {
                double angle = baseAngle + (Math.PI * 2.0 / count) * i;
                Vec3 dir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
                Vec3 start = feet.add(dir.scale(HUB_SKIP));
                Vec3 end = feet.add(dir.scale(staff.beamLength())).add(0.0D, TIP_RISE, 0.0D);
                drawBeam(poseStack, vc, cam, start, end, gr, GLOW_R, GLOW_G, GLOW_B, ga);
                drawBeam(poseStack, vc, cam, start, end, cr, CORE_R, CORE_G, CORE_B, ca);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        buffers.endBatch(renderType);
    }

    private static void drawBeam(PoseStack poseStack, VertexConsumer vc, Vec3 cam, Vec3 start, Vec3 end,
                                 float radius, int r, int g, int b, int a) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-3) {
            return;
        }
        Vec3 dir = delta.scale(1.0 / length);
        Vec3 ref = Math.abs(dir.y) < 0.999 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 side = dir.cross(ref).normalize();
        Vec3 up = dir.cross(side).normalize();

        poseStack.pushPose();
        poseStack.translate(start.x - cam.x, start.y - cam.y, start.z - cam.z);
        PoseStack.Pose pose = poseStack.last();
        blade(pose, vc, dir, side, length, radius, radius * TIP_FRAC, r, g, b, a);
        blade(pose, vc, dir, up, length, radius, radius * TIP_FRAC, r, g, b, a);
        poseStack.popPose();
    }

    private static void blade(PoseStack.Pose pose, VertexConsumer vc, Vec3 dir, Vec3 perp,
                              double length, float hubR, float tipR, int r, int g, int b, int a) {
        float hpx = (float) (perp.x * hubR), hpy = (float) (perp.y * hubR), hpz = (float) (perp.z * hubR);
        float tpx = (float) (perp.x * tipR), tpy = (float) (perp.y * tipR), tpz = (float) (perp.z * tipR);
        float lx = (float) (dir.x * length), ly = (float) (dir.y * length), lz = (float) (dir.z * length);
        float ax = -hpx, ay = -hpy, az = -hpz;
        float bx = hpx, by = hpy, bz = hpz;
        float cx = lx + tpx, cy = ly + tpy, cz = lz + tpz;
        float dx = lx - tpx, dy = ly - tpy, dz = lz - tpz;
        tri(pose, vc, ax, ay, az, bx, by, bz, cx, cy, cz, r, g, b, a);
        tri(pose, vc, ax, ay, az, cx, cy, cz, dx, dy, dz, r, g, b, a);
    }

    private static void tri(PoseStack.Pose pose, VertexConsumer vc,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            float x3, float y3, float z3, int r, int g, int b, int a) {
        vc.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        vc.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
    }

    private static int clamp255(float f) {
        return Math.max(0, Math.min(255, Math.round(f)));
    }
}
