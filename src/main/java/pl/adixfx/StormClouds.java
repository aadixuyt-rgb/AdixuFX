package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/**
 * Low-poly storm cloud canopy. Its neutral gray wireframe stays visible in a storm;
 * the GUI accent is layered over it according to the analyzed music loudness and beat.
 */
public final class StormClouds {
    private static final int PUFFS = 8;
    private static final int SEGMENTS = 18;

    private StormClouds() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.d.stormClouds || mc.world == null || mc.player == null
                || !mc.world.isThundering()) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider providers = context.consumers();
        if (matrices == null || providers == null) return;

        float loudness = Config.d.music && Music.isPlayingNow()
                ? BeatSync.level(Music.elapsedSeconds()) : 0.0f;
        float beat = Config.d.music && Music.isPlayingNow()
                ? BeatSync.pulse(Music.elapsedSeconds()) : 0.0f;
        float glow = clamp(0.10f + loudness * 0.58f + beat * 0.72f, 0.10f, 1.0f);

        long now = System.nanoTime();
        double time = now / 1_000_000_000.0;
        Vec3d camera = context.camera().getPos();
        Vec3d player = mc.player.getLerpedPos(context.tickCounter().getTickDelta(false));
        double cloudY = player.y + 3.6 + Math.sin(time * 0.55) * 0.10;

        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();

        // Soft graphite base; the accent becomes stronger on louder sections and bass hits.
        drawCloudLayer(lines, entry, camera, player.x, cloudY, player.z,
                time, 0.34f, 0.43f, 0.50f, 0.62f);
        int accent = GuiUtil.accent();
        float ar = ((accent >> 16) & 255) / 255.0f;
        float ag = ((accent >> 8) & 255) / 255.0f;
        float ab = (accent & 255) / 255.0f;
        drawCloudLayer(lines, entry, camera, player.x, cloudY, player.z,
                time, ar, ag, ab, 0.12f + glow * 0.78f);

        if (providers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getLines());
        }
    }

    private static void drawCloudLayer(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
                                       double centerX, double centerY, double centerZ,
                                       double time, float r, float g, float b, float alpha) {
        // A broad lower cloud plus eight rounded lobes make the silhouette read as one cloud.
        puff(out, entry, camera, centerX, centerY, centerZ,
                1.12, 0.34, 0.78, r, g, b, alpha);
        for (int i = 0; i < PUFFS; i++) {
            double angle = Math.PI * 2.0 * i / PUFFS + time * 0.035;
            double radius = (i % 2 == 0) ? 0.88 : 1.02;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius * 0.72;
            double y = centerY + 0.08 * Math.sin(time * 0.8 + i * 1.4);
            double puffSize = (i % 3 == 0) ? 0.72 : 0.58;
            puff(out, entry, camera, x, y, z, puffSize, 0.42, puffSize * 0.82,
                    r, g, b, alpha);
        }
    }

    private static void puff(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
                             double x, double y, double z, double rx, double ry, double rz,
                             float r, float g, float b, float alpha) {
        // Three perpendicular rings and two latitude rings create a soft, faceted puff.
        ring(out, entry, camera, x, y, z, rx, ry, 0.0, 0.0, r, g, b, alpha);
        ring(out, entry, camera, x, y, z, rx, 0.0, rz, 0.0, r, g, b, alpha * 0.88f);
        ring(out, entry, camera, x, y, z, 0.0, ry, rz, 0.0, r, g, b, alpha * 0.78f);
        ring(out, entry, camera, x, y - ry * 0.45, z, rx * 0.82, 0.0, rz * 0.82,
                0.0, r, g, b, alpha * 0.65f);
        ring(out, entry, camera, x, y + ry * 0.45, z, rx * 0.76, 0.0, rz * 0.76,
                0.0, r, g, b, alpha * 0.65f);
    }

    private static void ring(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
                             double x, double y, double z, double rx, double ry, double rz,
                             double unused, float r, float g, float b, float alpha) {
        double px = x + rx, py = y + ry, pz = z;
        for (int i = 1; i <= SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / SEGMENTS;
            double nx = x + Math.cos(angle) * rx;
            double ny = y + Math.sin(angle) * ry;
            double nz = z + Math.sin(angle) * rz;
            line(out, entry, px - camera.x, py - camera.y, pz - camera.z,
                    nx - camera.x, ny - camera.y, nz - camera.z, r, g, b, alpha);
            px = nx; py = ny; pz = nz;
        }
    }

    private static void line(VertexConsumer out, MatrixStack.Entry entry,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             float r, float g, float b, float alpha) {
        float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6f) return;
        dx /= length; dy /= length; dz /= length;
        out.vertex(entry, (float) x1, (float) y1, (float) z1)
                .color(r, g, b, alpha).normal(entry, dx, dy, dz);
        out.vertex(entry, (float) x2, (float) y2, (float) z2)
                .color(r, g, b, alpha).normal(entry, dx, dy, dz);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
