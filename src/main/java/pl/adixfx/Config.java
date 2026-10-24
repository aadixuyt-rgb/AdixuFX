package pl.adixfx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
        MULTI("Wiele kolorów naraz (zmieniają się)");

        public final String label;
        Mode(String label) { this.label = label; }
        public Mode next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public static final class Data {
        // hitboxy
        public boolean hitboxes = true;
        public String mode = Mode.MULTI.name();
        public int fixedColor = 0;            // 0 = jeszcze nie wylosowany
        public float speed = 1.0f;            // szybkość zmiany kolorów
        public int range = 64;
        public boolean players = true, hostile = true, passive = true, items = false, others = false;
        // muzyka
        public boolean music = true;
        /** głośność ogólna 0..2 (0-200%) */
        public float volume = 1.0f;
        /** głośność poszczególnych utworów 0..2 */
        public Map<String, Float> trackVolume = new HashMap<>();
        /** własne nazwy utworów */
        public Map<String, String> names = new HashMap<>();
        public Map<String, Boolean> tracks = new HashMap<>();
        /** własne (przeciągnięte) utwory */
        public List<CustomTracks.Entry> customTracks = new ArrayList<>();
        /** id wbudowanych utworów usuniętych na stałe (plik .ogg zostaje w .jar, ale mod go nie widzi) */
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
                Data r = GSON.fromJson(Files.readString(p), Data.class);
                if (r != null) d = r;
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
        if (d.fixedColor == 0) randomizeColor();
        save();
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(d));
        } catch (IOException ignored) {
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
