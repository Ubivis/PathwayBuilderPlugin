package com.ubivismedia.pathwaybuilder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.ubivismedia.pathwaybuilder.util.ChunkPreloader;
import com.ubivismedia.pathwaybuilder.util.PathBuilder;

import java.util.ArrayList;
import java.util.List;

public class PathwayBuilderPlugin extends JavaPlugin {

    private Location point1;
    private Location point2;
    private PathResult lastPathResult;
    private final List<PathResult> pathHistory = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("PathwayBuilderPlugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PathwayBuilderPlugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("setpathpoint")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /setpathpoint <1|2>");
                return true;
            }

            Location location = player.getLocation().getBlock().getLocation();

            if (args[0].equals("1")) {
                point1 = location;
                player.sendMessage("Point 1 set at: " + location);
            } else if (args[0].equals("2")) {
                point2 = location;
                player.sendMessage("Point 2 set at: " + location);
            } else {
                player.sendMessage("Invalid point. Use 1 or 2.");
            }

        } else if (command.getName().equalsIgnoreCase("buildpath")) {
            if (point1 == null || point2 == null) {
                player.sendMessage("Both points must be set before building a path.");
                return true;
            }

            player.sendMessage("Preloading chunks...");

            new ChunkPreloader(this, point1, point2, (ignored) -> {
                player.sendMessage("Chunks preloaded. Calculating path...");

                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    PathCalculator calculator = new PathCalculator(point1, point2, getConfig());
                    PathResult result = calculator.calculatePath();

                    Bukkit.getScheduler().runTask(this, () -> {
                        new PathBuilder(this, result, getConfig(), () -> {
                            lastPathResult = result;
                            pathHistory.add(result);
                            player.sendMessage("Path built!");
                        }).buildInChunks();
                    });
                });
            }).preloadChunks();

        } else if (command.getName().equalsIgnoreCase("undopath")) {
            if (pathHistory.isEmpty()) {
                player.sendMessage("No path to undo.");
            } else {
                PathResult toUndo = pathHistory.remove(pathHistory.size() - 1);
                toUndo.undoPath();
                lastPathResult = null;
                player.sendMessage("Path undone!");
            }
        }
        return true;
    }
}
