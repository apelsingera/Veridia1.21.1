package net.gera.veridia.common.worldgen.feature;

import net.minecraft.util.RandomSource;

/**
 * Волнистость края круглой формы: масштаб радиуса по углу = 1 + amplitude * (sin(a*k1+p1) + sin(a*k2+p2)) / 2.
 * Частоты и фазы случайны, поэтому каждый край получается своим.
 */
record Lobes(int firstFrequency, double firstPhase, int secondFrequency, double secondPhase, double amplitude) {
    static Lobes random(RandomSource random, double amplitude) {
        return new Lobes(2 + random.nextInt(2), random.nextDouble() * Math.TAU,
                3 + random.nextInt(3), random.nextDouble() * Math.TAU, amplitude);
    }

    /** Масштаб радиуса в направлении (dx, dz) от центра. */
    double scaleAt(double dx, double dz) {
        double angle = Math.atan2(dz, dx);
        double wave = Math.sin(angle * firstFrequency + firstPhase) + Math.sin(angle * secondFrequency + secondPhase);
        return 1.0D + amplitude * wave / 2.0D;
    }
}
