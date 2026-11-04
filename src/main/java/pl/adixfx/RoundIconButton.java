package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Duży, okrągły, biały przycisk odtwarzania/pauzy - jak środkowy przycisk w Spotify. */
final class RoundIconButton extends ButtonWidget {
    RoundIconButton(int x, int y, int size, Text icon, PressAction onPress) {
        super(x, y, size, size, icon, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x1 = getX(), y1 = getY(), x2 = x1 + getWidth(), y2 = y1 + getHeight();
        float beat = Config.d.music && Music.isPlayingNow()
                ? BeatSync.pulse(Music.elapsedSeconds()) : 0.0f;
        float wave = 0.5f + 0.5f * (float) Math.sin(System.nanoTime() / 210_000_000.0);
        int bg = this.isHovered() ? 0xFFE0E0E0
                : (beat > 0.18f ? GuiUtil.accent() : 0xFFF2F2F2);
        int halo = (int) (20 + 35 * Math.max(beat, wave * 0.15f));
        GuiUtil.roundedRect(ctx, x1 - 1, y1 - 1, x2 + 1, y2 + 1,
                (halo << 24) | (GuiUtil.accent() & 0x00FFFFFF), GuiUtil.PANEL, getWidth() / 2);
        GuiUtil.roundedRect(ctx, x1, y1, x2, y2, bg, GuiUtil.PANEL, getWidth() / 2);
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, GuiUtil.font(getMessage()),
                x1 + getWidth() / 2, y1 + (getHeight() - 8) / 2, 0xFF121212);
    }
}
