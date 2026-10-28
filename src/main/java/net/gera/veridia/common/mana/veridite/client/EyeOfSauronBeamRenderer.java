package net.gera.veridia.common.mana.veridite.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.gera.veridia.Veridia;
import net.minecraft.client.Camera;
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
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public final class EyeOfSauronBeamRenderer {

    private static final long BEAM_TTL_TICKS = 6L;

    private static final float CORE_RADIUS = 0.035F;
    private static final float GLOW_RADIUS = 0.09F;
    private static final float MUZZLE_GLOW_RADIUS = 0.13F;

    private static final int CORE_R = 255, CORE_G = 180, CORE_B = 110, CORE_A = 235;
    private static final int GLOW_R = 255, GLOW_G = 80, GLOW_B = 40, GLOW_A = 70;
    private static final int MUZZLE_R = 255, MUZZLE_G = 210, MUZZLE_B = 160, MUZZLE_A = 210;

    private static final double HAND_FP_FORWARD = 0.35D;
    private static final double HAND_FP_RIGHT = 0.42D;
    private static final double HAND_FP_DOWN = 0.42D;
    private static final double HAND_TP_FORWARD = 0.45D;
    private static final double HAND_TP_RIGHT = 0.35D;
    private static final double HAND_TP_DOWN = 0.45D;

    private static final Map<Integer, Beam> BEAMS = new HashMap<>();

    private EyeOfSauronBeamRenderer() {
    }

    private record Beam(Vec3 prevEnd, Vec3 end, long lastTick, int tier) {
    }

    public static void onBeamPayload(int shooterId, double ex, double ey, double ez, int tier) {
        Level level = Minecraft.getInstance().level;
        long now = level != null ? level.getGameTime() : 0L;
        Vec3 newEnd = new Vec3(ex, ey, ez);
        Beam prev = BEAMS.get(shooterId);
        Vec3 prevEnd = (prev != null && now - prev.lastTick() <= 2L) ? prev.end() : newEnd;
        BEAMS.put(shooterId, new Beam(prevEnd, newEnd, now, Math.max(1, Math.min(5, tier))));
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || BEAMS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            BEAMS.clear();
            return;
        }

        long now = level.getGameTime();
        BEAMS.values().removeIf(beam -> now - beam.lastTick() > BEAM_TTL_TICKS);
        if (BEAMS.isEmpty()) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType renderType = RenderType.dragonRays();
        VertexConsumer vc = buffers.getBuffer(renderType);

        for (Map.Entry<Integer, Beam> entry : BEAMS.entrySet()) {
            Entity shooter = level.getEntity(entry.getKey());
            if (shooter == null) {
                continue;
            }
            Beam beam = entry.getValue();
            float lerp = Math.max(0.0F, Math.min(1.0F, (now - beam.lastTick()) + partialTick));
            Vec3 rawEnd = beam.prevEnd().lerp(beam.end(), lerp);

            boolean firstPerson = shooter == mc.player && mc.options.getCameraType().isFirstPerson();
            Vec3 start = firstPerson
                    ? handPointFirstPerson(camera)
                    : handPointThirdPerson(shooter, partialTick);

            if (start.distanceToSqr(rawEnd) < 0.09) {
                continue;
            }
            float aMul = 0.45F + 0.13F * beam.tier();
            float rMul = 0.75F + 0.07F * beam.tier();

            drawBeam(poseStack, vc, cam, start, rawEnd, aMul, rMul);
            drawMuzzleGlow(poseStack, vc, cam, camera, start, aMul, rMul);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        buffers.endBatch(renderType);
    }

    private static void drawBeam(PoseStack poseStack, VertexConsumer vc, Vec3 cam, Vec3 start, Vec3 end,
                                 float aMul, float rMul) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-3) {
            return;
        }
        Vec3 dir = delta.scale(1.0 / length);
        Vec3 ref = Math.abs(dir.y) < 0.999 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 side = dir.cross(ref).normalize();
        Vec3 up = dir.cross(side).normalize();

        float gr = GLOW_RADIUS * rMul;
        float cr = CORE_RADIUS * rMul;
        int ga = clamp255(GLOW_A * aMul);
        int ca = clamp255(CORE_A * aMul);

        poseStack.pushPose();
        poseStack.translate(start.x - cam.x, start.y - cam.y, start.z - cam.z);
        PoseStack.Pose pose = poseStack.last();

        beamQuad(pose, vc, dir, side, length, gr, GLOW_R, GLOW_G, GLOW_B, ga);
        beamQuad(pose, vc, dir, up, length, gr, GLOW_R, GLOW_G, GLOW_B, ga);
        beamQuad(pose, vc, dir, side, length, cr, CORE_R, CORE_G, CORE_B, ca);
        beamQuad(pose, vc, dir, up, length, cr, CORE_R, CORE_G, CORE_B, ca);

        poseStack.popPose();
    }

    private static int clamp255(float f) {
        return Math.max(0, Math.min(255, Math.round(f)));
    }

    private static void beamQuad(PoseStack.Pose pose, VertexConsumer vc, Vec3 dir, Vec3 perp,
                                 double length, float radius, int r, int g, int b, int a) {
        float px = (float) (perp.x * radius);
        float py = (float) (perp.y * radius);
        float pz = (float) (perp.z * radius);
        float lx = (float) (dir.x * length);
        float ly = (float) (dir.y * length);
        float lz = (float) (dir.z * length);

        float ax = -px, ay = -py, az = -pz;
        float bx = px, by = py, bz = pz;
        float cx = lx + px, cy = ly + py, cz = lz + pz;
        float dx = lx - px, dy = ly - py, dz = lz - pz;

        tri(pose, vc, ax, ay, az, bx, by, bz, cx, cy, cz, r, g, b, a);
        tri(pose, vc, ax, ay, az, cx, cy, cz, dx, dy, dz, r, g, b, a);
    }

    private static Vec3 handPointFirstPerson(Camera camera) {
        Vec3 p = camera.getPosition();
        Vector3f f = camera.getLookVector();
        Vector3f l = camera.getLeftVector();
        Vector3f u = camera.getUpVector();
        return p.add(
                f.x() * HAND_FP_FORWARD - l.x() * HAND_FP_RIGHT - u.x() * HAND_FP_DOWN,
                f.y() * HAND_FP_FORWARD - l.y() * HAND_FP_RIGHT - u.y() * HAND_FP_DOWN,
                f.z() * HAND_FP_FORWARD - l.z() * HAND_FP_RIGHT - u.z() * HAND_FP_DOWN);
    }

    private static Vec3 handPointThirdPerson(Entity shooter, float partialTick) {
        Vec3 eye = shooter.getEyePosition(partialTick);
        Vec3 look = shooter.getViewVector(partialTick);
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        }
        right = right.normalize();
        return eye.add(look.scale(HAND_TP_FORWARD))
                .add(right.scale(HAND_TP_RIGHT))
                .subtract(0.0, HAND_TP_DOWN, 0.0);
    }
    private static void drawMuzzleGlow(PoseStack poseStack, VertexConsumer vc, Vec3 cam, Camera camera, Vec3 at,
                                       float aMul, float rMul) {
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        float r = MUZZLE_GLOW_RADIUS * rMul;
        int ma = clamp255(MUZZLE_A * aMul);

        poseStack.pushPose();
        poseStack.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        PoseStack.Pose pose = poseStack.last();

        int seg = 10;
        for (int i = 0; i < seg; i++) {
            double a0 = (i / (double) seg) * Math.PI * 2.0;
            double a1 = ((i + 1) / (double) seg) * Math.PI * 2.0;
            float r0x = (float) ((-left.x() * Math.cos(a0) + up.x() * Math.sin(a0)) * r);
            float r0y = (float) ((-left.y() * Math.cos(a0) + up.y() * Math.sin(a0)) * r);
            float r0z = (float) ((-left.z() * Math.cos(a0) + up.z() * Math.sin(a0)) * r);
            float r1x = (float) ((-left.x() * Math.cos(a1) + up.x() * Math.sin(a1)) * r);
            float r1y = (float) ((-left.y() * Math.cos(a1) + up.y() * Math.sin(a1)) * r);
            float r1z = (float) ((-left.z() * Math.cos(a1) + up.z() * Math.sin(a1)) * r);

            vc.addVertex(pose, 0.0F, 0.0F, 0.0F).setColor(MUZZLE_R, MUZZLE_G, MUZZLE_B, ma);
            vc.addVertex(pose, r0x, r0y, r0z).setColor(MUZZLE_R, MUZZLE_G, MUZZLE_B, 0);
            vc.addVertex(pose, r1x, r1y, r1z).setColor(MUZZLE_R, MUZZLE_G, MUZZLE_B, 0);
        }

        poseStack.popPose();
    }

    private static void tri(PoseStack.Pose pose, VertexConsumer vc,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            float x3, float y3, float z3, int r, int g, int b, int a) {
        vc.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        vc.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
    }
}