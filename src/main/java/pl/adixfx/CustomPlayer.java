package pl.adixfx;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Silnik grania własnych (przeciągniętych) utworów - niezależny od systemu dźwięku Minecrafta,
 * bo ten umie czytać tylko dźwięki wbudowane w zasoby gry. Gra bezpośrednio przez javax.sound.
 */
final class CustomPlayer {
    private static Clip clip;
    private static Playlist.Track track;
    private static boolean paused;
    private static long pauseFrame;

    private CustomPlayer() {}

    static boolean isPaused() {
        return paused;
    }

    static void play(Playlist.Track t) {
        stop();
        Path temporaryWav = null;
        try {
            Path source = Path.of(t.customFile());
            File audioFile = source.toFile();
            if (source.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".ogg")) {
                temporaryWav = OggAudio.decodeToTemporaryWav(source);
                audioFile = temporaryWav.toFile();
            }

            clip = AudioSystem.getClip();
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile)) {
                clip.open(ais);
            }
            if (temporaryWav != null) {
                Files.deleteIfExists(temporaryWav);
                temporaryWav = null;
            }
            track = t;
            paused = false;
            applyVolume();
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
            if (clip != null) clip.close();
            clip = null;
            track = null;
        } finally {
            if (temporaryWav != null) {
                try { Files.deleteIfExists(temporaryWav); } catch (Exception ignored) {}
            }
        }
    }

    static void pause() {
        if (clip != null && clip.isRunning()) {
            pauseFrame = clip.getLongFramePosition();
            clip.stop();
        }
        paused = true;
    }

    static void resume() {
        paused = false;
        if (clip != null) {
            clip.setFramePosition((int) Math.min(pauseFrame, Integer.MAX_VALUE));
            clip.start();
        }
    }

    static void stop() {
        if (clip != null) {
            try {
                clip.stop();
                clip.close();
            } catch (Exception ignored) {
            }
        }
        clip = null;
        track = null;
        paused = false;
        pauseFrame = 0;
    }

    static void applyVolume() {
        if (clip == null || track == null) return;
        try {
            FloatControl ctrl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float gain = Music.gain(track);
            float db = gain <= 0.0001f ? ctrl.getMinimum() : (float) (20.0 * Math.log10(gain));
            db = Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), db));
            ctrl.setValue(db);
        } catch (Exception ignored) {
            // niektóre linie audio nie mają kontroli głośności - trudno, gra ciszej/głośniej się nie da
        }
    }

    /** true, gdy utwór doszedł do końca sam (nie przez pauzę/stop). */
    static boolean finished() {
        return clip != null && !paused && !clip.isRunning()
                && clip.getLongFramePosition() >= clip.getFrameLength();
    }

    static void tick() {
        if (!paused) applyVolume();
    }

    /** Pozycja odtwarzania w mikrosekundach (do synchronizacji napisów). */
    static long microsecondPosition() {
        return clip != null ? clip.getMicrosecondPosition() : 0L;
    }

    /** Całkowita długość utworu w mikrosekundach (do synchronizacji napisów). */
    static long microsecondLength() {
        return clip != null ? clip.getMicrosecondLength() : 0L;
    }

    /** Przewija odtwarzanie do podanej liczby sekund (suwak postępu w GUI). */
    static void seekTo(float seconds) {
        if (clip == null) return;
        long micros = Math.max(0L, (long) (seconds * 1_000_000L));
        micros = Math.min(micros, clip.getMicrosecondLength());
        clip.setMicrosecondPosition(micros);
        if (paused) pauseFrame = clip.getLongFramePosition();
    }
}
