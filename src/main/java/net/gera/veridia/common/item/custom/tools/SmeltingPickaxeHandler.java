package net.gera.veridia.common.item.custom.tools;
import net.gera.veridia.Veridia;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Optional;
@EventBusSubscriber(modid = Veridia.MOD_ID)
public class SmeltingPickaxeHandler {
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        Entity breaker = event.getBreaker();
        if (breaker == null) return;
        if (!(event.getTool().getItem() instanceof SmeltingPickaxeItem)) return;
        ServerLevel level = event.getLevel();
        RecipeManager recipeManager = level.getRecipeManager();
        boolean minedOre = event.getState().is(Tags.Blocks.ORES);
        for (ItemEntity itemEntity : event.getDrops()) {
            ItemStack original = itemEntity.getItem();
            Optional<RecipeHolder<SmeltingRecipe>> match = recipeManager.getRecipeFor(
                    RecipeType.SMELTING, new SingleRecipeInput(original), level);
            match.ifPresent(recipe -> {
                ItemStack smelted = recipe.value().getResultItem(level.registryAccess()).copy();
                if (!minedOre && !(smelted.getItem() instanceof BlockItem)) {
                    return; // не руда и плавится не в блок - оставляем обычный дроп
                }
                smelted.setCount(original.getCount());
                itemEntity.setItem(smelted);
            });
        }
    }
}