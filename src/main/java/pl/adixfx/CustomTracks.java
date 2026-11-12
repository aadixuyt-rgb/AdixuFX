package pl.adixfx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/** Tracks imported in the GUI. All new audio files are stored in config/adixfx/music as .ogg. */
public final class CustomTracks {
    public static final String ADDING = "ADDING";
    public static final String READY = "READY";
    public static final String ERROR = "ERROR";

    public static final class Entry {
        public String id;
        public String name;
        /** Full path to the generated OGG file (null until conversion finishes). */
        public String file;
        public String status = ADDING;
    }

    private CustomTracks() {}

    /** Kept as a compatibility alias; imported and built-in tracks share one folder. */
    public static Path folder() {
        return MusicLibrary.folder();
    }

    /** Registers a new track while its MP3 is being converted. */
    public static Entry begin(String displayName) {
        Entry entry = new Entry();
        entry.id = "custom_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 100000);
        entry.name = (displayName == null || displayName.isBlank()) ? "Nowy utwór" : displayName.trim();
        entry.file = MusicLibrary.folder().resolve(entry.id + ".ogg").toString();
        entry.status = ADDING;
        Config.d.customTracks.add(entry);
        Config.save();
        return entry;
    }

    public static void markReady(Entry entry) {
        entry.status = READY;
        Config.save();
    }

    public static void markError(Entry entry) {
        entry.status = ERROR;
        try {
            if (entry.file != null) Files.deleteIfExists(Path.of(entry.file));
        } catch (IOException ignored) {
        }
        Config.save();
    }

    /** Removes the config entry and its audio file. */
    public static void remove(String id) {
        Iterator<Entry> iterator = Config.d.customTracks.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.id != null && entry.id.equals(id)) {
                try {
                    if (entry.file != null) Files.deleteIfExists(Path.of(entry.file));
                } catch (IOException ignored) {
                }
                iterator.remove();
            }
        }

        // Also supports OGG files dropped directly into config/adixfx/music.
        if (id != null && id.matches("[A-Za-z0-9._-]+")) {
            try {
                Files.deleteIfExists(MusicLibrary.folder().resolve(id + ".ogg"));
            } catch (IOException ignored) {
            }
        }
        Config.save();
    }
}
