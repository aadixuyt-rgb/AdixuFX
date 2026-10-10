package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Hitboxes {
    private static final int SEGMENTS = 8;   // na ile kawałków dzielimy każdą krawędź w trybie "wiele kolorów"

    private Hitboxes() {}

    private static boolean wanted(Entity e) {
        Config.Data d = Config.d;
        if (e instanceof PlayerEntity) return d.players;
        if (e instanceof Monster) return d.hostile;
        if (e instanceof PassiveEntity) return d.passive;
        if (e instanceof ItemEntity) return d.items;
        return d.others;
    }

    public static void render(WorldRenderContext ctx) {
        if (!Config.d.hitboxes) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        MatrixStack m = ctx.matrixStack();
        VertexConsumerProvider vcp = ctx.consumers();
        if (m == null || vcp == null) return;

        Vec3d cam = ctx.camera().getPos();
        float td = ctx.tickCounter().getTickDelta(false);
        VertexConsumer lc = vcp.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry en = m.peek();
        Config.Mode mode = Config.mode();
        double time = (System.currentTimeMillis() % 3_600_000L) / 1000.0 * Config.d.speed;
        double max = (double) Config.d.range * Config.d.range;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || e.isRemoved() || !wanted(e)) continue;
            if (e.squaredDistanceTo(mc.player) > max) continue;
            Vec3d diff = e.getLerpedPos(td).subtract(e.getPos());
            Box b = e.getBoundingBox().offset(diff).offset(-cam.x, -cam.y, -cam.z);
            switch (mode) {
                case STATIC -> box(en, lc, b, rgb(Config.d.fixedColor), null, 0);
                case CYCLE -> box(en, lc, b, rgb(MathHelper.hsvToRgb((float) ((time * 0.15) % 1.0), 1f, 1f)), null, 0);
                case MULTI -> box(en, lc, b, null, null, time);
            }
        }
        if (vcp instanceof VertexConsumerProvider.Immediate imm) imm.draw(RenderLayer.getLines());
    }

    private static float[] rgb(int c) {
        return new float[]{((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f};
    }

    /** Kolor dla punktu o parametrze t (0..1) wzdłuż obwodu - przesuwa się w czasie. */
    private static float[] multi(double t, double time) {
        float hue = (float) (((t + time * 0.2) % 1.0 + 1.0) % 1.0);
        return rgb(MathHelper.hsvToRgb(hue, 1f, 1f));
    }

    private static void box(MatrixStack.Entry en, VertexConsumer c, Box b, float[] solid, float[] unused, double time) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
        float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        float[][] edges = {
                {x0, y0, z0, x1, y0, z0}, {x1, y0, z0, x1, y0, z1}, {x1, y0, z1, x0, y0, z1}, {x0, y0, z1, x0, y0, z0},
                {x0, y1, z0, x1, y1, z0}, {x1, y1, z0, x1, y1, z1}, {x1, y1, z1, x0, y1, z1}, {x0, y1, z1, x0, y1, z0},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x1, y0, z1, x1, y1, z1}, {x0, y0, z1, x0, y1, z1}
        };
        for (int i = 0; i < edges.length; i++) {
            float[] ed = edges[i];
            if (solid != null) {
                line(en, c, ed[0], ed[1], ed[2], ed[3], ed[4], ed[5], solid);
            } else {
                for (int s = 0; s < SEGMENTS; s++) {
                    float a = s / (float) SEGMENTS, bb = (s + 1) / (float) SEGMENTS;
                    double t = (i * SEGMENTS + s + 0.5) / (edges.length * SEGMENTS);
                    line(en, c,
                            ed[0] + (ed[3] - ed[0]) * a, ed[1] + (ed[4] - ed[1]) * a, ed[2] + (ed[5] - ed[2]) * a,
                            ed[0] + (ed[3] - ed[0]) * bb, ed[1] + (ed[4] - ed[1]) * bb, ed[2] + (ed[5] - ed[2]) * bb,
                            multi(t, time));
                }
            }
        }
    }

    private static void line(MatrixStack.Entry en, VertexConsumer c, float x1, float y1, float z1,
                             float x2, float y2, float z2, float[] col) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6f) return;
        dx /= len; dy /= len; dz /= len;
        c.vertex(en, x1, y1, z1).color(col[0], col[1], col[2], 1f).normal(en, dx, dy, dz);
        c.vertex(en, x2, y2, z2).color(col[0], col[1], col[2], 1f).normal(en, dx, dy, dz);
    }
}
