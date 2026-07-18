package net.gera.veridia.worldgen.trees;

import net.gera.veridia.Veridia;
import net.gera.veridia.worldgen.ModConfiguredFeatures;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

public class TreeGrowers {
    public static final TreeGrower SCULK_TREE = new TreeGrower(Veridia.MOD_ID + ":sculktree",
            Optional.empty(), Optional.of(ModConfiguredFeatures.SCULK_TREE_KEY), Optional.empty());
    }