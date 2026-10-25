package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Napisy na ekranie dla wybranych utworów (tylko własne/przeciągnięte - mają dokładną
 * pozycję odtwarzania z CustomPlayer). Linie są równo rozłożone na całej długości utworu
 * (nie mamy dokładnego timingu wokalu), z krótkim fade in/out między liniami.
 * Włącznik WŁ/WYŁ w GUI pokazuje się tylko przy utworach, które tu mają wpis.
 */
public final class Lyrics {
    private Lyrics() {}

    private static final Map<String, List<String>> SONGS = new LinkedHashMap<>();

    static {
        SONGS.put("undress rehearsal", List.of(
                "Ooh, la la la, la la, la la la",
                "Ooh, la la la",
                "You deserve more than a cameo",
                "How are you not in my video?",
                "Lets take it back to my studio",
                "And we can let it roll tonight",
                "Girl, come with me now",
                "Your scene's about to start",
                "I do believe I can see you're a star",
                "Don't know your name but you're made for the part",
                "Cause you're perfect, babe",
                "Time to work it, babe",
                "It's your undress rehearsal",
                "So take your clothes off",
                "Show me how you show off",
                "It's your undress rehearsal",
                "You the main attraction",
                "Lights, camera, action",
                "Ooh, la la la, la la, la la la",
                "Ooh, la la la, la la la la la la",
                "Ooh, la la la, la la, la la la",
                "Ooh, la la la",
                "No, you don't need to audition, babe",
                "Don't need no headshots or pictures, babe",
                "There's no contract to negotiate",
                "So we should let it roll tonight",
                "Girl, come with me now",
                "Your scene's about to start",
                "I do believe I can see you're a star",
                "Don't know your name but you're made for the part",
                "Cause you're perfect, babe",
                "Time to work it, babe",
                "It's your undress rehearsal",
                "So take your clothes off",
                "Show me how you show off",
                "It's your undress rehearsal",
                "You the main attraction",
                "Lights, camera, action",
                "Ooh, la la la, la la, la la la",
                "Ooh, la la la, la la la la la la",
                "Ooh, la la la, la la, la la la",
                "Ooh, la la la, la la la la la la"
        ));
    }

    private static String key(Playlist.Track t) {
        return Playlist.displayName(t).trim().toLowerCase();
    }

    /** true = ten utwór ma zarejestrowane napisy (więc GUI pokazuje przy nim włącznik). */
    public static boolean has(Playlist.Track t) {
        return SONGS.containsKey(key(t));
    }

    public static boolean enabled(Playlist.Track t) {
        return Config.d.lyricsOn.getOrDefault(t.id(), true);
    }

    public static void setEnabled(Playlist.Track t, boolean v) {
        Config.d.lyricsOn.put(t.id(), v);
        Config.save();
    }

    public static void render(DrawContext ctx, float tickDelta) {
        Playlist.Track t = Music.playing();
        if (t == null || !t.isCustom() || !Music.isPlayingNow()) return;
        List<String> lines = SONGS.get(key(t));
        if (lines == null || lines.isEmpty() || !enabled(t)) return;

        long lenUs = CustomPlayer.microsecondLength();
        long posUs = CustomPlayer.microsecondPosition();
        if (lenUs <= 0) return;

        double slot = (double) lenUs / lines.size();
        int idx = (int) Math.min(lines.size() - 1, posUs / slot);
        double intoUs = posUs - idx * slot; // mikrosekundy od początku tej linii

        // zanikanie na 1 sekundę tuż przed startem następnej linii (nie proporcjonalnie do długości linii)
        long fadeUs = 1_000_000L;
        double fadeWindow = Math.min(fadeUs, slot * 0.9); // dla bardzo krótkich linii nie zanika dłużej niż ma trwać
        double remainingUs = slot - intoUs;

        float alpha = 1f;
        if (remainingUs < fadeWindow) alpha = (float) (remainingUs / fadeWindow);
        alpha = Math.max(0f, Math.min(1f, alpha));
        if (alpha <= 0.01f) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        int a = Math.round(alpha * 255);
        int color = (a << 24) | 0xFFFFFF;

        int cx = ctx.getScaledWindowWidth() / 2;
        int y = (int) (ctx.getScaledWindowHeight() * 0.72);
        ctx.drawCenteredTextWithShadow(mc.textRenderer,
                Text.literal(lines.get(idx)).formatted(Formatting.BOLD), cx, y, color);
    }
}
