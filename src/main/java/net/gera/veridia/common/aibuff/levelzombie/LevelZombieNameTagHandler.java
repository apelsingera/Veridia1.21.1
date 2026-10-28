package net.gera.veridia.common.aibuff.levelzombie;

import net.gera.veridia.Veridia;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

/**
 * Ограничивает видимость наших nametag'ов ("Зомби <римская цифра>" - см.
 * LevelZombieHandler.applyLevel) кубом (2*HALF_EXTENT+1) блоков вокруг камеры игрока - без этого
 * ванильный рендер держит nametag видимым на всю дальность прорисовки сущностей.
 *
 * ВАЖНО: раньше здесь стояла проверка zombie.getPersistentData().contains(LEVEL_KEY) - но
 * getPersistentData() НЕ синкается на клиент (только пишется/читается с диска на СЕРВЕРЕ, см.
 * javadoc IEntityExtension.getPersistentData - "written and read from disc", ни слова про сеть).
 * На клиенте эта проверка была ВСЕГДА false, метод выходил раньше проверки дистанции, и
 * ограничение по факту НИКОГДА не применялось - ник был виден на всю обычную ванильную дальность,
 * а не в заданном радиусе. Правильный клиентский признак "это наш зомби" - hasCustomName():
 * customName/customNameVisible - это SynchedEntityData, штатно синкается на клиент, а имя на
 * EntityType.ZOMBIE выставляет только наш код (см. LevelZombieHandler.applyLevel).
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public class LevelZombieNameTagHandler {
    // Куб 7x7x7 вокруг камеры (по 3.5 блока в каждую сторону от игрока).
    private static final double HALF_EXTENT = 3.5;

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!zombie.hasCustomName()) return;

        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) return;

        if (Math.abs(camera.getX() - zombie.getX()) > HALF_EXTENT
                || Math.abs(camera.getY() - zombie.getY()) > HALF_EXTENT
                || Math.abs(camera.getZ() - zombie.getZ()) > HALF_EXTENT) {
            event.setCanRender(TriState.FALSE);
        }
    }
}
