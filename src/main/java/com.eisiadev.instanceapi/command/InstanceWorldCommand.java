package com.eisiadev.instanceapi.command;

import com.eisiadev.instanceapi.InstanceAPIPlugin;
import com.eisiadev.instanceapi.data.InstanceData;
import com.eisiadev.instanceapi.manager.ConfigManager;
import com.eisiadev.instanceapi.manager.InstanceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class InstanceWorldCommand implements CommandExecutor, TabCompleter {

    private final InstanceAPIPlugin plugin;

    public InstanceWorldCommand(InstanceAPIPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "list":
                return handleList(sender, args);
            case "tp":
            case "teleport":
                return handleTeleport(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "reload":
                return handleReload(sender);
            case "debug":
                return handleDebug(sender, args);
            case "clear":
                return handleClear(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("instanceapi.create")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /instanceworld create <template> [player]")
                    .color(NamedTextColor.RED));
            return true;
        }

        String template = args[1];
        Player target;

        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Console must specify a player.")
                        .color(NamedTextColor.RED));
                return true;
            }
            target = (Player) sender;
        }

        plugin.getInstanceManager().createInstance(target, template);
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        InstanceManager manager = plugin.getInstanceManager();

        if (args.length < 2) {
            // Delete own instance
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Console must specify a world name.")
                        .color(NamedTextColor.RED));
                return true;
            }

            if (!sender.hasPermission("instanceapi.delete")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            Player player = (Player) sender;
            List<String> instances = manager.getPlayerInstances(player.getUniqueId());

            if (instances.isEmpty()) {
                // Do nothing - silently ignore if no instances
                return true;
            }

            sender.sendMessage(plugin.getConfigManager().getMessage("instance-deleting"));
            for (String worldName : instances) {
                manager.deleteInstance(worldName);
            }

        } else {
            // Delete specific world
            if (!sender.hasPermission("instanceapi.delete.others")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            String worldName = args[1];
            if (!manager.isInstanceWorld(worldName)) {
                // Silently ignore if world not found
                return true;
            }

            sender.sendMessage(plugin.getConfigManager().getMessage("instance-deleting"));
            manager.deleteInstance(worldName);
        }

        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("instanceapi.list")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        InstanceManager manager = plugin.getInstanceManager();

        if (args.length >= 2) {
            // List specific player's instances
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                return true;
            }

            List<String> instances = manager.getPlayerInstances(target.getUniqueId());
            sender.sendMessage(Component.text("Instances for " + target.getName() + ": " + instances.size())
                    .color(NamedTextColor.GOLD));

            for (String worldName : instances) {
                InstanceData data = manager.getInstanceData(worldName);
                if (data != null) {
                    sender.sendMessage(Component.text("  - " + worldName + " (Template: " + data.getTemplate() + ")")
                            .color(NamedTextColor.YELLOW));
                }
            }

        } else {
            // List all instances
            List<InstanceData> instances = manager.getAllInstances();
            sender.sendMessage(Component.text("Total instances: " + instances.size())
                    .color(NamedTextColor.GOLD));

            for (InstanceData data : instances) {
                String ownerName = Bukkit.getOfflinePlayer(data.getOwner()).getName();
                sender.sendMessage(Component.text("  - " + data.getWorldName() + " (Owner: " + ownerName +
                                ", Template: " + data.getTemplate() + ")")
                        .color(NamedTextColor.YELLOW));
            }
        }

        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("instanceapi.teleport")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can teleport.")
                    .color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /instanceworld tp <worldname>")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("world-not-found"));
            return true;
        }

        player.teleport(world.getSpawnLocation());
        sender.sendMessage(plugin.getConfigManager().getMessage("teleported"));

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("instanceapi.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /instanceworld info <worldname>")
                    .color(NamedTextColor.RED));
            return true;
        }

        String worldName = args[1];
        InstanceData data = plugin.getInstanceManager().getInstanceData(worldName);

        if (data == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("world-not-found"));
            return true;
        }

        String ownerName = Bukkit.getOfflinePlayer(data.getOwner()).getName();
        sender.sendMessage(Component.text("Instance Information:").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  World: " + data.getWorldName()).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Template: " + data.getTemplate()).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Owner: " + ownerName).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Created: " + new Date(data.getCreatedAt())).color(NamedTextColor.YELLOW));

        if (!data.getMetadata().isEmpty()) {
            sender.sendMessage(Component.text("  Metadata:").color(NamedTextColor.YELLOW));
            data.getMetadata().forEach((key, value) -> {
                sender.sendMessage(Component.text("    " + key + ": " + value).color(NamedTextColor.GRAY));
            });
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("instanceapi.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        plugin.getConfigManager().reload();
        sender.sendMessage(Component.text("Configuration reloaded!").color(NamedTextColor.GREEN));

        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("instanceapi.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /instanceworld debug <player>")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }

        InstanceManager manager = plugin.getInstanceManager();
        List<String> instances = manager.getPlayerInstances(target.getUniqueId());

        sender.sendMessage(Component.text("=== Debug Info for " + target.getName() + " ===")
                .color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("UUID: " + target.getUniqueId()).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Current World: " + target.getWorld().getName()).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Is in instance: " + manager.isInstanceWorld(target.getWorld()))
                .color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Registered instances: " + instances.size()).color(NamedTextColor.YELLOW));

        if (!instances.isEmpty()) {
            sender.sendMessage(Component.text("Instance list:").color(NamedTextColor.YELLOW));
            for (String worldName : instances) {
                World world = Bukkit.getWorld(worldName);
                boolean exists = world != null;
                int players = exists ? world.getPlayers().size() : 0;

                sender.sendMessage(Component.text("  - " + worldName + " (Loaded: " + exists +
                        ", Players: " + players + ")").color(NamedTextColor.GRAY));
            }
        }

        sender.sendMessage(Component.text("Total server instances: " + manager.getTotalInstanceCount())
                .color(NamedTextColor.YELLOW));

        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("instanceapi.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /instanceworld clear <player>")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetId;
        String targetName;

        if (target != null) {
            targetId = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Try as offline player
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            targetId = offline.getUniqueId();
            targetName = args[1];
        }

        InstanceManager manager = plugin.getInstanceManager();
        int count = manager.getPlayerInstanceCount(targetId);

        manager.clearPlayerInstances(targetId);

        sender.sendMessage(Component.text("Cleared " + count + " instance registrations for " + targetName)
                .color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Note: This only clears the registration. World files are not deleted.")
                .color(NamedTextColor.YELLOW));

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("InstanceAPI Commands:").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/instanceworld create <template> [player]").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/instanceworld delete [worldname]").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/instanceworld list [player]").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/instanceworld tp <worldname>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/instanceworld info <worldname>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/instanceworld debug <player>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/instanceworld clear <player>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/instanceworld reload").color(NamedTextColor.YELLOW));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "delete", "list", "tp", "info", "debug", "clear", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "create":
                    return getTemplateNames().stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "delete":
                case "tp":
                case "info":
                    return plugin.getInstanceManager().getAllInstances().stream()
                            .map(InstanceData::getWorldName)
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "list":
                case "debug":
                case "clear":
                    return null; // Player names
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return null; // Player names
        }

        return Collections.emptyList();
    }

    private List<String> getTemplateNames() {
        File templateDir = plugin.getConfigManager().getTemplateDirectory();
        File[] files = templateDir.listFiles(File::isDirectory);

        if (files == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(files)
                .map(File::getName)
                .collect(Collectors.toList());
    }
}