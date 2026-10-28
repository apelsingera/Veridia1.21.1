package net.gera.veridia.common.villager.network;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.villager.ModVillagers;
import net.gera.veridia.common.villager.VillagerProfessionMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class VillagerNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                OpenVillagerProfessionPayload.TYPE,
                OpenVillagerProfessionPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        context.enqueueWork(() -> handleOpen(player, payload.villagerEntityId()));
                    }
                }
        );
    }

    private static void handleOpen(ServerPlayer player, int villagerEntityId) {
        Entity entity = player.level().getEntity(villagerEntityId);
        if (!(entity instanceof Villager villager)) {
            return;
        }

        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (!ModVillagers.isEligibleForProfessionScreen(profession)) {
            return;
        }

        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, p) -> new VillagerProfessionMenu(containerId, inventory, profession, villager),
                        Component.translatable("menu.veridia.villager_profession")),
                buf -> {
                    buf.writeResourceLocation(BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession));
                    buf.writeVarInt(villager.getId());
                }
        );
    }
}
