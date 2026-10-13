package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Path;
import java.util.List;

/** Okno, na które przeciągasz plik mp3 z komputera prosto na okno gry. */
public class DropFileScreen extends Screen {
    private final Screen parent;

    public DropFileScreen(Screen parent) {
        super(Text.literal("Przeciągnij plik"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Anuluj"), b -> close())
                .dimensions(cx - 60, this.height / 2 + 60, 120, 20).build());
    }

    @Override
    public void filesDragged(List<Path> paths) {
        boolean any = false;
        for (Path p : paths) {
            String name = p.getFileName().toString().toLowerCase();
            if (name.endsWith(".mp3")) {
                AddTrackScreen.importMp3(p);
                any = true;
            }
        }
        if (any) close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2, cy = this.height / 2;
        ctx.fill(cx - 110, cy - 45, cx + 110, cy + 45, 0x33FFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Przeciągnij tutaj plik .mp3").formatted(Formatting.WHITE), cx, cy - 10, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("z komputera, na okno gry").formatted(Formatting.GRAY), cx, cy + 4, 0xFFFFFF);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
