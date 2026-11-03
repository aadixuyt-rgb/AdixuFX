package pl.adixfx;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Wspólna paleta i proste, czyste kształty GUI AdixFX. */
final class GuiUtil {
    private GuiUtil() {}

    static Text font(Text text) {
        // Zachowujemy zwykłą czcionkę Minecrafta.
        return text;
    }

    static final int BG = 0xE6121212;
    static final int PANEL = 0xFF181818;
    static final int CARD = 0xFF1E1E1E;
    static final int CARD_HOVER = 0xFF2A2A2A;
    static final int TRACK_BG = 0xFF3A3A3A;
    private static final int[] ACCENTS = {0xFF1DB954, 0xFF3B82F6, 0xFFB56BFF, 0xFFFF8A3D, 0xFFFF4F87, 0xFF20C7C9};
    static int accent() { return ACCENTS[Math.max(0, Math.min(ACCENTS.length - 1, Config.d.guiColor))]; }
    static int accentSoft() { return (0x2A << 24) | (accent() & 0x00FFFFFF); }
    static final int TEXT = 0xFFEDEDED;
    static final int TEXT_DIM = 0xFFA0A0A0;

    /**
     * Rysuje zaokrąglony prostokąt skanline'ami. Nie miesza krawędzi z szarym tłem,
     * dzięki czemu nie powstaje jasna poświata ani prostokątna obwódka.
     */
    static void roundedRect(DrawContext ctx, int x1, int y1, int x2, int y2,
                            int color, int behind, int radius) {
        if (x2 <= x1 || y2 <= y1) return;
        int r = Math.max(0, Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        if (r == 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }

        if (y1 + r < y2 - r) {
            ctx.fill(x1, y1 + r, x2, y2 - r, color);
        }

        for (int row = 0; row < r; row++) {
            double dy = r - (row + 0.5);
            int inset = Math.max(0, (int) Math.ceil(r - Math.sqrt(Math.max(0.0, r * r - dy * dy)) - 0.5));
            int left = x1 + inset;
            int right = x2 - inset;
            if (right <= left) continue;

            ctx.fill(left, y1 + row, right, y1 + row + 1, color);
            ctx.fill(left, y2 - row - 1, right, y2 - row, color);
        }
    }
}
