package net.gera.veridia.common.compat.curios.network;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;

@EventBusSubscriber(modid = Veridia.MOD_ID)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerCurioItem(event, ModItems.EYE_OF_SAURON.get());
        registerCurioItem(event, ModItems.EYE_OF_SAURON_2.get());
        registerCurioItem(event, ModItems.EYE_OF_SAURON_3.get());
        registerCurioItem(event, ModItems.EYE_OF_SAURON_4.get());
        registerCurioItem(event, ModItems.EYE_OF_SAURON_5.get());
        registerCurioItem(event, ModItems.AQUATIC_AMULET.get());
        registerCurioItem(event, ModItems.AQUATIC_AMULET_2.get());
        registerCurioItem(event, ModItems.AQUATIC_AMULET_3.get());
        registerCurioItem(event, ModItems.AQUATIC_AMULET_4.get());
        registerCurioItem(event, ModItems.AQUATIC_AMULET_5.get());
        registerCurioItem(event, ModItems.KNOCKBACK_RING.get());
        registerCurioItem(event, ModItems.KNOCKBACK_RING_2.get());
        registerCurioItem(event, ModItems.KNOCKBACK_RING_3.get());
        registerCurioItem(event, ModItems.KNOCKBACK_RING_4.get());
        registerCurioItem(event, ModItems.KNOCKBACK_RING_5.get());
        registerCurioItem(event, ModItems.SLOW_KILLER.get());
        registerCurioItem(event, ModItems.SLOW_KILLER_2.get());
        registerCurioItem(event, ModItems.SLOW_KILLER_3.get());
        registerCurioItem(event, ModItems.SLOW_KILLER_4.get());
        registerCurioItem(event, ModItems.SLOW_KILLER_5.get());
        registerCurioItem(event, ModItems.INFERNAL_STAFF.get());
        registerCurioItem(event, ModItems.INFERNAL_STAFF_2.get());
        registerCurioItem(event, ModItems.INFERNAL_STAFF_3.get());
        registerCurioItem(event, ModItems.INFERNAL_STAFF_4.get());
        registerCurioItem(event, ModItems.INFERNAL_STAFF_5.get());
        registerCurioItem(event, ModItems.BLINDING_PRISM.get());
    }

    private static void registerCurioItem(RegisterCapabilitiesEvent event, Item item) {
        event.registerItem(
                CuriosCapability.ITEM,
                (stack, context) -> new ICurio() {
                    @Override
                    public ItemStack getStack() {
                        return stack;
                    }
                },
                item
        );
    }
}