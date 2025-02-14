package com.ubivismedia.pathwaybuilder;

import org.bukkit.Location;
import org.bukkit.Material;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathResult implements Serializable {
    private final List<Location> steps;
    private final Map<Location, Material> originalMaterials;
    private final long creationTimestamp;

    public PathResult() {
        this.steps = new ArrayList<>();
        this.originalMaterials = new HashMap<>();
        this.creationTimestamp = System.currentTimeMillis();
    }

    public List<Location> getSteps() {
        return steps;
    }

    public Map<Location, Material> getOriginalMaterials() {
        return originalMaterials;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void addStep(Location location, Material originalMaterial) {
        steps.add(location);
        originalMaterials.put(location, originalMaterial);
    }

    public void undoPath() {
        for (Location step : steps) {
            Material originalMaterial = originalMaterials.get(step);
            if (originalMaterial != null) {
                step.getBlock().setType(originalMaterial);
            }
        }
    }
}
