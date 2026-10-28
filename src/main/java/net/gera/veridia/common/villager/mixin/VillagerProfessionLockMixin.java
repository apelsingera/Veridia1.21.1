package net.gera.veridia.common.villager.mixin;

import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.villager.ModVillagers;
import net.gera.veridia.common.villager.storage.VillagerEmployment;
import net.gera.veridia.common.villager.storage.VillagerStorageBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerProfessionLockMixin {
    @Unique
    private static final String JOB_SITE_POS_KEY = "veridia_lumberjack_jobsite_pos";
    @Unique
    private static final String JOB_SITE_DIM_KEY = "veridia_lumberjack_jobsite_dim";
    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
    private void veridia$keepLumberjackProfession(VillagerData villagerData, CallbackInfo callbackInfo) {
        if (villagerData.getProfession() != VillagerProfession.NONE) return;

        Villager villager = (Villager) (Object) this;
        if (villager.getVillagerData().getProfession() != ModVillagers.LUMBERJACK.value()) return;

        CompoundTag persisted = villager.getPersistentData();
        if (!persisted.contains(JOB_SITE_POS_KEY)) return;
        if (!(villager.level() instanceof ServerLevel level)) return;

        ResourceLocation dimension = ResourceLocation.tryParse(persisted.getString(JOB_SITE_DIM_KEY));
        if (!level.dimension().location().equals(dimension)) return;

        BlockPos pos = BlockPos.of(persisted.getLong(JOB_SITE_POS_KEY));
        if (level.isLoaded(pos) && !level.getBlockState(pos).is(ModBlocks.LUMBERJACK_LOG.get())) return;

        callbackInfo.cancel();
    }

    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
    private void veridia$blockFiredProfessionReclaim(VillagerData villagerData, CallbackInfo callbackInfo) {
        if (villagerData.getProfession() == VillagerProfession.NONE) return;

        Villager villager = (Villager) (Object) this;
        if (!(villager.level() instanceof ServerLevel level)) return;

        if (VillagerEmployment.isProfessionOnCooldown(villager, villagerData.getProfession(), level)) {
            callbackInfo.cancel();
        }
    }
    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
    private void veridia$keepStorageWorkerProfession(VillagerData villagerData, CallbackInfo callbackInfo) {
        if (villagerData.getProfession() != VillagerProfession.NONE) return;

        Villager villager = (Villager) (Object) this;
        if (villager.getVillagerData().getProfession() == VillagerProfession.NONE) return;
        if (!VillagerStorageBinding.hasAnyBinding(villager)) return;

        callbackInfo.cancel();
    }
}