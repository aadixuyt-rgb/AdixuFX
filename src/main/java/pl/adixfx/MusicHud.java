package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Kompaktowy pasek aktualnie odtwarzanego utworu na górze ekranu. */
public final class MusicHud {
    private MusicHud() {}

    private static String lastTitle = "";
    private static long scrollStartedAt;
    private static boolean editMode;
    private static boolean leftWasDown;
    private static int dragMode; // 0 = brak, 1 = przesuwanie, 2 = zmiana rozmiaru
    private static double lastMouseX, lastMouseY;

    private static String formatTime(float seconds) {
        if (!Float.isFinite(seconds) || seconds < 0f) seconds = 0f;
        int total = Math.round(seconds);
        return (total / 60) + ":" + String.format(java.util.Locale.ROOT, "%02d", total % 60);
    }

    /** Obsługa przeciągania HUD-u i jego uchwytu zmiany rozmiaru. */
    public static void tick(MinecraftClient mc) {
        if (mc == null || mc.getWindow() == null || !Config.d.music || !Config.d.songHud) {
            dragMode = 0;
            leftWasDown = false;
            return;
        }
        long handle = mc.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int baseW = Math.min(240, Math.max(160, sw - 16));
        float scale = Math.max(0.45f, Math.min(3.0f, Config.d.songHudScale));
        double hudX = sw / 2.0 + Config.d.songHudOffsetX;
        double hudY = 12.0 / Math.max(1.0, mc.getWindow().getScaleFactor()) + Config.d.songHudOffsetY;
        double hudW = baseW * 1.75 / Math.max(1.0, mc.getWindow().getScaleFactor()) * scale;
        double hudH = 27.0 * 1.75 / Math.max(1.0, mc.getWindow().getScaleFactor()) * scale;
        double left = hudX - hudW / 2.0, top = hudY;
        double editLeft = 8, editTop = sh / 2.0 - 9;
        if (down && !leftWasDown) {
            if (mx >= editLeft && mx <= editLeft + 42 && my >= editTop && my <= editTop + 18) {
                editMode = !editMode;
                dragMode = 0;
            } else if (editMode && mx >= left + hudW - 14 && mx <= left + hudW + 5
                    && my >= top + hudH - 14 && my <= top + hudH + 5) {
                dragMode = 2;
            } else if (editMode && mx >= left && mx <= left + hudW && my >= top && my <= top + hudH) {
                dragMode = 1;
            }
            lastMouseX = mx;
            lastMouseY = my;
        }
        if (down && dragMode != 0) {
            double dx = mx - lastMouseX, dy = my - lastMouseY;
            if (dragMode == 1) {
                Config.d.songHudOffsetX += (float) dx;
                Config.d.songHudOffsetY += (float) dy;
            } else {
                Config.d.songHudScale = Math.max(0.45f, Math.min(3.0f,
                        Config.d.songHudScale + (float) (dx / 90.0)));
            }
            lastMouseX = mx;
            lastMouseY = my;
        }
        if (!down && leftWasDown) {
            if (dragMode != 0) Config.save();
            dragMode = 0;
        }
        leftWasDown = down;
    }

    public static void render(DrawContext ctx) {
        if (!Config.d.music || !Config.d.songHud) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int editY = screenHeight / 2 - 9;
        GuiUtil.roundedRect(ctx, 8, editY, 50, editY + 18,
                editMode ? GuiUtil.accent() : 0xB8121212, 0x603A3A3A, 5);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(editMode ? "GOTOWE" : "HUD"),
                13, editY + 5, GuiUtil.TEXT);
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

        // Tylko tytuł; przy długich nazwach przewijamy tekst w pętli.
        String title = Playlist.displayName(track);
        if (!title.equals(lastTitle)) {
            lastTitle = title;
            scrollStartedAt = System.nanoTime();
        }

        int textX = x + 8;
        int textY = y + 3;
        int textRight = x + width - 8;
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
        if (editMode) {
            GuiUtil.roundedRect(ctx, x, y, x + width, y + height, 0x00000000, GuiUtil.accent(), 2);
            GuiUtil.roundedRect(ctx, x + width - 8, y + height - 8, x + width + 3, y + height + 3,
                    GuiUtil.accent(), 0xFF101010, 3);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal("↘"), x + width - 7, y + height - 8, GuiUtil.TEXT);
        }
        ctx.getMatrices().pop();
    }
}
