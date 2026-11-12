package pl.adixfx;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persistent music directory outside the mod JAR. */
public final class MusicLibrary {
    private MusicLibrary() {}

    public static Path folder() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("adixfx").resolve("music");
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException("Nie można utworzyć folderu muzyki AdixFX: " + path, e);
        }
        return path;
    }

    public static Path file(String id) {
        return folder().resolve(id + ".ogg");
    }
}