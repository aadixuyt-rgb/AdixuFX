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
                // Quiet bed with a clear 80 Hz bass transient every second. This exercises
                // the same 40-160 Hz bass detector used by Pulse in the game.
                int withinSecond = frame % sampleRate;
                int amplitude = withinSecond >= sampleRate / 5
                        && withinSecond < sampleRate * 35 / 100 ? 12000 : 600;
                short sample = (short) (Math.sin(2.0 * Math.PI * 80.0 * frame / sampleRate) * amplitude);
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
            Playlist.Track pulseTrack = new Playlist.Track(
                    "pulse-smoke", "Pulse smoke", null, ogg.toAbsolutePath().toString());
            BeatSync.prepare(pulseTrack);
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (BeatSync.durationSeconds() <= 0f && System.nanoTime() < deadline) {
                Thread.sleep(20L);
            }
            if (BeatSync.durationSeconds() <= 0f) {
                throw new IllegalStateException("BeatSync did not finish analyzing the external OGG track");
            }
            // Vorbis encoding/decoding may shift the transient slightly because of
            // codec delay and filter attack. Check the whole known transient window
            // instead of assuming its peak lands on exactly 250 ms.
            float pulse = 0f;
            for (int step = 0; step <= 20; step++) {
                float elapsed = 0.20f + step * 0.02f;
                pulse = Math.max(pulse, BeatSync.pulse(elapsed));
            }
            if (pulse < 0.2f) {
                throw new IllegalStateException("BeatSync did not detect the synthetic OGG transient in its expected window: " + pulse);
            }
            System.out.println("OGG encoder and external-track BeatSync smoke test passed: "
                    + encoded.length + " bytes, peak pulse=" + pulse);
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