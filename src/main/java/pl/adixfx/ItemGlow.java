package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Miękka, kolorowa poświata wokół upuszczonych przedmiotów.
 * Kolor pochodzi z akcentu GUI, a zasięg i jasność delikatnie reagują na
 * analizowany poziom dźwięku oraz wykryte uderzenia basu.
 */
public final class ItemGlow {
    private static final int LATITUDE_STEPS = 6;
    private static final int LONGITUDE_STEPS = 18;
    private static final int MAX_ITEMS = 36;

    private ItemGlow() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.d.itemGlow || mc.world == null || mc.player == null) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider providers = context.consumers();
        if (matrices == null || providers == null) return;

        Vec3d camera = context.camera().getPos();
        float tickDelta = context.tickCounter().getTickDelta(false);
        boolean musicPlaying = Config.d.music && Music.isPlayingNow();
        float level = musicPlaying ? BeatSync.level(Music.elapsedSeconds()) : 0.0f;
        float beat = musicPlaying ? BeatSync.pulse(Music.elapsedSeconds()) : 0.0f;
        float audio = clamp(level * 0.65f + beat * 0.85f, 0.0f, 1.0f);

        int color = GuiUtil.accent();
        float red = ((color >> 16) & 255) / 255.0f;
        float green = ((color >> 8) & 255) / 255.0f;
        float blue = (color & 255) / 255.0f;
        double maxDistanceSquared = (double) Config.d.range * Config.d.range;

        VertexConsumer quads = providers.getBuffer(RenderLayer.getDebugQuads());
        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        int rendered = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity item) || item.isRemoved() || item.getStack().isEmpty()) continue;
            if (item.squaredDistanceTo(mc.player) > maxDistanceSquared) continue;
            if (rendered >= MAX_ITEMS) break;
            rendered++;

            Vec3d pos = item.getLerpedPos(tickDelta);
            // ItemEntity naturalnie lekko podskakuje; poświata podąża za nim.
            double cx = pos.x;
            double cy = pos.y + 0.16;
            double cz = pos.z;

            float radius = 0.36f + 0.10f * level + 0.18f * beat;
            float height = 0.22f + 0.045f * audio;
            float outerAlpha = 0.075f + 0.10f * level + 0.20f * beat;
            float innerAlpha = 0.11f + 0.12f * audio;

            // Dwie półprzezroczyste powłoki tworzą miękką poświatę, nie zasłaniając itemu.
            ellipsoid(quads, entry, camera, cx, cy, cz,
                    radius * 1.55, height * 1.45, radius * 1.55,
                    red, green, blue, clamp(outerAlpha, 0.05f, 0.34f));
            ellipsoid(quads, entry, camera, cx, cy, cz,
                    radius, height, radius,
                    red, green, blue, clamp(innerAlpha, 0.08f, 0.32f));

            // Cienki pierścień zapewnia czytelny kolorowy kontur także przy jasnym otoczeniu.
            ring(lines, entry, camera, cx, pos.y + 0.055, cz,
                    radius * (1.10 + audio * 0.22), red, green, blue,
                    clamp(0.46f + audio * 0.48f, 0.0f, 1.0f));
        }

        if (providers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getDebugQuads());
            immediate.draw(RenderLayer.getLines());
        }
    }

    private static void ellipsoid(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
                                  double cx, double cy, double cz,
                                  double rx, double ry, double rz,
                                  float red, float green, float blue, float alpha) {
        for (int lat = 0; lat < LATITUDE_STEPS; lat++) {
            double a0 = -Math.PI / 2.0 + Math.PI * lat / LATITUDE_STEPS;
            double a1 = -Math.PI / 2.0 + Math.PI * (lat + 1) / LATITUDE_STEPS;
            for (int lon = 0; lon < LONGITUDE_STEPS; lon++) {
                double b0 = Math.PI * 2.0 * lon / LONGITUDE_STEPS;
                double b1 = Math.PI * 2.0 * (lon + 1) / LONGITUDE_STEPS;
                vertex(out, entry, camera, cx, cy, cz, rx, ry, rz, a0, b0, red, green, blue, alpha);
                vertex(out, entry, camera, cx, cy, cz, rx, ry, rz, a1, b0, red, green, blue, alpha);
                vertex(out, entry, camera, cx, cy, cz, rx, ry, rz, a1, b1, red, green, blue, alpha);
                vertex(out, entry, camera, cx, cy, cz, rx, ry, rz, a0, b1, red, green, blue, alpha);
            }
        }
    }

    private static void vertex(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
                               double cx, double cy, double cz, double rx, double ry, double rz,
                               double latitude, double longitude,
                               float red, float green, float blue, float alpha) {
        double cosLat = Math.cos(latitude);
        float x = (float) (cx + Math.cos(longitude) * cosLat * rx - camera.x);
        float y = (float) (cy + Math.sin(latitude) * ry - camera.y);
        float z = (float) (cz + Math.sin(longitude) * cosLat * rz - camera.z);
        out.vertex(entry, x, y, z).color(red, green, blue, alpha);
    }

    private static void ring(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
                             double cx, double y, double cz, double radius,
                             float red, float green, float blue, float alpha) {
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            double x0 = cx + Math.cos(a0) * radius;
            double z0 = cz + Math.sin(a0) * radius;
            double x1 = cx + Math.cos(a1) * radius;
            double z1 = cz + Math.sin(a1) * radius;
            float dx = (float) (x1 - x0);
            float dz = (float) (z1 - z0);
            float len = (float) Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0E-6f) continue;
            out.vertex(entry, (float) (x0 - camera.x), (float) (y - camera.y), (float) (z0 - camera.z))
                    .color(red, green, blue, alpha).normal(entry, 0.0f, 1.0f, 0.0f);
            out.vertex(entry, (float) (x1 - camera.x), (float) (y - camera.y), (float) (z1 - camera.z))
                    .color(red, green, blue, alpha).normal(entry, 0.0f, 1.0f, 0.0f);
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
