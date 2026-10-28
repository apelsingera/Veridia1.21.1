package net.gera.veridia.common.item.custom.gems.lvl1;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DangerousVerdantFluorite extends Item {
    private static final float DAMAGE_PER_SECOND = 1F;
    public DangerousVerdantFluorite(Properties properties) {
        super(properties);
    }
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && entity instanceof Player player) {
            if (!player.isCreative() && !player.isSpectator()) {
                if (level.getGameTime() % 20 == 0) {
                    player.hurt(level.damageSources().magic(), DAMAGE_PER_SECOND);
                }
            }
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}