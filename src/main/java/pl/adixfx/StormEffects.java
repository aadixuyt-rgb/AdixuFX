package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Widoczne, częste błyskawice nad graczem podczas burzy i krótkie błyski do rytmu muzyki. */
public final class StormEffects {
    private static long nextStormFlash;
    private static long flashStarted = Long.MIN_VALUE;
    private static long flashSeed;
    private static float flashPower;
    private static boolean wasStorm;
    private static boolean beatWasActive;
    private static long beatStarted = Long.MIN_VALUE;
    private static long beatSeed;
    private StormEffects() {}

    public static float flashIntensity() {
        if (flashStarted == Long.MIN_VALUE) return 0f;
        float t = (System.currentTimeMillis() - flashStarted) / 430f;
        return t < 0 || t > 1 ? 0f : flashPower * (1f - t);
    }

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || !Config.d.stormLightning) {
            wasStorm = false;
            beatWasActive = false;
            return;
        }
        long now = System.currentTimeMillis();
        boolean storm = mc.world.isThundering() || mc.world.isRaining();
        boolean music = Config.d.music && Music.isPlayingNow();

        if (storm && !wasStorm) {
            nextStormFlash = now + 700L;
            wasStorm = true;
        } else if (!storm) {
            wasStorm = false;
        }
        if (storm && now >= nextStormFlash) {
            flashStarted = now;
            flashSeed = now * 0x9E3779B97F4A7C15L;
            flashPower = 1.0f;
            nextStormFlash = now + 1800L + new Random(flashSeed).nextInt(2600);
        }

        float pulse = music ? BeatSync.pulse(Music.elapsedSeconds()) : 0f;
        if (pulse > 0.52f && !beatWasActive) {
            beatStarted = now;
            beatSeed = now ^ 0xD1B54A32D192ED03L;
        }
        beatWasActive = pulse > 0.16f;
        boolean musicFlash = music && beatStarted != Long.MIN_VALUE && now - beatStarted >= 0
                && now - beatStarted < 240;
        boolean stormFlash = flashStarted != Long.MIN_VALUE && now - flashStarted >= 0
                && now - flashStarted < 430;
        if (!musicFlash && !stormFlash) return;

        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider providers = ctx.consumers();
        if (matrices == null || providers == null) return;
        Vec3d camera = ctx.camera().getPos();
        int color = GuiUtil.accent();
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;
        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();

        if (stormFlash) {
            float fade = 1f - (now - flashStarted) / 430f;
            drawBolt(lines, entry, camera, new Random(flashSeed), r, g, b, 0.95f * fade, 12.0, 23.0);
        }
        if (musicFlash) {
            float fade = 1f - (now - beatStarted) / 240f;
            drawBolt(lines, entry, camera, new Random(beatSeed), r, g, b, 0.82f * fade, 5.0, 13.0);
        }
    }

    private static void drawBolt(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
                                 Random random, float r, float g, float b, float alpha,
                                 double minDistance, double maxDistance) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
        double x = camera.x + Math.cos(angle) * distance;
        double z = camera.z + Math.sin(angle) * distance;
        double top = camera.y + 23.0;
        double bottom = camera.y - 4.0;
        double px = x, py = top, pz = z;
        for (int i = 1; i <= 13; i++) {
            double ny = top + (bottom - top) * i / 13.0;
            double nx = x + (random.nextDouble() - 0.5) * 3.4;
            double nz = z + (random.nextDouble() - 0.5) * 3.4;
            segment(out, entry, camera, px, py, pz, nx, ny, nz, r, g, b, alpha);
            segment(out, entry, camera, px + 0.055, py, pz + 0.055,
                    nx + 0.055, ny, nz + 0.055, 1f, 1f, 1f, alpha);
            if (i > 2 && i < 12 && random.nextFloat() < 0.43f) {
                double ex = nx + (random.nextDouble() - 0.5) * 7.0;
                double ey = ny - 2.0 - random.nextDouble() * 5.0;
                double ez = nz + (random.nextDouble() - 0.5) * 7.0;
                segment(out, entry, camera, nx, ny, nz, ex, ey, ez, r, g, b, alpha * 0.9f);
            }
            px = nx; py = ny; pz = nz;
        }
    }

    private static void segment(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
                                double x1, double y1, double z1, double x2, double y2, double z2,
                                float r, float g, float b, float a) {
        float dx = (float)(x2-x1), dy = (float)(y2-y1), dz = (float)(z2-z1);
        float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-5f) return;
        dx/=len; dy/=len; dz/=len;
        out.vertex(entry,(float)(x1-camera.x),(float)(y1-camera.y),(float)(z1-camera.z))
                .color(r,g,b,a).normal(entry,dx,dy,dz);
        out.vertex(entry,(float)(x2-camera.x),(float)(y2-camera.y),(float)(z2-camera.z))
                .color(r,g,b,a).normal(entry,dx,dy,dz);
    }
}
