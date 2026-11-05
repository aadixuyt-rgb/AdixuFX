package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Światła muzyczne zakotwiczone w świecie; pozycja nie zależy od ruchu ani obrotu gracza. */
public final class MusicLights {
    private static final int LIGHT_COUNT = 17;
    private static final double MAX_DISTANCE = 4.0;
    private static final long[] expiresAt = new long[LIGHT_COUNT];
    private static final long[] startsAt = new long[LIGHT_COUNT];
    private static final double[] worldX = new double[LIGHT_COUNT];
    private static final double[] worldY = new double[LIGHT_COUNT];
    private static final double[] worldZ = new double[LIGHT_COUNT];
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
        float level = Math.max(0f, Math.min(1f, BeatSync.level(Music.elapsedSeconds())));
        float beat = Math.max(0f, Math.min(1f, BeatSync.pulse(Music.elapsedSeconds())));
        Vec3d camera = ctx.camera().getPos();
        Vec3d player = mc.player.getLerpedPos(ctx.tickCounter().getTickDelta(false));
        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        int color = GuiUtil.accent();
        float red = ((color >> 16) & 255) / 255f;
        float green = ((color >> 8) & 255) / 255f;
        float blue = (color & 255) / 255f;

        for (int i = 0; i < LIGHT_COUNT; i++) {
            double dxPlayer = worldX[i] - player.x;
            double dyPlayer = worldY[i] - player.y;
            double dzPlayer = worldZ[i] - player.z;
            double playerDistance = Math.sqrt(dxPlayer * dxPlayer + dyPlayer * dyPlayer + dzPlayer * dzPlayer);

            // Po przekroczeniu 4 blokow swiatlo odradza sie przy graczu.
            if (now >= expiresAt[i] || (startsAt[i] != 0L && playerDistance >= MAX_DISTANCE)) {
                startsAt[i] = now;
                expiresAt[i] = now + (2_000_000_000L + (long)(random.nextDouble() * 3_000_000_000L));
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = Math.sqrt(random.nextDouble()) * MAX_DISTANCE;
                worldX[i] = player.x + Math.cos(angle) * distance;
                worldZ[i] = player.z + Math.sin(angle) * distance;
                worldY[i] = player.y + 0.45 + random.nextDouble() * 2.3;
                seeds[i] = random.nextLong();
            }

            double cx = worldX[i] - camera.x;
            double cy = worldY[i] - camera.y;
            double cz = worldZ[i] - camera.z;
            double age = (now - startsAt[i]) / 1_000_000_000.0;
            double remaining = (expiresAt[i] - now) / 1_000_000_000.0;
            float fadeIn = (float)Math.min(1.0, age / 0.25);
            float fadeOut = (float)Math.min(1.0, remaining / 0.45);
            // Zanikanie od 3 do 4 blokow, z gladkim przejsciem.
            float distanceFade = (float)Math.max(0.0, Math.min(1.0, MAX_DISTANCE - playerDistance));
            distanceFade = distanceFade * distanceFade * (3.0f - 2.0f * distanceFade);
            float pulse = Math.max(level, beat * 0.85f);
            float alpha = (0.38f + 0.48f * pulse) * fadeIn * fadeOut * distanceFade;
            // Stały, mały rozmiar z bardzo ograniczoną reakcją na rytm — bez pojedynczych gigantycznych świateł.
            float radius = 0.026f + 0.012f * pulse;

            line(lines, entry, cx-radius,cy,cz, cx,cy+radius,cz, red,green,blue,alpha);
            line(lines, entry, cx,cy+radius,cz, cx+radius,cy,cz, red,green,blue,alpha);
            line(lines, entry, cx+radius,cy,cz, cx,cy-radius,cz, red,green,blue,alpha);
            line(lines, entry, cx,cy-radius,cz, cx-radius,cy,cz, red,green,blue,alpha);
            line(lines, entry, cx,cy-radius*1.25,cz, cx,cy+radius*1.25,cz,
                    red,green,blue,alpha*0.62f);
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
