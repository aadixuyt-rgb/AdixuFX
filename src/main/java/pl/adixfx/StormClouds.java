package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/**
 * Layered storm-cloud canopy. The cloud geometry is stationary (no bobbing or pulsing);
 * only a restrained accent tint responds to the music level.
 */
public final class StormClouds {
    private static final int LATITUDE_STEPS = 7;
    private static final int LONGITUDE_STEPS = 18;

    private StormClouds() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.d.stormClouds || mc.world == null || mc.player == null
                || !mc.world.isThundering()) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider providers = context.consumers();
        if (matrices == null || providers == null) return;

        Vec3d camera = context.camera().getPos();
        Vec3d player = mc.player.getLerpedPos(context.tickCounter().getTickDelta(false));
        // Keep the storm canopy above the player, not as a halo around their body.
        double baseY = Math.max(player.y + 24.0, 112.0);
        double centerX = Math.floor(player.x / 48.0) * 48.0 + 24.0;
        double centerZ = Math.floor(player.z / 48.0) * 48.0 + 24.0;

        float loudness = Config.d.music && Music.isPlayingNow()
                ? BeatSync.level(Music.elapsedSeconds()) : 0.0f;
        float glow = clamp(loudness, 0.0f, 1.0f) * 0.30f;

        VertexConsumer quads = providers.getBuffer(RenderLayer.getDebugQuads());
        MatrixStack.Entry entry = matrices.peek();

        // A wide, dark shelf forms the underside of a cumulonimbus-like cloud bank.
        puff(quads, entry, camera, centerX, baseY, centerZ,
                30.0, 5.5, 23.0, 0.19f, 0.22f, 0.27f, 0.96f);
        // Uneven upper towers and rounded lobes break the silhouette into storm layers.
        for (int i = 0; i < 9; i++) {
            double angle = Math.PI * 2.0 * i / 9.0;
            double radius = 10.0 + (i % 3) * 4.0;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius * 0.72;
            double y = baseY + 3.0 + (i % 3) * 1.8;
            double rx = 8.0 + (i % 2) * 2.5;
            double rz = 7.0 + (i % 3);
            puff(quads, entry, camera, x, y, z, rx, 7.0 + (i % 2) * 2.0, rz,
                    0.29f, 0.32f, 0.37f, 0.94f);
        }
        // A subtle accent wash, never a neon outline or a separate animated object.
        if (glow > 0.005f) {
            int accent = GuiUtil.accent();
            float r = ((accent >> 16) & 255) / 255.0f;
            float g = ((accent >> 8) & 255) / 255.0f;
            float b = (accent & 255) / 255.0f;
            puff(quads, entry, camera, centerX, baseY + 2.0, centerZ,
                    28.0, 4.8, 21.0, r, g, b, glow);
        }

        if (providers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getDebugQuads());
        }
    }

    private static void puff(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
