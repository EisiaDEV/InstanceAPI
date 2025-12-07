package com.eisiadev.instanceapi.manager;

import com.eisiadev.instanceapi.InstanceAPIPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public class ConfigManager {
    
    private final InstanceAPIPlugin plugin;
    private final FileConfiguration config;
    
    public ConfigManager(InstanceAPIPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }
    
    public File getTemplateDirectory() {
        String path = config.getString("template-directory", "worlds");
        File dir = new File(plugin.getDataFolder(), path);
        
        if (!dir.exists()) {
            dir.mkdirs();
            plugin.getLogger().info("Created template directory: " + dir.getAbsolutePath());
        }
        
        return dir;
    }
    
    public boolean isAutoDeleteEnabled() {
        return !config.getBoolean("auto-delete.enabled", true);
    }
    
    public int getEmptyDelayTicks() {
        return config.getInt("auto-delete.empty-delay-ticks", 20);
    }
    
    public int getWorldLoadDelayTicks() {
        return config.getInt("world-load-delay-ticks", 40);
    }
    
    public int getMaxInstancesPerPlayer() {
        return config.getInt("limits.max-per-player", 3);
    }
    
    public int getMaxTotalInstances() {
        return config.getInt("limits.max-total", 100);
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }
    
    public Component getMessage(String key, Object... args) {
        String prefix = config.getString("messages.prefix", "&a[&e InstanceAPI &a]&r ");
        String message = config.getString("messages." + key, key);
        
        // Replace placeholders
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }
        
        String fullMessage = prefix + message;
        return LegacyComponentSerializer.legacyAmpersand().deserialize(fullMessage);
    }
    
    public void reload() {
        plugin.reloadConfig();
    }
}