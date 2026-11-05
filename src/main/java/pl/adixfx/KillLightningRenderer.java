package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Wieloetapowa, pulsująca błyskawica po zabójstwie gracza. */
public final class KillLightningRenderer {
    private KillLightningRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || !Config.d.killLightning) return;
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider providers = context.consumers();
        if (matrices == null || providers == null) return;
        var flashes = KillLightning.active();
        if (flashes.isEmpty()) return;

        Vec3d camera = context.camera().getPos();
        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        int color = GuiUtil.accent();
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;
        long now = System.currentTimeMillis();

        for (KillLightning.Flash flash : flashes) {
            long ageMs = now - flash.startedAt();
            float fadeIn = Math.min(1f, ageMs / 110f);
            float fadeOut = Math.max(0f, Math.min(1f, (1600f - ageMs) / 430f));
            float flicker = 0.72f + 0.28f * (float)Math.sin(ageMs * 0.055 + (flash.seed() & 255));
            float alpha = fadeIn * fadeOut * flicker;
            Random random = new Random(flash.seed());
            double x = flash.x(), z = flash.z(), bottom = flash.y() + 0.15;
            double top = bottom + 11.0;
            double px = x, pz = z, py = top;

            for (int i = 1; i <= 13; i++) {
                double nx = x + (random.nextDouble() - 0.5) * 3.2;
                double nz = z + (random.nextDouble() - 0.5) * 3.2;
                double ny = top + (bottom - top) * i / 13.0;
                // Główny, kolorowy kanał i subtelny jasny rdzeń.
                line(lines, entry, camera, px,py,pz, nx,ny,nz, r,g,b,alpha);
                line(lines, entry, camera, px+0.035,py,pz+0.035, nx+0.035,ny,nz+0.035,
                        Math.min(1f,r*0.55f+0.35f), Math.min(1f,g*0.55f+0.35f),
                        Math.min(1f,b*0.55f+0.35f), alpha*0.72f);

                if (i >= 2 && i <= 11 && random.nextFloat() < 0.48f) {
                    double ex = nx + (random.nextDouble() - 0.5) * 5.5;
                    double ey = ny - 1.0 - random.nextDouble() * 3.5;
                    double ez = nz + (random.nextDouble() - 0.5) * 5.5;
                    line(lines, entry, camera, nx,ny,nz, ex,ey,ez, r,g,b,alpha*0.82f);
                    if (random.nextFloat() < 0.35f) {
                        double bx = ex + (random.nextDouble() - 0.5) * 2.2;
                        double bz = ez + (random.nextDouble() - 0.5) * 2.2;
                        line(lines, entry, camera, ex,ey,ez, bx,ey-1.2,bz, r,g,b,alpha*0.58f);
                    }
                }
                px=nx; py=ny; pz=nz;
            }
        }
    }

    private static void line(VertexConsumer c, MatrixStack.Entry e, Vec3d camera,
                             double x1,double y1,double z1,double x2,double y2,double z2,
                             float r,float g,float b,float a) {
        float dx=(float)(x2-x1), dy=(float)(y2-y1), dz=(float)(z2-z1);
        float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(length<1.0E-6f)return;
        dx/=length;dy/=length;dz/=length;
        c.vertex(e,(float)(x1-camera.x),(float)(y1-camera.y),(float)(z1-camera.z))
                .color(r,g,b,a).normal(e,dx,dy,dz);
        c.vertex(e,(float)(x2-camera.x),(float)(y2-camera.y),(float)(z2-camera.z))
                .color(r,g,b,a).normal(e,dx,dy,dz);
    }
}
