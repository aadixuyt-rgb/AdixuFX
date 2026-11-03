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
    private double lastMouseX;
    private double lastMouseY;

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
        return this.width / 2.0 + Config.d.songHudOffsetX - baseWidth() * renderScale() / 2.0;
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
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        if (button == 0 && insideHud(mouseX, mouseY)) {
            dragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && dragging) {
            Config.d.songHudOffsetX += (float) deltaX;
            Config.d.songHudOffsetY += (float) deltaY;
            clampPosition();
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        if (button == 0 && resizing) {
            double baseRenderedWidth = Math.max(1.0, baseWidth() * (1.75 /
                    Math.max(1.0, MinecraftClient.getInstance().getWindow().getScaleFactor())));
            Config.d.songHudScale = Math.max(0.45f, Math.min(3.0f,
                    Config.d.songHudScale + (float) (deltaX / baseRenderedWidth)));
            clampPosition();
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void clampPosition() {
        double w = hudWidth();
        double h = hudHeight();
        double minX = w / 2.0 - this.width / 2.0;
        double maxX = this.width / 2.0 - w / 2.0;
        Config.d.songHudOffsetX = (float) Math.max(minX, Math.min(maxX, Config.d.songHudOffsetX));

        double guiFactor = Math.max(1.0, MinecraftClient.getInstance().getWindow().getScaleFactor());
        double minY = -12.0 / guiFactor;
        double maxY = this.height - h - 12.0 / guiFactor;
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
                30, 0xA0121212, 0x603A3A3A, 6);
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
        ctx.getMatrices().translate(this.width / 2.0 + Config.d.songHudOffsetX, top, 0.0);
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
        ctx.fill(l, t, r, t + 1, GuiUtil.accent());
        ctx.fill(l, b - 1, r, b, GuiUtil.accent());
        ctx.fill(l, t, l + 1, b, GuiUtil.accent());
        ctx.fill(r - 1, t, r, b, GuiUtil.accent());
        GuiUtil.roundedRect(ctx, r - 8, b - 8, r + 8, b + 8, GuiUtil.accent(), 0xFF101010, 4);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("↘"), r - 6, b - 7, GuiUtil.TEXT);

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
