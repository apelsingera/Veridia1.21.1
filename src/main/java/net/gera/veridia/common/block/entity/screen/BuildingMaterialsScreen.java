package net.gera.veridia.common.block.entity.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BuildingMaterialsScreen extends WorkerStorageScreen<BuildingMaterialsMenu> {
    public BuildingMaterialsScreen(BuildingMaterialsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
