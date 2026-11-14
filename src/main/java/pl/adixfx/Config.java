package pl.adixfx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class Config {
    public enum Mode {
        STATIC("Jeden kolor (losowy, stały)"),
        CYCLE("Jeden kolor (zmienia się)"),
        MULTI("Wiele kolorów naraz (zmieniają się)"),
        PULSE("Pulse with Music");

        public final String label;
        Mode(String label) { this.label = label; }
        public Mode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public static final class Data {
        // hitboxy
        public boolean hitboxes = true;
        /** Kolorowe błyskawice podczas burzy. */
        public boolean stormLightning = true;
        /** Chmury burzowe nad graczem. */
        public boolean stormClouds = true;
        /** Błyskawica po zabiciu innego gracza. */
        public boolean killLightning = true;
        /** Osobny przełącznik małych świateł reagujących na muzykę. */
        public boolean musicLights = true;
        /** Półprzezroczyste szklane kostki zamiast kryształów. */
        public boolean musicLightsGlass = false;
        /** Śnieg reagujący wizualnie na poziom muzyki. */
        public boolean snowEnabled = false;
        /** 1 = mała, 2 = średnia, 3 = duża śnieżyca. */
        public int snowIntensity = 2;
        public String mode = Mode.MULTI.name();
        public int fixedColor = 0;
        public int pulseIdleColor = 0xFF000000;            // 0 = jeszcze nie wylosowany
        public float speed = 1.0f;            // szybkość zmiany kolorów
        public int range = 64;
        public boolean players = true, hostile = true, passive = true, items = false, others = false;
        // muzyka
        public boolean music = true;
        /** Pokazuje nazwę aktualnie odtwarzanego utworu u góry ekranu. */
        public boolean songHud = true;
        /** Pozycja i skala HUD-u muzyki, ustawiane przez przeciąganie w trybie edycji. */
        public float songHudOffsetX = 0.0f;
        public float songHudOffsetY = 0.0f;
        public float songHudScale = 1.0f;
        /** Indeks koloru akcentu GUI. */
        public int guiColor = 0;
        /** Szybkość przewijania długich nazw utworów w pikselach na sekundę. */
        public float titleScrollSpeed = 22.0f;
        /** głośność ogólna 0..2 (0-200%) */
        public float volume = 1.0f;
        /** głośność poszczególnych utworów 0..2 */
        public Map<String, Float> trackVolume = new HashMap<>();
        /** własne nazwy utworów */
        public Map<String, String> names = new HashMap<>();
        public Map<String, Boolean> tracks = new HashMap<>();
        /** własne (przeciągnięte) utwory */
        public List<CustomTracks.Entry> customTracks = new ArrayList<>();
        /** id utworów z osobnej paczki muzycznej ukrytych z playlisty (plik zostaje w config/adixfx/music). */
        public Set<String> removedBuiltins = new HashSet<>();
        /** włącznik napisów na ekranie, per utwór (domyślnie włączone, jeśli utwór je ma) */
        public Map<String, Boolean> lyricsOn = new HashMap<>();
    }

    public static Data d = new Data();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("adixfx.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                JsonObject saved = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                Data r = GSON.fromJson(saved, Data.class);
                if (r != null) {
                    // Starsze pliki config nie mają nowych opcji. Gson ustawia wtedy boolean na false,
                    // więc jawnie przywracamy ich wartości domyślne tylko dla brakujących pól.
                    if (!saved.has("stormClouds")) r.stormClouds = true;
                    if (!saved.has("stormLightning")) r.stormLightning = true;
                    if (!saved.has("killLightning")) r.killLightning = true;
                    if (!saved.has("musicLights")) r.musicLights = true;
                    if (!saved.has("snowEnabled")) r.snowEnabled = false;
                    if (!saved.has("snowIntensity")) r.snowIntensity = 2;
                    d = r;
                }
            }
        } catch (Exception ignored) {
        }
        if (d.tracks == null) d.tracks = new HashMap<>();
        if (d.trackVolume == null) d.trackVolume = new HashMap<>();
        if (d.names == null) d.names = new HashMap<>();
        if (d.customTracks == null) d.customTracks = new ArrayList<>();
        if (d.removedBuiltins == null) d.removedBuiltins = new HashSet<>();
        if (d.lyricsOn == null) d.lyricsOn = new HashMap<>();
        // wpisy "w trakcie dodawania" z poprzedniej (np. przerwanej) sesji na pewno już nie dokończą się same
        for (CustomTracks.Entry e : d.customTracks) {
            if (CustomTracks.ADDING.equals(e.status)) e.status = CustomTracks.ERROR;
        }
        d.volume = Math.max(0f, Math.min(2f, d.volume));
        d.guiColor = Math.max(0, Math.min(5, d.guiColor));
        if (!Float.isFinite(d.songHudOffsetX)) d.songHudOffsetX = 0.0f;
        if (!Float.isFinite(d.songHudOffsetY)) d.songHudOffsetY = 0.0f;
        if (!Float.isFinite(d.songHudScale)) d.songHudScale = 1.0f;
        d.songHudScale = Math.max(0.45f, Math.min(3.0f, d.songHudScale));
        if (!Float.isFinite(d.titleScrollSpeed)) d.titleScrollSpeed = 22.0f;
        d.titleScrollSpeed = Math.max(6.0f, Math.min(60.0f, d.titleScrollSpeed));
        if (d.fixedColor == 0) randomizeColor();
        save();
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(d));
        } catch (IOException ignored) {
        }
    }

    private static Path profilesDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("adixfx").resolve("profiles");
    }

    public static boolean saveProfile(String name) {
        if (!name.matches("[A-Za-z0-9_-]{1,24}")) return false;
        try {
            Path dir = profilesDir();
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(name + ".json"), GSON.toJson(d));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean loadProfile(String name) {
        if (!name.matches("[A-Za-z0-9_-]{1,24}")) return false;
        try {
            Path file = profilesDir().resolve(name + ".json");
            if (!Files.exists(file)) return false;
            Data loaded = GSON.fromJson(Files.readString(file), Data.class);
            if (loaded == null) return false;
            // Profile nie nadpisuje playlisty, własnych plików ani ustawień odtwarzania.
            Data current = d;
            loaded.music = current.music;
            loaded.songHud = current.songHud;
            loaded.songHudOffsetX = current.songHudOffsetX;
            loaded.songHudOffsetY = current.songHudOffsetY;
            loaded.songHudScale = current.songHudScale;
            loaded.titleScrollSpeed = current.titleScrollSpeed;
            loaded.volume = current.volume;
            loaded.trackVolume = current.trackVolume;
            loaded.names = current.names;
            loaded.tracks = current.tracks;
            loaded.customTracks = current.customTracks;
            loaded.removedBuiltins = current.removedBuiltins;
            loaded.lyricsOn = current.lyricsOn;
            d = loaded;
            if (d.trackVolume == null) d.trackVolume = new HashMap<>();
            if (d.names == null) d.names = new HashMap<>();
            if (d.tracks == null) d.tracks = new HashMap<>();
            if (d.customTracks == null) d.customTracks = new ArrayList<>();
            if (d.removedBuiltins == null) d.removedBuiltins = new HashSet<>();
            if (d.lyricsOn == null) d.lyricsOn = new HashMap<>();
            d.guiColor = Math.max(0, Math.min(5, d.guiColor));
            d.volume = Math.max(0f, Math.min(2f, d.volume));
            save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Mode mode() {
        try {
            return Mode.valueOf(d.mode);
        } catch (Exception e) {
            return Mode.MULTI;
        }
    }

    /** Losuje jeden, jaskrawy kolor, który potem się nie zmienia. */
    public static void randomizeColor() {
        float hue = new Random().nextFloat();
        d.fixedColor = net.minecraft.util.math.MathHelper.hsvToRgb(hue, 1f, 1f);
        save();
    }
}
