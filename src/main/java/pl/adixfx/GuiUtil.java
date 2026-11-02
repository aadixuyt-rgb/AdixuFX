package pl.adixfx;

import net.minecraft.client.gui.DrawContext;

/**
 * Wspólny, ciemny wygląd GUI (paleta + zaokrąglone prostokąty) używany przez AdixFxScreen,
 * żeby panele, suwaki i wiersze utworów nie wyglądały jak domyślne, kwadratowe okienka
 * Minecrafta, tylko trochę bardziej jak lista utworów w Spotify.
 */
final class GuiUtil {
    private GuiUtil() {}

    static final int BG = 0xE6121212;      // tło całego panelu (lekko przezroczyste)
    static final int PANEL = 0xFF181818;   // tło "karty"
    static final int CARD = 0xFF1E1E1E;    // tło wiersza/przycisku
    static final int CARD_HOVER = 0xFF2A2A2A;
    static final int TRACK_BG = 0xFF3A3A3A; // tło suwaka/paska postępu
    static final int ACCENT = 0xFF1DB954;   // zielony akcent
    static final int ACCENT_SOFT = 0x2A1DB954; // zielony akcent, półprzezroczysty (podświetlenie wiersza)
    static final int TEXT = 0xFFEDEDED;
    static final int TEXT_DIM = 0xFFA0A0A0;

    /**
     * Prostokąt ze ściętymi (przybliżenie zaokrąglenia bez shaderów) rogami o promieniu {@code r},
     * wypełniony {@code color}; {@code behind} to kolor tła "za" rogiem, którym są wycinane.
     */
    static void roundedRect(DrawContext ctx, int x1, int y1, int x2, int y2, int color, int behind, int r) {
        if (x2 <= x1 || y2 <= y1) return;
        r = Math.max(0, Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        ctx.fill(x1, y1, x2, y2, color);
        for (int i = 0; i < r; i++) {
            int cut = r - i;
            ctx.fill(x1, y1 + i, x1 + cut, y1 + i + 1, behind);
            ctx.fill(x2 - cut, y1 + i, x2, y1 + i + 1, behind);
            ctx.fill(x1, y2 - i - 1, x1 + cut, y2 - i, behind);
            ctx.fill(x2 - cut, y2 - i - 1, x2, y2 - i, behind);
        }
    }
}
