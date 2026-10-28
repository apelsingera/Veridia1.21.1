package net.gera.veridia;

import net.gera.veridia.common.aibuff.handlers.*;
import net.gera.veridia.common.aibuff.levelzombie.LevelHuskItems;
import net.gera.veridia.common.aibuff.levelzombie.LevelZombieItems;
import net.gera.veridia.common.block.ModBlocks;
import net.gera.veridia.common.block.entity.ModBlockEntities;
import net.gera.veridia.common.block.entity.client.LumberjackLogBlockEntityRenderer;
import net.gera.veridia.common.block.entity.client.SpiritualRunestoneRenderer;
import net.gera.veridia.common.block.entity.screen.AncientDocumentScreen;
import net.gera.veridia.common.block.entity.screen.BuildingMaterialsScreen;
import net.gera.veridia.common.block.entity.screen.CookingTableScreen;
import net.gera.veridia.common.block.entity.screen.ExtractorScreen;
import net.gera.veridia.common.block.entity.screen.FoodStorageScreen;
import net.gera.veridia.common.block.entity.screen.MenuTypes;
import net.gera.veridia.common.block.entity.screen.MythicalTableScreen;
import net.gera.veridia.common.block.entity.screen.RunicPedestalScreen;
import net.gera.veridia.common.effect.ModEffects;
import net.gera.veridia.common.effect.onentity.TagEffectHandler;
import net.gera.veridia.common.entity.ModEntities;
import net.gera.veridia.common.entity.client.HeavyProjectileRenderer;
import net.gera.veridia.common.entity.client.RustyNailRenderer;
import net.gera.veridia.common.entity.mutant.client.MutantRenderer;
import net.gera.veridia.common.entity.ominous.client.OminousWarriorRenderer;
import net.gera.veridia.common.entity.cinderroamer.client.CinderRoamerRenderer;
import net.gera.veridia.common.entity.cinderroamer.client.CinderRoamerScreen;
import net.gera.veridia.common.entity.ominous.rage.client.OminousRageBloom;
import net.gera.veridia.common.fluid.ModFluids;
import net.gera.veridia.common.item.ModCreativeModeTabs;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.item.custom.PortalEventsHandler;
import net.gera.veridia.common.mana.ModKnowledgeAttachments;
import net.gera.veridia.common.loot.ModLootModifier;
import net.gera.veridia.common.mana.ModAttachments;
import net.gera.veridia.common.mana.ModManaItems;
import net.gera.veridia.common.particle.ModParticles;
import net.gera.veridia.common.particle.client.RedBubbleParticle;
import net.gera.veridia.common.particle.client.RedBubblePopParticle;
import net.gera.veridia.common.particle.client.SparkParticle;
import net.gera.veridia.common.potion.ModPotions;
import net.gera.veridia.common.recipe.ModRecipes;
import net.gera.veridia.server.discord.DiscordGate;
import net.gera.veridia.server.discord.DiscordGateConfig;
import net.gera.veridia.common.aibuff.villager.farmer.FarmerUpgrades;
import net.gera.veridia.common.aibuff.villager.lumberjack.LumberjackUpgrades;
import net.gera.veridia.common.aibuff.villager.nailer.NailerUpgrades;
import net.gera.veridia.common.aibuff.villager.warrior.WarriorUpgrades;
import net.gera.veridia.common.client.AshLeavesTint;
import net.gera.veridia.common.client.VillagerProfessionScreen;
import net.gera.veridia.common.villager.upgrade.VillagerUpgradeTree;
import net.gera.veridia.common.sound.ModSounds;
import net.gera.veridia.common.util.Registries;
import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import javax.swing.*;

@Mod(Veridia.MOD_ID)
public class Veridia {
    public static final String MOD_ID = "veridia";
    //private static final Logger LOGGER = LogUtils.getLogger()
    public Veridia(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModFluids.register(modEventBus);
        net.gera.veridia.common.worldgen.terrain.ModDensityFunctions.register(modEventBus);
        Registries.register(modEventBus);
        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModEntities.register(modEventBus);
        ModLootModifier.register(modEventBus);
        MenuTypes.register(modEventBus);
        ModVillagers.register(modEventBus);
        net.gera.veridia.common.block.RunestonePoiTypes.register(modEventBus);
        LumberjackUpgrades.ensureLoaded();
        FarmerUpgrades.ensureLoaded();
        WarriorUpgrades.ensureLoaded();
        NailerUpgrades.ensureLoaded();
        VillagerUpgradeTree.registerAll(modEventBus);
        ModSounds.register(modEventBus);
        ModParticles.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModKnowledgeAttachments.register(modEventBus);
        ModManaItems.register(modEventBus);
        LevelZombieItems.register(modEventBus);
        LevelHuskItems.register(modEventBus);
        net.gera.veridia.common.worldgen.feature.ModFeatures.register(modEventBus);
        NeoForge.EVENT_BUS.register(PortalEventsHandler.class);
        NeoForge.EVENT_BUS.register(SpiderSummonHandler.class);
        NeoForge.EVENT_BUS.register(NoKnockbackHandler.class);
        NeoForge.EVENT_BUS.register(BurstShootHandler.class);
        NeoForge.EVENT_BUS.register(PoisonOnHitHandler.class);
        NeoForge.EVENT_BUS.register(LongReachHandler.class);
        NeoForge.EVENT_BUS.register(TagEffectHandler.class);
        NeoForge.EVENT_BUS.register(WitherBuffHandler.class);
        NeoForge.EVENT_BUS.register(ExplosiveMagmaCubeHandler.class);
        NeoForge.EVENT_BUS.register(ZombieBuffHandler.class);
        NeoForge.EVENT_BUS.register(DiscordGate.class);
        NeoForge.EVENT_BUS.register(SlownessOnHitHandler.class);
        NeoForge.EVENT_BUS.register(BuffAuraAttackSpeedHandler.class);
        NeoForge.EVENT_BUS.register(ExplosiveSlimeHandler.class);
        NeoForge.EVENT_BUS.register(GiantZombieHandler.class);
        NeoForge.EVENT_BUS.register(LaserTriggerHandler.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, DiscordGateConfig.SPEC, "veridia-discordgate.toml");
    }
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModFluids::registerCauldronInteractions);
        event.enqueueWork(net.gera.veridia.common.worldgen.biome.BiomeSetup::register);
    }
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.HEAVY_PROJECTILE.get(), HeavyProjectileRenderer::new);
            EntityRenderers.register(ModEntities.RUSTY_NAIL.get(), RustyNailRenderer::new);
            EntityRenderers.register(ModEntities.MUTANT.get(), MutantRenderer::new);
            EntityRenderers.register(ModEntities.OMINOUS_WARRIOR.get(), OminousWarriorRenderer::new);
            EntityRenderers.register(ModEntities.BRUTAL_OMINOUS_WARRIOR.get(), net.gera.veridia.common.entity.ominous.client.BrutalOminousWarriorRenderer::new);
            EntityRenderers.register(ModEntities.CINDER_ROAMER.get(), CinderRoamerRenderer::new);
        }
        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.SPARK.get(), SparkParticle.Provider::new);
            event.registerSpriteSet(ModParticles.RED_BUBBLE.get(), RedBubbleParticle.Provider::new);
            event.registerSpriteSet(ModParticles.RED_BUBBLE_POP.get(), RedBubblePopParticle.Provider::new);
        }
        @SubscribeEvent
        public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
            event.register((state, level, pos, tintIndex) -> 0x4A3222, ModFluids.TANNIN_CAULDRON.get());
            event.register(AshLeavesTint::blockColor, ModBlocks.ASHEN_LEAVES.get());
            event.register(AshLeavesTint::vineColor, net.minecraft.world.level.block.Blocks.VINE);
            event.register(net.gera.veridia.common.client.AshGrassTint::blockColor, ModBlocks.ASHEN_GRASS.get());
        }
        @SubscribeEvent
        public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
            event.register(AshLeavesTint::itemColor, ModBlocks.ASHEN_LEAVES.get());
        }
        @SubscribeEvent
        public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.LUMBERJACK_LOG_BE.get(), LumberjackLogBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.SPIRITUAL_RUNESTONE_BE.get(), SpiritualRunestoneRenderer::new);
        }
        // Шейдеры свечения ярости собираются заново после перезагрузки ресурсов (F3+T, смена ресурспаков)
        @SubscribeEvent
        public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> OminousRageBloom.reset());
        }
        @SubscribeEvent
            public static void registerScreens(RegisterMenuScreensEvent event) {
                event.register(MenuTypes.COOKING_TABLE_MENU.get(), CookingTableScreen::new);
                event.register(MenuTypes.MYTHICAL_TABLE_MENU.get(), MythicalTableScreen::new);
                event.register(MenuTypes.RUNIC_PEDESTAL_MENU.get(), RunicPedestalScreen::new);
                event.register(MenuTypes.EXTRACTOR_MENU.get(), ExtractorScreen::new);
                event.register(MenuTypes.ANCIENT_DOCUMENT_MENU.get(), AncientDocumentScreen::new);
                event.register(MenuTypes.VILLAGER_PROFESSION_MENU.get(), VillagerProfessionScreen::new);
                event.register(MenuTypes.CINDER_ROAMER_MENU.get(), CinderRoamerScreen::new);
                event.register(MenuTypes.BUILDING_MATERIALS_MENU_1.get(), BuildingMaterialsScreen::new);
                event.register(MenuTypes.BUILDING_MATERIALS_MENU_2.get(), BuildingMaterialsScreen::new);
                event.register(MenuTypes.BUILDING_MATERIALS_MENU_3.get(), BuildingMaterialsScreen::new);
                event.register(MenuTypes.BUILDING_MATERIALS_MENU_4.get(), BuildingMaterialsScreen::new);
                event.register(MenuTypes.FOOD_STORAGE_MENU_1.get(), FoodStorageScreen::new);
                event.register(MenuTypes.FOOD_STORAGE_MENU_2.get(), FoodStorageScreen::new);
                event.register(MenuTypes.FOOD_STORAGE_MENU_3.get(), FoodStorageScreen::new);
                event.register(MenuTypes.FOOD_STORAGE_MENU_4.get(), FoodStorageScreen::new);
            }
        }
    }