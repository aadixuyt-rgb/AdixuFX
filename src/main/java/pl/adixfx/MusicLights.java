package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Małe światła muzyczne: losowo pojawiają się w promieniu 0-10 bloków i po chwili zmieniają miejsce. */
public final class MusicLights {
    private static final int LIGHT_COUNT = 17;
    private static final double MAX_DISTANCE = 10.0;
    private static final long[] expiresAt = new long[LIGHT_COUNT];
    private static final long[] startsAt = new long[LIGHT_COUNT];
    private static final double[] offsetsX = new double[LIGHT_COUNT];
    private static final double[] offsetsZ = new double[LIGHT_COUNT];
    private static final double[] heights = new double[LIGHT_COUNT];
    private static final long[] seeds = new long[LIGHT_COUNT];
    private static final Random random = new Random();
    private MusicLights() {}

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.d.music || !Config.d.musicLights || !Music.isPlayingNow()
                || mc.player == null || mc.world == null) return;
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider providers = ctx.consumers();
        if (matrices == null || providers == null) return;

        long now = System.nanoTime();
        float level = BeatSync.level(Music.elapsedSeconds());
        float beat = BeatSync.pulse(Music.elapsedSeconds());
        Vec3d camera = ctx.camera().getPos();
        Vec3d player = mc.player.getLerpedPos(ctx.tickCounter().getTickDelta(false));
        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        int color = GuiUtil.accent();
        float red = ((color >> 16) & 255) / 255f;
        float green = ((color >> 8) & 255) / 255f;
        float blue = (color & 255) / 255f;

        for (int i = 0; i < LIGHT_COUNT; i++) {
            if (now >= expiresAt[i]) {
                // Każdy punkt żyje 2-5 sekund, po czym znika i losuje nowe miejsce.
                startsAt[i] = now;
                expiresAt[i] = now + (2_000_000_000L + (long)(random.nextDouble() * 3_000_000_000L));
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = Math.sqrt(random.nextDouble()) * MAX_DISTANCE;
                offsetsX[i] = Math.cos(angle) * distance;
                offsetsZ[i] = Math.sin(angle) * distance;
                heights[i] = 0.45 + random.nextDouble() * 2.3;
                seeds[i] = random.nextLong();
            }
            double x = player.x + offsetsX[i];
            double z = player.z + offsetsZ[i];
            double y = player.y + heights[i];
            double cx = x - camera.x, cy = y - camera.y, cz = z - camera.z;

            double age = (now - startsAt[i]) / 1_000_000_000.0;
            double remaining = (expiresAt[i] - now) / 1_000_000_000.0;
            float fadeIn = (float)Math.min(1.0, age / 0.25);
            float fadeOut = (float)Math.min(1.0, remaining / 0.45);
            float pulse = Math.max(level, beat * 0.85f);
            float alpha = (0.40f + 0.55f * pulse) * fadeIn * fadeOut;
            float radius = Math.min(0.058f, 0.018f + 0.020f * level + 0.020f * beat);
            double shimmer = Math.sin(now / 190_000_000.0 + (seeds[i] & 1023)) * 0.008;
            radius += (float)shimmer;

            line(lines, entry, cx-radius,cy,cz, cx,cy+radius,cz, red,green,blue,alpha);
            line(lines, entry, cx,cy+radius,cz, cx+radius,cy,cz, red,green,blue,alpha);
            line(lines, entry, cx+radius,cy,cz, cx,cy-radius,cz, red,green,blue,alpha);
            line(lines, entry, cx,cy-radius,cz, cx-radius,cy,cz, red,green,blue,alpha);
            line(lines, entry, cx,cy-radius*1.35,cz, cx,cy+radius*1.35,cz,
                    red,green,blue,alpha*0.68f);
        }
    }

    private static void line(VertexConsumer c, MatrixStack.Entry e,
                             double x1,double y1,double z1,double x2,double y2,double z2,
                             float r,float g,float b,float a) {
        float dx=(float)(x2-x1), dy=(float)(y2-y1), dz=(float)(z2-z1);
        float len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(len<1.0E-6f)return;
        dx/=len;dy/=len;dz/=len;
        c.vertex(e,(float)x1,(float)y1,(float)z1).color(r,g,b,a).normal(e,dx,dy,dz);
        c.vertex(e,(float)x2,(float)y2,(float)z2).color(r,g,b,a).normal(e,dx,dy,dz);
    }
}
