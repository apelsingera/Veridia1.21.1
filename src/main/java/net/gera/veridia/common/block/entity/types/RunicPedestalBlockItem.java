package net.gera.veridia.common.block.entity.types;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Пьедестал списывает xp_levels рецепта прямо со шкалы опыта игрока - предупреждаем об этом в тултипе. */
public class RunicPedestalBlockItem extends BlockItem {
    public RunicPedestalBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.veridia.runic_pedestal_xp_cost").withStyle(ChatFormatting.GRAY));
    }
}