package net.gera.veridia.common.worldgen.biome.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.gera.veridia.Veridia;
import net.gera.veridia.common.worldgen.biome.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Затемнение неба в пепельных биомах: пока игрок стоит в пепельных равнинах, лесу или у пепельной реки, небо плавно темнеет
 * как ночью (и днём, и ночью - затемнение только усиливает тьму, светлее оно ничего не делает), туман тускнеет,
 * солнце, луна и звёзды прячутся за пеплом, а облака становятся серыми - цвета тумана биома, только чуть темнее.
 * <p>
 * Это чисто клиентская картинка: на сервер, спавн мобов и настоящее время суток она не влияет.
 * <p>
 * Как устроено: каждый тик считается число {@link #amount} от 0 (обычное небо) до 1 (полная тьма),
 * оно нарастает, пока игрок в ash-биоме, и спадает, когда он вышел. Дальше его читают:
 * <ul>
 *   <li>{@code ClientLevelAshGloomMixin} - цвет неба, цвет облаков и освещение мира от неба;</li>
 *   <li>{@code LevelRendererAshGloomMixin} - прячет солнце, луну и звёзды;</li>
 *   <li>{@link #onComputeFogColor} - цвет тумана (через событие, миксин не нужен);</li>
 *   <li>{@link #onRenderFog} - дымка: туман ближе, видно хуже.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public final class AshGloom {
    // ---------- Настройки скорости (в тиках, 20 тиков = 1 секунда) ----------
    // За сколько тиков небо темнеет с нуля до полной тьмы после входа в биом
    private static final int FADE_IN_TICKS = 200;
    // За сколько тиков светлеет обратно после выхода из биома
    private static final int FADE_OUT_TICKS = 100;

    // ---------- Настройки яркости при полном затемнении ----------
    // Цвет неба умножается на это число: 1.0 - как обычно, 0.0 - чёрное небо (ванильная ночь близка к нулю)
    private static final float SKY_BRIGHTNESS = 0.12F;
    // Цвет тумана умножается на это число: 1.0 - как обычно, 0.0 - чёрный туман
    private static final float FOG_BRIGHTNESS = 0.6F;
    // Освещение блоков от неба (то, что даёт ночное затемнение земли): 1.0 - полный день, 0.2 - ванильная ночь.
    // Земля под открытым небом выглядит как место с таким светом при дневном освещении (красный и зелёный каналы карты
    // освещения, из 255): 0.2 - 80 (ночь), 0.3 - 106 (свет 7), 0.4 - 132 (свет 9), 0.45 - 144 (между 9 и 10), 0.5 - 156 (10),
    // 0.6 - 178 (свет 11-12). Синеватый оттенок ночи при этом остаётся. Ночью в биоме всё так же темно, как в ванили
    // (затемнение не светлит, только темнит). Факелы и другой свет от блоков это не трогает.
    private static final float WORLD_LIGHT = 0.45F;
    // Яркость облаков относительно тумана: 1.0 - такие же, как туман, меньше - темнее тумана
    private static final float CLOUD_BRIGHTNESS_VS_FOG = 0.85F;

    // ---------- Дымка при полном затемнении (туман по расстоянию) ----------
    // Ванильный туман начинается у самого края прорисовки и закрывает его узкой полосой, поэтому дальность видна как
    // обрыв мира. Дымка начинается рядом с игроком и густеет плавно (шейдер тумана смешивает по smoothstep), так что
    // вблизи её почти не видно, а вдали всё постепенно тонет в сером. К HAZE_END всё скрыто полностью; это ближе края
    // прорисовки, поэтому сам край не виден. Если прорисовка меньше HAZE_END, дымка просто доходит до её края.
    // С какого расстояния (блоков) начинается дымка
    private static final float HAZE_START = 12.0F;
    // На каком расстоянии (блоков) всё целиком в тумане: меньше - хуже видимость
    private static final float HAZE_END = 144.0F;

    // Оттенок облаков при полном затемнении: цвет тумана биома, приглушённый так же, как сам туман, и ещё чуть темнее.
    // Им умножается ванильный цвет облаков, поэтому ночью и в дождь облака темнеют так же, как в ванили.
    private static final Vec3 CLOUD_TINT = Vec3.fromRGB24(ModBiomes.FOG_COLOR)
            .scale(FOG_BRIGHTNESS * CLOUD_BRIGHTNESS_VS_FOG);

    // Уровень, для которого посчитано затемнение: при смене мира или измерения оно не должно плавно доезжать с нуля
    private static ClientLevel trackedLevel;
    private static float previousAmount;
    private static float currentAmount;
    // Ванильный туман земли текущего кадра, если дымка его заменила (иначе null), и туман, отложенный на время облаков
    private static FogRange vanillaTerrainFog;
    private static FogRange fogBeforeClouds;

    /** Начало и конец тумана в блоках. */
    private record FogRange(float start, float end) {
        static FogRange current() {
            return new FogRange(RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd());
        }

        void apply() {
            RenderSystem.setShaderFogStart(start);
            RenderSystem.setShaderFogEnd(end);
        }
    }

    private AshGloom() {
    }

    /** Степень затемнения от 0 до 1 с плавным переходом между тиками. */
    public static float amount(float partialTick) {
        float linear = Mth.lerp(partialTick, previousAmount, currentAmount);
        return (float) Mth.smoothstep(linear);
    }

    public static Vec3 darkenSky(Vec3 skyColor, float amount) {
        return skyColor.scale(Mth.lerp(amount, 1.0F, SKY_BRIGHTNESS));
    }

    public static Vec3 tintClouds(Vec3 cloudColor, float amount) {
        Vec3 tint = new Vec3(1.0D, 1.0D, 1.0D).lerp(CLOUD_TINT, amount);
        return cloudColor.multiply(tint);
    }

    public static float darkenWorldLight(float skyDarken, float amount) {
        return Mth.lerp(amount, skyDarken, Math.min(skyDarken, WORLD_LIGHT));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            reset();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        boolean inAshBiome = isAshBiome(level, player.blockPosition());
        if (level != trackedLevel) {
            snapTo(level, inAshBiome);
            return;
        }
        advance(inAshBiome);
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        float amount = amount((float) event.getPartialTick());
        if (amount <= 0.0F) {
            return;
        }
        float brightness = Mth.lerp(amount, 1.0F, FOG_BRIGHTNESS);
        event.setRed(event.getRed() * brightness);
        event.setGreen(event.getGreen() * brightness);
        event.setBlue(event.getBlue() * brightness);
    }

    /**
     * Дальность тумана: ближе к дымке по мере затемнения. Только на воздухе (под водой, в лаве и снегу свой туман) и
     * только если дымка ближе ванильного тумана: слепоту и тьму, у которых туман ещё ближе, не трогаем. Небо (FOG_SKY)
     * получает ту же дальность, чтобы полоса дымки у горизонта сливалась с утонувшей в тумане землёй.
     */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getMode() == FogRenderer.FogMode.FOG_TERRAIN) {
            vanillaTerrainFog = null;
        }
        float amount = amount((float) event.getPartialTick());
        if (amount <= 0.0F || event.getType() != FogType.NONE) {
            return;
        }
        float vanillaNear = event.getNearPlaneDistance();
        float vanillaFar = event.getFarPlaneDistance();
        if (event.getMode() == FogRenderer.FogMode.FOG_TERRAIN) {
            vanillaTerrainFog = new FogRange(vanillaNear, vanillaFar);
        }
        event.setNearPlaneDistance(Math.min(vanillaNear, Mth.lerp(amount, vanillaNear, HAZE_START)));
        event.setFarPlaneDistance(Math.min(vanillaFar, Mth.lerp(amount, vanillaFar, HAZE_END)));
        // Отменённое событие - знак NeoForge применить заданные здесь расстояния
        event.setCanceled(true);
    }

    /** Перед облаками (LevelRendererAshGloomMixin): вернуть ванильный туман земли этого кадра, если его заменила дымка. */
    public static void useVanillaFogForClouds() {
        if (vanillaTerrainFog == null) {
            return;
        }
        fogBeforeClouds = FogRange.current();
        vanillaTerrainFog.apply();
    }

    /** После облаков: снова дымка. */
    public static void restoreFogAfterClouds() {
        if (fogBeforeClouds != null) {
            fogBeforeClouds.apply();
            fogBeforeClouds = null;
        }
    }

    private static boolean isAshBiome(ClientLevel level, BlockPos pos) {
        return ModBiomes.isAshBiome(level.getBiome(pos));
    }

    private static void advance(boolean inAshBiome) {
        float step = inAshBiome ? 1.0F / FADE_IN_TICKS : -1.0F / FADE_OUT_TICKS;
        previousAmount = currentAmount;
        currentAmount = Mth.clamp(currentAmount + step, 0.0F, 1.0F);
    }

    // Зашли в мир (или сменили измерение) - сразу правильная картинка, без перехода
    private static void snapTo(ClientLevel level, boolean inAshBiome) {
        trackedLevel = level;
        previousAmount = inAshBiome ? 1.0F : 0.0F;
        currentAmount = previousAmount;
    }

    private static void reset() {
        trackedLevel = null;
        previousAmount = 0.0F;
        currentAmount = 0.0F;
    }
}
