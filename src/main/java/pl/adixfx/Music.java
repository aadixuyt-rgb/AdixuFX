package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.Source;
import pl.adixfx.mixin.SoundManagerAccessor;
import pl.adixfx.mixin.SoundSystemAccessor;

import java.util.ArrayDeque;
import java.util.Deque;
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
    private static final Deque<Playlist.Track> history = new ArrayDeque<>();
    private static final int MAX_HISTORY = 100;
    private static boolean continuous;
    /** true = utwór jest zatrzymany (pauza) i czeka na "Play" w tym samym miejscu. */
    private static boolean paused;

    private Music() {}

    public static Playlist.Track playing() {
        return currentTrack;
    }

    /** Ile sekund gra obecny utwór (0, jeśli nic nie gra). Do paska postępu w GUI. */
    public static float elapsedSeconds() {
        if (currentTrack == null) return 0f;
        if (currentTrack.isCustom()) return CustomPlayer.microsecondPosition() / 1_000_000f;
        return age / 20f; // age liczy tylko tiki, gdy utwór faktycznie gra (patrz tick())
    }

    /** Całkowita długość obecnego utworu w sekundach (0, jeśli nieznana albo nic nie gra). */
    public static float totalSeconds() {
        if (currentTrack == null) return 0f;
        if (currentTrack.isCustom()) return CustomPlayer.microsecondLength() / 1_000_000f;
        float analyzed = BeatSync.durationSeconds();
        return analyzed > 0f ? analyzed : TrackDurations.get(currentTrack.id());
    }

    /**
     * Czy suwak postępu może przewijać obecny utwór. Własne (przeciągnięte) pliki - tak, bo gra
     * je CustomPlayer. Wbudowane - nie, bo silnik dźwięku Minecrafta nie pozwala łatwo przeskoczyć
     * w czasie trwania dźwięku; pasek wtedy tylko pokazuje postęp, nie da się go przeciągnąć.
     */
    public static boolean isSeekable() {
        return currentTrack != null && currentTrack.isCustom();
    }

    /** Przewija obecny (własny) utwór do podanej sekundy. Nic nie robi dla wbudowanych utworów. */
    public static void seekTo(float seconds) {
        if (currentTrack == null || !currentTrack.isCustom()) return;
        CustomPlayer.seekTo(Math.max(0f, seconds));
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

    private static void remember(Playlist.Track track) {
        if (track == null) return;
        if (history.isEmpty() || !history.peekLast().equals(track)) history.addLast(track);
        while (history.size() > MAX_HISTORY) history.removeFirst();
    }

    /** Losowy utwór spośród włączonych, bez natychmiastowego powtarzania poprzedniego. */
    public static void playRandom() {
        if (!Config.d.music) return;
        List<Playlist.Track> enabled = Playlist.enabledTracks();
        if (enabled.isEmpty()) return;

        Playlist.Track avoid = currentTrack != null ? currentTrack : last;
        List<Playlist.Track> choices = enabled.stream()
                .filter(track -> enabled.size() == 1 || !track.equals(avoid))
                .toList();
        Playlist.Track pick = choices.get(RND.nextInt(choices.size()));
        playInternal(pick, true);
    }

    /** Odtwórz poprzedni utwór z historii. Każde kliknięcie cofa o jeden, bez ping-ponga A/B. */
    public static void previous() {
        if (!Config.d.music) return;
        List<Playlist.Track> enabled = Playlist.enabledTracks();
        while (!history.isEmpty()) {
            Playlist.Track target = history.removeLast();
            if (enabled.contains(target)) {
                playInternal(target, false);
                return;
            }
        }
        if (currentTrack != null) playInternal(currentTrack, false);
        else if (last != null && enabled.contains(last)) playInternal(last, false);
        else playRandom();
    }

    /** Pomija obecny utwór i losuje następny spośród włączonych. */
    public static void skip() {
        playRandom();
    }

    public static void play(Playlist.Track t) {
        playInternal(t, true);
    }

    private static void playInternal(Playlist.Track t, boolean rememberCurrent) {
        if (t == null) return;
        Playlist.Track old = currentTrack;
        boolean wasCurrent = old != null && old.equals(t);
        if (rememberCurrent && old != null && !wasCurrent) remember(old);

        stopPlaybackOnly();
        last = t;
        if (gain(t) <= 0f) return;

        continuous = true;
        paused = false;
        currentTrack = t;
        BeatSync.prepare(t);
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

    private static void stopPlaybackOnly() {
        BeatSync.reset();
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

    public static void stop() {
        stopPlaybackOnly();
        history.clear();
        last = null;
    }

    private static void advanceAfterEnd() {
        Playlist.Track ended = currentTrack;
        boolean advance = continuous;
        stopPlaybackOnly();
        remember(ended);
        if (advance) playRandom();
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
                advanceAfterEnd();
                AdixFxScreen.refreshIfOpen();
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
        if ((seen && !playing) || (!seen && age > 200)) {
            advanceAfterEnd();
            AdixFxScreen.refreshIfOpen();
        }
    }
}
