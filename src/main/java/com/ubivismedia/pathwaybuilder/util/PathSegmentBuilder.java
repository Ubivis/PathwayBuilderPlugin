package com.ubivismedia.pathwaybuilder.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

public class PathSegmentBuilder {

    private final FileConfiguration config;

    public PathSegmentBuilder(FileConfiguration config) {
        this.config = config;
    }

    public void buildSegment(Location center, int width) {
        World world = center.getWorld();
        Material pathMaterial = Material.matchMaterial(config.getString("path_styles.default", "GRASS_PATH"));
        Material bridgeMaterial = Material.matchMaterial(config.getString("path_styles.bridge", "OAK_PLANKS"));
        Material tunnelMaterial = Material.matchMaterial(config.getString("path_styles.tunnel", "STONE_BRICKS"));
        Material edgeMaterial = Material.matchMaterial(config.getString("decorative_edges", "STONE_BRICKS"));
        Material railingMaterial = Material.matchMaterial(config.getString("bridge_railings", "OAK_FENCE"));
        Material slopeMaterial = Material.matchMaterial(config.getString("slope_material", "STONE_STAIRS"));

        for (int dx = -width / 2; dx <= width / 2; dx++) {
            for (int dz = -width / 2; dz <= width / 2; dz++) {
                Location pathBlockLoc = center.clone().add(dx, 0, dz);
                Block pathBlock = world.getBlockAt(pathBlockLoc);
                Block belowBlock = world.getBlockAt(pathBlockLoc.getBlockX(), pathBlockLoc.getBlockY() - 1, pathBlockLoc.getBlockZ());

                if (belowBlock.getType() == Material.AIR || belowBlock.isLiquid()) {
                    while (belowBlock.getType() == Material.AIR || belowBlock.isLiquid()) {
                        belowBlock.setType(bridgeMaterial);
                        belowBlock = world.getBlockAt(belowBlock.getLocation().add(0, -1, 0));
                    }
                    pathBlock.setType(bridgeMaterial);
                    if (dx == -width / 2 || dx == width / 2) {
                        pathBlockLoc.clone().add(0, 1, 0).getBlock().setType(railingMaterial);
                    }
                } else {
                    boolean isTunnel = false;
                    for (int i = 1; i <= 2; i++) {
                        if (world.getBlockAt(pathBlockLoc.getBlockX(), pathBlockLoc.getBlockY() + i, pathBlockLoc.getBlockZ()).getType().isSolid()) {
                            isTunnel = true;
                            break;
                        }
                    }
                    if (isTunnel) {
                        for (int i = 0; i <= 2; i++) {
                            world.getBlockAt(pathBlockLoc.getBlockX(), pathBlockLoc.getBlockY() + i, pathBlockLoc.getBlockZ()).setType(Material.AIR);
                        }
                        pathBlock.setType(tunnelMaterial);
                    } else {
                        if (belowBlock.getType().isSolid() && belowBlock.getY() < pathBlock.getY()) {
                            pathBlock.setType(slopeMaterial);
                        } else if (pathBlock.getType() != pathMaterial) {
                            pathBlock.setType(pathMaterial);
                        }
                    }
                }
            }
        }
    }
}
