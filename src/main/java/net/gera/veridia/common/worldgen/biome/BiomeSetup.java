package net.gera.veridia.common.worldgen.biome;

import net.gera.veridia.Veridia;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

/** Подключает регион (пепельные биомы) и поверхностные правила Veridia к TerraBlender. Вызывать из enqueueWork. */
public class BiomeSetup {
    public static void register() {
        Regions.register(new AshLandsRegion());
        SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, Veridia.MOD_ID,
                AshWastesSurfaceRules.create());
    }
}
