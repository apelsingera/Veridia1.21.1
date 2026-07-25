package net.gera.veridiafoods;

import net.gera.veridiafoods.block.ModBlocks;
import net.gera.veridiafoods.block.entity.ModBlockEntities;
import net.gera.veridiafoods.block.entity.screen.CookingTableScreen;
import net.gera.veridiafoods.block.entity.screen.MenuTypes;
import net.gera.veridiafoods.effect.ModEffects;
import net.gera.veridiafoods.item.ModCreativeModeTabs;
import net.gera.veridiafoods.item.ModItems;
import net.gera.veridiafoods.loot.ModLootModifier;
import net.gera.veridiafoods.potion.ModPotions;
import net.gera.veridiafoods.recipe.ModRecipes;
import net.gera.veridiafoods.villager.ModVillagers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(VeridiaFoods.MOD_ID)
public class VeridiaFoods {
    public static final String MOD_ID = "veridiafoods";
    private static final Logger LOGGER = LogUtils.getLogger();

    public VeridiaFoods(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);
        ModCreativeModeTabs.register(modEventBus);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        ModRecipes.register(modEventBus);

        ModLootModifier.register(modEventBus);

        MenuTypes.register(modEventBus);
        ModVillagers.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }
        @SubscribeEvent
            public static void registerScreens(RegisterMenuScreensEvent event) {
                event.register(MenuTypes.COOKING_TABLE_MENU.get(), CookingTableScreen::new);
            }

        }
    }