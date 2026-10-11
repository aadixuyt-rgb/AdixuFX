package pl.adixfx;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista utworów. Żeby dodać nowy: wrzuć plik .ogg do assets/adixfx/sounds/,
 * dopisz go w sounds.json i dodaj wpis poniżej.
 */
public final class Playlist {
    public record Track(String id, String name, Identifier sound) {}

    public static final List<Track> TRACKS = new ArrayList<>(List.of(
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
            new Track("napalona", "NAPALONA (SxK, DJ SKIBA remix)", Identifier.of("adixfx", "napalona")),
            new Track("gimme_more", "Gimme More (Britney Spears)", Identifier.of("adixfx", "gimme_more")),
            new Track("on_the_floor", "On The Floor (Jennifer Lopez ft. Pitbull)", Identifier.of("adixfx", "on_the_floor")),
            new Track("danza_kuduro", "Danza Kuduro (Don Omar ft. Lucenzo)", Identifier.of("adixfx", "danza_kuduro")),
            new Track("oops_again", "Oops!... I Did It Again (Britney Spears)", Identifier.of("adixfx", "oops_again")),
            new Track("calabria_slowed", "Calabria 2007 (HardTekk, slowed)", Identifier.of("adixfx", "calabria_slowed")),
            new Track("sm_rihanna", "S&M (Rihanna)", Identifier.of("adixfx", "sm_rihanna")),
            new Track("toca_toca", "Toca Toca (Fly Project)", Identifier.of("adixfx", "toca_toca"))
            // kolejne: new Track("id", "Nazwa", Identifier.of("adixfx", "nazwa_pliku")),
    ));

    static {
        try {
            java.nio.file.Path dir = net.minecraft.client.MinecraftClient.getInstance().runDirectory.toPath()
                    .resolve("resourcepacks/adixfx_imported/assets/adixfx/sounds/imported");
            if (java.nio.file.Files.isDirectory(dir)) {
                try (var files = java.nio.file.Files.list(dir)) {
                    files.filter(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".ogg"))
                            .sorted().forEach(p -> {
                                String id = p.getFileName().toString().replaceFirst("(?i)\\.ogg$", "");
                                String trackId = "imported_" + id;
                                if (TRACKS.stream().noneMatch(t -> t.id().equals(trackId)))
                                    TRACKS.add(new Track(trackId, MusicImporter.cleanTitle(id), Identifier.of("adixfx", "imported/" + id)));
                            });
                }
            }
        } catch (Exception ignored) { }
    }

    public static void addImported(Track track) {
        if (TRACKS.stream().noneMatch(t -> t.id().equals(track.id()))) TRACKS.add(track);
    }

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

    public static List<Track> enabledTracks() {
        List<Track> out = new ArrayList<>();
        for (Track t : TRACKS) if (enabled(t)) out.add(t);
        return out;
    }
}
