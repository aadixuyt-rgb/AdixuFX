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
        int bg = !this.active ? 0xFF171B22 : accent ? GuiUtil.accent() :
                (this.isHovered() ? 0xFF293241 : 0xFF202631);
        GuiUtil.roundedRect(ctx, getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg, GuiUtil.PANEL, 7);
        if (accent || this.isHovered()) {
            int edge = accent ? GuiUtil.accent() : 0x885F718A;
            GuiUtil.roundedRect(ctx, getX() + 5, getY() + getHeight() - 2,
                    getX() + getWidth() - 5, getY() + getHeight() - 1, edge, bg, 1);
            if (!accent) ctx.fill(getX() + 1, getY() + 5, getX() + 2, getY() + getHeight() - 5, GuiUtil.accent());
        }

        int fallback = !this.active ? 0xFF7D8490 : accent ? 0xFF07150D : GuiUtil.TEXT;
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, fallback);
    }
}
