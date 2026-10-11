package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Imports audio into an AdixFX resource pack. FFmpeg is required for non-OGG files. */
public final class MusicImporter {
    private static final String PACK = "adixfx_imported";
    private static final Path PACK_DIR = MinecraftClient.getInstance().runDirectory.toPath()
            .resolve("resourcepacks").resolve(PACK);
    private MusicImporter() {}

    public static void chooseFile() {
        // Native OS file dialog; Swing JFileChooser can silently fail in some Minecraft launchers.
        Thread picker = new Thread(() -> {
            try {
                if (GraphicsEnvironment.isHeadless()) {
                    report("System nie udostępnia okna wyboru pliku. Sprawdź log latest.log.");
                    return;
                }
                FileDialog dialog = new FileDialog((Frame) null, "Dodaj muzykę do AdixFX", FileDialog.LOAD);
                dialog.setFilenameFilter((dir, name) -> {
                    String n = name.toLowerCase(Locale.ROOT);
                    return n.endsWith(".mp3") || n.endsWith(".ogg") || n.endsWith(".wav")
                            || n.endsWith(".flac") || n.endsWith(".m4a") || n.endsWith(".aac");
                });
                dialog.setVisible(true);
                String file = dialog.getFile();
                String directory = dialog.getDirectory();
                dialog.dispose();
                if (file == null || directory == null) return;
                Path selected = Path.of(directory, file);
                MinecraftClient.getInstance().execute(() -> importFile(selected));
            } catch (Throwable e) {
                e.printStackTrace();
                report("Nie można otworzyć wyboru pliku: " + e.getClass().getSimpleName()
                        + (e.getMessage() == null ? "" : " — " + e.getMessage()));
            }
        }, "AdixFX-file-picker");
        picker.setDaemon(true);
        picker.start();
    }

    public static void importDroppedFile(Path source) {
        MinecraftClient.getInstance().execute(() -> importFile(source));
    }

    private static void importFile(Path source) {
        try {
            Path sounds = PACK_DIR.resolve("assets/adixfx/sounds/imported");
            Files.createDirectories(sounds);
            String title = cleanTitle(source.getFileName().toString());
            String id = uniqueId(title, sounds);
            Path target = sounds.resolve(id + ".ogg");
            if (source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg")) {
                Files.copy(source, target);
            } else {
                Process p = new ProcessBuilder("ffmpeg", "-y", "-i", source.toAbsolutePath().toString(),
                        "-vn", "-c:a", "libvorbis", "-q:a", "5", target.toString())
                        .redirectErrorStream(true).start();
                byte[] log = p.getInputStream().readAllBytes();
                if (p.waitFor() != 0 || !Files.exists(target) || Files.size(target) == 0) {
                    Files.deleteIfExists(target);
                    throw new IOException("FFmpeg conversion failed. Install FFmpeg and add it to PATH. " +
                            new String(log, StandardCharsets.UTF_8));
                }
            }
            Playlist.addImported(new Playlist.Track("imported_" + id, title,
                    Identifier.of("adixfx", "imported/" + id)));
            writePackFiles();
            Config.save();
            MinecraftClient mc = MinecraftClient.getInstance();
            var manager = mc.getResourcePackManager();
            manager.scanPacks();
            Set<String> enabled = new LinkedHashSet<>();
            manager.getEnabledProfiles().forEach(profile -> enabled.add(profile.getId()));
            enabled.add("file/" + PACK);
            manager.setEnabledProfiles(enabled);
            mc.reloadResources();
            mc.inGameHud.getChatHud().addMessage(Text.literal("§aAdixFX: added " + title));
            if (mc.currentScreen instanceof AdixFxScreen || mc.currentScreen instanceof MusicDropScreen) {
                mc.setScreen(new AdixFxScreen());
            }
        } catch (Exception e) { report("Import failed: " + e.getMessage()); }
    }

    private static void writePackFiles() throws IOException {
        Path assets = PACK_DIR.resolve("assets/adixfx");
        Files.createDirectories(assets);
        List<Playlist.Track> tracks = Playlist.TRACKS.stream()
                .filter(t -> t.id().startsWith("imported_")).toList();
        StringBuilder json = new StringBuilder("{\n");
        for (int i = 0; i < tracks.size(); i++) {
            String path = tracks.get(i).sound().getPath();
            json.append("  \"").append(path).append("\": {\"sounds\":[{\"name\":\"")
                    .append(path).append("\",\"stream\":true}]}");
            if (i + 1 < tracks.size()) json.append(',');
            json.append('\n');
        }
        json.append("}\n");
        Files.writeString(assets.resolve("sounds.json"), json, StandardCharsets.UTF_8);
        Files.writeString(PACK_DIR.resolve("pack.mcmeta"),
                "{\"pack\":{\"pack_format\":46,\"description\":\"AdixFX imported music\"}}",
                StandardCharsets.UTF_8);
    }

    private static String uniqueId(String title, Path dir) {
        String base = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "_")
                .replaceAll("_+", "_").replaceAll("^[_-]+|[_-]+$", "");
        if (base.isBlank()) base = "track";
        String id = base;
        for (int n = 2; Files.exists(dir.resolve(id + ".ogg")); n++) id = base + "_" + n;
        return id;
    }

    static String cleanTitle(String filename) {
        String s = filename.replaceFirst("(?i)\\.[^.]+$", "")
                .replaceFirst("(?i)^YTSave[_ -]*", "")
                .replaceFirst("(?i)^YouTube[_ -]*(Media[_ -]*)?", "")
                .replaceFirst("(?i)^Media[_ -]*", "")
                .replaceFirst("(?i)^[A-Za-z0-9_-]{10,15}[_ -]+", "")
                .replaceFirst("(?i)_(?:\\d{2,}|\\d{3,}k)(?:[_-].*)?$", "")
                .replaceFirst("(?i)[ _-]+(lyrics|official audio|official video)$", "")
                .replaceAll("(?i)[_-](?:128k|192k|256k|320k|lyrics|official|audio|video)(?=[_-]|$)", "")
                .replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ").trim();
        if (s.isBlank()) return "Imported track";
        StringBuilder out = new StringBuilder();
        for (String word : s.toLowerCase(Locale.ROOT).split(" ")) {
            if (word.isBlank()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static void report(String message) {
        MinecraftClient.getInstance().execute(() ->
                MinecraftClient.getInstance().inGameHud.getChatHud()
                        .addMessage(Text.literal("§cAdixFX: " + message)));
    }
}
