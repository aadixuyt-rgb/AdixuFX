package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista tytułów wbudowanych utworów, które usunąłeś w grze - fizycznie nadal siedzą
 * w pliku moda (nie da się tego zrobić inaczej niż nową wersją moda), więc to tylko podgląd,
 * z którym idziesz do Claude, żeby je usunął naprawdę.
 */
public class TrashScreen extends Screen {
    private final Screen parent;

    public TrashScreen(Screen parent) {
        super(Text.literal("Kosz"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Zamknij"), b -> close())
                .dimensions(cx - 60, this.height - 28, 120, 20).build());
    }

    private List<String> names() {
        List<String> out = new ArrayList<>();
        for (Playlist.Track t : Playlist.TRACKS) {
            if (Config.d.removedBuiltins.contains(t.id())) out.add(t.name());
        }
        return out;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;
        List<String> names = names();

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Nadal w pliku moda - do usunięcia przy następnej wersji").formatted(Formatting.GRAY),
                cx, 40, 0xFFFFFF);

        if (names.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Kosz jest pusty").formatted(Formatting.GRAY), cx, 70, 0xFFFFFF);
            return;
        }
        int y = 66;
        for (String n : names) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("• " + n).formatted(Formatting.YELLOW),
                    cx, y, 0xFFFFFF);
            y += 12;
        }
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
