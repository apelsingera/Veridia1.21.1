package net.gera.veridia.common.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.villager.nailer.NailerUpgrades;
import net.gera.veridia.common.villager.ModVillagers;
import net.gera.veridia.common.villager.VillagerProfessionMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Отдельное окно профессии жителя - открывается кнопкой из VillagerMerchantGuiOverlay поверх
 * MerchantScreen (через VillagerProfessionMenu/openMenu, обычный контейнерный переход, как у
 * AncientDocumentScreen - НЕ через ручной Minecraft.setScreen()). Рамка (villager_gui.png, в
 * стиле окна достижений) и замощённый под ней фон, свой для каждой профессии.
 * <p>
 * Над рамкой - вкладки, как в окне достижений (см. VillagerProfessionTabs): BRAIN (дерево улучшений) у
 * всех, у nailer'а со второго уровня ещё BUILD (пока пустое окно с тем же фоном и рамкой).
 * <p>
 * Дерево улучшений (если у профессии есть) рисуется/кликается/показывает тултип ОДИНАКОВО для
 * любой профессии через {@link ProfessionUpgradeTreeView} - profession-специфичное (иконки,
 * описания, сам список узлов) в LumberjackTreeView/FarmerTreeView + LumberjackUpgrades/
 * FarmerUpgrades. Новая профессия с деревом - смотри viewFor() ниже.
 */
public class VillagerProfessionScreen extends AbstractContainerScreen<VillagerProfessionMenu> {

    private static final ResourceLocation FRAME =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/villager_gui.png");
    // Сам файл villager_gui.png - 256x256, но реально непрозрачная (видимая) рамка внутри него
    // занимает только 252x140 (шапка y0-17, боковые бортики по 8-9px, подвал y131-139, дальше в
    // файле пусто) - проверено побайтово по альфа-каналу. TEXTURE_FILE_SIZE нужен только для
    // корректной нормировки UV при blit, сама рамка рисуется и мостится строго по imageWidth/Height.
    private static final int TEXTURE_FILE_SIZE = 256;
    private static final ResourceLocation FARMER_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/farmer/background_level1.png");
    private static final ResourceLocation WARRIOR_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/warrior/background_level1.png");
    private static final ResourceLocation LUMBERJACK_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/lumberjack/background_level1.png");
    private static final ResourceLocation NAILER_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/nailer/background_level1.png");
    // Как и само дерево (см. NailerTreeView.isHidden) - фон переключается на background_level2,
    // как только у жителя разблокирован copper_block (level3), а не по случайному шансу, как было
    // раньше (экспериментальная фича, убрана по просьбе).
    private static final ResourceLocation NAILER_BACKGROUND_LEVEL2 =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/nailer/background_level2.png");

    private static final int BG_TILE_SIZE = 16;

    // --- Дерево улучшений - общее для любой профессии (см. ProfessionUpgradeTreeView). ---
    private static final ResourceLocation BLANK_LOCKED =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/blank_locked.png");
    private static final ResourceLocation DONE =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/done.png");
    private static final ResourceLocation LINE_H =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/line_h.png");
    private static final ResourceLocation LINE_V =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/line_v.png");

    private static final int NODE_SIZE = 24;
    private static final int DONE_SIZE = 9;
    private static final int LINE_H_TILE_W = 4;
    private static final int LINE_H_TILE_H = 3;
    private static final int LINE_V_TILE_W = 3;
    private static final int LINE_V_TILE_H = 4;

    @Nullable
    private final ProfessionUpgradeTreeView treeView;
    // Какая вкладка открыта (см. VillagerProfessionTabs). Окно всегда открывается на первой - BRAIN, дереве улучшений.
    private VillagerProfessionTabs.Tab selectedTab = VillagerProfessionTabs.Tab.BRAIN;

    public VillagerProfessionScreen(VillagerProfessionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.treeView = viewFor(menu.profession());
        this.imageWidth = 252;
        this.imageHeight = 140;
    }

    private static ResourceLocation backgroundFor(VillagerProfession profession, @Nullable Villager villager) {
        if (profession == ModVillagers.WARRIOR.value()) {
            return WARRIOR_BACKGROUND;
        }
        if (profession == ModVillagers.LUMBERJACK.value()) {
            return LUMBERJACK_BACKGROUND;
        }
        if (profession == ModVillagers.NAILER.value()) {
            return villager != null && NailerUpgrades.isLevel2Unlocked(villager)
                    ? NAILER_BACKGROUND_LEVEL2 : NAILER_BACKGROUND;
        }
        return FARMER_BACKGROUND;
    }

    /** Новая профессия с деревом улучшений - добавить ветку тут (плюс сам ...Upgrades-класс и
     * ...TreeView). Профессия без дерева (пока) - null, экран покажет только фон+рамку. */
    @Nullable
    private static ProfessionUpgradeTreeView viewFor(VillagerProfession profession) {
        if (profession == ModVillagers.LUMBERJACK.value()) {
            return new LumberjackTreeView();
        }
        if (profession == VillagerProfession.FARMER) {
            return new FarmerTreeView();
        }
        if (profession == ModVillagers.WARRIOR.value()) {
            return new WarriorTreeView();
        }
        if (profession == ModVillagers.NAILER.value()) {
            return new NailerTreeView();
        }
        return null;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // Раньше фон считался ОДИН РАЗ в конструкторе и кэшировался - если villager (например у
        // nailer'а) разблокировал level2 ПОКА окно уже было открыто (см. NailerUpgrades.
        // markWallComplete), дерево узлов (isHidden, считается каждый кадр) переключалось на
        // level2, а фон так и оставался старым - "дерево level2 с фоном level1". Теперь фон тоже
        // считается каждый кадр, синхронно с деревом.
        ResourceLocation background = backgroundFor(menu.profession(), menu.villager());
        for (int ty = 0; ty < imageHeight; ty += BG_TILE_SIZE) {
            int th = Math.min(BG_TILE_SIZE, imageHeight - ty);
            for (int tx = 0; tx < imageWidth; tx += BG_TILE_SIZE) {
                int tw = Math.min(BG_TILE_SIZE, imageWidth - tx);
                guiGraphics.blit(background, x + tx, y + ty, 0, 0, tw, th, BG_TILE_SIZE, BG_TILE_SIZE);
            }
        }
        guiGraphics.blit(FRAME, x, y, 0, 0, imageWidth, imageHeight, TEXTURE_FILE_SIZE, TEXTURE_FILE_SIZE);
        VillagerProfessionTabs.render(guiGraphics, x, y, currentTabs(), activeTab());

        Villager villager = menu.villager();
        if (treeShown() && villager != null) {
            renderTreeLinks(guiGraphics, x, y, villager);
            renderTreeNodes(guiGraphics, x, y, villager);
        }
    }

    private List<VillagerProfessionTabs.Tab> currentTabs() {
        return VillagerProfessionTabs.availableFor(menu.profession(), menu.villager());
    }

    /** Открытая вкладка; если её у жителя уже нет (список считается каждый кадр), - первая, BRAIN. */
    private VillagerProfessionTabs.Tab activeTab() {
        return currentTabs().contains(selectedTab) ? selectedTab : VillagerProfessionTabs.Tab.BRAIN;
    }

    /** Дерево улучшений рисуется, ловит клики и показывает подсказки только на вкладке BRAIN. */
    private boolean treeShown() {
        return treeView != null && activeTab() == VillagerProfessionTabs.Tab.BRAIN;
    }

    private void renderTreeLinks(GuiGraphics g, int x, int y, Villager villager) {
        assert treeView != null;
        for (int i = 0; i < treeView.size(); i++) {
            if (treeView.isHidden(villager, i)) continue;
            int prerequisite = treeView.prerequisiteIndex(i);
            if (prerequisite < 0 || treeView.isHidden(villager, prerequisite)) continue;

            int fromX = treeView.x(prerequisite);
            int fromY = treeView.y(prerequisite);
            int toX = treeView.x(i);
            int toY = treeView.y(i);

            if (fromY == toY) {
                // Как и в ветке fromAbove ниже - определяем, кто левее, а не считаем, что prerequisite
                // всегда слева: у nailer'а ветка trade_discount_1 -> trade_discount_2 -> copper_block
                // растёт ВЛЕВО (справа в рамке уже некуда), и с жёстким "prerequisite слева" линия
                // просто не рисовалась (fromRight оказывался правее toLeft, цикл не выполнялся).
                boolean fromLeft = fromX < toX;
                int startX = x + (fromLeft ? fromX + NODE_SIZE : toX + NODE_SIZE);
                int endX = x + (fromLeft ? toX : fromX);
                int lineY = y + fromY + (NODE_SIZE - LINE_H_TILE_H) / 2;
                for (int lx = startX; lx < endX; lx += LINE_H_TILE_W) {
                    int w = Math.min(LINE_H_TILE_W, endX - lx);
                    g.blit(LINE_H, lx, lineY, 0, 0, w, LINE_H_TILE_H, LINE_H_TILE_W, LINE_H_TILE_H);
                }
            } else {
                boolean fromAbove = fromY < toY;
                int startY = y + (fromAbove ? fromY + NODE_SIZE : toY + NODE_SIZE);
                int endY = y + (fromAbove ? toY : fromY);
                int lineX = x + fromX + (NODE_SIZE - LINE_V_TILE_W) / 2;
                for (int ly = startY; ly < endY; ly += LINE_V_TILE_H) {
                    int h = Math.min(LINE_V_TILE_H, endY - ly);
                    g.blit(LINE_V, lineX, ly, 0, 0, LINE_V_TILE_W, h, LINE_V_TILE_W, LINE_V_TILE_H);
                }
            }
        }
    }

    private void renderTreeNodes(GuiGraphics g, int x, int y, Villager villager) {
        assert treeView != null;
        for (int i = 0; i < treeView.size(); i++) {
            if (treeView.isHidden(villager, i)) continue;
            int nx = x + treeView.x(i);
            int ny = y + treeView.y(i);

            if (!treeView.isAvailable(villager, i)) {
                g.blit(BLANK_LOCKED, nx, ny, 0, 0, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);
                continue;
            }

            g.blit(treeView.icon(i), nx, ny, 0, 0, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);
            ItemStack overlay = treeView.overlayItem(i);
            if (overlay != null) {
                g.renderItem(overlay, nx + (NODE_SIZE - 16) / 2, ny + (NODE_SIZE - 16) / 2);
            }
            if (treeView.isUnlocked(villager, i)) {
                g.blit(DONE, nx + NODE_SIZE - DONE_SIZE, ny + NODE_SIZE - DONE_SIZE, 0, 0, DONE_SIZE, DONE_SIZE, DONE_SIZE, DONE_SIZE);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Заголовок пока не нужен - только фон+рамка(+дерево, если есть у профессии).
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        Villager villager = menu.villager();
        if (treeShown() && this.minecraft.player != null && villager != null) {
            renderNodeTooltip(guiGraphics, mouseX, mouseY, this.minecraft.player, villager);
        }
        VillagerProfessionTabs.Tab hoveredTab = VillagerProfessionTabs.tabAt(leftPos, topPos, currentTabs(), mouseX, mouseY);
        if (hoveredTab != null) {
            guiGraphics.renderTooltip(this.font, hoveredTab.title(), mouseX, mouseY);
        }
    }

    private void renderNodeTooltip(GuiGraphics g, int mouseX, int mouseY, Player player, Villager villager) {
        int index = hoveredNode(mouseX, mouseY, villager);
        if (index < 0) return;
        g.renderComponentTooltip(this.font, buildTooltip(player, villager, index), mouseX, mouseY);
    }

    private List<Component> buildTooltip(Player player, Villager villager, int index) {
        assert treeView != null;
        List<Component> lines = new ArrayList<>();
        lines.add(treeView.describe(index));

        if (!treeView.isAvailable(villager, index)) {
            lines.add(Component.translatable("tooltip.veridia.villager_upgrade.locked").withStyle(ChatFormatting.RED));
        } else if (treeView.isUnlocked(villager, index)) {
            lines.add(Component.translatable("tooltip.veridia.villager_upgrade.unlocked").withStyle(ChatFormatting.GREEN));
        } else {
            ItemStack costStack = treeView.costDisplayStack(player, index);
            // costDisplayStack уже несёт правильный count (см. её javadoc) - раньше тултип
            // показывал только название предмета без числа, сколько его нужно.
            Component costName = costStack.isEmpty()
                    ? Component.literal("?")
                    : Component.literal(costStack.getCount() + "x ").append(costStack.getHoverName());
            lines.add(Component.translatable("tooltip.veridia.villager_upgrade.cost", costName).withStyle(ChatFormatting.YELLOW));
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && selectTabAt(mouseX, mouseY)) {
            return true;
        }
        Villager villager = menu.villager();
        if (button == 0 && treeShown() && villager != null) {
            int index = hoveredNode(mouseX, mouseY, villager);
            if (index >= 0) {
                onNodeClicked(index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Клик по вкладке открывает её (звук как у кнопок); true - клик попал во вкладку. */
    private boolean selectTabAt(double mouseX, double mouseY) {
        VillagerProfessionTabs.Tab tab = VillagerProfessionTabs.tabAt(leftPos, topPos, currentTabs(), mouseX, mouseY);
        if (tab == null) {
            return false;
        }
        if (tab != activeTab()) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            selectedTab = tab;
        }
        return true;
    }

    private int hoveredNode(double mouseX, double mouseY, Villager villager) {
        if (treeView == null) return -1;
        for (int i = 0; i < treeView.size(); i++) {
            if (treeView.isHidden(villager, i)) continue;
            int nx = leftPos + treeView.x(i);
            int ny = topPos + treeView.y(i);
            if (mouseX >= nx && mouseX < nx + NODE_SIZE && mouseY >= ny && mouseY < ny + NODE_SIZE) {
                return i;
            }
        }
        return -1;
    }

    private void onNodeClicked(int nodeIndex) {
        assert treeView != null;
        Minecraft mc = this.minecraft;
        Player player = mc.player;
        Villager villager = menu.villager();
        if (player == null || mc.gameMode == null || villager == null) return;
        // Клиентская пред-проверка - как в AncientDocumentScreen: если узел и так недоступен или
        // уже разблокирован, кликать нечего, на сервер вообще не шлём.
        if (!treeView.isAvailable(villager, nodeIndex) || treeView.isUnlocked(villager, nodeIndex)) {
            return;
        }
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        mc.gameMode.handleInventoryButtonClick(menu.containerId, nodeIndex);
    }
}
