package pl.adixfx;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Efekt błyskawicy uruchamiany po śmierci innego gracza. */
public final class KillLightning {
    private static final List<Flash> ACTIVE = new ArrayList<>();
    private KillLightning() {}

    public static void onEntityStatus(EntityStatusS2CPacket packet) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.d.killLightning || mc.world == null || mc.player == null || packet.getStatus() != 3) return;
        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity) || entity == mc.player) return;
        synchronized (ACTIVE) {
            ACTIVE.add(new Flash(entity.getX(), entity.getY(), entity.getZ(),
                    System.currentTimeMillis(), new Random().nextLong()));
        }
    }

    public static List<Flash> active() {
        long now = System.currentTimeMillis();
        synchronized (ACTIVE) {
            ACTIVE.removeIf(f -> now - f.startedAt > 2000L);
            return List.copyOf(ACTIVE);
        }
    }

    public record Flash(double x, double y, double z, long startedAt, long seed) {}
}
