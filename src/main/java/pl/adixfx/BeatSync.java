package pl.adixfx;

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Offline bass-onset analysis used by the Pulse hitbox mode.
 * OGG files are decoded with Minecraft's bundled LWJGL/STB Vorbis decoder;
 * imported tracks are analyzed from the PCM WAV created by TrackConverter.
 */
final class BeatSync {
    private static final int FRAMES_PER_SECOND = 50; // 20 ms analysis windows
    private static final int BASELINE_FRAMES = 45;
    private static final int REFRACTORY_FRAMES = 8;

    private static final ConcurrentHashMap<String, Analysis> CACHE = new ConcurrentHashMap<>();
    private static final AtomicLong GENERATION = new AtomicLong();

    private static volatile Analysis activeAnalysis;

    private BeatSync() {}

    static void prepare(Playlist.Track track) {
        long generation = GENERATION.incrementAndGet();
        activeAnalysis = null;
        String key = cacheKey(track);

        Analysis cached = CACHE.get(key);
        if (cached != null) {
            activeAnalysis = cached;
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                Analysis result = analyze(track);
                CACHE.put(key, result);
                if (GENERATION.get() == generation) activeAnalysis = result;
            } catch (Throwable error) {
                if (GENERATION.get() == generation) {
                    System.err.println("[AdixFX] Nie udało się przeanalizować utworu do synchronizacji beatów: "
                            + track.id() + " (" + error.getMessage() + ")");
                }
            }
        }, "AdixFX-BeatAnalysis");
        worker.setDaemon(true);
        worker.start();
    }

    static void reset() {
        GENERATION.incrementAndGet();
        activeAnalysis = null;
    }

    /** Exact decoded duration (seconds), or 0 while background analysis is not ready. */
    static float durationSeconds() {
        Analysis analysis = activeAnalysis;
        return analysis == null ? 0f : analysis.durationSeconds;
    }

    /** Returns a short decaying flash (0..1) around a detected bass hit. */
    static float pulse(float elapsedSeconds) {
        Analysis analysis = activeAnalysis;
        if (analysis == null || !Float.isFinite(elapsedSeconds) || elapsedSeconds < 0f) return 0f;

        int frame = (int) (elapsedSeconds * FRAMES_PER_SECOND);
        float pulse = 0f;
        // Hold the flash for almost half a second so it remains visible at any playback volume.
        for (int back = 0; back <= 24; back++) {
            int index = frame - back;
            if (index < 0 || index >= analysis.beats.length) continue;
            float value = (float) (analysis.beats[index] * Math.exp(-back / 10.0));
            if (value > pulse) pulse = value;
        }
        return Math.max(0f, Math.min(1f, pulse));
    }

    private static String cacheKey(Playlist.Track track) {
        if (!track.isCustom()) return "builtin:" + track.id();
        File file = new File(track.customFile());
        return "custom:" + track.id() + ":" + file.length() + ":" + file.lastModified();
    }

    private static Analysis analyze(Playlist.Track track) throws Exception {
        return track.isCustom() ? analyzeWave(new File(track.customFile())) : analyzeOgg(track);
    }

    private static Analysis analyzeOgg(Playlist.Track track) throws IOException {
        String resourcePath = "/assets/adixfx/sounds/" + track.id() + ".ogg";
        byte[] encoded;
        try (InputStream in = BeatSync.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IOException("Brak zasobu " + resourcePath);
            encoded = in.readAllBytes();
        }

        ByteBuffer memory = MemoryUtil.memAlloc(encoded.length);
        try {
            memory.put(encoded).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                long decoder = STBVorbis.stb_vorbis_open_memory(memory, error, null);
                if (decoder == MemoryUtil.NULL) {
                    throw new IOException("STB Vorbis error " + error.get(0));
                }

                try {
                    STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                    STBVorbis.stb_vorbis_get_info(decoder, info);
                    int channels = info.channels();
                    int sampleRate = info.sample_rate();
                    if (channels < 1 || sampleRate < 1) throw new IOException("Nieprawidłowy format OGG");

                    BassAnalyzer analyzer = new BassAnalyzer(sampleRate);
                    float[] pcm = new float[8192 * channels];
                    int samplesPerChannel;
                    while ((samplesPerChannel = STBVorbis.stb_vorbis_get_samples_float_interleaved(
                            decoder, channels, pcm)) > 0) {
                        for (int frame = 0; frame < samplesPerChannel; frame++) {
                            float mono = 0f;
                            int base = frame * channels;
                            for (int channel = 0; channel < channels; channel++) {
                                mono += pcm[base + channel];
                            }
                            analyzer.accept(mono / channels);
                        }
                    }
                    return analyzer.finish();
                } finally {
                    STBVorbis.stb_vorbis_close(decoder);
                }
            }
        } finally {
            MemoryUtil.memFree(memory);
        }
    }

    private static Analysis analyzeWave(File file) throws Exception {
        try (AudioInputStream input = AudioSystem.getAudioInputStream(file)) {
            AudioFormat format = input.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
                    || format.getSampleSizeInBits() != 16
                    || format.getFrameSize() < format.getChannels() * 2) {
                throw new UnsupportedAudioFileException("Oczekiwano 16-bitowego PCM WAV");
            }

            int channels = format.getChannels();
            int frameSize = format.getFrameSize();
            int sampleRate = Math.round(format.getSampleRate());
            if (channels < 1 || sampleRate < 1) throw new UnsupportedAudioFileException("Nieprawidłowy WAV");

            BassAnalyzer analyzer = new BassAnalyzer(sampleRate);
            byte[] buffer = new byte[Math.max(frameSize * 1024, 8192)];
            int carry = 0;
            int read;
            boolean bigEndian = format.isBigEndian();

            while ((read = input.read(buffer, carry, buffer.length - carry)) != -1) {
                if (read == 0) continue;
                int available = carry + read;
                int usable = available - available % frameSize;

                for (int offset = 0; offset < usable; offset += frameSize) {
                    float mono = 0f;
                    for (int channel = 0; channel < channels; channel++) {
                        int sampleOffset = offset + channel * 2;
                        int first = buffer[sampleOffset] & 0xFF;
                        int second = buffer[sampleOffset + 1] & 0xFF;
                        int sample = bigEndian ? (short) ((first << 8) | second)
                                : (short) ((second << 8) | first);
                        mono += sample / 32768f;
                    }
                    analyzer.accept(mono / channels);
                }

                carry = available - usable;
                if (carry > 0) System.arraycopy(buffer, usable, buffer, 0, carry);
            }
            return analyzer.finish();
        }
    }

    private record Analysis(float[] beats, float durationSeconds) {}

    /** 40-160 Hz band energy, sampled in 20 ms windows. */
    private static final class BassAnalyzer {
        private final int sampleRate;
        private final int windowSize;
        private final double alphaLowPass;
        private final double alphaHighPass;
        private final FloatBuilder energy = new FloatBuilder();

        private double lowPass;
        private double highPass;
        private double squareSum;
        private int samplesInWindow;
        private long totalSamples;

        private BassAnalyzer(int sampleRate) {
            this.sampleRate = sampleRate;
            this.windowSize = Math.max(128, sampleRate / FRAMES_PER_SECOND);
            this.alphaLowPass = 1.0 - Math.exp(-2.0 * Math.PI * 160.0 / sampleRate);
            this.alphaHighPass = 1.0 - Math.exp(-2.0 * Math.PI * 40.0 / sampleRate);
        }

        private void accept(float sample) {
            totalSamples++;
            lowPass += alphaLowPass * (sample - lowPass);
            highPass += alphaHighPass * (sample - highPass);
            double bass = lowPass - highPass;
            squareSum += bass * bass;
            samplesInWindow++;

            if (samplesInWindow >= windowSize) flushWindow();
        }

        private void flushWindow() {
            energy.add((float) Math.sqrt(squareSum / Math.max(1, samplesInWindow)));
            squareSum = 0.0;
            samplesInWindow = 0;
        }

        private Analysis finish() {
            if (samplesInWindow > 0) flushWindow();
            float[] values = energy.toArray();
            float[] beats = new float[values.length];
            int lastBeat = -REFRACTORY_FRAMES;

            for (int i = 1; i < values.length; i++) {
                int start = Math.max(0, i - BASELINE_FRAMES);
                double sum = 0.0;
                for (int j = start; j < i; j++) sum += values[j];
                float baseline = i == start ? values[i - 1] : (float) (sum / (i - start));

                // Beat = quick bass-energy rise above the recent 0.9 s background level.
                // This is deliberately more sensitive than a strict isolated-peak detector,
                // so kick drums and softer bass hits are not silently ignored.
                float rise = values[i] - values[i - 1];
                float threshold = Math.max(0.0012f, baseline * 1.12f);
                float minimumRise = Math.max(0.00025f, baseline * 0.07f);
                boolean localPeak = values[i] >= values[Math.min(values.length - 1, i + 1)];

                if (localPeak && values[i] > threshold && rise > minimumRise
                        && i - lastBeat >= REFRACTORY_FRAMES) {
                    float relativeRise = (values[i] - baseline) / (baseline + 0.0025f);
                    float strength = 0.60f + relativeRise * 0.35f;
                    beats[i] = Math.max(0.60f, Math.min(1f, strength));
                    lastBeat = i;
                }
            }
            float duration = totalSamples / (float) sampleRate;
            return new Analysis(beats, duration);
        }
    }

    private static final class FloatBuilder {
        private float[] values = new float[4096];
        private int size;

        private void add(float value) {
            if (size == values.length) {
                float[] expanded = new float[values.length * 2];
                System.arraycopy(values, 0, expanded, 0, values.length);
                values = expanded;
            }
            values[size++] = value;
        }

        private float[] toArray() {
            float[] copy = new float[size];
            System.arraycopy(values, 0, copy, 0, size);
            return copy;
        }
    }
}
