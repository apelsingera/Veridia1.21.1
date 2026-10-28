package net.gera.veridia.common.item.custom.tools;

import net.gera.veridia.common.mana.ManaHandler;
import net.gera.veridia.common.mana.ManaTooltips;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ElectricSwordItem extends SwordItem {

    private static final int COOLDOWN_TICKS = 20 * 120;
    private static final float EXTRA_DAMAGE = 15.0F;
    private static final double DAMAGE_RADIUS = 3.0D;
    private static final int MANA_COST = 65;

    public ElectricSwordItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide()) {
            if (!(player instanceof ServerPlayer serverPlayer) || !ManaHandler.consumeMana(serverPlayer, MANA_COST)) {
                return InteractionResultHolder.fail(stack);
            }

            double reachDistance = 20.0D;
            Vec3 eyePosition = player.getEyePosition();
            Vec3 lookVector = player.getViewVector(1.0F);
            Vec3 traceEnd = eyePosition.add(lookVector.x * reachDistance, lookVector.y * reachDistance, lookVector.z * reachDistance);

            BlockHitResult hitResult = level.clip(new ClipContext(eyePosition, traceEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));

            BlockPos targetPos;
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                targetPos = hitResult.getBlockPos();
            } else {
                targetPos = BlockPos.containing(eyePosition.add(lookVector.x * 3, lookVector.y * 3, lookVector.z * 3));
            }

            Vec3 strikeVec = Vec3.atBottomCenterOf(targetPos);

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(strikeVec);
                bolt.setCause(serverPlayer);
                level.addFreshEntity(bolt);
            }

            AABB strikeArea = new AABB(targetPos).inflate(DAMAGE_RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, strikeArea,
                    entity -> entity.isAlive() && entity != player);

            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().lightningBolt(), EXTRA_DAMAGE);
            }

            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0F, 0.8F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        assert context.getPlayer() != null;
        InteractionResultHolder<ItemStack> result = this.use(context.getLevel(), context.getPlayer(), context.getHand());
        return result.getResult();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ManaTooltips.appendCooldownAndManaCost(tooltip, COOLDOWN_TICKS, MANA_COST);
    }
}