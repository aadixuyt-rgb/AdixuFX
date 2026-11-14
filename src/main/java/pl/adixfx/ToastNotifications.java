package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Krótkie, dyskretne powiadomienia ekranowe z płynnym wejściem i wyjściem. */
public final class ToastNotifications {
    private static String message = "";
    private static long shownAt;
    private static final long DURATION_NS = 2_400_000_000L;

    private ToastNotifications() {}

    public static void show(String text) {
        message = text == null ? "" : text;
        shownAt = System.nanoTime();
    }

    public static void render(DrawContext ctx) {
        if (message.isEmpty()) return;
        long elapsed = System.nanoTime() - shownAt;
        if (elapsed >= DURATION_NS) {
            message = "";
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int textWidth = mc.textRenderer.getWidth(message);
        int width = Math.min(mc.getWindow().getScaledWidth() - 16, textWidth + 24);
        float in = Math.min(1f, elapsed / 180_000_000f);
        float out = Math.min(1f, (DURATION_NS - elapsed) / 260_000_000f);
        float alpha = Math.min(in, out);
        int x = (mc.getWindow().getScaledWidth() - width) / 2;
        int y = 12 - Math.round((1f - in) * 9f);
        int a = Math.max(0, Math.min(210, Math.round(210 * alpha)));
        int bg = (a << 24) | 0x00151B25;
        int edge = (Math.max(0, Math.min(255, Math.round(170 * alpha))) << 24)
                | (GuiUtil.accent() & 0x00FFFFFF);
        GuiUtil.roundedRect(ctx, x, y, x + width, y + 22, bg, bg, 6);
        GuiUtil.roundedRect(ctx, x, y + 20, x + width, y + 22, edge, bg, 2);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(message),
                x + width / 2, y + 7, (Math.max(0, Math.min(255, Math.round(255 * alpha))) << 24) | 0x00FFFFFF);
    }
}
