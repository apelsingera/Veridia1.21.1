package net.gera.veridia.common.villager.mixin;

import net.gera.veridia.common.aibuff.villager.farmer.FarmerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Апгрейд rich_soil (FarmerUpgrades) - даёт farmer-жителю шанс при сборе урожая (тот самый
 * destroyBlock мёртвой грядки в ванильном HarvestFarmland.tick) улучшить грядку под собой из
 * обычного farmland в farmersdelight:rich_soil_farmland (сохраняя текущую влажность). Farmers
 * Delight уже умеет заставлять жителей работать на rich_soil_farmland (свой compat-миксин на
 * SecondaryPoiSensor) - тут только сама конвертация в другую сторону.
 */
@Mixin(HarvestFarmland.class)
public class HarvestFarmlandMixin {

    @Redirect(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;J)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"),
            remap = false
    )
    private boolean veridia$destroyAndMaybeUpgradeSoil(ServerLevel level, BlockPos cropPos, boolean dropBlock, Entity entity,
                                                         ServerLevel level2, Villager villager, long gameTime) {
        boolean result = level.destroyBlock(cropPos, dropBlock, entity);
        if (villager.getVillagerData().getProfession() == VillagerProfession.FARMER) {
            tryUpgradeToRichSoil(level, cropPos.below(), villager);
        }
        return result;
    }

    private static void tryUpgradeToRichSoil(ServerLevel level, BlockPos farmlandPos, Villager villager) {
        BlockState state = level.getBlockState(farmlandPos);
        if (!(state.getBlock() instanceof FarmBlock)) return;

        Block richSoilFarmland = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("farmersdelight", "rich_soil_farmland"));
        if (richSoilFarmland == Blocks.AIR || state.is(richSoilFarmland)) return;

        if (level.random.nextFloat() >= FarmerUpgrades.richSoilUpgradeChance(villager)) return;

        BlockState newState = richSoilFarmland.defaultBlockState();
        if (state.hasProperty(FarmBlock.MOISTURE) && newState.hasProperty(FarmBlock.MOISTURE)) {
            newState = newState.setValue(FarmBlock.MOISTURE, state.getValue(FarmBlock.MOISTURE));
        }
        level.setBlockAndUpdate(farmlandPos, newState);
    }
}
