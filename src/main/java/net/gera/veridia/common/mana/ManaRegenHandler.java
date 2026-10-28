package net.gera.veridia.common.mana;

import net.gera.veridia.Veridia;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class ManaRegenHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ManaHandler.isUnlocked(player)) return;
        int currentMana = ManaHandler.getMana(player);
        int maxMana = ManaProgression.getMaxMana(player);
        if (currentMana > maxMana) {
            player.setData(ModAttachments.MANA, maxMana);
            currentMana = maxMana;
        }
        if (currentMana >= maxMana) {
            player.setData(ModAttachments.MANA_REGEN_ACCUMULATOR, 0.0F);
            return;
        }

        float accumulator = player.getData(ModAttachments.MANA_REGEN_ACCUMULATOR)
                + (float) ManaProgression.getRegenPerTick(player);

        if (accumulator >= 1.0F) {
            int gained = (int) accumulator;
            accumulator -= gained;
            ManaHandler.addMana(player, gained);
        }

        player.setData(ModAttachments.MANA_REGEN_ACCUMULATOR, accumulator);
    }
}
