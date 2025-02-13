package com.ubivismedia.pathwaybuilder.util;

import com.ubivismedia.pathwaybuilder.PathResult;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicInteger;

public class PathBuilder {

    private final Plugin plugin;
    private final PathResult result;
    private final FileConfiguration config;
    private final Runnable onComplete;
    private final PathSegmentBuilder segmentBuilder;

    public PathBuilder(Plugin plugin, PathResult result, FileConfiguration config, Runnable onComplete) {
        this.plugin = plugin;
        this.result = result;
        this.config = config;
        this.onComplete = onComplete;
        this.segmentBuilder = new PathSegmentBuilder(config);
    }

    public void buildInChunks() {
        final int batchSize = config.getInt("path_build_batch_size", 5);
        final int pathWidth = config.getInt("path_width", 3);
        AtomicInteger taskId = new AtomicInteger();

        taskId.set(plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            private int index = 0;

            @Override
            public void run() {
                int limit = Math.min(index + batchSize, result.getSteps().size());
                for (; index < limit; index++) {
                    Location step = result.getSteps().get(index);
                    segmentBuilder.buildSegment(step, pathWidth);
                }

                if (index >= result.getSteps().size()) {
                    onComplete.run();
                    plugin.getServer().getScheduler().cancelTask(taskId.get());
                }
            }
        }, 0L, 1L).getTaskId());
    }
}
