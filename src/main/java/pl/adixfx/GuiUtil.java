package pl.adixfx;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Wspólna paleta i wygładzane narożniki GUI AdixFX.
 * Geometria i pozycje kontrolek pozostają bez zmian; wygładzanie dotyczy wyłącznie krawędzi.
 */
final class GuiUtil {
    private GuiUtil() {}

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
     * Zaokrąglony prostokąt z 4x4 supersamplingiem narożników.
     * Mieszanie koloru krawędzi z tłem wygładza schodki bez zmiany rozmiaru ani położenia.
     */
    static void roundedRect(DrawContext ctx, int x1, int y1, int x2, int y2,
                            int color, int behind, int radius) {
        if (x2 <= x1 || y2 <= y1) return;
        int r = Math.max(0, Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        if (r == 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }

        ctx.fill(x1, y1, x2, y2, color);
        final int samples = 4;
        final double total = samples * samples;

        for (int py = 0; py < r; py++) {
            for (int px = 0; px < r; px++) {
                // Lewy górny i prawy górny narożnik; odbicie obsługuje pozostałe dwa.
                drawCornerPixel(ctx, x1 + px, y1 + py, px, py, r, color, behind, samples, total);
                drawCornerPixel(ctx, x2 - 1 - px, y1 + py, px, py, r, color, behind, samples, total);
                drawCornerPixel(ctx, x1 + px, y2 - 1 - py, px, py, r, color, behind, samples, total);
                drawCornerPixel(ctx, x2 - 1 - px, y2 - 1 - py, px, py, r, color, behind, samples, total);
            }
        }
    }

    private static void drawCornerPixel(DrawContext ctx, int screenX, int screenY,
                                        int px, int py, int radius, int foreground, int background,
                                        int samples, double total) {
        double cx = radius;
        double cy = radius;
        int inside = 0;
        for (int sy = 0; sy < samples; sy++) {
            for (int sx = 0; sx < samples; sx++) {
                double dx = px + (sx + 0.5) / samples - cx;
                double dy = py + (sy + 0.5) / samples - cy;
                if (dx * dx + dy * dy <= (double) radius * radius) inside++;
            }
        }
        if (inside == samples * samples) return;
        int blended = blend(background, foreground, inside / total);
        ctx.fill(screenX, screenY, screenX + 1, screenY + 1, blended);
    }

    private static int blend(int background, int foreground, double amount) {
        int a = mix(background >>> 24, foreground >>> 24, amount);
        int r = mix((background >>> 16) & 255, (foreground >>> 16) & 255, amount);
        int g = mix((background >>> 8) & 255, (foreground >>> 8) & 255, amount);
        int b = mix(background & 255, foreground & 255, amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int mix(int from, int to, double amount) {
        return Math.max(0, Math.min(255, (int) Math.round(from + (to - from) * amount)));
    }
}
