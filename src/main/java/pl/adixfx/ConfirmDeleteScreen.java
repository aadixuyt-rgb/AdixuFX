package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Objects;

/** Potwierdzenie usunięcia utworu - "Tak" usuwa naprawdę (dla własnych plików: kasuje je z dysku). */
public class ConfirmDeleteScreen extends Screen {
    private final Screen parent;
    private final Playlist.Track track;

    public ConfirmDeleteScreen(Screen parent, Playlist.Track track) {
        super(Text.literal("Usuń utwór"));
        this.parent = parent;
        this.track = track;
    }

    @Override
    protected void init() {
        int cx = this.width / 2, cy = this.height / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Tak, usuń").formatted(Formatting.RED), b -> {
            if (Objects.equals(Music.playing(), track)) Music.stop();
            Playlist.remove(track);
            close();
        }).dimensions(cx - 110, cy + 18, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Nie"), b -> close())
                .dimensions(cx + 10, cy + 18, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Na pewno usunąć \"" + Playlist.displayName(track) + "\"?").formatted(Formatting.WHITE),
                this.width / 2, this.height / 2 - 28, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Usuwa na stałe, nie da się cofnąć").formatted(Formatting.GRAY),
                this.width / 2, this.height / 2 - 14, 0xFFFFFF);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
