package net.gera.veridia.common.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class BowlItem extends Item {

    public BowlItem(Properties properties) {
        super(properties);
    }
    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            if (stack.isEmpty()) {
                return new ItemStack(Items.BOWL);
            } else {
                player.getInventory().add(new ItemStack(Items.BOWL));
            }
        }
        return result;
    }
}