package net.gera.veridia.common.compat.curios;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface IActivatableCurio {
    void onActivate(ServerPlayer player, ItemStack stack);
}