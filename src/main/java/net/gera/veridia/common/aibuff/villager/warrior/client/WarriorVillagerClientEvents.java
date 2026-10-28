package net.gera.veridia.common.aibuff.villager.warrior.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.client.mixin.LivingEntityRendererAccessor;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.List;

/**
 * Довешиваем слои на ВАНИЛЬНЫЙ рендерер жителя (своей сущности у воина нет - это обычный
 * Villager со своей профессией):
 * <ul>
 *   <li>слой брони - у ванильного VillagerRenderer его нет вообще;</li>
 *   <li>подмена CrossedArmsItemLayer тремя вариантами позы (подробности - в
 *       WarriorVillagerHeldItemLayer): у воина оружие в руке сбоку (тюнинг под конкретное оружие),
 *       у лесоруба топор перед грудью в 3D-ориентации инструмента (GROUND для полноценного 3D-тула
 *       вроде топора выглядит почти ребром - плашмя нормально смотрится только для плоской еды), у
 *       всех остальных - как в ванили (GROUND).</li>
 * </ul>
 * Оба слоя безобидны для жителей без брони/особой позы: пустые слоты брони ничего не рисуют, а
 * предмет в руке у обычного жителя рисуется ровно как в ванили.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public class WarriorVillagerClientEvents {

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        LivingEntityRenderer<Villager, VillagerModel<Villager>> renderer = event.getRenderer(EntityType.VILLAGER);
        if (renderer == null) return;

        renderer.addLayer(new WarriorVillagerArmorLayer(renderer, event.getEntityModels()));
        replaceCrossedArmsLayer(renderer, event);
    }

    /**
     * Меняем ванильный слой на свой на месте, а не добавляем свой сверху - иначе предмет
     * рисовался бы дважды. removeLayer в NeoForge нет, поэтому список берём аксессором
     * (см. LivingEntityRendererAccessor).
     */
    private static void replaceCrossedArmsLayer(LivingEntityRenderer<Villager, VillagerModel<Villager>> renderer,
                                                EntityRenderersEvent.AddLayers event) {
        List<RenderLayer<?, ?>> layers = ((LivingEntityRendererAccessor) renderer).veridia$getLayers();
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i) instanceof CrossedArmsItemLayer) {
                layers.set(i, new WarriorVillagerHeldItemLayer(renderer, event.getContext().getItemInHandRenderer()));
                return;
            }
        }
    }
}
