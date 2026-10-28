package net.gera.veridia.common.compat.curios;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.gera.veridia.Veridia;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * Предмет для открытия слота "gem".
 * До использования виден "gem_locked" (текстура замка, ничего вставить нельзя).
 * После использования: "gem_locked" исчезает (-1), вместо него появляется
 * настоящий "gem" (+1) — визуально это смена текстуры одного и того же слота.
 */
public class GemSlotUnlockItem extends Item {

    private static final String GEM_SLOT_ID = "gem";
    private static final String GEM_LOCKED_SLOT_ID = "gem_locked";

    private static final ResourceLocation REMOVE_LOCKED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "gem_locked_remove");
    private static final ResourceLocation ADD_GEM_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "gem_slot_unlock");

    public GemSlotUnlockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        boolean unlocked = CuriosApi.getCuriosInventory(serverPlayer)
                .map(this::unlockSlotIfNeeded)
                .orElse(false);

        if (!unlocked) {
            return InteractionResultHolder.fail(stack);
        }

        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResultHolder.success(stack);
    }

    private boolean unlockSlotIfNeeded(ICuriosItemHandler handler) {
        ICurioStacksHandler gemHandler = handler.getCurios().get(GEM_SLOT_ID);
        boolean alreadyUnlocked = gemHandler != null && gemHandler.getStacks().getSlots() > 0;

        if (alreadyUnlocked) {
            return false;
        }

        AttributeModifier removeLocked = new AttributeModifier(
                REMOVE_LOCKED_MODIFIER_ID, -1.0D, AttributeModifier.Operation.ADD_VALUE
        );
        AttributeModifier addGem = new AttributeModifier(
                ADD_GEM_MODIFIER_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE
        );

        Multimap<String, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(GEM_LOCKED_SLOT_ID, removeLocked);
        modifiers.put(GEM_SLOT_ID, addGem);

        handler.addPermanentSlotModifiers(modifiers);
        return true;
    }
}