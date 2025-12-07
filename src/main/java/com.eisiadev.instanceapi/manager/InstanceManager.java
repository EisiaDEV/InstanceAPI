package com.eisiadev.instanceapi.manager;

import com.eisiadev.instanceapi.InstanceAPIPlugin;
import com.eisiadev.instanceapi.data.InstanceData;
import com.eisiadev.instanceapi.event.InstanceWorldCreatedEvent;
import com.eisiadev.instanceapi.event.InstanceWorldDeletedEvent;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class InstanceManager {

    private static final int UUID_SUBSTRING_LENGTH = 8;

    private final InstanceAPIPlugin plugin;
    private final Map<UUID, Set<String>> playerInstances; // player -> world names
    private final Map<String, InstanceData> instanceDataMap; // world name -> data

    public InstanceManager(InstanceAPIPlugin plugin) {
        this.plugin = plugin;
        this.playerInstances = new ConcurrentHashMap<>();
        this.instanceDataMap = new ConcurrentHashMap<>();

        loadExistingInstances();
    }

    private void loadExistingInstances() {
        File dataDir = new File(plugin.getDataFolder(), "instances");
        if (!dataDir.exists()) {
            return;
        }

        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                InstanceData data = InstanceData.load(file);
                instanceDataMap.put(data.getWorldName(), data);
                playerInstances.computeIfAbsent(data.getOwner(), k -> ConcurrentHashMap.newKeySet())
                        .add(data.getWorldName());

                plugin.getLogger().info("Loaded instance data: " + data.getWorldName());
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load instance data: " + file.getName(), e);
            }
        }
    }

    public CompletableFuture<World> createInstance(@NotNull Player player, @NotNull String templateName) {
        return createInstance(player, templateName, new HashMap<>());
    }

    public CompletableFuture<World> createInstance(@NotNull Player player, @NotNull String templateName,
                                                   @NotNull Map<String, Object> metadata) {
        UUID playerId = player.getUniqueId();
        ConfigManager config = plugin.getConfigManager();

        // Check limits
        int maxPerPlayer = config.getMaxInstancesPerPlayer();
        if (maxPerPlayer > 0 && getPlayerInstanceCount(playerId) >= maxPerPlayer) {
            CompletableFuture<World> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Player instance limit reached"));
            player.sendMessage(config.getMessage("instance-limit-player", maxPerPlayer));
            return future;
        }

        int maxTotal = config.getMaxTotalInstances();
        if (maxTotal > 0 && getTotalInstanceCount() >= maxTotal) {
            CompletableFuture<World> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Server instance limit reached"));
            player.sendMessage(config.getMessage("instance-limit-server", maxTotal));
            return future;
        }

        CompletableFuture<World> future = new CompletableFuture<>();

        player.sendMessage(config.getMessage("instance-creating"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File templateDir = new File(config.getTemplateDirectory(), templateName);

                if (!templateDir.exists() || !templateDir.isDirectory()) {
                    future.completeExceptionally(new IllegalArgumentException("Template not found: " + templateName));
                    player.sendMessage(config.getMessage("template-not-found", templateName,
                            config.getTemplateDirectory().getAbsolutePath()));
                    return;
                }

                String instanceName = generateInstanceName(playerId, templateName);
                File instanceFolder = new File(Bukkit.getWorldContainer(), instanceName);

                // Copy world files
                FileUtils.copyDirectory(templateDir, instanceFolder);
                cleanupUidFile(instanceFolder);

                // Create instance data
                InstanceData data = new InstanceData(instanceName, templateName, playerId);
                data.getMetadata().putAll(metadata);

                // Save instance data
                File dataFile = new File(plugin.getDataFolder(), "instances/" + instanceName + ".json");
                data.save(dataFile);

                // Load world on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    WorldCreator creator = new WorldCreator(instanceName);
                    World world = creator.createWorld();

                    if (world != null) {
                        registerInstance(playerId, instanceName, data);

                        // Fire event
                        InstanceWorldCreatedEvent event = new InstanceWorldCreatedEvent(world, data);
                        Bukkit.getPluginManager().callEvent(event);

                        player.sendMessage(config.getMessage("instance-created"));

                        // Teleport after delay
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                Location spawnLoc = world.getSpawnLocation();
                                player.teleport(spawnLoc);
                                player.sendMessage(config.getMessage("teleported"));
                            }
                        }, config.getWorldLoadDelayTicks());

                        future.complete(world);
                    } else {
                        future.completeExceptionally(new IllegalStateException("Failed to load world"));
                    }
                });

            } catch (Exception e) {
                future.completeExceptionally(e);
                plugin.getLogger().log(Level.SEVERE, "Failed to create instance", e);
            }
        });

        return future;
    }

    public void deleteInstance(@NotNull String worldName) {
        InstanceData data = instanceDataMap.get(worldName);

        // Unregister FIRST - even if world doesn't exist anymore
        if (data != null) {
            unregisterInstance(data.getOwner(), worldName);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[INSTANCE-DELETED] Player: " + data.getOwner() + ", World: " + worldName);
            }
        } else {
            // Try to find owner even if data is missing
            UUID owner = findOwnerByWorldName(worldName);
            if (owner != null) {
                unregisterInstance(owner, worldName);
                plugin.getLogger().warning("[INSTANCE-DELETED-NO-DATA] Found owner for world without data: " + worldName);
            }
        }

        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            // Teleport players out
            World mainWorld = Bukkit.getWorlds().get(0);
            for (Player p : world.getPlayers()) {
                p.teleport(mainWorld.getSpawnLocation());
                p.sendMessage(plugin.getConfigManager().getMessage("instance-deleted"));
            }

            // Unload world
            if (!Bukkit.unloadWorld(world, false)) {
                plugin.getLogger().severe("Failed to unload world: " + worldName);
                return;
            }
        } else {
            plugin.getLogger().info("World " + worldName + " already unloaded, skipping unload step");
        }

        // Delete files asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                if (worldFolder.exists()) {
                    FileUtils.deleteDirectory(worldFolder);
                    plugin.getLogger().info("Deleted instance files: " + worldName);
                } else {
                    plugin.getLogger().info("World folder " + worldName + " already deleted");
                }

                // Delete data file
                File dataFile = new File(plugin.getDataFolder(), "instances/" + worldName + ".json");
                if (dataFile.exists()) {
                    dataFile.delete();
                }

                // Fire event on main thread
                if (data != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        InstanceWorldDeletedEvent event = new InstanceWorldDeletedEvent(worldName, data);
                        Bukkit.getPluginManager().callEvent(event);
                    });
                }

            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete instance folder: " + worldName, e);
            }
        });
    }

    /**
     * Find owner by world name (slower, searches through all entries)
     */
    @Nullable
    private UUID findOwnerByWorldName(@NotNull String worldName) {
        return playerInstances.entrySet().stream()
                .filter(entry -> entry.getValue().contains(worldName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void cleanupAllInstances() {
        World mainWorld = Bukkit.getWorlds().get(0);

        for (InstanceData data : instanceDataMap.values()) {
            World world = Bukkit.getWorld(data.getWorldName());
            if (world != null) {
                for (Player p : world.getPlayers()) {
                    p.teleport(mainWorld.getSpawnLocation());
                    p.sendMessage(plugin.getConfigManager().getMessage("server-shutdown"));
                }
                Bukkit.unloadWorld(world, false);
            }
        }
    }

    private void registerInstance(UUID playerId, String worldName, InstanceData data) {
        playerInstances.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(worldName);
        instanceDataMap.put(worldName, data);

        plugin.getLogger().info("[INSTANCE-REGISTERED] Player: " + playerId + ", World: " + worldName +
                ", Total for player: " + playerInstances.get(playerId).size());
    }

    private void unregisterInstance(UUID playerId, String worldName) {
        Set<String> instances = playerInstances.get(playerId);
        if (instances != null) {
            instances.remove(worldName);
            plugin.getLogger().info("[INSTANCE-UNREGISTERED] Player: " + playerId + ", World: " + worldName +
                    ", Remaining for player: " + instances.size());
            if (instances.isEmpty()) {
                playerInstances.remove(playerId);
                plugin.getLogger().info("[INSTANCE-CLEANUP] Removed empty set for player: " + playerId);
            }
        }
        instanceDataMap.remove(worldName);
    }

    /**
     * Force unregister an instance from a player (for debugging/manual cleanup)
     */
    public void forceUnregisterInstance(@NotNull UUID playerId, @NotNull String worldName) {
        unregisterInstance(playerId, worldName);
        plugin.getLogger().warning("[FORCE-UNREGISTER] Player: " + playerId + ", World: " + worldName);
    }

    /**
     * Clear all instances for a player (for debugging/manual cleanup)
     */
    public void clearPlayerInstances(@NotNull UUID playerId) {
        Set<String> instances = playerInstances.get(playerId);
        if (instances != null) {
            int count = instances.size();
            instances.clear();
            playerInstances.remove(playerId);
            plugin.getLogger().warning("[FORCE-CLEAR] Cleared " + count + " instances for player: " + playerId);
        }
    }

    private void cleanupUidFile(File worldFolder) {
        Path uidFile = worldFolder.toPath().resolve("uid.dat");
        try {
            Files.deleteIfExists(uidFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete uid.dat", e);
        }
    }

    private String generateInstanceName(UUID playerId, String templateName) {
        String cleanTemplate = templateName.replace("_world", "");
        return cleanTemplate + "_" + playerId.toString().substring(0, UUID_SUBSTRING_LENGTH)
                + "_" + System.currentTimeMillis();
    }

    // Query methods
    public boolean isInstanceWorld(@NotNull String worldName) {
        return instanceDataMap.containsKey(worldName);
    }

    public boolean isInstanceWorld(@NotNull World world) {
        return isInstanceWorld(world.getName());
    }

    @Nullable
    public InstanceData getInstanceData(@NotNull String worldName) {
        return instanceDataMap.get(worldName);
    }

    @Nullable
    public UUID getOwner(@NotNull String worldName) {
        InstanceData data = instanceDataMap.get(worldName);
        return data != null ? data.getOwner() : null;
    }

    public List<String> getPlayerInstances(@NotNull UUID playerId) {
        Set<String> instances = playerInstances.get(playerId);
        return instances != null ? new ArrayList<>(instances) : Collections.emptyList();
    }

    public List<InstanceData> getAllInstances() {
        return new ArrayList<>(instanceDataMap.values());
    }

    public int getPlayerInstanceCount(@NotNull UUID playerId) {
        Set<String> instances = playerInstances.get(playerId);
        return instances != null ? instances.size() : 0;
    }

    public int getTotalInstanceCount() {
        return instanceDataMap.size();
    }
}