package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Kompaktowy pasek aktualnie odtwarzanego utworu na górze ekranu. */
public final class MusicHud {
    private MusicHud() {}

    private static String lastTitle = "";
    private static long scrollStartedAt;

    private static String formatTime(float seconds) {
        if (!Float.isFinite(seconds) || seconds < 0f) seconds = 0f;
        int total = Math.round(seconds);
        return (total / 60) + ":" + String.format(java.util.Locale.ROOT, "%02d", total % 60);
    }

    private static void drawPulseDot(DrawContext ctx, int cx, int cy, int radius, int color) {
        for (int row = -radius; row <= radius; row++) {
            int half = (int) Math.floor(Math.sqrt(radius * radius - row * row));
            ctx.fill(cx - half, cy + row, cx + half + 1, cy + row + 1, color);
        }
    }

    /** Edycja HUD-u odbywa się na osobnym ekranie MusicHudEditorScreen. */
    public static void tick(MinecraftClient mc) {
        // Brak globalnego przechwytywania myszy — sterowanie obsługuje ekran edycji.
    }

    public static void render(DrawContext ctx) {
        if (!Config.d.music || !Config.d.songHud) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        int screenWidth = mc.getWindow().getScaledWidth();
        Playlist.Track track = Music.playing();
        if (track == null) return;

        int width = Math.min(240, Math.max(160, screenWidth - 16));
        int height = 27;
        float guiFactor = (float) Math.max(1.0, mc.getWindow().getScaleFactor());
        float scale = (float) (1.75 / guiFactor) * Math.max(0.45f, Math.min(3.0f, Config.d.songHudScale));
        float topInset = (float) (12.0 / guiFactor) + Config.d.songHudOffsetY;

        // Zachowuje bazową pozycję na środku, ale pozwala przeciągać HUD po ekranie.
        ctx.getMatrices().push();
        ctx.getMatrices().translate(screenWidth / 2.0 * (1.0 - scale)
                + Config.d.songHudOffsetX, topInset, 0.0);
        ctx.getMatrices().scale(scale, scale, 1.0f);

        int x = -width / 2;
        int y = 0;

        GuiUtil.roundedRect(ctx, x, y, x + width, y + height, 0xB8121212, 0x603A3A3A, 6);

        // Trzy akcentowe światła pulsują zgodnie z analizą głośności utworu.
        float audioLevel = Music.isPlayingNow() ? BeatSync.level(Music.elapsedSeconds()) : 0.0f;
        long pulseClock = System.nanoTime();
        for (int i = 0; i < 3; i++) {
            float wave = 0.5f + 0.5f * (float) Math.sin(pulseClock / 180_000_000.0 + i * 1.9);
            int radius = 1 + Math.round(2.2f * Math.max(audioLevel, wave * 0.18f));
            int dotX = x + width - 10 - i * 9;
            int dotY = y + 7;
            drawPulseDot(ctx, dotX, dotY, radius, GuiUtil.accent());
        }

        // Tylko tytuł; przy długich nazwach przewijamy tekst w pętli.
        String title = Playlist.displayName(track);
        if (!title.equals(lastTitle)) {
            lastTitle = title;
            scrollStartedAt = System.nanoTime();
        }

        int textX = x + 8;
        int textY = y + 3;
        int textRight = x + width - 34;
        int availableWidth = textRight - textX;
        int titleWidth = mc.textRenderer.getWidth(title);
        Text renderedTitle = Text.literal(title);

        ctx.enableScissor(textX, textY, textRight, y + 12);
        if (titleWidth <= availableWidth) {
            ctx.drawTextWithShadow(mc.textRenderer, renderedTitle, textX, textY, GuiUtil.TEXT);
        } else {
            double seconds = (System.nanoTime() - scrollStartedAt) / 1_000_000_000.0;
            int gap = 22;
            double cycle = titleWidth + gap;
            double offset = (seconds * Config.d.titleScrollSpeed) % cycle;
            int drawX = textX - (int) offset;
            ctx.drawTextWithShadow(mc.textRenderer, renderedTitle, drawX, textY, GuiUtil.TEXT);
            ctx.drawTextWithShadow(mc.textRenderer, renderedTitle, drawX + titleWidth + gap, textY, GuiUtil.TEXT);
        }
        ctx.disableScissor();

        // Pasek postępu po lewej, a po prawej czas odtworzony / całkowita długość.
        float total = Music.totalSeconds();
        float elapsed = Music.elapsedSeconds();
        float progress = total > 0f ? Math.max(0f, Math.min(1f, elapsed / total)) : 0f;
        if (total > 0f) elapsed = Math.min(elapsed, total);

        String timeLabel = formatTime(elapsed) + " / " + (total > 0f ? formatTime(total) : "--:--");
        int timeWidth = mc.textRenderer.getWidth(timeLabel);
        int timeX = x + width - 8 - timeWidth;
        int timeY = y + 15;

        int barX = x + 8;
        int barY = y + 20;
        int barEnd = Math.max(barX + 8, timeX - 6);
        GuiUtil.roundedRect(ctx, barX, barY, barEnd, barY + 2, GuiUtil.TRACK_BG, GuiUtil.TRACK_BG, 2);
        if (progress > 0f) {
            int fillEnd = barX + Math.max(1, Math.round((barEnd - barX) * progress));
            GuiUtil.roundedRect(ctx, barX, barY, Math.min(barEnd, fillEnd), barY + 2,
                    GuiUtil.accent(), GuiUtil.TRACK_BG, 2);
        }
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(timeLabel), timeX, timeY, GuiUtil.TEXT_DIM);
        ctx.getMatrices().pop();
    }
}
