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

    public static void render(DrawContext ctx) {
        if (!Config.d.music || !Config.d.songHud) return;
        Playlist.Track track = Music.playing();
        if (track == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow().getScaledWidth();
        int width = Math.min(300, Math.max(180, screenWidth - 16));
        int height = 34;
        int x = (screenWidth - width) / 2;
        int y = 12;

        GuiUtil.roundedRect(ctx, x, y, x + width, y + height, 0xE6121212, 0x603A3A3A, 7);

        // Tylko tytuł; przy długich nazwach przewijamy tekst w pętli.
        String title = Playlist.displayName(track);
        if (!title.equals(lastTitle)) {
            lastTitle = title;
            scrollStartedAt = System.nanoTime();
        }

        int textX = x + 10;
        int textY = y + 4;
        int textRight = x + width - 10;
        int availableWidth = textRight - textX;
        int titleWidth = mc.textRenderer.getWidth(title);
        Text renderedTitle = Text.literal(title);

        ctx.enableScissor(textX, textY, textRight, y + 15);
        if (titleWidth <= availableWidth) {
            ctx.drawTextWithShadow(mc.textRenderer, renderedTitle, textX, textY, GuiUtil.TEXT);
        } else {
            double seconds = (System.nanoTime() - scrollStartedAt) / 1_000_000_000.0;
            int gap = 30;
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
        int timeX = x + width - 10 - timeWidth;
        int timeY = y + 19;

        int barX = x + 10;
        int barY = y + 23;
        int barEnd = Math.max(barX + 8, timeX - 8);
        GuiUtil.roundedRect(ctx, barX, barY, barEnd, barY + 3, GuiUtil.TRACK_BG, GuiUtil.TRACK_BG, 2);
        if (progress > 0f) {
            int fillEnd = barX + Math.max(1, Math.round((barEnd - barX) * progress));
            GuiUtil.roundedRect(ctx, barX, barY, Math.min(barEnd, fillEnd), barY + 3,
                    GuiUtil.accent(), GuiUtil.TRACK_BG, 2);
        }
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(timeLabel), timeX, timeY, GuiUtil.TEXT_DIM);
    }
}
