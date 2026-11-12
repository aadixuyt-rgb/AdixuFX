package pl.adixfx;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.MinecraftClient;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Converts imported MP3 audio into an OGG/Vorbis file stored outside the mod JAR. */
final class TrackConverter {
    private TrackConverter() {}

    interface Callback {
        void done(boolean ok);
    }

    static void convertAsync(Path mp3, Path outOgg, Callback cb) {
        Thread th = new Thread(() -> {
            boolean ok;
            try {
                convert(mp3, outOgg);
                ok = true;
            } catch (Throwable t) {
                t.printStackTrace();
                ok = false;
            }
            boolean finalOk = ok;
            MinecraftClient.getInstance().execute(() -> cb.done(finalOk));
        }, "AdixFX-Convert");
        th.setDaemon(true);
        th.start();
    }

    /**
     * Linearly resamples interleaved little-endian signed 16-bit PCM without
     * changing its channel count. MP3 decoding currently buffers the full track,
     * so this runs before the temporary WAV is written.
     */
    private static byte[] resamplePcm16(byte[] input, int sourceRate, int targetRate,
                                        int channels) {
        if (sourceRate <= 0 || targetRate <= 0 || channels <= 0) {
            throw new IllegalArgumentException("Nieprawidłowe parametry audio");
        }
        int bytesPerFrame = channels * 2;
        if (input.length % bytesPerFrame != 0) {
            throw new IllegalArgumentException("Niepełna ramka PCM z dekodera MP3");
        }
        if (sourceRate == targetRate) return input;

        int sourceFrames = input.length / bytesPerFrame;
        long targetFramesLong = Math.max(1L,
                Math.round((double) sourceFrames * targetRate / sourceRate));
        if (targetFramesLong > Integer.MAX_VALUE / bytesPerFrame) {
            throw new IllegalArgumentException("Plik MP3 jest zbyt duży do konwersji");
        }
        int targetFrames = (int) targetFramesLong;
        byte[] output = new byte[targetFrames * bytesPerFrame];

        for (int frame = 0; frame < targetFrames; frame++) {
            double sourcePosition = (double) frame * sourceRate / targetRate;
            int firstFrame = Math.min(sourceFrames - 1, (int) sourcePosition);
            int secondFrame = Math.min(sourceFrames - 1, firstFrame + 1);
            double fraction = sourcePosition - firstFrame;

            for (int channel = 0; channel < channels; channel++) {
                int firstOffset = firstFrame * bytesPerFrame + channel * 2;
                int secondOffset = secondFrame * bytesPerFrame + channel * 2;
                int firstSample = (short) ((input[firstOffset] & 0xFF)
                        | (input[firstOffset + 1] << 8));
                int secondSample = (short) ((input[secondOffset] & 0xFF)
                        | (input[secondOffset + 1] << 8));
                int sample = (int) Math.round(firstSample
                        + (secondSample - firstSample) * fraction);
                sample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));

                int outputOffset = frame * bytesPerFrame + channel * 2;
                output[outputOffset] = (byte) (sample & 0xFF);
                output[outputOffset + 1] = (byte) ((sample >>> 8) & 0xFF);
            }
        }
        return output;
    }

    static void convert(Path mp3, Path outOgg) throws Exception {
        int rate = 44100;
        int channels = 2;
        boolean first = true;
        Path temporaryWav = Files.createTempFile("adixfx-import-", ".wav");
        try {
            try (InputStream in = Files.newInputStream(mp3);
                 ByteArrayOutputStream pcm = new ByteArrayOutputStream()) {
                Bitstream bitstream = new Bitstream(in);
                Decoder decoder = new Decoder();
                Header header;
                while ((header = bitstream.readFrame()) != null) {
                    SampleBuffer buf = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                    if (first) {
                        rate = buf.getSampleFrequency();
                        channels = buf.getChannelCount();
                        first = false;
                    }
                    short[] samples = buf.getBuffer();
                    int len = buf.getBufferLength();
                    for (int i = 0; i < len; i++) {
                        short sample = samples[i];
                        pcm.write(sample & 0xFF);
                        pcm.write((sample >>> 8) & 0xFF);
                    }
                    bitstream.closeFrame();
                }

                byte[] data = pcm.toByteArray();
                if (data.length == 0) {
                    throw new IllegalStateException("Pusty plik MP3 (brak dźwięku do zdekodowania)");
                }

                if (channels < 1 || channels > 2) {
                    throw new IllegalStateException("MP3 musi mieć jeden lub dwa kanały audio");
                }

                // jVorbisEnc's 22/32 kHz setup tables have only two NoiseGuard
                // entries, while its long-block setup indexes four. Normalize
                // decoded MP3 audio to 44.1 kHz before Vorbis encoding so the
                // encoder consistently uses the complete 44.1 kHz setup table.
                final int encoderRate = 44100;
                byte[] normalized = resamplePcm16(data, rate, encoderRate, channels);
                AudioFormat format = new AudioFormat(encoderRate, 16, channels, true, false);
                try (AudioInputStream ais = new AudioInputStream(
                        new ByteArrayInputStream(normalized), format, normalized.length / format.getFrameSize())) {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, temporaryWav.toFile());
                }
            }

            OggEncoder.encode(temporaryWav, outOgg);
        } finally {
            Files.deleteIfExists(temporaryWav);
        }
    }
}