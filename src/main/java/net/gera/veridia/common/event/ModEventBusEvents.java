package net.gera.veridia.common.event;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.AbilityConfig;
import net.gera.veridia.common.entity.client.RustyNailModel;
import net.gera.veridia.common.item.custom.heavy_projectile.HeavyProjectileModel;
import net.gera.veridia.common.entity.mutant.Mutant;
import net.gera.veridia.common.entity.ominous.BrutalOminousWarrior;
import net.gera.veridia.common.entity.ominous.OminousWarrior;
import net.gera.veridia.common.entity.cinderroamer.CinderRoamer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HeavyProjectileModel.LAYER_LOCATION, HeavyProjectileModel::createBodyLayer);
        event.registerLayerDefinition(RustyNailModel.LAYER_LOCATION, RustyNailModel::createBodyLayer);
        event.registerLayerDefinition(Mutant.LAYER_LOCATION, Mutant::createBodyLayer);
        event.registerLayerDefinition(OminousWarrior.LAYER_LOCATION, OminousWarrior::createBodyLayer);
        event.registerLayerDefinition(BrutalOminousWarrior.LAYER_LOCATION, BrutalOminousWarrior::createBodyLayer);
        event.registerLayerDefinition(CinderRoamer.LAYER_LOCATION, CinderRoamer::createBodyLayer);
    }
    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.VILLAGER, Attributes.ATTACK_DAMAGE, AbilityConfig.VILLAGER_ATTACK_DAMAGE);
    }
}