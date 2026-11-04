package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** GUI: strona "Hitboxy" i strona "Muzyka". Otwierane klawiszem (domyślnie prawy Shift). */
public class AdixFxScreen extends Screen {
    private enum Page { HITBOXES, MUSIC }

    private static Page page = Page.HITBOXES;
    private static final int W = 240;     // szerokość strony hitboxów i zakładek
    private static final int MW = 390;    // szerokość strony muzyki
    private static final int ROW = 16;
    private static final float[] SPEEDS = {0.25f, 0.5f, 1.0f, 2.0f, 4.0f};
    private static final float[] TITLE_SCROLL_SPEEDS = {10.0f, 16.0f, 22.0f, 30.0f, 42.0f};
    private static final int[] RANGES = {16, 32, 64, 128};

    private static int trackScroll = 0;
    /** aktualnie otwarty ekran muzyki - żeby konwersja/odtwarzanie w tle mogło go odświeżyć */
    private static AdixFxScreen openInstance;

    private int y;
    private int visibleRows = 1;

    // pozycja/rozmiar paska postępu utworu - ustawiane w initMusic(), rysowane co klatkę w render()
    private int progX, progY, progW, progH;
    private int panelX1, panelX2;
    // wiersz grającego utworu - podświetlany delikatnym zielonym tłem w render(), jak w Spotify
    private int musicLeft;
    private int playingRowY = -1;
    private int guiColorsY = -1;
    private int controlRowY = -1;
    private int controlClusterX = -1;
    private int controlClusterW = 0;
    private long openedAtNanos = System.nanoTime();
    private long closingAtNanos = 0L;
    private boolean closing = false;

    public AdixFxScreen() {
        super(Text.literal("AdixFX"));
    }

    /** Wołane z konwertera mp3 w tle / odtwarzacza, żeby GUI (np. ▶ przy utworze) od razu się odświeżyło. */
    static void refreshIfOpen() {
        if (openInstance != null && MinecraftClient.getInstance().currentScreen == openInstance) {
            openInstance.clearAndInit();
        }
    }

    private static String fmtTime(float seconds) {
        int s = Math.max(0, Math.round(seconds));
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    /** Suwak 0-200% (krok 5%), w nowym płaskim stylu. Zapis do pliku po puszczeniu myszy. */
    private static final class PercentSlider extends SliderWidget {
        private final String prefix;
        private final Consumer<Float> setter;

        PercentSlider(int x, int y, int w, int h, String prefix, float value, Consumer<Float> setter) {
            super(x, y, w, h, Text.empty(), Math.max(0f, Math.min(2f, value)) / 2.0);
            this.prefix = prefix;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(prefix + Math.round(this.value * 200.0) + "%"));
        }

        @Override
        protected void applyValue() {
            this.value = Math.round(this.value * 40.0) / 40.0;
            setter.accept((float) (this.value * 2.0));
        }

        private boolean dragging;

        private void updateFromMouse(double mouseX) {
            double fraction = (mouseX - getX()) / Math.max(1.0, getWidth());
            this.value = Math.max(0.0, Math.min(1.0, fraction));
            applyValue();
            updateMessage();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            if (handled && button == 0) {
                dragging = true;
                updateFromMouse(mouseX);
            }
            return handled;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (button == 0 && dragging) {
                updateFromMouse(mouseX);
                return true;
            }
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0) dragging = false;
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            Config.save();
        }

        @Override
        public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int x1 = getX(), y1 = getY(), x2 = getX() + getWidth(), y2 = getY() + getHeight();
            GuiUtil.roundedRect(ctx, x1, y1, x2, y2, GuiUtil.CARD, GuiUtil.PANEL, 4);
            int fillX = x1 + (int) ((x2 - x1) * this.value);
            if (fillX > x1) ctx.fill(x1, y1, fillX, y2, GuiUtil.accent());
            ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(),
                    x1 + (x2 - x1) / 2, y1 + (getHeight() - 8) / 2, GuiUtil.TEXT);
        }
    }

    /** Małe, wyraźnie oddzielone przyciski transportu muzyki. */
    private static final class FramedButton extends FlatButton {
        FramedButton(int x, int y, int w, int h, Text message, PressAction onPress) {
            super(x, y, w, h, message, onPress);
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            super.renderWidget(ctx, mouseX, mouseY, delta);
            int x = getX(), y = getY(), r = x + getWidth(), b = y + getHeight();
            int line = GuiUtil.accent();
            ctx.fill(x, y, r, y + 1, line);
            ctx.fill(x, b - 1, r, b, line);
            ctx.fill(x, y, x + 1, b, line);
            ctx.fill(r - 1, y, r, b, line);
        }
    }

    /** Mała, klikalna próbka koloru akcentu GUI. */
    private static final class ColorSwatch extends ButtonWidget {
        private final int colorIndex;

        ColorSwatch(int x, int y, int colorIndex, PressAction onPress) {
            super(x, y, 15, 13, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
            this.colorIndex = colorIndex;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            boolean selected = Config.d.guiColor == colorIndex;
            int border = selected ? 0xFFFFFFFF : (isHovered() ? 0xFF777777 : 0xFF303030);
            GuiUtil.roundedRect(ctx, getX(), getY(), getX() + 15, getY() + 13,
                    border, GuiUtil.PANEL, 8);
            GuiUtil.roundedRect(ctx, getX() + 2, getY() + 2, getX() + 13, getY() + 11,
                    GuiUtil.accentColor(colorIndex), border, 6);
        }
    }

    private static Text onOff(String label, boolean v) {
        return Text.literal(label + ": ").append(Text.literal(v ? "WŁ." : "WYŁ.").formatted(v ? Formatting.GREEN : Formatting.RED));
    }

    private void add(String label, int w, BooleanSupplier get, Consumer<Boolean> set) {
        addDrawableChild(new FlatButton(this.width / 2 - w / 2, y, w, 19, onOff(label, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            Config.save();
            clearAndInit();
        }));
        y += 22;
    }

    private void addPlain(Text label, Runnable action) {
        addDrawableChild(new FlatButton(this.width / 2 - W / 2, y, W, 19, label, b -> {
            action.run();
            Config.save();
            clearAndInit();
        }));
        y += 19;
    }

    private void addHalf(Text label, int col, boolean newRow, Runnable action) {
        int half = (W - 4) / 2;
        addDrawableChild(new FlatButton(this.width / 2 - W / 2 + col * (half + 4), y, half, 19, label, b -> {
            action.run();
            Config.save();
            clearAndInit();
        }));
        if (newRow) y += 19;
    }

    private static int indexOf(float[] arr, float v) {
        int best = 0;
        for (int i = 0; i < arr.length; i++) if (Math.abs(arr[i] - v) < Math.abs(arr[best] - v)) best = i;
        return best;
    }

    @Override
    protected void init() {
        openInstance = this;
        int cx = this.width / 2;
        y = page == Page.MUSIC ? 48 : 50;
        int w = page == Page.MUSIC ? MW : W;
        panelX1 = cx - w / 2 - 10;
        panelX2 = cx + w / 2 + 10;

        // zakładki
        addDrawableChild(new FlatButton(cx - W / 2, 24, (W - 4) / 2, 20, Text.literal("Hitboxy"), b -> {
            page = Page.HITBOXES;
            clearAndInit();
        }).accent(page == Page.HITBOXES));
        addDrawableChild(new FlatButton(cx + 2, 24, (W - 4) / 2, 20, Text.literal("Muzyka"), b -> {
            page = Page.MUSIC;
            clearAndInit();
        }).accent(page == Page.MUSIC));

        // mały, dyskretny przycisk kosza - tylko gdy jest tam coś do pokazania
        if (page == Page.MUSIC && !Config.d.removedBuiltins.isEmpty()) {
            addDrawableChild(new FlatButton(this.width - 24, 20, 20, 20, Text.literal("🗑"), b ->
                    this.client.setScreen(new TrashScreen(this))
            ));
        }

        if (page == Page.HITBOXES) initHitboxes(); else initMusic();

        addDrawableChild(new FlatButton(cx - W / 2, this.height - 24, W, 18, Text.literal("Zamknij"), b -> close()));
    }

    private void initHitboxes() {
        Config.Data d = Config.d;
        add("Hitboxy", W, () -> d.hitboxes, v -> d.hitboxes = v);
        add("Błyskawice (burza + rytm muzyki)", W, () -> d.stormLightning, v -> d.stormLightning = v);
        add("Chmury burzowe", W, () -> d.stormClouds, v -> d.stormClouds = v);
        add("Światła muzyczne", W, () -> d.musicLights, v -> d.musicLights = v);
        add("Błyskawice za zabójstwo", W, () -> d.killLightning, v -> d.killLightning = v);
        addPlain(Text.literal("Tryb: " + Config.mode().label), () -> d.mode = Config.mode().next().name());
        if (Config.mode() == Config.Mode.PULSE) {
            int[] idleColors = {0xFF000000, 0xFFFFFFFF, 0xFFFF4040, 0xFF40A0FF, 0xFFFFD040, 0xFFB0B0B0};
            int current = 0;
            for (int i = 0; i < idleColors.length; i++) if (idleColors[i] == d.pulseIdleColor) current = i;
            final int selected = current;
            addPlain(Text.literal("Kolor przy cichej muzyce: " + new String[]{"Czarny", "Biały", "Czerwony", "Niebieski", "Żółty", "Szary"}[current]),
                    () -> d.pulseIdleColor = idleColors[(selected + 1) % idleColors.length]);
        }
        if (Config.mode() == Config.Mode.STATIC) {
            addPlain(Text.literal("Wylosuj nowy kolor"), Config::randomizeColor);
        }
        addDrawableChild(new FlatButton(this.width / 2 - W / 2, y, W, 16,
                Text.literal("Edytuj HUD muzyki"), b ->
                        this.client.setScreen(new MusicHudEditorScreen(this))));
        y += 19;

        int si = indexOf(SPEEDS, d.speed);
        addPlain(Text.literal("Szybkość zmiany kolorów: x" + SPEEDS[si]), () -> d.speed = SPEEDS[(si + 1) % SPEEDS.length]);
        int ri = 0;
        for (int i = 0; i < RANGES.length; i++) if (RANGES[i] == d.range) ri = i;
        int rIndex = ri;
        addPlain(Text.literal("Zasięg: " + d.range + " bloków"), () -> d.range = RANGES[(rIndex + 1) % RANGES.length]);

        addHalf(onOff("Gracze", d.players), 0, false, () -> d.players = !d.players);
        addHalf(onOff("Wrogie", d.hostile), 1, true, () -> d.hostile = !d.hostile);
        addHalf(onOff("Przyjazne", d.passive), 0, false, () -> d.passive = !d.passive);
        addHalf(onOff("Przedmioty", d.items), 1, true, () -> d.items = !d.items);
        addHalf(onOff("Inne", d.others), 0, true, () -> d.others = !d.others);
    }

    private void initMusic() {
        Config.Data d = Config.d;
        int cx = this.width / 2;
        int left = cx - MW / 2;
        musicLeft = left;
        playingRowY = -1;

        // Główny przełącznik muzyki po lewej, a po prawej osobny przełącznik HUD-u utworu.
        addDrawableChild(new FlatButton(left, y + 4, MW - 78, 12, onOff("Muzyka", d.music), b -> {
            d.music = !d.music;
            if (!d.music) Music.stop();
            Config.save();
            clearAndInit();
        }));
        // Nazwa funkcji nad przełącznikiem po prawej stronie panelu.
        // Etykieta jest rysowana w render() nad przełącznikiem HUD.
        addDrawableChild(new FlatButton(left + MW - 72, y + 4, 72, 12,
                Text.literal(d.songHud ? "ON" : "OFF").formatted(d.songHud ? Formatting.GREEN : Formatting.RED), b -> {
                    d.songHud = !d.songHud;
                    Config.save();
                    clearAndInit();
                }));
        y += 16;

        int scrollIndex = 2;
        for (int i = 0; i < TITLE_SCROLL_SPEEDS.length; i++) {
            if (Math.abs(TITLE_SCROLL_SPEEDS[i] - d.titleScrollSpeed) < Math.abs(TITLE_SCROLL_SPEEDS[scrollIndex] - d.titleScrollSpeed)) scrollIndex = i;
        }
        final int selectedScrollIndex = scrollIndex;
        String scrollLabel = selectedScrollIndex == 2 ? "Przewijanie nazw: x1.0 (podstawowa)" :
                "Przewijanie nazw: x" + String.format(java.util.Locale.ROOT, "%.2f", TITLE_SCROLL_SPEEDS[selectedScrollIndex] / 22.0f);
        addDrawableChild(new FlatButton(left, y, MW, 12, Text.literal(scrollLabel), b -> {
            d.titleScrollSpeed = TITLE_SCROLL_SPEEDS[(selectedScrollIndex + 1) % TITLE_SCROLL_SPEEDS.length];
            Config.save();
            clearAndInit();
        }));
        y += 13;

        // Widoczne próbki 6 kolorów akcentu GUI.
        guiColorsY = y;
        int swatchStart = left + 108;
        for (int i = 0; i < 6; i++) {
            final int colorIndex = i;
            addDrawableChild(new ColorSwatch(swatchStart + i * 18, y + 1, i, b -> {
                Config.d.guiColor = colorIndex;
                Config.save();
                clearAndInit();
            }));
        }
        y += 14;

        addDrawableChild(new PercentSlider(left, y, MW, 12, "Głośność ogólna: ", d.volume, v -> d.volume = v));
        y += 13;

        // Większy rząd sterowania; każdy przycisk ma własną ramkę i odstęp.
        int ctrlH = 22;
        controlRowY = y;
        addDrawableChild(new FramedButton(left, y + 2, 20, 18, Text.literal("+"), b ->
                this.client.setScreen(new AddTrackScreen(this))
        ));

        int playSize = 20, skipSize = 16, gap = 7;
        int clusterW = skipSize + gap + playSize + gap + skipSize;
        int clusterX = cx - clusterW / 2;
        controlClusterX = clusterX;
        controlClusterW = clusterW;
        addDrawableChild(new FramedButton(clusterX, y + (ctrlH - skipSize) / 2, skipSize, skipSize,
                Text.literal("⏮"), b -> {
                    Music.previous();
                    clearAndInit();
                }));
        addDrawableChild(new RoundIconButton(clusterX + skipSize + gap, y + (ctrlH - playSize) / 2, playSize,
                Text.literal(Music.isPlayingNow() ? "⏸" : "▶"), b -> {
                    Music.togglePlayPause();
                    clearAndInit();
                }));
        addDrawableChild(new FramedButton(clusterX + skipSize + gap + playSize + gap, y + (ctrlH - skipSize) / 2, skipSize, skipSize,
                Text.literal("⏭"), b -> {
                    Music.skip();
                    clearAndInit();
                }));
        y += ctrlH + 2;

        // pasek postępu utworu: suwak + "przesłuchane / długość" po prawej - rysowane co klatkę
        // w render(), żeby zawsze pokazywał aktualny czas; suwak przeciągany tylko, gdy da się
        // przewijać obecny utwór (własne, przeciągnięte pliki - patrz Music.isSeekable()).
        progX = left;
        progY = y;
        progW = MW - 78;
        progH = 12;
        if (Music.isSeekable()) {
            addDrawableChild(new SeekBar(progX, progY, progW, progH));
        }
        y += 13;

        // lista utworów (wbudowane + własne, z przewijaniem kółkiem myszy)
        List<Playlist.Track> all = Playlist.allTracks();
        visibleRows = Math.max(1, (this.height - 45 - y) / ROW);
        int maxScroll = Math.max(0, all.size() - visibleRows);
        trackScroll = Math.max(0, Math.min(maxScroll, trackScroll));

        Playlist.Track nowPlaying = Music.playing();
        int nameW = MW - 26 - 4 - 84 - 4 - 16 - 4 - 16 - 4;   // WŁ | nazwa | głośność | ✎ | ✕
        int end = Math.min(all.size(), trackScroll + visibleRows);
        for (int i = trackScroll; i < end; i++) {
            Playlist.Track t = all.get(i);
            boolean en = Playlist.enabled(t);
            boolean playingThis = Objects.equals(t, nowPlaying);
            if (playingThis) playingRowY = y;

            // [WŁ/WYŁ]
            addDrawableChild(new FlatButton(left, y + 1, 26, 12,
                    Text.literal(en ? "WŁ" : "WYŁ").formatted(en ? Formatting.GREEN : Formatting.RED), b -> {
                        Playlist.setEnabled(t, !Playlist.enabled(t));
                        clearAndInit();
                    }));

            // [nazwa] - kliknięcie włącza ten utwór, potem losowe z zaznaczonych.
            // Utwór, który gra teraz, ma zielony tekst i ikonę ▶/⏸ - jak podświetlony wiersz w Spotify.
            int rowNameW = nameW;
            Text nameText = Text.literal((playingThis ? (Music.isPaused() ? "⏸ " : "▶ ") : "") + Playlist.displayName(t))
                    .formatted(playingThis ? Formatting.GREEN : Formatting.WHITE);
            addDrawableChild(new FlatButton(left + 30, y + 1, rowNameW, 12, nameText, b -> {
                Music.play(t);
                clearAndInit();
            }));

            // [głośność utworu 0-200%]
            int volX = left + 30 + nameW + 4;
            addDrawableChild(new PercentSlider(volX, y + 1, 84, 12, "", Playlist.volume(t),
                    v -> Playlist.setVolume(t, v)));

            // [✎] zmiana nazwy
            int renameX = volX + 84 + 4;
            addDrawableChild(new FlatButton(renameX, y + 1, 16, 12, Text.literal("✎"),
                    b -> this.client.setScreen(new RenameScreen(this, t))));

            // [✕] usunięcie (z potwierdzeniem) - naprawdę kasuje utwór, nie tylko ukrywa
            int delX = renameX + 16 + 4;
            addDrawableChild(new FlatButton(delX, y + 1, 16, 12, Text.literal("✕").formatted(Formatting.RED),
                    b -> this.client.setScreen(new ConfirmDeleteScreen(this, t))));

            y += ROW;
        }

        // utwory w trakcie konwersji / z błędem - szare wiersze bez kontrolek
        for (CustomTracks.Entry e : Config.d.customTracks) {
            if (CustomTracks.READY.equals(e.status)) continue;
            boolean error = CustomTracks.ERROR.equals(e.status);
            String text = e.name + (error ? " — błąd konwersji" : " — dodawanie...");
            FlatButton pending = new FlatButton(left, y + 1, error ? MW - 20 : MW, 12,
                    Text.literal(text).formatted(Formatting.DARK_GRAY), b -> {});
            pending.active = false;
            addDrawableChild(pending);
            if (error) {
                addDrawableChild(new FlatButton(left + MW - 16, y + 1, 16, 12, Text.literal("✕").formatted(Formatting.RED), b -> {
                    CustomTracks.remove(e.id);
                    clearAndInit();
                }));
            }
            y += ROW;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (page == Page.MUSIC && Playlist.allTracks().size() > visibleRows) {
            trackScroll -= (int) Math.signum(vertical);
            clearAndInit();
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float opening = Math.min(1.0f, (now - openedAtNanos) / 190_000_000.0f);
        float closingProgress = closing ? Math.min(1.0f, (now - closingAtNanos) / 150_000_000.0f) : 0.0f;
        if (closing && closingProgress >= 1.0f) {
            if (this.client != null) this.client.setScreen(null);
            return;
        }
        this.renderBackground(ctx, mouseX, mouseY, delta);

        int slide = Math.round((1.0f - opening) * 9.0f + closingProgress * 7.0f);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0.0, slide, 0.0);
        GuiUtil.roundedRect(ctx, panelX1, 2, panelX2, this.height - 2,
                0xF20D1119, 0x00000000, 14);
        GuiUtil.roundedRect(ctx, panelX1 + 10, 14, panelX2 - 10, 15,
                0xFF303744, GuiUtil.PANEL, 1);
        int accentWidth = Math.max(1, Math.round((panelX2 - panelX1 - 24) * opening));
        GuiUtil.roundedRect(ctx, panelX1 + 12, 7, panelX1 + 12 + accentWidth, 9,
                GuiUtil.accent(), GuiUtil.PANEL, 1);
        ctx.drawCenteredTextWithShadow(this.textRenderer, GuiUtil.font(Text.literal("ADIXFX  •  CONTROL CENTER")),
                this.width / 2, 9, GuiUtil.TEXT_DIM);

        if (page == Page.MUSIC && playingRowY >= 0) {
            GuiUtil.roundedRect(ctx, musicLeft - 4, playingRowY - 1, musicLeft + MW + 4, playingRowY + 15,
                    GuiUtil.accentSoft(), GuiUtil.PANEL, 5);
        }
        if (page == Page.MUSIC && controlRowY >= 0 && controlClusterX >= 0) {
            // Dyskretne tło oddziela rząd transportu od suwaka głośności i listy utworów.
            GuiUtil.roundedRect(ctx, controlClusterX - 5, controlRowY - 1,
                    controlClusterX + controlClusterW + 5, controlRowY + 23,
                    GuiUtil.CARD, GuiUtil.accent(), 5);
            GuiUtil.roundedRect(ctx, musicLeft - 2, controlRowY,
                    musicLeft + 22, controlRowY + 22, GuiUtil.CARD, GuiUtil.accent(), 5);
        }

        super.render(ctx, mouseX, mouseY, delta);

        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 260_000_000.0);
        int pulseWidth = Math.max(18, Math.round(26 + pulse * 12));
        GuiUtil.roundedRect(ctx, this.width / 2 - pulseWidth / 2, this.height - 23,
                this.width / 2 + pulseWidth / 2, this.height - 21, GuiUtil.accent(), GuiUtil.PANEL, 1);

        if (page == Page.HITBOXES && Config.mode() == Config.Mode.STATIC) {
            // podgląd wylosowanego koloru
            int c = 0xFF000000 | Config.d.fixedColor;
            GuiUtil.roundedRect(ctx, this.width / 2 + W / 2 + 8, 46 + 48, this.width / 2 + W / 2 + 28, 46 + 68, c, GuiUtil.PANEL, 3);
        }
        if (page == Page.MUSIC) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("COLORS GUI"),
                    musicLeft + 8, guiColorsY + 4, GuiUtil.TEXT_DIM);
            // pasek postępu + czas - rysowane tu, nie w init(), żeby zawsze pokazywały aktualny stan
            float elapsed = Music.elapsedSeconds();
            float total = Music.totalSeconds();
            int by1 = progY + (progH - 3) / 2, by2 = by1 + 3;
            GuiUtil.roundedRect(ctx, progX, by1, progX + progW, by2, GuiUtil.TRACK_BG, GuiUtil.PANEL, 2);
            if (total > 0f && Music.playing() != null) {
                int fillX = progX + (int) (progW * Math.max(0f, Math.min(1f, elapsed / total)));
                if (fillX > progX) ctx.fill(progX, by1, fillX, by2, GuiUtil.accent());
            }
            String timeLabel = (Music.playing() != null) ? (fmtTime(elapsed) + " / " + fmtTime(total)) : "0:00 / 0:00";
            ctx.drawTextWithShadow(this.textRenderer, GuiUtil.font(Text.literal(timeLabel)),
                    progX + progW + 8, progY + (progH - 8) / 2, GuiUtil.TEXT_DIM);

            if (Playlist.allTracks().isEmpty() && Config.d.customTracks.isEmpty()) {
                ctx.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal("Brak utworów — kliknij \"+ Dodaj utwór\"").formatted(Formatting.GRAY),
                        this.width / 2, progY + 40, 0xFFFFFF);
            }
        }
        ctx.getMatrices().pop();
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            closingAtNanos = System.nanoTime();
        }
    }

    @Override
    public void removed() {
        Config.save();
        if (openInstance == this) openInstance = null;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
