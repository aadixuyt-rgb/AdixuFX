package pl.adixfx;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

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
        int y = height / 2 + 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Wybierz plik…"), b -> MusicImporter.chooseFile())
                .dimensions(cx - 100, y, 196, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Wróć"), b -> client.setScreen(parent))
                .dimensions(cx - 100, y + 26, 196, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 42, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Wybierz plik MP3, OGG, WAV, FLAC, M4A lub AAC"), width / 2, height / 2 - 18, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Konwersja formatów innych niż OGG wymaga FFmpeg"), width / 2, height / 2 + 2, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
