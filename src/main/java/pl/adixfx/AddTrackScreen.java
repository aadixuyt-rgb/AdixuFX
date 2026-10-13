package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.nio.file.Path;

/** "Dodaj utwór": po lewej przeciąganie pliku na okno gry, po prawej wybór z eksploratora plików. */
public class AddTrackScreen extends Screen {
    private final Screen parent;

    public AddTrackScreen(Screen parent) {
        super(Text.literal("Dodaj utwór"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2, cy = this.height / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("📥 Przeciągnij plik"), b ->
                this.client.setScreen(new DropFileScreen(parent))
        ).dimensions(cx - 164, cy - 10, 160, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("📂 Znajdź plik"), b -> {
            this.client.setScreen(parent);
            openFileChooser();
        }).dimensions(cx + 4, cy - 10, 160, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Anuluj"), b -> close())
                .dimensions(cx - 60, cy + 20, 120, 20).build());
    }

    private void openFileChooser() {
        Thread th = new Thread(() -> {
            FileDialog dialog = new FileDialog((Frame) null, "Wybierz plik mp3", FileDialog.LOAD);
            dialog.setFilenameFilter((dir, n) -> n.toLowerCase().endsWith(".mp3"));
            dialog.setVisible(true);
            String dir = dialog.getDirectory();
            String file = dialog.getFile();
            dialog.dispose();
            if (dir != null && file != null) {
                Path p = new File(dir, file).toPath();
                MinecraftClient.getInstance().execute(() -> importMp3(p));
            }
        }, "AdixFX-FileChooser");
        th.setDaemon(true);
        th.start();
    }

    /** Dodaje plik mp3 jako nowy utwór: od razu pojawia się szary wiersz "dodawanie...",
     *  a w tle trwa konwersja; po zakończeniu wiersz sam się odświeży. */
    static void importMp3(Path mp3) {
        String base = mp3.getFileName().toString();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        CustomTracks.Entry entry = CustomTracks.begin(base);
        AdixFxScreen.refreshIfOpen();
        TrackConverter.convertAsync(mp3, Path.of(entry.file), ok -> {
            if (ok) CustomTracks.markReady(entry); else CustomTracks.markError(entry);
            AdixFxScreen.refreshIfOpen();
        });
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
