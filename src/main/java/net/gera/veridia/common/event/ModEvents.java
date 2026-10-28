package net.gera.veridia.common.event;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.PullAttackGoal;
import net.gera.veridia.common.aibuff.villager.nailer.NailerUpgrades;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.compat.dynamictrees.DynamicTreesCompat;
import net.gera.veridia.common.compat.spartanweaponry.SpartanWeaponryCompat;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.gera.veridia.common.potion.ModPotions;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;
import java.util.Optional;


@EventBusSubscriber(modid = Veridia.MOD_ID)
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
        if (event.getType() == ModVillagers.WARRIOR.value()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            trades.get(1).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(ModItems.INFESTED_BRAIN.get(), 1),
                    new ItemStack(Items.EMERALD, 4), 5, 4, 0.05f));
        }
        if (event.getType() == ModVillagers.NAILER.value()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            // Хлам-доски любой породы гвоздарь скупает на тех же условиях.
            for (Block scrapWood : List.of(ModBlocks.OAK_SCRAP_WOOD.get(), ModBlocks.SPRUCE_SCRAP_WOOD.get(),
                    ModBlocks.ASHEN_SCRAP_WOOD.get(), ModBlocks.CHARRED_SCRAP_WOOD.get())) {
                trades.get(1).add((entity, randomSource) -> {
                    int cost = 22;
                    if (entity instanceof Villager villager) {
                        cost = Math.max(1, Math.round(cost * NailerUpgrades.tradeCostMultiplier(villager)));
                    }
                    return new MerchantOffer(
                            new ItemCost(scrapWood, cost),
                            new ItemStack(Items.EMERALD, 2), 5, 2, 0.05f);
                });
            }
        }
        if (event.getType() == ModVillagers.LUMBERJACK.value()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            trades.get(1).add((entity, randomSource) -> {
                ItemStack seed = DynamicTreesCompat.randomCommonSeedStack(randomSource, 1);
                if (seed.isEmpty()) return null;
                return new MerchantOffer(new ItemCost(Items.EMERALD, 4), seed, 8, 3, 0.05f);
            });
            trades.get(2).add((entity, randomSource) -> {
                ItemStack seed = DynamicTreesCompat.randomRareSeedStack(randomSource, 1);
                if (seed.isEmpty()) return null;
                return new MerchantOffer(new ItemCost(Items.EMERALD, 8), seed, 6, 4, 0.05f);
            });
            trades.get(2).add((entity, randomSource) -> {
                ItemStack seed = DynamicTreesCompat.randomMegaSeedStack(randomSource, 1);
                if (seed.isEmpty()) return null;
                return new MerchantOffer(new ItemCost(Items.EMERALD, 14), seed, 4, 6, 0.05f);
            });
            trades.get(3).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 20),
                    new ItemStack(SpartanWeaponryCompat.copperBattleaxe(), 1), 2, 12, 0.05f));
            trades.get(3).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 24),
                    new ItemStack(DynamicTreesCompat.dendroPotion(), 1), 2, 15, 0.05f));
        }
        if (event.getType() == VillagerProfession.MASON) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

            trades.get(2).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.GRAVEL, 1),
                    new ItemStack(Items.SAND, 2), 64, 2, 0.05f));
        }
    }
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof Zombie zombie) {
            zombie.goalSelector.addGoal(1, new PullAttackGoal(zombie));
        }
    }
}