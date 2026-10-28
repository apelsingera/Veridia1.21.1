package net.gera.veridia.common.villager.mixin;

import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.world.entity.ai.behavior.PoiCompetitorScan;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PoiCompetitorScan.class)
public class PoiCompetitorScanMixin {

    @Inject(method = "selectWinner", at = @At("HEAD"), cancellable = true, remap = false)
    private static void veridia$shareStrop(Villager villagerA, Villager villagerB,
                                                CallbackInfoReturnable<Villager> cir) {
        if (veridia$isWarrior(villagerA) && veridia$isWarrior(villagerB)) {
            cir.setReturnValue(villagerA);
        }
    }
    @Unique
    private static boolean veridia$isWarrior(Villager villager) {
        VillagerProfession profession = villager.getVillagerData().getProfession();
        return profession == ModVillagers.WARRIOR.value();
    }
}