package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Płaski, zaokrąglony przycisk (karta) zamiast domyślnego, szarego i kwadratowego przycisku
 * Minecrafta - żeby GUI wyglądało bardziej jak lista w Spotify niż jak okienko ustawień gry.
 * Działanie (klik, dźwięk, narracja) zostaje po prostu takie, jak w zwykłym ButtonWidget -
 * zmienia się tylko to, jak się rysuje.
 */
class FlatButton extends ButtonWidget {
    private boolean accent;

    FlatButton(int x, int y, int w, int h, Text message, PressAction onPress) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    /** Zielone tło (np. aktywna zakładka) zamiast zwykłej karty. */
    FlatButton accent(boolean on) {
        this.accent = on;
        return this;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int bg;
        if (!this.active) bg = GuiUtil.CARD;
        else if (accent) bg = GuiUtil.accent();
        else bg = this.isHovered() ? GuiUtil.CARD_HOVER : GuiUtil.CARD;
        GuiUtil.roundedRect(ctx, getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg, GuiUtil.PANEL, 8);
        if (accent || this.isHovered()) {
            int edge = accent ? GuiUtil.accent() : GuiUtil.accentSoft();
            GuiUtil.roundedRect(ctx, getX() + 3, getY() + getHeight() - 2,
                    getX() + getWidth() - 3, getY() + getHeight() - 1, edge, bg, 1);
        }

        int fallback = !this.active ? 0xFF707070 : accent ? 0xFF0B1F0F : GuiUtil.TEXT;
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, fallback);
    }
}
