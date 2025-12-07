package com.eisiadev.instanceapi.api;

import com.eisiadev.instanceapi.InstanceAPIPlugin;
import com.eisiadev.instanceapi.data.InstanceData;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for InstanceAPI plugin
 * This class provides methods for other plugins to interact with instance worlds
 */
public class InstanceAPI {
    
    /**
     * Creates a new instance world for a player
     * 
     * @param owner The player who will own the instance
     * @param template The template world name to copy from
     * @return CompletableFuture that completes with the created World
     */
    @NotNull
    public static CompletableFuture<World> createInstance(@NotNull Player owner, @NotNull String template) {
        return InstanceAPIPlugin.getInstance().getInstanceManager().createInstance(owner, template);
    }
    
    /**
     * Creates a new instance world for a player with custom metadata
     * 
     * @param owner The player who will own the instance
     * @param template The template world name to copy from
     * @param metadata Custom metadata to store with the instance
     * @return CompletableFuture that completes with the created World
     */
    @NotNull
    public static CompletableFuture<World> createInstance(@NotNull Player owner, @NotNull String template, 
                                                          @NotNull Map<String, Object> metadata) {
        return InstanceAPIPlugin.getInstance().getInstanceManager().createInstance(owner, template, metadata);
    }
    
    /**
     * Deletes an instance world by name
     * 
     * @param worldName The name of the world to delete
     */
    public static void deleteInstance(@NotNull String worldName) {
        InstanceAPIPlugin.getInstance().getInstanceManager().deleteInstance(worldName);
    }
    
    /**
     * Checks if a world is an instance world
     * 
     * @param world The world to check
     * @return true if the world is an instance, false otherwise
     */
    public static boolean isInstanceWorld(@NotNull World world) {
        return InstanceAPIPlugin.getInstance().getInstanceManager().isInstanceWorld(world);
    }
    
    /**
     * Checks if a world name belongs to an instance world
     * 
     * @param worldName The world name to check
     * @return true if the world is an instance, false otherwise
     */
    public static boolean isInstanceWorld(@NotNull String worldName) {
        return InstanceAPIPlugin.getInstance().getInstanceManager().isInstanceWorld(worldName);
    }
    
    /**
     * Gets the instance data for a world
     * 
     * @param worldName The world name
     * @return InstanceData or null if not found
     */
    @Nullable
    public static InstanceData getInstanceData(@NotNull String worldName) {
        return InstanceAPIPlugin.getInstance().getInstanceManager().getInstanceData(worldName);
    }
    
    /**
     * Gets the template name of an instance world
     * 
     * @param worldName The world name
     * @return The template name or null if not found
     */
    @Nullable
    public static String getTemplate(@NotNull String worldName) {
        InstanceData data = getInstanceData(worldName);
        return data != null ? data.getTemplate() : null;
    }
    
    /**
     * Gets the owner UUID of an instance world
     * 
     * @param worldName The world name
     * @return The owner UUID or null if not found
     */
    @Nullable
    public static UUID getOwner(@NotNull String worldName) {
        return InstanceAPIPlugin.getInstance().getInstanceManager().getOwner(worldName);
    }
    
    /**
     * Gets all instance world names owned by a player
     * 
     * @param playerId The player's UUID
     * @return List of world names
     */
    @NotNull
    public static List<String> getPlayerInstances(@NotNull UUID playerId) {
        return InstanceAPIPlugin.getInstance().getInstanceManager().getPlayerInstances(playerId);
    }
    
    /**
     * Gets all active instances
     * 
     * @return List of InstanceData for all instances
     */
    @NotNull
    public static List<InstanceData> getAllInstances() {
        return InstanceAPIPlugin.getInstance().getInstanceManager().getAllInstances();
    }
    
    /**
     * Gets the number of instances owned by a player
     * 
     * @param playerId The player's UUID
     * @return Number of instances
     */
    public static int getPlayerInstanceCount(@NotNull UUID playerId) {
        return InstanceAPIPlugin.getInstance().getInstanceManager().getPlayerInstanceCount(playerId);
    }
    
    /**
     * Gets the total number of active instances
     * 
     * @return Total instance count
     */
    public static int getTotalInstanceCount() {
        return InstanceAPIPlugin.getInstance().getInstanceManager().getTotalInstanceCount();
    }
}