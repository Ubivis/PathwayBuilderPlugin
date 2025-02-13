package com.ubivismedia.pathwaybuilder.util;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public class ChunkPreloader {
    private final Plugin plugin;
    private final Location point1;
    private final Location point2;
    private final Consumer<Void> onComplete;

    public ChunkPreloader(Plugin plugin, Location point1, Location point2, Consumer<Void> onComplete) {
        this.plugin = plugin;
        this.point1 = point1;
        this.point2 = point2;
        this.onComplete = onComplete;
    }

    public void preloadChunks() {
        World world = point1.getWorld();
        int minX = Math.min(point1.getBlockX(), point2.getBlockX()) >> 4;
        int maxX = Math.max(point1.getBlockX(), point2.getBlockX()) >> 4;
        int minZ = Math.min(point1.getBlockZ(), point2.getBlockZ()) >> 4;
        int maxZ = Math.max(point1.getBlockZ(), point2.getBlockZ()) >> 4;

        int totalChunks = (maxX - minX + 1) * (maxZ - minZ + 1);
        int[] loadedChunks = {0};

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                final int cx = x;
                final int cz = z;

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Chunk chunk = world.getChunkAt(cx, cz);
                    if (!chunk.isLoaded()) {
                        chunk.load(true);
                    }
                    loadedChunks[0]++;

                    if (loadedChunks[0] >= totalChunks) {
                        onComplete.accept(null);
                    }
                });
            }
        }
    }
}
