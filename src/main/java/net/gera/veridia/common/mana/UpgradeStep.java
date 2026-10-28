package net.gera.veridia.common.mana;

import java.util.Set;

public record UpgradeStep(String tier, int tierStep, String registryName,
                           int capacityDelta, double regenPercentDelta,
                           double failureChanceDelta, double superChanceDelta,
                           Set<Perk> perks) {

    public UpgradeStep(String tier, int tierStep, String registryName,
                        int capacityDelta, double regenPercentDelta,
                        double failureChanceDelta, double superChanceDelta) {
        this(tier, tierStep, registryName, capacityDelta, regenPercentDelta,
                failureChanceDelta, superChanceDelta, Set.of());
    }
}
