package net.gera.veridiafoods.event;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.gera.veridiafoods.VeridiaFoods;
import net.gera.veridiafoods.item.ModItems;
import net.gera.veridiafoods.villager.ModVillagers;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.gera.veridiafoods.potion.ModPotions;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = VeridiaFoods.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ModEvents {

    @SubscribeEvent
    public static void onBrewingRecipeRegister(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();
        builder.addMix(Potions.STRONG_HARMING, Items.SLIME_BALL, ModPotions.PLAGUE_POTION);
    }

    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.FARMER) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

            trades.get(3).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 4),
                    new ItemStack(ModItems.CRANBERRIES.get(), 1), 12, 3, 0.1f));
        }
        if (event.getType() == ModVillagers.SAGE.value()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

            trades.get(1).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(ModItems.GARLIC, 1),
                    Optional.of(new ItemCost(ModItems.DANDELIONS_ROOT, 1)),
                    new ItemStack(ModItems.RED_ONION.get(), 3), 4, 1, 0.05f));
            trades.get(1).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(ModItems.PLANT_STRING, 11),
                    new ItemStack(ModItems.BELL_PEPPER_SEEDS.get(), 1), 5, 1, 0.05f));
            trades.get(2).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(ModItems.RED_BELL_PEPPER, 1),
                    new ItemStack(ModItems.TOMATO_CHERRY.get(), 2), 4, 2, 0.05f));
            trades.get(2).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(ModItems.MILK_N_BUTTER, 2),
                    new ItemStack(ModItems.CHEESE.get(), 1), 5, 2, 0.05f));
            trades.get(3).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(ModItems.FIBER_NET, 1),
                    Optional.of(new ItemCost(Items.MILK_BUCKET, 1)),
                    new ItemStack(ModItems.BUTTER.get(), 4), 5, 5, 0.05f));
            trades.get(3).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 1),
                    new ItemStack(ModItems.EXOTIC_SEASONING1.get(), 3), 3, 4, 0.05f));
            trades.get(4).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 2),
                    new ItemStack(ModItems.EXOTIC_SEASONING2.get(), 3), 3, 4, 0.05f));
            trades.get(4).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 3),
                    new ItemStack(ModItems.EXOTIC_SEASONING3.get(), 3), 3, 4, 0.05f));
        }
        if (event.getType() == VillagerProfession.MASON) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

            trades.get(2).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.GRAVEL, 1),
                    new ItemStack(Items.SAND, 2), 64, 2, 0.05f));
        }
    }
}