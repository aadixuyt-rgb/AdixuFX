package pl.adixfx;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** In-game drop target for importing audio files. */
public final class MusicDropScreen extends Screen {
    private final Screen parent;
    private String status = "Przeciągnij plik audio na to okno.";

    public MusicDropScreen(Screen parent) {
        super(Text.literal("AdixFX — dodaj muzykę"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 + 34;
        addDrawableChild(ButtonWidget.builder(Text.literal("Wybierz plik…"), b -> MusicImporter.chooseFile())
                .dimensions(cx - 100, y, 196, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Wróć"), b -> client.setScreen(parent))
                .dimensions(cx - 100, y + 26, 196, 20).build());
    }

    @Override
    public void filesDragged(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            status = "Nie wykryto pliku.";
            return;
        }
        Path file = paths.get(0);
        if (!Files.isRegularFile(file)) {
            status = "To nie jest plik.";
            return;
        }
        String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (!(name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav")
                || name.endsWith(".flac") || name.endsWith(".m4a") || name.endsWith(".aac"))) {
            status = "Nieobsługiwany format pliku.";
            return;
        }
        status = "Importuję: " + file.getFileName();
        MusicImporter.importDroppedFile(file);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 42, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Upuść tutaj plik MP3, OGG, WAV, FLAC, M4A lub AAC"), width / 2, height / 2 - 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, height / 2 + 4, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
