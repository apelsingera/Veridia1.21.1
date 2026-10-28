package net.gera.veridia.common.compat.tfmg;

/**
 * Стальной бак TFMG (миксин compat/tfmg/mixin/SteelTankBlockEntityMixin): нагрев ректификационной колонны, который
 * считает главный блок бака.
 */
public interface DistillationTank {
    int getDistillationHeat();

    DistillationReport getDistillationReport();
}
