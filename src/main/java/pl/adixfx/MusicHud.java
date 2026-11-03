package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Minimalny pasek aktualnie odtwarzanego utworu na górze ekranu. */
public final class MusicHud {
    private MusicHud() {}

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

        String title = Playlist.displayName(track);
        int maxWidth = width - 58;
        while (!title.isEmpty() && mc.textRenderer.getWidth(title) > maxWidth) {
            title = title.substring(0, title.length() - 2) + "…";
        }
        ctx.drawTextWithShadow(mc.textRenderer, GuiUtil.font(Text.literal(title).formatted(Formatting.WHITE)),
                x + 27, y + 17, 0xFFFFFFFF);

        // Mały wskaźnik odtwarzania po prawej.
        int bx = x + width - 24;
        int base = y + 31;
        int[] heights = {8, 15, 11, 19};
        for (int i = 0; i < heights.length; i++) {
            ctx.fill(bx + i * 4, base - heights[i], bx + i * 4 + 2, base, GuiUtil.ACCENT);
        }
    }
}
