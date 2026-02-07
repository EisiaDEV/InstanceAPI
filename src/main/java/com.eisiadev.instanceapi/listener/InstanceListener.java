package com.eisiadev.instanceapi.listener;

import com.eisiadev.instanceapi.InstanceAPIPlugin;
import com.eisiadev.instanceapi.manager.InstanceManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class InstanceListener implements Listener {

    private final InstanceAPIPlugin plugin;

    public InstanceListener(InstanceAPIPlugin plugin) {
        this.plugin = plugin;
    }

    // HIGH 우선순위에서 처리 - 다른 플러그인보다 먼저 감지
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!plugin.getConfigManager().isAutoDeleteEnabled()) {
            return;
        }

        World fromWorld = event.getFrom().getWorld();
        World toWorld = event.getTo() != null ? event.getTo().getWorld() : null;

        // toWorld가 null이면 처리 안 함
        if (toWorld == null) {
            return;
        }

        // 인스턴스 월드에서 다른 월드로 텔레포트
        if (!fromWorld.equals(toWorld)) {
            InstanceManager manager = plugin.getInstanceManager();

            if (manager.isInstanceWorld(fromWorld)) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[TELEPORT-DETECTED] Player: " + event.getPlayer().getName() +
                            " leaving instance: " + fromWorld.getName());
                }
                scheduleEmptyCheck(fromWorld.getName());
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (!plugin.getConfigManager().isAutoDeleteEnabled()) {
            return;
        }

        World fromWorld = event.getFrom();
        String worldName = fromWorld.getName();
        InstanceManager manager = plugin.getInstanceManager();

        // 인스턴스 월드인지 확인 (RuneInstance 방식)
        if (manager.isInstanceWorld(fromWorld)) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[WORLD-CHANGE] Player: " + event.getPlayer().getName() +
                        ", From: " + fromWorld.getName() +
                        ", To: " + event.getPlayer().getWorld().getName());
            }

            // RuneInstance처럼 1틱 후에 스케줄
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                World currentWorld = Bukkit.getWorld(worldName);

                if (currentWorld != null && currentWorld.getPlayers().isEmpty()) {
                    plugin.getLogger().info("[AUTO-DELETE] Deleting empty instance: " + worldName);
                    plugin.getInstanceManager().deleteInstance(worldName);
                }
            }, 20L); // RuneInstance와 동일하게 60틱
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getConfigManager().isAutoDeleteEnabled()) {
            return;
        }

        World world = event.getPlayer().getWorld();
        InstanceManager manager = plugin.getInstanceManager();

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[PLAYER-QUIT] Player: " + event.getPlayer().getName() +
                    ", World: " + world.getName());
        }

        if (manager.isInstanceWorld(world)) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[QUIT-DETECTED] Scheduling empty check for: " + world.getName());
            }
            scheduleEmptyCheck(world.getName());
        }
    }

    private void scheduleEmptyCheck(String worldName) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World world = Bukkit.getWorld(worldName);

            if (world != null && world.getPlayers().isEmpty()) {
                plugin.getLogger().info("[AUTO-DELETE] Deleting empty instance: " + worldName);
                plugin.getInstanceManager().deleteInstance(worldName);
            } else if (world == null) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[AUTO-DELETE-SKIP] World already unloaded: " + worldName);
                }
            } else {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[AUTO-DELETE-SKIP] World still has players (" +
                            world.getPlayers().size() + "): " + worldName);
                }
            }
        }, 1L);
    }
}