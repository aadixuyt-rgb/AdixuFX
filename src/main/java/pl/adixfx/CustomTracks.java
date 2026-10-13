package pl.adixfx;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Własne utwory dodane przez przeciągnięcie / wybranie pliku mp3. W odróżnieniu od wbudowanej
 * playlisty (skompilowanej w mod), te pliki leżą naprawdę na dysku w folderze config/adixfx/custom
 * i usuwanie ich usuwa je stamtąd na stałe.
 */
public final class CustomTracks {
    public static final String ADDING = "ADDING";
    public static final String READY = "READY";
    public static final String ERROR = "ERROR";

    public static final class Entry {
        public String id;
        public String name;
        /** pełna ścieżka do pliku audio (.wav) - null, dopóki konwersja się nie skończy */
        public String file;
        public String status = ADDING;
    }

    private CustomTracks() {}

    public static Path folder() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("adixfx").resolve("custom");
        try {
            Files.createDirectories(p);
        } catch (IOException ignored) {
        }
        return p;
    }

    /** Rejestruje nowy wpis w stanie "dodawanie" (plik jeszcze nie istnieje) i od razu go zapisuje. */
    public static Entry begin(String displayName) {
        Entry e = new Entry();
        e.id = "custom_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 100000);
        e.name = (displayName == null || displayName.isBlank()) ? "Nowy utwór" : displayName.trim();
        e.file = folder().resolve(e.id + ".wav").toString();
        e.status = ADDING;
        Config.d.customTracks.add(e);
        Config.save();
        return e;
    }

    public static void markReady(Entry e) {
        e.status = READY;
        Config.save();
    }

    public static void markError(Entry e) {
        e.status = ERROR;
        try {
            if (e.file != null) Files.deleteIfExists(Path.of(e.file));
        } catch (IOException ignored) {
        }
        Config.save();
    }

    /** Usuwa wpis naprawdę: kasuje plik audio z dysku i wpis z configu. */
    public static void remove(String id) {
        Iterator<Entry> it = Config.d.customTracks.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.id.equals(id)) {
                try {
                    if (e.file != null) Files.deleteIfExists(Path.of(e.file));
                } catch (IOException ignored) {
                }
                it.remove();
            }
        }
        Config.save();
    }
}
