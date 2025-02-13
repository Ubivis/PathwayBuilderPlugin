package com.ubivismedia.pathwaybuilder;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class PathResult {
    private final List<Location> steps = new ArrayList<>();
    private final List<Material> originalMaterials = new ArrayList<>();

    public void addStep(Location location) {
        steps.add(location);
        originalMaterials.add(location.getBlock().getType());
    }

    public List<Location> getSteps() {
        return steps;
    }

    public void undoPath() {
        for (int i = 0; i < steps.size(); i++) {
            Block block = steps.get(i).getBlock();
            block.setType(originalMaterials.get(i));
        }
    }
}
