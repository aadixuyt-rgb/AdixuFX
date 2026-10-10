package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;

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

    private Music() {}

    public static Playlist.Track playing() {
        return current == null ? null : currentTrack;
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
        // Minecraft ogranicza końcową głośność do 100%, więc 100% w GUI = połowa mocy,
        // a 200% = pełna moc - dzięki temu ustawienia powyżej 100% naprawdę coś zmieniają.
        float gain = Math.max(0f, Math.min(1f, Config.d.volume * Playlist.volume(t) * 0.5f));
        last = t;
        if (gain <= 0f) return;
        continuous = true;
        currentTrack = t;
        age = 0;
        seen = false;
        current = PositionedSoundInstance.master(SoundEvent.of(t.sound()), 1.0f, gain);
        mc.getSoundManager().play(current);
    }

    public static void stop() {
        continuous = false;
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
