package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Małe, losowo rozmieszczone światła muzyczne w promieniu maksymalnie 15 bloków. */
public final class MusicLights {
    private static final int LIGHT_COUNT = 17;
    private MusicLights() {}

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.d.music || !Config.d.musicLights || !Music.isPlayingNow()
                || mc.player == null || mc.world == null) return;
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider providers = ctx.consumers();
        if (matrices == null || providers == null) return;

        float level = BeatSync.level(Music.elapsedSeconds());
        float beat = BeatSync.pulse(Music.elapsedSeconds());
        double time = System.nanoTime() / 1_000_000_000.0;
        Vec3d camera = ctx.camera().getPos();
        Vec3d player = mc.player.getLerpedPos(ctx.tickCounter().getTickDelta(false));
        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        int color = GuiUtil.accent();
        float red = ((color >> 16) & 255) / 255f;
        float green = ((color >> 8) & 255) / 255f;
        float blue = (color & 255) / 255f;

        for (int i = 0; i < LIGHT_COUNT; i++) {
            // Każde światło ma własny losowy punkt startowy w kole o promieniu 15 bloków.
            Random random = new Random(0xAD1F00DL + i * 7919L);
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 1.2 + Math.sqrt(random.nextDouble()) * 13.8;
            double drift = 0.35 + (i % 4) * 0.12;
            double phase = time * drift + i * 1.91;
            double x = player.x + Math.cos(angle) * distance + Math.cos(phase) * 0.55;
            double z = player.z + Math.sin(angle) * distance + Math.sin(phase) * 0.55;
            double y = player.y + 0.65 + random.nextDouble() * 2.4
                    + Math.sin(time * 0.55 + i * 1.7) * 0.22;

            double dx = x - player.x, dz = z - player.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal > 15.0) {
                x = player.x + dx * (15.0 / horizontal);
                z = player.z + dz * (15.0 / horizontal);
            }
            float radius = Math.min(0.052f, 0.014f + 0.018f * level + 0.020f * beat);
            float alpha = 0.42f + 0.54f * Math.max(level, beat * 0.85f);
            double cx = x-camera.x, cy = y-camera.y, cz = z-camera.z;
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
