package com.ubivismedia.pathwaybuilder;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import com.ubivismedia.pathwaybuilder.util.ChunkPreloader;
import com.ubivismedia.pathwaybuilder.util.PathBuilder;

import java.io.*;
import java.util.*;

public class PathwayBuilderPlugin extends JavaPlugin implements Listener {

    private Location point1;
    private Location point2;
    private final HashMap<String, PathResult> pathHistory = new HashMap<>();
    private File pathStorageFile;
    private final String PATHWAY_STICK_NAME = ChatColor.GOLD + "Pathway Stick";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        File pathsDir = new File(getDataFolder(), "paths");
        if (!pathsDir.exists()) {
            pathsDir.mkdirs();
        }
        pathStorageFile = new File(pathsDir, "pathways.db");
        loadPathsFromFile();
        Bukkit.getPluginManager().registerEvents(this, this);
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

        if (!player.isOp()) {
            player.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("getpathwaystick")) {
            ItemStack stick = new ItemStack(Material.STICK);
            ItemMeta meta = stick.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(PATHWAY_STICK_NAME);
                stick.setItemMeta(meta);
            }
            player.getInventory().addItem(stick);
            player.sendMessage(ChatColor.GREEN + "You have received the Pathway Stick!");

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
                            String pathId = UUID.randomUUID().toString();
                            pathHistory.put(pathId, result);
                            savePathToFile(pathId, result);
                            player.sendMessage("Path built! Path ID: " + pathId);
                        }).buildInChunks();
                    });
                });
            }).preloadChunks();

        } else if (command.getName().equalsIgnoreCase("undopath")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /undopath <id>");
                return true;
            }
            String pathId = args[0];

            PathResult toUndo = pathHistory.remove(pathId);
            if (toUndo == null) {
                player.sendMessage("Path with ID " + pathId + " not found.");
            } else {
                toUndo.undoPath();
                removePathFromFile(pathId);
                player.sendMessage("Path " + pathId + " undone!");
            }
        } else if (command.getName().equalsIgnoreCase("listpaths")) {
            if (pathHistory.isEmpty()) {
                player.sendMessage("No paths available.");
            } else {
                player.sendMessage("Built Paths:");
                for (String pathId : pathHistory.keySet()) {
                    player.spigot().sendMessage(
                            new ComponentBuilder("§a[Click to Show Path]§r " + pathId)
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/showpath " + pathId))
                                    .create()
                    );
                }
            }
        } else if (command.getName().equalsIgnoreCase("showpath")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /showpath <id>");
                return true;
            }
            String pathId = args[0];

            PathResult pathResult = pathHistory.get(pathId);
            if (pathResult == null) {
                player.sendMessage("Path with ID " + pathId + " not found.");
            } else {
                player.sendMessage("Highlighting path " + pathId + " for 1 minute.");
                highlightPath(player, pathResult);
            }
        }
        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp() || player.getInventory().getItemInMainHand().getType() != Material.STICK) return;

        ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
        if (meta == null || !PATHWAY_STICK_NAME.equals(meta.getDisplayName())) return;

        if (event.getAction().toString().contains("LEFT_CLICK")) {
            point1 = player.getLocation().getBlock().getLocation();
            player.sendMessage(ChatColor.YELLOW + "Point 1 set with Pathway Stick at: " + point1);
        } else if (event.getAction().toString().contains("RIGHT_CLICK")) {
            point2 = player.getLocation().getBlock().getLocation();
            player.sendMessage(ChatColor.YELLOW + "Point 2 set with Pathway Stick at: " + point2);
        }
    }

private void highlightPath(Player player, PathResult pathResult) {
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 1200) {
                    Bukkit.getScheduler().cancelTask(this.hashCode());
                    return;
                }

                for (Location loc : pathResult.getSteps()) {
                    player.spawnParticle(Particle.REDSTONE, loc.clone().add(0.5, 1, 0.5), 5, new Particle.DustOptions(org.bukkit.Color.RED, 1));
                }
                ticks += 20;
            }
        }, 0L, 20L);
    }

    private void savePathToFile(String pathId, PathResult result) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pathStorageFile, true))) {
            for (Location step : result.getSteps()) {
                writer.write(pathId + ";" + step.getWorld().getName() + ";" + step.getBlockX() + ";" + step.getBlockY() + ";" + step.getBlockZ());
                writer.newLine();
            }
        } catch (IOException e) {
            getLogger().severe("Failed to save path to file: " + e.getMessage());
        }
    }

    private void removePathFromFile(String pathId) {
        try {
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(pathStorageFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith(pathId + ";")) {
                        lines.add(line);
                    }
                }
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(pathStorageFile))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            getLogger().severe("Failed to remove path from file: " + e.getMessage());
        }
    }

    private void loadPathsFromFile() {
        if (!pathStorageFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(pathStorageFile))) {
            String line;
            PathResult currentPathResult = null;
            String currentPathId = null;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length != 5) continue;

                String pathId = parts[0];
                String worldName = parts[1];
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);
                int z = Integer.parseInt(parts[4]);

                Location location = new Location(Bukkit.getWorld(worldName), x, y, z);

                if (!pathId.equals(currentPathId)) {
                    if (currentPathId != null) {
                        pathHistory.put(currentPathId, currentPathResult);
                    }
                    currentPathResult = new PathResult();
                    currentPathId = pathId;
                }

                if (currentPathResult != null) {
                    currentPathResult.addStep(location);
                }
            }

            if (currentPathId != null) {
                pathHistory.put(currentPathId, currentPathResult);
            }

        } catch (IOException e) {
            getLogger().severe("Failed to load paths from file: " + e.getMessage());
        }
    }
}
