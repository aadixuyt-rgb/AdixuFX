package pl.adixfx;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.MinecraftClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** MP3 -> real Ogg Vorbis conversion. The resulting .ogg is stored outside the mod JAR. */
final class TrackConverter {
    private static final int OUTPUT_RATE = 44100;

    private TrackConverter() {}

    interface Callback {
        void done(boolean ok);
    }

    private record PcmData(int sampleRate, int channels, byte[] bytes) {}

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

    private static void convert(Path mp3, Path outOgg) throws Exception {
        PcmData decoded = decodeMp3(mp3);
        byte[] stereo44100 = resampleToStereo(decoded);

        Path parent = outOgg.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = outOgg.resolveSibling(outOgg.getFileName().toString() + ".tmp");
        try {
            OggVorbisEncoder.encode(stereo44100, OUTPUT_RATE, temporary);
            try {
                Files.move(temporary, outOgg, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, outOgg, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static PcmData decodeMp3(Path mp3) throws Exception {
        int sampleRate = -1;
        int channels = -1;

        try (InputStream in = Files.newInputStream(mp3);
             ByteArrayOutputStream pcm = new ByteArrayOutputStream()) {
            Bitstream bitstream = new Bitstream(in);
            Decoder decoder = new Decoder();
            try {
                Header header;
                while ((header = bitstream.readFrame()) != null) {
                    SampleBuffer buffer = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                    if (sampleRate < 0) {
                        sampleRate = buffer.getSampleFrequency();
                        channels = buffer.getChannelCount();
                    } else if (sampleRate != buffer.getSampleFrequency()
                            || channels != buffer.getChannelCount()) {
                        throw new IllegalStateException("Plik MP3 zmienia format audio w trakcie odtwarzania");
                    }

                    short[] samples = buffer.getBuffer();
                    int length = buffer.getBufferLength();
                    for (int i = 0; i < length; i++) {
                        short sample = samples[i];
                        pcm.write(sample & 0xFF);
                        pcm.write((sample >>> 8) & 0xFF);
                    }
                    bitstream.closeFrame();
                }
            } finally {
                bitstream.close();
            }

            byte[] bytes = pcm.toByteArray();
            if (bytes.length == 0 || sampleRate <= 0 || (channels != 1 && channels != 2)) {
                throw new IllegalStateException("Nie udało się odczytać dźwięku z pliku MP3");
            }
            return new PcmData(sampleRate, channels, bytes);
        }
    }

    /** Converts decoded mono/stereo PCM to the stereo 44.1 kHz format used by the Vorbis encoder. */
    private static byte[] resampleToStereo(PcmData source) {
        int frameSize = source.channels() * 2;
        if (source.bytes().length % frameSize != 0) {
            throw new IllegalStateException("Nieprawidłowe dane PCM z dekodera MP3");
        }

        int inputFrames = source.bytes().length / frameSize;
        if (source.sampleRate() == OUTPUT_RATE && source.channels() == 2) {
            return source.bytes();
        }

        long outputFramesLong = Math.round((double) inputFrames * OUTPUT_RATE / source.sampleRate());
        if (outputFramesLong <= 0 || outputFramesLong > Integer.MAX_VALUE / 4) {
            throw new IllegalStateException("Plik MP3 jest zbyt długi do konwersji");
        }

        int outputFrames = (int) outputFramesLong;
        byte[] output = new byte[outputFrames * 4];
        for (int outFrame = 0; outFrame < outputFrames; outFrame++) {
            double sourcePosition = (double) outFrame * source.sampleRate() / OUTPUT_RATE;
            int leftFrame = Math.min((int) sourcePosition, inputFrames - 1);
            int rightFrame = Math.min(leftFrame + 1, inputFrames - 1);
            double fraction = sourcePosition - leftFrame;

            for (int channel = 0; channel < 2; channel++) {
                int sourceChannel = source.channels() == 1 ? 0 : channel;
                short a = readSample(source.bytes(), leftFrame, source.channels(), sourceChannel);
                short b = readSample(source.bytes(), rightFrame, source.channels(), sourceChannel);
                short mixed = (short) Math.round(a + (b - a) * fraction);
                int offset = outFrame * 4 + channel * 2;
                output[offset] = (byte) (mixed & 0xFF);
                output[offset + 1] = (byte) ((mixed >>> 8) & 0xFF);
            }
        }
        return output;
    }

    private static short readSample(byte[] pcm, int frame, int channels, int channel) {
        int offset = (frame * channels + channel) * 2;
        return (short) ((pcm[offset + 1] << 8) | (pcm[offset] & 0xFF));
    }
}
