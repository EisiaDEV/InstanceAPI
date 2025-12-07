package com.eisiadev.instanceapi.event;

import com.eisiadev.instanceapi.data.InstanceData;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class InstanceWorldCreatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final World world;
    private final InstanceData instanceData;

    public InstanceWorldCreatedEvent(@NotNull World world, @NotNull InstanceData instanceData) {
        this.world = world;
        this.instanceData = instanceData;
    }

    @NotNull
    public World getWorld() {
        return world;
    }

    @NotNull
    public InstanceData getInstanceData() {
        return instanceData;
    }

    @NotNull
    public String getTemplate() {
        return instanceData.getTemplate();
    }

    @NotNull
    public UUID getOwner() {
        return instanceData.getOwner();
    }

    public long getCreatedAt() {
        return instanceData.getCreatedAt();
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}