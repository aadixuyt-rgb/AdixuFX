package pl.adixfx;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Niewidoczna nakładka na pasku postępu utworu - łapie klik i przeciąganie, żeby przewijać.
 * Sam pasek (tło, zielone wypełnienie, czas) rysuje {@link AdixFxScreen#render} co klatkę, więc
 * zawsze pokazuje aktualny postęp - ten widget nie rysuje nic sam z siebie.
 * Dodawany tylko, gdy {@link Music#isSeekable()} (czyli dla własnych, przeciągniętych plików).
 */
final class SeekBar extends ButtonWidget {
    SeekBar(int x, int y, int w, int h) {
        super(x, y, w, h, Text.empty(), b -> {}, DEFAULT_NARRATION_SUPPLIER);
    }

    private void seekAt(double mouseX) {
        float total = Music.totalSeconds();
        if (total <= 0) return;
        double frac = Math.max(0.0, Math.min(1.0, (mouseX - getX()) / getWidth()));
        Music.seekTo((float) (frac * total));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled && button == 0) seekAt(mouseX);
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            seekAt(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // celowo puste - pasek rysuje AdixFxScreen.render()
    }
}
