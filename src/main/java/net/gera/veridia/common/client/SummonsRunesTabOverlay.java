package net.gera.veridia.common.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.client.mixin.CreativeModeInventoryScreenAccessor;
import net.gera.veridia.common.item.ModCreativeModeTabs;
import net.gera.veridia.common.item.SummonsRunesCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Рисует слева от GUI креатива кнопки-подвкладки, пока открыта вкладка "Призыв и руны", и по клику
 * переключает {@link SummonsRunesCategory#active}, перезаполняя сетку. Экран не трогаем виджетами -
 * всё рендерится и хиттестится вручную в событиях (без миксинов в поведение).
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public final class SummonsRunesTabOverlay {

    private static final int BTN = 26;
    private static final int GAP = 4;
    private static final int OFFSET_X = 30; // насколько левее панели GUI
    private static final int FIRST_Y = 6;   // отступ первой кнопки от верха панели

    private static final int BG = 0xC0202020;
    private static final int BORDER = 0xFF000000;
    private static final int BORDER_SELECTED = 0xFFFFFFFF;
    private static final int BORDER_HOVERED = 0xFFA0A0A0;

    private SummonsRunesTabOverlay() {
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen) || isMyTab()) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        int x = screen.getGuiLeft() - OFFSET_X;

        SummonsRunesCategory[] categories = SummonsRunesCategory.values();
        for (int i = 0; i < categories.length; i++) {
            SummonsRunesCategory category = categories[i];
            int y = screen.getGuiTop() + FIRST_Y + i * (BTN + GAP);
            boolean hovered = inside(mouseX, mouseY, x, y);
            boolean selected = SummonsRunesCategory.active == category;

            g.fill(x - 1, y - 1, x + BTN + 1, y + BTN + 1,
                    selected ? BORDER_SELECTED : hovered ? BORDER_HOVERED : BORDER);
            g.fill(x, y, x + BTN, y + BTN, BG);
            g.renderFakeItem(new ItemStack(category.icon()), x + 5, y + 5);

            if (hovered) {
                g.renderTooltip(Minecraft.getInstance().font,
                        Component.translatable(category.titleKey), mouseX, mouseY);
            }
        }
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0
                || !(event.getScreen() instanceof CreativeModeInventoryScreen screen)
                || isMyTab()) {
            return;
        }

        int x = screen.getGuiLeft() - OFFSET_X;
        SummonsRunesCategory[] categories = SummonsRunesCategory.values();
        for (int i = 0; i < categories.length; i++) {
            int y = screen.getGuiTop() + FIRST_Y + i * (BTN + GAP);
            if (!inside((int) event.getMouseX(), (int) event.getMouseY(), x, y)) {
                continue;
            }
            if (SummonsRunesCategory.active != categories[i]) {
                SummonsRunesCategory.active = categories[i];
                rebuild(screen);
            }
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            event.setCanceled(true);
            return;
        }
    }

    private static boolean isMyTab() {
        return CreativeModeInventoryScreenAccessor.veridia$getSelectedTab() != ModCreativeModeTabs.SUMMONS_RUNES_TAB.get();
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + BTN && mouseY >= y && mouseY < y + BTN;
    }

    private static void rebuild(CreativeModeInventoryScreen screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        CreativeModeTab tab = ModCreativeModeTabs.SUMMONS_RUNES_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                mc.player.connection.enabledFeatures(),
                mc.player.canUseGameMasterBlocks(),
                mc.player.level().registryAccess()));
        ((CreativeModeInventoryScreenAccessor) screen).veridia$refreshCurrentTabContents(tab.getDisplayItems());
    }
}