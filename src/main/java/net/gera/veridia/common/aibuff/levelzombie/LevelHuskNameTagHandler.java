package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.Veridia;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Husk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/**
 * Полная копия LevelZombieNameTagHandler, но для Кадавра (Husk) - ограничивает видимость его
 * nametag'ов ("Кадавр <римская цифра>") кубом 7x7x7 вокруг камеры игрока. Признак "это наш
 * кадавр" - hasCustomName() (SynchedEntityData, штатно синкается на клиент) - НЕ
 * getPersistentData() (та не синкается вообще, см. javadoc IEntityExtension.getPersistentData).
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public class LevelHuskNameTagHandler {
    private static final double HALF_EXTENT = 3.5;

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Husk husk)) return;
        if (!husk.hasCustomName()) return;

        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) return;

        if (Math.abs(camera.getX() - husk.getX()) > HALF_EXTENT
                || Math.abs(camera.getY() - husk.getY()) > HALF_EXTENT
                || Math.abs(camera.getZ() - husk.getZ()) > HALF_EXTENT) {
            event.setCanRender(TriState.FALSE);
        }
    }
}
