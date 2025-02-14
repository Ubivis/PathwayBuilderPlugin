package com.ubivismedia.pathwaybuilder;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class PathCalculator {

    private final Location start;
    private final Location end;
    private final FileConfiguration config;

    public PathCalculator(Location start, Location end, FileConfiguration config) {
        this.start = start;
        this.end = end;
        this.config = config;
    }

    public PathResult calculatePath() {
        PathResult result = new PathResult();

        int x1 = start.getBlockX();
        int y1 = start.getBlockY();
        int z1 = start.getBlockZ();
        int x2 = end.getBlockX();
        int y2 = end.getBlockY();
        int z2 = end.getBlockZ();

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int dz = Math.abs(z2 - z1);

        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int sz = z1 < z2 ? 1 : -1;

        int err = dx - dz;

        while (true) {
            Location current = new Location(start.getWorld(), x1, y1, z1);
            result.addStep(current, current.getBlock().getType());

            if (x1 == x2 && y1 == y2 && z1 == z2) break;

            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                z1 += sz;
            }
        }

        return result;
    }
}
