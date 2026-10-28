package net.gera.veridia.common.compat.curios.network;

import com.mojang.blaze3d.platform.InputConstants;
import net.gera.veridia.Veridia;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Единая клавиша активации всех гемов в слоте "gem" куриоса - включая лазер веридитовой
 * брони (BlindingPrismItem), который раньше активировался отдельной клавишей (H).
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public class ModKeyBindings {

    public static final KeyMapping ACTIVATE_CURIO = new KeyMapping(
            "key.veridia.activate_curio",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.veridia"
    );

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE_CURIO);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ACTIVATE_CURIO.consumeClick()) {
            PacketDistributor.sendToServer(new ActivateCurioPayload());
        }
    }
}