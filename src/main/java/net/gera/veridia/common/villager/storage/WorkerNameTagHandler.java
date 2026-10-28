package net.gera.veridia.common.villager.storage;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public class WorkerNameTagHandler {
    private static final double HALF_EXTENT = 5.0;

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (!villager.hasCustomName()) return;
        if (!isWorkerProfession(villager.getVillagerData().getProfession())) return;

        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) return;

        if (Math.abs(camera.getX() - villager.getX()) > HALF_EXTENT
                || Math.abs(camera.getY() - villager.getY()) > HALF_EXTENT
                || Math.abs(camera.getZ() - villager.getZ()) > HALF_EXTENT) {
            event.setCanRender(TriState.FALSE);
        }
    }

    private static boolean isWorkerProfession(VillagerProfession profession) {
        return profession == ModVillagers.LUMBERJACK.value()
                || profession == ModVillagers.NAILER.value()
                || profession == VillagerProfession.FARMER;
    }
}
