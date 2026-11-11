package pl.adixfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Clean-room client-side Backtrack implementation.
 * Keeps a short history of remote player positions and temporarily uses a
 * delayed bounding box while the local attack key is held.
 */
public final class Backtrack {
    private static final long DELAY_NANOS = 150_000_000L;
    private static final long HISTORY_NANOS = 1_000_000_000L;
    private static final Map<UUID, ArrayDeque<PositionSample>> HISTORY = new HashMap<>();

    private Backtrack() {}

    private record PositionSample(long time, Vec3d position) {}

    public static void tick(MinecraftClient client) {
        if (!Config.d.backtrack || client.world == null || client.player == null) {
            HISTORY.clear();
            return;
        }

        long now = System.nanoTime();
        Set<UUID> seen = new HashSet<>();
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || player.isRemoved()) continue;
            UUID id = player.getUuid();
            seen.add(id);
            ArrayDeque<PositionSample> samples = HISTORY.computeIfAbsent(id, key -> new ArrayDeque<>());
            Vec3d pos = player.getPos();

            PositionSample last = samples.peekLast();
            if (last != null && last.position().squaredDistanceTo(pos) > 64.0) {
                samples.clear();
            }
            samples.addLast(new PositionSample(now, pos));
            while (!samples.isEmpty() && now - samples.peekFirst().time() > HISTORY_NANOS) {
                samples.removeFirst();
            }
        }
        HISTORY.keySet().removeIf(id -> !seen.contains(id));
    }

    /** Returns the historical box only during an attack input, leaving normal movement/rendering alone. */
    public static Box historicalBox(PlayerEntity target, Box currentBox) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!Config.d.backtrack || client.player == null || client.options == null
                || !client.options.attackKey.isPressed() || target == client.player) {
            return currentBox;
        }

        ArrayDeque<PositionSample> samples = HISTORY.get(target.getUuid());
        if (samples == null || samples.isEmpty()) return currentBox;

        long wantedTime = System.nanoTime() - DELAY_NANOS;
        PositionSample selected = null;
        for (PositionSample sample : samples) {
            if (sample.time() <= wantedTime) selected = sample;
            else break;
        }
        if (selected == null) selected = samples.peekFirst();

        Vec3d offset = selected.position().subtract(target.getPos());
        return currentBox.offset(offset);
    }
}
