package net.gera.veridia.common.entity.cinderroamer.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.entity.cinderroamer.CinderRoamerEntity;
import net.gera.veridia.common.entity.cinderroamer.CinderRoamerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * Окно пеплохода (фон - окно лошади): слева два слота сёдел и сам пеплоход, справа ячейки открытого рюкзака и над
 * ними вкладки 1-3 - по одной на каждый висящий рюкзак. Клик по вкладке открывает этот рюкзак. Над окном справа -
 * кнопка «Лечь/Встать» (своей текстуры нет - обычная кнопка игры).
 */
public class CinderRoamerScreen extends AbstractContainerScreen<CinderRoamerMenu> {
    private static final ResourceLocation BACKGROUND = texture("cinderroamer_gui.png");
    private static final ResourceLocation BACKPACK_SLOTS = texture("15slots.png");
    private static final ResourceLocation[] TABS = {texture("1.png"), texture("2.png"), texture("3.png")};
    private static final ResourceLocation SADDLE_SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/horse/saddle_slot");

    private static final int BACKPACK_SLOTS_X = 79;
    private static final int BACKPACK_SLOTS_Y = 17;
    private static final int BACKPACK_SLOTS_WIDTH = 90;
    private static final int BACKPACK_SLOTS_HEIGHT = 54;
    // Вкладки 9x9 над ячейками: x = TAB_X + номер * TAB_STEP
    private static final int TAB_X = 79;
    private static final int TAB_Y = 8;
    private static final int TAB_STEP = 10;
    private static final int TAB_SIZE = 9;
    // Не открытые вкладки темнее
    private static final float CLOSED_TAB_BRIGHTNESS = 0.55F;
    private static final int SADDLE_SLOT_X = 7;
    private static final int FRONT_SADDLE_Y = 17;
    private static final int REAR_SADDLE_Y = 35;
    // Пеплоход в чёрном окошке; он длинный, поэтому мельче лошади (у неё 17)
    private static final int PREVIEW_LEFT = 26;
    private static final int PREVIEW_TOP = 18;
    private static final int PREVIEW_RIGHT = 78;
    private static final int PREVIEW_BOTTOM = 70;
    private static final int PREVIEW_SCALE = 11;

    // Кнопка «Лечь/Встать» над окном, у правого края (справа часто занято списком предметов)
    private static final int LIE_BUTTON_GAP = 2;
    private static final int LIE_BUTTON_WIDTH = 56;
    private static final int LIE_BUTTON_HEIGHT = 20;
    private static final Component LIE_DOWN = Component.translatable("gui.veridia.cinder_roamer.lie_down");
    private static final Component STAND_UP = Component.translatable("gui.veridia.cinder_roamer.stand_up");

    private float mouseXForPreview;
    private float mouseYForPreview;
    private Button lieButton;

    public CinderRoamerScreen(CinderRoamerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        lieButton = addRenderableWidget(Button.builder(LIE_DOWN, button -> pressLieButton())
                .bounds(leftPos + imageWidth - LIE_BUTTON_WIDTH, topPos - LIE_BUTTON_HEIGHT - LIE_BUTTON_GAP,
                        LIE_BUTTON_WIDTH, LIE_BUTTON_HEIGHT)
                .build());
    }

    private void pressLieButton() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CinderRoamerMenu.LIE_DOWN_BUTTON);
        }
    }

    // Надпись по тому, лежит ли он сейчас; пока на нём кто-то едет, лечь нельзя
    private void updateLieButton() {
        CinderRoamerEntity roamer = menu.getRoamer();
        lieButton.visible = roamer != null && roamer.isTame();
        lieButton.active = roamer != null && !roamer.isVehicle();
        lieButton.setMessage(roamer != null && roamer.isInSittingPose() ? STAND_UP : LIE_DOWN);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/cinderroamer/" + name);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        guiGraphics.blitSprite(SADDLE_SLOT_SPRITE, leftPos + SADDLE_SLOT_X, topPos + FRONT_SADDLE_Y, 18, 18);
        guiGraphics.blitSprite(SADDLE_SLOT_SPRITE, leftPos + SADDLE_SLOT_X, topPos + REAR_SADDLE_Y, 18, 18);
        int backpacks = menu.backpackCount();
        if (menu.getOpenBackpack() < backpacks) {
            guiGraphics.blit(BACKPACK_SLOTS, leftPos + BACKPACK_SLOTS_X, topPos + BACKPACK_SLOTS_Y, 0, 0,
                    BACKPACK_SLOTS_WIDTH, BACKPACK_SLOTS_HEIGHT, BACKPACK_SLOTS_WIDTH, BACKPACK_SLOTS_HEIGHT);
        }
        for (int tab = 0; tab < backpacks; tab++) {
            renderTab(guiGraphics, tab, tab == menu.getOpenBackpack());
        }
        CinderRoamerEntity roamer = menu.getRoamer();
        if (roamer != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, leftPos + PREVIEW_LEFT, topPos + PREVIEW_TOP,
                    leftPos + PREVIEW_RIGHT, topPos + PREVIEW_BOTTOM, PREVIEW_SCALE, 0.25F,
                    mouseXForPreview, mouseYForPreview, roamer);
        }
    }

    private void renderTab(GuiGraphics guiGraphics, int tab, boolean open) {
        float brightness = open ? 1.0F : CLOSED_TAB_BRIGHTNESS;
        guiGraphics.setColor(brightness, brightness, brightness, 1.0F);
        guiGraphics.blit(TABS[tab], leftPos + TAB_X + tab * TAB_STEP, topPos + TAB_Y, 0, 0,
                TAB_SIZE, TAB_SIZE, TAB_SIZE, TAB_SIZE);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int tab = 0; tab < menu.backpackCount(); tab++) {
            if (isOverTab(tab, mouseX, mouseY)) {
                openTab(tab);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOverTab(int tab, double mouseX, double mouseY) {
        double x = mouseX - (leftPos + TAB_X + tab * TAB_STEP);
        double y = mouseY - (topPos + TAB_Y);
        return x >= 0 && x < TAB_SIZE && y >= 0 && y < TAB_SIZE;
    }

    private void openTab(int tab) {
        if (tab == menu.getOpenBackpack()) {
            return;
        }
        menu.openBackpack(tab);
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, tab);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.mouseXForPreview = mouseX;
        this.mouseYForPreview = mouseY;
        updateLieButton();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
