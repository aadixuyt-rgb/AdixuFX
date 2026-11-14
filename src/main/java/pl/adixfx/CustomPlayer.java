package pl.adixfx;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Silnik grania własnych utworów. Wczytywanie, dekodowanie OGG i otwieranie
 * urządzenia audio odbywa się poza wątkiem gry, aby zmiana piosenki nie blokowała FPS.
 */
final class CustomPlayer {
    private static final AtomicLong GENERATION = new AtomicLong();

    private static Clip clip;
    private static Playlist.Track track;
    private static boolean paused;
    private static boolean loading;
    private static long pauseFrame;

    private CustomPlayer() {}

    static synchronized boolean isPaused() {
        return paused;
    }

    static void play(Playlist.Track t) {
        stop();
        if (t == null) return;

        final long generation = GENERATION.get();
        synchronized (CustomPlayer.class) {
            track = t;
            paused = false;
            loading = true;
            pauseFrame = 0;
        }

        Thread worker = new Thread(() -> loadAndStart(t, generation),
                "AdixFX-MusicLoader");
        worker.setDaemon(true);
        worker.start();
    }

    private static void loadAndStart(Playlist.Track t, long generation) {
        Path temporaryWav = null;
        Clip newClip = null;
        try {
            Path source = Path.of(t.customFile());
            File audioFile = source.toFile();
            if (source.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".ogg")) {
                temporaryWav = OggAudio.decodeToTemporaryWav(source);
                audioFile = temporaryWav.toFile();
            }

            newClip = AudioSystem.getClip();
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile)) {
                newClip.open(ais);
            }

            synchronized (CustomPlayer.class) {
                if (GENERATION.get() != generation) {
                    newClip.close();
                    return;
                }

                clip = newClip;
                loading = false;
                applyVolumeLocked();
                if (!paused) newClip.start();
            }
        } catch (Exception e) {
            synchronized (CustomPlayer.class) {
                if (GENERATION.get() == generation) {
                    loading = false;
                    if (clip != null) clip.close();
                    clip = null;
                    track = null;
                    paused = false;
                }
            }
            System.err.println("[AdixFX] Nie udało się uruchomić utworu: "
                    + t.id() + " (" + e.getMessage() + ")");
            if (newClip != null && newClip != clip) newClip.close();
        } finally {
            if (temporaryWav != null) {
                try { Files.deleteIfExists(temporaryWav); } catch (Exception ignored) {}
            }
        }
    }

    static synchronized void pause() {
        paused = true;
        if (clip != null && clip.isRunning()) {
            pauseFrame = clip.getLongFramePosition();
            clip.stop();
        }
    }

    static synchronized void resume() {
        paused = false;
        if (clip != null) {
            clip.setFramePosition((int) Math.min(pauseFrame, Integer.MAX_VALUE));
            clip.start();
        }
        // Jeśli plik nadal się wczytuje, worker uruchomi go po zakończeniu.
    }

    static synchronized void stop() {
        GENERATION.incrementAndGet();
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
        loading = false;
        pauseFrame = 0;
    }

    static synchronized void applyVolume() {
        applyVolumeLocked();
    }

    private static void applyVolumeLocked() {
        if (clip == null || track == null) return;
        try {
            FloatControl ctrl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float gain = Music.gain(track);
            float db = gain <= 0.0001f ? ctrl.getMinimum() : (float) (20.0 * Math.log10(gain));
            db = Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), db));
            ctrl.setValue(db);
        } catch (Exception ignored) {
            // Niektóre linie audio nie obsługują regulacji głośności.
        }
    }

    /** true, gdy utwór doszedł do końca sam (nie przez pauzę/stop). */
    static synchronized boolean finished() {
        return !loading && clip != null && !paused && !clip.isRunning()
                && clip.getLongFramePosition() >= clip.getFrameLength();
    }

    static synchronized void tick() {
        if (!paused && !loading) applyVolumeLocked();
    }

    /** Pozycja odtwarzania w mikrosekundach (do synchronizacji napisów). */
    static synchronized long microsecondPosition() {
        return clip != null ? clip.getMicrosecondPosition() : 0L;
    }

    /** Całkowita długość utworu w mikrosekundach (do synchronizacji napisów). */
    static synchronized long microsecondLength() {
        return clip != null ? clip.getMicrosecondLength() : 0L;
    }

    /** Przewija odtwarzanie do podanej liczby sekund (suwak postępu w GUI). */
    static synchronized void seekTo(float seconds) {
        if (clip == null) return;
        long micros = Math.max(0L, (long) (seconds * 1_000_000L));
        micros = Math.min(micros, clip.getMicrosecondLength());
        clip.setMicrosecondPosition(micros);
        if (paused) pauseFrame = clip.getLongFramePosition();
    }
}
