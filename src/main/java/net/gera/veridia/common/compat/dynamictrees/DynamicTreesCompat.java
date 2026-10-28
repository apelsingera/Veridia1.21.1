package net.gera.veridia.common.compat.dynamictrees;

import com.dtteam.dynamictrees.DynamicTrees;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.leaves.DynamicLeavesBlock;
import com.dtteam.dynamictrees.block.soil.SoilBlock;
import com.dtteam.dynamictrees.registry.DTRegistries;
import com.dtteam.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.http.annotation.Experimental;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
@Experimental
public final class DynamicTreesCompat {
    private DynamicTreesCompat() {}

    private static final List<ResourceLocation> SEED_COMMON = List.of(
            ResourceLocation.fromNamespaceAndPath("dynamictrees", "oak"),
            ResourceLocation.fromNamespaceAndPath("dynamictrees", "birch")
    );
    private static final List<ResourceLocation> SEED_RARE = List.of(
            ResourceLocation.fromNamespaceAndPath("dynamictrees", "spruce"),
            ResourceLocation.fromNamespaceAndPath("dynamictrees", "acacia"),
            ResourceLocation.fromNamespaceAndPath("dynamictrees", "dark_oak")
    );
    private static final List<ResourceLocation> SEED_MEGA = List.of(
            ResourceLocation.fromNamespaceAndPath("dynamictrees", "mangrove"),
            ResourceLocation.fromNamespaceAndPath("dynamictrees", "cherry"),
            ResourceLocation.fromNamespaceAndPath("dynamictrees", "jungle")
    );
    /**
     * BranchBlock.destroyMode по умолчанию SLOPPY: когда ветку заменяют любым не-веткой блоком (level.setBlock - наш
     * TreeCutter, стена nailer'а, /setblock, WorldEdit), BranchBlock.onRemove обрушивает ВСЮ связанную сеть соседних
     * веток вместе с листвой (sloppyBreak -> destroyBranchFromNode, плюс падающее дерево-сущность). onRemove
     * вызывается из LevelChunk.setBlockState всегда, флаги setBlock на это не влияют. Поэтому код, который по одному
     * заменяет блоки дерева и записывает их для отката, терял остальное дерево прямо посреди цикла. На время action
     * режим переключается в IGNORE - тот же приём, что у штатного BranchBlock.setBlockStateIgnored. Только серверный
     * поток; прежний режим возвращается всегда.
     */
    public static void withoutBranchCascade(Runnable action) {
        DynamicTrees.DestroyMode previous = BranchBlock.destroyMode;
        BranchBlock.destroyMode = DynamicTrees.DestroyMode.IGNORE;
        try {
            action.run();
        } finally {
            BranchBlock.destroyMode = previous;
        }
    }

    public static void fellTree(Level level, BlockPos pos, LivingEntity entity) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BranchBlock branch) {
            branch.futureBreak(state, level, pos, entity);
        }
    }
    public static boolean isSoil(BlockState state) {
        return state.getBlock() instanceof SoilBlock;
    }

    /** Любой блок мода Dynamic Trees (ветка, корень, листва, плод и т.п.) - по неймспейсу
     * регистрации, а не по конкретному классу, чтобы не перечислять все типы блоков дерева
     * поштучно. Используется VillageBlocks/TreeCutter, чтобы отличать сеть DT-дерева от
     * ванильных брёвен построек игрока, которые касаются этого дерева, но деревом не являются. */
    public static boolean isDynamicTreesBlock(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key != null && "dynamictrees".equals(key.getNamespace());
    }

    /** Листва Dynamic Trees (в теге minecraft:leaves её может не быть - проверяем по классу блока). */
    public static boolean isDynamicLeaves(BlockState state) {
        return isDynamicTreesBlock(state) && state.getBlock() instanceof DynamicLeavesBlock;
    }

    /** Обычная земля под корневым блоком Dynamic Trees (тот самый блок, в котором дерево "растёт", с породой в
     * блок-сущности), или null, если это не корневой блок. Им заменяют корень при вырубке дерева, чтобы на
     * месте дерева не оставалась яма. */
    @Nullable
    public static BlockState primitiveSoil(BlockState state) {
        return state.getBlock() instanceof SoilBlock soil ? soil.getPrimitiveSoilState(state) : null;
    }

    /** Толщина ветки/ствола (1..8, растёт вместе с деревом) - 0, если это вообще не ветка DT. */
    public static int branchRadius(BlockState state) {
        return state.getBlock() instanceof BranchBlock branch ? branch.getRadius(state) : 0;
    }
    public static Item dirtBucket() {
        return DTRegistries.DIRT_BUCKET.get();
    }
    public static Item dendroPotion() {
        return DTRegistries.DENDRO_POTION.get();
    }

    private static ItemStack randomSeedStack(List<ResourceLocation> pool, RandomSource random, int count) {
        List<Species> withSeed = new ArrayList<>();
        for (ResourceLocation id : pool) {
            Species species = Species.REGISTRY.get(id);
            if (species.hasSeed()) {
                withSeed.add(species);
            }
        }
        if (withSeed.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return withSeed.get(random.nextInt(withSeed.size())).getSeedStack(count);
    }

    public static ItemStack randomCommonSeedStack(RandomSource random, int count) {
        return randomSeedStack(SEED_COMMON, random, count);
    }

    public static ItemStack randomRareSeedStack(RandomSource random, int count) {
        return randomSeedStack(SEED_RARE, random, count);
    }

    public static ItemStack randomMegaSeedStack(RandomSource random, int count) {
        return randomSeedStack(SEED_MEGA, random, count);
    }
}