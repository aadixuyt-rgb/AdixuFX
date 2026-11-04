package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Proceduralne, wielowarstwowe chmury burzowe z nieregularnymi krawędziami. */
public final class StormClouds {
    private static final int LAT = 9;
    private static final int LON = 24;
    private StormClouds() {}

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.d.stormClouds || mc.world == null || mc.player == null
                || (!mc.world.isRaining() && !mc.world.isThundering())) return;
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider providers = ctx.consumers();
        if (matrices == null || providers == null) return;

        Vec3d camera = ctx.camera().getPos();
        Vec3d player = mc.player.getLerpedPos(ctx.tickCounter().getTickDelta(false));
        double cx = player.x, cz = player.z, cy = player.y + 25.0;
        VertexConsumer out = providers.getBuffer(RenderLayer.getDebugQuads());
        MatrixStack.Entry entry = matrices.peek();

        float flash = StormEffects.flashIntensity();
        Random random = new Random(0x57A7C10DL);

        // Szeroka, szara pokrywa z zachodzących na siebie, spłaszczonych pasm.
        puff(out, entry, camera, cx, cy, cz, 43, 5.0, 35,
                0.205f, 0.218f, 0.235f, 1f);
        for (int band = 0; band < 3; band++) {
            int count = 22 + band * 5;
            double radiusBase = 8.0 + band * 8.0;
            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2.0 * i / count + band * 0.31
                        + (random.nextDouble() - 0.5) * 0.24;
                double radius = radiusBase + (random.nextDouble() - 0.5) * 9.0;
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius * 0.78;
                double y = cy + (random.nextDouble() - 0.5) * (2.0 + band);
                double rx = 7.0 + random.nextDouble() * 6.0;
                double rz = 5.5 + random.nextDouble() * 5.0;
                float shade = 0.175f + random.nextFloat() * 0.075f;
                puff(out, entry, camera, x, y, z, rx, 3.0 + random.nextDouble() * 2.4, rz,
                        shade * 0.91f, shade * 0.94f, shade, 1f);
            }
        }
        // Wyższe, łagodniejsze wypiętrzenia.
        for (int i = 0; i < 26; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 5.0 + random.nextDouble() * 29.0;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius * 0.78;
            double y = cy + 2.0 + random.nextDouble() * 5.0;
            double rx = 6.0 + random.nextDouble() * 7.0;
            double rz = 5.0 + random.nextDouble() * 5.0;
            float shade = 0.235f + random.nextFloat() * 0.055f;
            puff(out, entry, camera, x, y, z, rx, 3.8 + random.nextDouble() * 3.0, rz,
                    shade, shade * 1.015f, shade * 1.04f, 1f);
        }
        // Ciemniejsze fałdy od spodu.
        for (int i = 0; i < 18; i++) {
            double angle = Math.PI * 2.0 * i / 18.0;
            double radius = 9.0 + (i % 4) * 6.0;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius * 0.75;
            puff(out, entry, camera, x, cy - 3.0, z, 10.0, 2.4, 6.8,
                    0.105f, 0.113f, 0.125f, 1f);
        }
        // Delikatne rozjaśnienie chmur kolorem akcentu wybranym w GUI.
        if (flash > 0.01f) {
            int accent = GuiUtil.accent();
            float strength = Math.min(0.22f, flash * 0.18f);
            float rr = ((accent >> 16) & 255) / 255f;
            float gg = ((accent >> 8) & 255) / 255f;
            float bb = (accent & 255) / 255f;
            puff(out, entry, camera, cx, cy + 0.8, cz, 42, 4.0, 34,
                    0.205f + rr * strength, 0.218f + gg * strength,
                    0.235f + bb * strength, 1f);
        }
    }

    private static void puff(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
                             double cx, double cy, double cz, double rx, double ry, double rz,
                             float r, float g, float b, float a) {
        for (int lat = 0; lat < LAT; lat++) {
            double a0 = -Math.PI / 2 + Math.PI * lat / LAT;
            double a1 = -Math.PI / 2 + Math.PI * (lat + 1) / LAT;
            for (int lon = 0; lon < LON; lon++) {
                double b0 = Math.PI * 2 * lon / LON;
                double b1 = Math.PI * 2 * (lon + 1) / LON;
                vertex(out, entry, camera, cx, cy, cz, rx, ry, rz, a0, b0, r, g, b, a);
                vertex(out, entry, camera, cx, cy, cz, rx, ry, rz, a1, b0, r, g, b, a);
                vertex(out, entry, camera, cx, cy, cz, rx, ry, rz, a1, b1, r, g, b, a);
                vertex(out, entry, camera, cx, cy, cz, rx, ry, rz, a0, b1, r, g, b, a);
            }
        }
    }

    private static void vertex(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera,
                               double cx, double cy, double cz, double rx, double ry, double rz,
                               double lat, double lon, float r, float g, float b, float a) {
        double wobble = 1.0 + 0.075 * Math.sin(lon * 4.0 + lat * 3.0)
                + 0.035 * Math.cos(lon * 9.0 - lat * 5.0)
                + 0.018 * Math.sin(lon * 13.0 + lat * 8.0);
        double cl = Math.cos(lat);
        float x = (float)(cx + Math.cos(lon) * cl * rx * wobble - camera.x);
        float y = (float)(cy + Math.sin(lat) * ry * wobble - camera.y);
        float z = (float)(cz + Math.sin(lon) * cl * rz * wobble - camera.z);
        out.vertex(entry, x, y, z).color(r, g, b, a);
    }
}
