package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Odtwarzacz: po końcu utworu startuje losowy WŁĄCZONY utwór (nie po kolei). */
public final class Music {
    private static final Random RND = new Random();

    private static SoundInstance current;
    private static Playlist.Track currentTrack;
    private static Playlist.Track last;
    private static boolean continuous;
    private static int age;
    private static boolean seen;
    /** true = utwór jest zatrzymany (pauza) i czeka na "Play" w tym samym miejscu. */
    private static boolean paused;

    /** Wszystkie kategorie dźwięku oprócz tej, w której gra nasz utwór - do pauzowania tylko jego. */
    private static final SoundCategory[] ALL_BUT_OURS = Arrays.stream(SoundCategory.values())
            .filter(c -> c != TrackSound.CATEGORY)
            .toArray(SoundCategory[]::new);

    private Music() {}

    public static Playlist.Track playing() {
        return current == null ? null : currentTrack;
    }

    /** true, gdy utwór gra (nie jest zatrzymany). */
    public static boolean isPlayingNow() {
        return current != null && !paused;
    }

    public static boolean isPaused() {
        return current != null && paused;
    }

    /**
     * Przycisk Stop/Play: gdy coś gra - zatrzymuje utwór w miejscu (pauza);
     * gdy utwór jest zatrzymany - wznawia ten sam utwór od tego samego momentu;
     * gdy nic nie było odtwarzane - startuje ostatni utwór (albo losowy).
     */
    public static void togglePlayPause() {
        if (!Config.d.music) return;
        if (current == null) {
            if (last != null && Playlist.TRACKS.contains(last)) play(last);
            else playRandom();
            return;
        }
        if (paused) resume();
        else pause();
    }

    private static void pause() {
        paused = true;
        MinecraftClient.getInstance().getSoundManager().pauseAllExcept(ALL_BUT_OURS);
    }

    private static void resume() {
        paused = false;
        MinecraftClient.getInstance().getSoundManager().resumeAll();
    }

    /** Losowy utwór spośród włączonych (nie ten sam co ostatnio, jeśli jest wybór). */
    public static void playRandom() {
        if (!Config.d.music) return;
        List<Playlist.Track> list = Playlist.enabledTracks();
        if (list.isEmpty()) return;
        Playlist.Track pick = list.get(RND.nextInt(list.size()));
        if (list.size() > 1) {
            int guard = 0;
            while (pick == last && guard++ < 20) pick = list.get(RND.nextInt(list.size()));
        }
        play(pick);
    }

    /** Pomija obecny utwór i losuje następny spośród włączonych. */
    public static void skip() {
        playRandom();
    }

    public static void play(Playlist.Track t) {
        MinecraftClient mc = MinecraftClient.getInstance();
        stop();
        last = t;
        if (gain(t) <= 0f) return;
        continuous = true;
        paused = false;
        currentTrack = t;
        age = 0;
        seen = false;
        current = new TrackSound(t);
        mc.getSoundManager().play(current);
    }

    /**
     * Końcowa głośność utworu (0..1), liczona na bieżąco z suwaków.
     * Minecraft ogranicza głośność do 100% mocy, więc 100% w GUI = połowa mocy,
     * a 200% = pełna moc - dzięki temu ustawienia powyżej 100% naprawdę coś zmieniają.
     */
    public static float gain(Playlist.Track t) {
        return Math.max(0f, Math.min(1f, Config.d.volume * Playlist.volume(t) * 0.5f));
    }

    public static void stop() {
        continuous = false;
        paused = false;
        if (current instanceof TrackSound ts) ts.finish();
        if (current != null) {
            MinecraftClient.getInstance().getSoundManager().stop(current);
        }
        current = null;
        currentTrack = null;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || !Config.d.music) {
            if (current != null) stop();
            return;
        }
        if (current == null) return;
        if (paused) {
            // wyjście z menu pauzy gry wznawia wszystkie dźwięki - zatrzymujemy nasz utwór ponownie
            mc.getSoundManager().pauseAllExcept(ALL_BUT_OURS);
            return;
        }
        age++;
        boolean playing = mc.getSoundManager().isPlaying(current);
        if (playing) seen = true;
        if ((seen && !playing) || (!seen && age > 100)) {
            current = null;
            currentTrack = null;
            if (continuous) playRandom();
        }
    }
}
