package net.gera.veridia.common.villager.mixin;

import net.gera.veridia.common.aibuff.villager.farmer.FarmerUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ComposterBlock.class)
public class ComposterBlockMixin {

    @Redirect(
            method = "addItem(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/" +
                    "LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/ComposterBlock;getValue(Lnet/minecraft/world/item/ItemStack;)F"),
            remap = false
    )
    private static float veridia$boostedComposterChance(ItemStack composterItem,
                                                          Entity entity, BlockState state, LevelAccessor level,
                                                          BlockPos pos, ItemStack stack) {
        float base = ComposterBlock.getValue(composterItem);
        if (entity instanceof Villager villager
                && villager.getVillagerData().getProfession() == VillagerProfession.FARMER) {
            return FarmerUpgrades.boostedComposterChance(villager, base);
        }
        return base;
    }
}
