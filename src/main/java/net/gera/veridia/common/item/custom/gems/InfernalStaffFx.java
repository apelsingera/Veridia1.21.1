package net.gera.veridia.common.item.custom.gems;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Тёмно-красное облако частиц у ног игрока в центре вертушки Infernal Staff - "старые" dust-частицы
 * 0x8B0000, теперь собранные в хаб под геометрическими клинками. Общий для всех 5 уровней.
 */
public final class InfernalStaffFx {

    private static final DustParticleOptions DARK_RED =
            new DustParticleOptions(Vec3.fromRGB24(0x8B0000).toVector3f(), 1.3F);
    private static final int PER_TICK = 5;
    private static final double INNER_RADIUS = 0.3D;
    private static final double OUTER_RADIUS = 1.5D;

    private InfernalStaffFx() {
    }

    public static void spawnHubParticles(ServerLevel level, Vec3 center) {
        for (int i = 0; i < PER_TICK; i++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = INNER_RADIUS + level.getRandom().nextDouble() * (OUTER_RADIUS - INNER_RADIUS);
            level.sendParticles(DARK_RED,
                    center.x + Math.cos(angle) * radius,
                    center.y + 0.06D,
                    center.z + Math.sin(angle) * radius,
                    1, 0.0D, 0.015D, 0.0D, 0.0D);
        }
    }
}