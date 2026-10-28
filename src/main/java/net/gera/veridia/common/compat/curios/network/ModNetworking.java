package net.gera.veridia.common.compat.curios.network;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.compat.curios.IActivatableCurio;
import net.gera.veridia.common.mana.veridite.InfernalStaffBeamPayload;
import net.gera.veridia.common.mana.veridite.LaserBeamPayload;
import net.gera.veridia.common.mana.veridite.PlayLaserShakePayload;
import net.gera.veridia.common.mana.veridite.client.EyeOfSauronBeamRenderer;
import net.gera.veridia.common.mana.veridite.client.InfernalStaffBeamRenderer;
import net.gera.veridia.common.mana.veridite.client.VeriditeLaserClientEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                ActivateCurioPayload.TYPE,
                ActivateCurioPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        context.enqueueWork(() -> handleActivate(player));
                    }
                }
        );
        registrar.playToClient(
                PlayLaserShakePayload.TYPE,
                PlayLaserShakePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> VeriditeLaserClientEffects.startShake(payload.durationTicks()))
        );
        registrar.playToClient(
                LaserBeamPayload.TYPE,
                LaserBeamPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> EyeOfSauronBeamRenderer.onBeamPayload(
                        payload.shooterId(), payload.endX(), payload.endY(), payload.endZ(), payload.tier()))
        );
        registrar.playToClient(
                InfernalStaffBeamPayload.TYPE,
                InfernalStaffBeamPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> InfernalStaffBeamRenderer.onBeamPayload(
                        payload.playerId(), payload.beamCount(), payload.beamLength(),
                        payload.radiansPerTick(), payload.tier()))
        );
    }

    private static void handleActivate(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            ICurioStacksHandler stacksHandler = handler.getCurios().get("gem");
            if (stacksHandler == null) return;

            IDynamicStackHandler stackHandler = stacksHandler.getStacks();
            for (int i = 0; i < stackHandler.getSlots(); i++) {
                ItemStack stack = stackHandler.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof IActivatableCurio activatable) {
                    activatable.onActivate(player, stack);
                }
            }
        });
    }
}