package com.ubivismedia.pathwaybuilder;

import org.bukkit.Location;
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
        PathResult pathResult = new PathResult();

        int dx = end.getBlockX() - start.getBlockX();
        int dz = end.getBlockZ() - start.getBlockZ();
        int steps = Math.max(Math.abs(dx), Math.abs(dz));

        for (int i = 0; i <= steps; i++) {
            double x = start.getBlockX() + (dx * i / (double) steps);
            double z = start.getBlockZ() + (dz * i / (double) steps);
            Location step = new Location(start.getWorld(), x, start.getBlockY(), z);
            pathResult.addStep(step);
        }

        return pathResult;
    }
}
