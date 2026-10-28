package net.gera.veridia.common.item.custom.gems.lvl1;

import net.gera.veridia.Veridia;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class AquaticAmuletHandler {

    public static final String PENDING_KEY = "veridia_aquatic_amulet_attack_speed_end";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        long expireAt = player.getPersistentData().getLong(PENDING_KEY);
        if (expireAt == 0L) return;

        if (player.level().getGameTime() >= expireAt) {
            AttributeInstance attackSpeedAttribute = player.getAttribute(Attributes.ATTACK_SPEED);
            if (attackSpeedAttribute != null) {
                attackSpeedAttribute.removeModifier(AquaticAmuletItem.ATTACK_SPEED_MODIFIER_ID);
            }
            player.getPersistentData().remove(PENDING_KEY);
        }
    }
}