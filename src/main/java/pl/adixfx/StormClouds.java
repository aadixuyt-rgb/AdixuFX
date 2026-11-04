package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/**
 * Stacjonarna, warstwowa pokrywa ciężkich chmur burzowych.
 * Barwa pozostaje grafitowa; kolor GUI jest tylko delikatnym odbitym światłem,
 * którego intensywność rośnie wraz z głośnością muzyki.
 */
public final class StormClouds {
    private static final int LATITUDE_STEPS = 8;
    private static final int LONGITUDE_STEPS = 22;

    private StormClouds() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.d.stormClouds || mc.world == null || mc.player == null
                || (!mc.world.isRaining() && !mc.world.isThundering())) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider providers = context.consumers();
        if (matrices == null || providers == null) return;

        Vec3d camera = context.camera().getPos();
        Vec3d player = mc.player.getLerpedPos(context.tickCounter().getTickDelta(false));

        // Chmury są blisko nad graczem, zamiast kilkadziesiąt bloków nad nim lub poza kadrem.
        double centerX = player.x;
        double centerZ = player.z;
        double baseY = player.y + 19.0;

        float loudness = Config.d.music && Music.isPlayingNow()
                ? clamp(BeatSync.level(Music.elapsedSeconds()), 0.0f, 1.0f) : 0.0f;
        int accent = GuiUtil.accent();
        float ar = ((accent >> 16) & 255) / 255.0f;
        float ag = ((accent >> 8) & 255) / 255.0f;
        float ab = (accent & 255) / 255.0f;
        float tint = 0.035f + loudness * 0.22f;

        VertexConsumer quads = providers.getBuffer(RenderLayer.getDebugQuads());
        MatrixStack.Entry entry = matrices.peek();

        // Szeroka, ciężka podstawa. Ciemne odcienie utrzymują wygląd grafitowej burzy.
        puff(quads, entry, camera, centerX, baseY, centerZ,
                34.0, 6.5, 25.0,
                mix(0.20f, ar, tint), mix(0.23f, ag, tint), mix(0.28f, ab, tint), 1.0f);

        // Nierówne, zachodzące na siebie wieże i kopuły nadają chmurom warstwowy profil.
        for (int i = 0; i < 11; i++) {
            double angle = Math.PI * 2.0 * i / 11.0;
            double ring = 9.0 + (i % 4) * 3.2;
            double x = centerX + Math.cos(angle) * ring;
            double z = centerZ + Math.sin(angle) * ring * 0.72;
            double y = baseY + 2.8 + (i % 3) * 1.45;
            double rx = 8.5 + (i % 3) * 1.4;
            double rz = 6.8 + (i % 2) * 1.8;
            float shade = 0.28f + (i % 3) * 0.018f;
            puff(quads, entry, camera, x, y, z, rx, 7.0 + (i % 2) * 1.8, rz,
                    mix(shade, ar, tint), mix(shade + 0.025f, ag, tint),
                    mix(shade + 0.07f, ab, tint), 1.0f);
        }

        // Niższe, spłaszczone pasma tworzą ciemny spód chmury.
        for (int i = 0; i < 5; i++) {
            double angle = Math.PI * 2.0 * i / 5.0 + 0.35;
            double x = centerX + Math.cos(angle) * (18.0 + (i % 2) * 3.0);
            double z = centerZ + Math.sin(angle) * (14.0 + (i % 2) * 2.0);
            puff(quads, entry, camera, x, baseY - 2.2, z,
                    11.0, 4.0, 8.0, 0.16f, 0.19f, 0.24f, 1.0f);
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

    private static float mix(float base, float accent, float amount) {
        return base + (accent - base) * amount;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
