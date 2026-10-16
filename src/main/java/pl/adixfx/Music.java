package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.Source;
import pl.adixfx.mixin.SoundManagerAccessor;
import pl.adixfx.mixin.SoundSystemAccessor;

import java.util.List;
import java.util.Random;

/**
 * Odtwarzacz: po końcu utworu startuje losowy WŁĄCZONY utwór (nie po kolei).
 * Wbudowane utwory gra silnik dźwięku Minecrafta (TrackSound); własne, przeciągnięte pliki
 * gra osobny CustomPlayer (javax.sound) - patrz Playlist.Track.isCustom().
 */
public final class Music {
    private static final Random RND = new Random();

    // silnik wbudowany (Minecraft SoundManager)
    private static SoundInstance builtinSound;
    private static int age;
    private static boolean seen;

    private static Playlist.Track currentTrack;
    private static Playlist.Track last;
    private static boolean continuous;
    /** true = utwór jest zatrzymany (pauza) i czeka na "Play" w tym samym miejscu. */
    private static boolean paused;

    private Music() {}

    public static Playlist.Track playing() {
        return currentTrack;
    }

    /** true, gdy utwór gra (nie jest zatrzymany). */
    public static boolean isPlayingNow() {
        return currentTrack != null && !paused;
    }

    public static boolean isPaused() {
        return currentTrack != null && paused;
    }

    /**
     * Przycisk Stop/Play: gdy coś gra - zatrzymuje utwór w miejscu (pauza);
     * gdy utwór jest zatrzymany - wznawia ten sam utwór od tego samego momentu;
     * gdy nic nie było odtwarzane - startuje ostatni utwór (albo losowy).
     */
    public static void togglePlayPause() {
        if (!Config.d.music) return;
        if (currentTrack == null) {
            if (last != null && Playlist.allTracks().contains(last)) play(last);
            else playRandom();
            return;
        }
        if (paused) resume();
        else pause();
    }

    /** Źródło dźwięku (OpenAL) aktualnie granego wbudowanego utworu - do pauzy/wznowienia tylko jego. */
    private static Channel.SourceManager sourceOf(SoundInstance sound) {
        try {
            Object manager = MinecraftClient.getInstance().getSoundManager();
            if (sound == null || !(manager instanceof SoundManagerAccessor accessor)) return null;
            SoundSystem system = accessor.adixfx$getSoundSystem();
            if (!(system instanceof SoundSystemAccessor sources)) return null;
            return sources.adixfx$getSources().get(sound);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void pause() {
        if (currentTrack == null) return;
        if (currentTrack.isCustom()) {
            CustomPlayer.pause();
            paused = true;
            return;
        }
        Channel.SourceManager source = sourceOf(builtinSound);
        if (source != null) {
            paused = true;
            source.run(Source::pause);
        } else {
            // brak dostępu do silnika dźwięku - zatrzymujemy utwór, a Play zacznie go od początku
            Playlist.Track t = currentTrack;
            stop();
            last = t;
        }
    }

    /**
     * Woła ją mixin SoundManagerResumeMixin dokładnie w momencie, gdy Minecraft (np. przy wyjściu
     * z ekwipunku w singleplayerze) sam wznawia wszystkie dźwięki - jeśli nasz utwór miał być
     * zatrzymany, zatrzymujemy go z powrotem od razu, zanim zdąży coś zabrzmieć.
     */
    public static void reassertPauseIfNeeded() {
        if (currentTrack != null && paused && !currentTrack.isCustom()) {
            Channel.SourceManager source = sourceOf(builtinSound);
            if (source != null) source.run(Source::pause);
        }
    }

    private static void resume() {
        if (currentTrack == null) return;
        paused = false;
        if (currentTrack.isCustom()) {
            CustomPlayer.resume();
            return;
        }
        Channel.SourceManager source = sourceOf(builtinSound);
        if (source != null) source.run(Source::resume);
    }

    /** Losowy utwór spośród włączonych (nie ten sam co ostatnio, jeśli jest wybór). */
    public static void playRandom() {
        if (!Config.d.music) return;
        List<Playlist.Track> list = Playlist.enabledTracks();
        if (list.isEmpty()) return;
        Playlist.Track pick = list.get(RND.nextInt(list.size()));
        if (list.size() > 1) {
            int guard = 0;
            while (pick.equals(last) && guard++ < 20) pick = list.get(RND.nextInt(list.size()));
        }
        play(pick);
    }

    /** Pomija obecny utwór i losuje następny spośród włączonych. */
    public static void skip() {
        playRandom();
    }

    public static void play(Playlist.Track t) {
        stop();
        last = t;
        if (gain(t) <= 0f) return;
        continuous = true;
        paused = false;
        currentTrack = t;
        if (t.isCustom()) {
            CustomPlayer.play(t);
        } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            age = 0;
            seen = false;
            builtinSound = new TrackSound(t);
            mc.getSoundManager().play(builtinSound);
        }
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
        if (currentTrack != null && currentTrack.isCustom()) {
            CustomPlayer.stop();
        } else if (builtinSound != null) {
            if (builtinSound instanceof TrackSound ts) ts.finish();
            MinecraftClient.getInstance().getSoundManager().stop(builtinSound);
        }
        builtinSound = null;
        currentTrack = null;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || !Config.d.music) {
            if (currentTrack != null) stop();
            return;
        }
        if (currentTrack == null) return;

        if (currentTrack.isCustom()) {
            if (paused) return;
            CustomPlayer.tick();
            if (CustomPlayer.finished()) {
                currentTrack = null;
                if (continuous) playRandom();
            }
            return;
        }

        if (paused) {
            // wyjście z menu pauzy gry wznawia wszystkie dźwięki - zatrzymujemy nasz utwór ponownie
            Channel.SourceManager source = sourceOf(builtinSound);
            if (source != null) source.run(Source::pause);
            return;
        }
        age++;
        boolean playing = mc.getSoundManager().isPlaying(builtinSound);
        if (playing) seen = true;
        if ((seen && !playing) || (!seen && age > 100)) {
            builtinSound = null;
            currentTrack = null;
            if (continuous) playRandom();
        }
    }
}
