package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Naturalnie nieregularne, kolorowe wyładowania widoczne podczas burzy. */
public final class StormEffects {
    private static final long CYCLE_MS = 3400L;

    private StormEffects() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || !Config.d.stormLightning
                || !client.world.isThundering()) return;

        long now = System.currentTimeMillis();
        long cycle = Math.floorMod(now, CYCLE_MS);
        // Dwa krótkie uderzenia w jednym cyklu: łatwiej je zauważyć, nadal są nieregularne.
        if (!(cycle < 340L || (cycle >= 520L && cycle < 670L))) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider providers = context.consumers();
        if (matrices == null || providers == null) return;

        long seed = Math.floorDiv(now, CYCLE_MS);
        Random random = new Random(seed * 0x9E3779B97F4A7C15L);
        Vec3d camera = context.camera().getPos();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 18.0 + random.nextDouble() * 16.0;
        double x = camera.x + Math.cos(angle) * distance;
        double z = camera.z + Math.sin(angle) * distance;
        double top = camera.y + 58.0;
        double bottom = Math.max(camera.y + 2.0, top - 44.0 - random.nextDouble() * 15.0);

        int color = GuiUtil.accent();
        float r = Math.min(1.0f, ((color >> 16) & 255) / 255.0f * 1.18f);
        float g = Math.min(1.0f, ((color >> 8) & 255) / 255.0f * 1.18f);
        float b = Math.min(1.0f, (color & 255) / 255.0f * 1.18f);

        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        double px = x, py = top, pz = z;
        int segments = 13;
        for (int i = 1; i <= segments; i++) {
            double nextY = top + (bottom - top) * i / segments;
            double nextX = x + (random.nextDouble() - 0.5) * 6.0;
            double nextZ = z + (random.nextDouble() - 0.5) * 6.0;

            // Lekko rozdzielone równoległe kreski tworzą czytelniejszy, jasny rdzeń błyskawicy.
            drawLine(lines, entry, camera, px, py, pz, nextX, nextY, nextZ, r, g, b, 1.0f);
            drawLine(lines, entry, camera, px + 0.055, py, pz + 0.055,
                    nextX + 0.055, nextY, nextZ + 0.055, r, g, b, 0.58f);

            if (i >= 3 && i <= 10 && random.nextFloat() < 0.36f) {
                double branchX = nextX, branchY = nextY, branchZ = nextZ;
                double endX = branchX + (random.nextDouble() - 0.5) * 13.0;
                double endY = branchY - 4.0 - random.nextDouble() * 11.0;
                double endZ = branchZ + (random.nextDouble() - 0.5) * 13.0;
                drawLine(lines, entry, camera, branchX, branchY, branchZ,
                        endX, endY, endZ, r, g, b, 0.88f);
            }
            px = nextX;
            py = nextY;
            pz = nextZ;
        }
        if (providers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getLines());
        }
    }

    private static void drawLine(VertexConsumer consumer, MatrixStack.Entry entry, Vec3d camera,
                                 double x1, double y1, double z1, double x2, double y2, double z2,
                                 float r, float g, float b, float alpha) {
        float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
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
}
