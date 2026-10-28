package net.gera.veridia.common.mana;

import net.gera.veridia.common.item.ModArmorMaterials;
import net.gera.veridia.common.item.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

class ManaArmorEffects {
    private static final EquipmentSlot[] ARMOR_SLOTS =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static final double VERIDITE_PENALTY_PER_PIECE = -5.0;
    private static final double NEWBIE_MANTLE_BONUS = 25.0;
    private static final double EXPERIENCED_MANTLE_BONUS = 35.0;

    static double getRegenPercentModifier(Player player) {
        return veriditePenalty(player) + mantleBonus(player);
    }

    private static double veriditePenalty(Player player) {
        int pieces = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorItem armorItem
                    && armorItem.getMaterial() == ModArmorMaterials.VERIDITE_ARMOR_MATERIAL) {
                pieces++;
            }
        }
        return pieces * VERIDITE_PENALTY_PER_PIECE;
    }

    private static double mantleBonus(Player player) {
        if (isFullMantleSet(player, ModItems.NEWBIE_MANTLE_HELMET.get(),
                ModItems.NEWBIE_MANTLE_CHESTPLATE.get(), ModItems.NEWBIE_MANTLE_BOOTS.get())) {
            return NEWBIE_MANTLE_BONUS;
        }
        if (isFullMantleSet(player, ModItems.EXPERIENCED_MANTLE_HELMET.get(),
                ModItems.EXPERIENCED_MANTLE_CHESTPLATE.get(), ModItems.EXPERIENCED_MANTLE_BOOTS.get())) {
            return EXPERIENCED_MANTLE_BONUS;
        }
        return 0.0;
    }

    private static boolean isFullMantleSet(Player player, Item helmet, Item chestplate, Item boots) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(helmet)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(chestplate)
                && player.getItemBySlot(EquipmentSlot.FEET).is(boots);
    }
}