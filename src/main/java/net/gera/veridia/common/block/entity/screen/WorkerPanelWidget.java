package net.gera.veridia.common.block.entity.screen;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.villager.storage.WorkerSlots;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Панель жителей в окне хранилища: ряды "профессия + четыре кнопки" с прокруткой. Кнопки и полосы рисуются
 * отдельными текстурами (textures/gui/levels/village), в фон окна они больше не запечены, кроме иконки информации.
 * <p>
 * Все размеры здесь - в пикселях ТЕКСТУР (512x512 у фона, ассеты в том же масштабе), логический пиксель GUI равен
 * двум пикселям текстуры (TEXTURE_PER_GUI_PIXEL): рисуем с масштабом 0.5, а координаты мыши переводим умножением на 2.
 * Один ряд = один житель: профессия и имя, кнопки действуют на него одного (id действия - маска из одного бита слота,
 * см. WorkerPanelActions). Мест в хранилище MAX_WORKERS, видно VISIBLE_ROWS рядов, остальные прокручиваются колесом
 * или ползунком (прокрутка всегда по целому ряду).
 */
public final class WorkerPanelWidget {

    private static final ResourceLocation VILLAGE_GUI = ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/levels/village/");

    private static ResourceLocation village(String name) {
        return VILLAGE_GUI.withSuffix(name + ".png");
    }

    private static final ResourceLocation ROW_TEXTURE = village("bar_slot");
    private static final ResourceLocation GLOW_TEXTURE = village("highlight_button");
    private static final ResourceLocation ON_TEXTURE = village("on_button");
    private static final ResourceLocation OFF_TEXTURE = village("off_button");
    private static final ResourceLocation STATUS_TEXTURE = village("question_button");
    private static final ResourceLocation SCROLL_BAR_TEXTURE = village("scroll_bar");
    private static final ResourceLocation SCROLLER_TEXTURE = village("scroller");

    /** Во сколько раз пиксель текстуры мельче логического пикселя GUI. */
    public static final int TEXTURE_PER_GUI_PIXEL = 2;

    // --- геометрия, пиксели текстуры, от левого верхнего угла окна ---
    /** Внутренняя область панели в рамке фона (172x110): ряды лежат внутри неё. */
    private static final int PANEL_X = 162;
    private static final int PANEL_Y = 20;
    private static final int ROW_WIDTH = 172;
    private static final int ROW_HEIGHT = 34;
    private static final int ROW_STEP = 36;
    private static final int ROW_TOP_PAD = 2;
    public static final int VISIBLE_ROWS = 3;

    private static final int BUTTON_SIZE = 17;
    /** Кнопки лежат сеткой 2x2 справа в ряду: [свет][ON] сверху, [OFF][?] снизу (как раньше, когда они были в фоне). */
    private static final int BUTTONS_X = ROW_WIDTH - 2 * BUTTON_SIZE - 1;
    private static final int BUTTONS_Y = 0;

    private static final int TEXT_X = 8;
    private static final int TEXT_MAX_WIDTH = BUTTONS_X - TEXT_X - 4;
    /** Две строки в ряду высотой 34: профессия и имя; шрифт чуть мельче обычного. */
    private static final float TEXT_SCALE = 0.8F;
    private static final int PROFESSION_LINE_Y = 3;
    private static final int WORKER_LINE_Y = 18;
    private static final int PROFESSION_COLOR = 0x404040;
    private static final int WORKER_COLOR = 0x606060;

    private static final int SCROLL_BAR_X = 338;
    private static final int SCROLL_BAR_Y = 16;
    private static final int SCROLL_BAR_WIDTH = 8;
    private static final int SCROLL_BAR_HEIGHT = 118;
    private static final int SCROLLER_HEIGHT = 12;

    /** Иконка информации запечена в фон окна (16x16 пикселей текстуры). */
    private static final int INFO_X = 309;
    private static final int INFO_Y = 142;
    private static final int INFO_SIZE = 16;

    private static final int DIM_OVERLAY_COLOR = 0x90000000;

    public static final int ACTION_INFO = 100;

    private enum Button {
        GLOW(WorkerPanelActions.ACTION_GLOW, 0, 0, GLOW_TEXTURE),
        ON(WorkerPanelActions.ACTION_ON, 1, 0, ON_TEXTURE),
        OFF(WorkerPanelActions.ACTION_OFF, 0, 1, OFF_TEXTURE),
        STATUS(WorkerPanelActions.ACTION_STATUS, 1, 1, STATUS_TEXTURE);

        final int action;
        final int column;
        final int line;
        final ResourceLocation texture;

        Button(int action, int column, int line, ResourceLocation texture) {
            this.action = action;
            this.column = column;
            this.line = line;
            this.texture = texture;
        }
    }

    // --- состояние прокрутки (живёт столько же, сколько открытое окно) ---
    private int scrollRow;
    private boolean draggingScroller;

    // --- ряды ---

    /** Один ряд панели = один житель: маска из единственного бита его слота. Пустые слоты пропускаются. */
    private static List<Integer> workerRows(WorkerPanelMenuAccess menu) {
        List<Integer> masks = new ArrayList<>();
        for (int slot = 0; slot < WorkerSlots.MAX_WORKERS; slot++) {
            if (menu.isSlotBound(slot)) {
                masks.add(1 << slot);
            }
        }
        return masks;
    }

    private int maxScroll(int rowCount) {
        return Math.max(0, rowCount - VISIBLE_ROWS);
    }

    private void clampScroll(int rowCount) {
        scrollRow = Math.max(0, Math.min(scrollRow, maxScroll(rowCount)));
    }

    // --- отрисовка ---

    public void render(GuiGraphics guiGraphics, Font font, WorkerPanelMenuAccess menu, int guiX, int guiY) {
        List<Integer> rows = workerRows(menu);
        clampScroll(rows.size());

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(guiX, guiY, 0);
        guiGraphics.pose().scale(1.0F / TEXTURE_PER_GUI_PIXEL, 1.0F / TEXTURE_PER_GUI_PIXEL, 1.0F);
        drawScrollBar(guiGraphics, rows.size());
        for (int line = 0; line < VISIBLE_ROWS && scrollRow + line < rows.size(); line++) {
            drawRow(guiGraphics, font, menu, rows.get(scrollRow + line), line);
        }
        guiGraphics.pose().popPose();
    }

    private void drawScrollBar(GuiGraphics guiGraphics, int rowCount) {
        guiGraphics.blit(SCROLL_BAR_TEXTURE, SCROLL_BAR_X, SCROLL_BAR_Y, 0, 0,
                SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT, SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT);
        guiGraphics.blit(SCROLLER_TEXTURE, SCROLL_BAR_X, scrollerY(rowCount), 0, 0,
                SCROLL_BAR_WIDTH, SCROLLER_HEIGHT, SCROLL_BAR_WIDTH, SCROLLER_HEIGHT);
    }

    /** Верх ползунка: при нулевой прокрутке - у верха полосы, на последней - у низа. */
    private int scrollerY(int rowCount) {
        int max = maxScroll(rowCount);
        int travel = SCROLL_BAR_HEIGHT - SCROLLER_HEIGHT;
        return SCROLL_BAR_Y + (max == 0 ? 0 : travel * scrollRow / max);
    }

    private static int rowY(int line) {
        return PANEL_Y + ROW_TOP_PAD + line * ROW_STEP;
    }

    private static int buttonX(Button button) {
        return PANEL_X + BUTTONS_X + button.column * BUTTON_SIZE;
    }

    private static int buttonY(int line, Button button) {
        return rowY(line) + BUTTONS_Y + button.line * BUTTON_SIZE;
    }

    private void drawRow(GuiGraphics guiGraphics, Font font, WorkerPanelMenuAccess menu, int mask, int line) {
        guiGraphics.blit(ROW_TEXTURE, PANEL_X, rowY(line), 0, 0, ROW_WIDTH, ROW_HEIGHT, ROW_WIDTH, ROW_WIDTH);
        for (Button button : Button.values()) {
            guiGraphics.blit(button.texture, buttonX(button), buttonY(line, button), 0, 0,
                    BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
        }
        // Как раньше: включённым работникам гасится OFF, выключенным - ON (светится текущее состояние).
        int firstSlot = Integer.numberOfTrailingZeros(mask);
        Button dimmed = menu.isSlotEnabled(firstSlot) ? Button.OFF : Button.ON;
        guiGraphics.fill(buttonX(dimmed), buttonY(line, dimmed),
                buttonX(dimmed) + BUTTON_SIZE, buttonY(line, dimmed) + BUTTON_SIZE, DIM_OVERLAY_COLOR);

        drawWorkerText(guiGraphics, font, menu, firstSlot, rowY(line));
    }

    private static void drawWorkerText(GuiGraphics guiGraphics, Font font, WorkerPanelMenuAccess menu, int slot, int rowY) {
        Component unloaded = Component.translatable("gui.veridia.worker_panel.unloaded");
        Component profession = professionName(menu.slotProfessionId(slot));
        if (profession == null) {
            drawLine(guiGraphics, font, unloaded, rowY + PROFESSION_LINE_Y, PROFESSION_COLOR);
            return;
        }
        drawLine(guiGraphics, font, profession, rowY + PROFESSION_LINE_Y, PROFESSION_COLOR);
        // Профессию хранилище помнит и у незагруженного жителя; имя известно только загруженному (индекс < 0 - не загружен).
        int nameIndex = menu.slotNameIndex(slot);
        if (nameIndex > 0) {
            drawLine(guiGraphics, font, Component.translatable("entity.veridia.worker_name." + nameIndex), rowY + WORKER_LINE_Y, WORKER_COLOR);
        } else if (nameIndex < 0) {
            drawLine(guiGraphics, font, unloaded, rowY + WORKER_LINE_Y, WORKER_COLOR);
        }
    }

    /** Строка обычным шрифтом (поверх масштаба 0.5 фона), уменьшенным TEXT_SCALE и сжатая до ширины TEXT_MAX_WIDTH. */
    private static void drawLine(GuiGraphics guiGraphics, Font font, Component text, int textureY, int color) {
        float scale = TEXT_SCALE * TEXTURE_PER_GUI_PIXEL;
        float fit = Math.min(1.0F, TEXT_MAX_WIDTH / (font.width(text) * scale));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(PANEL_X + TEXT_X, textureY, 0);
        guiGraphics.pose().scale(scale * fit, scale * fit, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    @Nullable
    private static Component professionName(int professionId) {
        if (professionId < 0) return null;
        VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.byId(professionId);
        if (profession == null) return null;
        ResourceLocation key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        if (key == null) return null;
        String suffix = key.getNamespace().equals("minecraft") ? key.getPath() : key.getNamespace() + "." + key.getPath();
        return Component.translatable("entity.minecraft.villager." + suffix);
    }

    // --- попадание мышью (координаты - логические, от левого верхнего угла окна) ---

    /** ACTION_INFO, id действия для menu button (маска слотов * ACTIONS_PER_SLOT + действие) или -1. */
    public int hitTest(WorkerPanelMenuAccess menu, double relMouseX, double relMouseY) {
        double tx = relMouseX * TEXTURE_PER_GUI_PIXEL;
        double ty = relMouseY * TEXTURE_PER_GUI_PIXEL;
        if (within(tx, ty, INFO_X, INFO_Y, INFO_SIZE, INFO_SIZE)) {
            return ACTION_INFO;
        }
        List<Integer> rows = workerRows(menu);
        for (int line = 0; line < VISIBLE_ROWS && scrollRow + line < rows.size(); line++) {
            for (Button button : Button.values()) {
                if (within(tx, ty, buttonX(button), buttonY(line, button), BUTTON_SIZE, BUTTON_SIZE)) {
                    return rows.get(scrollRow + line) * WorkerPanelActions.ACTIONS_PER_SLOT + button.action;
                }
            }
        }
        return -1;
    }

    private static boolean within(double x, double y, int boxX, int boxY, int width, int height) {
        return x >= boxX && x < boxX + width && y >= boxY && y < boxY + height;
    }

    // --- прокрутка ---

    /** Колесо над панелью или полосой прокрутки. true - событие съедено. */
    public boolean mouseScrolled(WorkerPanelMenuAccess menu, double relMouseX, double relMouseY, double deltaY) {
        double tx = relMouseX * TEXTURE_PER_GUI_PIXEL;
        double ty = relMouseY * TEXTURE_PER_GUI_PIXEL;
        boolean overPanel = within(tx, ty, PANEL_X, PANEL_Y, ROW_WIDTH, VISIBLE_ROWS * ROW_STEP + ROW_TOP_PAD);
        boolean overBar = within(tx, ty, SCROLL_BAR_X, SCROLL_BAR_Y, SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT);
        if (!overPanel && !overBar) {
            return false;
        }
        int rowCount = workerRows(menu).size();
        if (maxScroll(rowCount) == 0 || deltaY == 0) {
            return true;
        }
        scrollRow += deltaY > 0 ? -1 : 1;
        clampScroll(rowCount);
        return true;
    }

    /** Нажатие на полосу или ползунок: начинает перетаскивание и сразу ставит ползунок под мышь. */
    public boolean mousePressed(WorkerPanelMenuAccess menu, double relMouseX, double relMouseY) {
        double tx = relMouseX * TEXTURE_PER_GUI_PIXEL;
        double ty = relMouseY * TEXTURE_PER_GUI_PIXEL;
        if (!within(tx, ty, SCROLL_BAR_X, SCROLL_BAR_Y, SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT)) {
            return false;
        }
        draggingScroller = true;
        dragTo(menu, relMouseY);
        return true;
    }

    public boolean mouseDragged(WorkerPanelMenuAccess menu, double relMouseY) {
        if (!draggingScroller) {
            return false;
        }
        dragTo(menu, relMouseY);
        return true;
    }

    public void mouseReleased() {
        draggingScroller = false;
    }

    /** Ряд, соответствующий положению мыши на полосе: центр ползунка идёт за мышью по всей длине хода. */
    private void dragTo(WorkerPanelMenuAccess menu, double relMouseY) {
        int rowCount = workerRows(menu).size();
        int max = maxScroll(rowCount);
        if (max == 0) {
            scrollRow = 0;
            return;
        }
        double travel = SCROLL_BAR_HEIGHT - SCROLLER_HEIGHT;
        double fraction = (relMouseY * TEXTURE_PER_GUI_PIXEL - SCROLL_BAR_Y - SCROLLER_HEIGHT / 2.0) / travel;
        scrollRow = (int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * max);
    }

    // --- подсказки и информация ---

    /** Кнопка INFO - вместо отдельного экрана просто красиво расписывает кнопки в чат. */
    public static void sendInfoMessage(Player player) {
        player.sendSystemMessage(Component.literal("===== ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("gui.veridia.worker_panel.info_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal(" =====").withStyle(ChatFormatting.GOLD)));
        sendLine(player, "☀", ChatFormatting.YELLOW, "gui.veridia.worker_panel.glow");
        sendLine(player, "ON", ChatFormatting.GREEN, "gui.veridia.worker_panel.on");
        sendLine(player, "OFF", ChatFormatting.RED, "gui.veridia.worker_panel.off");
        sendLine(player, "❓", ChatFormatting.AQUA, "gui.veridia.worker_panel.status");
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.translatable("gui.veridia.worker_panel.info_job_application")
                .withStyle(ChatFormatting.AQUA));
    }

    private static void sendLine(Player player, String icon, ChatFormatting color, String descKey) {
        player.sendSystemMessage(Component.literal(icon + " ").withStyle(color, ChatFormatting.BOLD)
                .append(Component.translatable(descKey).withStyle(ChatFormatting.GRAY)));
    }

    public static Component tooltipFor(int action) {
        if (action == ACTION_INFO) {
            return Component.translatable("gui.veridia.worker_panel.info");
        }
        return switch (action % WorkerPanelActions.ACTIONS_PER_SLOT) {
            case WorkerPanelActions.ACTION_GLOW -> Component.translatable("gui.veridia.worker_panel.glow");
            case WorkerPanelActions.ACTION_ON -> Component.translatable("gui.veridia.worker_panel.on");
            case WorkerPanelActions.ACTION_OFF -> Component.translatable("gui.veridia.worker_panel.off");
            case WorkerPanelActions.ACTION_STATUS -> Component.translatable("gui.veridia.worker_panel.status");
            default -> Component.empty();
        };
    }
}
