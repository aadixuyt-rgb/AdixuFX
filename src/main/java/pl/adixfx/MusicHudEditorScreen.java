package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Dedicated screen for moving and resizing the music HUD. */
public final class MusicHudEditorScreen extends Screen {
    private final Screen parent;
    private final boolean previousHudHidden;
    private boolean dragging;
    private boolean resizing;
    private double grabDx;
    private double grabDy;
    private double resizeGrabX;
    private double resizeGrabY;
    private double resizeStartLeft;
    private double resizeStartTop;
    private double resizeStartWidth;
    private double resizeStartHeight;
    private float resizeStartScale;

    public MusicHudEditorScreen(Screen parent) {
        super(Text.literal("Edytuj HUD muzyki"));
        this.parent = parent;
        MinecraftClient mc = MinecraftClient.getInstance();
        this.previousHudHidden = mc.options.hudHidden;
        mc.options.hudHidden = true;
    }

    private int baseWidth() {
        return Math.min(240, Math.max(160, this.width - 16));
    }

    private float renderScale() {
        double guiFactor = Math.max(1.0, MinecraftClient.getInstance().getWindow().getScaleFactor());
        return (float) (1.75 / guiFactor) * Math.max(0.45f, Math.min(3.0f, Config.d.songHudScale));
    }

    private double hudLeft() {
        float scale = renderScale();
        return this.width / 2.0 * (1.0 - scale) + Config.d.songHudOffsetX
                - baseWidth() * scale / 2.0;
    }

    private double hudTop() {
        double guiFactor = Math.max(1.0, MinecraftClient.getInstance().getWindow().getScaleFactor());
        return 12.0 / guiFactor + Config.d.songHudOffsetY;
    }

    private double hudWidth() {
        return baseWidth() * renderScale();
    }

    private double hudHeight() {
        return 27.0 * renderScale();
    }

    private boolean onResizeHandle(double mouseX, double mouseY) {
        double right = hudLeft() + hudWidth();
        double bottom = hudTop() + hudHeight();
        return mouseX >= right - 8 && mouseX <= right + 10
                && mouseY >= bottom - 8 && mouseY <= bottom + 10;
    }

    private boolean insideHud(double mouseX, double mouseY) {
        return mouseX >= hudLeft() && mouseX <= hudLeft() + hudWidth()
                && mouseY >= hudTop() && mouseY <= hudTop() + hudHeight();
    }

    @Override
    protected void init() {
        int buttonY = this.height - 34;
        addDrawableChild(new FlatButton(this.width / 2 - 48, buttonY, 96, 18,
                Text.literal("Gotowe"), button -> {
                    Config.save();
                    this.client.setScreen(parent);
                }).accent(true));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && onResizeHandle(mouseX, mouseY)) {
            resizing = true;
            resizeStartLeft = hudLeft();
            resizeStartTop = hudTop();
            resizeStartWidth = Math.max(1.0, hudWidth());
            resizeStartHeight = Math.max(1.0, hudHeight());
            resizeStartScale = Config.d.songHudScale;
            resizeGrabX = mouseX - (resizeStartLeft + resizeStartWidth);
            resizeGrabY = mouseY - (resizeStartTop + resizeStartHeight);
            return true;
        }
        if (button == 0 && insideHud(mouseX, mouseY)) {
            dragging = true;
            grabDx = mouseX - hudLeft();
            grabDy = mouseY - hudTop();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && dragging) {
            float scale = renderScale();
            double left = mouseX - grabDx;
            double top = mouseY - grabDy;
            Config.d.songHudOffsetX = (float) (left
                    - this.width / 2.0 * (1.0 - scale) + baseWidth() * scale / 2.0);
            double guiFactor = Math.max(1.0, MinecraftClient.getInstance().getWindow().getScaleFactor());
            Config.d.songHudOffsetY = (float) (top - 12.0 / guiFactor);
            clampPosition();
            return true;
        }
        if (button == 0 && resizing) {
            double wantedWidth = Math.max(1.0, mouseX - resizeGrabX - resizeStartLeft);
            double wantedHeight = Math.max(1.0, mouseY - resizeGrabY - resizeStartTop);
            double ratio = (wantedWidth / resizeStartWidth + wantedHeight / resizeStartHeight) / 2.0;
            Config.d.songHudScale = Math.max(0.45f, Math.min(3.0f, resizeStartScale * (float) ratio));

            float scale = renderScale();
            double newWidth = baseWidth() * scale;
            Config.d.songHudOffsetX = (float) (resizeStartLeft
                    - this.width / 2.0 * (1.0 - scale) + newWidth / 2.0);
            double guiFactor = Math.max(1.0, MinecraftClient.getInstance().getWindow().getScaleFactor());
            Config.d.songHudOffsetY = (float) (resizeStartTop - 12.0 / guiFactor);
            clampPosition();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void clampPosition() {
        double w = hudWidth();
        double h = hudHeight();
        float scale = renderScale();
        double baseX = this.width / 2.0 * (1.0 - scale);
        double minX = -baseX + w / 2.0;
        double maxX = this.width - baseX - w / 2.0;
        Config.d.songHudOffsetX = (float) Math.max(minX, Math.min(maxX, Config.d.songHudOffsetX));

        double guiFactor = Math.max(1.0, MinecraftClient.getInstance().getWindow().getScaleFactor());
        double topInset = 12.0 / guiFactor;
        double minY = -topInset;
        double maxY = this.height - h - topInset;
        Config.d.songHudOffsetY = (float) Math.max(minY, Math.min(maxY, Config.d.songHudOffsetY));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (dragging || resizing)) {
            dragging = false;
            resizing = false;
            Config.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Przezroczysty ekran: świat pozostaje ostry i widoczny, bez dużego panelu.
        GuiUtil.roundedRect(ctx, this.width / 2 - 137, 10, this.width / 2 + 137,
                30, 0xA0121212, 0xA055AAFF, 6);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("EDYTUJ HUD  •  przeciągnij pasek  •  ↘ zmienia rozmiar"),
                this.width / 2, 17, GuiUtil.TEXT);

        Playlist.Track track = Music.playing();
        String title = track == null ? "Podgląd nazwy utworu" : Playlist.displayName(track);
        String time = track == null ? "1:23 / 3:45" :
                formatTime(Music.elapsedSeconds()) + " / " + formatTime(Music.totalSeconds());

        double left = hudLeft();
        double top = hudTop();
        float scale = renderScale();
        int width = baseWidth();

        ctx.getMatrices().push();
        ctx.getMatrices().translate(this.width / 2.0 * (1.0 - scale)
                + Config.d.songHudOffsetX, top, 0.0);
        ctx.getMatrices().scale(scale, scale, 1.0f);
        int x = -width / 2;
        GuiUtil.roundedRect(ctx, x, 0, x + width, 27, 0xB8121212, 0x603A3A3A, 6);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(title), x + 8, 3, GuiUtil.TEXT);
        int timeWidth = this.textRenderer.getWidth(time);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(time), x + width - timeWidth - 8, 15, GuiUtil.TEXT_DIM);
        int barEnd = width - timeWidth - 22;
        GuiUtil.roundedRect(ctx, x + 8, 20, x + barEnd, 22, GuiUtil.TRACK_BG, GuiUtil.TRACK_BG, 2);
        float progress = track == null || Music.totalSeconds() <= 0f ? 0.37f :
                Math.max(0f, Math.min(1f, Music.elapsedSeconds() / Music.totalSeconds()));
        GuiUtil.roundedRect(ctx, x + 8, 20, x + 8 + Math.max(1, (int) ((barEnd - 8) * progress)),
                22, GuiUtil.accent(), GuiUtil.TRACK_BG, 2);
        ctx.getMatrices().pop();

        int l = (int) Math.round(left);
        int t = (int) Math.round(top);
        int r = (int) Math.round(left + hudWidth());
        int b = (int) Math.round(top + hudHeight());
        ctx.fill(l - 2, t - 2, r + 2, t, 0xFF55AAFF);
        ctx.fill(l - 2, b, r + 2, b + 2, 0xFF55AAFF);
        ctx.fill(l - 2, t, l, b, 0xFF55AAFF);
        ctx.fill(r, t, r + 2, b, 0xFF55AAFF);

        // Duży, kontrastowy uchwyt jak w edytorze Heart: łatwo go złapać myszką.
        int hx = r + 2, hy = b + 2, handle = 12;
        ctx.fill(hx, hy, hx + handle, hy + handle, 0xFF55AAFF);
        ctx.fill(hx, hy, hx + handle, hy + 1, 0xFF101010);
        ctx.fill(hx, hy + handle - 1, hx + handle, hy + handle, 0xFF101010);
        ctx.fill(hx, hy, hx + 1, hy + handle, 0xFF101010);
        ctx.fill(hx + handle - 1, hy, hx + handle, hy + handle, 0xFF101010);
        for (int i = 3; i <= 8; i++) {
            ctx.fill(hx + i, hy + i, hx + i + 1, hy + i + 1, 0xFF101010);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private static String formatTime(float seconds) {
        if (!Float.isFinite(seconds) || seconds < 0f) seconds = 0f;
        int total = Math.round(seconds);
        return (total / 60) + ":" + String.format(java.util.Locale.ROOT, "%02d", total % 60);
    }

    @Override
    public void removed() {
        MinecraftClient.getInstance().options.hudHidden = previousHudHidden;
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
