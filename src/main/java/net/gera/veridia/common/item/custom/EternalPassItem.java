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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
@SuppressWarnings("all")
public class EternalPassItem extends Item {

    public static final Set<BlockPos> ETERNAL_PORTALS = new HashSet<>();

    public EternalPassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (level.isClientSide || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();

        if (level.getBlockState(clickedPos).is(Blocks.OBSIDIAN)) {
            BlockPos targetPos = clickedPos.relative(context.getClickedFace());

            if (level.isEmptyBlock(targetPos)) {
                List<BlockPos> portalBlocks = PortalHelper.tryCreatePortal(level, targetPos);

                if (portalBlocks != null && !portalBlocks.isEmpty()) {
                    ServerLevel serverLevel = (ServerLevel) level;

                    serverLevel.playSound(null, targetPos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
                    for (BlockPos pos : portalBlocks) {
                        serverLevel.sendParticles(
                                ParticleTypes.SOUL_FIRE_FLAME,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                20, 0.3, 0.3, 0.3, 0.05
                        );
                    }

                    if (player != null && !player.getAbilities().instabuild) {
                        ItemStack itemStack = context.getItemInHand();
                        itemStack.shrink(1);
                    }

                    ETERNAL_PORTALS.addAll(portalBlocks);

                    if (player != null) {
                        player.sendSystemMessage(
                                Component.literal("Портал будет активен вечно")
                                        .withStyle(ChatFormatting.RED)
                                        .withStyle(ChatFormatting.BOLD)
                        );
                    }
                } else {
                    if (player != null) {
                        player.sendSystemMessage(
                                Component.literal("!!!CRITICAL ERROR!!!")
                                        .withStyle(ChatFormatting.DARK_RED)
                                        .withStyle(ChatFormatting.BOLD)
                        );
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}