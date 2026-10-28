package net.gera.veridia.common.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Предмет блока пепельного и обожжённого дерева, который не топливо: ваниль считает топливом доски, брёвна, двери и
 * прочее деревянное по тегам, а это дерево не горит (0 - не топливо ни в одной печи).
 */
public class NoFuelBlockItem extends BlockItem {
    public NoFuelBlockItem(Block block, Properties properties) {
        super(block, properties);
    }
    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return 0;
    }
}