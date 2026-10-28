package net.gera.veridia.common.entity.ominous.rage.client;

import net.minecraft.client.sounds.AudioStream;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.function.DoubleSupplier;

/**
 * Поток трека, который начинается не с начала. Перемотки у ванильных потоков нет, поэтому всё до нужного места
 * декодируется и выбрасывается (несколько минут ogg - доли секунды, делается не в главном потоке).
 */
final class OffsetAudioStream implements AudioStream {
    private static final int SKIP_CHUNK_BYTES = 64 * 1024;

    private final AudioStream source;
    // Хвост последнего пропущенного куска, который уже после нужного места
    @Nullable
    private ByteBuffer leftover;

    private OffsetAudioStream(AudioStream source) {
        this.source = source;
    }

    /**
     * Пропускает начало трека до targetSeconds. Цель спрашивается заново на каждом шаге: пока декодируем, трек
     * «играет» дальше, и запуск догоняет его.
     */
    static AudioStream skipTo(AudioStream source, DoubleSupplier targetSeconds) {
        OffsetAudioStream stream = new OffsetAudioStream(source);
        try {
            stream.skip(targetSeconds);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return stream;
    }

    private void skip(DoubleSupplier targetSeconds) throws IOException {
        AudioFormat format = source.getFormat();
        int frameSize = format.getFrameSize();
        double bytesPerSecond = format.getSampleRate() * frameSize;
        long skipped = 0L;
        while (true) {
            long target = (long) (targetSeconds.getAsDouble() * bytesPerSecond) / frameSize * frameSize;
            long remaining = target - skipped;
            if (remaining <= 0L) {
                return;
            }
            ByteBuffer chunk = source.read((int) Math.min(remaining, SKIP_CHUNK_BYTES));
            int size = chunk.remaining();
            if (size == 0) {
                return;
            }
            if (size > remaining) {
                chunk.position(chunk.position() + (int) remaining);
                leftover = chunk;
                return;
            }
            skipped += size;
        }
    }

    @Override
    public AudioFormat getFormat() {
        return source.getFormat();
    }

    @Override
    public ByteBuffer read(int size) throws IOException {
        if (leftover != null) {
            ByteBuffer rest = leftover;
            leftover = null;
            if (rest.hasRemaining()) {
                return rest;
            }
        }
        return source.read(size);
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}
