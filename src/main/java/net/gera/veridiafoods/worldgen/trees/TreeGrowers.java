package net.gera.veridiafoods.worldgen.trees;

import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.worldgen.ModConfiguredFeatures;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

public class TreeGrowers {
    public static final TreeGrower SCULK_TREE = new TreeGrower(VeridiaFoods.MOD_ID + ":sculktree",
            Optional.empty(), Optional.of(ModConfiguredFeatures.SCULK_TREE_KEY), Optional.empty());
    }