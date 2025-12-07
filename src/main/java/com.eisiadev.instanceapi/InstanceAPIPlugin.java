package com.eisiadev.instanceapi;

import com.eisiadev.instanceapi.command.InstanceWorldCommand;
import com.eisiadev.instanceapi.listener.InstanceListener;
import com.eisiadev.instanceapi.manager.InstanceManager;
import com.eisiadev.instanceapi.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class InstanceAPIPlugin extends JavaPlugin {
    
    private static InstanceAPIPlugin instance;
    private InstanceManager instanceManager;
    private ConfigManager configManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Load configuration
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        
        // Initialize managers
        instanceManager = new InstanceManager(this);
        
        // Register commands
        var command = getCommand("instanceworld");
        if (command != null) {
            var executor = new InstanceWorldCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe("Command 'instanceworld' not found in plugin.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new InstanceListener(this), this);
        
        // Check for Skript
        if (Bukkit.getPluginManager().getPlugin("Skript") != null) {
            try {
                com.eisiadev.instanceapi.skript.SkriptIntegration.register();
                getLogger().info("Skript integration enabled.");
            } catch (Exception e) {
                getLogger().warning("Failed to register Skript integration: " + e.getMessage());
            }
        }
        
        getLogger().info("InstanceAPI has been enabled!");
        getLogger().info("Template directory: " + configManager.getTemplateDirectory().getAbsolutePath());
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Cleaning up all instances...");
        
        if (instanceManager != null) {
            instanceManager.cleanupAllInstances();
        }
        
        getLogger().info("InstanceAPI has been disabled!");
    }
    
    public static InstanceAPIPlugin getInstance() {
        return instance;
    }
    
    public InstanceManager getInstanceManager() {
        return instanceManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
}