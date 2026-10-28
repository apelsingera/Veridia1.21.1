package net.gera.veridia.common.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;

public class PortalEventsHandler {

    @SubscribeEvent
    public static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (!PortalHelper.IS_CUSTOM_IGNITION) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<LimitedPassItem.TimedPortal> iterator = LimitedPassItem.ACTIVE_PORTALS.iterator();

        while (iterator.hasNext()) {
            LimitedPassItem.TimedPortal portal = iterator.next();
            portal.ticksLeft--;

            if (portal.ticksLeft == 180 * 20) {
                portal.level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("Портал закроется через 3 минуты")
                                .withStyle(ChatFormatting.DARK_RED), false
                );
            }

            if (portal.ticksLeft <= 0) {
                for (BlockPos pos : portal.blocks) {
                    if (portal.level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
                        portal.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                }

                MinecraftServer server = portal.level.getServer();
                ServerLevel nether = server.getLevel(Level.NETHER);

                if (nether != null) {
                    for (ServerPlayer player : nether.players()) {
                        player.hurt(nether.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                    }
                }
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("Портал закрылся! Все игроки в Незере уничтожены")
                                .withStyle(ChatFormatting.DARK_RED), false
                );

                iterator.remove();
            }
        }
    }
}