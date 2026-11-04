package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;

/** Delikatna poświata w kolorze akcentu GUI wokół przedmiotów na ziemi i w dłoniach innych graczy. */
public final class ItemGlow {
    private static final int LATITUDE_STEPS = 6;
    private static final int LONGITUDE_STEPS = 18;
    private static final int MAX_ITEMS = 36;

    private ItemGlow() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.d.itemGlow || mc.world == null || mc.player == null) return;
        if (!Config.d.droppedItemGlow && !Config.d.heldItemGlow) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider providers = context.consumers();
        if (matrices == null || providers == null) return;

        Vec3d camera = context.camera().getPos();
        float tickDelta = context.tickCounter().getTickDelta(false);
        boolean musicPlaying = Config.d.music && Music.isPlayingNow();
        float level = musicPlaying ? BeatSync.level(Music.elapsedSeconds()) : 0.0f;
        float beat = musicPlaying ? BeatSync.pulse(Music.elapsedSeconds()) : 0.0f;
        float audio = clamp(level * 0.60f + beat * 0.75f, 0.0f, 1.0f);

        int color = GuiUtil.accent();
        float red = ((color >> 16) & 255) / 255.0f;
        float green = ((color >> 8) & 255) / 255.0f;
        float blue = (color & 255) / 255.0f;
        double maxDistanceSquared = (double) Config.d.range * Config.d.range;

        VertexConsumer quads = providers.getBuffer(RenderLayer.getDebugQuads());
        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();

        if (Config.d.droppedItemGlow) {
            int rendered = 0;
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof ItemEntity item) || item.isRemoved() || item.getStack().isEmpty()) continue;
                if (item.squaredDistanceTo(mc.player) > maxDistanceSquared) continue;
                if (rendered >= MAX_ITEMS) break;
                rendered++;

                Vec3d pos = item.getLerpedPos(tickDelta);
                drawGlow(quads, lines, entry, camera, pos.x, pos.y + 0.16, pos.z,
                        0.29f, level, beat, audio, red, green, blue);
            }
        }

        // Poświata przy obu dłoniach innych graczy. Nie renderujemy jej na własnych rękach.
        if (Config.d.heldItemGlow) {
            for (PlayerEntity other : mc.world.getPlayers()) {
                if (other == mc.player || other.isRemoved()
                        || other.squaredDistanceTo(mc.player) > maxDistanceSquared) continue;

                Vec3d pos = other.getLerpedPos(tickDelta);
                Vec3d forward = other.getRotationVec(tickDelta).multiply(1.0, 0.0, 1.0).normalize();
                if (forward.lengthSquared() < 1.0E-5) forward = new Vec3d(0.0, 0.0, 1.0);
                Vec3d right = new Vec3d(forward.z, 0.0, -forward.x);
                double mainSide = other.getMainArm() == Arm.RIGHT ? 1.0 : -1.0;

                if (!other.getMainHandStack().isEmpty()) {
                    Vec3d hand = pos.add(forward.multiply(0.30)).add(right.multiply(0.27 * mainSide));
                    drawGlow(quads, lines, entry, camera, hand.x, hand.y + 1.12, hand.z,
                            0.23f, level, beat, audio, red, green, blue);
                }
                if (!other.getOffHandStack().isEmpty()) {
                    Vec3d hand = pos.add(forward.multiply(0.30)).add(right.multiply(-0.27 * mainSide));
                    drawGlow(quads, lines, entry, camera, hand.x, hand.y + 1.12, hand.z,
                            0.23f, level, beat, audio, red, green, blue);
                }
            }
        }

        if (providers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getDebugQuads());
            immediate.draw(RenderLayer.getLines());
        }
    }

    private static void drawGlow(VertexConsumer quads, VertexConsumer lines,
                                 MatrixStack.Entry entry, Vec3d camera,
                                 double x, double y, double z, float baseRadius,
                                 float level, float beat, float audio,
                                 float red, float green, float blue) {
        // Niewielka, półprzezroczysta poświata: ma podkreślać item, a nie go zasłaniać.
        float radius = baseRadius * (0.92f + 0.10f * level + 0.16f * beat);
        float height = baseRadius * (0.44f + 0.05f * audio);
        float outerAlpha = 0.035f + 0.055f * level + 0.075f * beat;
        float innerAlpha = 0.045f + 0.055f * audio;

        ellipsoid(quads, entry, camera, x, y, z,
                radius * 1.45, height * 1.25, radius * 1.45,
                red, green, blue, clamp(outerAlpha, 0.025f, 0.17f));
        ellipsoid(quads, entry, camera, x, y, z,
                radius, height, radius,
                red, green, blue, clamp(innerAlpha, 0.035f, 0.14f));
        ring(lines, entry, camera, x, y - height * 0.42, z,
                radius * (1.12 + audio * 0.12), red, green, blue,
                clamp(0.20f + audio * 0.30f, 0.0f, 0.52f));
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
