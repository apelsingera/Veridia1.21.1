package net.gera.veridia.common.entity.ominous.rage.client;

import net.gera.veridia.Veridia;
import net.gera.veridia.common.entity.ominous.rage.OminousRage;
import net.gera.veridia.common.sound.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Музыка ярости. Трек the_big_gundown идёт в пепельных биомах всю ярость, будто его играет сам биом: игрок слышит его,
 * только пока он там. Вышел из биома - трек затихает; умер, перезашёл, перезагрузил звук - обрывается. Снова в
 * ярости - трек продолжается с того места, где он сейчас «играет» (по часам мира от начала ярости), а не с начала.
 * Пока играет трек, обычная музыка молчит. Громкость - ползунок «Музыка» в настройках звука.
 */
@EventBusSubscriber(modid = Veridia.MOD_ID, value = Dist.CLIENT)
public final class OminousRageMusic {
    // Плавный вход (в том числе с середины трека) и затихание при выходе из биома: доля громкости за тик
    private static final float FADE_IN_PER_TICK = 1.0F / 20.0F;
    private static final float FADE_OUT_PER_TICK = 1.0F / 40.0F;
    // У самого конца трека заново не запускаем
    private static final int MIN_REMAINING_TICKS = 40;
    // Звук не запустился (нет свободного канала) - следующая попытка не раньше чем через столько тиков
    private static final int RETRY_DELAY_TICKS = 20;
    private static final double TICKS_PER_SECOND = 20.0D;

    @Nullable
    private static RageTrack playing;
    private static int retryDelay;

    private OminousRageMusic() {
    }

    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        if (OminousRageClient.isPlayerInRage()) {
            event.setMusic(null);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean inRage = OminousRageClient.isPlayerInRage();
        // Доиграл, затих или оборвался (смерть, выход из мира, перезагрузка звука)
        if (playing != null && (playing.isStopped() || !minecraft.getSoundManager().isActive(playing))) {
            playing = null;
        }
        if (playing != null) {
            playing.setAudible(inRage);
            return;
        }
        if (retryDelay > 0) {
            retryDelay--;
            return;
        }
        long elapsedTicks = OminousRageClient.elapsedTicks();
        if (inRage && elapsedTicks >= 0L && elapsedTicks < OminousRage.DURATION_TICKS - MIN_REMAINING_TICKS) {
            playing = new RageTrack(elapsedTicks);
            minecraft.getSoundManager().play(playing);
            retryDelay = RETRY_DELAY_TICKS;
        }
    }

    /** Трек, запущенный с места startTicks от начала ярости; плавно входит и затихает. */
    private static final class RageTrack extends AbstractTickableSoundInstance {
        private final long startTicks;
        private final long createdNanos = System.nanoTime();
        private boolean audible = true;

        RageTrack(long startTicks) {
            super(ModSounds.THE_BIG_GUNDOWN.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.startTicks = startTicks;
            this.volume = 0.0F;
            this.looping = false;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        void setAudible(boolean audible) {
            this.audible = audible;
        }

        // Начинает с нулевой громкости и разгорается в tick
        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (audible) {
                volume = Math.min(1.0F, volume + FADE_IN_PER_TICK);
            } else {
                volume = Math.max(0.0F, volume - FADE_OUT_PER_TICK);
                if (volume <= 0.0F) {
                    stop();
                }
            }
        }

        // Пропуск начала - в фоновом потоке, чтобы игра не замирала
        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
            return soundBuffers.getStream(sound.getPath(), looping).thenApplyAsync(
                    stream -> OffsetAudioStream.skipTo(stream, this::currentPositionSeconds), Util.backgroundExecutor());
        }

        // Где трек должен быть сейчас: место запуска плюс время, прошедшее с запуска
        private double currentPositionSeconds() {
            return startTicks / TICKS_PER_SECOND + (System.nanoTime() - createdNanos) / 1.0E9D;
        }
    }
}
