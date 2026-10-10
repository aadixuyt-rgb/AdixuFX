package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Zmiana nazwy utworu. */
public class RenameScreen extends Screen {
    private final Screen parent;
    private final Playlist.Track track;
    private TextFieldWidget field;

    public RenameScreen(Screen parent, Playlist.Track track) {
        super(Text.literal("Nazwa utworu"));
        this.parent = parent;
        this.track = track;
    }

    @Override
    protected void init() {
        int cx = this.width / 2, cy = this.height / 2;
        field = new TextFieldWidget(this.textRenderer, cx - 110, cy - 10, 220, 20, Text.empty());
        field.setMaxLength(40);
        field.setText(Playlist.displayName(track));
        addDrawableChild(field);
        setInitialFocus(field);

        addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> save())
                .dimensions(cx - 110, cy + 18, 72, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Domyślna"), b -> {
            Playlist.setName(track, null);
            close();
        }).dimensions(cx - 36, cy + 18, 72, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Anuluj"), b -> close())
                .dimensions(cx + 38, cy + 18, 72, 20).build());
    }

    private void save() {
        Playlist.setName(track, field.getText());
        close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {   // Enter
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Zmień nazwę utworu").formatted(Formatting.WHITE),
                this.width / 2, this.height / 2 - 28, 0xFFFFFF);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
