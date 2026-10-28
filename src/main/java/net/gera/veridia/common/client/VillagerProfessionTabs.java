package net.gera.veridia.common.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.aibuff.villager.nailer.NailerUpgrades;
import net.gera.veridia.common.villager.ModVillagers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Вкладки над рамкой окна профессии (VillagerProfessionScreen), как в окне достижений: те же спрайты
 * tab_above_left/middle/right (28x32, обычный и _selected), только с нашей иконкой вместо предмета. Тут - какие
 * вкладки есть у жителя, как они рисуются и куда можно кликнуть; что внутри вкладки, решает экран.
 * <p>
 * У всех жителей есть вкладка BRAIN (дерево улучшений). У nailer'а со второго уровня (NailerUpgrades.isLevel2Unlocked,
 * где технологии: улучшение стен, башни) добавляется вторая, BUILD - пока пустое окно с фоном.
 */
final class VillagerProfessionTabs {

    private VillagerProfessionTabs() {
    }

    enum Tab {
        BRAIN("textures/gui/villager/brain_nobg.png", "gui.veridia.villager_tab.brain"),
        BUILD("textures/gui/villager/nailer/hammer_nobg.png", "gui.veridia.villager_tab.build");

        private final ResourceLocation icon;
        private final Component title;

        Tab(String iconPath, String titleKey) {
            this.icon = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, iconPath);
            this.title = Component.translatable(titleKey);
        }

        Component title() {
            return title;
        }
    }

    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    /** Промежуток между вкладками - как в окне достижений. */
    private static final int TAB_GAP = 4;
    /** На сколько выбранная вкладка заходит на рамку: спрайт 32 высотой, окно начинается на TAB_OVERLAP строк выше низа. */
    private static final int TAB_OVERLAP = 4;
    /** Иконки наших вкладок - 18x18 с прозрачным фоном; по ширине центрируются, по высоте - как предмет 16x16 у ванили. */
    private static final int ICON_SIZE = 18;
    private static final int ICON_TOP = 8;

    private static final ResourceLocation LEFT = sprite("tab_above_left");
    private static final ResourceLocation LEFT_SELECTED = sprite("tab_above_left_selected");
    private static final ResourceLocation MIDDLE = sprite("tab_above_middle");
    private static final ResourceLocation MIDDLE_SELECTED = sprite("tab_above_middle_selected");
    private static final ResourceLocation RIGHT = sprite("tab_above_right");
    private static final ResourceLocation RIGHT_SELECTED = sprite("tab_above_right_selected");

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/" + name + ".png");
    }

    /** Вкладки жителя по порядку; BRAIN всегда первая. */
    static List<Tab> availableFor(VillagerProfession profession, @Nullable Villager villager) {
        List<Tab> tabs = new ArrayList<>();
        tabs.add(Tab.BRAIN);
        if (profession == ModVillagers.NAILER.value() && villager != null && NailerUpgrades.isLevel2Unlocked(villager)) {
            tabs.add(Tab.BUILD);
        }
        return tabs;
    }

    static void render(GuiGraphics g, int windowLeft, int windowTop, List<Tab> tabs, Tab selected) {
        for (int index = 0; index < tabs.size(); index++) {
            Tab tab = tabs.get(index);
            int x = tabX(windowLeft, index);
            int y = tabY(windowTop);
            g.blit(spriteFor(index, tabs.size(), tab == selected), x, y, 0, 0, TAB_WIDTH, TAB_HEIGHT, TAB_WIDTH, TAB_HEIGHT);
            g.blit(tab.icon, x + (TAB_WIDTH - ICON_SIZE) / 2, y + ICON_TOP, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
    }

    /** Вкладка под курсором или null. */
    @Nullable
    static Tab tabAt(int windowLeft, int windowTop, List<Tab> tabs, double mouseX, double mouseY) {
        int y = tabY(windowTop);
        for (int index = 0; index < tabs.size(); index++) {
            int x = tabX(windowLeft, index);
            if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT) {
                return tabs.get(index);
            }
        }
        return null;
    }

    private static int tabX(int windowLeft, int index) {
        return windowLeft + (TAB_WIDTH + TAB_GAP) * index;
    }

    private static int tabY(int windowTop) {
        return windowTop - TAB_HEIGHT + TAB_OVERLAP;
    }

    /** Первая вкладка - левый спрайт, последняя из нескольких - правый, остальные - средний. */
    private static ResourceLocation spriteFor(int index, int count, boolean selected) {
        if (index == 0) {
            return selected ? LEFT_SELECTED : LEFT;
        }
        if (index == count - 1) {
            return selected ? RIGHT_SELECTED : RIGHT;
        }
        return selected ? MIDDLE_SELECTED : MIDDLE;
    }
}
