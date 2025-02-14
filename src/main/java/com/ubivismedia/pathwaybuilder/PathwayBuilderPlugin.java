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
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Color;

import java.io.*;
import java.util.*;

public class PathwayBuilderPlugin extends JavaPlugin implements Listener {

    private Location point1;
    private Location point2;
    private final HashMap<String, PathResult> pathHistory = new HashMap<>();
    private File pathStorageFile;
    private final String PATHWAY_STICK_NAME = ChatColor.GOLD + "Pathway Stick";
    private int highlightTaskId = -1;

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
                for (Map.Entry<String, PathResult> entry : pathHistory.entrySet()) {
                    String pathId = entry.getKey();
                    PathResult path = entry.getValue();
                    String creationDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(new java.util.Date(path.getCreationTimestamp()));

                    player.spigot().sendMessage(
                            new ComponentBuilder("§a[Click to Show Path]§r " + pathId + " (Created: " + creationDate + ")")
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/showpath " + pathId))
                                    .create()
                    );
                }
            }
        }
        return true;
    }

    private void savePathToFile(String pathId, PathResult result) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(pathStorageFile, true))) {
            oos.writeObject(pathId);
            oos.writeObject(result);
        } catch (IOException e) {
            getLogger().severe("Failed to save path: " + e.getMessage());
        }
    }

    private void loadPathsFromFile() {
        if (!pathStorageFile.exists()) {
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(pathStorageFile))) {
            while (true) {
                try {
                    String pathId = (String) ois.readObject();
                    PathResult result = (PathResult) ois.readObject();
                    pathHistory.put(pathId, result);
                } catch (EOFException e) {
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            getLogger().severe("Failed to load paths: " + e.getMessage());
        }
    }

    private void removePathFromFile(String pathId) {
        pathHistory.remove(pathId);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(pathStorageFile))) {
            for (Map.Entry<String, PathResult> entry : pathHistory.entrySet()) {
                oos.writeObject(entry.getKey());
                oos.writeObject(entry.getValue());
            }
        } catch (IOException e) {
            getLogger().severe("Failed to update path storage: " + e.getMessage());
        }
    }

    public void highlightPath(Player player, PathResult result) {
        highlightTaskId = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            private int index = 0;
            private int timer = 0;

            @Override
            public void run() {
                if (index < result.getSteps().size()) {
                    Location step = result.getSteps().get(index);
                    player.spawnParticle(Particle.FLAME, step, 5, 0, 0, 0, 0);
                    index++;
                }

                timer++;
                if (timer >= 60 * 20) { // Stop after 1 minute
                    Bukkit.getScheduler().cancelTask(highlightTaskId);
                    highlightTaskId = -1;
                }
            }
        }, 0L, 5L).getTaskId();
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.hasItemMeta() && PATHWAY_STICK_NAME.equals(item.getItemMeta().getDisplayName())) {
            if (event.getAction().toString().contains("LEFT_CLICK")) {
                point1 = event.getClickedBlock().getLocation();
                player.sendMessage(ChatColor.GREEN + "Point 1 set at: " + point1);
            } else if (event.getAction().toString().contains("RIGHT_CLICK")) {
                point2 = event.getClickedBlock().getLocation();
                player.sendMessage(ChatColor.GREEN + "Point 2 set at: " + point2);
            }
        }
    }
}
