package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Niezależne od chmur błyskawice w kolorze akcentu GUI.
 * Podczas burzy pojawiają się losowo, a podczas odtwarzania muzyki
 * dodatkowo uderzają w wykryty rytm i jaśnieją wraz z głośnością utworu.
 */
public final class StormEffects {
    private static final long AMBIENT_CYCLE_MS = 7600L;
    private static final long AMBIENT_FLASH_MS = 190L;
    private static final long MUSIC_FLASH_MS = 210L;

    private static boolean beatWasActive;
    private static long beatFlashStartedAt = Long.MIN_VALUE;
    private static long beatFlashSeed;
    private static long beatSequence;
    private static float beatFlashStrength;

    private StormEffects() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || !Config.d.stormLightning) return;

        boolean storm = client.world.isThundering();
        boolean music = Config.d.music && Music.isPlayingNow();
        if (!storm && !music) {
            beatWasActive = false;
            beatFlashStartedAt = Long.MIN_VALUE;
            return;
        }

        long now = System.currentTimeMillis();
        float pulse = music ? BeatSync.pulse(Music.elapsedSeconds()) : 0.0f;
        float level = music ? BeatSync.level(Music.elapsedSeconds()) : 0.0f;

        // BeatSync daje krótki impuls przy wykrytym uderzeniu basu.
        // Zapamiętujemy seed, żeby błyskawica nie zmieniała kształtu co klatkę.
        if (pulse >= 0.50f && !beatWasActive) {
            beatWasActive = true;
            beatFlashStartedAt = now;
            beatFlashSeed = (++beatSequence) * 0x9E3779B97F4A7C15L;
            beatFlashStrength = pulse;
        } else if (pulse < 0.16f) {
            beatWasActive = false;
        }

        boolean musicFlash = music && beatFlashStartedAt != Long.MIN_VALUE
                && now - beatFlashStartedAt >= 0L
                && now - beatFlashStartedAt <= MUSIC_FLASH_MS;

        long ambientCycle = Math.floorMod(now, AMBIENT_CYCLE_MS);
        boolean ambientFlash = storm && ambientCycle <= AMBIENT_FLASH_MS;
        if (!musicFlash && !ambientFlash) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider providers = context.consumers();
        if (matrices == null || providers == null) return;

        Vec3d camera = context.camera().getPos();
        int color = GuiUtil.accent();
        float red = ((color >> 16) & 255) / 255.0f;
        float green = ((color >> 8) & 255) / 255.0f;
        float blue = (color & 255) / 255.0f;

        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();

        if (musicFlash) {
            float fade = 1.0f - (now - beatFlashStartedAt) / (float) MUSIC_FLASH_MS;
            float outputGain = Music.playing() == null ? 0.0f : Music.gain(Music.playing());
            // Głośniejsze fragmenty i mocniejsze uderzenia dają jaśniejszy błysk.
            float intensity = clamp(fade, 0.0f, 1.0f)
                    * (0.18f + 0.82f * clamp(level, 0.0f, 1.0f))
                    * (0.35f + 0.65f * clamp(outputGain, 0.0f, 1.0f))
                    * (0.55f + 0.45f * clamp(beatFlashStrength, 0.0f, 1.0f));
            Random random = new Random(beatFlashSeed);
            drawBolt(lines, entry, camera, random, red, green, blue, intensity, 0.075);
        }

        if (ambientFlash) {
            long cycleSeed = Math.floorDiv(now, AMBIENT_CYCLE_MS);
            Random random = new Random(cycleSeed * 0xD1B54A32D192ED03L);
            float fade = 1.0f - ambientCycle / (float) AMBIENT_FLASH_MS;
            drawBolt(lines, entry, camera, random, red, green, blue,
                    0.82f * clamp(fade, 0.0f, 1.0f), 0.045);
        }
    }

    private static void drawBolt(VertexConsumer lines, MatrixStack.Entry entry, Vec3d camera,
                                 Random random, float red, float green, float blue,
                                 float intensity, double coreWidth) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 20.0 + random.nextDouble() * 17.0;
        double x = camera.x + Math.cos(angle) * distance;
        double z = camera.z + Math.sin(angle) * distance;
        double top = camera.y + 52.0;
        double bottom = Math.max(camera.y + 2.0, top - 39.0 - random.nextDouble() * 17.0);

        double px = x, py = top, pz = z;
        int segments = 12;
        for (int i = 1; i <= segments; i++) {
            double nextY = top + (bottom - top) * i / segments;
            double nextX = x + (random.nextDouble() - 0.5) * 6.5;
            double nextZ = z + (random.nextDouble() - 0.5) * 6.5;

            drawLine(lines, entry, camera, px, py, pz, nextX, nextY, nextZ,
                    red, green, blue, intensity);
            // Druga bliska krawędź daje błyskawicy lekko jaśniejszy rdzeń.
            drawLine(lines, entry, camera, px + coreWidth, py, pz + coreWidth,
                    nextX + coreWidth, nextY, nextZ + coreWidth,
                    red, green, blue, intensity * 0.52f);

            if (i >= 3 && i <= 10 && random.nextFloat() < 0.34f) {
                double branchX = nextX;
                double branchY = nextY;
                double branchZ = nextZ;
                double endX = branchX + (random.nextDouble() - 0.5) * 12.0;
                double endY = branchY - 4.0 - random.nextDouble() * 10.0;
                double endZ = branchZ + (random.nextDouble() - 0.5) * 12.0;
                drawLine(lines, entry, camera, branchX, branchY, branchZ,
                        endX, endY, endZ, red, green, blue, intensity * 0.82f);
            }
            px = nextX;
            py = nextY;
            pz = nextZ;
        }
    }

    private static void drawLine(VertexConsumer consumer, MatrixStack.Entry entry, Vec3d camera,
                                 double x1, double y1, double z1, double x2, double y2, double z2,
                                 float r, float g, float b, float alpha) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-5f) return;
        dx /= length;
        dy /= length;
        dz /= length;
        consumer.vertex(entry, (float) (x1 - camera.x), (float) (y1 - camera.y), (float) (z1 - camera.z))
                .color(r, g, b, alpha).normal(entry, dx, dy, dz);
        consumer.vertex(entry, (float) (x2 - camera.x), (float) (y2 - camera.y), (float) (z2 - camera.z))
                .color(r, g, b, alpha).normal(entry, dx, dy, dz);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
