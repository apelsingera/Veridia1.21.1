package net.gera.veridia.common.mana.spell;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface GemSpell {
        void castNormal(ServerPlayer player, ItemStack stack);
        void castSuper(ServerPlayer player, ItemStack stack);
    }