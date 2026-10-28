package net.gera.veridia.common.mana.veridite;

import net.gera.veridia.common.enchantment.ModEnchantments;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.mana.ManaHandler;
import net.gera.veridia.common.mana.ManaProgression;
import net.gera.veridia.common.mana.Perk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class VeriditeLaserAbility {
    static final String COOLDOWN_UNTIL_KEY = "veridia_veridite_laser_cooldown_until";
    static final String CHARGE_UNTIL_KEY = "veridia_veridite_laser_charge_until";
    static final String FIRING_UNTIL_KEY = "veridia_veridite_laser_firing_until";

    static final int COOLDOWN_TICKS = 20 * 60;
    static final int CHARGE_DURATION_TICKS = 15;
    static final int FIRE_DURATION_TICKS = 30;
    static final int MANA_COST = 100;

    private static final int MAX_SOLID_BLOCKS_ABOVE = 15;
    private static final long NIGHT_START_TICK = 13000L;
    private static final long DAY_LENGTH_TICKS = 24000L;

    public static boolean tryActivate(ServerPlayer player) {
        return tryActivate(player, ItemStack.EMPTY);
    }

    public static boolean tryActivate(ServerPlayer player, ItemStack activatingStack) {
        if (!hasFullVeriditeArmor(player)) return false;
        if (!ManaProgression.hasPerk(player, Perk.VERIDITE_LASER)) return false;
        if (isBlockedByCeiling(player)) return false;
        if (isNight(player)) return false;

        long now = player.level().getGameTime();
        long cooldownUntil = player.getPersistentData().getLong(COOLDOWN_UNTIL_KEY);
        if (now < cooldownUntil) return false;

        if (!ManaHandler.consumeMana(player, ModEnchantments.reduceManaCost(player, activatingStack, MANA_COST))) return false;

        player.getPersistentData().putLong(COOLDOWN_UNTIL_KEY, now + COOLDOWN_TICKS);
        player.getPersistentData().putLong(CHARGE_UNTIL_KEY, now + CHARGE_DURATION_TICKS);
        player.getCooldowns().addCooldown(ModItems.BLINDING_PRISM.get(), COOLDOWN_TICKS);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    public static void clearCooldown(ServerPlayer player) {
        player.getPersistentData().remove(COOLDOWN_UNTIL_KEY);
        player.getCooldowns().removeCooldown(ModItems.BLINDING_PRISM.get());
    }

    private static boolean isBlockedByCeiling(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                player.getBlockX(), player.getBlockY() + 1, player.getBlockZ());

        int solidBlocks = 0;
        int maxY = level.getMaxBuildHeight();
        while (pos.getY() < maxY) {
            if (!level.getBlockState(pos).isAir()) {
                solidBlocks++;
                if (solidBlocks >= MAX_SOLID_BLOCKS_ABOVE) {
                    return true;
                }
            }
            pos.move(Direction.UP);
        }
        return false;
    }

    private static boolean isNight(ServerPlayer player) {
        long timeOfDay = player.level().getDayTime() % DAY_LENGTH_TICKS;
        return timeOfDay >= NIGHT_START_TICK;
    }

    private static boolean hasFullVeriditeArmor(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        return helmet.is(ModItems.VERIDITE_HELMET.get())
                && chest.is(ModItems.VERIDITE_CHESTPLATE.get())
                && legs.is(ModItems.VERIDITE_LEGGINGS.get())
                && boots.is(ModItems.VERIDITE_BOOTS.get());
    }
}