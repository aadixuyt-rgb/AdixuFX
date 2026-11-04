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
    private static final int LAT = 7;
    private static final int LON = 18;
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
        double cx = player.x, cz = player.z, cy = player.y + 23.0;
        VertexConsumer out = providers.getBuffer(RenderLayer.getDebugQuads());
        MatrixStack.Entry entry = matrices.peek();

        float flash = StormEffects.flashIntensity();
        float base = 0.105f + flash * 0.30f;
        Random random = new Random(0x51A7C10DL);

        // Szeroki, nieregularny spód chmury — kilka spłaszczonych warstw zamiast jednej kuli.
        puff(out, entry, camera, cx, cy, cz, 30, 4.4, 24,
                base * 0.76f, base * 0.84f, base, 1f);
        for (int i = 0; i < 24; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 5.0 + random.nextDouble() * 23.0;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius * 0.78;
            double y = cy + random.nextDouble() * 3.0 - 1.8;
            double rx = 5.5 + random.nextDouble() * 5.5;
            double rz = 4.0 + random.nextDouble() * 4.0;
            float shade = 0.105f + random.nextFloat() * 0.045f + flash * 0.28f;
            puff(out, entry, camera, x, y, z, rx, 3.2 + random.nextDouble() * 2.4, rz,
                    shade * 0.78f, shade * 0.88f, shade * 1.08f, 1f);
        }

        // Wyższe, poszarpane wieże konwekcyjne. Asymetryczne rozmiary unikają efektu równych kulek.
        for (int i = 0; i < 13; i++) {
            double angle = Math.PI * 2.0 * i / 13.0 + 0.18;
            double radius = 7.0 + (i % 4) * 4.8;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius * 0.72;
            double y = cy + 3.0 + (i % 4) * 1.7;
            double rx = 5.0 + (i % 3) * 1.8;
            double rz = 4.0 + (i % 2) * 1.9;
            float shade = 0.16f + (i % 3) * 0.018f + flash * 0.34f;
            puff(out, entry, camera, x, y, z, rx, 5.0 + (i % 3) * 1.5, rz,
                    shade * 0.78f, shade * 0.88f, shade * 1.12f, 1f);
        }

        // Długie, ciemniejsze pasma od spodu nadają chmurze ciężki, burzowy wygląd.
        for (int i = 0; i < 9; i++) {
            double angle = Math.PI * 2.0 * i / 9.0;
            double x = cx + Math.cos(angle) * (12.0 + (i % 3) * 5.0);
            double z = cz + Math.sin(angle) * (10.0 + (i % 3) * 4.0);
            puff(out, entry, camera, x, cy - 3.4, z, 8.0, 2.5, 5.5,
                    0.075f + flash * 0.23f, 0.09f + flash * 0.25f,
                    0.13f + flash * 0.30f, 1f);
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
        double wobble = 1.0 + 0.055 * Math.sin(lon * 5.0 + lat * 3.0)
                + 0.025 * Math.cos(lon * 9.0 - lat * 4.0);
        double cl = Math.cos(lat);
        float x = (float)(cx + Math.cos(lon) * cl * rx * wobble - camera.x);
        float y = (float)(cy + Math.sin(lat) * ry * wobble - camera.y);
        float z = (float)(cz + Math.sin(lon) * cl * rz * wobble - camera.z);
        out.vertex(entry, x, y, z).color(r, g, b, a);
    }
}
