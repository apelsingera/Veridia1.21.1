package net.gera.veridia.common.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.villager.ModVillagers;
import net.gera.veridia.common.villager.network.OpenVillagerProfessionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.ref.WeakReference;

/**
 * Кнопка поверх торгового окна (MerchantScreen) - только для жителей наших профессий (farmer -
 * ванильная, warrior/lumberjack/nailer - свои, см. ModVillagers). Для всех остальных торговцев
 * (в т.ч. ванильных) окно рисуется как обычно. Клик отправляет OpenVillagerProfessionPayload на
 * сервер, тот открывает VillagerProfessionMenu через обычный ServerPlayer#openMenu - ровно тот же
 * механизм перехода между контейнерными экранами, что у AncientDocumentScreen (закрытие текущего
 * контейнера ServerPlayer#openMenu делает сам). Раньше здесь был ручной клиентский
 * Minecraft.setScreen()/closeContainer() - при таком переходе на экране повисал блюр от
 * MerchantScreen (настройка игрока Menu Background Blurriness) и не пропадал; через штатный
 * серверный openMenu (как в MerchantScreen/AncientDocumentScreen и вообще везде в ваниле) этого
 * не происходит.
 * <p>
 * MerchantMenu на клиенте не хранит ссылку на реального жителя (там ClientSideMerchant-заглушка,
 * см. декомпилированный MerchantMenu), поэтому профессию узнаём иначе: запоминаем последнего
 * жителя, с которым игрок взаимодействовал (PlayerInteractEvent.EntityInteract срабатывает
 * раньше, чем сервер откроет окно), и используем его на момент рендера/клика.
 * <p>
 * Подмена фона торгового окна (MerchantScreenBackgroundMixin, redirect на GuiGraphics.blit
 * внутри renderBg) временно отключена в backgroundOverride() - villager_gui.png теперь под
 * VillagerProfessionScreen, а не под фон MerchantScreen (размеры текстур больше не совпадают).
 * Отдельная текстура под фон торгового окна будет позже.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public final class VillagerMerchantGuiOverlay {

    private VillagerMerchantGuiOverlay() {
    }

    private static final ResourceLocation BUTTON =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/button.png");
    private static final ResourceLocation BUTTON_HIGHLIGHTED =
            ResourceLocation.fromNamespaceAndPath(Veridia.MOD_ID, "textures/gui/villager/button_highlighted.png");

    private static final int BUTTON_X = 251;
    private static final int BUTTON_Y = 62;
    private static final int BUTTON_SIZE = 18;

    private static WeakReference<Villager> lastInteractedVillager = new WeakReference<>(null);

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Villager villager) {
            lastInteractedVillager = new WeakReference<>(villager);
        }
    }

    private static boolean isEligibleProfession() {
        Villager villager = lastInteractedVillager.get();
        if (villager == null) {
            return false;
        }
        VillagerProfession profession = villager.getVillagerData().getProfession();
        return ModVillagers.isEligibleForProfessionScreen(profession);
    }

    /** Вызывается из MerchantScreenBackgroundMixin вместо оригинальной текстуры фона.
     * Подмена временно отключена (см. javadoc класса) - фон торгового окна всегда ванильный. */
    public static ResourceLocation backgroundOverride(ResourceLocation vanillaBackground) {
        return vanillaBackground;
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof MerchantScreen screen) || !isEligibleProfession()) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        int x = screen.getGuiLeft() + BUTTON_X;
        int y = screen.getGuiTop() + BUTTON_Y;
        boolean hovered = event.getMouseX() >= x && event.getMouseX() < x + BUTTON_SIZE
                && event.getMouseY() >= y && event.getMouseY() < y + BUTTON_SIZE;

        ResourceLocation texture = hovered ? BUTTON_HIGHLIGHTED : BUTTON;
        g.blit(texture, x, y, 0, 0.0F, 0.0F, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || !(event.getScreen() instanceof MerchantScreen screen) || !isEligibleProfession()) {
            return;
        }

        int x = screen.getGuiLeft() + BUTTON_X;
        int y = screen.getGuiTop() + BUTTON_Y;
        if (event.getMouseX() < x || event.getMouseX() >= x + BUTTON_SIZE
                || event.getMouseY() < y || event.getMouseY() >= y + BUTTON_SIZE) {
            return;
        }

        Villager villager = lastInteractedVillager.get();
        if (villager != null) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            PacketDistributor.sendToServer(new OpenVillagerProfessionPayload(villager.getId()));
        }
        event.setCanceled(true);
    }
}
