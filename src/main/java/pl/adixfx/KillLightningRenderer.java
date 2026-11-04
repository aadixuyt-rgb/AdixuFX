package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Niezależny renderer błyskawic przy śmierci gracza. */
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
            float fade = Math.max(0f, 1f - (now - flash.startedAt()) / 2000f);
            Random random = new Random(flash.seed());
            double x = flash.x(), z = flash.z(), bottom = flash.y() + 0.15;
            double top = bottom + 10.0;
            double px = x, pz = z, py = top;
            for (int i = 1; i <= 11; i++) {
                double nx = x + (random.nextDouble() - 0.5) * 4.0;
                double nz = z + (random.nextDouble() - 0.5) * 4.0;
                double ny = top + (bottom - top) * i / 11.0;
                line(lines, entry, px-camera.x, py-camera.y, pz-camera.z,
                        nx-camera.x, ny-camera.y, nz-camera.z, r,g,b,fade);
                if (i >= 2 && i <= 9 && random.nextFloat() < 0.42f) {
                    double ex = nx + (random.nextDouble() - 0.5) * 7.0;
                    double ey = ny - 1.0 - random.nextDouble() * 3.0;
                    double ez = nz + (random.nextDouble() - 0.5) * 7.0;
                    line(lines, entry, nx-camera.x, ny-camera.y, nz-camera.z,
                            ex-camera.x, ey-camera.y, ez-camera.z, r,g,b,fade*0.8f);
                }
                px=nx; py=ny; pz=nz;
            }
        }
        if (providers instanceof VertexConsumerProvider.Immediate immediate) immediate.draw(RenderLayer.getLines());
    }

    private static void line(VertexConsumer c, MatrixStack.Entry e,
                             double x1,double y1,double z1,double x2,double y2,double z2,
                             float r,float g,float b,float a) {
        float dx=(float)(x2-x1), dy=(float)(y2-y1), dz=(float)(z2-z1);
        float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(length<1.0E-6f)return;
        dx/=length;dy/=length;dz/=length;
        c.vertex(e,(float)x1,(float)y1,(float)z1).color(r,g,b,a).normal(e,dx,dy,dz);
        c.vertex(e,(float)x2,(float)y2,(float)z2).color(r,g,b,a).normal(e,dx,dy,dz);
    }
}
