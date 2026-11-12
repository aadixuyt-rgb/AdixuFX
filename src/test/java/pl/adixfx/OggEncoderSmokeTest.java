package pl.adixfx;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** CI smoke test: creates a short WAV and verifies that OggEncoder writes a Vorbis stream. */
public final class OggEncoderSmokeTest {
    private OggEncoderSmokeTest() {}

    public static void main(String[] args) throws Exception {
        Path wav = Files.createTempFile("adixfx-ogg-smoke-", ".wav");
        Path ogg = Files.createTempFile("adixfx-ogg-smoke-", ".ogg");
        try {
            int sampleRate = 44100;
            int channels = 2;
            int seconds = 2;
            int frames = sampleRate * seconds;
            byte[] pcm = new byte[frames * channels * 2];
            for (int frame = 0; frame < frames; frame++) {
                short sample = (short) (Math.sin(2.0 * Math.PI * 440.0 * frame / sampleRate) * 12000);
                for (int channel = 0; channel < channels; channel++) {
                    int offset = (frame * channels + channel) * 2;
                    pcm[offset] = (byte) (sample & 0xFF);
                    pcm[offset + 1] = (byte) ((sample >>> 8) & 0xFF);
                }
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
            try (AudioInputStream input = new AudioInputStream(
                    new ByteArrayInputStream(pcm), format, frames)) {
                AudioSystem.write(input, AudioFileFormat.Type.WAVE, wav.toFile());
            }

            OggEncoder.encode(wav, ogg);
            byte[] encoded = Files.readAllBytes(ogg);
            if (encoded.length < 128
                    || encoded[0] != 'O' || encoded[1] != 'g'
                    || encoded[2] != 'g' || encoded[3] != 'S'
                    || !containsVorbisHeader(encoded)) {
                throw new IllegalStateException("OggEncoder nie utworzył poprawnego strumienia OGG/Vorbis");
            }
            System.out.println("OGG encoder smoke test passed: " + encoded.length + " bytes");
        } finally {
            Files.deleteIfExists(wav);
            Files.deleteIfExists(ogg);
        }
    }

    private static boolean containsVorbisHeader(byte[] bytes) {
        byte[] signature = {'v', 'o', 'r', 'b', 'i', 's'};
        outer:
        for (int i = 0; i <= bytes.length - signature.length; i++) {
            for (int j = 0; j < signature.length; j++) {
                if (bytes[i + j] != signature[j]) continue outer;
            }
            return true;
        }
        return false;
    }
}