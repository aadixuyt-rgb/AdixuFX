package pl.adixfx;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Lekka, klientowa śnieżyca z niezmienną paletą niebieskich barw. */
public final class Snowfall {
    private static final List<Flake> FLAKES = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static long lastWorldTime = Long.MIN_VALUE;

    private Snowfall() {}

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null || !Config.d.snowEnabled) {
            FLAKES.clear();
            lastWorldTime = Long.MIN_VALUE;
            return;
        }
        long worldTime = mc.world.getTime();
        if (worldTime != lastWorldTime) {
            lastWorldTime = worldTime;
        }

        int intensity = Math.max(1, Math.min(3, Config.d.snowIntensity));
        int target = intensity == 1 ? 120 : intensity == 2 ? 360 : 1200;
        long now = System.nanoTime();

        Iterator<Flake> iterator = FLAKES.iterator();
        while (iterator.hasNext()) {
            Flake f = iterator.next();
            double fromPlayerX = f.x - mc.player.getX();
            double fromPlayerZ = f.z - mc.player.getZ();
            double fromPlayerY = f.y - mc.player.getY();
            if (now - f.bornAt > 9_000_000_000L
                    || fromPlayerX * fromPlayerX + fromPlayerZ * fromPlayerZ > 20.0 * 20.0
                    || Math.abs(fromPlayerY) > 30.0) {
                iterator.remove();
                continue;
            }
            f.x += f.vx;
            f.y += f.vy;
            f.z += f.vz;
            f.vx += (RANDOM.nextDouble() - 0.5) * 0.0012;
            f.vz += (RANDOM.nextDouble() - 0.5) * 0.0012;
            f.vx = Math.max(-0.045, Math.min(0.045, f.vx));
            f.vz = Math.max(-0.045, Math.min(0.045, f.vz));
        }

        // Przy szybkim locie elytrą generuj śnieg głównie przed graczem,
        // aby opady nie zostawały z tyłu kamery.
        Vec3d velocity = mc.player.getVelocity();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double dirX = horizontalSpeed > 0.12 ? velocity.x / horizontalSpeed : 0.0;
        double dirZ = horizontalSpeed > 0.12 ? velocity.z / horizontalSpeed : 0.0;
        while (FLAKES.size() < target) {
            // Pełna, ruchoma strefa opadów: 20 bloków w poziomie
            // i 30 bloków powyżej oraz poniżej gracza.
            double x = mc.player.getX() + (RANDOM.nextDouble() * 40.0 - 20.0);
            double z = mc.player.getZ() + (RANDOM.nextDouble() * 40.0 - 20.0);
            double y = mc.player.getY() + (RANDOM.nextDouble() * 60.0 - 30.0);
            FLAKES.add(new Flake(x, y, z,
                    (RANDOM.nextDouble() - 0.5) * 0.035,
                    -0.055 - RANDOM.nextDouble() * 0.045,
                    (RANDOM.nextDouble() - 0.5) * 0.035,
                    now, RANDOM.nextLong()));
        }
    }

    public static void render(WorldRenderContext ctx) {
        if (!Config.d.snowEnabled || FLAKES.isEmpty()) return;
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider providers = ctx.consumers();
        if (matrices == null || providers == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        Vec3d camera = ctx.camera().getPos();
        VertexConsumer lines = providers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        long now = System.nanoTime();

        // Zawsze jasnoniebieski śnieg.
        int color = 0xFF8AD8FF;
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;
        int intensity = Math.max(1, Math.min(3, Config.d.snowIntensity));

        for (Flake f : FLAKES) {
            double age = (now - f.bornAt) / 1_000_000_000.0;
            float fade = (float)Math.min(1.0, Math.min(age / 0.35, (9.0 - age) / 0.9));
            boolean pulses = intensity == 1 || (intensity == 2
                    ? (f.seed & 1L) == 0L : (f.seed & 3L) == 0L);
            float pulse = pulses ? 0.68f + 0.32f * (float)Math.sin(now / 310_000_000.0 + (f.seed & 1023)) : 0.78f;
            float alpha = Math.max(0f, fade) * pulse;
            double size = 0.045 + (pulses ? pulse * 0.018 : 0.006);
            double x = f.x - camera.x, y = f.y - camera.y, z = f.z - camera.z;
            // Mały, sześciokątny płatek z delikatnym krzyżykiem.
            line(lines, entry, x-size,y,z, x-size*0.35,y,z, r,g,b,alpha);
            line(lines, entry, x-size*0.35,y,z, x+size*0.35,y,z, r,g,b,alpha);
            line(lines, entry, x+size*0.35,y,z, x+size,y,z, r,g,b,alpha);
            line(lines, entry, x,y-size,z, x,y+size,z, r,g,b,alpha);
            line(lines, entry, x-size*0.55,y-size*0.55,z, x+size*0.55,y+size*0.55,z, r,g,b,alpha*0.75f);
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

    private static final class Flake {
        double x,y,z,vx,vy,vz;
        final long bornAt, seed;
        Flake(double x,double y,double z,double vx,double vy,double vz,long bornAt,long seed) {
            this.x=x;this.y=y;this.z=z;this.vx=vx;this.vy=vy;this.vz=vz;
            this.bornAt=bornAt;this.seed=seed;
        }
    }
}
