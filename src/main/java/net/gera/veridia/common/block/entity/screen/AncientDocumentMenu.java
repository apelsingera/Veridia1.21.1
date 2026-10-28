package net.gera.veridia.common.block.entity.screen;

import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.recipe.knowledge.RecipeKnowledge;
import net.gera.veridia.common.recipe.knowledge.RecipeKnowledge.KnowledgeTier;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.List;

public class AncientDocumentMenu extends AbstractContainerMenu {
    private static final List<List<KnowledgeTier>> TREES = RecipeKnowledge.TREES;
    private static final int PENTAGRAM_POINTS = 5;
    private static final double PENTAGRAM_RADIUS = 1.6;
    public static final int PLACEHOLDER_TREES = 4;

    private final Player player;

    public AncientDocumentMenu(int containerId, Inventory inventory) {
        super(MenuTypes.ANCIENT_DOCUMENT_MENU.get(), containerId);
        this.player = inventory.player;
    }

    public int treeCount() {
        return TREES.size();
    }

    public int totalColumnCount() {
        return TREES.size() + PLACEHOLDER_TREES;
    }

    public int tierCount(int col) {
        return TREES.get(col).size();
    }

    public KnowledgeTier tierAt(int col, int row) {
        return TREES.get(col).get(row);
    }

    public boolean isUnlocked(int col, int row) {
        return TREES.get(col).get(row).isUnlocked(player);
    }

    public boolean isAvailable(int col, int row) {
        return row == 0 || isUnlocked(col, row) || TREES.get(col).get(row - 1).isUnlocked(player);
    }

    public static int buttonId(int col, int row) {
        return col * 100 + row;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int col = id / 100;
        int row = id % 100;
        if (col < 0 || col >= TREES.size() || row < 0 || row >= TREES.get(col).size()
                || !(player instanceof ServerPlayer serverPlayer)) {
            return super.clickMenuButton(player, id);
        }
        return tryUnlock(serverPlayer, col, row);
    }

    private boolean tryUnlock(ServerPlayer player, int col, int row) {
        KnowledgeTier tier = TREES.get(col).get(row);
        if (tier.isUnlocked(player)) {
            player.displayClientMessage(Component.translatable("message.veridia.knowledge_already_unlocked"), true);
            return false;
        }

        if (row > 0 && !TREES.get(col).get(row - 1).isUnlocked(player)) {
            player.displayClientMessage(Component.translatable("message.veridia.knowledge_requires_previous"), true);
            return false;
        }

        int required = tier.requiredNeuronClumps();
        Item material = ModItems.NEURON_CLUMP.get();
        int have = countItem(player, material);
        if (have < required) {
            player.displayClientMessage(Component.translatable("message.veridia.knowledge_missing_material", required), true);
            return false;
        }

        consumeItem(player, material, required);
        tier.unlock(player);

        spawnUnlockEffect(player);
        player.displayClientMessage(Component.translatable("message.veridia.knowledge_unlocked",
                Component.translatable(tier.resultItem().getDescriptionId())), false);
        return true;
    }

    private static int countItem(Player player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consumeItem(Player player, Item item, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) break;
            if (stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    private void spawnUnlockEffect(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        double centerX = player.getX();
        double centerY = player.getY() + 0.05;
        double centerZ = player.getZ();

        double[] px = new double[PENTAGRAM_POINTS];
        double[] pz = new double[PENTAGRAM_POINTS];
        for (int i = 0; i < PENTAGRAM_POINTS; i++) {
            double angle = -Math.PI / 2 + (2 * Math.PI / PENTAGRAM_POINTS) * i;
            px[i] = centerX + Math.cos(angle) * PENTAGRAM_RADIUS;
            pz[i] = centerZ + Math.sin(angle) * PENTAGRAM_RADIUS;
        }

        DustParticleOptions whiteDust = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.2F);

        int circlePoints = 48;
        for (int i = 0; i < circlePoints; i++) {
            double angle = 2 * Math.PI * i / circlePoints;
            double x = centerX + Math.cos(angle) * PENTAGRAM_RADIUS;
            double z = centerZ + Math.sin(angle) * PENTAGRAM_RADIUS;
            serverLevel.sendParticles(whiteDust, x, centerY, z, 0, 0.0, 0.0, 0.0, 0.0);
        }

        int segments = 10;
        for (int i = 0; i < PENTAGRAM_POINTS; i++) {
            int next = (i + 2) % PENTAGRAM_POINTS;
            for (int s = 0; s <= segments; s++) {
                double t = (double) s / segments;
                double x = px[i] + (px[next] - px[i]) * t;
                double z = pz[i] + (pz[next] - pz[i]) * t;
                serverLevel.sendParticles(whiteDust, x, centerY, z, 0, 0.0, 0.0, 0.0, 0.0);
            }
        }

        serverLevel.sendParticles(ParticleTypes.END_ROD, centerX, centerY + 0.5, centerZ, 40, 0.4, 0.6, 0.4, 0.02);

        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 0.6F);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && player.isAlive() && !player.isRemoved();
    }
}