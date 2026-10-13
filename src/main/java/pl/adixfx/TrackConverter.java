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

/**
 * Zamienia mp3 na plik audio gotowy do odtworzenia przez mod (dekodowanie: JLayer, bez żadnych
 * programów zewnętrznych). Minecraft wymaga .ogg tylko dla dźwięków wbudowanych w zasoby gry -
 * własne utwory gra osobny, prosty silnik (CustomPlayer) niezależny od silnika dźwięku gry,
 * więc wystarczy zwykły .wav i nie trzeba ryzykownego kodera Ogg Vorbis w Javie.
 */
final class TrackConverter {
    private TrackConverter() {}

    interface Callback {
        void done(boolean ok);
    }

    static void convertAsync(Path mp3, Path outWav, Callback cb) {
        Thread th = new Thread(() -> {
            boolean ok;
            try {
                convert(mp3, outWav);
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

    private static void convert(Path mp3, Path outWav) throws Exception {
        int rate = 44100;
        int channels = 2;
        boolean first = true;

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
                    short s = samples[i];
                    pcm.write(s & 0xFF);
                    pcm.write((s >> 8) & 0xFF);
                }
                bitstream.closeFrame();
            }

            byte[] data = pcm.toByteArray();
            if (data.length == 0) throw new IllegalStateException("Pusty plik mp3 (brak dźwięku do zdekodowania)");

            AudioFormat format = new AudioFormat(rate, 16, channels, true, false);
            try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(data), format,
                    data.length / format.getFrameSize())) {
                Files.createDirectories(outWav.getParent());
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outWav.toFile());
            }
        }
    }
}
