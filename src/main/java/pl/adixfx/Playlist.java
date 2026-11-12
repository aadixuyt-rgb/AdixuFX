package pl.adixfx;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lista metadanych dla opcjonalnej paczki muzycznej. Pliki OGG znajdują się poza JAR-em
 * w config/adixfx/music; utwory importowane przez użytkownika trafiają do tego samego folderu.
 */
public final class Playlist {
    public record Track(String id, String name, Identifier sound, String customFile) {
        public Track(String id, String name, Identifier sound) {
            this(id, name, sound, null);
        }

        /** true = zewnętrzny plik audio (odtwarzany przez CustomPlayer poza zasobami Minecrafta). */
        public boolean isCustom() {
            return customFile != null;
        }
    }

    public static final List<Track> TRACKS = List.of(
            new Track("zelenuyu", "ZELENUYU (slowed)", Identifier.of("adixfx", "zelenuyu")),
                        new Track("starships", "Starships (Nicki Minaj)", Identifier.of("adixfx", "starships")),
                        new Track("hellblade_super", "ZELENUYU HELLBLADE (super slowed)", Identifier.of("adixfx", "hellblade_super")),
                        new Track("hellblade_ultra", "HELLBLADE ZELENUYU (ultra slowed)", Identifier.of("adixfx", "hellblade_ultra")),
                        new Track("my_destiny", "My Destiny (slowed)", Identifier.of("adixfx", "my_destiny")),
                        new Track("musor_drop", "МУСОР DROP", Identifier.of("adixfx", "musor_drop")),
                        new Track("zani_preparaty", "Зани препараты", Identifier.of("adixfx", "zani_preparaty")),
                        new Track("dark_triad", "Заны Заны Препараты (krak3 | The Dark Triad)", Identifier.of("adixfx", "dark_triad")),
                        new Track("misery", "Misery", Identifier.of("adixfx", "misery")),
                        new Track("meant_to_be", "Meant To Be (HardTekk, slowed + reverb)", Identifier.of("adixfx", "meant_to_be")),
                        new Track("sexyback", "SexyBack (HardTekk TikTok, slowed)", Identifier.of("adixfx", "sexyback")),
                        new Track("calabria", "Calabria 2007 (HardTekk)", Identifier.of("adixfx", "calabria")),
                        new Track("sometimes", "Sometimes (HardTekk TikTok)", Identifier.of("adixfx", "sometimes")),
                        new Track("so_lucky", "I'm So Lucky (jumpstyle, super slowed)", Identifier.of("adixfx", "so_lucky")),
                        new Track("love_me_not", "Love Me Not (Ravyn Lenae)", Identifier.of("adixfx", "love_me_not")),
            new Track("lush_life", "Lush Life (Zara Larsson)", Identifier.of("adixfx", "lush_life")),
            new Track("gimme_more", "Gimme More (Britney Spears)", Identifier.of("adixfx", "gimme_more")),
            new Track("on_the_floor", "On The Floor (Jennifer Lopez ft. Pitbull)", Identifier.of("adixfx", "on_the_floor")),
            new Track("danza_kuduro", "Danza Kuduro (Don Omar ft. Lucenzo)", Identifier.of("adixfx", "danza_kuduro")),
            new Track("oops_again", "Oops!... I Did It Again (Britney Spears)", Identifier.of("adixfx", "oops_again")),
            new Track("calabria_slowed", "Calabria 2007 (HardTekk, slowed)", Identifier.of("adixfx", "calabria_slowed")),
            new Track("sm_rihanna", "S&M (Rihanna)", Identifier.of("adixfx", "sm_rihanna")),
            new Track("toca_toca", "Toca Toca (Fly Project)", Identifier.of("adixfx", "toca_toca"))
            // kolejne: new Track("id", "Nazwa", Identifier.of("adixfx", "nazwa_pliku")),
    );

    private Playlist() {}

    /** nazwa wyświetlana (własna, jeśli ją zmieniłeś) */
    public static String displayName(Track t) {
        String n = Config.d.names.get(t.id());
        return n == null || n.isBlank() ? t.name() : n;
    }

    public static void setName(Track t, String name) {
        if (name == null || name.isBlank() || name.equals(t.name())) Config.d.names.remove(t.id());
        else Config.d.names.put(t.id(), name.trim());
        Config.save();
    }

    /** głośność utworu 0..2 (domyślnie 1 = 100%) */
    public static float volume(Track t) {
        return Config.d.trackVolume.getOrDefault(t.id(), 1.0f);
    }

    public static void setVolume(Track t, float v) {
        Config.d.trackVolume.put(t.id(), Math.max(0f, Math.min(2f, v)));
    }

    public static boolean enabled(Track t) {
        return Config.d.tracks.getOrDefault(t.id(), true);
    }

    public static void setEnabled(Track t, boolean v) {
        Config.d.tracks.put(t.id(), v);
        Config.save();
    }

    /** Pliki z opcjonalnej paczki muzycznej oraz utwory dodane ręcznie. */
    public static List<Track> allTracks() {
        List<Track> out = new ArrayList<>();
        Path musicFolder = MusicLibrary.folder();
        for (Track builtin : TRACKS) {
            if (Config.d.removedBuiltins.contains(builtin.id())) continue;
            Path file = musicFolder.resolve(builtin.id() + ".ogg");
            if (Files.isRegularFile(file)) {
                out.add(new Track(builtin.id(), builtin.name(), null, file.toString()));
            }
        }
        for (CustomTracks.Entry entry : Config.d.customTracks) {
            if (!CustomTracks.READY.equals(entry.status) || entry.file == null) continue;
            if (!Files.isRegularFile(Path.of(entry.file))) continue;
            out.add(new Track(entry.id, entry.name, null, entry.file));
        }
        return out;
    }

    public static List<Track> enabledTracks() {
        List<Track> out = new ArrayList<>();
        for (Track t : allTracks()) if (enabled(t)) out.add(t);
        return out;
    }

    /**
     * Usuwa utwór z playlisty. Importowany plik jest kasowany z folderu muzyki;
     * plik z paczki jest ukrywany w konfiguracji, ale zostaje na dysku.
     */
    public static void remove(Track t) {
        boolean packTrack = TRACKS.stream().anyMatch(builtin -> builtin.id().equals(t.id()));
        if (packTrack) {
            Config.d.removedBuiltins.add(t.id());
        } else {
            CustomTracks.remove(t.id());
        }
        Config.d.names.remove(t.id());
        Config.d.trackVolume.remove(t.id());
        Config.d.tracks.remove(t.id());
        Config.save();
    }
}
