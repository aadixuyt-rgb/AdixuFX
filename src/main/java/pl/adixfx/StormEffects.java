package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Subtelne, nieregularne błyskawice w kolorze akcentu AdixFX podczas burzy. */
public final class StormEffects {
    private static final long CYCLE_MS = 7600L;
    private static final long FLASH_MS = 210L;

    private StormEffects() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || !client.world.isThundering()) return;

        long now = System.currentTimeMillis();
        long cycle = Math.floorMod(now, CYCLE_MS);
        // Większość czasu niebo pozostaje ciemne; krótki, nieregularny błysk.
        if (cycle > FLASH_MS) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider providers = context.consumers();
        if (matrices == null || providers == null) return;

        long seed = Math.floorDiv(now, CYCLE_MS);
        Random random = new Random(seed * 0x9E3779B97F4A7C15L);
        Vec3d camera = context.camera().getPos();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 26.0 + random.nextDouble() * 20.0;
        double x = camera.x + Math.cos(angle) * distance;
        double z = camera.z + Math.sin(angle) * distance;
        double top = Math.max(camera.y + 24.0, client.world.getTopY() - 3.0);
        double bottom = Math.max(camera.y + 2.0, top - 30.0 - random.nextDouble() * 18.0);

        int color = GuiUtil.accent();
        float r = ((color >> 16) & 255) / 255.0f;
        float g = ((color >> 8) & 255) / 255.0f;
        float b = (color & 255) / 255.0f;

        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        double px = x, py = top, pz = z;
        int segments = 9;
        for (int i = 1; i <= segments; i++) {
            double nextY = top + (bottom - top) * i / segments;
            double nextX = x + (random.nextDouble() - 0.5) * 5.0;
            double nextZ = z + (random.nextDouble() - 0.5) * 5.0;
            drawLine(lines, entry, px - camera.x, py - camera.y, pz - camera.z,
                    nextX - camera.x, nextY - camera.y, nextZ - camera.z, r, g, b, 0.98f);

            if (i >= 3 && i <= 7 && random.nextFloat() < 0.28f) {
                double branchX = nextX, branchY = nextY, branchZ = nextZ;
                double endX = branchX + (random.nextDouble() - 0.5) * 11.0;
                double endY = branchY - 4.0 - random.nextDouble() * 9.0;
                double endZ = branchZ + (random.nextDouble() - 0.5) * 11.0;
                drawLine(lines, entry, branchX - camera.x, branchY - camera.y, branchZ - camera.z,
                        endX - camera.x, endY - camera.y, endZ - camera.z, r, g, b, 0.78f);
            }
            px = nextX; py = nextY; pz = nextZ;
        }
        if (providers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getLines());
        }
    }

    private static void drawLine(VertexConsumer consumer, MatrixStack.Entry entry,
                                 double x1, double y1, double z1, double x2, double y2, double z2,
                                 float r, float g, float b, float alpha) {
        float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-5f) return;
        dx /= length; dy /= length; dz /= length;
        consumer.vertex(entry, (float) x1, (float) y1, (float) z1)
                .color(r, g, b, alpha).normal(entry, dx, dy, dz);
        consumer.vertex(entry, (float) x2, (float) y2, (float) z2)
                .color(r, g, b, alpha).normal(entry, dx, dy, dz);
    }
}
