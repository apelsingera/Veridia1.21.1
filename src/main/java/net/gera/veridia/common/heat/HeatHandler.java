package net.gera.veridia.common.heat;

import net.gera.veridia.Veridia;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Жар: рядом с большим лавовым озером в пепельных биомах (см. {@link LavaHeat}) и во всём Незере игрок сначала
 * накапливает жар, и только через EXPOSURE_DELAY_TICKS тиков (6 секунд) подряд эффект включается. Пока он действует:
 * <ul>
 *   <li>раз в секунду урон без отдачи и тряски камеры ({@link HeatDamage}) и красная надпись над панелью;</li>
 *   <li>замедление (обычный эффект замедления);</li>
 *   <li>красная виньетка и сужение угла обзора на клиенте (сервер шлёт силу эффекта, см. {@link HeatPayload}).</li>
 * </ul>
 * Эффект нарастает и спадает плавно. Когда игрок отошёл от источника, накопленный жар спадает вдвое быстрее, чем
 * копился. Творческий режим и наблюдатель жара не чувствуют. Обойти жар сейчас нельзя ({@link HeatProtection}); жар
 * озёр можно выключить на весь мир командой /veridiadebug heat lakes off ({@link HeatSettings}).
 */
@EventBusSubscriber(modid = Veridia.MOD_ID)
public final class HeatHandler {
    // ---------- Настройки (20 тиков = 1 секунда) ----------
    // Сколько тиков подряд игрок должен простоять у источника жара, прежде чем эффект включится
    private static final int EXPOSURE_DELAY_TICKS = 120;
    // Накопленный жар не растёт выше задержки больше чем на столько тиков: так эффект быстро проходит, когда игрок ушёл
    private static final int EXPOSURE_OVERFLOW_TICKS = 20;
    // Во сколько раз быстрее жар спадает, когда игрок ушёл от источника
    private static final int COOLDOWN_MULTIPLIER = 2;
    // За сколько тиков эффект нарастает до полной силы (медленно, 5 секунд) и спадает до нуля
    private static final float RISE_TICKS = 100.0F;
    private static final float FALL_TICKS = 80.0F;
    // Как часто (тиков) ищется источник жара вокруг игрока
    private static final int SCAN_INTERVAL_TICKS = 10;
    // Урон раз в столько тиков, а сам урон в единицах здоровья (2 = одно сердце): в Незере, у озера и вплотную к лаве озера
    private static final int DAMAGE_INTERVAL_TICKS = 20;
    private static final float NETHER_DAMAGE = 1.0F;
    private static final float LAKE_DAMAGE = 1.0F;
    private static final float LAKE_NEAR_DAMAGE = 2.0F;
    // Ближе скольки блоков до лавы озера жар сильнее
    private static final double LAKE_NEAR_DISTANCE = 6.0D;
    // Замедление: пока эффект нарастает, уровень 0 (-15% скорости), на полной силе уровень 1 (-30%); длительность обновляется
    private static final int SLOWNESS_DURATION_TICKS = 40;
    private static final int SLOWNESS_PARTIAL_AMPLIFIER = 0;
    private static final int SLOWNESS_FULL_AMPLIFIER = 1;
    // Клиенту сила эффекта шлётся, когда изменилась не меньше чем на столько процентов (и всегда на 0 и на 100)
    private static final int SYNC_STEP_PERCENT = 4;

    private static final Map<UUID, HeatState> STATES = new HashMap<>();

    private HeatHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HeatState state = STATES.computeIfAbsent(player.getUUID(), id -> new HeatState());
        if (canFeelHeat(player)) {
            tickHeat(player, state);
        } else {
            state.reset();
        }
        syncToClient(player, state);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
    }

    private static boolean canFeelHeat(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator() && !HeatProtection.isProtected(player);
    }

    private static void tickHeat(ServerPlayer player, HeatState state) {
        // В Незере источник всегда есть и проверка ничего не стоит, поэтому отсчёт задержки идёт с первого тика; у озёр
        // поиск дорогой и идёт раз в SCAN_INTERVAL_TICKS, так что включение может опоздать не больше чем на полсекунды
        boolean inNether = player.level().dimension() == Level.NETHER;
        if (inNether || player.tickCount % SCAN_INTERVAL_TICKS == 0) {
            state.damage = heatDamage(player);
        }
        boolean exposed = state.damage > 0.0F;
        state.exposureTicks = exposed
                ? Math.min(state.exposureTicks + 1, EXPOSURE_DELAY_TICKS + EXPOSURE_OVERFLOW_TICKS)
                : Math.max(0, state.exposureTicks - COOLDOWN_MULTIPLIER);

        boolean active = state.exposureTicks >= EXPOSURE_DELAY_TICKS;
        state.intensity = active
                ? Math.min(1.0F, state.intensity + 1.0F / RISE_TICKS)
                : Math.max(0.0F, state.intensity - 1.0F / FALL_TICKS);

        if (active && exposed && player.tickCount % DAMAGE_INTERVAL_TICKS == 0) {
            HeatDamage.deal(player, state.damage);
            player.displayClientMessage(Component.translatable("message.veridia.too_hot")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        }
        if (state.intensity > 0.0F && player.tickCount % SCAN_INTERVAL_TICKS == 0) {
            applySlowness(player, state.intensity);
        }
    }

    // Урон за раз от источника жара рядом с игроком или 0, если источника нет
    private static float heatDamage(ServerPlayer player) {
        if (player.level().dimension() == Level.NETHER) {
            return NETHER_DAMAGE;
        }
        if (!HeatSettings.get(player.server).lakeHeat()) {
            return 0.0F;
        }
        OptionalDouble lakeDistance = LavaHeat.nearestLakeDistance(player.level(), player.blockPosition());
        if (lakeDistance.isEmpty()) {
            return 0.0F;
        }
        return lakeDistance.getAsDouble() <= LAKE_NEAR_DISTANCE ? LAKE_NEAR_DAMAGE : LAKE_DAMAGE;
    }

    // Скрытое от частиц замедление: значок эффекта виден, а облака частиц вокруг игрока нет
    private static void applySlowness(ServerPlayer player, float intensity) {
        int amplifier = intensity >= 1.0F ? SLOWNESS_FULL_AMPLIFIER : SLOWNESS_PARTIAL_AMPLIFIER;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION_TICKS, amplifier,
                true, false, true));
    }

    private static void syncToClient(ServerPlayer player, HeatState state) {
        int percent = Math.round(state.intensity * 100.0F);
        boolean bigStep = Math.abs(percent - state.sentPercent) >= SYNC_STEP_PERCENT;
        boolean edge = percent != state.sentPercent && (percent == 0 || percent == 100);
        if (bigStep || edge) {
            state.sentPercent = percent;
            PacketDistributor.sendToPlayer(player, new HeatPayload(percent));
        }
    }
}
