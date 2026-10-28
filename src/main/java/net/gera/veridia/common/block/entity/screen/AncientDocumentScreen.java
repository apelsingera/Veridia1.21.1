package net.gera.veridia.common.block.entity.screen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.compat.jei.JeiRefreshHook;
import net.gera.veridia.common.item.ModItems;
import net.gera.veridia.common.recipe.knowledge.RecipeKnowledge.KnowledgeTier;
import net.gera.veridia.common.recipe.mythicaltable.SlotIngredient;
import net.gera.veridia.common.recipe.runicpedestal.RunicPedestalRingRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AncientDocumentScreen extends AbstractContainerScreen<AncientDocumentMenu> {
    private static final ResourceLocation BACKGROUND_TILE = ResourceLocation.fromNamespaceAndPath(
            Veridia.MOD_ID, "textures/gui/ancient_document/background_tile.png");
    private static final ResourceLocation BACKGROUND_TILE_2 = ResourceLocation.fromNamespaceAndPath(
            Veridia.MOD_ID, "textures/gui/ancient_document/background_tile_2.png");
    private static final ResourceLocation BACKGROUND_TILE_3 = ResourceLocation.fromNamespaceAndPath(
            Veridia.MOD_ID, "textures/gui/ancient_document/background_tile_3.png");
    private static final ResourceLocation NODE_FRAME = ResourceLocation.fromNamespaceAndPath(
            Veridia.MOD_ID, "textures/gui/ancient_document/node_frame.png");
    private static final ResourceLocation BORDER_H = ResourceLocation.fromNamespaceAndPath(
            Veridia.MOD_ID, "textures/gui/ancient_document/border_h.png");
    private static final ResourceLocation BORDER_V = ResourceLocation.fromNamespaceAndPath(
            Veridia.MOD_ID, "textures/gui/ancient_document/border_v.png");
    private static final int BORDER_THICKNESS = 3;
    private static final int BORDER_H_TILE_W = 4;
    private static final int BORDER_V_TILE_H = 4;
    private static final int BG_TILE_SIZE = 32;
    private static final int NODE_SIZE = 28;
    private static final int NODE_HALF = NODE_SIZE / 2;
    private static final int NODE_STATE_LOCKED = 0;
    private static final int NODE_STATE_UNLOCKED = 1;
    private static final int DESIRED_COL_SPACING = 64;
    private static final int MIN_COL_SPACING = 34;
    private static final int ROW_SPACING_Y = 42;
    private static final int GRID_LEFT_X = 24;
    private static final int GRID_TOP_Y = 30;
    private static final int BOTTOM_MARGIN = 18;
    private static final int TITLE_X = 10;
    private static final int TITLE_Y = 8;
    private static final int COLOR_LINE_ACTIVE = 0xFFC9A227;
    private static final int COLOR_LINE_INACTIVE = 0x80553318;
    private static final int COLOR_NODE_DIM = 0x90000000;
    private static final int COLOR_UNLOCKED_BADGE = 0xFF4CD964;

    private int colSpacing = DESIRED_COL_SPACING;

    public AncientDocumentScreen(AncientDocumentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.titleLabelX = TITLE_X;
        this.titleLabelY = TITLE_Y;
    }

    @Override
    protected void init() {
        int totalCols = menu.totalColumnCount();
        int desiredWidth = GRID_LEFT_X * 2 + (totalCols - 1) * DESIRED_COL_SPACING + NODE_SIZE;
        this.imageWidth = Math.min(desiredWidth, this.width);

        int availableForSpacing = this.imageWidth - GRID_LEFT_X * 2 - NODE_SIZE;
        this.colSpacing = totalCols > 1
                ? Math.max(MIN_COL_SPACING, Math.min(DESIRED_COL_SPACING, availableForSpacing / (totalCols - 1)))
                : DESIRED_COL_SPACING;

        int desiredHeight = GRID_TOP_Y + (maxRows() - 1) * ROW_SPACING_Y + NODE_SIZE + BOTTOM_MARGIN;
        this.imageHeight = Math.min(desiredHeight, this.height);

        super.init();
    }

    @Override
    public void removed() {
        super.removed();
        JeiRefreshHook.onRunicRingKnowledgeChanged.run();
    }

    private int maxRows() {
        int max = 1;
        for (int col = 0; col < menu.treeCount(); col++) {
            max = Math.max(max, menu.tierCount(col));
        }
        return max;
    }

    private int nodeX(int col) {
        return GRID_LEFT_X + NODE_HALF + col * colSpacing;
    }

    private int nodeY(int row) {
        return GRID_TOP_Y + NODE_HALF + row * ROW_SPACING_Y;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        renderTiledBackground(guiGraphics, x, y);

        for (int col = 0; col < menu.treeCount(); col++) {
            renderTreeConnections(guiGraphics, x, y, col);
            for (int row = 0; row < menu.tierCount(col); row++) {
                renderNode(guiGraphics, x, y, col, row);
            }
        }

        for (int col = menu.treeCount(); col < menu.totalColumnCount(); col++) {
            renderPlaceholderNode(guiGraphics, x, y, col);
        }

        renderBorder(guiGraphics, x, y);
    }

    private void renderBorder(GuiGraphics guiGraphics, int x, int y) {
        for (int tx = 0; tx < imageWidth; tx += BORDER_H_TILE_W) {
            int w = Math.min(BORDER_H_TILE_W, imageWidth - tx);
            guiGraphics.blit(BORDER_H, x + tx, y, 0, 0, w, BORDER_THICKNESS, BORDER_H_TILE_W, BORDER_THICKNESS);
            guiGraphics.blit(BORDER_H, x + tx, y + imageHeight - BORDER_THICKNESS, 0, 0, w, BORDER_THICKNESS, BORDER_H_TILE_W, BORDER_THICKNESS);
        }
        for (int ty = 0; ty < imageHeight; ty += BORDER_V_TILE_H) {
            int h = Math.min(BORDER_V_TILE_H, imageHeight - ty);
            guiGraphics.blit(BORDER_V, x, y + ty, 0, 0, BORDER_THICKNESS, h, BORDER_THICKNESS, BORDER_V_TILE_H);
            guiGraphics.blit(BORDER_V, x + imageWidth - BORDER_THICKNESS, y + ty, 0, 0, BORDER_THICKNESS, h, BORDER_THICKNESS, BORDER_V_TILE_H);
        }
    }

    private void renderTiledBackground(GuiGraphics guiGraphics, int x, int y) {
        int col = 0;
        for (int ty = 0; ty < imageHeight; ty += BG_TILE_SIZE, col++) {
            int h = Math.min(BG_TILE_SIZE, imageHeight - ty);
            int row = 0;
            for (int tx = 0; tx < imageWidth; tx += BG_TILE_SIZE, row++) {
                int w = Math.min(BG_TILE_SIZE, imageWidth - tx);
                guiGraphics.blit(tileTextureFor(row, col), x + tx, y + ty,
                        0, 0, w, h, BG_TILE_SIZE, BG_TILE_SIZE);
            }
        }
    }

    private ResourceLocation tileTextureFor(int tileX, int tileY) {
        int hash = (tileX * 92821) ^ (tileY * 68917);
        int bucket = Math.floorMod(hash, 100);
        if (bucket < 84) return BACKGROUND_TILE;
        if (bucket < 92) return BACKGROUND_TILE_2;
        return BACKGROUND_TILE_3;
    }
    private void renderTreeConnections(GuiGraphics guiGraphics, int x, int y, int col) {
        int lineX = x + nodeX(col) - 1;
        for (int row = 0; row < menu.tierCount(col) - 1; row++) {
            int fromY = y + nodeY(row) + NODE_HALF;
            int toY = y + nodeY(row + 1) - NODE_HALF;
            int color = menu.isUnlocked(col, row) ? COLOR_LINE_ACTIVE : COLOR_LINE_INACTIVE;
            guiGraphics.fill(lineX, fromY, lineX + 3, toY, color);
        }
    }

    private void renderNode(GuiGraphics guiGraphics, int x, int y, int col, int row) {
        boolean unlocked = menu.isUnlocked(col, row);
        boolean available = menu.isAvailable(col, row);
        KnowledgeTier tier = menu.tierAt(col, row);

        int nodeX = x + nodeX(col);
        int nodeY = y + nodeY(row);

        int state = (unlocked || available) ? NODE_STATE_UNLOCKED : NODE_STATE_LOCKED;
        guiGraphics.blit(NODE_FRAME, nodeX - NODE_HALF, nodeY - NODE_HALF, state * NODE_SIZE, 0,
                NODE_SIZE, NODE_SIZE, NODE_SIZE * 2, NODE_SIZE);

        if (!available) {
            guiGraphics.fill(nodeX - NODE_HALF, nodeY - NODE_HALF, nodeX + NODE_HALF, nodeY + NODE_HALF, COLOR_NODE_DIM);
            return;
        }

        guiGraphics.renderItem(new ItemStack(tier.resultItem()), nodeX - 8, nodeY - 8);
        if (unlocked) {
            guiGraphics.fill(nodeX + NODE_HALF - 7, nodeY - NODE_HALF, nodeX + NODE_HALF, nodeY - NODE_HALF + 7, COLOR_UNLOCKED_BADGE);
        }
    }

    private void renderPlaceholderNode(GuiGraphics guiGraphics, int x, int y, int col) {
        int nodeX = x + nodeX(col);
        int nodeY = y + nodeY(0);

        guiGraphics.blit(NODE_FRAME, nodeX - NODE_HALF, nodeY - NODE_HALF, NODE_STATE_LOCKED * NODE_SIZE, 0,
                NODE_SIZE, NODE_SIZE, NODE_SIZE * 2, NODE_SIZE);
        guiGraphics.renderItem(new ItemStack(Items.BARRIER), nodeX - 8, nodeY - 8);
        guiGraphics.fill(nodeX - NODE_HALF, nodeY - NODE_HALF, nodeX + NODE_HALF, nodeY + NODE_HALF, COLOR_NODE_DIM);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xEEEEEE, false);
    }

    @Override
    public void render( GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        for (int col = 0; col < menu.treeCount(); col++) {
            for (int row = 0; row < menu.tierCount(col); row++) {
                if (isHoveringNode(mouseX, mouseY, col, row)) {
                    guiGraphics.renderComponentTooltip(this.font, buildNodeTooltip(col, row), mouseX, mouseY);
                }
            }
        }

        for (int col = menu.treeCount(); col < menu.totalColumnCount(); col++) {
            if (isHoveringNode(mouseX, mouseY, col, 0)) {
                guiGraphics.renderComponentTooltip(this.font, List.of(
                        Component.translatable("gui.veridia.knowledge_category_placeholder").withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
            }
        }
    }
    private List<Component> buildNodeTooltip(int col, int row) {
        KnowledgeTier tier = menu.tierAt(col, row);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(tier.resultItem().getDescriptionId()));

        if (!menu.isAvailable(col, row)) {
            lines.add(Component.translatable("gui.veridia.knowledge_locked_label").withStyle(ChatFormatting.RED));
            return lines;
        }

        if (menu.isUnlocked(col, row)) {
            lines.add(Component.translatable("gui.veridia.knowledge_unlocked_label").withStyle(ChatFormatting.GREEN));
        } else {
            assert this.minecraft != null;
            Player player = this.minecraft.player;
            int have = player == null ? 0 : countNeuronClump(player);
            int required = tier.requiredNeuronClumps();
            lines.add(Component.translatable("gui.veridia.knowledge_progress", have, required)
                    .withStyle(have >= required ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.veridia.knowledge_click_to_open").withStyle(ChatFormatting.YELLOW));
        }

        assert this.minecraft != null;
        Optional<RunicPedestalRingRecipe> recipe = this.minecraft.level != null
                ? RunicPedestalRingRecipe.findForKnowledge(this.minecraft.level, tier.id())
                : Optional.empty();

        lines.add(Component.translatable("gui.veridia.knowledge_pedestal_recipe").withStyle(ChatFormatting.GRAY));
        if (recipe.isPresent()) {
            for (SlotIngredient ingredient : recipe.get().materials()) {
                ItemStack[] items = ingredient.ingredient().getItems();
                if (items.length > 0) {
                    lines.add(Component.literal("- x" + ingredient.count() + " ")
                            .append(Component.translatable(items[0].getDescriptionId()))
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        } else {
            lines.add(Component.translatable("gui.veridia.knowledge_pedestal_recipe_missing").withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    private int countNeuronClump(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.NEURON_CLUMP.get())) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int col = 0; col < menu.treeCount(); col++) {
                for (int row = 0; row < menu.tierCount(col); row++) {
                    if (menu.isAvailable(col, row) && !menu.isUnlocked(col, row) && isHoveringNode(mouseX, mouseY, col, row)) {
                        assert (this.minecraft != null ? this.minecraft.gameMode : null) != null;
                        this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AncientDocumentMenu.buttonId(col, row));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHoveringNode(double mouseX, double mouseY, int col, int row) {
        int x = leftPos + nodeX(col);
        int y = topPos + nodeY(row);
        return mouseX >= x - NODE_HALF && mouseX < x + NODE_HALF && mouseY >= y - NODE_HALF && mouseY < y + NODE_HALF;
    }
}