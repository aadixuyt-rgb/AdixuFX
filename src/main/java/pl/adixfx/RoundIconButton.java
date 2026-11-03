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
        int bg = this.isHovered() ? 0xFFE0E0E0 : 0xFFFFFFFF;
        GuiUtil.roundedRect(ctx, x1, y1, x2, y2, bg, GuiUtil.PANEL, getWidth() / 2);
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, GuiUtil.font(getMessage()),
                x1 + getWidth() / 2, y1 + (getHeight() - 8) / 2, 0xFF121212);
    }
}
