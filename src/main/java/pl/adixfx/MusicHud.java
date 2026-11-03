package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Minimalny pasek aktualnie odtwarzanego utworu na górze ekranu. */
public final class MusicHud {
    private MusicHud() {}

    private static String lastTitle = "";
    private static long scrollStartedAt;

    public static void render(DrawContext ctx) {
        if (!Config.d.music || !Config.d.songHud) return;
        Playlist.Track track = Music.playing();
        if (track == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int width = 369;
        int height = 43;
        int x = (mc.getWindow().getScaledWidth() - width) / 2;
        int y = 4;

        GuiUtil.roundedRect(ctx, x, y, x + width, y + height, 0xE6121212, 0x603A3A3A, 9);
        GuiUtil.roundedRect(ctx, x + 12, y + 9, x + 15, y + 34, GuiUtil.ACCENT, GuiUtil.ACCENT, 1);

        // Nazwa jest zawsze pobierana z aktualnej nazwy utworu zapisanej w playliście.
        String title = Playlist.displayName(track);
        if (!title.equals(lastTitle)) {
            lastTitle = title;
            scrollStartedAt = System.nanoTime();
        }

        int textX = x + 27;
        int textY = y + 17;
        int textRight = x + width - 42;
        int availableWidth = textRight - textX;
        int titleWidth = mc.textRenderer.getWidth(title);
        Text renderedTitle = GuiUtil.font(Text.literal(title).formatted(Formatting.WHITE));

        ctx.enableScissor(textX, y + 7, textRight, y + height - 7);
        if (titleWidth <= availableWidth) {
            ctx.drawTextWithShadow(mc.textRenderer, renderedTitle, textX, textY, 0xFFFFFFFF);
        } else {
            // Powolny, ciągły przesuw w lewo; po końcu tytuł wraca od prawej strony.
            double seconds = (System.nanoTime() - scrollStartedAt) / 1_000_000_000.0;
            int gap = 42;
            double cycle = titleWidth + gap;
            double offset = (seconds * 22.0) % cycle;
            int drawX = textX - (int) offset;
            ctx.drawTextWithShadow(mc.textRenderer, renderedTitle, drawX, textY, 0xFFFFFFFF);
            ctx.drawTextWithShadow(mc.textRenderer, renderedTitle, drawX + titleWidth + gap, textY, 0xFFFFFFFF);
        }
        ctx.disableScissor();

        // Mały wskaźnik odtwarzania po prawej.
        int bx = x + width - 24;
        int base = y + 31;
        int[] heights = {8, 15, 11, 19};
        for (int i = 0; i < heights.length; i++) {
            ctx.fill(bx + i * 4, base - heights[i], bx + i * 4 + 2, base, GuiUtil.ACCENT);
        }
    }
}
