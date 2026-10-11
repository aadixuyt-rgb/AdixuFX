package pl.adixfx;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** In-game screen for importing audio files into AdixFX. */
public final class MusicDropScreen extends Screen {
    private final Screen parent;

    public MusicDropScreen(Screen parent) {
        super(Text.literal("AdixFX — dodaj muzykę"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int buttonY = height / 2 + 35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Wybierz plik…"), b -> MusicImporter.chooseFile())
                .dimensions(cx - 100, buttonY, 200, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Wróć"), b -> client.setScreen(parent))
                .dimensions(cx - 100, buttonY + 28, 200, 20).build());
    }

    public void filesDragged(List<Path> paths) {
        if (paths == null || paths.isEmpty()) return;
        for (Path path : paths) {
            if (path == null || path.getFileName() == null) continue;
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav")
                    || name.endsWith(".flac") || name.endsWith(".m4a") || name.endsWith(".aac")) {
                MusicImporter.importDroppedFile(path);
                return;
            }
        }
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§cAdixFX: Upuść plik OGG, MP3, WAV, FLAC, M4A lub AAC."), false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int cx = width / 2;
        int panelWidth = Math.min(360, width - 24);
        int left = cx - panelWidth / 2;
        int top = height / 2 - 82;
        int panelHeight = 106;
        context.fill(left, top, left + panelWidth, top + panelHeight, 0xDD171B24);
        context.drawBorder(left, top, panelWidth, panelHeight, 0xFF8B91A3);
        context.drawCenteredTextWithShadow(textRenderer, title, cx, top + 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("PRZECIĄGNIJ PLIK TUTAJ"), cx, top + 37, 0xFFB8C8FF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("albo kliknij „Wybierz plik…”"), cx, top + 54, 0xFFE0E0E0);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("OGG bez konwersji; pozostałe formaty konwertowane w modzie"), cx, top + 80, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
