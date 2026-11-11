package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/**
 * Standalone animated green sky ribbons. This renderer does not alter the
 * sky color, weather, clouds, items, players, HUD, or any other AdixuFX effect.
 */
public final class SkyLines {
    private static final float RED = 0.035f;
    private static final float GREEN = 1.0f;
    private static final float BLUE = 0.19f;

    private SkyLines() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider providers = context.consumers();
        if (matrices == null || providers == null) return;

        Vec3d camera = context.camera().getPos();
        Vec3d player = client.player.getLerpedPos(context.tickCounter().getTickDelta(false));
        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();

        double time = (System.nanoTime() - START_TIME) / 1_000_000_000.0;
        double rotation = time * 0.105;
        int bands = 7;
        int segments = 192;
        double baseRadius = 38.0;

        // Several offset, slowly rotating ribbons create a layered aurora-like
        // pattern. Each band has a different angular speed and wave phase.
        for (int band = 0; band < bands; band++) {
            double radius = baseRadius + band * 1.55;
            double bandRotation = rotation * (band % 2 == 0 ? 1.0 : -0.72)
                    + band * 0.34;
            double height = player.y + 26.0 + band * 1.15;

            for (int segment = 0; segment < segments; segment++) {
                double a0 = Math.PI * 2.0 * segment / segments;
                double a1 = Math.PI * 2.0 * (segment + 1) / segments;

                double p0 = a0 + bandRotation;
                double p1 = a1 + bandRotation;

                double wave0 = Math.sin(p0 * 3.0 + time * 0.34 + band * 0.8) * 3.8
                        + Math.sin(p0 * 7.0 - time * 0.19 + band) * 1.15;
                double wave1 = Math.sin(p1 * 3.0 + time * 0.34 + band * 0.8) * 3.8
                        + Math.sin(p1 * 7.0 - time * 0.19 + band) * 1.15;

                double radius0 = radius + Math.sin(p0 * 2.0 + time * 0.22) * 1.1;
                double radius1 = radius + Math.sin(p1 * 2.0 + time * 0.22) * 1.1;

                double x0 = player.x + Math.cos(p0) * radius0;
                double z0 = player.z + Math.sin(p0) * radius0;
                double x1 = player.x + Math.cos(p1) * radius1;
                double z1 = player.z + Math.sin(p1) * radius1;

                double y0 = height + wave0;
                double y1 = height + wave1;

                float pulse = (float) (0.78 + 0.22
                        * Math.sin(time * 0.72 + band * 0.67));
                float alpha = (0.19f + band * 0.012f) * pulse;

                // One clean strand per loop avoids three parallel lines.
                drawLine(lines, entry, camera, x0, y0, z0,
                        x1, y1, z1, alpha);
            }
        }

        // Short bridges connect neighboring closed loops into one flowing ribbon.
        for (int band = 0; band < bands - 1; band++) {
            double radius0 = baseRadius + band * 1.55;
            double radius1 = baseRadius + (band + 1) * 1.55;
            double rotation0 = rotation * (band % 2 == 0 ? 1.0 : -0.72) + band * 0.34;
            double rotation1 = rotation * ((band + 1) % 2 == 0 ? 1.0 : -0.72)
                    + (band + 1) * 0.34;
            for (int segment = 0; segment < segments; segment += 8) {
                double angle = Math.PI * 2.0 * segment / segments;
                double p0 = angle + rotation0;
                double p1 = angle + rotation1;
                double wave0 = Math.sin(p0 * 3.0 + time * 0.34 + band * 0.8) * 3.8
                        + Math.sin(p0 * 7.0 - time * 0.19 + band) * 1.15;
                double wave1 = Math.sin(p1 * 3.0 + time * 0.34 + (band + 1) * 0.8) * 3.8
                        + Math.sin(p1 * 7.0 - time * 0.19 + (band + 1)) * 1.15;
                double r0 = radius0 + Math.sin(p0 * 2.0 + time * 0.22) * 1.1;
                double r1 = radius1 + Math.sin(p1 * 2.0 + time * 0.22) * 1.1;
                double x0 = player.x + Math.cos(p0) * r0;
                double z0 = player.z + Math.sin(p0) * r0;
                double x1 = player.x + Math.cos(p1) * r1;
                double z1 = player.z + Math.sin(p1) * r1;
                double y0 = player.y + 26.0 + band * 1.15 + wave0;
                double y1 = player.y + 26.0 + (band + 1) * 1.15 + wave1;
                drawLine(lines, entry, camera, x0, y0, z0, x1, y1, z1, 0.22f);
            }
        }
    }

    private static final long START_TIME = System.nanoTime();

    private static void drawLine(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2, float alpha) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-5f) return;

        dx /= length;
        dy /= length;
        dz /= length;

        out.vertex(entry, (float) (x1 - camera.x), (float) (y1 - camera.y),
                        (float) (z1 - camera.z))
                .color(RED, GREEN, BLUE, alpha)
                .normal(entry, dx, dy, dz);
        out.vertex(entry, (float) (x2 - camera.x), (float) (y2 - camera.y),
                        (float) (z2 - camera.z))
                .color(RED, GREEN, BLUE, alpha)
                .normal(entry, dx, dy, dz);
    }
}
