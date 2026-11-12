package pl.adixfx;

import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Playlist metadata. All playable OGG files are read from config/adixfx/music;
 * no music files are loaded from the mod JAR.
 */
public final class Playlist {
    public record Track(String id, String name, Identifier sound, String customFile) {
        public Track(String id, String name, Identifier sound) {
            this(id, name, sound, null);
        }

        /** true = external audio file played by CustomPlayer. */
        public boolean isCustom() {
            return customFile != null;
        }
    }

    /** Metadata for the tracks from the separate AdixFX music pack (23 OGG files). */
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
    );

    private Playlist() {}

    /** Display name (custom name wins when set). */
    public static String displayName(Track track) {
        String name = Config.d.names.get(track.id());
        return name == null || name.isBlank() ? track.name() : name;
    }

    public static void setName(Track track, String name) {
        if (name == null || name.isBlank() || name.equals(track.name())) Config.d.names.remove(track.id());
        else Config.d.names.put(track.id(), name.trim());
        Config.save();
    }

    /** Per-track volume, 0..2 (default 1 = 100%). */
    public static float volume(Track track) {
        return Config.d.trackVolume.getOrDefault(track.id(), 1.0f);
    }

    public static void setVolume(Track track, float value) {
        Config.d.trackVolume.put(track.id(), Math.max(0f, Math.min(2f, value)));
    }

    public static boolean enabled(Track track) {
        return Config.d.tracks.getOrDefault(track.id(), true);
    }

    public static void setEnabled(Track track, boolean value) {
        Config.d.tracks.put(track.id(), value);
        Config.save();
    }

    /**
     * Loads the 23 music-pack tracks, imported tracks and any extra .ogg files
     * directly from config/adixfx/music.
     */
    public static List<Track> allTracks() {
        List<Track> out = new ArrayList<>();
        Set<Path> loadedFiles = new HashSet<>();
        Path musicFolder = MusicLibrary.folder();

        // Keep the familiar names and order for the 23 tracks from the separate music pack.
        for (Track builtin : TRACKS) {
            if (Config.d.removedBuiltins.contains(builtin.id())) continue;
            Path file = musicFolder.resolve(builtin.id() + ".ogg");
            if (Files.isRegularFile(file)) {
                out.add(new Track(builtin.id(), builtin.name(), null, file.toString()));
                loadedFiles.add(file.toAbsolutePath().normalize());
            }
        }

        // Entries imported in the GUI. This also keeps compatibility with older .wav imports.
        for (CustomTracks.Entry entry : Config.d.customTracks) {
            if (entry == null || !CustomTracks.READY.equals(entry.status) || entry.file == null) continue;
            try {
                Path file = Path.of(entry.file);
                if (Files.isRegularFile(file) && loadedFiles.add(file.toAbsolutePath().normalize())) {
                    String id = entry.id == null || entry.id.isBlank()
                            ? removeExtension(file.getFileName().toString()) : entry.id;
                    String name = entry.name == null || entry.name.isBlank()
                            ? readableName(id) : entry.name;
                    out.add(new Track(id, name, null, file.toString()));
                }
            } catch (RuntimeException ignored) {
            }
        }

        // Also accept OGG files copied manually into the music folder, without a config entry.
        try (Stream<Path> files = Files.list(musicFolder)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .sorted(Comparator.comparing(file -> file.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .forEach(file -> {
                        String id = removeExtension(file.getFileName().toString());
                        if (Config.d.removedBuiltins.contains(id)) return;
                        Path normalized = file.toAbsolutePath().normalize();
                        if (!loadedFiles.add(normalized)) return;
                        out.add(new Track(id, readableName(id), null, file.toString()));
                    });
        } catch (IOException ignored) {
        }

        return out;
    }

    private static String removeExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String readableName(String id) {
        String name = id.replace('_', ' ').replace('-', ' ').trim();
        if (name.isEmpty()) return "Nowy utwór";
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public static List<Track> enabledTracks() {
        List<Track> out = new ArrayList<>();
        for (Track track : allTracks()) if (enabled(track)) out.add(track);
        return out;
    }

    /** Removes an imported track or hides a track from the external music pack. */
    public static void remove(Track track) {
        boolean packTrack = TRACKS.stream().anyMatch(builtin -> builtin.id().equals(track.id()));
        if (packTrack) {
            Config.d.removedBuiltins.add(track.id());
        } else {
            CustomTracks.remove(track.id());
        }
        Config.d.names.remove(track.id());
        Config.d.trackVolume.remove(track.id());
        Config.d.tracks.remove(track.id());
        Config.save();
    }
}
