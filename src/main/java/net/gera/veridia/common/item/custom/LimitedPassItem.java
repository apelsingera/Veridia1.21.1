package net.gera.veridia.common.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LimitedPassItem extends Item {

    public static final List<TimedPortal> ACTIVE_PORTALS = new ArrayList<>();

    public LimitedPassItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (level.isClientSide || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }

        if (player != null && player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();

        if (level.getBlockState(clickedPos).is(Blocks.OBSIDIAN)) {
            BlockPos targetPos = clickedPos.relative(context.getClickedFace());

            if (level.isEmptyBlock(targetPos)) {
                List<BlockPos> portalBlocks = PortalHelper.tryCreatePortal(level, targetPos);

                if (portalBlocks != null && !portalBlocks.isEmpty()) {
                    ServerLevel serverLevel = (ServerLevel) level;
                    serverLevel.playSound(null, targetPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    for (BlockPos pos : portalBlocks) {
                        serverLevel.sendParticles(
                                ParticleTypes.FLAME,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                15, 0.25, 0.25, 0.25, 0.05
                        );
                    }

                    if (player != null && !player.getAbilities().instabuild) {
                        ItemStack itemStack = context.getItemInHand();
                        itemStack.shrink(1);
                    }

                    ACTIVE_PORTALS.add(new TimedPortal(portalBlocks, serverLevel, 600 * 20));

                    if (player != null) {
                        player.getCooldowns().addCooldown(this, 20 * 120);
                        player.sendSystemMessage(
                                Component.literal("Портал активирован на 10 минут!")
                                        .withStyle(ChatFormatting.RED)
                        );
                    }
                } else {
                    if (player != null) {
                        player.sendSystemMessage(
                                Component.literal("❌Ошибка")
                                        .withStyle(ChatFormatting.DARK_RED)
                        );
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    public static class TimedPortal {
        public List<BlockPos> blocks;
        public ServerLevel level;
        public int ticksLeft;

        public TimedPortal(List<BlockPos> blocks, ServerLevel level, int ticksLeft) {
            this.blocks = blocks;
            this.level = level;
            this.ticksLeft = ticksLeft;
        }
    }
}