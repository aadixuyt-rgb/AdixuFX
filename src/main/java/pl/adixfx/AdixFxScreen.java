package pl.adixfx;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** GUI: strona "Hitboxy" i strona "Muzyka". Otwierane klawiszem (domyślnie prawy Shift). */
public class AdixFxScreen extends Screen {
    private enum Page { HITBOXES, MUSIC }

    private static Page page = Page.HITBOXES;
    private static final int W = 220;     // szerokość strony hitboxów i zakładek
    private static final int MW = 340;    // szerokość strony muzyki
    private static final int ROW = 22;
    private static final float[] SPEEDS = {0.25f, 0.5f, 1.0f, 2.0f, 4.0f};
    private static final int[] RANGES = {16, 32, 64, 128};

    private static int trackScroll = 0;

    private int y;
    private int visibleRows = 1;

    public AdixFxScreen() {
        super(Text.literal("AdixFX"));
    }

    /** Suwak 0-200% (krok 5%). Zapis do pliku po puszczeniu myszy / zamknięciu GUI. */
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

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            Config.save();
        }
    }

    private static Text onOff(String label, boolean v) {
        return Text.literal(label + ": ").append(Text.literal(v ? "WŁ." : "WYŁ.").formatted(v ? Formatting.GREEN : Formatting.RED));
    }

    private void add(String label, int w, BooleanSupplier get, Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(onOff(label, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            Config.save();
            clearAndInit();
        }).dimensions(this.width / 2 - w / 2, y, w, 20).build());
        y += 24;
    }

    private void addPlain(Text label, Runnable action) {
        addDrawableChild(ButtonWidget.builder(label, b -> {
            action.run();
            Config.save();
            clearAndInit();
        }).dimensions(this.width / 2 - W / 2, y, W, 20).build());
        y += 24;
    }

    private void addHalf(Text label, int col, boolean newRow, Runnable action) {
        int half = (W - 4) / 2;
        addDrawableChild(ButtonWidget.builder(label, b -> {
            action.run();
            Config.save();
            clearAndInit();
        }).dimensions(this.width / 2 - W / 2 + col * (half + 4), y, half, 20).build());
        if (newRow) y += 24;
    }

    private static int indexOf(float[] arr, float v) {
        int best = 0;
        for (int i = 0; i < arr.length; i++) if (Math.abs(arr[i] - v) < Math.abs(arr[best] - v)) best = i;
        return best;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        y = 40;
        // zakładki
        addDrawableChild(ButtonWidget.builder(Text.literal(page == Page.HITBOXES ? "» Hitboxy «" : "Hitboxy"), b -> {
            page = Page.HITBOXES;
            clearAndInit();
        }).dimensions(cx - W / 2, 14, (W - 4) / 2, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(page == Page.MUSIC ? "» Muzyka «" : "Muzyka"), b -> {
            page = Page.MUSIC;
            clearAndInit();
        }).dimensions(cx + 2, 14, (W - 4) / 2, 20).build());

        if (page == Page.HITBOXES) initHitboxes(); else initMusic();

        addDrawableChild(ButtonWidget.builder(Text.literal("Zamknij"), b -> close())
                .dimensions(cx - W / 2, this.height - 28, W, 20).build());
    }

    private void initHitboxes() {
        Config.Data d = Config.d;
        add("Hitboxy", W, () -> d.hitboxes, v -> d.hitboxes = v);
        addPlain(Text.literal("Tryb: " + Config.mode().label), () -> d.mode = Config.mode().next().name());
        if (Config.mode() == Config.Mode.STATIC) {
            addPlain(Text.literal("Wylosuj nowy kolor"), Config::randomizeColor);
        }
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

        add("Muzyka", MW, () -> d.music, v -> {
            d.music = v;
            if (!v) Music.stop();
        });

        addDrawableChild(new PercentSlider(left, y, MW, 20, "Głośność ogólna: ", d.volume, v -> d.volume = v));
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.literal("+ ADD"), b -> MusicImporter.chooseFile())
                .dimensions(left + MW - 78, y, 78, 20).build());
        y += 24;

        // Skip | Stop/Play (Stop gdy coś gra, Play gdy utwór jest zatrzymany lub nic nie gra)
        int half = (MW - 4) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Skip ⏭"), b -> {
            Music.skip();
            clearAndInit();
        }).dimensions(left, y, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(Music.isPlayingNow() ? "Stop ⏸" : "Play ▶"), b -> {
            Music.togglePlayPause();
            clearAndInit();
        }).dimensions(left + half + 4, y, half, 20).build());
        y += 28;

        // lista utworów (z przewijaniem kółkiem myszy)
        List<Playlist.Track> all = Playlist.TRACKS;
        visibleRows = Math.max(1, (this.height - 50 - y) / ROW);
        int maxScroll = Math.max(0, all.size() - visibleRows);
        trackScroll = Math.max(0, Math.min(maxScroll, trackScroll));

        Playlist.Track nowPlaying = Music.playing();
        int end = Math.min(all.size(), trackScroll + visibleRows);
        for (int i = trackScroll; i < end; i++) {
            Playlist.Track t = all.get(i);
            boolean en = Playlist.enabled(t);

            // [WŁ/WYŁ]
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(en ? "WŁ" : "WYŁ").formatted(en ? Formatting.GREEN : Formatting.RED), b -> {
                        Playlist.setEnabled(t, !Playlist.enabled(t));
                        clearAndInit();
                    }).dimensions(left, y, 30, 20).build());

            // [nazwa] - kliknięcie włącza ten utwór, potem losowe z zaznaczonych
            String label = (t == nowPlaying ? (Music.isPaused() ? "⏸ " : "▶ ") : "") + Playlist.displayName(t);
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                Music.play(t);
                clearAndInit();
            }).dimensions(left + 34, y, MW - 162, 20).build());

            // [głośność utworu 0-200%]
            addDrawableChild(new PercentSlider(left + MW - 120, y, 70, 20, "", Playlist.volume(t),
                    v -> Playlist.setVolume(t, v)));

            // [✎] zmiana nazwy
            addDrawableChild(ButtonWidget.builder(Text.literal("✎"),
                    b -> this.client.setScreen(new RenameScreen(this, t))).dimensions(left + MW - 44, y, 20, 20).build());
            if (t.id().startsWith("imported_")) {
                addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                    MusicImporter.deleteImportedTrack(t);
                    clearAndInit();
                }).dimensions(left + MW - 20, y, 20, 20).build());
            }
            y += ROW;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (page == Page.MUSIC && Playlist.TRACKS.size() > visibleRows) {
            trackScroll -= (int) Math.signum(vertical);
            clearAndInit();
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        if (page == Page.HITBOXES && Config.mode() == Config.Mode.STATIC) {
            // podgląd wylosowanego koloru
            int c = 0xFF000000 | Config.d.fixedColor;
            ctx.fill(this.width / 2 + W / 2 + 8, 40 + 48, this.width / 2 + W / 2 + 28, 40 + 68, c);
        }
        if (page == Page.MUSIC) {
            if (Playlist.TRACKS.isEmpty()) {
                ctx.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal("Brak utworów — wyślij pliki, a dodam je do listy").formatted(Formatting.GRAY),
                        this.width / 2, 40 + 24 * 2 + 24 + 28 + 6, 0xFFFFFF);
            }
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Klik w utwór = gra, potem losowe z włączonych • kółko myszy = przewijanie • ✎ = nazwa")
                            .formatted(Formatting.GRAY),
                    this.width / 2, this.height - 42, 0xFFFFFF);
        }
    }

    @Override
    public void removed() {
        Config.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
